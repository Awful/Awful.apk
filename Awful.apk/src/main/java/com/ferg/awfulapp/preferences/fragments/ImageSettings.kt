package com.ferg.awfulapp.preferences.fragments

import com.ferg.awfulapp.R

/**
 * Created by baka kaba on 04/05/2015.
 */
class ImageSettings : SettingsFragment() {
    init {
        SETTINGS_XML_RES_ID = R.xml.imagesettings
        VALUE_SUMMARY_PREF_KEYS = intArrayOf(
            R.string.pref_key_imgur_thumbnails
        )
    }


    override val title: String
        get() = getString(R.string.image_settings)
}
