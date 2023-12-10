package com.ferg.awfulapp.preferences.fragments;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.Keys;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.FeatureRequest;
import com.ferg.awfulapp.task.RefreshUserProfileRequest;

/**
 * Created by baka kaba on 04/05/2015.
 */
public class AccountSettings extends SettingsFragment {

    {
        SETTINGS_XML_RES_ID = R.xml.accountsettings;
        prefClickListeners.put(new FeaturesListener(), new int[] {
                R.string.pref_key_account_features_menu_item
        });
        prefClickListeners.put(new ImgurListener(), new int[] {
                R.string.pref_key_account_imgur_menu_item
        });
    }


    @NonNull
    @Override
    public String getTitle() {
        return getString(R.string.prefs_account);
    }

    @Override
    public void onResume() {
        super.onResume();
        setSummaries();
    }

    @Override
    protected void onSetSummaries() {
        findPrefById(R.string.pref_key_username).setSummary(mPrefs.username);
        //Set summary for the 'Refresh account options' option
        String platinum = "Platinum: " + ((mPrefs.hasPlatinum) ? "Yes" : "No");
        String archives = "Archives: " + ((mPrefs.hasArchives) ? "Yes" : "No");
        String noAds    = "No Ads: " + ((mPrefs.hasNoAds) ? "Yes" : "No");
        String separator = " "+" "+" "+" ";
        String summaryText = TextUtils.join(separator, new String[] {platinum, archives, noAds});
        findPrefById(R.string.pref_key_account_features_menu_item).setSummary(summaryText);
        if (mPrefs.imgurAccount != null) {
            findPrefById(R.string.pref_key_account_imgur_menu_item).setSummary("user: " + mPrefs.imgurAccount);
        }
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
                            NetworkUtils.queueRequest(new RefreshUserProfileRequest(getActivity()).build(null, null));
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

    private class ImgurListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (mPrefs.imgurAccount != null) {
                new AlertDialog.Builder(getActivity())
                        .setTitle("Remove account?")
                        .setPositiveButton(R.string.confirm,
                                (dialog, button) -> {
                                    mPrefs.setPreference(Keys.IMGUR_ACCOUNT_TOKEN, (String) null);
                                    mPrefs.setPreference(Keys.IMGUR_REFRESH_TOKEN, (String) null);
                                    mPrefs.setPreference(Keys.IMGUR_ACCOUNT, (String) null);
                                    mPrefs.setPreference(Keys.IMGUR_TOKEN_EXPIRES, 0L);
                                    findPrefById(R.string.pref_key_account_imgur_menu_item).setSummary(R.string.imgur_account_summary);
                                })
                        .setNegativeButton(R.string.cancel, (dialog, button) -> {
                        })
                        .show();
            } else {
                final String AUTHORIZATION_URL = "https://api.imgur.com/oauth2/authorize";
                Uri imgurLogin = Uri.parse(AUTHORIZATION_URL).buildUpon()
                        .appendQueryParameter("client_id", getResources().getString(R.string.imgur_api_client_id))
                        .appendQueryParameter("response_type", "token")
                        .build();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, imgurLogin);
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(browserIntent);
            }
            return true;
        }
    }
}
