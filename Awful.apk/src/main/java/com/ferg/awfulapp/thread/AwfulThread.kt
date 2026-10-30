/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the software nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferg.awfulapp.thread

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageLoader.ImageContainer
import com.android.volley.toolbox.ImageLoader.ImageListener
import com.ferg.awfulapp.AwfulFragment
import com.ferg.awfulapp.ForumDisplayFragment
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils.imageLoader
import com.ferg.awfulapp.network.NetworkUtils.unencodeHtml
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.AwfulPreferences.Companion.getInstance
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.provider.ColorProvider.Companion.getBookmarkColor
import com.ferg.awfulapp.provider.DatabaseHelper
import com.ferg.awfulapp.thread.AwfulPostIcon.Companion.getClassicIconDrawable
import com.ferg.awfulapp.thread.AwfulRatings.getDrawable
import com.ferg.awfulapp.thread.AwfulRatings.getType
import org.jsoup.nodes.Document
import timber.log.Timber.Forest.i
import timber.log.Timber.Forest.v
import timber.log.Timber.Forest.w
import java.sql.Timestamp
import java.util.Locale
import androidx.core.net.toUri

class AwfulThread : AwfulPagedItem() {
    // TODO: 04/06/2017 explicit default values, nulls where parsed data doesn't set values (i.e. never added to the ContentValues)?
    var id: Int = 0
    var index: Int = 0
    var title: String? = null

    var forumId: Int = 0

    //  public   String forumTitle;
    var author: String? = null
    var authorId: Int = 0
    var lastPoster: String? = null
    var lastPostDate: Long = 0
    var postCount: Int = 0
    var unreadCount: Int = 0

    var rating: Int = 0
    var bookmarkType: Int = 0

    var isLocked: Boolean = false
    var isSticky: Boolean = false
    var canOpenClose: Boolean = false
    var hasBeenViewed: Boolean = false
    var archived: Boolean = false

    var tagUrl: String? = null
    var tagCacheFile: String? = null
    var tagExtra: Int = 0
    var category: Int = 0


    fun toContentValues(): ContentValues {
        val cv = ContentValues().apply {
            put(ID, id)
            put(INDEX, index)
            put(FORUM_ID, forumId)
            put(TITLE, title)
            put(AUTHOR, author)
            put(AUTHOR_ID, authorId)

            put(CAN_OPEN_CLOSE, asSqlBoolean(canOpenClose))

            put(LASTPOSTER, lastPoster)
            put(LAST_POST_DATE, lastPostDate)
            put(LOCKED, asSqlBoolean(isLocked))
            put(STICKY, asSqlBoolean(isSticky))
            put(RATING, rating)
            put(TAG_URL, tagUrl)
            put(CATEGORY, category)
            put(TAG_CACHEFILE, tagCacheFile)

            put(TAG_EXTRA, tagExtra)
            put(POSTCOUNT, postCount)

            put(UNREADCOUNT, unreadCount)
            put(HAS_VIEWED_THREAD, asSqlBoolean(hasBeenViewed))
            put(BOOKMARKED, bookmarkType)
        }
        return cv
    }

    fun hasNewPosts(): Boolean {
        return unreadCount > 0
    }

    fun getPageCount(postsPerPage: Int): Int {
        return indexToPage(postCount, postsPerPage)
    }

    val readCount: Int
        get() = postCount - unreadCount


    companion object {
        const val PATH: String = "/thread"
        const val UCP_PATH: String = "/ucpthread"
        @JvmField
        val CONTENT_URI: Uri = "content://${Constants.AUTHORITY}$PATH".toUri()
        @JvmField
        val CONTENT_URI_UCP: Uri = "content://${Constants.AUTHORITY}$UCP_PATH".toUri()

        const val ID: String = "_id"
        const val INDEX: String = "thread_index"
        const val FORUM_ID: String = "forum_id"
        const val TITLE: String = "title"
        const val POSTCOUNT: String = "post_count"
        const val UNREADCOUNT: String = "unread_count"
        const val AUTHOR: String = "author"
        const val AUTHOR_ID: String = "author_id"
        const val LOCKED: String = "locked"
        const val CAN_OPEN_CLOSE: String = "can_open_close"
        const val BOOKMARKED: String = "bookmarked"
        const val STICKY: String = "sticky"
        const val CATEGORY: String = "category"
        const val LASTPOSTER: String = "killedby"
        const val LAST_POST_DATE: String = "last_post_date"
        const val FORUM_TITLE: String = "forum_title"
        const val HAS_NEW_POSTS: String = "has_new_posts"
        const val HAS_VIEWED_THREAD: String = "has_viewed_thread"
        const val ARCHIVED: String = "archived"
        const val RATING: String = "rating"
        const val TAG_URL: String = "tag_url"
        const val TAG_CACHEFILE: String = "tag_cachefile"
        const val TAG_EXTRA: String = "tag_extra"


        fun fromCursorRow(row: Cursor): AwfulThread? {
            if (row.isBeforeFirst || row.isAfterLast) {
                w("fromCursor: passed empty row")
                return null
            }
            val thread = AwfulThread()

            thread.id = row.getInt(row.getColumnIndexOrThrow(ID))
            thread.index = row.getInt(row.getColumnIndexOrThrow(INDEX))
            thread.title = row.getString(row.getColumnIndexOrThrow(TITLE))

            thread.forumId = row.getInt(row.getColumnIndexOrThrow(FORUM_ID))

            // TODO: 03/06/2017 this column name is taken from the thread projection, but is it ever used?
//        thread.forumTitle = row.getString(row.getColumnIndex(FORUM_TITLE));
            thread.author = row.getString(row.getColumnIndexOrThrow(AUTHOR))
            thread.authorId = row.getInt(row.getColumnIndexOrThrow(AUTHOR_ID))
            thread.lastPoster = row.getString(row.getColumnIndexOrThrow(LASTPOSTER))
            thread.lastPostDate = row.getLong(row.getColumnIndexOrThrow(LAST_POST_DATE))
            thread.postCount = row.getInt(row.getColumnIndexOrThrow(POSTCOUNT))
            thread.unreadCount = row.getInt(row.getColumnIndexOrThrow(UNREADCOUNT))

            thread.rating = row.getInt(row.getColumnIndexOrThrow(RATING))
            thread.bookmarkType = row.getInt(row.getColumnIndexOrThrow(BOOKMARKED))

            thread.isLocked = row.getInt(row.getColumnIndexOrThrow(LOCKED)) > 0
            thread.archived = row.getInt(row.getColumnIndexOrThrow(ARCHIVED)) > 0
            thread.isSticky = row.getInt(row.getColumnIndexOrThrow(STICKY)) > 0
            thread.canOpenClose = row.getInt(row.getColumnIndexOrThrow(CAN_OPEN_CLOSE)) > 0
            thread.hasBeenViewed = row.getInt(row.getColumnIndexOrThrow(HAS_VIEWED_THREAD)) == 1

            thread.tagUrl = row.getString(row.getColumnIndexOrThrow(TAG_URL))
            thread.tagCacheFile = row.getString(row.getColumnIndexOrThrow(TAG_CACHEFILE))
            thread.tagExtra = row.getInt(row.getColumnIndexOrThrow(TAG_EXTRA))
            thread.category = row.getInt(row.getColumnIndexOrThrow(CATEGORY))

            return thread
        }


        private fun asSqlBoolean(value: Boolean): Int {
            return if (value) 1 else 0
        }

        /**
         * Parse a list of threads in a forum and generate their metadata.
         * 
         * 
         * This doesn't write to the database, you need to do this with the returned data.
         * 
         * @param forumPage  the page to parse
         * @param forumId    the ID of the forum this page is from
         * @param startIndex the threads' positions in the forum will start from this index
         * @return the list of all the threads' metadata objects, ready for storage
         */
        fun parseForumThreads(
            forumPage: Document,
            forumId: Int,
            startIndex: Int
        ): List<ContentValues> {
            var startIndex = startIndex
            val startTime = System.currentTimeMillis()
            val update_time = Timestamp(startTime).toString()
            v("Update time: %s", update_time)
            val username = getInstance().username

            val parseTasks: MutableList<ForumParseTask> = mutableListOf()
            for (threadElement in forumPage.select("#forum .thread")) {
                if (TextUtils.isEmpty(threadElement.id())) {
                    //skip the table header
                    continue
                }
                parseTasks.add(
                    ForumParseTask(
                        threadElement,
                        forumId,
                        startIndex,
                        username!!,
                        update_time
                    )
                )
                startIndex++
            }
            val result: List<ContentValues> = parse(parseTasks)

            val averageParseTime = (System.currentTimeMillis() - startTime) / result.size.toFloat()
            i("%d threads parsed\nAverage parse time: %.3fms", result.size, averageParseTime)
            return result
        }


        /**
         * Parse a page from a thread, updating metadata and parsing the contained posts.
         * 
         * 
         * This will update the current read/unread counts, estimating the total number of posts
         * if the last recorded total is too low (by the current number of pages) and this isn't the last page
         * (meaning we only know how many full pages there are, not how many posts are on the last page).
         * Defaults to a minimum estimate, i.e. a single post on the last page.
         * 
         * 
         * Also stores/updates the rest of the thread metadata - title, locked status etc., and passes
         * the page to [AwfulPost] for parsing and syncing.
         * @param resolver     a ContentResolver used to access the database
         * @param page         the thread page's HTML document
         * @param threadId     the ID of this thread
         * @param pageNumber   which page of the thread this document represents
         * @param lastPageNumber the number of the last page in this thread
         * @param postsPerPage used to calculate post counts
         * @param prefs        a preferences instance
         * @param filterUserId if this page is for a thread filtered by user, this should be set to the user's ID, otherwise 0
         */
        fun parseThreadPage(
            resolver: ContentResolver,
            page: Document,
            threadId: Int,
            pageNumber: Int,
            lastPageNumber: Int,
            postsPerPage: Int,
            prefs: AwfulPreferences,
            filterUserId: Int
        ) {
            val startTime = System.currentTimeMillis()
            // TODO: 03/06/2017 see issue #503 on GitHub - filtering by user means the thread data gets overwritten by the pages from this new, shorter thread containing their posts
            // val BLANK_USER_ID = 0
            // TODO: 05/01/2018 this filtering on userID thing isn't actually doing anything...
            // val filteringOnUserId = filterUserId > BLANK_USER_ID

            // finally write new thread data to the database
            val cv = ThreadPageParseTask(
                resolver,
                page,
                threadId,
                pageNumber,
                lastPageNumber,
                postsPerPage,
                prefs
            ).call()
            // TODO: 04/06/2017 this should be handled in the database-management classes
            val update_time = Timestamp(startTime).toString()
            cv.put(DatabaseHelper.UPDATED_TIMESTAMP, update_time)
            if (resolver.update(
                    ContentUris.withAppendedId(CONTENT_URI, threadId.toLong()),
                    cv,
                    null,
                    null
                ) < 1
            ) {
                resolver.insert(CONTENT_URI, cv)
            }

            i("Thread parse time: %dms", System.currentTimeMillis() - startTime)
        }


        fun setDataOnThreadListItem(
            item: View,
            prefs: AwfulPreferences,
            data: Cursor,
            parent: AwfulFragment?
        ) {
            val thread: AwfulThread? = fromCursorRow(data)
            if (thread == null) {
                w("setDataOnThreadView: unable to get data for thread!")
                return
            }

            if (prefs.hiddenThreadIds!!.contains(thread.id.toString())) {
                markThreadAsHidden(item, thread, prefs.threadInfo_Tag)
                return
            }

            val resources = item.resources
            val context = item.context
            // get the forum ID for getting themed resources
            var forumId: Int? = null
            if (parent is ForumDisplayFragment) {
                forumId = parent.forumId
            }


            // highlight threads authored by the current user
            if (prefs.highlightYourThreads && prefs.userId > 0 && thread.authorId == prefs.userId) {
                item.setBackgroundColor(ColorProvider.SELF_THREAD_BACKGROUND.getColor(forumId))
            } else {
                item.setBackgroundColor(ColorProvider.BACKGROUND.getColor(forumId))
            }

            // thread title
            val title = item.findViewById<TextView>(R.id.title)
            title.text = if (thread.title != null) thread.title else "UNKNOWN"
            title.setTextColor(ColorProvider.PRIMARY_TEXT.getColor(forumId))


            // main thread tag
            val threadTag = item.findViewById<ImageView>(R.id.thread_tag)
            threadTag.visibility = View.GONE
            if (prefs.threadInfo_Tag) {
                if (!TextUtils.isEmpty(thread.tagCacheFile)) {
                    threadTag.visibility = View.VISIBLE
                    val url = thread.tagUrl!!
                    val localFileName =
                        "@drawable/" + url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'))
                            .replace('-', '_').lowercase(
                                Locale.getDefault()
                            )

                    val imageID = resources.getIdentifier(localFileName, null, context.packageName)
                    if (imageID == 0) {
                        imageLoader?.get(url, object : ImageListener {
                            override fun onResponse(
                                response: ImageContainer,
                                isImmediate: Boolean
                            ) {
                                val classicTag = if (response.bitmap == null) {
                                    getDrawable(R.drawable.empty_thread_tag, resources)
                                } else {
                                    getClassicIconDrawable(
                                        response.bitmap,
                                        threadTag.context
                                    )
                                }
                                threadTag.setImageDrawable(classicTag)

                            }

                            override fun onErrorResponse(error: VolleyError?) {
                                threadTag.setImageResource(R.drawable.empty_thread_tag)
                            }
                        })
                    } else {
                        threadTag.setImageResource(imageID)
                    }
                }
            }


            // tag overlay (secondary tags etc)
            val forumTagOverlay = item.findViewById<ImageView>(R.id.thread_tag_overlay)
            val inlineForumTagOverlay =
                item.findViewById<ImageView>(R.id.thread_tag_overlay_optional)
            forumTagOverlay.visibility = View.GONE
            inlineForumTagOverlay.visibility = View.GONE
            if (ExtraTags.getType(thread.tagExtra) != ExtraTags.TYPE_NO_TAG) {
                val tagIcon = ExtraTags.getDrawable(thread.tagExtra, resources)
                if (tagIcon != null) {
                    if (prefs.threadInfo_Tag) {
                        showImage(forumTagOverlay, tagIcon)
                    } else {
                        showImage(inlineForumTagOverlay, tagIcon)
                    }
                }
            }


            // page count / author / last poster info line
            val info = item.findViewById<TextView>(R.id.thread_info)
            info.visibility = View.VISIBLE
            val tmp = String.format(
                Locale.US, "%d pgs | %s: %s",
                thread.getPageCount(prefs.postPerPage),
                if (thread.hasBeenViewed) "Last" else "OP",
                unencodeHtml(if (thread.hasBeenViewed) thread.lastPoster else thread.author)
            )
            info.text = tmp.trim { it <= ' ' }
            info.setTextColor(ColorProvider.ALT_TEXT.getColor(forumId))


            // ratings
            val threadRating = item.findViewById<ImageView>(R.id.thread_rating)
            val inlineThreadRating = item.findViewById<ImageView>(R.id.thread_rating_optional)
            threadRating.visibility = View.GONE
            inlineThreadRating.visibility = View.GONE
            // if we're showing ratings...
            if (prefs.threadInfo_Rating) {
                val ratingIcon = getDrawable(thread.rating, resources)
                // Film Dump replaces the actual thread tag, instead of using the separate rating view
                if (getType(thread.rating) == AwfulRatings.TYPE_FILM_DUMP) {
                    showImage(threadTag, ratingIcon)
                } else {
                    showImage(
                        if (prefs.threadInfo_Tag) threadRating else inlineThreadRating,
                        ratingIcon
                    )
                }
            }


            // locked and sticky status
            val threadLocked = item.findViewById<ImageView>(R.id.thread_locked)
            val threadSticky = item.findViewById<ImageView>(R.id.thread_sticky)
            threadSticky.visibility = if (thread.isSticky) View.VISIBLE else View.GONE
            threadLocked.visibility = if (thread.isLocked && !thread.isSticky) View.VISIBLE else View.GONE

            // unread counter
            val unread = item.findViewById<TextView>(R.id.unread_count)
            unread.visibility = View.GONE
            if (thread.hasBeenViewed) {
                unread.visibility = View.VISIBLE
                unread.setTextColor(ColorProvider.UNREAD_TEXT.getColor(forumId))
                unread.text = thread.unreadCount.toString()
                val counter = resources.getDrawable(R.drawable.unread_counter) as GradientDrawable?
                if (counter != null) {
                    counter.mutate()
                    val dim = !thread.hasNewPosts()
                    if (thread.bookmarkType > 0 && prefs.coloredBookmarks) {
                        counter.setColor(getBookmarkColor(thread.bookmarkType, dim))
                    } else {
                        val colorAttr =
                            if (dim) ColorProvider.UNREAD_BACKGROUND_DIM else ColorProvider.UNREAD_BACKGROUND
                        counter.setColor(colorAttr.getColor(forumId))
                    }
                    unread.setBackgroundDrawable(counter)
                }
            }
        }

        private fun markThreadAsHidden(item: View, thread: AwfulThread, showTags: Boolean) {
            val title = item.findViewById<TextView>(R.id.title)
            title.setText(R.string.thread_hidden)
            val threadTag = item.findViewById<ImageView>(R.id.thread_tag)
            if (showTags) {
                threadTag.setImageResource(R.drawable.empty_thread_tag)
            } else {
                threadTag.visibility = View.GONE
            }
            item.findViewById<View>(R.id.thread_tag_overlay).visibility = View.GONE
            item.findViewById<View>(R.id.thread_tag_overlay_optional).visibility = View.GONE
            item.findViewById<View>(R.id.thread_info).visibility = View.INVISIBLE
            item.findViewById<View>(R.id.thread_rating).visibility = View.GONE
            item.findViewById<View>(R.id.thread_rating_optional).visibility = View.GONE
            item.findViewById<View>(R.id.thread_sticky).visibility = if (thread.isSticky) View.VISIBLE else View.GONE
            item.findViewById<View>(R.id.thread_locked).visibility = if (thread.isLocked && !thread.isSticky) View.VISIBLE else View.GONE
            item.findViewById<View>(R.id.unread_count).visibility = View.GONE
        }

        /** Utility method to set and show an imageview  */
        private fun showImage(imageView: ImageView, drawable: Drawable?) {
            imageView.visibility = View.VISIBLE
            imageView.setImageDrawable(drawable)
        }
    }
}
