package com.ferg.awfulapp.task

import android.content.ContentValues
import android.content.Context
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
class PreviewPostRequest (context: Context, reply: ContentValues)
    : AwfulRequest<String>(context, FUNCTION_POST_REPLY, isPostRequest = true) {
// TODO: 18/12/2017 this and PreviewEditRequest are almost identical, merge 'em
    init {
        with(parameters) {
            val threadId = reply.getAsInteger(AwfulMessage.ID)?.toString()
                    ?: throw IllegalArgumentException("No thread ID included")
            add(PARAM_ACTION, "postreply")
            add(PARAM_THREAD_ID, threadId)
            add(PARAM_FORMKEY, reply.getAsString(AwfulPost.FORM_KEY))
            add(PARAM_FORM_COOKIE, reply.getAsString(AwfulPost.FORM_COOKIE))
            add(PARAM_MESSAGE, reply.getAsString(AwfulMessage.REPLY_CONTENT).run(NetworkUtils::encodeHtml))

            add(PARAM_PARSEURL, YES)
            // TODO: this bookmarks every thread you post in, unless you turn it off in a browser - seems bad?
            if (reply.getAsString(AwfulPost.FORM_BOOKMARK).equals("checked", ignoreCase = true)) {
                add(PARAM_BOOKMARK, YES)
            }
            listOf(AwfulMessage.REPLY_SIGNATURE, AwfulMessage.REPLY_DISABLE_SMILIES)
                    .forEach { if (reply.containsKey(it)) add(it, YES) }

            reply.getAsString(AwfulMessage.REPLY_ATTACHMENT)?.let { filePath -> attachFile(PARAM_ATTACHMENT, filePath) }
            add(PARAM_SUBMIT, SUBMIT_REPLY)
            add(PARAM_PREVIEW, PREVIEW_REPLY)
        }
    }


    override fun handleResponse(doc: Document): String = PostPreviewParseTask(doc).call()

}
