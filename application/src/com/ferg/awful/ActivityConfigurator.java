package com.ferg.awful;

import com.ferg.awful.network.NetworkUtils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;

/**
 * Responsible for setting activity preferences which need to affect every activity in the app.
 * Standard activities do not need to deal with this class manually and can simply extend
 * {@link AwfulActivity} instead. Things that would prefer ListActivities or something should
 * follow {@link AwfulActivity}'s example and call one of this class's lifecycle methods along with
 * their own.
 *  
 */
public class ActivityConfigurator implements OnSharedPreferenceChangeListener {
	private Activity mActivity;
	private SharedPreferences mPrefs;
	
	public ActivityConfigurator(Activity activity) {
		mActivity = activity;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
	}
	
	public void onCreate() {}
	
	public void onStart() {
		NetworkUtils.restoreLoginCookies(mActivity);
	}
	
	public void onResume() {
		setOrientation();
		mPrefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	public void onPause() {
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	public void onStop() {}
	
	public void onDestroy() {}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if(key.equals("orientation")) {
			setOrientation();
		}
	}
	
	private void setOrientation() {
		String orientationStr = mPrefs.getString("orientation", "default");
		int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
		if(orientationStr.equals("portrait")) {
			orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		} else if(orientationStr.equals("landscape")) {
			orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		} else if(orientationStr.equals("sensor")) {
			orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
		}
		mActivity.setRequestedOrientation(orientation);
	}
}
