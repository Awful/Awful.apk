package com.ferg.awfulapp.preferences.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.preference.ListPreference;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.ferg.awfulapp.AwfulApplication;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.util.AwfulUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by baka kaba on 04/05/2015.
 */
public class ThemeSettings extends SettingsFragment {

    {
        SETTINGS_XML_RES_ID = R.xml.themesettings;
        VALUE_SUMMARY_PREF_KEYS = new String[] {
                "theme", "layouts", "preferred_font"
        };
    }

    private static final String TAG = "ThemeSettings";

    @Override
    protected void initialiseSettings() {
        super.initialiseSettings();
        Pattern fontFilename = Pattern.compile("fonts/(.*).ttf.mp3", Pattern.CASE_INSENSITIVE);
        if(AwfulUtils.isMarshmallow()){
            int permissionCheck = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE);
            } else {
                loadExternalOptions();
            }
        }else{
            loadExternalOptions();
        }

        ListPreference f = (ListPreference) findPreference("preferred_font");
        String[] fontList = ((AwfulApplication) getActivity().getApplication()).getFontList();
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
        f.setEntries(fontNames);
        f.setEntryValues(fontList);

    }

    private void loadExternalOptions(){
        ListPreference themePref = (ListPreference) findPreference("theme");
        ListPreference layoutPref = (ListPreference) findPreference("layouts");
        File[] SDcard = Environment.getExternalStorageDirectory().listFiles();
        if (SDcard != null) {
            for (File folder: SDcard){
                if("awful".equals(folder.getName()) && folder.canRead()){
                    File[] files = folder.listFiles();
                    ArrayList<CharSequence> themes = new ArrayList<CharSequence>();
                    ArrayList<CharSequence> themeValues = new ArrayList<CharSequence>();
                    ArrayList<CharSequence> layouts = new ArrayList<CharSequence>();
                    ArrayList<CharSequence> layoutValues = new ArrayList<CharSequence>();
                    themes.addAll(Arrays.asList(themePref.getEntries()));
                    themeValues.addAll(Arrays.asList(themePref.getEntryValues()));
                    layouts.addAll(Arrays.asList(layoutPref.getEntries()));
                    layoutValues.addAll(Arrays.asList(layoutPref.getEntryValues()));
                    for(File folderFile: files){
                        if(folderFile.canRead() && folderFile.getName() != null){
                            String[] fileName = folderFile.getName().split("\\.");
                            if("css".equals(fileName[fileName.length-1])){
                                if(StringUtils.countMatches(folderFile.getName(), ".")>1){
                                    themes.add(fileName[0]+" ("+fileName[fileName.length-2]+")");
                                }else{
                                    themes.add(fileName[0]);
                                }
                                themeValues.add(folderFile.getName());
                            }
                            if("mustache".equals(fileName[fileName.length-1])){
                                layouts.add(fileName[0]);
                                layoutValues.add(folderFile.getName());
                            }
                        }
                    }
                    layoutPref.setEntries(layouts.toArray(new CharSequence[layouts.size()]));
                    layoutPref.setEntryValues(layoutValues.toArray(new CharSequence[layoutValues.size()]));

                    themePref.setEntries(themes.toArray(new CharSequence[themes.size()]));
                    themePref.setEntryValues(themeValues.toArray(new CharSequence[themeValues.size()]));
                }
            }
        }
        else{
            Log.w(TAG, "Unable to access ExternalStorageDirectory - themes and layouts not loaded");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadExternalOptions();
                }
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
