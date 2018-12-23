package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.*
import org.jsoup.nodes.Document

/**
 * Attempts to toggle the locked/unlocked state of a thread.
 */
class ThreadLockUnlockRequest(context: Context, private val threadId: Int)
    : AwfulRequest<Void?>(context, FUNCTION_POSTINGS) {

    init {
        with(parameters) {
            add(PARAM_THREAD_ID, threadId.toString())
            add(PARAM_ACTION, ACTION_TOGGLE_THREAD_LOCKED)
        }
    }

    override fun handleResponse(doc: Document): Void? = null

}
