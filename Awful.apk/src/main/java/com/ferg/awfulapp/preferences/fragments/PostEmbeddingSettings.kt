package com.ferg.awfulapp.preferences.fragments

import com.ferg.awfulapp.R

/**
 * Created by baka kaba on 26/04/2017.
 * 
 * Settings fragment for embedding options (videos and the like).
 */
class PostEmbeddingSettings : SettingsFragment() {
    init {
        SETTINGS_XML_RES_ID = R.xml.post_embedding_settings
    }


    override val title: String
        get() = getString(R.string.embedding_settings_title)
}
