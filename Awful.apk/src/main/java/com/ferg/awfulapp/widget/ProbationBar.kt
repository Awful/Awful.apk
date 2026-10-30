package com.ferg.awfulapp.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.ferg.awfulapp.R
import com.ferg.awfulapp.databinding.ProbationBarBinding
import com.ferg.awfulapp.preferences.AwfulPreferences.Companion.getInstance
import com.ferg.awfulapp.preferences.BooleanPreference
import java.text.DateFormat
import java.util.Date

/**
 * Created by baka kaba on 25/05/2016.
 * 
 * 
 * Probation bar widget, for dropping into UI that needs it.
 * 
 * 
 * Set the probation time with [.setProbation] to show or hide the widget, and set
 * a click listener with [.setListener] to handle user interaction.
 */
class ProbationBar : LinearLayout {
    lateinit var binding: ProbationBarBinding

    private var listener: Callbacks? = null

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        binding = ProbationBarBinding.inflate(LayoutInflater.from(context), this, true)
        binding.closeProbationBar.setOnClickListener {
            getInstance().setPreference(
                BooleanPreference.PROBATION_IGNORE,
                true
            )
        }
        binding.goToLC.setOnClickListener {
            if (listener != null) {
                listener!!.onProbationButtonClicked()
            }
        }
    }


    /**
     * Set the listener for user interaction callbacks, i.e. clicking the Leper Colony button
     * 
     * @param listener An optional callback listener
     */
    fun setListener(listener: Callbacks?) {
        this.listener = listener
    }


    /**
     * Display the probation notice for the provided deadline, or hide the probation bar.
     * 
     * @param probationTime the probation expiry timestamp, or null to hide the bar
     */
    fun setProbation(probationTime: Long?) {
        if (probationTime == null) {
            this.visibility = GONE
            return
        }
        this.visibility = VISIBLE
        val probeEnd = DateFormat.getDateTimeInstance().format(Date(probationTime))
        binding.probationMessage.text = String.format(
            binding.getRoot().resources.getString(R.string.probation_message), probeEnd
        )
    }

    interface Callbacks {
        /**
         * Called when the 'go to leper colony' button is clicked on the probation bar
         */
        fun onProbationButtonClicked()
    }
}
