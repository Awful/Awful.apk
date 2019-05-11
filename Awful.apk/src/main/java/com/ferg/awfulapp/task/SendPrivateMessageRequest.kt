package com.ferg.awfulapp.task

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.widget.Toast
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.provider.AwfulProvider
import com.ferg.awfulapp.thread.AwfulMessage
import org.jsoup.nodes.Document

/**
 * Submit a Private Message.
 *
 * This takes the ID of a private message draft stored in the database, and uses that data to
 * submit the message.
 */
class SendPrivateMessageRequest(context: Context, private val pmId: Int)
    : AwfulRequest<Void?>(context, FUNCTION_PRIVATE_MESSAGE, isPostRequest = true) {

    init {
        // TODO: do this extraction elsewhere, handle failure there, just pass in valid data
        // TODO: pmId is being used as a draft ID AND the ID of a PM you're replying to ("prevmessageid")??? what's that about
        // try and extract a stored draft with the given ID
        val uri = ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, pmId.toLong())
        val storedDraft = contentResolver.query(uri, AwfulProvider.DraftProjection, null, null, null)
        var failed = true

        storedDraft?.takeIf(Cursor::moveToFirst)?.apply {
            with(parameters) {
                add(PARAM_ACTION, ACTION_DOSEND)
                add(DESTINATION_TOUSER, getString(getColumnIndex(AwfulMessage.RECIPIENT)))
                add(PARAM_TITLE, getString(getColumnIndex(AwfulMessage.TITLE)).run(NetworkUtils::encodeHtml))
                if (pmId > 0) add("prevmessageid", pmId.toString())
                add(PARAM_PARSEURL, YES)
                add("savecopy", YES)
                add("iconid", getString(getColumnIndex(AwfulMessage.REPLY_ICON)))
                add(PARAM_MESSAGE, getString(getColumnIndex(AwfulMessage.REPLY_CONTENT)).run(NetworkUtils::encodeHtml))
            }
            failed = false
        }
        storedDraft?.close()
        if (failed) Toast.makeText(context, "Unable to send private message!", Toast.LENGTH_LONG).show()
    }

    override fun handleResponse(doc: Document): Void? {
        contentResolver.delete(AwfulMessage.CONTENT_URI, "${AwfulMessage.ID}=?", arrayOf(pmId.toString()))
        return null
    }

}
