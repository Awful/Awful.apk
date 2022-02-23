package com.ferg.awfulapp.thread

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.AwfulProvider
import com.ferg.awfulapp.provider.DatabaseHelper
import com.ferg.awfulapp.thread.AwfulPost.*
import com.ferg.awfulapp.thread.AwfulThread.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber
import java.util.concurrent.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by baka kaba on 10/11/2017.
 *
 * Handles parsing forums html to extract data for posts, threads etc.
 *
 * Different types of parsing (posts on a thread page, threads on a forum page etc.) have their own
 * tasks - you can either #call these yourself, or use one of the #parse functions to run the tasks
 * on the dedicated parsing threads. Generally you should call [parse] which attempts to run tasks
 * in parallel if possible, and handles errors and fallback to running on the calling thread.
 */

private val parseTaskExecutor: ExecutorService by lazy { Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()) }

@Throws(Exception::class)
fun <T> parseSingleThreaded(parseTasks: Collection<Callable<T>>) = parseTasks.map(Callable<T>::call)

@Throws(InterruptedException::class, ExecutionException::class)
fun <T> parseMultiThreaded(parseTasks: Collection<Callable<T>>) =
    parseTaskExecutor.invokeAll(parseTasks).map(Future<T>::get)

/**
 * Run a set of parse tasks in parallel, retrying on the current thread if there's a failure.
 *
 * This function blocks until all results are available.
 */
fun <T> parse(parseTasks: Collection<Callable<T>>): List<T> {
    try {
        return parseMultiThreaded(parseTasks)
    } catch (e: InterruptedException) {
        Timber.w(e, "parse: parallel parse failed - attempting on main thread")
    } catch (e: ExecutionException) {
        Timber.w(e, "parse: parallel parse failed - attempting on main thread")
    }

    return try {
        parseSingleThreaded(parseTasks)
    } catch (e: Exception) {
        Timber.w(e, "parse: single-thread parse failed")
        emptyList()
    }
}


/**
 * A task that parses data from a post on a thread page, and returns it as a [ContentValues],
 * as defined in [AwfulPost].
 *
 * The Element you need to pass in is the one that wraps a single post's content, and has either
 * an _id_ attribute of "postXXXXXX", or a _data-idx_ attribute of "XXXXX". Currently (as of 5/1/18)
 * this is a _table_.
 *
 * Don't use this for parsing previews! Use [PostPreviewParseTask] instead.
 *
 * @param[postData]         an Element containing a post structure
 * @param[updateTime]       a parsing timestamp, which should be the same for each parsing task in a page load
 * @param[index]            the index of this post in the thread
 * @param[lastReadIndex]    the index of the last-read post, used to mark this post as seen or unseen
 * @param[threadId]         the ID of this post's thread
 * @param[opId]             the user ID of the person who created the thread
 * @returns the post data represented as a ContentValues (see [AwfulPost])
 */
class PostParseTask(
    private val postData: Element,
    private val updateTime: String,
    private val index: Int,
    private val lastReadIndex: Int,
    private val threadId: Int,
    private val opId: Int,
    private val prefs: AwfulPreferences
) : Callable<ContentValues> {

    companion object {
        private val USER_ID_REGEX = Pattern.compile("userid=(\\d+)")
        private val POST_ID_GARBAGE = "\\D".toRegex()
        private val POST_TIMESTAMP_GARBAGE = "[^\\w\\s:,]".toRegex()
    }

    @Throws(Exception::class)
    override fun call(): ContentValues {
        return ContentValues().apply {
            //timestamp for DB trimming after a week
            put(DatabaseHelper.UPDATED_TIMESTAMP, updateTime)
            put(THREAD_ID, threadId)

            //post id is formatted "post1234567", so we strip out the "post" prefix.
            put(AwfulPost.ID, postData.id().replace(POST_ID_GARBAGE, "").toInt())
            //we calculate this beforehand, but now can pull this from the post (thanks cooch!)
            //wait actually no, FYAD doesn't support this. ~FYAD Privilege~
            put(
                POST_INDEX,
                postData.attr("data-idx").replace(POST_ID_GARBAGE, "").toIntOrNull() ?: index
            )

            // Check for "class=seenX", or just rely on unread index
            val markedSeen = postData.selectFirst("[class^=seen]") != null
            val postHasBeenRead = markedSeen || index <= lastReadIndex
            put(PREVIOUSLY_READ, postHasBeenRead.sqlBool)

            put(USERNAME, textForClass("author"))
            put(REGDATE, textForClass("registered"))
            put(IS_PLAT, postData.hasDescendantWithClass("platinum").sqlBool)
            put(ROLE, getRole())

            // grab the custom title, and also avatar and alternate avatar if there are any
            postData.selectFirst(".title")!!
                .also { put(AVATAR_TEXT, it.text()) }
                .select("img")
                .take(2)
                .forEachIndexed { index, image ->
                    image.let {
                        tryConvertToHttps(it)
                        put(
                            if (index == 0) { AVATAR } else { AVATAR_SECOND },
                            it.attr("src")
                        )
                    }
                }

            // FYAD has its post contents inside the .complete_shit element, so we just grab that instead of the full .postbody
            val postBody = postData.selectFirst(".postbody")
            val fyadPostBody = postBody!!.selectFirst(".complete_shit")
            (fyadPostBody ?: postBody).apply {
                convertVideos(this, prefs.inlineYoutube)
                getElementsByTag("img").forEach { processPostImage(it, postHasBeenRead, prefs) }
                getElementsByTag("a").forEach(::tryConvertToHttps)
                if (this == fyadPostBody) {
                    // FYAD sigs are currently a sibling div alongside .complete_shit, so we need to stick them at the end of the content
                    postBody.selectFirst("> .signature")?.appendTo(this)
                }
                put(CONTENT, html())
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
                Timber.w("Failed to parse UID!")
            }

            postData.getElementsByClass("editedBy")
                .mapNotNull { it.children().first() }
                .firstOrNull()
                ?.let { put(EDITED, "<i>${it.text()}</i>") }

            put(EDITABLE, postData.getElementsByAttributeValue("alt", "Edit").isNotEmpty().sqlBool)
        }
    }

    private val Boolean.sqlBool: Int
        get() = if (this) 1 else 0

    private fun textForClass(cssClass: String): String =
        postData.selectFirst(".$cssClass")?.text() ?: "data missing"

    private fun getRole(): String =
        postData.selectFirst(".author")?.classNames()?.find { it.startsWith("role-") }?.substring(5) ?: ""

    private fun Element.hasDescendantWithClass(cssClass: String): Boolean =
        this.selectFirst(".$cssClass") != null
}


/**
 * A task that parses the post preview HTML from a preview page.
 *
 * @param previewPage the page provided by the site when you request a preview
 * @returns the HTML content of the preview
 */
class PostPreviewParseTask(private val previewPage: Document) : Callable<String> {
    override fun call() =
        previewPage.selectFirst(".standard > .postbody")?.html() ?: "Preview error!"
}


/**
 * A task that parses thread data from an item in a thread list, and returns it as a [ContentValues],
 * as defined in [AwfulThread].
 *
 * The Element you need to pass in is the one that wraps a single thread entry in the list, and has
 * an _id_ attribute of "threadXXXXXX". Currently (as of 5/1/18) this is a _tr_.
 *
 * @param threadElement an Element containing a a thread list entry
 * @param forumId the ID of the forum this thread is in
 * @param threadIndex the index of this thread in the thread list
 * @param username the user's username
 * @param parseTimestamp the timestamp for this data (generally you want the same for a set of tasks)
 * @returns the post data represented as a ContentValues (see [AwfulThread])
 */
class ForumParseTask(
    private val threadElement: Element,
    private val forumId: Int,
    private val threadIndex: Int,
    private val username: String,
    private val parseTimestamp: String
) : Callable<ContentValues> {

    companion object {
        private val THREAD_URL_ID_REGEX = Pattern.compile("([^#]+)#(\\d+)$")
    }

    override fun call(): ContentValues {
        // start building thread data
        val awfulThread = AwfulThread()
        with(awfulThread) {
            id = threadElement.id().replace("\\D".toRegex(), "").toInt()
            index = threadIndex
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

            lastPoster = threadElement.selectFirst(".lastpost .author")!!.text()
            isLocked = threadElement.hasClass("closed")
            isSticky = threadElement.selectFirst(".title_sticky") != null

            // optional thread rating
            rating = threadElement.selectFirst(".rating img")
                ?.let { AwfulRatings.getId(it.attr("src")) } ?: AwfulRatings.NO_RATING

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
                ?.let { ExtraTags.getId(it.attr("src")) } ?: ExtraTags.NO_TAG


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
                star!!.hasClass("bm0") -> 1
                star.hasClass("bm1") -> 2
                star.hasClass("bm2") -> 3
                star.hasClass("bm3") -> 4
                star.hasClass("bm4") -> 5
                star.hasClass("bm5") -> 6
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


/**
 * A task that parses thread data from a thread page and returns it as a [ContentValues],
 * as defined in [AwfulThread].
 *
 * @param resolver used to load current data for this thread
 * @param page a Document representing a page from a thread
 * @param threadId the ID of the thread this page is from
 * @param pageNumber this page's number in the thread when it was fetched
 * @param postsPerPage the posts-per-page setting used while fetching this page
 * @returns new or updated data for this thread, represented as a ContentValues (see [AwfulThread])
 */
class ThreadPageParseTask(
        private val resolver: ContentResolver,
        private val page: Document,
        private val threadId: Int,
        private val pageNumber: Int,
        private val lastPageNumber: Int,
        private val postsPerPage: Int,
        private val prefs: AwfulPreferences
) : Callable<ContentValues> {

    companion object {
        val FORUM_ID_REGEX: Pattern = Pattern.compile("forumid=(\\d+)")
    }

    override fun call(): ContentValues {
        // try and load the current thread data from the DB, otherwise create a new AwfulThread
        val uri = ContentUris.withAppendedId(AwfulThread.CONTENT_URI, threadId.toLong())
        val thread = resolver.query(uri, AwfulProvider.ThreadProjection, null, null, null).use {
            it?.apply { moveToFirst() }?.let(::fromCursorRow) ?: AwfulThread()
        }

        with(thread) {
            id = threadId
            title = page.selectFirst(".bclast")?.text() ?: "UNKNOWN TITLE"
            // look for a real reply button - if there isn't one, this thread is locked
            isLocked = page.selectFirst("[alt=Reply]:not([src*='forum-closed'])") == null
            canOpenClose = page.selectFirst("[alt='Close thread'],[alt='Open thread']") != null

            val bookmarkButton = page.selectFirst(".thread_bookmark")
            archived = bookmarkButton == null
            val bookmarked =
                bookmarkButton != null && bookmarkButton.attr("src").contains("unbookmark")
            if (!bookmarked) {
                bookmarkType = 0
            } else if (bookmarkType == 0) {
                // so if the thread IS bookmarked, check if the data has an 'unbookmarked' value (i.e. 0) and give it a bookmarked one if necessary
                bookmarkType = 1
            }
            // The breadcrumbs display the forum hiearchy, from the top level down through forums and subforums to the thread.
            // So the thread's parent forum is the last forum element in that sequence
            forumId = page.selectFirst(".breadcrumbs")
                ?.select("[href]")
                ?.map { FORUM_ID_REGEX.matcher(it.attr("href")) }
                ?.lastOrNull(Matcher::find)
                ?.group(1)?.toInt() ?: -1


            // now calculate some read/unread numbers based on what we can see on the page
            val firstPostOnPageIndex = AwfulPagedItem.pageToIndex(pageNumber, postsPerPage, 0)
            val firstUnreadIndex = if (!hasBeenViewed) 0 else postCount - unreadCount

            // hand off the page for post parsing, and get back the number of posts it found
            // TODO: 02/06/2017 sort out the ignored posts issue, the post parser doesn't put them in the DB (if you have 'always hide' on in the settings) and it messes up the numbers
            val postsOnThisPage = syncPosts(
                resolver,
                page,
                threadId,
                firstUnreadIndex,
                authorId,
                prefs,
                firstPostOnPageIndex
            )
            val postsOnPreviousPages = (pageNumber - 1) * postsPerPage
            // calculate the read total by counting posts on this + preceding pages - only update the read count if it has grown (e.g. going back to an old page will give a lower count)
            val postsRead = (postsOnPreviousPages + postsOnThisPage).coerceAtLeast(readCount)

            postCount = if (pageNumber == lastPageNumber) {
                // this is the last page, so we've read all the posts in the thread
                postsRead
            } else {
                // not the last page, so we can't tell how many posts the thread has, we have to estimate it
                // we can calculate a minimum and maximum posts range by looking at the last page number
                val minPosts =
                    (lastPageNumber - 1) * postsPerPage + 1   // one post on the last page, any preceding pages are full
                val maxPosts = lastPageNumber * postsPerPage             // all pages full
                // if the old post count is within this range, let's just assume it's more accurate than taking the minimum
                // if it's outside of that range it's obviously a stale value, use the min as our best guess
                if (postCount in minPosts..maxPosts) postCount else minPosts
            }
            // TODO: 16/06/2017 would it be better to store postCount and postsRead in the DB, and calculate the unread count from that?
            unreadCount = postCount - postsRead

            Timber.d(
                "getThreadPosts: Thread ID %d, page %d of %d, %d posts on page%n%d posts total: %d read/%d unread",
                id, pageNumber, lastPageNumber, postsOnThisPage, postCount, postsRead, unreadCount
            )

        }
        return thread.toContentValues()
    }
}

