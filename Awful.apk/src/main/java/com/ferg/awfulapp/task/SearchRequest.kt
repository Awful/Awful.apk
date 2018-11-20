package com.ferg.awfulapp.task

import android.content.Context
import android.net.Uri
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.thread.AwfulSearch
import com.ferg.awfulapp.thread.AwfulSearchResult
import org.jsoup.nodes.Document

/**
 * Run a search query, optionally limiting the search to a set of forum IDs.
 */
class SearchRequest(context: Context, query: String, forums: IntArray?)
    : AwfulRequest<AwfulSearchResult>(context, null) {
    init {
        addPostParam(PARAM_ACTION, ACTION_QUERY)
        addPostParam(PARAM_QUERY, query)
        forums?.forEachIndexed { index, forumId ->
            addPostParam(String.format(PARAM_FORUMS, index), forumId.toString())
        }
        buildFinalRequest()
    }

    override fun generateUrl(urlBuilder: Uri.Builder?): String = FUNCTION_SEARCH

    override fun handleResponse(doc: Document): AwfulSearchResult {
        return AwfulSearchResult.parseSearch(doc).apply {
            resultList = AwfulSearch.parseSearchResult(doc)
        }
    }

}
