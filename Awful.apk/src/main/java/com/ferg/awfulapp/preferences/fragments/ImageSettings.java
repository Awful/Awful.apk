package com.ferg.awfulapp.preferences.fragments;

import android.support.annotation.NonNull;

import com.ferg.awfulapp.R;

/**
 * Created by baka kaba on 04/05/2015.
 */
public class ImageSettings extends SettingsFragment {

    {
        SETTINGS_XML_RES_ID = R.xml.imagesettings;
        VALUE_SUMMARY_PREF_KEYS = new int[] {
                R.string.pref_key_imgur_thumbnails
        };
    }


    @NonNull
    @Override
    public String getTitle() {
        return getString(R.string.image_settings);
    }
}
