package com.ferg.awfulapp.preferences.fragments;

import android.support.annotation.NonNull;

import com.ferg.awfulapp.R;

/**
 * Created by baka kaba on 07/05/2015.
 */
public class PostHighlightingSettings extends SettingsFragment {

    {
        SETTINGS_XML_RES_ID = R.xml.post_highlighting_settings;
    }


    @NonNull
    @Override
    public String getTitle() {
        return getString(R.string.highlighting_settings_title);
    }
}
