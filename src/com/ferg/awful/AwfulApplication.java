package com.ferg.awful;

import android.app.Application;
import android.util.Log;

import org.acra.*;
import org.acra.annotation.*;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import com.ferg.awful.constants.Constants;

@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=d6a53a0d", formKey = Constants.ACRA_FORMKEY) 
public class AwfulApplication extends Application {
	private static String TAG = "AwfulApplication";

    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();

        Log.i(TAG, "Starting instance");
        GoogleAnalyticsTracker.getInstance().startNewSession("UA-26815058-1", this);
        GoogleAnalyticsTracker.getInstance().dispatch();
    }
}
