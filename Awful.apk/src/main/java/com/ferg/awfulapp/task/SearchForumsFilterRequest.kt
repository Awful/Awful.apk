package com.ferg.awfulapp.task

import android.content.Context
import android.net.Uri
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.thread.AwfulSearchForum
import org.jsoup.nodes.Document
import java.util.*

/**
 * Get a list of forums that can be searched, and their default selection states.
 */
class SearchForumsFilterRequest(context: Context) : AwfulRequest<ArrayList<AwfulSearchForum>>(context, null) {

    override fun generateUrl(urlBuilder: Uri.Builder?): String = Constants.FUNCTION_SEARCH

    override fun handleResponse(doc: Document): ArrayList<AwfulSearchForum> {
        return AwfulSearchForum.parseSearchForums(doc)
    }

}
