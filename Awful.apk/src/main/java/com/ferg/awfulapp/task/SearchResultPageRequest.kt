package com.ferg.awfulapp.task

import android.content.Context
import android.net.Uri
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
        buildFinalRequest()
    }

    override fun generateUrl(urlBuilder: Uri.Builder?): String {
        with(urlBuilder!!){
            appendQueryParameter(PARAM_ACTION, ACTION_RESULTS)
            appendQueryParameter(PARAM_QID, queryId.toString())
            appendQueryParameter(PAGE, page.toString())
            return build().toString()
        }
    }

    override fun handleResponse(doc: Document): ArrayList<AwfulSearch> {
        return AwfulSearch.parseSearchResult(doc)
    }

}
