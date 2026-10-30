package com.ferg.awfulapp.task

import android.content.ContentValues
import android.content.Context
import com.ferg.awfulapp.constants.Constants.FUNCTION_POST_REPLY
import com.ferg.awfulapp.constants.Constants.PARAM_ACTION
import com.ferg.awfulapp.constants.Constants.PARAM_ATTACHMENT
import com.ferg.awfulapp.constants.Constants.PARAM_BOOKMARK
import com.ferg.awfulapp.constants.Constants.PARAM_FORMKEY
import com.ferg.awfulapp.constants.Constants.PARAM_FORM_COOKIE
import com.ferg.awfulapp.constants.Constants.PARAM_MESSAGE
import com.ferg.awfulapp.constants.Constants.PARAM_PARSEURL
import com.ferg.awfulapp.constants.Constants.PARAM_THREAD_ID
import com.ferg.awfulapp.constants.Constants.YES
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.thread.AwfulPost
import org.jsoup.nodes.Document

/**
 * Submit a post described by a set of ContentValues
 */
class SendPostRequest(context: Context, reply: ContentValues)
    : AwfulRequest<Void?>(context, FUNCTION_POST_REPLY, isPostRequest = true) {

    init {
        with(parameters) {
            add(PARAM_ACTION, "postreply")
            add(PARAM_THREAD_ID, reply.getAsInteger(AwfulMessage.ID).toString())
            add(PARAM_FORMKEY, reply.getAsString(AwfulPost.FORM_KEY))
            add(PARAM_FORM_COOKIE, reply.getAsString(AwfulPost.FORM_COOKIE))
            add(PARAM_MESSAGE, NetworkUtils.encodeHtml(reply.getAsString(AwfulMessage.REPLY_CONTENT)))
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
