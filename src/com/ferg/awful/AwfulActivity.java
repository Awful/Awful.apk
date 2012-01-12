package com.ferg.awful;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import android.support.v4.app.FragmentActivity;

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

    public boolean isTablet() {
        Configuration config = getResources().getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // If it's a Honeycomb device, it has to be a tablet
            return true;
        } else if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) && config.smallestScreenWidthDp >= 600) {
            // If it's 3.2+ and the smallest screen width is at least a 7" device, it's a tablet
            return true;
        }

        return false;
    }

    public static boolean useLegacyActionbar() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;
    }
}
