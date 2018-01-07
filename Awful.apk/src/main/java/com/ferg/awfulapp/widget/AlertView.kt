package com.ferg.awfulapp.widget

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.app.FragmentActivity
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.ferg.awfulapp.R
import com.ferg.awfulapp.util.AwfulError
import com.ferg.awfulapp.util.hide
import com.ferg.awfulapp.util.show


/**
 * Displays a popup alert as a custom view in a toast.
 *
 * Uses the builder pattern to be friendly with java.
 *
 * TODO: Once all calls to this class are made in kotlin, all of the set*() methods can be replaced with a single builder method with nullable values
 * FOR THE TIME BEING this method will not work because of this error, but I'd like to keep a note if that changes
 * [stackoverflow reference](https://stackoverflow.com/questions/47016590/stringres-drawableres-layoutres-and-so-on-android-annotations-lint-check-wi)
 */

class AlertView(private val activity: FragmentActivity?) {

    private var root: ViewGroup? = null
    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var iconView: ImageView


    private var title: String? = null
    private var subtitle: String? = null
    @DrawableRes
    private var iconResId = 0
    private var animation: Animation? = null


    fun setTitle(@StringRes title: Int): AlertView {
        activity?.getString(title)?.let {
            this.title = it
        }
        return this
    }

    fun setTitle(title: String?): AlertView {
        this.title = title
        return this
    }

    fun setSubtitle(@StringRes subtitle: Int): AlertView {
        activity?.getString(subtitle)?.let {
            this.subtitle = it
        }
        return this
    }

    fun setSubtitle(subtitle: String?): AlertView {
        this.subtitle = subtitle
        return this
    }

    fun setIcon(@DrawableRes iconResId: Int): AlertView {
        this.iconResId = iconResId
        return this
    }

    fun setIconAnimation(animation: Animation?): AlertView {
        this.animation = animation
        return this
    }

    fun show() {
        activity?.runOnUiThread {
            displayAlert(title, subtitle, iconResId, animation)
        }
    }

    fun show(error: AwfulError) {
        activity?.runOnUiThread {
            displayAlert(error.message, error.subMessage, error.iconResource, error.iconAnimation)
        }
    }


    // inflates views exactly once. yay!
    private fun inflateView() {
        if(root == null) {
            root = activity?.layoutInflater?.inflate(R.layout.alert_popup,
                    activity.findViewById(R.id.alert_popup_root)) as ViewGroup
            root?.let {
                titleView = it.findViewById(R.id.popup_title)
                subtitleView = it.findViewById(R.id.popup_subtitle)
                iconView = it.findViewById(R.id.popup_icon)
            }
        }

        titleView.hide()
        subtitleView.hide()
        iconView.hide()
    }


    /**
      * Builds the alert and sets the view content to the input parameters.
      * If there is no content for a corresponding view, it remains hidden
      * The purpose of the [clear] is to reset all the values so the next call will be a fresh start
      * This will allow the view to be reused to avoid unecessary multiple inflations and allocations
      * This may be unecessary optimization for such a simple view, but since inflation is one of the
      * most expensive operations in the android OS we might as well avoid it
      * @param title
      * @param subtitle
      * @param iconRes
      * @param animate
     */
    private fun displayAlert(title: String?, subtitle: String?, iconRes: Int, animate: Animation?) {

        inflateView()

        if (!title.isNullOrEmpty()) {
            titleView.show()
            titleView.text = title
        }

        if (!subtitle.isNullOrEmpty()) {
            subtitleView.show()
            subtitleView.text = subtitle
        }

        if (iconRes != 0) {
            iconView.show()
            iconView.setImageResource(iconRes)
            animate?.let { iconView.startAnimation(animate) }
        }

        with(Toast(activity)) {
            setGravity(Gravity.CENTER_VERTICAL, 0, 0)
            duration = Toast.LENGTH_LONG
            view = root
            show()
        }

        clear()
    }

    // reset all the data so the next toast starts with a blank slate
    private fun clear() {
        title = null
        subtitle = null
        animation = null
        iconResId = 0
    }


    // uncomment for fun when kotlin gets fixed
    /*
    fun show(@StringRes titleRes: Int = 0,
              titleString: String? = null,
              @StringRes subtitleRes: Int = 0,
              subtitleString: String? = null,
              @DrawableRes iconRes: Int = 0,
              animation: Animation? = null)
    {

        val title = if(titleRes != 0) activity?.getString(titleRes) else titleString
        val subtitle = if(subtitleRes != 0) activity?.getString(subtitleRes) else subtitleString

        activity?.runOnUiThread {
            displayAlert(title, subtitle, iconRes, animation)
        }
    }
    */
}