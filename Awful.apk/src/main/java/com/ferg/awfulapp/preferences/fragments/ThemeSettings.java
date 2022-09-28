package com.ferg.awfulapp.preferences.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.ListPreference;
import android.preference.Preference;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;

import com.ferg.awfulapp.BuildConfig;
import com.ferg.awfulapp.FontManager;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.provider.AwfulTheme;
import com.ferg.awfulapp.util.AwfulUtils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by baka kaba on 04/05/2015.
 *
 * Settings fragment for the Themes section.
 */
public class ThemeSettings extends SettingsFragment {

    {
        SETTINGS_XML_RES_ID = R.xml.themesettings;
        VALUE_SUMMARY_PREF_KEYS = new int[]{
                R.string.pref_key_theme_changing,
                R.string.pref_key_theme,
                R.string.pref_key_theme_dark_mode,
                R.string.pref_key_layout,
                R.string.pref_key_preferred_font
        };
    }

    private static final String TAG = "ThemeSettings";


    @NonNull
    @Override
    public String getTitle() {
        return getString(R.string.theme_settings);
    }

    @Override
    protected void initialiseSettings() {
        super.initialiseSettings();
        findPrefById(R.string.pref_key_launcher_icon).setOnPreferenceChangeListener(new IconListener());
        Activity activity = getActivity();
        // TODO: 25/04/2017 a separate permissions class would probably be good, keep all this garbage in one place
        if (AwfulUtils.isMarshmallow()) {
            int permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.permission_rationale_external_storage)
                            .setTitle("Permission request")
                            .setIcon(R.drawable.frog_icon)
                            .setPositiveButton("Got it", (dialogInterface, i) -> {})
                            .setOnDismissListener(dialogInterface -> requestStoragePermissions())
                            .show();
                } else {
                    requestStoragePermissions();
                }
            }
        }
        refreshListPreferences();
    }

    private void refreshListPreferences() {
        refreshLayoutPreference();
        refreshThemePreference();
        refreshFontListPreference();
    }

    /**
     * Rebuild the theme-chooser list preference.
     *
     * Replaces all entries with the stock app themes, and adds any custom ones it can find.
     */
    private void refreshThemePreference() {
        List<CharSequence> themeNames = new ArrayList<>();
        List<CharSequence> themeValues = new ArrayList<>();
        ListPreference themePref = (ListPreference) findPrefById(R.string.pref_key_theme);
        ListPreference darkThemePref = (ListPreference) findPrefById(R.string.pref_key_theme_dark_mode);
        if (themePref == null || darkThemePref == null) {
            throw new RuntimeException("Theme or layout preference is missing!");
        }

        // add the default app themes
        for (AwfulTheme theme : AwfulTheme.APP_THEMES) {
            themeNames.add(theme.displayName);
            themeValues.add(theme.cssFilename);
        }

        // get any custom themes
        File customDir = getCustomDir();
        if (customDir != null) {
            /*
             * Regex that matches filenames with a '.css' extension
             * Group 1 holds the name part (before the extension). If it contains any separating '.' characters,
             * e.g. 'like.this.here.css', group 2 will contain the last part ('here') and group 1 holds the rest ('like.this').
             */
            Pattern pattern = Pattern.compile("(.+?)(?:\\.([^.]+))?\\.css$", Pattern.CASE_INSENSITIVE);
            for (String filename : customDir.list()) {
                Matcher matcher = pattern.matcher(filename);
                if (matcher.matches()) {
                    String displayName = matcher.group(1);
                    String style = matcher.group(2);
                    themeValues.add(filename);
                    themeNames.add(displayName + (style == null ? "" : String.format(" (%s)", style)));
                }
            }
        }

        setListPreferenceChoices(themePref, themeNames, themeValues);
        setListPreferenceChoices(darkThemePref, themeNames, themeValues);
    }


    /**
     * Rebuild the layout-chooser list preference.
     *
     * Retains any stock app layouts defined in the XML, and adds any custom ones it can find.
     */
    private void refreshLayoutPreference() {
        ListPreference layoutPref = (ListPreference) findPrefById(R.string.pref_key_layout);
        if (layoutPref == null) {
            throw new RuntimeException("Theme or layout preference is missing!");
        }
        List<CharSequence> layoutNames = new ArrayList<>(Arrays.asList(layoutPref.getEntries()));
        List<CharSequence> layoutValues = new ArrayList<>(Arrays.asList(layoutPref.getEntryValues()));

        File customDir = getCustomDir();
        if (customDir == null) {
            return;
        }
        // add all '.mustache' files, using the bit before the extension as the display name
        for (String filename : customDir.list((dir, name) -> name.toLowerCase().endsWith(".mustache"))) {
            layoutNames.add(StringUtils.substringBeforeLast(filename, "."));
            layoutValues.add(filename);
        }

        setListPreferenceChoices(layoutPref, layoutNames, layoutValues);
    }


    /**
     * Get the path to the folder where custom files go.
     *
     * @return null if the folder can't be accessed
     */
    @Nullable
    private File getCustomDir() {
        File customDir = new File(AwfulTheme.getCustomThemePath());
        if (!customDir.canRead() || !customDir.isDirectory()) {
            Log.w(TAG, "Unable to access custom theme folder - themes and layouts not loaded\nPath: " + customDir.getPath());
            return null;
        }
        return customDir;
    }


    private void setListPreferenceChoices(@NonNull ListPreference pref,
                                          @NonNull List<CharSequence> entries,
                                          @NonNull List<CharSequence> values) {
        pref.setEntries(entries.toArray(new CharSequence[entries.size()]));
        pref.setEntryValues(values.toArray(new CharSequence[values.size()]));
    }

    private void refreshFontListPreference() {
        ListPreference listPreference = (ListPreference) findPrefById(R.string.pref_key_preferred_font);

        // reload the font files
        FontManager.getInstance().buildFontList(mPrefs.preferredFont, getActivity().getAssets());

        // noinspection ConstantConditions - let it crash if the preference is missing, someone screwed up
        listPreference.setEntries(FontManager.getInstance().getFontNames());
        listPreference.setEntryValues(FontManager.getInstance().getFontFilenames());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestStoragePermissions() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE:
                refreshListPreferences();
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /** Listener for changes on the launcher icon preference */
    private class IconListener implements Preference.OnPreferenceChangeListener {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            PackageManager packageManager = getActivity().getPackageManager();
            String[] iconValues = getResources().getStringArray(R.array.launcher_icon_values);

            for (String iconValue : iconValues) {
                if(iconValue != newValue) {
                    // make sure old icon is disabled
                    packageManager.setComponentEnabledSetting(
                            new ComponentName(BuildConfig.APPLICATION_ID, "com.ferg.awfulapp.ForumsIndexActivity." + iconValue),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                }
            }

            // activate new icon
            packageManager.setComponentEnabledSetting(
                    new ComponentName(BuildConfig.APPLICATION_ID, "com.ferg.awfulapp.ForumsIndexActivity." + (String) newValue),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            return true;
        }
    }
}
