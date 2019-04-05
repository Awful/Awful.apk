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
 * Request the data you get by starting a post edit on the main site.
 *
 * This provides you with the text and BBCode added to the post composer,
 * the form key and cookie (for authentication?) as well as any selected
 * options (see [Reply.processEdit]) and a current timestamp.
 */
class EditRequest(context: Context, private val threadId: Int, private val postId: Int)
    : AwfulRequest<ContentValues>(context, FUNCTION_EDIT_POST) {

    // TODO: this and the quote/reply requests are all very similar - they all just load the "start replying" page and grab any existing contents. Combine them maybe?
    init {
        with(parameters) {
            add(PARAM_ACTION, "editpost")
            add(PARAM_POST_ID, postId.toString())
        }
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): ContentValues {
        return Reply.processEdit(doc, threadId, postId).apply {
            put(DatabaseHelper.UPDATED_TIMESTAMP, Timestamp(System.currentTimeMillis()).toString())
        }
    }

}
