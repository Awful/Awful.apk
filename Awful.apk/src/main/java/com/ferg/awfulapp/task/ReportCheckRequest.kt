package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.*
import org.jsoup.nodes.Document

/**
 * A request that checks whether a post has already been reported recently.
 */
class ReportCheckRequest(context: Context, postId: Int)
    : AwfulRequest<Boolean>(context, FUNCTION_REPORT) {

    init {
        parameters.add(PARAM_POST_ID, postId.toString())
    }

    override fun handleResponse(doc: Document): Boolean {
        val body = doc.body()
        return body != null && body.hasClass("standarderror")
    }
}
