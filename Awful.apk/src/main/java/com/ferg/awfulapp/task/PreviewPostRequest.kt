package com.ferg.awfulapp.task

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.thread.AwfulPost
import com.ferg.awfulapp.thread.PostPreviewParseTask
import org.jsoup.nodes.Document

/**
 * A request that fetches a preview of a reply, providing the parsed HTML.
 *
 * This functions like the Preview button when composing a post on the site, which
 * takes the current content in the post window and renders it as HTML. You supply
 * the data on the edit page via the ContentValues parameter, which is sent to
 * the site to retrieve the preview.
 */
class PreviewPostRequest (context: Context, reply: ContentValues) : AwfulRequest<String>(context, null) {
// TODO: 18/12/2017 this and PreviewEditRequest are almost identical, merge 'em
    init {
        with(reply) {
            val threadId = getAsInteger(AwfulMessage.ID)?.toString()
                    ?: throw IllegalArgumentException("No thread ID included")
            addPostParam(PARAM_ACTION, "postreply")
            addPostParam(PARAM_THREAD_ID, threadId)
            addPostParam(PARAM_FORMKEY, getAsString(AwfulPost.FORM_KEY))
            addPostParam(PARAM_FORM_COOKIE, getAsString(AwfulPost.FORM_COOKIE))
            addPostParam(PARAM_MESSAGE, getAsString(AwfulMessage.REPLY_CONTENT).run(NetworkUtils::encodeHtml))

            addPostParam(PARAM_PARSEURL, YES)
            // TODO: this bookmarks every thread you post in, unless you turn it off in a browser - seems bad?
            if (getAsString(AwfulPost.FORM_BOOKMARK).equals("checked", ignoreCase = true)) {
                addPostParam(PARAM_BOOKMARK, YES)
            }
            listOf(AwfulMessage.REPLY_SIGNATURE, AwfulMessage.REPLY_DISABLE_SMILIES)
                    .forEach { if (containsKey(it)) addPostParam(it, YES) }

            getAsString(AwfulMessage.REPLY_ATTACHMENT)?.let { filePath -> attachFile(PARAM_ATTACHMENT, filePath) }
            addPostParam(PARAM_SUBMIT, SUBMIT_REPLY)
            addPostParam(PARAM_PREVIEW, PREVIEW_REPLY)
        }
        buildFinalRequest()
    }

    override fun generateUrl(urlBuilder: Uri.Builder?) = FUNCTION_POST_REPLY

    override fun handleResponse(doc: Document): String = PostPreviewParseTask(doc).call()

}
