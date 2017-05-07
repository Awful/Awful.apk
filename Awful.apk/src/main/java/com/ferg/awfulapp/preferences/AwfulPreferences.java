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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.util.AwfulUtils;
import com.google.gson.Gson;

import org.jsoup.nodes.Document;

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
	private final Resources mResources;
	private final WeakHashMap<AwfulPreferenceUpdate, Object> mCallback = new WeakHashMap<>();

    //GENERAL STUFF
    public String username;
    public String userTitle;
	/** this is only set when the user is on probation! See {@link com.ferg.awfulapp.util.AwfulError#checkPageErrors(Document, AwfulPreferences)} */
    public int userId;
    public boolean hasPlatinum;
    public boolean hasArchives;
    public boolean hasNoAds;
    public boolean sendUsernameInReport;
    public float scaleFactor;
    public String orientation;
    public String pageLayout;

    //THEME STUFF
    public int postFontSizeSp;
    public int postFixedFontSizeSp;
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
    public boolean alwaysOpenUrls;

    //FORUM STUFF
    public boolean newThreadsFirstUCP;
    public boolean newThreadsFirstForum;
    public boolean threadInfo_Rating;
    public boolean threadInfo_Tag;
    public boolean forumIndexShowSections;
	public boolean forumIndexShowSubtitles;
	public boolean forumIndexHideSubforums;

    //EXPERIMENTAL STUFF
    public boolean inlineYoutube;
    public boolean inlineTweets;
    public boolean inlineVines;
    public boolean inlineWebm;
	public boolean autostartWebm;
    public boolean disablePullNext;
    public long probationTime;
    public boolean showIgnoreWarning;
    public String ignoreFormkey;
    public Set<String> markedUsers;
    public Float p2rDistance;
    public boolean immersionMode;
    public String transformer;

	// APP VERSION STUFF
    public int alertIDShown;
	public int lastVersionSeen;

    private static final int PREFERENCES_VERSION = 1;
    private int currPrefVersion;

    private HashSet<String> longKeys;


    public interface AwfulPreferenceUpdate {
        void onPreferenceChange(AwfulPreferences preferences, String key);
    }

    /**
	 * Constructs a new AwfulPreferences object, registers preference change listener, and updates values.
	 * @param context
	 */
	private AwfulPreferences(@NonNull Context context) {
		mContext = context;
		mResources = context.getResources();
		// this is sort of redundant with what's going on in updateValues(), but best to be sure eh
		SettingsActivity.setDefaultsFromXml(context);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mPrefs.registerOnSharedPreferenceChangeListener(this);
		updateValues(mPrefs);
		upgradePreferences();
		
		longKeys = new HashSet<>();
		longKeys.add(mResources.getString(R.string.pref_key_probation_time));
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
		username                 = getPreference(Keys.USERNAME, "Username");
        userTitle                = getPreference(Keys.USER_TITLE, (String) null);
		hasPlatinum              = getPreference(Keys.HAS_PLATINUM, false);
		hasArchives              = getPreference(Keys.HAS_ARCHIVES, false);
		hasNoAds         	     = getPreference(Keys.HAS_NO_ADS, false);
		postFontSizeSp = getPreference(Keys.POST_FONT_SIZE_SP, Constants.DEFAULT_FONT_SIZE_SP);
        postFixedFontSizeSp = getPreference(Keys.POST_FIXED_FONT_SIZE_SP, Constants.DEFAULT_FIXED_FONT_SIZE_SP);
		postFontSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, postFontSizeSp, mContext.getResources().getDisplayMetrics());
		theme					 = getPreference(Keys.THEME, "default.css");
		layout					 = getPreference(Keys.LAYOUT, "default");
        imagesEnabled            = getPreference(Keys.IMAGES_ENABLED, true);
        no3gImages	             = getPreference(Keys.NO_3G_IMAGES, false);
        avatarsEnabled           = getPreference(Keys.AVATARS_ENABLED, true);
        hideOldImages            = getPreference(Keys.HIDE_OLD_IMAGES, false);
        showSmilies              = getPreference(Keys.SHOW_SMILIES, true);
        postPerPage              = getPreference(Keys.POST_PER_PAGE, Constants.ITEMS_PER_PAGE);
       	alternateBackground      = getPreference(Keys.ALTERNATE_BACKGROUND, false);
        highlightUserQuote       = getPreference(Keys.HIGHLIGHT_USER_QUOTE, true);
        highlightUsername        = getPreference(Keys.HIGHLIGHT_USERNAME, true);
        highlightSelf			 = getPreference(Keys.HIGHLIGHT_SELF, true);
        highlightOP				 = getPreference(Keys.HIGHLIGHT_OP, true);
		inlineYoutube            = getPreference(Keys.INLINE_YOUTUBE, false);
		inlineTweets             = getPreference(Keys.INLINE_TWEETS, false);
		inlineVines            	 = getPreference(Keys.INLINE_VINES, false);
		inlineWebm            	 = getPreference(Keys.INLINE_WEBM, false);
		autostartWebm            = getPreference(Keys.AUTOSTART_WEBM, true);
        showAllSpoilers			 = getPreference(Keys.SHOW_ALL_SPOILERS, false);
        threadInfo_Rating		 = getPreference(Keys.THREAD_INFO_RATING, true);
        threadInfo_Tag		 	 = getPreference(Keys.THREAD_INFO_TAG, true);
        imgurThumbnails			 = getPreference(Keys.IMGUR_THUMBNAILS, "d");
        newThreadsFirstUCP		 = getPreference(Keys.NEW_THREADS_FIRST_UCP, false);
        newThreadsFirstForum	 = getPreference(Keys.NEW_THREADS_FIRST_FORUM, false);
        preferredFont			 = getPreference(Keys.PREFERRED_FONT, "default");
        upperNextArrow		     = getPreference(Keys.UPPER_NEXT_ARROW, false);
        sendUsernameInReport	 = getPreference(Keys.SEND_USERNAME_IN_REPORT, true);
        disableGifs	 			 = getPreference(Keys.DISABLE_GIFS, true);
        hideOldPosts	 	 	 = getPreference(Keys.HIDE_OLD_POSTS, true);
        alwaysOpenUrls	 	 	 = getPreference(Keys.ALWAYS_OPEN_URLS, false);
        lockScrolling			 = getPreference(Keys.LOCK_SCROLLING, false);
        disableTimgs			 = getPreference(Keys.DISABLE_TIMGS, false);
        currPrefVersion          = getPreference(Keys.CURR_PREF_VERSION, 0);
        disablePullNext          = getPreference(Keys.DISABLE_PULL_NEXT, false);
        alertIDShown             = getPreference(Keys.ALERT_ID_SHOWN, 0);
		lastVersionSeen 		 = getPreference(Keys.LAST_VERSION_SEEN, 0);
		volumeScroll         	 = getPreference(Keys.VOLUME_SCROLL, false);
		forceForumThemes		 = getPreference(Keys.FORCE_FORUM_THEMES, true);
		noFAB					 = getPreference(Keys.NO_FAB, false);
		probationTime			 = getPreference(Keys.PROBATION_TIME, 0L);
        userId					 = getPreference(Keys.USER_ID, 0);
		showIgnoreWarning		 = getPreference(Keys.SHOW_IGNORE_WARNING, true);
		ignoreFormkey			 = getPreference(Keys.IGNORE_FORMKEY, (String) null);
		orientation				 = getPreference(Keys.ORIENTATION, "default");
		pageLayout				 = getPreference(Keys.PAGE_LAYOUT, "auto");
		coloredBookmarks		 = getPreference(Keys.COLORED_BOOKMARKS, false);
		p2rDistance				 = getPreference(Keys.P2R_DISTANCE, 0.5f);
		immersionMode = AwfulUtils.isKitKat() && getPreference(Keys.IMMERSION_MODE, false);
		hideSignatures  		 = getPreference(Keys.HIDE_SIGNATURES, false);
		transformer  		     = getPreference(Keys.TRANSFORMER, "Default");
		amberDefaultPos  		 = getPreference(Keys.AMBER_DEFAULT_POS, false);
		hideIgnoredPosts  		 = getPreference(Keys.HIDE_IGNORED_POSTS, false);
		markedUsers = getPreference(Keys.MARKED_USERS, new HashSet<>());
		forumIndexShowSections = getPreference(Keys.FORUM_INDEX_SHOW_SECTIONS, true);
		forumIndexShowSubtitles = getPreference(Keys.FORUM_INDEX_SHOW_SUBTITLES, true);
		forumIndexHideSubforums = getPreference(Keys.FORUM_INDEX_HIDE_SUBFORUMS, true);

        //I have never seen this before oh god
    }

	/*
		Type-checked preference getters

		Lint can't infer the correct signature by the type annotation, so if there's any
		ambiguity (e.g. int/long) then cast the value parameter according to the key type

		The @StringRes annotation is there to enforce storing keys as resource strings!
	 */

	@Nullable
	private String getPreference(@Keys.StringPreference @StringRes int key,
								 @Nullable String defaultValue) {
		return mPrefs.getString(mResources.getString(key), defaultValue);
	}

	@NonNull
	private Set<String> getPreference(@Keys.StringSetPreference @StringRes int key,
									  @NonNull Set<String> defaultValue) {
		return mPrefs.getStringSet(mResources.getString(key), defaultValue);
	}

	private boolean getPreference(@Keys.BooleanPreference @StringRes int key, boolean defaultValue) {
		return mPrefs.getBoolean(mResources.getString(key), defaultValue);
	}

	private int getPreference(@Keys.IntPreference @StringRes int key, int defaultValue) {
		return mPrefs.getInt(mResources.getString(key), defaultValue);
	}

	private long getPreference(@Keys.LongPreference @StringRes int key, long defaultValue) {
		return mPrefs.getLong(mResources.getString(key), defaultValue);
	}

	private float getPreference(@Keys.FloatPreference @StringRes int key, float defaultValue) {
		return mPrefs.getFloat(mResources.getString(key), defaultValue);
	}


	/*
		Type-checked preference setters

		Lint can't infer the correct signature by the type annotation, so if there's any
		ambiguity (e.g. int/long) then cast the value parameter according to the key type

		The @StringRes annotation is there to enforce storing keys as resource strings!
	 */

	public void setPreference(@Keys.StringPreference @StringRes int key,
							  @Nullable String value) {
		mPrefs.edit().putString(mResources.getString(key), value).apply();
	}

	public void setPreference(@Keys.StringSetPreference @StringRes int key,
							  @NonNull Set<String> value) {
		mPrefs.edit().putStringSet(mResources.getString(key), value).apply();
	}

	public void setPreference(@Keys.BooleanPreference @StringRes int key, boolean value) {
		mPrefs.edit().putBoolean(mResources.getString(key), value).apply();
	}

	public void setPreference(@Keys.IntPreference @StringRes int key, int value) {
		mPrefs.edit().putInt(mResources.getString(key), value).apply();
	}

	public void setPreference(@Keys.LongPreference @StringRes int key, long value) {
		mPrefs.edit().putLong(mResources.getString(key), value).apply();
	}

	public void setPreference(@Keys.FloatPreference @StringRes int key, float value) {
		mPrefs.edit().putFloat(mResources.getString(key), value).apply();
	}

	
	public void upgradePreferences() {
		if(currPrefVersion < PREFERENCES_VERSION) {
			switch(currPrefVersion) {//this switch intentionally falls through!
			case 0:
				// Get the current value of the obsolete preference, then remove it
				String obsoleteKey = "new_threads_first";
				boolean newThreadsFirst = mPrefs.getBoolean(obsoleteKey, false);
				mPrefs.edit().remove(obsoleteKey).apply();
				// transfer the value to the new key
				setPreference(Keys.NEW_THREADS_FIRST_UCP, newThreadsFirst);
        		newThreadsFirstUCP = newThreadsFirst;
				break;
			default://make sure to keep this break statement on the last case of this switch
				break;
			}

			//update the preferences so this doesn't run again
    		setPreference(Keys.CURR_PREF_VERSION, PREFERENCES_VERSION);
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
				setPreference(Keys.PROBATION_TIME, 0L);
				return false;
			}
			return true;
		}
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
			if ("Boolean".equals(classname)){
				mPrefs.edit().putBoolean(key, (Boolean) entry.getValue()).apply();
			} else if ("String".equals(classname)){
				mPrefs.edit().putString(key, (String) entry.getValue()).apply();
			} else if ("Float".equals(classname)) {
				mPrefs.edit().putFloat(key, (Float) entry.getValue()).apply();
			} else if ("ArrayList".equals(classname)) {
				mPrefs.edit().putStringSet(key, new HashSet<>((ArrayList<String>) entry.getValue())).apply();
			} else {
				if (longKeys.contains(key)) {
					mPrefs.edit().putLong(key, ((Double)entry.getValue()).longValue()).apply();
				} else {
					mPrefs.edit().putInt(key, ((Double)entry.getValue()).intValue()).apply();
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
		setPreference(Keys.MARKED_USERS, newMarkedUsers);
		markedUsers = newMarkedUsers;
	}
	
	public void unmarkUser(String username){
		Set<String> newMarkedUsers = new HashSet<String>(markedUsers);
		newMarkedUsers.remove(username);
		setPreference(Keys.MARKED_USERS, newMarkedUsers);
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
