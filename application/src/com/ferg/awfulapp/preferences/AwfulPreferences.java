package com.ferg.awfulapp.preferences;

import java.util.ArrayList;

import com.ferg.awfulapp.AwfulUpdateCallback;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.preference.PreferenceManager;

/**
 * This class acts as a convenience wrapper and simple cache for commonly used preference values. 
 * Any changes made to primitive values will not carry over or affect the saved preferences.
 *
 */
public class AwfulPreferences implements OnSharedPreferenceChangeListener {
	private SharedPreferences mPrefs;
	private Context mContext;
	private ArrayList<AwfulUpdateCallback> mCallback = new ArrayList<AwfulUpdateCallback>();
	
	public String username;
	public boolean hasPlatinum;
	public int postFontSize;
	public int postFontColor;
	public int postFontColor2;
	public int postBackgroundColor;
	public int postBackgroundColor2;
	public int postReadBackgroundColor;
	public int postReadBackgroundColor2;
	public int postReadFontColor;
	public int postOPColor;
	public int postLinkQuoteColor;
	public boolean imagesEnabled;
	public boolean avatarsEnabled;
	public boolean showSmilies;
	public boolean hideOldImages;
	public boolean alternateBackground;
	public int postPerPage;
    public boolean highlightUserQuote;
    public boolean highlightUsername;
    public boolean inlineYoutube;
    public boolean wrapThreadTitles;
	public boolean showAllSpoilers;
	public boolean threadInfo_Author;
	public boolean threadInfo_Killed;
	public boolean threadInfo_Page;
	public boolean threadInfo_Tag;
	public String imgurThumbnails;
	public boolean newThreadsFirst;
	public String preferredFont;
	public String theme;
	public int postDividerColor;
	public int postHeaderBackgroundColor;
	public int postHeaderFontColor;
	public boolean upperNextArrow;

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
		username                 = mPrefs.getString("username", "Username");
		hasPlatinum              = mPrefs.getBoolean("has_platinum", true);
		postFontSize             = mPrefs.getInt("default_post_font_size", 22);
		postFontColor            = mPrefs.getInt("default_post_font_color", mContext.getResources().getColor(R.color.default_post_font));
		postFontColor2           = mPrefs.getInt("secondary_post_font_color", mContext.getResources().getColor(R.color.secondary_post_font));
      	postBackgroundColor      = mPrefs.getInt("default_post_background_color", mContext.getResources().getColor(R.color.background));
       	postBackgroundColor2     = mPrefs.getInt("alternative_post_background_color", mContext.getResources().getColor(R.color.alt_background));
    	postReadBackgroundColor  = mPrefs.getInt("read_post_background_color", mContext.getResources().getColor(R.color.background_read));
    	postReadBackgroundColor2 = mPrefs.getInt("alternative_read_post_background_color", mContext.getResources().getColor(R.color.alt_background_read));
    	postReadFontColor  		 = mPrefs.getInt("read_post_font_color", mContext.getResources().getColor(R.color.font_read));
    	postOPColor              = mPrefs.getInt("op_post_color", mContext.getResources().getColor(R.color.op_post));
    	postLinkQuoteColor       = mPrefs.getInt("link_quote_color", mContext.getResources().getColor(R.color.link_quote));
      	postHeaderBackgroundColor      = mPrefs.getInt("post_header_background_color", mContext.getResources().getColor(R.color.forums_blue));
      	postHeaderFontColor      = mPrefs.getInt("post_header_font_color", mContext.getResources().getColor(R.color.forums_gray));
      	postDividerColor      = mPrefs.getInt("post_divider_color", mContext.getResources().getColor(R.color.abs__holo_blue_light));
        imagesEnabled            = mPrefs.getBoolean("images_enabled", true);
        avatarsEnabled            = mPrefs.getBoolean("avatars_enabled", true);
        hideOldImages            = mPrefs.getBoolean("hide_read_images", false);
        showSmilies              = mPrefs.getBoolean("show_smilies", true);
        postPerPage              = Math.min(mPrefs.getInt("post_per_page", Constants.ITEMS_PER_PAGE), Constants.ITEMS_PER_PAGE);//can't make the preference page honor a max value
       	alternateBackground      = mPrefs.getBoolean("alternate_backgrounds",false);
        highlightUserQuote       = mPrefs.getBoolean("user_quotes", true);
        highlightUsername        = mPrefs.getBoolean("user_highlight", true);
        inlineYoutube            = mPrefs.getBoolean("inline_youtube", false);
        wrapThreadTitles		 = mPrefs.getBoolean("wrap_thread_titles", true);
        showAllSpoilers			 = mPrefs.getBoolean("show_all_spoilers", false);
        threadInfo_Author		 = mPrefs.getBoolean("threadinfo_author", false);
        threadInfo_Killed		 = mPrefs.getBoolean("threadinfo_killed", false);
        threadInfo_Page		 	 = mPrefs.getBoolean("threadinfo_pages", true);
        threadInfo_Tag		 	 = mPrefs.getBoolean("threadinfo_tag", true);
        imgurThumbnails			 = mPrefs.getString("imgur_thumbnails", "d");
        newThreadsFirst			 = mPrefs.getBoolean("new_threads_first", false);
        preferredFont			 = mPrefs.getString("preferred_font", "default");
        theme					 = mPrefs.getString("selected_theme", (Constants.isWidescreen(mContext)?"light":"dark"));//TODO update for proper dynamic tablet shit
        upperNextArrow		     = mPrefs.getBoolean("upper_next_arrow", false);
       	 //TODO: I have never seen this before oh god
	}
}
