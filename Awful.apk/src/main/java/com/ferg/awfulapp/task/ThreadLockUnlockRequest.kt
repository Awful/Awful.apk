package com.ferg.awfulapp.task

import android.content.Context
import android.net.Uri
import com.ferg.awfulapp.constants.Constants.*
import org.jsoup.nodes.Document

/**
 * Attempts to toggle the locked/unlocked state of a thread.
 */
class ThreadLockUnlockRequest(context: Context, private val threadId: Int)
    : AwfulRequest<Void?>(context, FUNCTION_POSTINGS) {

    override fun generateUrl(urlBuilder: Uri.Builder?): String {
        addPostParam(PARAM_THREAD_ID, threadId.toString())
        with(urlBuilder!!) {
            appendQueryParameter(PARAM_ACTION, ACTION_TOGGLE_THREAD_LOCKED)
            return build().toString()
        }
    }

    override fun handleResponse(doc: Document): Void? = null

}
