package com.ferg.awfulapp.task

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri

import com.android.volley.VolleyError
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.provider.AwfulProvider
import com.ferg.awfulapp.thread.AwfulPost
import com.ferg.awfulapp.thread.AwfulThread
import com.ferg.awfulapp.util.AwfulError
import com.ferg.awfulapp.util.toSqlBoolean

import org.jsoup.nodes.Document

/**
 * An AwfulRequest that sets a given post as the last one read in a particular thread, updating
 * the database to reflect this when the request is successful.
 */
class MarkLastReadRequest(context: Context, private val threadId: Int, private val postIndex: Int) : AwfulRequest<Void?>(context, null) {
    init {
        addPostParam(Constants.PARAM_ACTION, "setseen")
        addPostParam(Constants.PARAM_THREAD_ID, Integer.toString(threadId))
        addPostParam(Constants.PARAM_INDEX, Integer.toString(postIndex))
    }

    override fun generateUrl(urlBuilder: Uri.Builder?): String = Constants.FUNCTION_THREAD

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? {
        var cv = ContentValues()
        with(contentResolver) {
            fun where(greaterThan: Boolean) =
                    "${AwfulPost.THREAD_ID}=? AND ${AwfulPost.POST_INDEX} ${if (greaterThan) ">" else "<="}?"

            val params = listOf(threadId, postIndex).map(Int::toString).toTypedArray()

            // set later posts to unread, and this post (and all previous) to read
            cv.put(AwfulPost.PREVIOUSLY_READ, false.toSqlBoolean)
            update(AwfulPost.CONTENT_URI, cv, where(greaterThan = true), params)

            cv.put(AwfulPost.PREVIOUSLY_READ, true.toSqlBoolean)
            update(AwfulPost.CONTENT_URI, cv, where(greaterThan = false), params)

            // update the thread's unread count
            val threadData = query(ContentUris.withAppendedId(AwfulThread.CONTENT_URI, threadId.toLong()), AwfulProvider.ThreadProjection, null, null, null)
            threadData?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val newPostCount = cursor.getInt(cursor.getColumnIndex(AwfulThread.POSTCOUNT)) - postIndex
                    cv = ContentValues().apply { put(AwfulThread.UNREADCOUNT, newPostCount) }
                    update(AwfulThread.CONTENT_URI, cv, AwfulThread.ID + "=?", arrayOf(threadId.toString()))
                }
            }
        }
        return null
    }


    override fun customizeProgressListenerError(error: VolleyError) = AwfulError("Failed to mark post!")
}
