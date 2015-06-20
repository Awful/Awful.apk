package com.ferg.awfulapp;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import com.ferg.awfulapp.network.NetworkUtils;

import android.app.Application;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ferg.awfulapp.preferences.AwfulPreferences;

public class AwfulApplication extends Application implements AwfulPreferences.AwfulPreferenceUpdate{
	private static final String TAG = "AwfulApplication";
	
	private AwfulPreferences mPref;
	private final HashMap<String, Typeface> fonts = new HashMap<>();
	private Typeface currentFont;

    @Override
    public void onCreate() {
        super.onCreate();
		NetworkUtils.init(this);
        Fabric.with(this, new Crashlytics());
        mPref = AwfulPreferences.getInstance(this, this);
        onPreferenceChange(mPref,null);

        if(mPref.sendUsernameInReport){
			Crashlytics.setUserName(mPref.username);
        }
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
