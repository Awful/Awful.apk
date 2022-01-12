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
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import android.util.Log;
import android.util.TypedValue;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
    public String userAvatarUrl;
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
	public String launcherIcon;
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
    /** some user-specific validation key that's required when sending a request to ignore a user */
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
        void onPreferenceChange(AwfulPreferences preferences, @Nullable String key);
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
		updateValues();
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

	public SharedPreferences getSharedPrefs(){
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
		updateValues();
		for (AwfulPreferenceUpdate auc : mCallback.keySet()) {
			if (auc != null) {
				auc.onPreferenceChange(this, key);
			}
		}
	}

	private void updateValues() {
		Resources res = mContext.getResources();
		scaleFactor				 = res.getDisplayMetrics().density;
		username                 = getPreference(Keys.USERNAME, "Username");
        userAvatarUrl            = getPreference(Keys.USER_AVATAR_URL, (String) null);
		hasPlatinum              = getPreference(Keys.HAS_PLATINUM, false);
		hasArchives              = getPreference(Keys.HAS_ARCHIVES, false);
		hasNoAds         	     = getPreference(Keys.HAS_NO_ADS, false);
		postFontSizeSp = getPreference(Keys.POST_FONT_SIZE_SP, Constants.DEFAULT_FONT_SIZE_SP);
        postFixedFontSizeSp = getPreference(Keys.POST_FIXED_FONT_SIZE_SP, Constants.DEFAULT_FIXED_FONT_SIZE_SP);
		postFontSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, postFontSizeSp, mContext.getResources().getDisplayMetrics());
		theme					 = getPreference(Keys.THEME, "default.css");
		launcherIcon			 = getPreference(Keys.LAUNCHER_ICON, "frog");
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
		inlineYoutube            = getPreference(Keys.INLINE_YOUTUBE, true);
		inlineTweets             = getPreference(Keys.INLINE_TWEETS, true);
		inlineVines            	 = getPreference(Keys.INLINE_VINES, false);
		inlineWebm            	 = getPreference(Keys.INLINE_WEBM, true);
		autostartWebm            = getPreference(Keys.AUTOSTART_WEBM, false);
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
		immersionMode			 = getPreference(Keys.IMMERSION_MODE, false);
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
	public String getPreference(@Keys.StringPreference @StringRes int key,
								 @Nullable String defaultValue) {
		return mPrefs.getString(mResources.getString(key), defaultValue);
	}

	@NonNull
	public Set<String> getPreference(@Keys.StringSetPreference @StringRes int key,
									  @NonNull Set<String> defaultValue) {
		return mPrefs.getStringSet(mResources.getString(key), defaultValue);
	}

	public boolean getPreference(@Keys.BooleanPreference @StringRes int key, boolean defaultValue) {
		return mPrefs.getBoolean(mResources.getString(key), defaultValue);
	}

	public int getPreference(@Keys.IntPreference @StringRes int key, int defaultValue) {
		return mPrefs.getInt(mResources.getString(key), defaultValue);
	}

	public long getPreference(@Keys.LongPreference @StringRes int key, long defaultValue) {
		return mPrefs.getLong(mResources.getString(key), defaultValue);
	}

	public float getPreference(@Keys.FloatPreference @StringRes int key, float defaultValue) {
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


	/**
	 * Export the app's current preferences to a user-picked location.
	 *
	 * @param settingsUri the file/location to export to
	 * @return false if the export failed
	 * @see #importSettings(Uri) 
	 */
	public boolean exportSettings(@NonNull Uri settingsUri) {
		Map settings = mPrefs.getAll();
		Gson gson = new Gson();
		// serialise all SharedPreferences mappings to JSON
		String settingsJson = gson.toJson(settings);

		// save the JSON in binary format
		Log.i(TAG, "exporting settings to uri: " + settingsUri.getLastPathSegment());
		try (OutputStream out = getContext().getContentResolver().openOutputStream(settingsUri)) {
			out.write(settingsJson.getBytes());
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}


	/**
	 * Import an exported settings file, and apply it to the app, updating AwfulPreferences.
	 *
	 * @param settingsUri the file to import
	 * @return false if importing failed completely
	 * @see #exportSettings(Uri)
	 */
	public boolean importSettings(@NonNull Uri settingsUri) {
		Log.i(TAG, "importing settings from file: " + settingsUri.getLastPathSegment());
		BufferedReader br;
		try {
			InputStream in = getContext().getContentResolver().openInputStream(settingsUri);
			if (in == null) {
				Log.w(TAG, "importSettings: unable to get input stream for uri: " + settingsUri);
				return false;
			}
			br = new BufferedReader(new InputStreamReader(in));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}

		// read settings JSON file and deserialise into the types SharedPreferences dumps as
		Map<String, Object> settings;
		try {
			settings = new Gson().fromJson(br, new TypeToken<Map<String, Object>>() {
			}.getType());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		SharedPreferences.Editor editor = mPrefs.edit();

		// TODO: 15/12/2017 there's no checking here at all - need to handle any errors safely. What happens when a pref no longer exists, or has its type changed between versions?
		for (Map.Entry<String, Object> entry : settings.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			// basically switching on the value type so we can call the correct setter method :/
			if (value instanceof Boolean) {
				editor.putBoolean(key, (Boolean) value);
			} else if (value instanceof String) {
				editor.putString(key, (String) value);
			} else if (value instanceof Float) {
				editor.putFloat(key, (Float) value);
			} else if (value instanceof List) {
				// this one's a little different, list -> string set
				Set<String> values = new HashSet<>();
				for (Object item : (List) value) {
					values.add(item.toString());
				}
				editor.putStringSet(key, values);
			} else {
				// catch everything else - seems bad, look for Ints/Longs only
				// TODO: the following prefs currently export and import as doubles, and get cast to either int or long - why??
				/*
				default_post_fixed_font_size_dip is type Double -> parses as int (are these two legacy settings?)
				default_post_font_size_dip is type Double -> int
				curr_pref_version is type Double -> int
				last_version_seen is type Double -> int
				alert_id_shown is type Double => int
				probation_time is type Double -> long
		 		*/

				if (longKeys.contains(key)) {
					editor.putLong(key, ((Double) value).longValue());
				} else {
					editor.putInt(key, ((Double) value).intValue());
				}
				// TODO: 15/12/2017 once the doubles are sorted out, probably better to explicitly catch those types and have a safe failure default
			}
		}
		editor.apply();
		updateValues();
		return true;
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
