package com.ferg.awfulapp.preferences.fragments;

import androidx.annotation.NonNull;

import com.ferg.awfulapp.R;

/**
 * Created by baka kaba on 26/04/2017.
 *
 * Settings fragment for embedding options (videos and the like).
 */
public class PostEmbeddingSettings extends SettingsFragment {

    {
        SETTINGS_XML_RES_ID = R.xml.post_embedding_settings;
    }


    @NonNull
    @Override
    public String getTitle() {
        return getString(R.string.embedding_settings_title);
    }
}
