package com.ferg.awfulapp;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.sync.SyncManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;

public class AwfulApplication extends Application implements AwfulPreferences.AwfulPreferenceUpdate{
	private static final String TAG = "AwfulApplication";
	
	private AwfulPreferences mPref;
	private final HashMap<String, Typeface> fonts = new HashMap<>();
	private Typeface currentFont;

    @Override
    public void onCreate() {
        super.onCreate();
		NetworkUtils.init(this);
        mPref = AwfulPreferences.getInstance(this, this);
        onPreferenceChange(mPref,null);

		// work out how long it's been since the app was updated
		long hoursSinceInstall = Long.MAX_VALUE;
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			long millisSinceInstall = System.currentTimeMillis() - packageInfo.lastUpdateTime;
			hoursSinceInstall = TimeUnit.HOURS.convert(millisSinceInstall, TimeUnit.MILLISECONDS);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		Log.i(TAG, String.format("App installed %d hours ago", hoursSinceInstall));
		// enable Crashlytics on non-debug builds, or debug builds that have been installed for a while
		if (!Constants.DEBUG || hoursSinceInstall > 4) {
			Fabric.with(this, new Crashlytics());
			if(mPref.sendUsernameInReport){
				Crashlytics.setUserName(mPref.username);
			}
		}

		if (Constants.DEBUG) {
			Log.d("DEBUG!", "*\n*\n*Debug active\n*\n*");
			/*
			This checks destroyed cursors aren't left open, and crashes (with a log) if it finds one
			Really this is here to avoid introducing any more leaks, since there are some issues with
			too many open cursors
			*/
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectLeakedSqlLiteObjects()
					.penaltyLog()
					.penaltyDeath()
					.build());
		}

		SyncManager.sync(this);
    }

	public void setFontFromPreference(TextView textView, int flags){
		if(flags < 0 && textView.getTypeface() != null){
			flags = textView.getTypeface().getStyle();
		}else{
			flags = Typeface.NORMAL;
		}
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
	public void onPreferenceChange(AwfulPreferences prefs, String key) {
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
		fonts.put("default",Typeface.defaultFromStyle(Typeface.NORMAL));
		try {
			String[] files = getAssets().list("fonts");
			for(String file : files){
				String fileName = "fonts/"+file;
				fonts.put(fileName, Typeface.createFromAsset(getAssets(), fileName));
				Log.i(TAG, "Processed Font: "+fileName);
			}
		} catch (IOException | RuntimeException e) {
			e.printStackTrace();
		}
		onPreferenceChange(mPref, null);
	}

	@Override
	public File getCacheDir() {
		Log.e(TAG, "getCacheDir(): " + super.getCacheDir());
		return super.getCacheDir();
	}


    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if(level != Application.TRIM_MEMORY_UI_HIDDEN && level != Application.TRIM_MEMORY_BACKGROUND){
			NetworkUtils.clearImageCache();
        }
    }

    @Override
    public void onLowMemory() {
		super.onLowMemory();
		NetworkUtils.clearImageCache();
    }
}
