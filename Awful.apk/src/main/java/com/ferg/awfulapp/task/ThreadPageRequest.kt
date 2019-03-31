package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.thread.AwfulThread
import org.jsoup.nodes.Document

/**
 * A request to fetch and parse the data on a thread page, updating the database with the results.
 *
 * Supplying a valid [userId] will fetch from the "show posts by this user" thread view, i.e. the
 * thread will consist entirely of their posts, and will have as many pages as those posts can fill.
 *
 * Currently this overwrites the "normal" thread view in the database, so viewing page 1 of a thread,
 * and then page 1 with a given [userId] will result in page 1 being fully or partially overwritten
 * with that user's posts, depending on how many there are. This is only a problem when viewing the
 * cached data (since usually the page will be reloaded and rewritten when you view it) but it's
 * something to be aware of.
 */
class ThreadPageRequest(context: Context, private val threadId: Int, private val page: Int, private val userId: Int = 0)
    : AwfulStrippedRequest<Void?>(context, FUNCTION_THREAD) {


    override val requestTag: Any
        get() = REQUEST_TAG

    init {
        with(parameters) {
            add(PARAM_THREAD_ID, threadId.toString())
            add(PARAM_PER_PAGE, preferences.postPerPage.toString())
            add(PARAM_PAGE, page.toString())
            if (userId > 0) add(PARAM_USER_ID, userId.toString())
        }
    }

    override fun handleResponse(doc: Document): Void? {
        AwfulThread.parseThreadPage(contentResolver, doc, threadId, page, -1, preferences.postPerPage, preferences, userId)
        return null
    }

    public override fun handleStrippedResponse(document: Document, currentPage: Int?, totalPages: Int?): Void? {
        // TODO: this is all kinda janky, best to use the passed data from the response, right? Instead of relying on 'page' from the request
        val lastPage = totalPages ?: page
        AwfulThread.parseThreadPage(contentResolver, document, threadId, page, lastPage, preferences.postPerPage, preferences, userId)
        return null
    }


    companion object {
        val REQUEST_TAG = Any()
    }
}
