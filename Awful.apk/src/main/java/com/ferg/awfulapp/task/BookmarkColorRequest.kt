package com.ferg.awfulapp.task

import android.content.Context
import android.net.Uri

import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.util.AwfulError

import org.jsoup.nodes.Document

/**
 * Created by Matthew on 8/9/13.
 */
class BookmarkColorRequest(context: Context, threadId: Int) : AwfulRequest<Void?>(context, null) {
    init {
        addPostParam(Constants.PARAM_ACTION, "cat_toggle")
        addPostParam(Constants.PARAM_THREAD_ID, Integer.toString(threadId))
    }

    override fun generateUrl(urlBuilder: Uri.Builder?): String {
        // TODO wat
        //since we aren't adding query arguments to a POST request,
        //we can just pass null in the constructor URL field and it'll skip this Uri.Builder
        return Constants.FUNCTION_BOOKMARK
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? = null

}
