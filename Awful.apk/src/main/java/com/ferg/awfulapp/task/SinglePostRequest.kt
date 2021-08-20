package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.thread.AwfulPost
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


    init {
        with(parameters) {
            add(PARAM_ACTION, ACTION_SHOWPOST)
            add(PARAM_POST_ID, postId)
        }
    }

    override fun handleResponse(doc: Document): String {
        val prefs = AwfulPreferences.getInstance();
        val postBody = doc.selectFirst(".postbody")
        val fyadPostBody = postBody?.selectFirst(".complete_shit") ?: throw AwfulError("Couldn't find post content")
        (fyadPostBody ?: postBody).apply {
            AwfulPost.convertVideos(this, prefs.inlineYoutube)
            getElementsByTag("img").forEach {
                AwfulPost.processPostImage(
                    it,
                    false,
                    prefs
                )
            }
            getElementsByTag("a").forEach(AwfulPost::tryConvertToHttps)
            if (this == fyadPostBody) {
                // FYAD sigs are currently a sibling div alongside .complete_shit, so we need to stick them at the end of the content
                postBody.selectFirst("> .signature")?.appendTo(this)
            }
            return html()
        }
}

    companion object {
        private val REQUEST_TAG = Any()
    }
}
