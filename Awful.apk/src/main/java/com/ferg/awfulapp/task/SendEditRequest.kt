package com.ferg.awfulapp.task

import android.content.ContentValues
import android.content.Context
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.thread.AwfulPost
import org.jsoup.nodes.Document

/**
 * Submit a post edit described by a set of ContentValues
 */
class SendEditRequest(context: Context, reply: ContentValues)
    : AwfulRequest<Void?>(context, FUNCTION_EDIT_POST, isPostRequest = true) {
    // TODO: again this is v similar to the Edit/PostPreview requests - better to merge them?
    // TODO: it's also probably neater to ditch the ContentValues and just use normal params instead of all this wrangling
    init {
        with(parameters) {
            add(PARAM_ACTION, "updatepost")
            add(PARAM_POST_ID, reply.getAsString(AwfulPost.EDIT_POST_ID))
            add(PARAM_MESSAGE, reply.getAsString(AwfulMessage.REPLY_CONTENT).run(NetworkUtils::encodeHtml))
            add(PARAM_PARSEURL, YES)
            if (reply.getAsString(AwfulPost.FORM_BOOKMARK).equals("checked", ignoreCase = true)) {
                add(PARAM_BOOKMARK, YES)
            }
            listOf(AwfulMessage.REPLY_SIGNATURE, AwfulMessage.REPLY_DISABLE_SMILIES)
                    .forEach { key -> if (reply.containsKey(key)) add(key, YES) }
            reply.getAsString(AwfulMessage.REPLY_ATTACHMENT)?.let { filePath ->
                attachFile(PARAM_ATTACHMENT, filePath)
            }
        }
    }

    override fun handleResponse(doc: Document): Void? = null

}
