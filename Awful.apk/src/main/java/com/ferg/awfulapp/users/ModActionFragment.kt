package com.ferg.awfulapp.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import com.android.volley.VolleyError
import com.ferg.awfulapp.AwfulFragment
import com.ferg.awfulapp.NavigationEvent
import com.ferg.awfulapp.NavigationEvent.Companion.parse
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants.PARAM_SUBMIT
import com.ferg.awfulapp.task.AwfulRequest
import com.ferg.awfulapp.task.ModQueueFormRequest
import com.ferg.awfulapp.task.ModQueueFormRequest.ModQueueForm
import com.ferg.awfulapp.task.ModQueueSubmitRequest
import com.ferg.awfulapp.util.bind

/**
 * Displays a modqueue probation/ban request page as a native form, and submits the request.
 *
 * The page to load is described by a [NavigationEvent.ModQueueRequest] parsed from the activity's
 * intent. Submitting the request just waits for the form post to complete (without displaying the
 * site's response) and closes the screen, so the user lands back where they came from.
 */
class ModActionFragment : AwfulFragment() {

    private val formContainer: View by bind(R.id.form_container)
    private val userName: TextView by bind(R.id.user_name)
    private val userHistory: TextView by bind(R.id.user_history)
    private val durationSection: View by bind(R.id.duration_section)
    private val durationSpinner: Spinner by bind(R.id.duration_spinner)
    private val banTypeSection: View by bind(R.id.ban_type_section)
    private val banTypeGroup: RadioGroup by bind(R.id.ban_type_group)
    private val reasonInput: EditText by bind(R.id.reason_input)
    private val notesInput: EditText by bind(R.id.notes_input)
    private val submitButton: Button by bind(R.id.submit_button)

    private lateinit var request: NavigationEvent.ModQueueRequest
    private var form: ModQueueForm? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflateView(R.layout.modqueue_request, container, inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        submitButton.setOnClickListener { submit() }
    }

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        val event = activity?.intent?.parse() as? NavigationEvent.ModQueueRequest
        if (event == null) {
            activity?.finish()
            return
        }
        request = event
        loadForm()
    }

    override fun getTitle(): String? =
            if (::request.isInitialized) {
                getString(if (request.ban) R.string.mod_ban_request_title else R.string.mod_probation_request_title)
            } else null

    private fun loadForm() {
        activity?.let { context ->
            queueRequest(ModQueueFormRequest(context, request.ban, request.userId, request.postId, request.threadId)
                    .build(this, object : AwfulRequest.AwfulResultCallback<ModQueueForm> {

                        override fun success(result: ModQueueForm) {
                            form = result
                            showForm(result)
                        }

                        override fun failure(error: VolleyError?) {}
                    }))
        }
    }

    private fun showForm(form: ModQueueForm) {
        userName.text = getString(R.string.mod_request_user, form.username ?: "")
        userHistory.text = form.history
        userHistory.visibility = if (form.history.isNullOrEmpty()) View.GONE else View.VISIBLE

        if (request.ban) {
            banTypeSection.visibility = View.VISIBLE
            banTypeGroup.removeAllViews()
            form.banTypes.forEach { choice ->
                val button = RadioButton(requireContext()).apply {
                    id = View.generateViewId()
                    text = choice.label
                    tag = choice.value
                }
                banTypeGroup.addView(button)
                if (choice.selected) banTypeGroup.check(button.id)
            }
        } else {
            durationSection.visibility = View.VISIBLE
            durationSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, form.durations.map { it.label })
                    .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            durationSpinner.setSelection(form.durations.indexOfFirst { it.selected }.coerceAtLeast(0))
        }
        formContainer.visibility = View.VISIBLE
    }

    private fun submit() {
        val form = form ?: return
        val reason = reasonInput.text.toString().trim()
        if (reason.isEmpty()) {
            reasonInput.error = getString(R.string.mod_request_reason_required)
            return
        }
        val choice = if (request.ban) {
            val checked = banTypeGroup.findViewById<RadioButton>(banTypeGroup.checkedRadioButtonId) ?: return
            ModQueueFormRequest.FIELD_BAN_TYPE to checked.tag as String
        } else {
            val selected = form.durations.getOrNull(durationSpinner.selectedItemPosition) ?: return
            ModQueueFormRequest.FIELD_DURATION to selected.value
        }
        val params = form.hiddenFields + choice + mapOf(
                ModQueueFormRequest.FIELD_REASON to reason,
                ModQueueFormRequest.FIELD_NOTES to notesInput.text.toString(),
                PARAM_SUBMIT to "Submit"
        )

        submitButton.isEnabled = false
        activity?.let { context ->
            queueRequest(ModQueueSubmitRequest(context, params)
                    .build(this, object : AwfulRequest.AwfulResultCallback<Void?> {

                        override fun success(result: Void?) {
                            makeToast(if (request.ban) R.string.mod_request_ban_queued else R.string.mod_request_probation_queued)
                            activity?.finish()
                        }

                        override fun failure(error: VolleyError?) {
                            submitButton.isEnabled = true
                        }
                    }))
        }
    }
}
