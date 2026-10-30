package com.ferg.awfulapp.preferences.fragments

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.ferg.awfulapp.BuildConfig
import com.ferg.awfulapp.FontManager.Companion.getInstance
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.provider.AwfulTheme
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.util.Locale
import java.util.regex.Pattern

/**
 * Created by baka kaba on 04/05/2015.
 * 
 * Settings fragment for the Themes section.
 */
class ThemeSettings : SettingsFragment() {
    init {
        SETTINGS_XML_RES_ID = R.xml.themesettings
        VALUE_SUMMARY_PREF_KEYS = intArrayOf(
            R.string.pref_key_theme,
            R.string.pref_key_layout,
            R.string.pref_key_preferred_font
        )
    }

    override val title: String
        get() = getString(R.string.theme_settings)

    override fun initialiseSettings() {
        super.initialiseSettings()
        findPrefById(R.string.pref_key_launcher_icon)?.onPreferenceChangeListener = IconListener()
        val activity: Activity = requireActivity()
        // TODO: 25/04/2017 a separate permissions class would probably be good, keep all this garbage in one place
        val permissionCheck =
            ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                AlertDialog.Builder(activity)
                    .setMessage(R.string.permission_rationale_external_storage)
                    .setTitle("Permission request")
                    .setIcon(R.drawable.frog_icon)
                    .setPositiveButton(
                        "Got it"
                    ) { _: DialogInterface?, _: Int -> }
                    .setOnDismissListener { _: DialogInterface? -> requestStoragePermissions() }
                    .show()
            } else {
                requestStoragePermissions()
            }
        }
        refreshListPreferences()
    }

    private fun refreshListPreferences() {
        refreshLayoutPreference()
        refreshThemePreference()
        refreshFontListPreference()
    }

    /**
     * Rebuild the theme-chooser list preference.
     * 
     * Replaces all entries with the stock app themes, and adds any custom ones it can find.
     */
    private fun refreshThemePreference() {
        val themeNames: MutableList<CharSequence?> = ArrayList()
        val themeValues: MutableList<CharSequence?> = ArrayList()
        val themePref = findPrefById(R.string.pref_key_theme) as ListPreference?
            ?: throw RuntimeException("Theme or layout preference is missing!")

        // add the default app themes
        for (theme in AwfulTheme.APP_THEMES) {
            themeNames.add(theme.displayName)
            themeValues.add(theme.cssFilename)
        }

        // get any custom themes
        val customDir = this.customDir
        if (customDir != null) {
            /*
             * Regex that matches filenames with a '.css' extension
             * Group 1 holds the name part (before the extension). If it contains any separating '.' characters,
             * e.g. 'like.this.here.css', group 2 will contain the last part ('here') and group 1 holds the rest ('like.this').
             */
            val pattern = Pattern.compile("(.+?)(?:\\.([^.]+))?\\.css$", Pattern.CASE_INSENSITIVE)
            for (filename in customDir.list()!!) {
                val matcher = pattern.matcher(filename)
                if (matcher.matches()) {
                    val displayName = matcher.group(1)!!
                    val style = matcher.group(2)
                    themeValues.add(filename)
                    themeNames.add(
                        displayName + (if (style == null) "" else String.format(
                            " (%s)",
                            style
                        ))
                    )
                }
            }
        }

        setListPreferenceChoices(themePref, themeNames, themeValues)
    }


    /**
     * Rebuild the layout-chooser list preference.
     * 
     * Retains any stock app layouts defined in the XML, and adds any custom ones it can find.
     */
    private fun refreshLayoutPreference() {
        val layoutPref = findPrefById(R.string.pref_key_layout) as ListPreference?
            ?: throw RuntimeException("Theme or layout preference is missing!")
        val layoutNames: MutableList<CharSequence?> =
            ArrayList(listOf(*layoutPref.entries))
        val layoutValues: MutableList<CharSequence?> =
            ArrayList(listOf(*layoutPref.entryValues))

        val customDir = this.customDir ?: return
        // add all '.mustache' files, using the bit before the extension as the display name
        for (filename in customDir.list { _: File, name: String ->
            name.lowercase(
                Locale.getDefault()
            ).endsWith(".mustache")
        }!!) {
            layoutNames.add(StringUtils.substringBeforeLast(filename, "."))
            layoutValues.add(filename)
        }

        setListPreferenceChoices(layoutPref, layoutNames, layoutValues)
    }


    private val customDir: File?
        /**
         * Get the path to the folder where custom files go.
         * 
         * @return null if the folder can't be accessed
         */
        get() {
            val customDir = File(AwfulTheme.customThemePath)
            if (!customDir.canRead() || !customDir.isDirectory) {
                Log.w(
                    TAG,
                    "Unable to access custom theme folder - themes and layouts not loaded\nPath: " + customDir.path
                )
                return null
            }
            return customDir
        }


    private fun setListPreferenceChoices(
        pref: ListPreference,
        entries: MutableList<CharSequence?>,
        values: MutableList<CharSequence?>
    ) {
        pref.entries = entries.toTypedArray<CharSequence?>()
        pref.entryValues = values.toTypedArray<CharSequence?>()
    }

    private fun refreshFontListPreference() {
        val listPreference = findPrefById(R.string.pref_key_preferred_font) as ListPreference?

        // reload the font files
        getInstance().buildFontList(mPrefs!!.preferredFont, requireActivity().assets)

        // noinspection ConstantConditions - let it crash if the preference is missing, someone screwed up
        listPreference!!.entries = getInstance().fontNames
        listPreference.entryValues = getInstance().fontFilenames
    }

    private fun requestStoragePermissions() {
        requestPermissions(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE -> refreshListPreferences()
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    /** Listener for changes on the launcher icon preference  */
    private inner class IconListener : Preference.OnPreferenceChangeListener {
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            val packageManager = activity!!.packageManager
            val iconValues = resources.getStringArray(R.array.launcher_icon_values)

            for (iconValue in iconValues) {
                if (iconValue !== newValue) {
                    // make sure old icon is disabled
                    packageManager.setComponentEnabledSetting(
                        ComponentName(
                            BuildConfig.APPLICATION_ID,
                            "com.ferg.awfulapp.ForumsIndexActivity.$iconValue"
                        ),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }

            // activate new icon
            packageManager.setComponentEnabledSetting(
                ComponentName(
                    BuildConfig.APPLICATION_ID,
                    "com.ferg.awfulapp.ForumsIndexActivity.$newValue"
                ),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            return true
        }
    }

    companion object {
        private const val TAG = "ThemeSettings"
    }
}
