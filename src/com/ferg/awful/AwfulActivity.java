package com.ferg.awful;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.support.v4.app.FragmentActivity;

import com.ferg.awful.service.AwfulServiceConnection;

/**
 * Convenience class to avoid having to call a configurator's lifecycle methods everywhere. This
 * class should avoid implementing things directly; the ActivityConfigurator does that job.
 * 
 * Most Activities in this awful app should extend this guy; that will provide things like locking
 * orientation according to user preference.
 * 
 * This class also provides a few helper methods for grabbing preferences and the like.
 */
public class AwfulActivity extends FragmentActivity {
    private ActivityConfigurator mConf;
    private AwfulServiceConnection mService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConf = new ActivityConfigurator(this);
        mConf.onCreate();
        mService = new AwfulServiceConnection();
        mService.connect(this);
    }

    public AwfulServiceConnection getServiceConnection(){
        return mService;
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
        mService.disconnect(this);
    }

    public boolean isHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }
}
