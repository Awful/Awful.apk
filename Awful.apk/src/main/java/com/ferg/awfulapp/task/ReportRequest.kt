package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * A request that submits a report for a post.
 *
 * This sends a report to the mods for a given [postId], providing [comments] as
 * the report reason. The response is a status message when the report was
 * successful, otherwise an error is thrown.
 */
class ReportRequest(context: Context, private val postId: Int, private val comments: String)
    : AwfulRequest<String>(context, FUNCTION_REPORT, isPostRequest = true) {

        init {
        with(parameters) {
            add(PARAM_COMMENTS, comments)
            add(PARAM_POST_ID, postId.toString())
            add(PARAM_ACTION, "submit")
        }
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): String {
        val message = doc.selectFirst("standard")?.takeIf(Element::hasText)?.text()
        fun responseSays(text: String) = message?.contains(text, ignoreCase = true) ?: false
        // TODO: when I tested this I got a toast with a different success message - no clue where it came from!
        return when {
            responseSays("your alert has been submitted") ->
               "Report sent!"
            responseSays("this thread has already been reported") ->
                "Someone has already reported this thread recently"
            else ->
                throw AwfulError("An error occurred while trying to send your report")
        }
    }

}
