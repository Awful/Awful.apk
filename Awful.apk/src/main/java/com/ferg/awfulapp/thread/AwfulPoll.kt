package com.ferg.awfulapp.thread

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.nodes.Document

/**
 * A thread's poll, shown above the posts on every page of the thread.
 *
 * A poll is either [Votable] (the page shows the voting form) or [Results] (the page shows the
 * current results instead, because the user has already voted or the poll is closed). Polls are
 * stored in the threads table as JSON, via [toJson] and [fromJson].
 */
sealed class AwfulPoll {

    abstract val question: String

    abstract fun toJson(): String

    /** A poll the user can still vote on. */
    class Votable(
            override val question: String,
            val pollId: Int,
            val multipleChoice: Boolean,
            val options: List<Option>
    ) : AwfulPoll() {

        override fun toJson(): String = JSONObject().apply {
            put(KEY_TYPE, TYPE_VOTABLE)
            put(KEY_QUESTION, question)
            put(KEY_POLL_ID, pollId)
            put(KEY_MULTIPLE_CHOICE, multipleChoice)
            put(KEY_OPTIONS, JSONArray().apply {
                options.forEach { put(JSONObject().put(KEY_NUMBER, it.number).put(KEY_LABEL, it.label)) }
            })
        }.toString()
    }

    /** An option in a [Votable] poll - [number] is the value the site expects when voting. */
    data class Option(val number: String, val label: String)

    /** The results of a poll the user can't vote on (already voted, or it's closed). */
    class Results(
            override val question: String,
            val rows: List<Row>,
            val totalVotes: String
    ) : AwfulPoll() {

        override fun toJson(): String = JSONObject().apply {
            put(KEY_TYPE, TYPE_RESULTS)
            put(KEY_QUESTION, question)
            put(KEY_TOTAL_VOTES, totalVotes)
            put(KEY_ROWS, JSONArray().apply {
                rows.forEach {
                    put(JSONObject().put(KEY_LABEL, it.label).put(KEY_VOTES, it.votes).put(KEY_PERCENTAGE, it.percentage))
                }
            })
        }.toString()
    }

    /** A row in a poll's results - [votes] and [percentage] are the site's rendering, e.g. "116" and "48.74%". */
    data class Row(val label: String, val votes: String, val percentage: String) {
        val fraction: Float
            get() = (percentage.trimEnd('%').toFloatOrNull() ?: 0f) / 100f
    }

    companion object {

        private const val TYPE_VOTABLE = "votable"
        private const val TYPE_RESULTS = "results"
        private const val KEY_TYPE = "type"
        private const val KEY_QUESTION = "question"
        private const val KEY_POLL_ID = "pollId"
        private const val KEY_MULTIPLE_CHOICE = "multipleChoice"
        private const val KEY_OPTIONS = "options"
        private const val KEY_NUMBER = "number"
        private const val KEY_LABEL = "label"
        private const val KEY_ROWS = "rows"
        private const val KEY_VOTES = "votes"
        private const val KEY_PERCENTAGE = "percentage"
        private const val KEY_TOTAL_VOTES = "totalVotes"

        private val CHECKBOX_OPTION_NUMBER = """\[(\d+)]""".toRegex()

        /** Parse the poll on a thread page, if there is one. */
        @JvmStatic
        fun parse(page: Document): AwfulPoll? {
            page.selectFirst("form[action=poll.php]")?.let { form ->
                val pollId = form.selectFirst("input[name=pollid]")?.`val`()?.toIntOrNull() ?: return null
                val question = form.selectFirst("th")?.text()?.trim() ?: return null
                val inputs = form.select("input[type=radio], input[type=checkbox]")
                val multipleChoice = inputs.any { it.attr("type") == "checkbox" }
                val options = inputs.mapNotNull { input ->
                    val label = input.parent()?.nextElementSibling()?.text()?.trim()
                    val number = if (multipleChoice) {
                        CHECKBOX_OPTION_NUMBER.find(input.attr("name"))?.groupValues?.get(1)
                    } else {
                        input.`val`()
                    }
                    if (label != null && !number.isNullOrEmpty()) Option(number, label) else null
                }
                return if (options.isEmpty()) null else Votable(question, pollId, multipleChoice, options)
            }
            return parseResults(page)
        }

        /** Parse poll results from a page containing a results table, e.g. a thread page or a showresults page. */
        @JvmStatic
        fun parseResults(page: Document): Results? {
            val table = page.select("table").firstOrNull { it.selectFirst("td.graphbar") != null } ?: return null
            val question = (table.selectFirst("th b") ?: table.selectFirst("th"))?.text()?.trim() ?: return null
            val rows = table.select("tr").mapNotNull { tr ->
                val cells = tr.select("td")
                if (tr.selectFirst("td.graphbar") == null || cells.size < 4) null
                else Row(cells[0].text().trim(), cells[2].text().trim(), cells[3].text().trim())
            }
            // the total row is "Total: | N votes" on thread pages, but "Total: | N votes | 100%" on showresults pages
            val totalVotes = table.select("tr").last()?.select("td b")?.getOrNull(1)?.text()?.trim() ?: ""
            return if (rows.isEmpty()) null else Results(question, rows, totalVotes)
        }

        @JvmStatic
        fun fromJson(json: String?): AwfulPoll? {
            if (json.isNullOrEmpty()) return null
            return try {
                val poll = JSONObject(json)
                when (poll.getString(KEY_TYPE)) {
                    TYPE_VOTABLE -> Votable(
                            poll.getString(KEY_QUESTION),
                            poll.getInt(KEY_POLL_ID),
                            poll.getBoolean(KEY_MULTIPLE_CHOICE),
                            poll.getJSONArray(KEY_OPTIONS).map { Option(it.getString(KEY_NUMBER), it.getString(KEY_LABEL)) }
                    )
                    TYPE_RESULTS -> Results(
                            poll.getString(KEY_QUESTION),
                            poll.getJSONArray(KEY_ROWS).map { Row(it.getString(KEY_LABEL), it.getString(KEY_VOTES), it.getString(KEY_PERCENTAGE)) },
                            poll.getString(KEY_TOTAL_VOTES)
                    )
                    else -> null
                }
            } catch (e: JSONException) {
                null
            }
        }

        private fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
                (0 until length()).map { transform(getJSONObject(it)) }
    }
}
