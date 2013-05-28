/********************************************************************************
 * Copyright (c) 2012, Matthew Shepard
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the software nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
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
 *******************************************************************************/

package com.ferg.awfulapp.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import com.ferg.awfulapp.AwfulUpdateCallback;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;

import java.util.ArrayList;
import java.util.Date;

/**
 * This class acts as a convenience wrapper and simple cache for commonly used preference values. 
 * Any changes made to primitive values will not carry over or affect the saved preferences.
 *
 */
public class AwfulPreferences implements OnSharedPreferenceChangeListener {
	private SharedPreferences mPrefs;
	private Context mContext;
	private ArrayList<AwfulUpdateCallback> mCallback = new ArrayList<AwfulUpdateCallback>();
	
	//GENERAL STUFF
	public String username;
	public int userId;
	public boolean hasPlatinum;
	public boolean hasArchives;
	public boolean hasNoAds;
	public boolean debugMode;
	public boolean sendUsernameInReport;
	public float scaleFactor;
	
	//THEME STUFF
	public int postFontSizeDip;
	public int postFontSizePx;
	public int postFontColor;
	public int postFontColor2;
	public int postBackgroundColor;
	public int postBackgroundColor2;
	public int postReadBackgroundColor;
	public int postReadBackgroundColor2;
	public int postReadFontColor;
	public int postOPColor;
	public int postLinkQuoteColor;
	public int postDividerColor;
	public boolean postDividerEnabled;
	public int postHeaderBackgroundColor;
	public int postHeaderFontColor;
	public int actionbarColor;
	public int actionbarFontColor;
	public boolean refreshFrog;
	public boolean lockScrolling;
	public int unreadCounterColor;
	public int unreadCounterColorDim;
	public boolean unreadCounterFontBlack;
	/**
	 * for selecting icon set
	 * light
	 * dark
	 */
	public String icon_theme;
	public String preferredFont;
	public boolean alternateBackground;
	
	//THREAD STUFF
	public int postPerPage;
	public boolean imagesEnabled;
	public boolean avatarsEnabled;
	public boolean showSmilies;
	public boolean hideOldImages;
    public boolean highlightUserQuote;
    public boolean highlightUsername;
	public boolean showAllSpoilers;
	public String imgurThumbnails;
	public boolean upperNextArrow;
	public boolean disableGifs;
	public boolean hideOldPosts;
	public boolean disableTimgs;
	public boolean volumeScroll;
	/**
	 * TO BE REMOVED
	 * forces threadview into specific layout, values: auto - phone - tablet 
	 */
	public String threadLayout;
	public boolean alwaysOpenUrls;
	
	//FORUM STUFF
	public boolean newThreadsFirstUCP;
	public boolean newThreadsFirstForum;
	public boolean threadInfo_Rating;
	public boolean threadInfo_Author;
	public boolean threadInfo_Killed;
	public boolean threadInfo_Page;
	public boolean threadInfo_Tag;
    public boolean wrapThreadTitles;
    
    //EXPERIMENTAL STUFF
    public boolean inlineYoutube;
    public boolean enableHardwareAcceleration;
    public boolean disablePullNext;
    public long probationTime;

    public int alertIDShown;
	
	private static final int PREFERENCES_VERSION = 1;
	private int currPrefVersion;


    /**
	 * Constructs a new AwfulPreferences object, registers preference change listener, and updates values.
	 * @param context
	 */
	public AwfulPreferences(Context context) {
		mContext = context;

		PreferenceManager.setDefaultValues(mContext, R.xml.settings, false);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mPrefs.registerOnSharedPreferenceChangeListener(this);
		updateValues(mPrefs);
		upgradePreferences();
	}
	
	public AwfulPreferences(Context context, AwfulUpdateCallback updateCallback){
		this(context);
		mCallback.add(updateCallback);
	}

	public void unRegisterListener(){
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	public SharedPreferences getPrefs(){
		return mPrefs;
	}
	
	public void registerCallback(AwfulUpdateCallback client){
		if(!mCallback.contains(client)){
			mCallback.add(client);
		}
	}
	
	public void unregisterCallback(AwfulUpdateCallback client){
		mCallback.remove(client);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		updateValues(prefs);
		for(AwfulUpdateCallback auc : mCallback){
			auc.onPreferenceChange(this);
		}
	}

    public void setUsername(String aUsername) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            mPrefs.edit().putString("username", aUsername).apply();
        } else {
            mPrefs.edit().putString("username", aUsername).commit();
        }
    }

	private void updateValues(SharedPreferences prefs) {
		Resources res = mContext.getResources();
		scaleFactor				 = res.getDisplayMetrics().density;
		username                 = mPrefs.getString("username", "Username");
		hasPlatinum              = mPrefs.getBoolean("has_platinum", false);
		hasArchives              = mPrefs.getBoolean("has_archives", false);
		hasNoAds         	     = mPrefs.getBoolean("has_no_ads", false);
		postFontSizeDip            = mPrefs.getInt("default_post_font_size_dip", Constants.DEFAULT_FONT_SIZE);
		postFontSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, postFontSizeDip, mContext.getResources().getDisplayMetrics());
		postFontColor            = mPrefs.getInt("default_post_font_color", res.getColor(R.color.default_post_font));
		postFontColor2           = mPrefs.getInt("secondary_post_font_color", res.getColor(R.color.secondary_post_font));
      	postBackgroundColor      = mPrefs.getInt("default_post_background_color", res.getColor(R.color.background));
       	postBackgroundColor2     = mPrefs.getInt("alternative_post_background_color", res.getColor(R.color.alt_background));
    	postReadBackgroundColor  = mPrefs.getInt("read_post_background_color", res.getColor(R.color.background_read));
    	postReadBackgroundColor2 = mPrefs.getInt("alternative_read_post_background_color", res.getColor(R.color.alt_background_read));
    	postReadFontColor  		 = mPrefs.getInt("read_post_font_color", res.getColor(R.color.font_read));
    	postOPColor              = mPrefs.getInt("op_post_color", res.getColor(R.color.op_post));
    	postLinkQuoteColor       = mPrefs.getInt("link_quote_color", res.getColor(R.color.link_quote));
      	postHeaderBackgroundColor      = mPrefs.getInt("post_header_background_color", res.getColor(R.color.forums_blue));
      	postHeaderFontColor      = mPrefs.getInt("post_header_font_color", res.getColor(R.color.forums_gray));
      	postDividerColor      	 = mPrefs.getInt("post_divider_color", res.getColor(R.color.holo_blue_light));
      	postDividerEnabled     	 = mPrefs.getBoolean("post_divider_enabled", false);
      	actionbarColor      	 = mPrefs.getInt("actionbar_color", res.getColor(R.color.actionbar_color));
      	actionbarFontColor       = mPrefs.getInt("actionbar_font_color", res.getColor(R.color.actionbar_font_color));
      	unreadCounterColor		 = mPrefs.getInt("unread_posts", res.getColor(R.color.unread_posts));
      	unreadCounterColorDim	 = mPrefs.getInt("unread_posts_dim", res.getColor(R.color.unread_posts_dim));
        unreadCounterFontBlack   = mPrefs.getBoolean("unread_posts_font_black", false);
        imagesEnabled            = mPrefs.getBoolean("images_enabled", true);
        avatarsEnabled           = mPrefs.getBoolean("avatars_enabled", true);
        hideOldImages            = mPrefs.getBoolean("hide_read_images", false);
        showSmilies              = mPrefs.getBoolean("show_smilies", true);
        postPerPage              = Math.max(Math.min(mPrefs.getInt("post_per_page", Constants.ITEMS_PER_PAGE), Constants.ITEMS_PER_PAGE),1);//can't make the preference page honor a max value
       	alternateBackground      = mPrefs.getBoolean("alternate_backgrounds",false);
        highlightUserQuote       = mPrefs.getBoolean("user_quotes", true);
        highlightUsername        = mPrefs.getBoolean("user_highlight", true);
        inlineYoutube            = mPrefs.getBoolean("inline_youtube", false);
        enableHardwareAcceleration = mPrefs.getBoolean("enable_hardware_acceleration", (Constants.isJellybean()?true:false));
        debugMode            	 = false;//= mPrefs.getBoolean("debug_mode", false);
        wrapThreadTitles		 = mPrefs.getBoolean("wrap_thread_titles", true);
        showAllSpoilers			 = mPrefs.getBoolean("show_all_spoilers", false);
        threadInfo_Rating		 = mPrefs.getBoolean("threadinfo_rating", false);
        threadInfo_Author		 = mPrefs.getBoolean("threadinfo_author", false);
        threadInfo_Killed		 = mPrefs.getBoolean("threadinfo_killed", true);
        threadInfo_Page		 	 = mPrefs.getBoolean("threadinfo_pages", true);
        threadInfo_Tag		 	 = mPrefs.getBoolean("threadinfo_tag", true);
        imgurThumbnails			 = mPrefs.getString("imgur_thumbnails", "d");
        threadLayout			 = (Constants.canBeWidescreen(mContext)? mPrefs.getString("page_layout", "auto") :"auto");
        newThreadsFirstUCP		 = mPrefs.getBoolean("new_threads_first_ucp", false);
        newThreadsFirstForum	 = mPrefs.getBoolean("new_threads_first_forum", false);
        preferredFont			 = mPrefs.getString("preferred_font", "default");
        icon_theme				 = mPrefs.getString("selected_theme", (Constants.isWidescreen(mContext)?"light":"dark"));//TODO update for proper dynamic tablet shit
        upperNextArrow		     = mPrefs.getBoolean("upper_next_arrow", false);
        sendUsernameInReport	 = mPrefs.getBoolean("send_username_in_report", true);
        disableGifs	 			 = mPrefs.getBoolean("disable_gifs2", true);
        hideOldPosts	 	 	 = mPrefs.getBoolean("hide_old_posts", false);
        alwaysOpenUrls	 	 	 = mPrefs.getBoolean("always_open_urls", false);
        refreshFrog				 = mPrefs.getBoolean("refresh_frog", false);
        lockScrolling			 = mPrefs.getBoolean("lock_scrolling", false);
        disableTimgs			 = mPrefs.getBoolean("disable_timgs", true);
        currPrefVersion          = mPrefs.getInt("curr_pref_version", 0);
        disablePullNext          = mPrefs.getBoolean("disable_pull_next", false);
        alertIDShown             = mPrefs.getInt("alert_id_shown", 0);
        volumeScroll         	 = mPrefs.getBoolean("volume_scroll", false);
        probationTime			 = mPrefs.getLong("probation_time", 0);
        userId					 = mPrefs.getInt("user_id", 0);
       	 //TODO: I have never seen this before oh god
	}

	public void setBooleanPreference(String key, boolean value) {
		if(Constants.isGingerbread()){
			mPrefs.edit().putBoolean(key, value).apply();
		}else{
			mPrefs.edit().putBoolean(key, value).commit();
		}
	}

	public void setStringPreference(String key, String value) {
		if(Constants.isGingerbread()){
			mPrefs.edit().putString(key, value).apply();
		}else{
			mPrefs.edit().putString(key, value).commit();
		}
	}

	public void setLongPreference(String key, long value) {
		if(Constants.isGingerbread()){
			mPrefs.edit().putLong(key, value).apply();
		}else{
			mPrefs.edit().putLong(key, value).commit();
		}
	}

	public void setIntegerPreference(String key, int value) {
		if(Constants.isGingerbread()){
			mPrefs.edit().putInt(key, value).apply();
		}else{
			mPrefs.edit().putInt(key, value).commit();
		}
	}
	
	public void upgradePreferences() {
		if(currPrefVersion < PREFERENCES_VERSION) {
			switch(currPrefVersion) {//this switch intentionally falls through!
			case 0:
				// Removing new_threads_first preference and applying it to new new_threads_first_ucp preference
				boolean newPrefsFirst = mPrefs.getBoolean("new_threads_first", false);
        		setBooleanPreference("new_threads_first_ucp", newPrefsFirst);
        		if(Constants.isGingerbread()){
        			mPrefs.edit().remove("new_threads_first").apply();
        		}else{
        			mPrefs.edit().remove("new_threads_first").commit();
        		}
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
}
