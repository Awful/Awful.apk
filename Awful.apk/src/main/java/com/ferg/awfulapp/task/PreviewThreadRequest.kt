package com.ferg.awfulapp.task

import android.content.ContentValues
import android.content.Context
import com.ferg.awfulapp.constants.Constants.FUNCTION_POST_THREAD
import com.ferg.awfulapp.constants.Constants.PARAM_ACTION
import com.ferg.awfulapp.constants.Constants.PARAM_ATTACHMENT
import com.ferg.awfulapp.constants.Constants.PARAM_BOOKMARK
import com.ferg.awfulapp.constants.Constants.PARAM_FORMKEY
import com.ferg.awfulapp.constants.Constants.PARAM_FORM_COOKIE
import com.ferg.awfulapp.constants.Constants.PARAM_FORUM_ID
import com.ferg.awfulapp.constants.Constants.PARAM_MESSAGE
import com.ferg.awfulapp.constants.Constants.PARAM_PARSEURL
import com.ferg.awfulapp.constants.Constants.PARAM_PREVIEW
import com.ferg.awfulapp.constants.Constants.PARAM_SUBJECT
import com.ferg.awfulapp.constants.Constants.PREVIEW_REPLY
import com.ferg.awfulapp.constants.Constants.YES
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.thread.AwfulPost
import com.ferg.awfulapp.thread.PostPreviewParseTask
import org.jsoup.nodes.Document

/**
 * A request that fetches a preview of a thread, providing the parsed HTML.
 *
 * This functions like the Preview button when composing a thread on the site, which
 * takes the current content in the post window and renders it as HTML. You supply
 * the data on the edit page via the ContentValues parameter, which is sent to
 * the site to retrieve the preview.
 */
class PreviewThreadRequest (context: Context, reply: ContentValues)
    : AwfulRequest<String>(context, FUNCTION_POST_THREAD, isPostRequest = true) {
// TODO: 23/05/2024 this, PreviewPostRequest and PreviewEditRequest are almost identical, oh no it's getting worse
    init {
        with(parameters) {
            val threadId = reply.getAsInteger(AwfulMessage.ID)?.toString()
                    ?: throw IllegalArgumentException("No forum ID included")
            add(PARAM_ACTION, "postthread")
            add(PARAM_FORUM_ID, reply.getAsInteger(AwfulMessage.ID).toString())
            add(PARAM_FORMKEY, reply.getAsString(AwfulPost.FORM_KEY))
            add(PARAM_FORM_COOKIE, reply.getAsString(AwfulPost.FORM_COOKIE))
            add(PARAM_SUBJECT, NetworkUtils.encodeHtml(reply.getAsString(AwfulMessage.POST_SUBJECT)))
            add(PARAM_MESSAGE, NetworkUtils.encodeHtml(reply.getAsString(AwfulMessage.POST_CONTENT)))

            add(PARAM_PARSEURL, YES)
            // TODO: this bookmarks every thread you post in, unless you turn it off in a browser - seems bad?
            if (reply.getAsString(AwfulPost.FORM_BOOKMARK).equals("checked", ignoreCase = true)) {
                add(PARAM_BOOKMARK, YES)
            }
            listOf(AwfulMessage.REPLY_SIGNATURE, AwfulMessage.REPLY_DISABLE_SMILIES)
                    .forEach { if (reply.containsKey(it)) add(it, YES) }

            reply.getAsString(AwfulMessage.REPLY_ATTACHMENT)?.let { filePath -> attachFile(PARAM_ATTACHMENT, filePath) }
            add(PARAM_PREVIEW, PREVIEW_REPLY)
        }
    }


    override fun handleResponse(doc: Document): String = PostPreviewParseTask(doc).call()

}
