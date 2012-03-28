package com.ferg.awful;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import android.app.Application;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.acra.*;
import org.acra.annotation.*;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.preferences.AwfulPreferences;

//@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=d6a53a0d", formKey = Constants.ACRA_FORMKEY) 
@ReportsCrashes(formKey = "dFlKM0NmVlotelN0VDJPV0RfajlyUmc6MQ") 
public class AwfulApplication extends Application implements AwfulUpdateCallback{
	private static String TAG = "AwfulApplication";
	
	private AwfulPreferences mPref;
	private HashMap<String, Typeface> fonts = new HashMap<String, Typeface>();

	private Typeface currentFont;
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
        
        mPref = new AwfulPreferences(this, this);
        onPreferenceChange(mPref);
    }

	public void setFontFromPreference(TextView textView, int flags){
		if(fonts.size() == 0){
			processFonts();
		}
		if(currentFont != null){
			if(mPref.preferredFont.contains("mono")){
				switch(flags){
				case Typeface.BOLD:
					textView.setTypeface(currentFont, Typeface.MONOSPACE.BOLD);
					break;
				case Typeface.ITALIC:
					textView.setTypeface(currentFont, Typeface.MONOSPACE.ITALIC);
					break;
				case Typeface.BOLD_ITALIC:
					textView.setTypeface(currentFont, Typeface.MONOSPACE.BOLD_ITALIC);
					break;
				case Typeface.NORMAL:
				default:
					textView.setTypeface(currentFont, Typeface.MONOSPACE.NORMAL);
					break;
				}
			}else{
				textView.setTypeface(currentFont, flags);
			}
		}
    }
    
    public void setFontFromPreferenceRecurse(ViewGroup viewGroup, int flags){
    	for(int x=0;x<viewGroup.getChildCount();x++){
    		View child = viewGroup.getChildAt(x);
    		if(child instanceof TextView){
    			setFontFromPreference((TextView)child, flags);
    		}else if(child instanceof ViewGroup){
    			setFontFromPreferenceRecurse((ViewGroup)child, flags);
    		}
    	}
    }

	public void setPreferredFont(View view, int flags) {
		if(view instanceof TextView){
			setFontFromPreference((TextView)view, flags);
		}else if(view instanceof ViewGroup){
			setFontFromPreferenceRecurse((ViewGroup)view, flags);
		}
	}

	@Override
	public void loadingFailed() {}
	@Override
	public void loadingStarted() {}
	@Override
	public void loadingSucceeded() {}

	@Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		currentFont = fonts.get(mPref.preferredFont);
		Log.e(TAG,"FONT SELECTED: "+mPref.preferredFont);
	}

	public String[] getFontList() {
		if(fonts.size() == 0){
			processFonts();
		}
		Set<String> keys = fonts.keySet();
		for(String key : keys){
			Log.e(TAG,"Font: "+key);
		}
		return keys.toArray(new String[keys.size()]);
	}
	
	private void processFonts(){
		fonts.clear();
		try {
			String[] files = getAssets().list("fonts");
			for(String file : files){
				String fileName = "fonts/"+file;
				fonts.put(fileName, Typeface.createFromAsset(getAssets(), fileName));
				Log.i(TAG, "Processed Font: "+fileName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		onPreferenceChange(mPref);
	}
}
