package com.ferg.awfulapp.task

import android.content.ContentValues
import android.content.Context
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.provider.DatabaseHelper
import com.ferg.awfulapp.reply.Reply
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document
import java.sql.Timestamp

/**
 * Request the data you get by quoting a post on the main site.
 *
 * This provides you with the text and BBCode added to the reply composer,
 * the form key and cookie (for authentication?) as well as any selected
 * options (see [Reply.processQuote]) and a current timestamp.
 */
class QuoteRequest(context: Context, private val threadId: Int, private val postId: Int)
    : AwfulRequest<ContentValues>(context, FUNCTION_POST_REPLY) {

    init {
        with(parameters) {
            add(PARAM_ACTION, "newreply")
            add(PARAM_POST_ID, postId.toString())
        }
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): ContentValues {
        return Reply.processQuote(doc, threadId, postId).apply {
            put(DatabaseHelper.UPDATED_TIMESTAMP, Timestamp(System.currentTimeMillis()).toString())
        }
    }

}
