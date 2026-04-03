package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

/**
 * Checks whether a post has already been reported, and if it hasn't it grabs the warning
 * text above the report editor (e.g. the post is more than two weeks old).
 *
 * Overrides [handleCriticalError] since the already-reported page triggers the base
 * class's standarderror detection.
 */
class ReportCheckRequest(context: Context, postId: Int)
    : AwfulRequest<ReportCheckResult>(context, FUNCTION_REPORT) {

    init {
        parameters.add(PARAM_POST_ID, postId.toString())
    }

    override fun handleCriticalError(error: AwfulError, doc: Document): Boolean = true

    override fun handleResponse(doc: Document): ReportCheckResult {
        val body = doc.body()
        if (body.hasClass("standarderror")) {
            return ReportCheckResult(alreadyReported = true)
        }
        val warning = body.select("span.warningsmalltext")
            ?.mapNotNull { it.text().takeIf(String::isNotBlank) }
            ?.joinToString("\n")
            ?.takeIf(String::isNotEmpty)
        return ReportCheckResult(alreadyReported = false, warning = warning)
    }
}

data class ReportCheckResult(
    val alreadyReported: Boolean,
    val warning: String? = null
)
