package com.ferg.awfulapp.preferences.fragments;

import android.app.Dialog;
import android.os.Build;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.preferences.Keys;
import com.ferg.awfulapp.util.AwfulUtils;

/**
 * Created by baka kaba on 04/05/2015.
 */
public class MiscSettings extends SettingsFragment {

    {
        SETTINGS_XML_RES_ID = R.xml.miscsettings;
        VALUE_SUMMARY_PREF_KEYS = new int[] {
                R.string.pref_key_orientation
        };
        VERSION_DEPENDENT_SUMMARY_PREF_KEYS = new int[] {
                R.string.pref_key_disable_gifs,
                R.string.pref_key_immersion_mode,
                R.string.pref_key_transformer
        };
        prefClickListeners.put(new P2RDistanceListener(), new int[] {
                R.string.pref_key_pull_to_refresh_distance
        });
    }


    @Override
    protected void initialiseSettings() {
        super.initialiseSettings();

        findPrefById(R.string.pref_key_disable_gifs).setEnabled(true);
        findPrefById(R.string.pref_key_immersion_mode).setEnabled(AwfulUtils.isKitKat());
        boolean tab = AwfulUtils.isTablet(getActivity(), true);
        boolean jellybeanMr1 = AwfulUtils.isAtLeast(Build.VERSION_CODES.JELLY_BEAN_MR1);
        findPrefById(R.string.pref_key_page_layout).setEnabled(tab);
        findPrefById(R.string.pref_key_transformer).setEnabled(jellybeanMr1 && !tab);
//        if(!tab){
//            findPreference("page_layout").setSummary(getString(R.string.page_layout_summary_disabled));
//        }
    }


    @Override
    protected void onSetSummaries() {
        // p2r amount summary
        String summary = getString(R.string.pull_to_refresh_distance_summary);
        summary += "\n" + String.valueOf(Math.round(mPrefs.p2rDistance * 100.f)) + "%";
        summary += " of the screen's height";
        findPrefById(R.string.pref_key_pull_to_refresh_distance).setSummary(summary);

        // Thread layout option
        ListPreference p = (ListPreference) findPrefById(R.string.pref_key_page_layout);
        if (p.isEnabled()) {
            p.setSummary(p.getEntry());
        } else {
            p.setSummary(getString(R.string.page_layout_summary_disabled));
        }
    }



    /** Listener for the 'Pull-to-refresh distance' option */
    private class P2RDistanceListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final Dialog mP2RDistanceDialog = new Dialog(getActivity());

            mP2RDistanceDialog.setContentView(R.layout.p2rdistance);
            mP2RDistanceDialog.setTitle("Set Pull-to-refresh distance");

            final TextView mP2RDistanceText = (TextView) mP2RDistanceDialog.findViewById(R.id.p2rdistanceText);
            SeekBar bar = (SeekBar) mP2RDistanceDialog.findViewById(R.id.p2rdistanceBar);
            Button click = (Button) mP2RDistanceDialog.findViewById(R.id.p2rdistanceButton);

            click.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mP2RDistanceDialog.dismiss();
                }
            });

            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    float distanceFloat = seekBar.getProgress();
                    mPrefs.setPreference(Keys.P2R_DISTANCE, (distanceFloat / 100));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mP2RDistanceText.setText(progress+ "%"+((progress<20||progress>75)?" (not recommended)":""));
                }
            });
            bar.setProgress(Math.round(mPrefs.p2rDistance*100));
            mP2RDistanceText.setText(bar.getProgress()+ "%"+((bar.getProgress()<20||bar.getProgress()>75)?" (not recommended)":""));
            mP2RDistanceDialog.show();
            return true;
        }
    }
}
