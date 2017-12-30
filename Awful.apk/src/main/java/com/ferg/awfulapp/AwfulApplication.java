package com.ferg.awfulapp;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.ferg.awfulapp.announcements.AnnouncementsManager;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.sync.SyncManager;
import com.jakewharton.threetenabp.AndroidThreeTen;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class AwfulApplication extends Application implements AwfulPreferences.AwfulPreferenceUpdate{
	private static final String TAG = "AwfulApplication";
	private static final String APP_STATE_PREFERENCES = "app_state_prefs";
	/**
	 * Used for storing misc app data, separate from user preferences, so onPreferenceChange callbacks aren't triggered
	 */
	private static SharedPreferences appStatePrefs;
    private static boolean crashlyticsEnabled = false;

	private AwfulPreferences mPref;
	private final HashMap<String, Typeface> fonts = new HashMap<>();
	private Typeface currentFont;

    @Override
    public void onCreate() {
        super.onCreate();
		// initialise the AwfulPreferences singleton first since a lot of things rely on it for a Context
		mPref = AwfulPreferences.getInstance(this, this);
		appStatePrefs = this.getSharedPreferences(APP_STATE_PREFERENCES, MODE_PRIVATE);

		if(BuildConfig.DEBUG) {
			Timber.plant(new Timber.DebugTree());
		}

		NetworkUtils.init(this);
		AndroidThreeTen.init(this);
		AnnouncementsManager.init();
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
		crashlyticsEnabled = !Constants.DEBUG || hoursSinceInstall > 4;
        if (crashlyticsEnabled) {
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


    /**
     * Returns true if the Crashlytics singleton has been initialised and can be used.
     */
    public static boolean crashlyticsEnabled() {
        return crashlyticsEnabled;
    }


	/**
	 * Get the SharedPreferences used for storing basic app state.
	 * <p>
	 * These are separate from the default shared preferences, and won't trigger onPreferenceChange callbacks.
	 *
	 * @see AwfulPreferences.AwfulPreferenceUpdate#onPreferenceChange(AwfulPreferences, String)
	 */
	public static SharedPreferences getAppStatePrefs() {
		return appStatePrefs;
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
