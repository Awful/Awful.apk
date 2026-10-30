package com.ferg.awfulapp.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.StringRes
import com.ferg.awfulapp.R

/**
 * Created by baka kaba on 14/08/2017.
 * 
 * 
 * A widget that displays a status message and an optional activity spinner, with a frog icon in the
 * background.
 * 
 * 
 * This is meant for use on blank areas where you need to explain there's no content, the user needs
 * to do something (like selecting an item in a master-detail layout), or show the current status and
 * activity (e.g. content is being fetched).
 */
class StatusFrog : RelativeLayout {
    lateinit var statusMessage: TextView

    lateinit var progressBar: ProgressBar

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }


    private fun init(context: Context, attrs: AttributeSet?) {
        val view = LayoutInflater.from(context).inflate(R.layout.status_frog, this, true)
        statusMessage = view.findViewById(R.id.status_message)
        progressBar = view.findViewById(R.id.status_progress_bar)

        // handle any custom XML attributes
        if (attrs != null) {
            val typedArray =
                context.theme.obtainStyledAttributes(attrs, R.styleable.StatusFrog, 0, 0)
            setStatusText(typedArray.getString(R.styleable.StatusFrog_status_message))
            showSpinner(typedArray.getBoolean(R.styleable.StatusFrog_show_spinner, false))
            typedArray.recycle()
        }
    }


    /**//////////////////////////////////////////////////////////////////////// */ // Update methods
    /**//////////////////////////////////////////////////////////////////////// */
    fun setStatusText(text: String?): StatusFrog {
        statusMessage.text = text ?: ""
        return this
    }

    fun setStatusText(@StringRes resId: Int): StatusFrog {
        return setStatusText(context.getString(resId))
    }

    /**
     * Display or hide the activity spinner.
     */
    fun showSpinner(show: Boolean): StatusFrog {
        progressBar.visibility = if (show) VISIBLE else INVISIBLE
        return this
    }
}
