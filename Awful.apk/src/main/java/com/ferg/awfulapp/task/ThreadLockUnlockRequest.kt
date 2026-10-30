package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.ACTION_TOGGLE_THREAD_LOCKED
import com.ferg.awfulapp.constants.Constants.FUNCTION_POSTINGS
import com.ferg.awfulapp.constants.Constants.PARAM_ACTION
import com.ferg.awfulapp.constants.Constants.PARAM_THREAD_ID
import org.jsoup.nodes.Document

/**
 * Attempts to toggle the locked/unlocked state of a thread.
 */
class ThreadLockUnlockRequest(context: Context, private val threadId: Int)
    : AwfulRequest<Void?>(context, FUNCTION_POSTINGS, isPostRequest = true) {

    init {
        with(parameters) {
            add(PARAM_THREAD_ID, threadId.toString())
            add(PARAM_ACTION, ACTION_TOGGLE_THREAD_LOCKED)
        }
    }

    override fun handleResponse(doc: Document): Void? = null

}
