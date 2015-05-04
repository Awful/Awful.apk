package com.ferg.awfulapp.preferences.fragments;

import android.app.Dialog;
import android.graphics.Typeface;
import android.preference.Preference;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;

/**
 * Created by baka kaba on 04/05/2015.
 */
public class PostSettings extends SettingsFragment {

    {
        SETTINGS_XML_RES_ID = R.xml.postsettings;
        VERSION_DEPENDENT_SUMMARY_PREF_KEYS = new String[] {
                "inline_youtube"
        };

        SUBMENU_OPENING_KEYS = new String[] {
                "highlighting"
        };

        prefClickListeners.put(new FontSizeListener(), new String[] {
                "default_post_font_size_dip",
                "default_post_fixed_font_size_dip"
        });
    }

    @Override
    protected void onSetSummaries() {
        findPreference("default_post_font_size_dip")
                .setSummary(String.valueOf(mPrefs.postFontSizeDip));
        findPreference("default_post_fixed_font_size_dip")
                .setSummary(String.valueOf(mPrefs.postFixedFontSizeDip));
        findPreference("post_per_page2")
                .setSummary(String.valueOf(mPrefs.postPerPage));
    }

    /** Listener for the default post font size options */
    private class FontSizeListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final String prefKey = preference.getKey();
            final int minSize = Constants.MINIMUM_FONT_SIZE;
            final Dialog mFontSizeDialog = new Dialog(getActivity());

            mFontSizeDialog.setContentView(R.layout.font_size);
            if (prefKey.equals("default_post_font_size_dip")){
                mFontSizeDialog.setTitle(getString(R.string.default_font_size_dialog_title));
            }
            else if (prefKey.equals("default_post_fixed_font_size_dip")) {
                mFontSizeDialog.setTitle(getString(R.string.default_fixed_font_size_dialog_title));
            }
            final TextView mFontSizeText = (TextView) mFontSizeDialog.findViewById(R.id.fontSizeText);
            SeekBar bar = (SeekBar) mFontSizeDialog.findViewById(R.id.fontSizeBar);
            Button click = (Button) mFontSizeDialog.findViewById(R.id.fontSizeButton);

            click.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mFontSizeDialog.dismiss();
                }
            });

            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mPrefs.setIntegerPreference(prefKey, seekBar.getProgress()+minSize);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mFontSizeText.setText((progress+minSize)+ "  Get out");
                    mFontSizeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, (progress+minSize));
                }
            });
            if (prefKey.equals("default_post_font_size_dip")){
                bar.setProgress(mPrefs.postFontSizeDip-minSize);
            }
            else if (prefKey.equals("default_post_fixed_font_size_dip")) {
                bar.setProgress(mPrefs.postFixedFontSizeDip-minSize);
                mFontSizeText.setTypeface(Typeface.MONOSPACE);
            }
            else Log.w(TAG, "Tried to set font size for: " + prefKey + ", not a valid key!");

            mFontSizeText.setText((bar.getProgress()+minSize)+ "  Get out");
            mFontSizeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, (bar.getProgress()+minSize));
            mFontSizeDialog.show();
            return true;
        }
    }
}
