package com.ferg.awfulapp.task

import android.content.ContentValues
import android.content.Context
import com.ferg.awfulapp.constants.Constants.FUNCTION_POST_THREAD
import com.ferg.awfulapp.constants.Constants.PARAM_ACTION
import com.ferg.awfulapp.constants.Constants.PARAM_FORUM_ID
import com.ferg.awfulapp.provider.DatabaseHelper
import com.ferg.awfulapp.thread.Thread
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document
import java.sql.Timestamp

/**
 * Request the data you get when starting a new thread on the site.
 *
 * This provides you with any initial op contents, the form key
 * and cookie (for authentication?) as well as any selected
 * options (see [Thread.processReply]) and a current timestamp.
 */
class ThreadRequest(context: Context, private val forumId: Int)
    : AwfulRequest<ContentValues>(context, FUNCTION_POST_THREAD) {

    init {
        with(parameters) {
            add(PARAM_ACTION, "newthread")
            add(PARAM_FORUM_ID, forumId.toString())
        }
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): ContentValues {
        return Thread.processThread(doc, forumId).apply {
            put(DatabaseHelper.UPDATED_TIMESTAMP, Timestamp(System.currentTimeMillis()).toString())
        }
    }

}
