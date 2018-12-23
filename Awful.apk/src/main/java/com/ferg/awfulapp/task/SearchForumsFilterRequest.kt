package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.FUNCTION_SEARCH
import com.ferg.awfulapp.thread.AwfulSearchForum
import org.jsoup.nodes.Document
import java.util.*

/**
 * Get a list of forums that can be searched, and their default selection states.
 */
class SearchForumsFilterRequest(context: Context)
    : AwfulRequest<ArrayList<AwfulSearchForum>>(context, FUNCTION_SEARCH) {

    override fun handleResponse(doc: Document): ArrayList<AwfulSearchForum> =
            AwfulSearchForum.parseSearchForums(doc)

}
