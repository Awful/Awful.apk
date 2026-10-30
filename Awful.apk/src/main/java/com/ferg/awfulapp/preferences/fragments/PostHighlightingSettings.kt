package com.ferg.awfulapp.preferences.fragments

import com.ferg.awfulapp.R

/**
 * Created by baka kaba on 07/05/2015.
 */
class PostHighlightingSettings : SettingsFragment() {
    init {
        SETTINGS_XML_RES_ID = R.xml.post_highlighting_settings
    }


    override val title: String
        get() = getString(R.string.highlighting_settings_title)
}
