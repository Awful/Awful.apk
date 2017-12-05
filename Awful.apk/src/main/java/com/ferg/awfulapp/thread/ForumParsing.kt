package com.ferg.awfulapp.thread

import android.content.ContentValues
import android.util.Log
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.DatabaseHelper
import com.ferg.awfulapp.thread.AwfulPost.*
import org.jsoup.nodes.Element
import java.util.concurrent.Callable
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
                put(ID, Integer.parseInt(postData.id().replace(POST_ID_GARBAGE, "")))
                //we calculate this beforehand, but now can pull this from the post (thanks cooch!)
                //wait actually no, FYAD doesn't support this. ~FYAD Privilege~
                try {
                    put(POST_INDEX, Integer.parseInt(postData.attr("data-idx").replace(POST_ID_GARBAGE, "")))
                } catch (nfe: NumberFormatException) {
                    put(POST_INDEX, index)
                }
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
                    .flatMap { it.classNames() }
                    .map { it.substringAfter("userid-", "") }
                    .firstOrNull { it.isNotEmpty() }
                    ?.toInt()

            if (userId == null) {
                postData.selectFirst(".profilelinks [href*='userid=']")
                        ?.let {
                            val matcher = USER_ID_REGEX.matcher(it.attr("href"))
                            if (matcher.find()) {
                                userId = Integer.parseInt(matcher.group(1))
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
