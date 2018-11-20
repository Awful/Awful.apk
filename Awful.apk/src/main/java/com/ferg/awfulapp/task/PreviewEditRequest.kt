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
import timber.log.Timber

/**
 * A request that fetches a preview of an edited post, providing the parsed HTML.
 *
 * This functions like the Preview button when editing a post on the site, which
 * takes the current content in the edit window and renders it as HTML. You supply
 * the data on the edit page via the ContentValues parameter, which is sent to
 * the site to retrieve the preview.
 */
class PreviewEditRequest(context: Context, reply: ContentValues) : AwfulRequest<String>(context, null) {
    init {
        with(reply) {
            val postId = getAsInteger(AwfulPost.EDIT_POST_ID)?.toString()
                    ?: throw IllegalArgumentException("No post ID included")
            addPostParam(PARAM_ACTION, "updatepost")
            addPostParam(PARAM_POST_ID, postId)
            Timber.i("$PARAM_POST_ID: $postId")
            addPostParam(PARAM_MESSAGE, getAsString(AwfulMessage.REPLY_CONTENT).run(NetworkUtils::encodeHtml))
            addPostParam(PARAM_PARSEURL, YES)
            // TODO: this bookmarks every thread you edit a post in, unless you turn it off in a browser - seems bad for replies, worse for edits?
            if (getAsString(AwfulPost.FORM_BOOKMARK).equals("checked", ignoreCase = true)) {
                addPostParam(PARAM_BOOKMARK, YES)
            }

            listOf(AwfulMessage.REPLY_SIGNATURE, AwfulMessage.REPLY_DISABLE_SMILIES)
                    .forEach { if (containsKey(it)) addPostParam(it, YES) }
            addPostParam(PARAM_SUBMIT, SUBMIT_REPLY)
            addPostParam(PARAM_PREVIEW, PREVIEW_REPLY)
        }

        buildFinalRequest()
    }

    override fun generateUrl(urlBuilder: Uri.Builder?) = FUNCTION_EDIT_POST

    override fun handleResponse(doc: Document): String = PostPreviewParseTask(doc).call()

}
