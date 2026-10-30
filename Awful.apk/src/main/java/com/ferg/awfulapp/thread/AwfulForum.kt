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
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.forums.ForumRepository.Companion.getInstance
import com.ferg.awfulapp.provider.AwfulProvider.Companion.int2StrArray
import com.ferg.awfulapp.provider.DatabaseHelper
import org.jsoup.nodes.Document
import java.sql.Timestamp
import java.util.regex.Pattern

object AwfulForum : AwfulPagedItem() {
    const val PATH: String = "/forum"
    val CONTENT_URI: Uri = "content://${Constants.AUTHORITY}$PATH".toUri()
    const val ID: String = "_id"
    const val PARENT_ID: String = "parent_forum_id"
    const val INDEX: String = "forum_index" //for ordering by
    const val TITLE: String = "title"
    const val SUBTEXT: String = "subtext"
    const val PAGE_COUNT: String = "page_count"
    const val TAG_URL: String = "tag_url"
    const val TAG_CACHEFILE: String = "tag_cachefile"
    private const val TAG = "AwfulForum"
    private val forumId_regex: Pattern = Pattern.compile("forumid=(\\d+)")

    fun processForumIcons(response: Document, contentInterface: ContentResolver) {
        val forumIcons = response.getElementById("forums")!!.getElementsByClass("icon")
        for (node in forumIcons) {
            if (node != null) {
                val forum = ContentValues()
                val imgTag = node.getElementsByTag("img").first()
                if (imgTag != null && imgTag.hasAttr("src")) {
                    var url : String? = imgTag.attr("src")
                    if (url != null) {
                        if (url.startsWith("//")) {
                            // damn you and you protocol-less image urls, ZDR
                            url = Constants.BASE_URL.substring(
                                0,
                                Constants.BASE_URL.indexOf("//")
                            ) + url
                        }
                        //thread tag stuff
                        val fileNameMatcher = AwfulEmote.fileName_regex.matcher(url)
                        if (fileNameMatcher.find()) {
                            forum.put(TAG_CACHEFILE, fileNameMatcher.group(1))
                        }
                        forum.put(TAG_URL, url)
                    }
                }
                val forumId = getForumId(node.getElementsByTag("a").first()!!.attr("href"))
                contentInterface.update(
                    ContentUris.withAppendedId(CONTENT_URI, forumId.toLong()),
                    forum,
                    null,
                    null
                )
            }
        }
    }


    /**
     * Parse a forum page, storing the thread list and updating the threads in the database.
     * @param forumId          the ID of the forum being parsed
     * @param pageNumber       the number of the page being parsed, e.g. page 2 of GBS
     * @param lastPageNumber
     * @param page             a forum page containing a list of threads
     * @param contentInterface used for database access
     */
    fun parseThreads(
        forumId: Int,
        pageNumber: Int,
        lastPageNumber: Int,
        page: Document,
        contentInterface: ContentResolver
    ) {
        // get the threads on a (normal) forum page, index them and store
        val threads = AwfulThread.parseForumThreads(page, forumId, forumPageToIndex(pageNumber))
        deletePageOfThreads(forumId, pageNumber, contentInterface)
        insertThreads(threads, contentInterface)

        // update page count for forum
        getInstance(null).setPageCount(forumId, lastPageNumber)
    }


    /**
     * Parse a Bookmarks page, storing the thread list and updating the threads in the database.
     * @param page             a page containing the user's bookmarks
     * @param pageNumber       the number of the page being parsed, e.g. page 2 of the bookmarks
     * @param lastPageNumber
     * @param contentInterface used for database access
     */
    fun parseUCPThreads(
        page: Document,
        pageNumber: Int,
        lastPageNumber: Int,
        contentInterface: ContentResolver
    ) {
        // get all the threads on the bookmarks page, with their INDEXes set appropriately, and store them
        val threads =
            AwfulThread.parseForumThreads(page, Constants.USERCP_ID, forumPageToIndex(pageNumber))
        insertThreads(threads, contentInterface)

        // for each thread on the page, create a bookmark (with the thread's ID) in the same position (same index)
        val update_time = Timestamp(System.currentTimeMillis()).toString()
        val bookmarks: MutableList<ContentValues> = mutableListOf()

        var start_index = forumPageToIndex(pageNumber)
        for (thread in threads) {
            val bookmark = ContentValues()
            bookmark.put(AwfulThread.ID, thread.getAsInteger(AwfulThread.ID))
            bookmark.put(AwfulThread.INDEX, start_index)
            bookmark.put(DatabaseHelper.UPDATED_TIMESTAMP, update_time)
            start_index++
            bookmarks.add(bookmark)
        }
        Log.i(TAG, "Parsed UCP entries: " + bookmarks.size)

        // delete all the bookmarked threads for this page, re-add the new ones in the same place (thanks to the matching indices)
        deletePageOfBookmarks(pageNumber, contentInterface)
        insertBookmarks(bookmarks, contentInterface)
        // update bookmarks forum
        getInstance(null).setPageCount(Constants.USERCP_ID, lastPageNumber)
    }


    private fun insertThreads(threads: List<ContentValues>, resolver: ContentResolver) {
        resolver.bulkInsert(AwfulThread.CONTENT_URI, threads.toTypedArray())
    }


    private fun insertBookmarks(bookmarks: List<ContentValues>, resolver: ContentResolver) {
        resolver.bulkInsert(AwfulThread.CONTENT_URI_UCP, bookmarks.toTypedArray())
    }


    private fun deletePageOfThreads(forumId: Int, pageNum: Int, resolver: ContentResolver) {
        if (forumId == Constants.USERCP_ID) {
            throw RuntimeException("This method deletes threads from forums, not the bookmarks table!")
        }
        resolver.delete(
            AwfulThread.CONTENT_URI,
            String.format(
                "%s=? AND %s>=? AND %s<?",
                AwfulThread.FORUM_ID,
                AwfulThread.INDEX,
                AwfulThread.INDEX
            ),
            int2StrArray(forumId, forumPageToIndex(pageNum), forumPageToIndex(pageNum + 1))
        )
    }


    private fun deletePageOfBookmarks(pageNum: Int, resolver: ContentResolver) {
        resolver.delete(
            AwfulThread.CONTENT_URI_UCP,
            String.format("%s>=? AND %s<?", AwfulThread.INDEX, AwfulThread.INDEX),
            int2StrArray(forumPageToIndex(pageNum), forumPageToIndex(pageNum + 1))
        )
    }


    @JvmStatic
    fun getForumId(aHref: String): Int {
        val matcher = forumId_regex.matcher(aHref)
        return if (matcher.find()) {
            matcher.group(1)!!.toInt()
        } else {
            -1
        }
    }
}
