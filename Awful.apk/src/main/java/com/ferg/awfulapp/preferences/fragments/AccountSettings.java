package com.ferg.awfulapp.preferences.fragments;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.preference.Preference;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.FeatureRequest;
import com.ferg.awfulapp.task.ProfileRequest;

/**
 * Created by baka kaba on 04/05/2015.
 */
public class AccountSettings extends SettingsFragment {

    {
        SETTINGS_XML_RES_ID = R.xml.accountsettings;
        prefClickListeners.put(new FeaturesListener(), new String[] {
                "account_features"
        });
    }

    @Override
    protected void onSetSummaries() {
        findPreference("username").setSummary(mPrefs.username);
        //Set summary for the 'Refresh account options' option
        String platinum = "Platinum: " + ((mPrefs.hasPlatinum) ? "Yes" : "No");
        String archives = "Archives: " + ((mPrefs.hasArchives) ? "Yes" : "No");
        String noAds    = "No Ads: " + ((mPrefs.hasNoAds) ? "Yes" : "No");
        String separator = " "+" "+" "+" ";
        String summaryText = TextUtils.join(separator, new String[] {platinum, archives, noAds});
        findPreference("account_features").setSummary(summaryText);
    }


    private class FeaturesListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final Dialog dialog = ProgressDialog.show(getActivity(), "Loading", "Fetching Account Features", true);
            NetworkUtils.queueRequest(new FeatureRequest(getActivity())
                    .build(null, new AwfulRequest.AwfulResultCallback<Void>() {
                        @Override
                        public void success(Void result) {
                            dialog.dismiss();
                            setSummaries();
                            NetworkUtils.queueRequest(new ProfileRequest(getActivity()).build(null, null));
                        }

                        @Override
                        public void failure(VolleyError error) {
                            dialog.dismiss();
                            Toast.makeText(getActivity(), "An error occured", Toast.LENGTH_LONG).show();
                        }
                    }));
            return true;
        }
    }
}
