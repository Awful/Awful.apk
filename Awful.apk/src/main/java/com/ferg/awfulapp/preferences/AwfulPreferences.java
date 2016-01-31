/**
 * *****************************************************************************
 * Copyright (c) 2012, Matthew Shepard
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the software nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *****************************************************************************
 */

package com.ferg.awfulapp.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.util.AwfulUtils;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * This class acts as a convenience wrapper and simple cache for commonly used preference values. 
 * Any changes made to primitive values will not carry over or affect the saved preferences.
 *
 */
public class AwfulPreferences implements OnSharedPreferenceChangeListener {

    private static final String TAG = "AwfulPreferences";

    private static AwfulPreferences mSelf;

    private SharedPreferences mPrefs;


    private Context mContext;
	private final WeakHashMap<AwfulPreferenceUpdate, Object> mCallback = new WeakHashMap<>();

    //GENERAL STUFF
    public String username;
    public String userTitle;
    public int userId;
    public boolean hasPlatinum;
    public boolean hasArchives;
    public boolean hasNoAds;
    public boolean debugMode;
    public boolean sendUsernameInReport;
    public float scaleFactor;
    public String orientation;
    public String pageLayout;

    //THEME STUFF
    public int postFontSizeDip;
    public int postFixedFontSizeDip;
    public int postFontSizePx;
    public boolean lockScrolling;
    public String theme;
    public boolean forceForumThemes;
    public String layout;
    public String preferredFont;
    public boolean alternateBackground;
    public boolean amberDefaultPos;

    //THREAD STUFF
    public int postPerPage;
    public boolean imagesEnabled;
    public boolean no3gImages;
    public boolean avatarsEnabled;
    public boolean showSmilies;
    public boolean hideOldImages;
    public boolean highlightUserQuote;
    public boolean highlightUsername;
    public boolean highlightSelf;
    public boolean highlightOP;
    public boolean showAllSpoilers;
    public String imgurThumbnails;
    public boolean upperNextArrow;
    public boolean disableGifs;
    public boolean hideOldPosts;
    public boolean disableTimgs;
    public boolean volumeScroll;
    public boolean coloredBookmarks;
	public boolean hideSignatures;
	public boolean hideIgnoredPosts;
    public boolean noFAB;
    /**
     * TO BE REMOVED
     * forces threadview into specific layout, values: auto - phone - tablet
     */
    public boolean alwaysOpenUrls;

    //FORUM STUFF
    public boolean newThreadsFirstUCP;
    public boolean newThreadsFirstForum;
    public boolean threadInfo_Rating;
    public boolean threadInfo_Tag;
    public boolean wrapThreadTitles;

    //EXPERIMENTAL STUFF
    public boolean inlineYoutube;
    public boolean inlineTweets;
    public boolean inlineVines;
    public boolean inlineWebm;
	public boolean autostartWebm;
    public boolean enableHardwareAcceleration;
    public boolean disablePullNext;
    public long probationTime;
    public boolean showIgnoreWarning;
    public String ignoreFormkey;
    public Set<String> markedUsers;
    public Float p2rDistance;
    public boolean immersionMode;
    public String transformer;

    public int alertIDShown;

    private static final int PREFERENCES_VERSION = 1;
    private int currPrefVersion;

    HashSet<String> longKeys;


    public interface AwfulPreferenceUpdate {
        void onPreferenceChange(AwfulPreferences preferences, String key);
    }

    /**
	 * Constructs a new AwfulPreferences object, registers preference change listener, and updates values.
	 * @param context
	 */
	private AwfulPreferences(Context context) {
		mContext = context;
		// this is sort of redundant with what's going on in updateValues(), but best to be sure eh
		SettingsActivity.setDefaultsFromXml(context);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mPrefs.registerOnSharedPreferenceChangeListener(this);
		updateValues(mPrefs);
		upgradePreferences();
		
		longKeys = new HashSet<>();
		longKeys.add("probation_time");
	}

	
	public static AwfulPreferences getInstance(){
		return mSelf;
	}
	
	public static AwfulPreferences getInstance(Context context){
		if(mSelf == null){
			mSelf = new AwfulPreferences(context);
		}
		return mSelf;
	}
	
	public static AwfulPreferences getInstance(Context context, AwfulPreferenceUpdate updateCallback) {
		AwfulPreferences instance = getInstance(context);
		instance.registerCallback(updateCallback);
		return instance;
	}

	public void unRegisterListener(){
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	public SharedPreferences getPrefs(){
		return mPrefs;
	}
	
	public void registerCallback(AwfulPreferenceUpdate client) {
		mCallback.put(client, null);
	}
	
	public void unregisterCallback(AwfulPreferenceUpdate client){
		mCallback.remove(client);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		updateValues(prefs);
		for (AwfulPreferenceUpdate auc : mCallback.keySet()) {
			if (auc != null) {
				auc.onPreferenceChange(this, key);
			}
		}
	}

	private void updateValues(SharedPreferences prefs) {
		Resources res = mContext.getResources();
		scaleFactor				 = res.getDisplayMetrics().density;
		username                 = mPrefs.getString("username", "Username");
        userTitle                = mPrefs.getString("user_title", null);
		hasPlatinum              = mPrefs.getBoolean("has_platinum", false);
		hasArchives              = mPrefs.getBoolean("has_archives", false);
		hasNoAds         	     = mPrefs.getBoolean("has_no_ads", false);
		postFontSizeDip            = mPrefs.getInt("default_post_font_size_dip", Constants.DEFAULT_FONT_SIZE);
        postFixedFontSizeDip     = mPrefs.getInt("default_post_fixed_font_size_dip", Constants.DEFAULT_FIXED_FONT_SIZE);
		postFontSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, postFontSizeDip, mContext.getResources().getDisplayMetrics());
		theme					 = mPrefs.getString("theme", "default.css");
		layout					 = mPrefs.getString("layouts", "default");
        imagesEnabled            = mPrefs.getBoolean("images_enabled", true);
        no3gImages	             = mPrefs.getBoolean("no_3g_images", false);
        avatarsEnabled           = mPrefs.getBoolean("avatars_enabled", true);
        hideOldImages            = mPrefs.getBoolean("hide_read_images", false);
        showSmilies              = mPrefs.getBoolean("show_smilies", true);
        postPerPage              = mPrefs.getInt("posts_per_page", Constants.ITEMS_PER_PAGE);
       	alternateBackground      = mPrefs.getBoolean("alternate_backgrounds", false);
        highlightUserQuote       = mPrefs.getBoolean("user_quotes", true);
        highlightUsername        = mPrefs.getBoolean("user_highlight", true);
        highlightSelf			 = mPrefs.getBoolean("self_highlight", true);
        highlightOP				 = mPrefs.getBoolean("op_highlight", true);
		inlineYoutube            = mPrefs.getBoolean("inline_youtube", false);
		inlineTweets             = mPrefs.getBoolean("inline_tweets", false);
		inlineVines            	 = mPrefs.getBoolean("inline_vines", false);
		inlineWebm            	 = mPrefs.getBoolean("inline_webm", false);
		autostartWebm            = mPrefs.getBoolean("autostart_webm", true);
        enableHardwareAcceleration = mPrefs.getBoolean("enable_hardware_acceleration", AwfulUtils.isJellybean());
        debugMode            	 = false;//= mPrefs.getBoolean("debug_mode", false);
        wrapThreadTitles		 = mPrefs.getBoolean("wrap_thread_titles", false);
        showAllSpoilers			 = mPrefs.getBoolean("show_all_spoilers", false);
        threadInfo_Rating		 = mPrefs.getBoolean("threadinfo_rating", true);
        threadInfo_Tag		 	 = mPrefs.getBoolean("threadinfo_tag", true);
        imgurThumbnails			 = mPrefs.getString("imgur_thumbnails", "d");
        newThreadsFirstUCP		 = mPrefs.getBoolean("new_threads_first_ucp", false);
        newThreadsFirstForum	 = mPrefs.getBoolean("new_threads_first_forum", false);
        preferredFont			 = mPrefs.getString("preferred_font", "default");
        upperNextArrow		     = mPrefs.getBoolean("upper_next_arrow", false);
        sendUsernameInReport	 = mPrefs.getBoolean("send_username_in_report", true);
        disableGifs	 			 = mPrefs.getBoolean("disable_gifs2", true);
        hideOldPosts	 	 	 = mPrefs.getBoolean("hide_old_posts", true);
        alwaysOpenUrls	 	 	 = mPrefs.getBoolean("always_open_urls", false);
        lockScrolling			 = mPrefs.getBoolean("lock_scrolling", false);
        disableTimgs			 = mPrefs.getBoolean("disable_timgs", false);
        currPrefVersion          = mPrefs.getInt("curr_pref_version", 0);
        disablePullNext          = mPrefs.getBoolean("disable_pull_next", false);
        alertIDShown             = mPrefs.getInt("alert_id_shown", 0);
        volumeScroll         	 = mPrefs.getBoolean("volume_scroll", false);
		forceForumThemes		 = mPrefs.getBoolean("force_forum_themes", true);
		noFAB					 = mPrefs.getBoolean("no_fab", false);
        probationTime			 = mPrefs.getLong("probation_time", 0);
        userId					 = mPrefs.getInt("user_id", 0);
        showIgnoreWarning		 = mPrefs.getBoolean("show_ignore_warning", true);
        ignoreFormkey			 = mPrefs.getString("ignore_formkey", null);
		orientation				 = mPrefs.getString("orientation", "default");
		pageLayout				 = mPrefs.getString("page_layout", "auto");
        coloredBookmarks		 = mPrefs.getBoolean("color_bookmarks", false);
        p2rDistance				 = mPrefs.getFloat("pull_to_refresh_distance", 0.5f);
        immersionMode			 = AwfulUtils.isKitKat() && mPrefs.getBoolean("immersion_mode", false);
        hideSignatures  		 = mPrefs.getBoolean("hide_signatures", false);
        transformer  		     = mPrefs.getString("transformer", "Default");
		amberDefaultPos  		 = mPrefs.getBoolean("amber_default_pos", false);
		hideIgnoredPosts  		 = mPrefs.getBoolean("hide_ignored_posts", false);
        markedUsers				 = mPrefs.getStringSet("marked_users", new HashSet<String>());

        //I have never seen this before oh god
    }

	public void setBooleanPreference(String key, boolean value) {
		mPrefs.edit().putBoolean(key, value).apply();
	}

	public void setStringPreference(String key, String value) {
        mPrefs.edit().putString(key, value).apply();
	}

	public void setLongPreference(String key, long value) {

		mPrefs.edit().putLong(key, value).apply();
	}

	public void setIntegerPreference(String key, int value) {
		mPrefs.edit().putInt(key, value).apply();
	}
	
	public void setFloatPreference(String key, float value) {
		mPrefs.edit().putFloat(key, value).apply();
	}
	
	public void setStringSetPreference(String key, Set<String> value){
    	mPrefs.edit().putStringSet(key, value).apply();
	}
	
	public void upgradePreferences() {
		if(currPrefVersion < PREFERENCES_VERSION) {
			switch(currPrefVersion) {//this switch intentionally falls through!
			case 0:
				// Removing new_threads_first preference and applying it to new new_threads_first_ucp preference
				boolean newPrefsFirst = mPrefs.getBoolean("new_threads_first", false);
        		setBooleanPreference("new_threads_first_ucp", newPrefsFirst);
        		mPrefs.edit().remove("new_threads_first").apply();
        		newThreadsFirstUCP = newPrefsFirst;
				break;
			default://make sure to keep this break statement on the last case of this switch
				break;
			}

			//update the preferences so this doesn't run again
    		setIntegerPreference("curr_pref_version", PREFERENCES_VERSION);
    		currPrefVersion = PREFERENCES_VERSION;
		}
	}
	

	public Resources getResources(){
		return mContext.getResources();
	}
	
	public boolean isOnProbation(){
		if(probationTime == 0){
			return false;
		}else{
			if(new Date(probationTime).compareTo(new Date()) < 0){
				setLongPreference("probation_time", 0);
				return false;
			}
			return true;
		}
	}
	
	public boolean hasFlash(){
		try {
		  PackageManager pm =  mContext.getPackageManager();
		  ApplicationInfo ai = pm.getApplicationInfo("com.adobe.flashplayer", 0);
		  if (ai != null)
		    return true;
		} catch (NameNotFoundException e) {
			return false;
		}
		return false;
	}

	public boolean canLoadImages() {
		ConnectivityManager conman = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		return imagesEnabled && !(no3gImages && !conman.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected());
	}
	
	public boolean canLoadAvatars(){
		return avatarsEnabled && canLoadImages();
	}
	
	public void exportSettings(){
		Map settings = mPrefs.getAll();
		Calendar date = Calendar.getInstance();
		Gson gson = new Gson();
		String settingsJson = gson.toJson(settings);
	    try {
		PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);

			if(Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)){
				File awfulFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/awful");
				
				if(!awfulFolder.exists()){
					if (!awfulFolder.mkdir()) {
                        Log.i(TAG, "failed to create missing awful folder!");
					}
				}
				Log.i(TAG, "exporting settings to file: awful-"+pInfo.versionCode+"-"+date.get(Calendar.DATE)+"-"+(date.get(Calendar.MONTH)+1)+"-"+date.get(Calendar.YEAR)+".settings");

	        	FileOutputStream out = new FileOutputStream(new File(awfulFolder.getAbsolutePath(), "awful-"+pInfo.versionCode+"-"+date.get(Calendar.DATE)+"-"+(date.get(Calendar.MONTH)+1)+"-"+date.get(Calendar.YEAR)+".settings"));
	        	out.write(settingsJson.getBytes());
	        	out.close();
	        }
	    }
	    catch (IOException e) {
			e.printStackTrace();
	    } catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		Toast.makeText(getContext(), "Settings exported", Toast.LENGTH_LONG).show();
	}
	
	public void importSettings(File settingsFile){
		Log.i(TAG, "importing settings from file: "+settingsFile.getName());
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(settingsFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		Gson gson = new Gson();
		Map settings = gson.fromJson(br, mPrefs.getAll().getClass());
		for (Object setting : settings.entrySet()) {
			HashMap.Entry entry = (HashMap.Entry) setting;
			String classname = entry.getValue().getClass().getSimpleName();
            String key = (String)entry.getKey();
			if("Boolean".equals(classname)){
				setBooleanPreference(key, (Boolean)entry.getValue());
			}else if("String".equals(classname)){
				setStringPreference(key, (String)entry.getValue());
			}else if("Float".equals(classname)) {
				setFloatPreference(key, (Float) entry.getValue());
			}else if("ArrayList".equals(classname)){
				setStringSetPreference(key, new HashSet<String>((ArrayList<String>)entry.getValue()));
			}else{
				if(longKeys.contains(key)){
					setLongPreference(key, ((Double)entry.getValue()).longValue());
				}else{
					setIntegerPreference(key, ((Double)entry.getValue()).intValue());
				}
			}
		}
		updateValues(mPrefs);
	}
	
	@Override
	protected void finalize() throws Throwable {
		unRegisterListener();
		super.finalize();
	}
	
	public void markUser(String username){
		Set<String> newMarkedUsers = new HashSet<String>(markedUsers);
		newMarkedUsers.add(username);
		setStringSetPreference("marked_users", newMarkedUsers);
		markedUsers = newMarkedUsers;
	}
	
	public void unmarkUser(String username){
		Set<String> newMarkedUsers = new HashSet<String>(markedUsers);
		newMarkedUsers.remove(username);
		setStringSetPreference("marked_users", newMarkedUsers);
		markedUsers = newMarkedUsers;
	}

    /**
     * Only use in emergencies, terrible hack
     * @returns a context
     */
    public Context getContext() {
        return mContext;
    }

}
