package com.ferg.awfulapp.thread

import android.content.ContentValues
import android.net.Uri
import android.util.Log
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.DatabaseHelper
import com.ferg.awfulapp.thread.AwfulPost.*
import com.ferg.awfulapp.thread.AwfulThread.FORUM_ID
import com.ferg.awfulapp.thread.AwfulThread.INDEX
import org.jsoup.nodes.Element
import java.util.concurrent.*
import java.util.regex.Pattern

/**
 * Created by baka kaba on 10/11/2017.
 *
 * A Callable that parses post data from an [Element] and returns it as a [ContentValues], as defined
 * in [AwfulPost].
 *
 * This is meant to be used with a multithreaded executor, so you can parse posts in parallel and
 * speed up page loads.
 */

private val parseTaskExecutor: ExecutorService by lazy { Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()) }

@Throws(Exception::class)
fun <T> parseSingleThreaded(parseTasks: Collection<Callable<T>>) = parseTasks.map(Callable<T>::call)

@Throws(InterruptedException::class, ExecutionException::class)
fun <T> parseMultiThreaded(parseTasks: Collection<Callable<T>>) = parseTaskExecutor.invokeAll(parseTasks).map(Future<T>::get)

/**
 * Run a set of parse tasks in parallel, retrying on the current thread if there's a failure.
 *
 * This function blocks until all results are available.
 */
fun <T> parse(parseTasks: Collection<Callable<T>>): List<T> {
    try {
        return parseMultiThreaded(parseTasks)
    } catch (e: InterruptedException) {
        Log.w(TAG, "parse: parallel parse failed - attempting on main thread", e)
    } catch (e: ExecutionException) {
        Log.w(TAG, "parse: parallel parse failed - attempting on main thread", e)
    }

    return try {
        parseSingleThreaded(parseTasks)
    } catch (e: Exception) {
        Log.w(TAG, "parse: single-thread parse failed", e)
        emptyList()
    }
}


private val USER_ID_REGEX = Pattern.compile("userid=(\\d+)")
private val POST_ID_GARBAGE = "\\D".toRegex()
private val POST_TIMESTAMP_GARBAGE = "[^\\w\\s:,]".toRegex()
private const val TAG = "KotlinPostParseTask"

/**
 *
 * @param[postData]         an Element containing a post structure
 * @param[updateTime]       a parsing timestamp, which should be the same for each parsing task in a page load
 * @param[index]            the index of this post in the thread
 * @param[lastReadIndex]    the index of the last-read post, used to mark this post as seen or unseen
 * @param[threadId]         the ID of this post's thread
 * @param[opId]             the user ID of the person who created the thread
 * @param[preview]          true if this post is from a post preview page, false for a normal thread view
 * @returns the post data represented as a ContentValues (see [AwfulPost])
 */
class PostParseTaskKt constructor(
        private val postData: Element,
        private val updateTime: String,
        private val index: Int,
        private val lastReadIndex: Int,
        private val threadId: Int,
        private val opId: Int,
        private val preview: Boolean,
        private val prefs: AwfulPreferences
) : Callable<ContentValues> {

    @Throws(Exception::class)
    override fun call(): ContentValues {
        return ContentValues().apply {
            //timestamp for DB trimming after a week
            put(DatabaseHelper.UPDATED_TIMESTAMP, updateTime)
            put(THREAD_ID, threadId)

            if (!preview) {
                //post id is formatted "post1234567", so we strip out the "post" prefix.
                put(ID, postData.id().replace(POST_ID_GARBAGE, "").toInt())
                //we calculate this beforehand, but now can pull this from the post (thanks cooch!)
                //wait actually no, FYAD doesn't support this. ~FYAD Privilege~
                put(POST_INDEX, postData.attr("data-idx").replace(POST_ID_GARBAGE, "").toIntOrNull() ?: index)
            }

            // Check for "class=seenX", or just rely on unread index
            val markedSeen = postData.selectFirst("[class^=seen]") != null
            val postHasBeenRead = markedSeen || index <= lastReadIndex
            put(PREVIOUSLY_READ, postHasBeenRead.sqlBool)

            put(USERNAME, textForClass("author"))
            put(REGDATE, textForClass("registered"))
            put(IS_PLAT, postData.hasDescendantWithClass("platinum").sqlBool)
            put(IS_MOD, postData.hasDescendantWithClass("role-mod").sqlBool)
            put(IS_ADMIN, postData.hasDescendantWithClass("role-admin").sqlBool)

            // grab the custom title, and also the avatar if there is one
            postData.selectFirst(".title")!!
                    .also { put(AVATAR_TEXT, it.text()) }
                    .selectFirst("img")
                    ?.let {
                        tryConvertToHttps(it)
                        put(AVATAR, it.attr("src"))
                    }

            // FYAD has its post contents inside the .complete_shit element, so we just grab that instead of the full .postbody
            val postBody = postData.selectFirst(".postbody")
            val fyadPostBody = postBody!!.selectFirst(".complete_shit")
            (fyadPostBody ?: postBody).also {
                convertVideos(it, prefs.inlineYoutube)
                it.getElementsByTag("img").forEach { processPostImage(it, postHasBeenRead, prefs) }
                it.getElementsByTag("a").forEach { tryConvertToHttps(it) }
                put(CONTENT, it.html())
            }

            // extract and clean up post timestamp
            NetworkUtils.unencodeHtml(textForClass("postdate"))
                    .replace(POST_TIMESTAMP_GARBAGE, "").trim()
                    .let { put(DATE, it) }


            // parse user ID - fall back to the profile link if necessary
            var userId = postData.getElementsByClass("userinfo")
                    .flatMap(Element::classNames)
                    .map { it.substringAfter("userid-", "") }
                    .firstOrNull(String::isNotEmpty)
                    ?.toInt()

            if (userId == null) {
                postData.selectFirst(".profilelinks [href*='userid=']")?.let {
                    with(USER_ID_REGEX.matcher(it.attr("href"))) {
                        if (find()) {
                            userId = group(1).toInt()
                        }
                    }
                }
            }

            if (userId != null) {
                put(USER_ID, userId)
                put(IS_OP, (opId == userId).sqlBool)
            } else {
                // TODO: 10/11/2017 better error handling? This is sort of a deal-breaker
                Log.w(TAG, "Failed to parse UID!")
            }

            postData.getElementsByClass("editedBy")
                    .mapNotNull { it.children().first() }
                    .firstOrNull()
                    ?.let { put(EDITED, "<i>${ it.text() }</i>") }

            put(EDITABLE, postData.getElementsByAttributeValue("alt", "Edit").isNotEmpty().sqlBool)
        }
    }

    private val Boolean.sqlBool: Int
        get() = if (this) 1 else 0

    private fun textForClass(cssClass: String): String =
            postData.selectFirst(".$cssClass")?.text() ?: "data missing"

    private fun Element.hasDescendantWithClass(cssClass: String): Boolean =
            this.selectFirst(".$cssClass") != null
}


private val THREAD_URL_ID_REGEX = Pattern.compile("([^#]+)#(\\d+)$")

class ForumParseTask(
        private val threadElement: Element,
        private val forumId: Int,
        private val startIndex: Int,
        private val username: String,
        private val parseTimestamp: String
) : Callable<ContentValues> {

    override fun call(): ContentValues {
        // start building thread data
        val awfulThread = AwfulThread()
        with(awfulThread) {
            id = threadElement.id().replace("\\D".toRegex(), "").toInt()
            index = startIndex
            forumId = this@ForumParseTask.forumId

            threadElement.selectFirst(".thread_title")?.let { title = it.text() }
            threadElement.selectFirst(".author")?.let {
                author = it.text()
                it.selectFirst("a[href*='userid']")
                        ?.attr("href")
                        ?.let { Uri.parse(it).getQueryParameter("userid") }
                        ?.let { authorId = it.toInt() }
            }
            canOpenClose = author == username

            lastPoster = threadElement.selectFirst(".lastpost .author").text()
            isLocked = threadElement.hasClass("closed")
            isSticky = threadElement.selectFirst(".title_sticky") != null

            // optional thread rating
            rating = threadElement.selectFirst(".rating img")
                    ?.let { AwfulRatings.getId(it.attr("src")) }
                    ?: AwfulRatings.NO_RATING

            // main thread tag
            threadElement.selectFirst(".icon img")?.let {
                with(THREAD_URL_ID_REGEX.matcher(it.attr("src"))) {
                    if (find()) {
                        tagUrl = group(1)
                        category = group(2).toInt()
                        with(AwfulEmote.fileName_regex.matcher(tagUrl)) {
                            if (find()) {
                                tagCacheFile = group(1)
                            }
                        }
                    } else {
                        category = 0
                    }
                }
            }

            // secondary thread tag (e.g. Ask/Tell type)
            tagExtra = threadElement.selectFirst(".icon2 img")
                    ?.let { ExtraTags.getId(it.attr("src")) }
                    ?: ExtraTags.NO_TAG


            // replies / postcount
            // this represents the number of replies, but the actual postcount includes OP
            threadElement.selectFirst(".replies")?.let { postCount = it.text().toInt() + 1 }

            // unread count / viewed status
            unreadCount = threadElement.selectFirst(".count")?.text()?.toInt() ?: 0
            // If there are X's then the user has viewed the thread
            hasBeenViewed = unreadCount > 0 || threadElement.selectFirst(".x") != null

            // Bookmarks can only be detected now by the presence of a "bmX" class - no star image
            val star = threadElement.selectFirst(".star")
            bookmarkType = when {
                star.hasClass("bm0") -> 1
                star.hasClass("bm1") -> 2
                star.hasClass("bm2") -> 3
                else -> 0
            }
        }
        // finally create and add the parsed thread
        // TODO: 04/06/2017 handle this in the database classes
        return awfulThread.toContentValues().apply {
            put(DatabaseHelper.UPDATED_TIMESTAMP, parseTimestamp)
            // don't update these values if we are loading bookmarks, or it will overwrite the cached forum results.
            if (forumId == Constants.USERCP_ID) {
                remove(INDEX)
                remove(FORUM_ID)
            }
        }
    }

}
