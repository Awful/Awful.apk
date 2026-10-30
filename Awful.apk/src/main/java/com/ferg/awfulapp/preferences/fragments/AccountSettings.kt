package com.ferg.awfulapp.preferences.fragments

import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.android.volley.VolleyError
import com.ferg.awfulapp.AwfulActivity
import com.ferg.awfulapp.R
import com.ferg.awfulapp.network.NetworkUtils.queueRequest
import com.ferg.awfulapp.task.AwfulRequest.AwfulResultCallback
import com.ferg.awfulapp.task.FeatureRequest
import com.ferg.awfulapp.task.RefreshUserProfileRequest
import androidx.core.net.toUri
import com.ferg.awfulapp.preferences.LongPreference
import com.ferg.awfulapp.preferences.StringPreference

/**
 * Created by baka kaba on 04/05/2015.
 */
class AccountSettings : SettingsFragment() {
    init {
        SETTINGS_XML_RES_ID = R.xml.accountsettings
        prefClickListeners[FeaturesListener()] = intArrayOf(
            R.string.pref_key_account_features_menu_item
        )
        prefClickListeners[ImgurListener()] = intArrayOf(
            R.string.pref_key_account_imgur_menu_item
        )
    }


    override val title: String
        get() = getString(R.string.prefs_account)

    override fun onResume() {
        super.onResume()
        setSummaries()
    }

    override fun onSetSummaries() {
        mPrefs?.let {
            findPrefById(R.string.pref_key_username)?.setSummary(it.username)
            //Set summary for the 'Refresh account options' option
            val platinum = "Platinum: " + (if (it.hasPlatinum) "Yes" else "No")
            val archives = "Archives: " + (if (it.hasArchives) "Yes" else "No")
            val noAds = "No Ads: " + (if (it.hasNoAds) "Yes" else "No")
            val summaryText = TextUtils.join("    ", arrayOf(platinum, archives, noAds))
            findPrefById(R.string.pref_key_account_features_menu_item)?.setSummary(summaryText)
            if (it.imgurAccount != null) {
                findPrefById(R.string.pref_key_account_imgur_menu_item)?.setSummary("user: " + it.imgurAccount)
            }
        }
    }


    private inner class FeaturesListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            val dialog: Dialog = ProgressDialog.show(activity, "Loading", "Fetching Account Features", true)
            (activity as AwfulActivity).setPreferredFont(dialog.findViewById(android.R.id.title))
            queueRequest(
                FeatureRequest(requireActivity())
                    .build(null, object : AwfulResultCallback<Void?> {
                        override fun success(result: Void?) {
                            dialog.dismiss()
                            setSummaries()
                            queueRequest(
                                RefreshUserProfileRequest(requireActivity()).build(
                                    null,
                                    null
                                )
                            )
                        }

                        override fun failure(error: VolleyError?) {
                            dialog.dismiss()
                            Toast.makeText(activity, "An error occured", Toast.LENGTH_LONG)
                                .show()
                        }
                    })
            )
            return true
        }
    }

    private inner class ImgurListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            mPrefs?.let {
                if (it.imgurAccount != null) {
                    AlertDialog.Builder(requireActivity())
                        .setTitle("Remove account?")
                        .setPositiveButton(
                            R.string.confirm
                        ) { _: DialogInterface?, _: Int ->
                            it.setPreference(StringPreference.IMGUR_ACCOUNT_TOKEN, null as String?)
                            it.setPreference(StringPreference.IMGUR_REFRESH_TOKEN, null as String?)
                            it.setPreference(StringPreference.IMGUR_ACCOUNT, null as String?)
                            it.setPreference(LongPreference.IMGUR_TOKEN_EXPIRES, 0L)
                            findPrefById(R.string.pref_key_account_imgur_menu_item)?.setSummary(R.string.imgur_account_summary)
                        }
                        .setNegativeButton(
                            R.string.cancel
                        ) { _: DialogInterface?, _: Int -> }
                        .show()
                } else {
                    val AUTHORIZATION_URL = "https://api.imgur.com/oauth2/authorize"
                    val imgurLogin = AUTHORIZATION_URL.toUri().buildUpon()
                        .appendQueryParameter(
                            "client_id",
                            resources.getString(R.string.imgur_api_client_id)
                        )
                        .appendQueryParameter("response_type", "token")
                        .build()
                    val browserIntent = Intent(Intent.ACTION_VIEW, imgurLogin)
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    requireActivity().startActivity(browserIntent)
                }
                return true
            }
            return false
        }
    }
}
