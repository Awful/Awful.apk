package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.thread.AwfulSearch
import org.jsoup.nodes.Document
import java.util.*

/**
 * Fetch a [page] of results from an existing search query by providing its [queryId].
 */
class SearchResultPageRequest(context: Context, private val queryId: Int, private val page: Int)
    : AwfulRequest<ArrayList<AwfulSearch>>(context, FUNCTION_SEARCH) {

    init {
        with(parameters){
            add(PARAM_ACTION, ACTION_RESULTS)
            add(PARAM_QID, queryId.toString())
            add(PAGE, page.toString())
        }
    }

    override fun handleResponse(doc: Document): ArrayList<AwfulSearch> =
            AwfulSearch.parseSearchResult(doc)

}
