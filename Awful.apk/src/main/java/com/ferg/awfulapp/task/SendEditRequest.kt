package com.ferg.awfulapp.task

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.thread.AwfulPost
import org.jsoup.nodes.Document

/**
 * Submit a post edit described by a set of ContentValues
 */
class SendEditRequest(context: Context, reply: ContentValues) : AwfulRequest<Void?>(context, null) {
    // TODO: again this is v similar to the Edit/PostPreview requests - better to merge them?
    // TODO: it's also probably neater to ditch the ContentValues and just use normal params instead of all this wrangling
    init {
        with(reply) {
            addPostParam(PARAM_ACTION, "updatepost")
            addPostParam(PARAM_POST_ID, getAsString(AwfulPost.EDIT_POST_ID))
            addPostParam(PARAM_MESSAGE, getAsString(AwfulMessage.REPLY_CONTENT).run(NetworkUtils::encodeHtml))
            addPostParam(PARAM_PARSEURL, YES)
            if (getAsString(AwfulPost.FORM_BOOKMARK).equals("checked", ignoreCase = true)) {
                addPostParam(PARAM_BOOKMARK, YES)
            }
            listOf(AwfulMessage.REPLY_SIGNATURE, AwfulMessage.REPLY_DISABLE_SMILIES)
                    .forEach { key -> if (containsKey(key)) addPostParam(key, YES) }
            getAsString(AwfulMessage.REPLY_ATTACHMENT)?.let { filePath -> attachFile(PARAM_ATTACHMENT, filePath) }
        }

        buildFinalRequest()
    }

    override fun generateUrl(urlBuilder: Uri.Builder?): String = FUNCTION_EDIT_POST

    override fun handleResponse(doc: Document): Void? = null

}
