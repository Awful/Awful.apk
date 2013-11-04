package com.ferg.awfulapp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.ferg.awfulapp.preferences.AwfulPreferences;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


//TODO: Make this a preferenceFragment some day.
public class ThemeSettingsActivity extends PreferenceActivity implements AwfulPreferences.AwfulPreferenceUpdate {
	private Pattern fontFilename = Pattern.compile("fonts/(.*).ttf.mp3", Pattern.CASE_INSENSITIVE);
	AwfulPreferences mPrefs;
	ActivityConfigurator mConf;
	String lastTheme;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mConf = new ActivityConfigurator(this);
		mConf.onCreate(); 
		mPrefs = AwfulPreferences.getInstance(this,this);
		lastTheme = mPrefs.theme;
		addPreferencesFromResource(R.xml.themesettings);
		ListPreference themePref = (ListPreference) findPreference("theme");
		ListPreference layoutPref = (ListPreference) findPreference("layouts");
		themePref.setSummary(themePref.getEntry());
		layoutPref.setSummary(layoutPref.getEntry());
		
		File[] SDcard = Environment.getExternalStorageDirectory().listFiles();
		
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
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mConf.onPause();
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
		"theme"
	};

	@Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		ListPreference pTheme = (ListPreference) findPreference("theme");
		if(pTheme !=null){
			pTheme.setSummary(prefs.theme);
		}
	}
}
