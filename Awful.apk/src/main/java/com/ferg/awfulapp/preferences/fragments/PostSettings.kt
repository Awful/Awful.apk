package com.ferg.awfulapp.preferences.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.preference.Preference
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.preferences.IntPreference
import com.ferg.awfulapp.widget.MinMaxNumberPicker

/**
 * Created by baka kaba on 04/05/2015.
 * 
 * Settings fragment for the Posts settings submenu.
 */
class PostSettings : SettingsFragment() {
    init {
        SETTINGS_XML_RES_ID = R.xml.postsettings

        SUBMENU_OPENING_KEYS = intArrayOf(
            R.string.pref_key_highlighting_menu_item,
            R.string.pref_key_embedding_menu_item
        )

        prefClickListeners[FontSizeListener()] = intArrayOf(
            R.string.pref_key_post_font_size_sp,
            R.string.pref_key_post_fixed_font_size_sp
        )

        prefClickListeners[PostsPerPageListener()] = intArrayOf(
            R.string.pref_key_post_per_page
        )
    }


    override val title: String
        get() = getString(R.string.prefs_post_display)

    override fun onSetSummaries() {
        findPrefById(R.string.pref_key_post_font_size_sp)!!
            .setSummary(mPrefs!!.postFontSizeSp.toString())
        findPrefById(R.string.pref_key_post_fixed_font_size_sp)!!
            .setSummary(mPrefs!!.postFixedFontSizeSp.toString())
        findPrefById(R.string.pref_key_post_per_page)!!
            .setSummary(mPrefs!!.postPerPage.toString())
    }

    /**
     * Listener for the default post font size options
     */
    private inner class FontSizeListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            val prefKey = preference.key
            val FONT_SIZE_KEY = getString(R.string.pref_key_post_font_size_sp)
            val FIXED_FONT_SIZE_KEY = getString(R.string.pref_key_post_fixed_font_size_sp)
            val SIZE_PICKER_FORMAT_STRING = getString(R.string.font_size_picker_format_string)
            val MIN_SIZE = Constants.MINIMUM_FONT_SIZE_SP
            val mFontSizeDialog = Dialog(requireActivity())

            mFontSizeDialog.setContentView(R.layout.font_size)
            if (prefKey == FONT_SIZE_KEY) {
                mFontSizeDialog.setTitle(getString(R.string.default_font_size_dialog_title))
            } else if (prefKey == FIXED_FONT_SIZE_KEY) {
                mFontSizeDialog.setTitle(getString(R.string.default_fixed_font_size_dialog_title))
            }
            val mFontSizeText = mFontSizeDialog.findViewById<View?>(R.id.fontSizeText) as TextView
            val bar = mFontSizeDialog.findViewById<View?>(R.id.fontSizeBar) as SeekBar
            val click = mFontSizeDialog.findViewById<View?>(R.id.fontSizeButton) as Button

            click.setOnClickListener { _: View? -> mFontSizeDialog.dismiss() }

            bar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    if (prefKey == FONT_SIZE_KEY) {
                        mPrefs?.setPreference(
                            IntPreference.POST_FONT_SIZE_SP,
                            seekBar.progress + MIN_SIZE
                        )
                    } else if (prefKey == FIXED_FONT_SIZE_KEY) {
                        mPrefs?.setPreference(
                            IntPreference.POST_FIXED_FONT_SIZE_SP,
                            seekBar.progress + MIN_SIZE
                        )
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val selectedSize = progress + MIN_SIZE
                    mFontSizeText.text = String.format(SIZE_PICKER_FORMAT_STRING, selectedSize)
                    mFontSizeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, selectedSize.toFloat())
                }
            })

            when (prefKey) {
                FONT_SIZE_KEY -> {
                    bar.progress = mPrefs!!.postFontSizeSp - MIN_SIZE
                }
                FIXED_FONT_SIZE_KEY -> {
                    bar.progress = mPrefs!!.postFixedFontSizeSp - MIN_SIZE
                    mFontSizeText.typeface = Typeface.MONOSPACE
                }
                else -> Log.w(TAG, "Tried to set font size for: " + prefKey + ", not a valid key!")
            }

            val selectedSize = bar.progress + MIN_SIZE
            mFontSizeText.text = String.format(SIZE_PICKER_FORMAT_STRING, selectedSize)
            mFontSizeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, selectedSize.toFloat())
            mFontSizeDialog.show()
            return true
        }
    }

    private inner class PostsPerPageListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            val minPages = 1
            val maxPages = Constants.ITEMS_PER_PAGE

            MinMaxNumberPicker(
                requireActivity(),
                minPages,
                maxPages,
                mPrefs?.postPerPage ?: 40,
                getString(R.string.setting_posts_per_page),
             object: MinMaxNumberPicker.ResultListener {
                override fun onButtonPressed(button: Int, resultValue: Int) {
                    if (button == DialogInterface.BUTTON_POSITIVE) {
                        val key = preference.key
                        if (key == getString(R.string.pref_key_post_per_page)) {
                            mPrefs?.setPreference(IntPreference.POST_PER_PAGE, resultValue)
                        }
                    }
                }
            }).show()

            return true
        }
    }
}
