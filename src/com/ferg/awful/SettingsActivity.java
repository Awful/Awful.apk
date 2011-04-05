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

package com.ferg.awful;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Simple, purely xml driven preferences. Access using
 * {@link PreferenceManager#getDefaultSharedPreferences(android.content.Context)}
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private static final int DIALOG_ABOUT = 1;
	Preference mAboutPreference;
	
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
		
		mAboutPreference = getPreferenceScreen().findPreference("about");
		mAboutPreference.setOnPreferenceClickListener(onAboutListener);
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
			return new AlertDialog.Builder(this)
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
	
	// All keys representing int values whose Summaries should be set to their values
	private static final String[] VALUE_SUMMARY_KEYS_INT = { 
		"default_post_font_size" 
		};
	
	private static final String[] VALUE_SUMMARY_KEYS_LIST = {
		"orientation"
	};
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		for(String valueSummaryKey : VALUE_SUMMARY_KEYS_INT) {
			if(valueSummaryKey.equals(key)) {
				findPreference(key).setSummary(String.valueOf(prefs.getInt(key, 0)));
			}
		}
		
		for(String valueSummaryKey : VALUE_SUMMARY_KEYS_LIST) {
			if(valueSummaryKey.equals(key)) {
				ListPreference p = (ListPreference) findPreference(key);
				p.setSummary(p.getEntry());
			}
		}
	}
	
	private void setSummaries() {
		for(String key : VALUE_SUMMARY_KEYS_INT) {
			findPreference(key).setSummary(String.valueOf(mPrefs.getInt(key, 0)));
		}
		for(String key : VALUE_SUMMARY_KEYS_LIST) {
			ListPreference p = (ListPreference) findPreference(key);
			p.setSummary(p.getEntry());
		}
	}

}
