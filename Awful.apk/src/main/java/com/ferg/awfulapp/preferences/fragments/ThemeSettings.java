package com.ferg.awfulapp.preferences.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.ListPreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.ferg.awfulapp.AwfulApplication;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.provider.AwfulTheme;
import com.ferg.awfulapp.util.AwfulUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

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
                R.string.pref_key_theme,
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
        Pattern fontFilename = Pattern.compile("fonts/(.*).ttf.mp3", Pattern.CASE_INSENSITIVE);
        Activity activity = getActivity();
        // TODO: 25/04/2017 a separate permissions class would probably be good, keep all this garbage in one place
        if (AwfulUtils.isMarshmallow()) {
            int permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.permission_rationale_external_storage)
                            .setTitle("Permission request")
                            .setIcon(R.mipmap.ic_launcher)
                            .setPositiveButton("Got it", (dialogInterface, i) -> {})
                            .setOnDismissListener(dialogInterface -> requestStoragePermissions())
                            .show();
                } else {
                    requestStoragePermissions();
                }
            }
        }
        refreshListPreferences();

        // completely replace all entries in the font ListPreference
        ListPreference f = (ListPreference) findPrefById(R.string.pref_key_preferred_font);
        String[] fontList = ((AwfulApplication) activity.getApplication()).getFontList();
        String[] fontNames = new String[fontList.length];
        String thisFontName;
        for (int x = 0; x < fontList.length; x++) {
            Matcher fontName = fontFilename.matcher(fontList[x]);
            if (fontName.find()) {
                thisFontName = fontName.group(1).replaceAll("_", " ");
            } else {//if the regex fails, try our best to clean up the filename.
                thisFontName = fontList[x].replaceAll(".ttf.mp3", "").replaceAll("fonts/", "").replaceAll("_", " ");
            }
            fontNames[x] = WordUtils.capitalize(thisFontName);
        }
        //noinspection ConstantConditions - let it crash if the preference is missing, someone screwed up
        f.setEntries(fontNames);
        f.setEntryValues(fontList);
    }

    private void refreshListPreferences() {
        refreshLayoutPreference();
        refreshThemePreference();
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
        if (themePref == null) {
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
}
