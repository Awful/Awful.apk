package com.ferg.awfulapp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


//TODO: Make this a preferenceFragment some day.
public class ThemeSettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private Pattern fontFilename = Pattern.compile("fonts/(.*).ttf.mp3", Pattern.CASE_INSENSITIVE);
	SharedPreferences mPrefs;
	ActivityConfigurator mConf;
	String lastTheme;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mConf = new ActivityConfigurator(this);
		mConf.onCreate(); 
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		lastTheme = mPrefs.getString("themes","default");
		addPreferencesFromResource(R.xml.themesettings);
		ListPreference p = (ListPreference) findPreference("themes");
		p.setSummary(p.getEntry());
		
		File[] SDcard = Environment.getExternalStorageDirectory().listFiles();
		
		for (File folder: SDcard){
			if("awful".equals(folder.getName()) && folder.canRead()){
				File[] files = folder.listFiles();
				ArrayList<CharSequence> themes = new ArrayList<CharSequence>();
				ArrayList<CharSequence> themeValues = new ArrayList<CharSequence>();
				themes.addAll(Arrays.asList(p.getEntries()));
				themeValues.addAll(Arrays.asList(p.getEntryValues()));
				for(File folderFile: files){
					if(folderFile.canRead() && folderFile.getName() != null){
						String[] fileName = folderFile.getName().split("\\.");
						if("css".equals(fileName[fileName.length-1])){
							themes.add(fileName[0]);
							themeValues.add(folderFile.getName());
						}
					}
				}
				p.setEntries(themes.toArray(new CharSequence[themes.size()]));
				p.setEntryValues(themeValues.toArray(new CharSequence[themeValues.size()]));
			}
		}
		
		ListPreference f = (ListPreference) findPreference("preferred_font");
		String[] fontList = ((AwfulApplication)getApplication()).getFontList();
		String[] fontNames = new String[fontList.length];
		for(int x=0; x<fontList.length;x++){
			Matcher fontName = fontFilename.matcher(fontList[x]);
			if(fontName.find()){
				fontNames[x] = fontName.group(1).replaceAll("_", " ");
			}else{//if the regex fails, try our best to clean up the filename.
				fontNames[x] = fontList[x].replaceAll(".ttf.mp3", "").replaceAll("fonts/", "").replaceAll("_", " ");
			}
		}
		f.setEntries(fontNames);
		f.setEntryValues(fontList);
		f.setSummary(f.getEntry());
		for(String valueSummaryKey : VALUE_SUMMARY_KEYS_LIST) {
			ListPreference pl = (ListPreference) findPreference(valueSummaryKey);
			pl.setSummary(pl.getEntry());
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		mConf.onStart();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mConf.onResume();
		
		mPrefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mConf.onPause();
		
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mConf.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mConf.onDestroy();
	}
	
	private static final String[] VALUE_SUMMARY_KEYS_LIST = {
		"themes",
		"selected_theme"
	};
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		for(String valueSummaryKey : VALUE_SUMMARY_KEYS_LIST) {
			if(valueSummaryKey.equals(key)) {
				if("selected_theme".equals(key)){
					ListPreference p = (ListPreference) findPreference(key);
					p.setSummary(p.getEntry());
				}
				if("themes".equals(key)){
					ListPreference p = (ListPreference) findPreference(key);
					p.setSummary(p.getEntry());
				}
			}
		}
	}


    private void savePreferences(Editor aPrefs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            aPrefs.apply();
        } else {
            aPrefs.commit();
        }
    }
}
