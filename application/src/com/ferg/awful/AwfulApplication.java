package com.ferg.awful;

import android.app.Application;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.acra.*;
import org.acra.annotation.*;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import com.ferg.awful.constants.Constants;

//@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=d6a53a0d", formKey = Constants.ACRA_FORMKEY) 
@ReportsCrashes(formKey = "dFlKM0NmVlotelN0VDJPV0RfajlyUmc6MQ") 
public class AwfulApplication extends Application {
	private static String TAG = "AwfulApplication";
	private Typeface[] fonts;
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
        fonts = new Typeface[]{
        		Typeface.createFromAsset(getAssets(), "fonts/terminus.ttf"),
        		Typeface.createFromAsset(getAssets(), "fonts/terminus_bold.ttf")
        };
    }
    
    
    
    public void setFontFromPreference(TextView textView, int fontId, int flags){
    	textView.setTypeface(fonts[fontId], flags);
    }
    
    public void setFontFromPreferenceRecurse(ViewGroup viewGroup, int fontId, int flags){
    	for(int x=0;x<viewGroup.getChildCount();x++){
    		View child = viewGroup.getChildAt(x);
    		if(child instanceof TextView){
    			setFontFromPreference((TextView)child, fontId, flags);
    		}else if(child instanceof ViewGroup){
    			setFontFromPreferenceRecurse((ViewGroup)child, fontId, flags);
    		}
    	}
    }
}
