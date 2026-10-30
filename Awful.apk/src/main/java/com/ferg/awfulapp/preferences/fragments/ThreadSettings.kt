package com.ferg.awfulapp.preferences.fragments

import com.ferg.awfulapp.R

/**
 * Created by baka kaba on 04/05/2015.
 */
class ThreadSettings : SettingsFragment() {
    init {
        SETTINGS_XML_RES_ID = R.xml.threadinfosettings
    }

    override val title: String
        get() = getString(R.string.thread_settings)
}