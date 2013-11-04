package com.ferg.awfulapp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.ferg.awfulapp.util.AwfulUtils;
import com.ferg.awfulapp.util.LRUImageCache;
import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.graphics.Typeface;
import android.net.http.HttpResponseCache;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;

@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=9bf6cd4d", formKey = Constants.ACRA_FORMKEY) 
public class AwfulApplication extends Application implements AwfulPreferences.AwfulPreferenceUpdate{
	private static String TAG = "AwfulApplication";
	
	private AwfulPreferences mPref;
	private HashMap<String, Typeface> fonts = new HashMap<String, Typeface>();

    private RequestQueue networkQueue;
    private ImageLoader imageLoader;
    private LRUImageCache imageCache;

	private Typeface currentFont;
    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();

        mPref = AwfulPreferences.getInstance(this, this);
        onPreferenceChange(mPref);
        if(mPref.sendUsernameInReport){
        	ACRA.getErrorReporter().putCustomData("SA Username", mPref.username);
        }

        if(AwfulUtils.isICS()){
            try {
                HttpResponseCache.install(new File(getCacheDir(), "httpcache"), 5242880);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        networkQueue = Volley.newRequestQueue(this);
        imageCache = new LRUImageCache();
        imageLoader = new ImageLoader(networkQueue, imageCache);
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
	public void onPreferenceChange(AwfulPreferences prefs) {
		currentFont = fonts.get(mPref.preferredFont);
		Log.e(TAG,"FONT SELECTED: "+mPref.preferredFont);
        if(mPref.sendUsernameInReport){
        	ACRA.getErrorReporter().putCustomData("SA Username", mPref.username);
        }else{
            ACRA.getErrorReporter().removeCustomData("SA Username");
        }
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
		} catch (IOException e) {
			e.printStackTrace();
		} catch( RuntimeException e){
			e.printStackTrace();
		}
		onPreferenceChange(mPref);
	}

	@Override
	public File getCacheDir() {
		Log.e(TAG,"getCacheDir(): "+super.getCacheDir());
		return super.getCacheDir();
	}

    public void queueRequest(Request request){
        networkQueue.add(request);
    }

    public void cancelRequests(Object tag){
        networkQueue.cancelAll(tag);
    }

    public ImageLoader getImageLoader(){
        return imageLoader;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if(level != Application.TRIM_MEMORY_UI_HIDDEN && level != Application.TRIM_MEMORY_BACKGROUND && imageCache != null){
            imageCache.clear();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if(imageCache != null){
            imageCache.clear();
        }
    }

    public void clearDiskCache(){
        if(networkQueue != null && networkQueue.getCache() != null){
            networkQueue.getCache().clear();
        }
    }
}
