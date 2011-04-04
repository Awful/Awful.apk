package com.ferg.awful;

import android.app.Activity;
import android.os.Bundle;

/**
 * Convenience class to avoid having to call a configurator's lifecycle methods everywhere. This
 * class should avoid implementing things directly; the ActivityConfigurator does that job.
 * 
 * Most Activities in this awful app should extend this guy; that will provide things like locking
 * orientation according to user preference.
 */
public class AwfulActivity extends Activity {
	private ActivityConfigurator mConf;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mConf = new ActivityConfigurator(this);
		mConf.onCreate();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		mConf.onStart();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mConf.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mConf.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		mConf.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mConf.onDestroy();
	}
	
}
