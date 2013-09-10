package com.ferg.awful;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.ferg.awful.R;


public class ImageSettingsActivity extends PreferenceActivity {
	ActivityConfigurator mConf;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mConf = new ActivityConfigurator(this);
		mConf.onCreate();
		addPreferencesFromResource(R.xml.imagesettings);
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
