package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants.ACTION_REQUEST_BAN
import com.ferg.awfulapp.constants.Constants.ACTION_REQUEST_PROBATION
import com.ferg.awfulapp.constants.Constants.FUNCTION_MODQUEUE
import com.ferg.awfulapp.constants.Constants.PARAM_ACTION
import com.ferg.awfulapp.constants.Constants.PARAM_TARGET_POST_ID
import com.ferg.awfulapp.constants.Constants.PARAM_THREAD_ID
import com.ferg.awfulapp.constants.Constants.PARAM_USER_ID
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

/**
 * Request to load the modqueue probation/ban request page for a post, parsing out the details
 * needed to display the form and submit it through a [ModQueueSubmitRequest].
 */
class ModQueueFormRequest(context: Context, ban: Boolean, userId: Int, postId: Int, threadId: Int) :
        AwfulRequest<ModQueueFormRequest.ModQueueForm>(context, FUNCTION_MODQUEUE) {

    companion object {
        const val FIELD_DURATION = "data"
        const val FIELD_BAN_TYPE = "act"
        const val FIELD_REASON = "reason"
        const val FIELD_NOTES = "notes"
    }

    init {
        with(parameters) {
            add(PARAM_ACTION, if (ban) ACTION_REQUEST_BAN else ACTION_REQUEST_PROBATION)
            add(PARAM_USER_ID, userId.toString())
            add(PARAM_TARGET_POST_ID, postId.toString())
            add(PARAM_THREAD_ID, threadId.toString())
        }
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): ModQueueForm {
        val form = doc.selectFirst("form[action=modqueue.php]")
                ?: throw AwfulError(context.getString(R.string.mod_request_form_load_failed))

        val hiddenFields = form.select("input[type=hidden]")
                .associate { it.attr("name") to it.attr("value") }

        fun tableValueFor(label: String) =
                form.select("td").firstOrNull { it.text().trim() == label }?.nextElementSibling()

        val username = tableValueFor("User:")?.text()?.trim()
        val history = tableValueFor("History:")?.run {
            select("a").remove()
            text().replace("""\(\s*\)""".toRegex(), "").trim()
        }

        val durations = form.select("select[name=$FIELD_DURATION] option")
                .map { Choice(it.`val`(), it.text().trim(), it.hasAttr("selected")) }
        val banTypes = form.select("input[name=$FIELD_BAN_TYPE][type=radio]")
                .map { Choice(it.`val`(), it.nextElementSibling()?.text()?.trim() ?: "", it.hasAttr("checked")) }

        return ModQueueForm(username, history, durations, banTypes, hiddenFields)
    }

    /** A selectable option in the form, e.g. a probation duration or ban type */
    data class Choice(val value: String, val label: String, val selected: Boolean)

    /**
     * The pertinent parts of a modqueue request page: the target [username] and punishment
     * [history], the available [durations] (probation) or [banTypes] (ban), and the form's
     * [hiddenFields] which need to be sent back on submission.
     */
    data class ModQueueForm(
            val username: String?,
            val history: String?,
            val durations: List<Choice>,
            val banTypes: List<Choice>,
            val hiddenFields: Map<String, String>
    )
}


/**
 * Submits a modqueue probation/ban request. [params] should hold the form's hidden fields plus
 * the user's input, as parsed from a [ModQueueFormRequest.ModQueueForm].
 */
class ModQueueSubmitRequest(context: Context, params: Map<String, String>) :
        AwfulRequest<Void?>(context, FUNCTION_MODQUEUE, isPostRequest = true) {

    init {
        params.forEach { (name, value) -> parameters.add(name, value) }
    }

    override fun handleResponse(doc: Document): Void? = null
}
