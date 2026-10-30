package com.ferg.awfulapp.preferences.fragments

import android.app.Dialog
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.ferg.awfulapp.R
import com.ferg.awfulapp.preferences.FloatPreference
import com.ferg.awfulapp.util.AwfulUtils
import kotlin.math.roundToInt

/**
 * Created by baka kaba on 04/05/2015.
 */
class MiscSettings : SettingsFragment() {
    init {
        SETTINGS_XML_RES_ID = R.xml.miscsettings
        VALUE_SUMMARY_PREF_KEYS = intArrayOf(
            R.string.pref_key_orientation
        )
        prefClickListeners[P2RDistanceListener()] = intArrayOf(
            R.string.pref_key_pull_to_refresh_distance
        )
    }


    override val title: String
        get() = getString(R.string.prefs_misc)


    override fun initialiseSettings() {
        super.initialiseSettings()
        val tab = AwfulUtils.isTablet(requireActivity(), true)
        findPrefById(R.string.pref_key_page_layout)?.isEnabled = tab
        findPrefById(R.string.pref_key_transformer)?.isEnabled = !tab
    }


    override fun onSetSummaries() {
        // p2r amount summary
        var summary = getString(R.string.pull_to_refresh_distance_summary)
        summary += "\n" + (mPrefs?.p2rDistance!! * 100f).roundToInt().toString() + "%"
        summary += " of the screen's height"
        findPrefById(R.string.pref_key_pull_to_refresh_distance)!!.setSummary(summary)

        // Thread layout option
        val p = findPrefById(R.string.pref_key_page_layout) as ListPreference?
        if (p!!.isEnabled) {
            p.setSummary(p.getEntry())
        } else {
            p.setSummary(getString(R.string.page_layout_summary_disabled))
        }
    }


    /** Listener for the 'Pull-to-refresh distance' option  */
    private inner class P2RDistanceListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            val mP2RDistanceDialog = Dialog(requireActivity())

            mP2RDistanceDialog.setContentView(R.layout.p2rdistance)
            mP2RDistanceDialog.setTitle("Set Pull-to-refresh distance")

            val mP2RDistanceText =
                mP2RDistanceDialog.findViewById<View?>(R.id.p2rdistanceText) as TextView
            val bar = mP2RDistanceDialog.findViewById<View?>(R.id.p2rdistanceBar) as SeekBar
            val click = mP2RDistanceDialog.findViewById<View?>(R.id.p2rdistanceButton) as Button

            click.setOnClickListener { mP2RDistanceDialog.dismiss() }

            bar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    val distanceFloat = seekBar.progress.toFloat()
                    mPrefs?.setPreference(FloatPreference.P2R_DISTANCE, (distanceFloat / 100))
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    mP2RDistanceText.text = "${progress}%${if (progress !in 20..75) " (not recommended)" else ""}"
                }
            })
            bar.progress = (mPrefs?.p2rDistance!! * 100).roundToInt()
            mP2RDistanceText.text = "${bar.progress}%${if (bar.progress !in 20..75) " (not recommended)" else ""}"
            mP2RDistanceDialog.show()
            return true
        }
    }
}
