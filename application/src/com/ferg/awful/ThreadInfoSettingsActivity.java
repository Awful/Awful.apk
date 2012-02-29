package com.ferg.awful;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class ThreadInfoSettingsActivity extends PreferenceActivity {
	ActivityConfigurator mConf;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        new Thread(new Runnable() {
            public void run() {
                GoogleAnalyticsTracker.getInstance().trackPageView("/ImageSettingsActivity");
                GoogleAnalyticsTracker.getInstance().dispatch();
            }
        }).start();

		mConf = new ActivityConfigurator(this);
		mConf.onCreate();
		addPreferencesFromResource(R.xml.threadinfosettings);
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
}
