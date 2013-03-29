package com.ferg.awfulapp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class ColorSettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
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
		addPreferencesFromResource(R.xml.colorsettings);
		ListPreference p = (ListPreference) findPreference("themes");
		p.setSummary(p.getEntry());
		
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
				if(key.equals("themes")){
					ListPreference p = (ListPreference) findPreference(key);
					if(lastTheme.equals("yospos")){
						Editor prefEdit = prefs.edit();
						prefEdit.putString("preferred_font", "default");
						savePreferences(prefEdit);
					}
					if(p.getValue().equals("dark")){
						if(lastTheme.equals("custom")){
							saveCustomTheme();
						}
						Editor prefEdit = prefs.edit();
						prefEdit.putInt("default_post_font_color", getResources().getColor(R.color.dark_default_post_font));
						prefEdit.putInt("secondary_post_font_color", getResources().getColor(R.color.dark_secondary_post_font));
						prefEdit.putInt("default_post_background_color", getResources().getColor(R.color.dark_background));
						prefEdit.putInt("alternative_post_background_color", getResources().getColor(R.color.dark_alt_background));
						prefEdit.putInt("read_post_font_color", getResources().getColor(R.color.dark_secondary_post_font));
						prefEdit.putInt("read_post_background_color", getResources().getColor(R.color.dark_background_read));
						prefEdit.putInt("alternative_read_post_background_color", getResources().getColor(R.color.dark_alt_background_read));
						prefEdit.putInt("post_header_background_color", getResources().getColor(R.color.dark_header_background));//TODO
						prefEdit.putInt("post_divider_color", getResources().getColor(R.color.dark_header_divider));//TODO
						prefEdit.putBoolean("post_divider_enabled", false);
						prefEdit.putInt("post_header_font_color", getResources().getColor(R.color.dark_header_font));//TODO
						prefEdit.putInt("op_post_color", getResources().getColor(R.color.dark_op_post));
						prefEdit.putInt("link_quote_color", getResources().getColor(R.color.dark_link_quote));
						prefEdit.putInt("unread_posts", getResources().getColor(R.color.unread_posts));//TODO
						prefEdit.putInt("unread_posts_dim", getResources().getColor(R.color.unread_posts_dim));
						prefEdit.putBoolean("unread_posts_font_black", false);
						prefEdit.putString("selected_theme", "dark");
                        savePreferences(prefEdit);
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
						prefEdit.putInt("read_post_font_color", getResources().getColor(R.color.default_post_font));
						prefEdit.putInt("read_post_background_color", getResources().getColor(R.color.background_read));
						prefEdit.putInt("alternative_read_post_background_color", getResources().getColor(R.color.alt_background_read));
						prefEdit.putInt("post_header_background_color", getResources().getColor(R.color.forums_blue));//TODO
						prefEdit.putInt("post_divider_color", getResources().getColor(R.color.background));//TODO
						prefEdit.putInt("post_header_font_color", getResources().getColor(R.color.forums_gray));//TODO
						prefEdit.putBoolean("post_divider_enabled", false);
						prefEdit.putInt("op_post_color", getResources().getColor(R.color.op_post));
						prefEdit.putInt("link_quote_color", getResources().getColor(R.color.link_quote));
						prefEdit.putInt("unread_posts", getResources().getColor(R.color.unread_posts));//TODO
						prefEdit.putInt("unread_posts_dim", getResources().getColor(R.color.unread_posts_dim));
						prefEdit.putBoolean("unread_posts_font_black", false);
						prefEdit.putString("selected_theme", "dark");
                        savePreferences(prefEdit);
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
						prefEdit.putInt("read_post_font_color", getResources().getColor(R.color.yospos_default_post_font));
						prefEdit.putInt("read_post_background_color", getResources().getColor(R.color.yospos_background_read));
						prefEdit.putInt("alternative_read_post_background_color", getResources().getColor(R.color.yospos_alt_background_read));
						prefEdit.putInt("post_header_background_color", getResources().getColor(R.color.yospos_background));//TODO
						prefEdit.putInt("post_divider_color", getResources().getColor(R.color.yospos_default_post_font));//TODO
						prefEdit.putInt("post_header_font_color", getResources().getColor(R.color.yospos_default_post_font));//TODO
						prefEdit.putBoolean("post_divider_enabled", true);
						prefEdit.putString("preferred_font", "fonts/terminus_mono.ttf.mp3");
						prefEdit.putInt("op_post_color", getResources().getColor(R.color.yospos_op_post));
						prefEdit.putInt("link_quote_color", getResources().getColor(R.color.yospos_link_quote));
						prefEdit.putInt("unread_posts", getResources().getColor(R.color.unread_posts));//TODO
						prefEdit.putInt("unread_posts_dim", getResources().getColor(R.color.unread_posts_dim));
						prefEdit.putBoolean("unread_posts_font_black", false);
						prefEdit.putString("selected_theme", "dark");
                        savePreferences(prefEdit);
						p.setSummary("yospos, bitch");
						lastTheme = "yospos";
						this.finish();
						return;
					}else if(p.getValue().equals("darkblue")){
						if(lastTheme.equals("custom")){
							saveCustomTheme();
						}
						Editor prefEdit = prefs.edit();
						prefEdit.putInt("default_post_font_color", getResources().getColor(R.color.dark_default_post_font));
						prefEdit.putInt("secondary_post_font_color", getResources().getColor(R.color.dark_secondary_post_font));
						prefEdit.putInt("default_post_background_color", getResources().getColor(R.color.dark_background));
						prefEdit.putInt("alternative_post_background_color", getResources().getColor(R.color.dark_background));
						prefEdit.putInt("read_post_font_color", getResources().getColor(R.color.dark_secondary_post_font));
						prefEdit.putInt("read_post_background_color", getResources().getColor(R.color.dark_background));
						prefEdit.putInt("alternative_read_post_background_color", getResources().getColor(R.color.dark_background));
						prefEdit.putInt("post_header_background_color", getResources().getColor(R.color.dark_background));//TODO
						prefEdit.putInt("post_divider_color", getResources().getColor(R.color.dark_blue));//TODO
						prefEdit.putBoolean("post_divider_enabled", true);
						prefEdit.putInt("post_header_font_color", getResources().getColor(R.color.dark_header_font));//TODO
						prefEdit.putInt("op_post_color", getResources().getColor(R.color.dark_op_post));
						prefEdit.putInt("link_quote_color", getResources().getColor(R.color.dark_link_quote));
						prefEdit.putInt("unread_posts", getResources().getColor(R.color.unread_posts));//TODO
						prefEdit.putInt("unread_posts_dim", getResources().getColor(R.color.unread_posts_dim));
						prefEdit.putBoolean("unread_posts_font_black", false);
						prefEdit.putString("selected_theme", "dark");
                        savePreferences(prefEdit);
						p.setSummary("Darkish Blueish");
						lastTheme = "darkblue";
						this.finish();
						return;
					}else if(!lastTheme.equals("custom")){
							Editor prefEdit = prefs.edit();
							prefEdit.putInt("default_post_font_color", mPrefs.getInt("custom_default_post_font_color", getResources().getColor(R.color.default_post_font)));
							prefEdit.putInt("secondary_post_font_color", mPrefs.getInt("custom_secondary_post_font_color", getResources().getColor(R.color.secondary_post_font)));
							prefEdit.putInt("default_post_background_color", mPrefs.getInt("custom_default_post_background_color", getResources().getColor(R.color.background)));
							prefEdit.putInt("alternative_post_background_color", mPrefs.getInt("custom_alternative_post_background_color", getResources().getColor(R.color.alt_background)));
							prefEdit.putInt("read_post_font_color", mPrefs.getInt("custom_read_post_font_color", getResources().getColor(R.color.font_read)));
							prefEdit.putInt("read_post_background_color", mPrefs.getInt("custom_read_post_background_color", getResources().getColor(R.color.background_read)));
							prefEdit.putInt("alternative_read_post_background_color", mPrefs.getInt("custom_alternative_read_post_background_color", getResources().getColor(R.color.alt_background_read)));
							prefEdit.putInt("post_header_background_color", mPrefs.getInt("custom_default_post_font_color", getResources().getColor(R.color.forums_blue)));//TODO
							prefEdit.putInt("post_divider_color", mPrefs.getInt("custom_default_post_font_color", getResources().getColor(R.color.background)));//TODO
							prefEdit.putBoolean("post_divider_enabled", mPrefs.getBoolean("post_divider_enabled", false));
							prefEdit.putInt("post_header_font_color", mPrefs.getInt("custom_default_post_font_color", getResources().getColor(R.color.forums_gray)));//TODO
							prefEdit.putInt("op_post_color", mPrefs.getInt("custom_op_post_color", getResources().getColor(R.color.op_post)));
							prefEdit.putInt("link_quote_color", mPrefs.getInt("custom_link_quote_color", getResources().getColor(R.color.link_quote)));
							prefEdit.putInt("unread_posts",  mPrefs.getInt("unread_posts", getResources().getColor(R.color.unread_posts)));//TODO
							prefEdit.putInt("unread_posts_dim",  mPrefs.getInt("unread_posts_dim", getResources().getColor(R.color.unread_posts_dim)));
							prefEdit.putBoolean("unread_posts_font_black", mPrefs.getBoolean("unread_posts_font_black", false));
                            savePreferences(prefEdit);
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
		prefEdit.putInt("custom_read_post_font_color", mPrefs.getInt("read_post_font_color", getResources().getColor(R.color.font_read)));
		prefEdit.putInt("custom_read_post_background_color", mPrefs.getInt("read_post_background_color", getResources().getColor(R.color.background_read)));
		prefEdit.putInt("custom_alternative_read_post_background_color", mPrefs.getInt("alternative_read_post_background_color", getResources().getColor(R.color.alt_background_read)));
		prefEdit.putInt("custom_post_header_background_color", mPrefs.getInt("post_header_background_color", getResources().getColor(R.color.forums_blue)));
		prefEdit.putInt("custom_post_divider_color", mPrefs.getInt("post_divider_color", getResources().getColor(R.color.background)));
		prefEdit.putInt("custom_post_header_font_color", mPrefs.getInt("post_header_font_color", getResources().getColor(R.color.forums_gray)));
		prefEdit.putInt("custom_op_post_color", mPrefs.getInt("op_post_color", getResources().getColor(R.color.op_post)));
		prefEdit.putInt("custom_link_quote_color", mPrefs.getInt("link_quote_color", getResources().getColor(R.color.link_quote)));
		prefEdit.putInt("custom_unread_posts", mPrefs.getInt("unread_posts", getResources().getColor(R.color.unread_posts)));
		prefEdit.putInt("custom_unread_posts_dim", mPrefs.getInt("unread_posts_dim", getResources().getColor(R.color.unread_posts_dim)));
		prefEdit.putBoolean("custom_unread_posts_font_black", mPrefs.getBoolean("unread_posts_font_black", false));
        savePreferences(prefEdit);
	}

    private void savePreferences(Editor aPrefs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            aPrefs.apply();
        } else {
            aPrefs.commit();
        }
    }
}
