package com.ferg.awfulapp.task

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.thread.AwfulMessage
import org.jsoup.nodes.Document

/**
 * A request that fetches and stores data for a reply to a Private Message.
 *
 * The request takes the [id] of the message being replied to, fetches the reply page
 * from the site, and parses the username, title and reply contents from that page.
 * This is stored in the database as a message draft, unless a draft for a reply to
 * this PM already exists, in which case the title and reply contents are unchanged.
 */
class PMReplyRequest(context: Context, private val id: Int)
    : AwfulRequest<Void?>(context, FUNCTION_PRIVATE_MESSAGE) {

    init {
        with(parameters) {
            add(PARAM_ACTION, "newmessage")
            add(PARAM_PRIVATE_MESSAGE_ID, id.toString())
        }
    }

    override fun handleResponse(doc: Document): Void? {
        // parse the reply data, and create a version that doesn't overwrite the title or reply content,
        // for updating any existing draft
        val newReply = AwfulMessage.processReplyMessage(doc, id)
        val updateDraft = ContentValues(newReply).apply {
            remove(AwfulMessage.TITLE)
            remove(AwfulMessage.REPLY_CONTENT)
        }

        // try and update the draft - if it fails, insert the full reply data
        // (including title and message content) as a new draft
        val currentDraft = ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, id.toLong())
        if (contentResolver.update(currentDraft, updateDraft, null, null) < 1) {
            // no update so the draft didn't exist
            contentResolver.insert(AwfulMessage.CONTENT_URI_REPLY, newReply)
        }
        return null
    }

}
