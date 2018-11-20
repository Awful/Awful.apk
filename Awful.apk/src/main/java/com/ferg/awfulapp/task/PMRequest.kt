package com.ferg.awfulapp.task

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.constants.Constants.FUNCTION_PRIVATE_MESSAGE
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

/**
 * A request that fetches a Private Message by its [id], and parses and stores
 * its data in the database.
 */
class PMRequest(context: Context, private val id: Int) : AwfulRequest<Void?>(context, FUNCTION_PRIVATE_MESSAGE) {

    override fun generateUrl(urlBuilder: Uri.Builder?): String = with (urlBuilder!!) {
        appendQueryParameter(Constants.PARAM_ACTION, "show")
        appendQueryParameter(Constants.PARAM_PRIVATE_MESSAGE_ID, id.toString())
        build().toString()
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? {
        with (contentResolver) {
            val message = AwfulMessage.processMessage(doc, id)
            val messageUri = ContentUris.withAppendedId(AwfulMessage.CONTENT_URI, id.toLong())
            if (update(messageUri, message, null, null) < 1) {
                insert(AwfulMessage.CONTENT_URI, message)
            }
        }
        return null
    }

}
