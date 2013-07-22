package com.ferg.awfulapp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.os.Message;
import android.preference.PreferenceManager;

import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;

/**
 * Responsible for setting activity preferences which need to affect every activity in the app.
 * Standard activities do not need to deal with this class manually and can simply extend
 * {@link AwfulActivity} instead. Things that would prefer ListActivities or something should
 * follow {@link AwfulActivity}'s example and call one of this class's lifecycle methods along with
 * their own.
 *  
 */
public class ActivityConfigurator implements AwfulUpdateCallback {
	private Activity mActivity;
	private AwfulPreferences mPrefs;
	
	public ActivityConfigurator(Activity activity) {
		mActivity = activity;
		mPrefs = AwfulPreferences.getInstance(activity,this);
	}
	
	public void onCreate() {}
	
	public void onStart() {
		NetworkUtils.restoreLoginCookies(mActivity);
	}
	
	public void onResume() {setOrientation();}
	
	public void onPause() {	}
	
	public void onStop() {}
	
	public void onDestroy() {}
	
	private void setOrientation() {
		String orientationStr = mPrefs.orientation;
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

	@Override
	public void loadingFailed(Message aMsg) {}

	@Override
	public void loadingStarted(Message aMsg) {}

	@Override
	public void loadingUpdate(Message aMsg) {}

	@Override
	public void loadingSucceeded(Message aMsg) {}

	@Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		setOrientation();
	}
}
