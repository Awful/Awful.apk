package com.ferg.awful;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class ColorSettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	SharedPreferences mPrefs;
	ActivityConfigurator mConf;
	String lastTheme;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        GoogleAnalyticsTracker.getInstance().trackPageView("/ColorSettingsActivity");

		mConf = new ActivityConfigurator(this);
		mConf.onCreate(); 
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		lastTheme = mPrefs.getString("themes","default");
		addPreferencesFromResource(R.xml.themesettings);
		ListPreference p = (ListPreference) findPreference("themes");
		p.setSummary(p.getEntry());

		if(mPrefs.getString("themes","default").equals("custom")){
            addPreferencesFromResource(R.xml.colorsettings);
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
		"themes"
	};
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		for(String valueSummaryKey : VALUE_SUMMARY_KEYS_LIST) {
			if(valueSummaryKey.equals(key)) {
				if(key.equals("themes")){
					ListPreference p = (ListPreference) findPreference(key);
					if(p.getValue().equals("dark")){
						if(lastTheme.equals("custom")){
							saveCustomTheme();
						}
						Editor prefEdit = prefs.edit();
						prefEdit.putInt("default_post_font_color", getResources().getColor(R.color.dark_default_post_font));
						prefEdit.putInt("secondary_post_font_color", getResources().getColor(R.color.dark_secondary_post_font));
						prefEdit.putInt("default_post_background_color", getResources().getColor(R.color.dark_background));
						prefEdit.putInt("alternative_post_background_color", getResources().getColor(R.color.dark_alt_background));
						prefEdit.putInt("read_post_background_color", getResources().getColor(R.color.dark_background_read));
						prefEdit.putInt("alternative_read_post_background_color", getResources().getColor(R.color.dark_alt_background_read));
						prefEdit.putInt("op_post_color", getResources().getColor(R.color.dark_op_post));
						prefEdit.putInt("link_quote_color", getResources().getColor(R.color.dark_link_quote));
						prefEdit.commit();
						p.setSummary("Dark");
						lastTheme = "dark";
						this.finish();
						return;
					}else if(p.getValue().equals("default")){
						if(lastTheme.equals("custom")){
							saveCustomTheme();
						}
						Editor prefEdit = prefs.edit();
						prefEdit.putInt("default_post_font_color", getResources().getColor(R.color.default_post_font));
						prefEdit.putInt("secondary_post_font_color", getResources().getColor(R.color.secondary_post_font));
						prefEdit.putInt("default_post_background_color", getResources().getColor(R.color.background));
						prefEdit.putInt("alternative_post_background_color", getResources().getColor(R.color.alt_background));
						prefEdit.putInt("read_post_background_color", getResources().getColor(R.color.background_read));
						prefEdit.putInt("alternative_read_post_background_color", getResources().getColor(R.color.alt_background_read));
						prefEdit.putInt("op_post_color", getResources().getColor(R.color.op_post));
						prefEdit.putInt("link_quote_color", getResources().getColor(R.color.link_quote));
						prefEdit.commit();
						p.setSummary("Default");
						lastTheme = "default";
						this.finish();
						return;
					}else if(p.getValue().equals("yospos")){
						if(lastTheme.equals("custom")){
							saveCustomTheme();
						}
						Editor prefEdit = prefs.edit();
						prefEdit.putInt("default_post_font_color", getResources().getColor(R.color.yospos_default_post_font));
						prefEdit.putInt("secondary_post_font_color", getResources().getColor(R.color.yospos_secondary_post_font));
						prefEdit.putInt("default_post_background_color", getResources().getColor(R.color.yospos_background));
						prefEdit.putInt("alternative_post_background_color", getResources().getColor(R.color.yospos_alt_background));
						prefEdit.putInt("read_post_background_color", getResources().getColor(R.color.yospos_background_read));
						prefEdit.putInt("alternative_read_post_background_color", getResources().getColor(R.color.yospos_alt_background_read));
						prefEdit.putInt("op_post_color", getResources().getColor(R.color.yospos_op_post));
						prefEdit.putInt("link_quote_color", getResources().getColor(R.color.yospos_link_quote));
						prefEdit.commit();
						p.setSummary("yospos, bitch");
						lastTheme = "yospos";
						this.finish();
						return;
					}else if(!lastTheme.equals("custom")){
							Editor prefEdit = prefs.edit();
							prefEdit.putInt("default_post_font_color", mPrefs.getInt("custom_default_post_font_color", getResources().getColor(R.color.default_post_font)));
							prefEdit.putInt("secondary_post_font_color", mPrefs.getInt("custom_secondary_post_font_color", getResources().getColor(R.color.secondary_post_font)));
							prefEdit.putInt("default_post_background_color", mPrefs.getInt("custom_default_post_background_color", getResources().getColor(R.color.background)));
							prefEdit.putInt("alternative_post_background_color", mPrefs.getInt("custom_alternative_post_background_color", getResources().getColor(R.color.alt_background)));
							prefEdit.putInt("read_post_background_color", mPrefs.getInt("custom_read_post_background_color", getResources().getColor(R.color.background_read)));
							prefEdit.putInt("alternative_read_post_background_color", mPrefs.getInt("custom_alternative_read_post_background_color", getResources().getColor(R.color.alt_background_read)));
							prefEdit.putInt("op_post_color", mPrefs.getInt("custom_op_post_color", getResources().getColor(R.color.op_post)));
							prefEdit.putInt("link_quote_color", mPrefs.getInt("custom_link_quote_color", getResources().getColor(R.color.link_quote)));
							prefEdit.commit();
							addPreferencesFromResource(R.xml.colorsettings);
							p.setSummary("Custom");
							lastTheme = "custom";
						}

				}
			}
		}
	}

	private void saveCustomTheme() {
		Editor prefEdit = mPrefs.edit();
		prefEdit.putInt("custom_default_post_font_color", mPrefs.getInt("default_post_font_color", getResources().getColor(R.color.default_post_font)));
		prefEdit.putInt("custom_secondary_post_font_color", mPrefs.getInt("secondary_post_font_color", getResources().getColor(R.color.secondary_post_font)));
		prefEdit.putInt("custom_default_post_background_color", mPrefs.getInt("default_post_background_color", getResources().getColor(R.color.background)));
		prefEdit.putInt("custom_alternative_post_background_color", mPrefs.getInt("alternative_post_background_color", getResources().getColor(R.color.alt_background)));
		prefEdit.putInt("custom_read_post_background_color", mPrefs.getInt("read_post_background_color", getResources().getColor(R.color.background_read)));
		prefEdit.putInt("custom_alternative_read_post_background_color", mPrefs.getInt("alternative_read_post_background_color", getResources().getColor(R.color.alt_background_read)));
		prefEdit.putInt("custom_op_post_color", mPrefs.getInt("op_post_color", getResources().getColor(R.color.op_post)));
		prefEdit.putInt("custom_link_quote_color", mPrefs.getInt("link_quote_color", getResources().getColor(R.color.link_quote)));
		prefEdit.commit();
	}
}
