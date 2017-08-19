package com.ferg.awfulapp.preferences.fragments;

import android.support.annotation.NonNull;

import com.ferg.awfulapp.R;

/**
 * Created by baka kaba on 04/05/2015.
 */
public class ThreadSettings extends SettingsFragment {

    {
        SETTINGS_XML_RES_ID = R.xml.threadinfosettings;
    }

    @NonNull
    @Override
    public String getTitle() {
        return getString(R.string.thread_settings);
    }
}