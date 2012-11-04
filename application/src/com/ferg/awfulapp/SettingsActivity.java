/********************************************************************************
 * Copyright (c) 2011, Dan Bjorge
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the software nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY DAN BJORGE ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.ferg.awfulapp;

import org.apache.commons.lang3.text.WordUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.ferg.awfulapp.constants.Constants;

/**
 * Simple, purely xml driven preferences. Access using
 * {@link PreferenceManager#getDefaultSharedPreferences(android.content.Context)}
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private static final int DIALOG_ABOUT = 1;
	Preference mAboutPreference;
	Preference mImagePreference;
	Preference mInfoPreference;
	Preference mColorsPreference;
	Preference mFontSizePreference;
	Preference mUsernamePreference;
	Context mThis = this;
	Dialog mFontSizeDialog;
	TextView mFontSizeText;
	
	SharedPreferences mPrefs;
	ActivityConfigurator mConf;

	// ---------------------------------------------- //
	// ---------------- LIFECYCLE ------------------- //
	// ---------------------------------------------- //
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		mConf = new ActivityConfigurator(this);
		mConf.onCreate();
		
		addPreferencesFromResource(R.xml.settings);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		findPreference("inline_youtube").setEnabled(Constants.isICS());
		
		mAboutPreference = getPreferenceScreen().findPreference("about");
		mAboutPreference.setOnPreferenceClickListener(onAboutListener);
		mColorsPreference = getPreferenceScreen().findPreference("colors");
		mColorsPreference.setOnPreferenceClickListener(onColorsListener);
		mImagePreference = getPreferenceScreen().findPreference("image_settings");
		mImagePreference.setOnPreferenceClickListener(onImagesListener);
		mInfoPreference = getPreferenceScreen().findPreference("threadinfo");
		mInfoPreference.setOnPreferenceClickListener(onInfoListener);
		mFontSizePreference = getPreferenceScreen().findPreference("default_post_font_size_dip");
		mFontSizePreference.setOnPreferenceClickListener(onFontSizeListener);
		
		mUsernamePreference = getPreferenceScreen().findPreference("username");
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
		
		setSummaries();
		
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
	
	
	// ---------------------------------------------- //
	// ------------- OTHER LISTENERS ---------------- //
	// ---------------------------------------------- //
	
	@Override
	protected Dialog onCreateDialog(int dialogId) {
		switch(dialogId) {
		case DIALOG_ABOUT:
			CharSequence app_version = getText(R.string.app_name);
			try {
				app_version = app_version + " " +
					getPackageManager().getPackageInfo(getPackageName(), 0)
					.versionName;
			} catch (NameNotFoundException e) {
				// rather unlikely, just show app_name without version
			}

			return new AlertDialog.Builder(this)
				.setTitle(app_version)
				.setMessage(R.string.about_message)
				.setNeutralButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
					}})
				.create();
		default:
			return super.onCreateDialog(dialogId);
		}
	}
	
	private OnPreferenceClickListener onAboutListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			showDialog(DIALOG_ABOUT);
			return true;
		}
	};

	private OnPreferenceClickListener onColorsListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			startActivity(new Intent().setClass(mThis, ColorSettingsActivity.class));
			return true;
		}
	};
	
	private OnPreferenceClickListener onImagesListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			startActivity(new Intent().setClass(mThis, ImageSettingsActivity.class));
			return true;
		}
	};
	
	private OnPreferenceClickListener onInfoListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			startActivity(new Intent().setClass(mThis, ThreadInfoSettingsActivity.class));
			return true;
		}
	};
	
	private OnPreferenceClickListener onFontSizeListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			mFontSizeDialog = new Dialog(mThis);

			mFontSizeDialog.setContentView(R.layout.font_size);
			mFontSizeDialog.setTitle("Set Default Font Size");

			mFontSizeText = (TextView) mFontSizeDialog.findViewById(R.id.fontSizeText);
			SeekBar bar = (SeekBar) mFontSizeDialog.findViewById(R.id.fontSizeBar);
			Button click = (Button) mFontSizeDialog.findViewById(R.id.fontSizeButton);
			
			click.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					mFontSizeDialog.dismiss();
				}
			});
			
	        bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					Editor sizeEdit = mPrefs.edit();
					sizeEdit.putInt("default_post_font_size_dip", seekBar.getProgress()+10);
					sizeEdit.commit();
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					mFontSizeText.setText((progress+10)+ "  Get out");
			        mFontSizeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, (progress+10));
				}
			});
	        bar.setProgress(mPrefs.getInt("default_post_font_size_dip", Constants.DEFAULT_FONT_SIZE)-10);
	        mFontSizeText.setText((bar.getProgress()+10)+ "  Get out");
	        mFontSizeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, (bar.getProgress()+10));
	        mFontSizeDialog.show();
			return true;
		}
	};
	
	// All keys representing int values whose Summaries should be set to their values
	private static final String[] VALUE_SUMMARY_KEYS_INT = { 
		"default_post_font_size_dip",
		"post_per_page"
		};
	private static final int[] VALUE_SUMMARY_DEFAULTS_INT = { 
		Constants.DEFAULT_FONT_SIZE,
		Constants.ITEMS_PER_PAGE
		};
	
	private static final String[] VALUE_SUMMARY_KEYS_LIST = {
		"orientation"
	};
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		setSummaries(); 
	}
	
	private void setSummaries() {
		for(int x=0;x<VALUE_SUMMARY_KEYS_INT.length;x++) {
			findPreference(VALUE_SUMMARY_KEYS_INT[x]).setSummary(String.valueOf(mPrefs.getInt(VALUE_SUMMARY_KEYS_INT[x], VALUE_SUMMARY_DEFAULTS_INT[x])));
		}
		for(String key : VALUE_SUMMARY_KEYS_LIST) {
			ListPreference p = (ListPreference) findPreference(key);
			p.setSummary(p.getEntry());
		}
		mUsernamePreference.setSummary(mPrefs.getString("username", "Not Set"));
		mColorsPreference.setSummary(WordUtils.capitalize(mPrefs.getString("themes", "Default"))+" Theme");
	}

}
