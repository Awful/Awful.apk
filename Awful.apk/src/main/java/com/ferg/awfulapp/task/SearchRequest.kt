package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.thread.AwfulSearch
import com.ferg.awfulapp.thread.AwfulSearchResult
import org.jsoup.nodes.Document

/**
 * Run a search query, optionally limiting the search to a set of forum IDs.
 */
class SearchRequest(context: Context, query: String, forums: IntArray?)
    : AwfulRequest<AwfulSearchResult>(context, FUNCTION_SEARCH, isPostRequest = true) {
    init {
        with(parameters) {
            add(PARAM_ACTION, ACTION_QUERY)
            add(PARAM_QUERY, query)
            forums?.forEachIndexed { index, forumId ->
                add(String.format(PARAM_FORUMS, index), forumId.toString())
            }
        }
    }


    override fun handleResponse(doc: Document): AwfulSearchResult {
        return AwfulSearchResult.parseSearch(doc).apply {
            resultList = AwfulSearch.parseSearchResult(doc)
        }
    }

}
