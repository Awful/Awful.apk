package com.ferg.awfulapp.task

import android.content.Context
import android.net.Uri
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

/**
 * Fetch and parse a single post, using the given [postId].
 *
 * This returns the post content as raw HTML.
 */
class SinglePostRequest(context: Context, private val postId: String)
    : AwfulRequest<String>(context, FUNCTION_THREAD) {


    override val requestTag: Any
        get() = REQUEST_TAG


    override fun generateUrl(urlBuilder: Uri.Builder?): String {
        with(urlBuilder!!) {
            appendQueryParameter(PARAM_ACTION, ACTION_SHOWPOST)
            appendQueryParameter(PARAM_POST_ID, postId)
            return build().toString()
        }
    }

    override fun handleResponse(doc: Document): String =
            doc.selectFirst(".postbody")?.html() ?: throw AwfulError("Couldn't find post content")


    companion object {
        private val REQUEST_TAG = Any()
    }
}
