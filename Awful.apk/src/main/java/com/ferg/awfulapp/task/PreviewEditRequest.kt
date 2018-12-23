package com.ferg.awfulapp.task

import android.content.ContentValues
import android.content.Context
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.thread.AwfulPost
import com.ferg.awfulapp.thread.PostPreviewParseTask
import org.jsoup.nodes.Document
import timber.log.Timber

/**
 * A request that fetches a preview of an edited post, providing the parsed HTML.
 *
 * This functions like the Preview button when editing a post on the site, which
 * takes the current content in the edit window and renders it as HTML. You supply
 * the data on the edit page via the ContentValues parameter, which is sent to
 * the site to retrieve the preview.
 */
class PreviewEditRequest(context: Context, reply: ContentValues)
    : AwfulRequest<String>(context, FUNCTION_EDIT_POST, isPostRequest = true) {

    init {
        with(parameters) {
            val postId = reply.getAsInteger(AwfulPost.EDIT_POST_ID)?.toString()
                    ?: throw IllegalArgumentException("No post ID included")
            Timber.i("$PARAM_POST_ID: $postId")

            add(PARAM_ACTION, "updatepost")
            add(PARAM_POST_ID, postId)
            add(PARAM_MESSAGE, reply.getAsString(AwfulMessage.REPLY_CONTENT).run(NetworkUtils::encodeHtml))
            add(PARAM_PARSEURL, YES)
            // TODO: this bookmarks every thread you edit a post in, unless you turn it off in a browser - seems bad for replies, worse for edits?
            if (reply.getAsString(AwfulPost.FORM_BOOKMARK).equals("checked", ignoreCase = true)) {
                parameters.add(PARAM_BOOKMARK, YES)
            }

            listOf(AwfulMessage.REPLY_SIGNATURE, AwfulMessage.REPLY_DISABLE_SMILIES)
                    .forEach { if (reply.containsKey(it)) parameters.add(it, YES) }
            add(PARAM_SUBMIT, SUBMIT_REPLY)
            add(PARAM_PREVIEW, PREVIEW_REPLY)
        }
    }


    override fun handleResponse(doc: Document): String = PostPreviewParseTask(doc).call()

}
