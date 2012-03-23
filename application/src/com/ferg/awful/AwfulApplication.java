package com.ferg.awful;

import android.app.Application;
import android.util.Log;

import org.acra.*;
import org.acra.annotation.*;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import com.ferg.awful.constants.Constants;

//@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=d6a53a0d", formKey = Constants.ACRA_FORMKEY) 
@ReportsCrashes(formKey = "dFlKM0NmVlotelN0VDJPV0RfajlyUmc6MQ") 
public class AwfulApplication extends Application {
	private static String TAG = "AwfulApplication";

    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();

        GoogleAnalyticsTracker.getInstance().startNewSession("UA-26815058-1", this);

        try {
            GoogleAnalyticsTracker.getInstance().setCustomVar(1, "App Version", getPackageManager().getPackageInfo(getPackageName(), 0).versionName, 0);
        } catch (Exception e) {
            GoogleAnalyticsTracker.getInstance().setCustomVar(2, "Version Failure", "Failed to retrieve", 0);
        }

        GoogleAnalyticsTracker.getInstance().dispatch();
    }
}
