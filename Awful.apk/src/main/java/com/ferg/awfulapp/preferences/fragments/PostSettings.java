package com.ferg.awfulapp.preferences.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.preference.Preference;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.Keys;

/**
 * Created by baka kaba on 04/05/2015.
 */
public class PostSettings extends SettingsFragment {

    {
        SETTINGS_XML_RES_ID = R.xml.postsettings;
        VERSION_DEPENDENT_SUMMARY_PREF_KEYS = new int[] {
                R.string.pref_key_inline_youtube
        };

        SUBMENU_OPENING_KEYS = new int[] {
                R.string.pref_key_highlighting_menu_item
        };

        prefClickListeners.put(new FontSizeListener(), new int[] {
                R.string.pref_key_post_font_size_dip,
                R.string.pref_key_post_fixed_font_size_dip
        });

        prefClickListeners.put(new PostsPerPageListener(), new int[] {
                R.string.pref_key_post_per_page
        });
    }

    @Override
    protected void onSetSummaries() {
        findPrefById(R.string.pref_key_post_font_size_dip)
                .setSummary(String.valueOf(mPrefs.postFontSizeDip));
        findPrefById(R.string.pref_key_post_fixed_font_size_dip)
                .setSummary(String.valueOf(mPrefs.postFixedFontSizeDip));
        findPrefById(R.string.pref_key_post_per_page)
                .setSummary(String.valueOf(mPrefs.postPerPage));
    }

    /**
     * Listener for the default post font size options
     */
    private class FontSizeListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final String prefKey = preference.getKey();
            final String FONT_SIZE_KEY = getString(R.string.pref_key_post_font_size_dip);
            final String FIXED_FONT_SIZE_KEY = getString(R.string.pref_key_post_fixed_font_size_dip);
            final String SIZE_PICKER_FORMAT_STRING = getString(R.string.font_size_picker_format_string);
            final int    MIN_SIZE = Constants.MINIMUM_FONT_SIZE;
            final Dialog mFontSizeDialog = new Dialog(getActivity());

            mFontSizeDialog.setContentView(R.layout.font_size);
            if (prefKey.equals(FONT_SIZE_KEY)){
                mFontSizeDialog.setTitle(getString(R.string.default_font_size_dialog_title));
            }
            else if (prefKey.equals(FIXED_FONT_SIZE_KEY)) {
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
                    if (prefKey.equals(FONT_SIZE_KEY)) {
                        mPrefs.setPreference(Keys.POST_FONT_SIZE_DIP, seekBar.getProgress() + MIN_SIZE);
                    } else if (prefKey.equals(FIXED_FONT_SIZE_KEY)) {
                        mPrefs.setPreference(Keys.POST_FIXED_FONT_SIZE_DIP, seekBar.getProgress() + MIN_SIZE);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int selectedSize = progress + MIN_SIZE;
                    mFontSizeText.setText(String.format(SIZE_PICKER_FORMAT_STRING, selectedSize));
                    mFontSizeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, selectedSize);
                }
            });

            if (prefKey.equals(FONT_SIZE_KEY)){
                bar.setProgress(mPrefs.postFontSizeDip - MIN_SIZE);
            }
            else if (prefKey.equals(FIXED_FONT_SIZE_KEY)) {
                bar.setProgress(mPrefs.postFixedFontSizeDip - MIN_SIZE);
                mFontSizeText.setTypeface(Typeface.MONOSPACE);
            }
            else Log.w(TAG, "Tried to set font size for: " + prefKey + ", not a valid key!");

            int selectedSize = bar.getProgress() + MIN_SIZE;
            mFontSizeText.setText(String.format(SIZE_PICKER_FORMAT_STRING, selectedSize));
            mFontSizeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, selectedSize);
            mFontSizeDialog.show();
            return true;
        }
    }

    private class PostsPerPageListener implements Preference.OnPreferenceClickListener {

        @Override
        public boolean onPreferenceClick(final Preference preference) {

            @SuppressLint("InflateParams")
            final View pickerView = getActivity().getLayoutInflater().inflate(R.layout.number_picker, null);
            final NumberPicker  picker = (NumberPicker) pickerView.findViewById(R.id.pagePicker);
            final Button        minButton = (Button) pickerView.findViewById(R.id.min);
            final Button        maxButton = (Button) pickerView.findViewById(R.id.max);
            final int minPages = 1;
            final int maxPages = Constants.ITEMS_PER_PAGE;

            picker.setMinValue(minPages);
            picker.setMaxValue(maxPages);
            picker.setValue(mPrefs.postPerPage);
            minButton.setText(String.format("%d", minPages));
            maxButton.setText(String.format("%d", maxPages));

            View.OnClickListener buttonsListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v == minButton) {
                        picker.setValue(minPages);
                    } else if (v == maxButton) {
                        picker.setValue(maxPages);
                    }
                }
            };
            minButton.setOnClickListener(buttonsListener);
            maxButton.setOnClickListener(buttonsListener);

            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.setting_posts_per_page)
                    .setView(pickerView)
                    .setPositiveButton(R.string.alert_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface aDialog, int aWhich) {
                                    String key = preference.getKey();
                                    if (key.equals(getString(R.string.pref_key_post_per_page))) {
                                        mPrefs.setPreference(Keys.POST_PER_PAGE, picker.getValue());
                                    }
                                }
                            })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        }
    }
}
