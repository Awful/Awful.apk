package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.FUNCTION_BOOKMARK
import com.ferg.awfulapp.constants.Constants.PARAM_ACTION
import com.ferg.awfulapp.constants.Constants.PARAM_THREAD_ID
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

/**
 * Cycle the bookmark colour set on the site for a given thread.
 */
class BookmarkColorRequest(context: Context, threadId: Int)
    : AwfulRequest<Void?>(context, FUNCTION_BOOKMARK, isPostRequest = true) {
    init {
        with (parameters) {
            add(PARAM_ACTION, "cat_toggle")
            add(PARAM_THREAD_ID, threadId.toString())
        }
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? = null

}
