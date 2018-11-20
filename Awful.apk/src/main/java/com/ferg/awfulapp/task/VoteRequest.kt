package com.ferg.awfulapp.task

import android.content.Context
import android.net.Uri

import com.android.volley.VolleyError
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.util.AwfulError

import org.jsoup.nodes.Document

/**
 * A request that submits a rating for a thread.
 */
class VoteRequest(context: Context, threadId: Int, vote: Int) : AwfulRequest<Void?>(context, null) {
    init {
        addPostParam(Constants.PARAM_THREAD_ID, threadId.toString())
        addPostParam(Constants.PARAM_VOTE, vote.toString())
    }

    override fun generateUrl(urlBuilder: Uri.Builder?) = Constants.FUNCTION_RATE_THREAD

    override fun handleResponse(doc: Document): Void? = null

    override fun customizeProgressListenerError(error: VolleyError): VolleyError =
            AwfulError(context.getString(R.string.vote_failed))
}
