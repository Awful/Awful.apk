package com.ferg.awfulapp.preferences.fragments

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.ferg.awfulapp.AwfulActivity
import com.ferg.awfulapp.NavigationEvent
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.dialog.Changelog
import com.ferg.awfulapp.preferences.SettingsActivity
import org.apache.commons.lang3.StringUtils
import java.util.Calendar
import java.util.Locale

/**
 * Created by baka kaba on 04/05/2015.
 * 
 * 
 * The SettingsFragment that forms the root of the Settings hierarchy
 */
class RootSettings : SettingsFragment() {
    init {
        SETTINGS_XML_RES_ID = R.xml.rootsettings

        SUBMENU_OPENING_KEYS = intArrayOf(
            R.string.pref_key_theme_menu_item,
            R.string.pref_key_forum_index_menu_item,
            R.string.pref_key_thread_menu_item,
            R.string.pref_key_posts_menu_item,
            R.string.pref_key_images_menu_item,
            R.string.pref_key_misc_menu_item,
            R.string.pref_key_account_menu_item
        )

        prefClickListeners[AboutListener()] = intArrayOf(
            R.string.pref_key_about_menu_item
        )
        prefClickListeners[ThreadListener()] = intArrayOf(
            R.string.pref_key_open_thread_menu_item
        )
        // TODO: fix
        prefClickListeners[ChangelogListener()] = intArrayOf(
            R.string.pref_key_changelog_menu_item
        )
        prefClickListeners[ExportListener()] = intArrayOf(
            R.string.pref_key_export_settings_menu_item
        )
        prefClickListeners[ImportListener()] = intArrayOf(
            R.string.pref_key_import_settings_menu_item
        )
    }

    override val title: String
        get() = getString(R.string.settings_activity_title)

    /**
     * Listener for the 'About...' option
     */
    private inner class AboutListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            var app_version = getText(R.string.app_name)
            try {
                app_version = "$app_version " +
                        requireActivity().packageManager
                            .getPackageInfo(requireActivity().packageName, 0)
                            .versionName
            } catch (e: PackageManager.NameNotFoundException) {
                // rather unlikely, just show app_name without version
            }
            // Build the text for the About dialog
            val res = resources
            var aboutText: String? = getString(R.string.about_contributors_title) + "\n\n"
            aboutText += StringUtils.join(
                res.getStringArray(R.array.about_contributors_array),
                '\n'
            )
            aboutText += "\n\n" + getString(R.string.about_libraries_title) + "\n\n"
            aboutText += StringUtils.join(res.getStringArray(R.array.about_libraries_array), '\n')
            val about: Dialog = AlertDialog.Builder(activity!!)
                .setTitle(app_version)
                .setMessage(aboutText)
                .setNeutralButton(
                    android.R.string.ok
                ) { _: DialogInterface?, _: Int -> }
                .show()

            val activity = requireActivity() as AwfulActivity
            activity.setPreferredFont(about.findViewById(androidx.appcompat.R.id.alertTitle))
            activity.setPreferredFont(about.findViewById(android.R.id.message))
            activity.setPreferredFont(about.findViewById(android.R.id.button3))

            return true
        }
    }

    private inner class ChangelogListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            Changelog.showDialog(activity!!, null)
            return true
        }
    }


    /**
     * Listener for 'Go to the Awful thread' option
     */
    private inner class ThreadListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            navigate(NavigationEvent.Thread(Constants.AWFUL_THREAD_ID, null, null))
            return true
        }
    }

    /**
     * Listener for the 'Export settings' option
     */
    private inner class ExportListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            val context: Activity? = activity
            val pInfo: PackageInfo
            try {
                pInfo = context!!.packageManager.getPackageInfo(context.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                // super unlikely
                e.printStackTrace()
                return false
            }
            val date = Calendar.getInstance()

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType("*/*")
                .addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(
                    Intent.EXTRA_TITLE, String.format(
                        Locale.US,
                        "awful-%d-%d-%d-%d.settings",
                        pInfo.versionCode,
                        date.get(Calendar.DATE),
                        date.get(Calendar.MONTH) + 1,
                        date.get(
                            Calendar.YEAR
                        )
                    )
                )

            requireActivity().startActivityForResult(
                Intent.createChooser(
                    intent,
                    getString(R.string.export_settings_chooser_title)
                ), SettingsActivity.SETTINGS_EXPORT
            )
            return true
        }
    }

    /**
     * Listener for the 'Import settings' option
     */
    private inner class ImportListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            // ACTION_GET_CONTENT may return URIs for deleted content as well,
            // which is super confusing. workarounds seem like more trouble
            // than they're worth right now.
            // see https://stackoverflow.com/questions/55122556
            val intent = Intent(Intent.ACTION_GET_CONTENT)
                .setType("*/*")
                .addCategory(Intent.CATEGORY_OPENABLE)
            requireActivity().startActivityForResult(
                Intent.createChooser(
                    intent,
                    getString(R.string.import_settings_chooser_title)
                ), SettingsActivity.SETTINGS_IMPORT
            )
            return true
        }
    }
}