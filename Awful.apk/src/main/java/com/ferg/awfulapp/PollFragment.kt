package com.ferg.awfulapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TableLayout
import android.widget.TextView
import com.android.volley.VolleyError
import com.ferg.awfulapp.NavigationEvent.Companion.parse
import com.ferg.awfulapp.task.AwfulRequest
import com.ferg.awfulapp.task.PollResultsRequest
import com.ferg.awfulapp.task.PollVoteRequest
import com.ferg.awfulapp.thread.AwfulPoll
import com.ferg.awfulapp.util.bind

/**
 * Displays a thread's poll - the voting form if the user can still vote on it, otherwise the
 * current results as a table with bar graphs, like the site shows.
 *
 * The poll to display is described by a [NavigationEvent.Poll] parsed from the activity's intent.
 * After a vote is submitted the fresh results are fetched and displayed, and the thread's stored
 * poll data is updated so its poll button reflects that the user has voted.
 */
class PollFragment : AwfulFragment() {

    private val questionView: TextView by bind(R.id.poll_question)
    private val voteContainer: View by bind(R.id.vote_container)
    private val optionsGroup: RadioGroup by bind(R.id.poll_options)
    private val voteButton: Button by bind(R.id.vote_button)
    private val resultsContainer: View by bind(R.id.results_container)
    private val resultsTable: TableLayout by bind(R.id.results_table)
    private val resultsTotal: TextView by bind(R.id.results_total)

    private var threadId = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflateView(R.layout.poll, container, inflater)

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        val event = activity?.intent?.parse() as? NavigationEvent.Poll
        val poll = event?.let { AwfulPoll.fromJson(it.pollJson) }
        if (event == null || poll == null) {
            activity?.finish()
            return
        }
        threadId = event.threadId
        questionView.text = poll.question
        when (poll) {
            is AwfulPoll.Votable -> showVoteForm(poll)
            is AwfulPoll.Results -> showResults(poll)
        }
    }

    override fun getTitle(): String? = getString(R.string.poll_title)

    private fun showVoteForm(poll: AwfulPoll.Votable) {
        optionsGroup.removeAllViews()
        poll.options.forEach { option ->
            val button: CompoundButton =
                    if (poll.multipleChoice) CheckBox(requireContext()) else RadioButton(requireContext())
            button.id = View.generateViewId()
            button.text = option.label
            button.tag = option.number
            optionsGroup.addView(button)
        }
        voteButton.setOnClickListener { submitVote(poll) }
        resultsContainer.visibility = View.GONE
        voteContainer.visibility = View.VISIBLE
    }

    private fun submitVote(poll: AwfulPoll.Votable) {
        val selected = (0 until optionsGroup.childCount)
                .mapNotNull { optionsGroup.getChildAt(it) as? CompoundButton }
                .filter { it.isChecked }
                .map { it.tag as String }
        if (selected.isEmpty()) {
            makeToast(R.string.poll_select_an_option)
            return
        }
        voteButton.isEnabled = false
        activity?.let { context ->
            queueRequest(PollVoteRequest(context, poll, selected)
                    .build(this, object : AwfulRequest.AwfulResultCallback<Void?> {

                        override fun success(result: Void?) {
                            loadResults(poll.pollId)
                        }

                        override fun failure(error: VolleyError?) {
                            voteButton.isEnabled = true
                        }
                    }))
        }
    }

    private fun loadResults(pollId: Int) {
        activity?.let { context ->
            queueRequest(PollResultsRequest(context, pollId, threadId)
                    .build(this, object : AwfulRequest.AwfulResultCallback<AwfulPoll.Results> {

                        override fun success(result: AwfulPoll.Results) {
                            showResults(result)
                        }

                        override fun failure(error: VolleyError?) {
                            activity?.finish()
                        }
                    }))
        }
    }

    private fun showResults(results: AwfulPoll.Results) {
        resultsTable.removeAllViews()
        results.rows.forEach { row ->
            val tableRow = layoutInflater.inflate(R.layout.poll_result_row, resultsTable, false)
            tableRow.findViewById<TextView>(R.id.result_label).text = row.label
            tableRow.findViewById<TextView>(R.id.result_votes).text = row.votes
            tableRow.findViewById<TextView>(R.id.result_percent).text = row.percentage
            val fraction = row.fraction.coerceIn(0f, 1f)
            (tableRow.findViewById<View>(R.id.result_bar_fill).layoutParams as LinearLayout.LayoutParams).weight = fraction
            (tableRow.findViewById<View>(R.id.result_bar_space).layoutParams as LinearLayout.LayoutParams).weight = 1f - fraction
            resultsTable.addView(tableRow)
        }
        resultsTotal.text = getString(R.string.poll_total_votes, results.totalVotes)
        resultsTotal.visibility = if (results.totalVotes.isEmpty()) View.GONE else View.VISIBLE
        voteContainer.visibility = View.GONE
        resultsContainer.visibility = View.VISIBLE
    }
}
