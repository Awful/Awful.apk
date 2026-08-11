package com.ferg.awfulapp.task

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants.ACTION_POLL_VOTE
import com.ferg.awfulapp.constants.Constants.ACTION_SHOW_RESULTS
import com.ferg.awfulapp.constants.Constants.FUNCTION_POLL
import com.ferg.awfulapp.constants.Constants.PARAM_ACTION
import com.ferg.awfulapp.constants.Constants.PARAM_POLL_ID
import com.ferg.awfulapp.thread.AwfulPoll
import com.ferg.awfulapp.thread.AwfulThread
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

/**
 * Submits a vote on a poll. [selectedOptions] holds the option numbers the user chose, as parsed
 * into the [AwfulPoll.Votable]. The site's response isn't displayed - follow up with a
 * [PollResultsRequest] to show the updated results.
 */
class PollVoteRequest(context: Context, poll: AwfulPoll.Votable, selectedOptions: List<String>) :
        AwfulRequest<Void?>(context, FUNCTION_POLL, isPostRequest = true) {

    companion object {
        const val FIELD_OPTION = "optionnumber"
    }

    init {
        with(parameters) {
            add(PARAM_ACTION, ACTION_POLL_VOTE)
            add(PARAM_POLL_ID, poll.pollId.toString())
            if (poll.multipleChoice) {
                selectedOptions.forEach { add("$FIELD_OPTION[$it]", "yes") }
            } else {
                selectedOptions.firstOrNull()?.let { add(FIELD_OPTION, it) }
            }
        }
    }

    override fun handleResponse(doc: Document): Void? = null
}


/**
 * Fetches a poll's current results, updating the stored poll data for [threadId] so the thread
 * view reflects that the user can no longer vote.
 */
class PollResultsRequest(context: Context, pollId: Int, private val threadId: Int) :
        AwfulRequest<AwfulPoll.Results>(context, FUNCTION_POLL) {

    init {
        with(parameters) {
            add(PARAM_ACTION, ACTION_SHOW_RESULTS)
            add(PARAM_POLL_ID, pollId.toString())
        }
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): AwfulPoll.Results {
        val results = AwfulPoll.parseResults(doc)
                ?: throw AwfulError(context.getString(R.string.poll_results_load_failed))
        val cv = ContentValues().apply { put(AwfulThread.POLL, results.toJson()) }
        context.contentResolver.update(ContentUris.withAppendedId(AwfulThread.CONTENT_URI, threadId.toLong()), cv, null, null)
        return results
    }
}
