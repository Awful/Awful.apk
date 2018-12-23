package com.ferg.awfulapp.task

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import com.android.volley.VolleyError
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.thread.AwfulPost
import com.ferg.awfulapp.thread.AwfulThread
import com.ferg.awfulapp.util.AwfulError
import com.ferg.awfulapp.util.toSqlBoolean
import org.jsoup.nodes.Document

/**
 * A request to mark a thread as unread.
 */
class MarkUnreadRequest(context: Context, private val threadId: Int)
    : AwfulRequest<Void?>(context, FUNCTION_THREAD, isPostRequest = true) {
    init {
        with(parameters) {
            add(PARAM_THREAD_ID, threadId.toString())
            add(PARAM_ACTION, "resetseen")
        }
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? {
        with (contentResolver) {
            // set all posts in the thread as unread
            val unreadPost = ContentValues().apply { put(AwfulPost.PREVIOUSLY_READ, false.toSqlBoolean) }
            update(AwfulPost.CONTENT_URI, unreadPost, AwfulPost.THREAD_ID + "=?", arrayOf(threadId.toString()))

            // update the thread data to reflect an unread state
            val unreadThread = ContentValues().apply {
                put(AwfulThread.UNREADCOUNT, 0)
                put(AwfulThread.HAS_VIEWED_THREAD, false.toSqlBoolean)
            }
            update(ContentUris.withAppendedId(AwfulThread.CONTENT_URI, threadId.toLong()), unreadThread, null, null)
        }
        return null
    }


    override fun customizeProgressListenerError(error: VolleyError) = AwfulError("Failed to mark unread!")
}
