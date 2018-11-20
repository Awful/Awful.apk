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
 * Submit a post described by a set of ContentValues
 */
class SendPostRequest(context: Context, reply: ContentValues) : AwfulRequest<Void?>(context, null) {
    init {
        with(reply) {
            addPostParam(PARAM_ACTION, "postreply")
            addPostParam(PARAM_THREAD_ID, Integer.toString(getAsInteger(AwfulMessage.ID)!!))
            addPostParam(PARAM_FORMKEY, getAsString(AwfulPost.FORM_KEY))
            addPostParam(PARAM_FORM_COOKIE, getAsString(AwfulPost.FORM_COOKIE))
            addPostParam(PARAM_MESSAGE, NetworkUtils.encodeHtml(getAsString(AwfulMessage.REPLY_CONTENT)))
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

    override fun generateUrl(urlBuilder: Uri.Builder?): String = FUNCTION_POST_REPLY

    override fun handleResponse(doc: Document): Void? = null

}
