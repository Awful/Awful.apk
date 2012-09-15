package com.ferg.awfulapp.preferences;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.TypedValue;

import com.ferg.awfulapp.AwfulUpdateCallback;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;

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
	public boolean hasPlatinum;
	public boolean debugMode;
	public boolean sendUsernameInReport;
	
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
	public String icon_theme;//for selecting icon set TODO rename setting to icon_theme
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
	
	//FORUM STUFF
	public boolean newThreadsFirst;
	public boolean threadInfo_Author;
	public boolean threadInfo_Killed;
	public boolean threadInfo_Page;
	public boolean threadInfo_Tag;
    public boolean wrapThreadTitles;
    
    //EXPERIMENTAL STUFF
    public boolean inlineYoutube;
	public boolean staticThreadView;

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
		Resources res = mContext.getResources();
		username                 = mPrefs.getString("username", "Username");
		hasPlatinum              = mPrefs.getBoolean("has_platinum", true);
		postFontSizeDip             = mPrefs.getInt("default_post_font_size_dip", Constants.DEFAULT_FONT_SIZE);
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
      	postDividerColor      	 = mPrefs.getInt("post_divider_color", res.getColor(R.color.abs__holo_blue_light));
      	postDividerEnabled     	 = mPrefs.getBoolean("post_divider_enabled", false);
      	actionbarColor      	 = mPrefs.getInt("actionbar_color", res.getColor(R.color.actionbar_color));
      	actionbarFontColor       = mPrefs.getInt("actionbar_font_color", res.getColor(R.color.actionbar_font_color));
        imagesEnabled            = mPrefs.getBoolean("images_enabled", true);
        avatarsEnabled           = mPrefs.getBoolean("avatars_enabled", true);
        hideOldImages            = mPrefs.getBoolean("hide_read_images", false);
        showSmilies              = mPrefs.getBoolean("show_smilies", true);
        postPerPage              = Math.max(Math.min(mPrefs.getInt("post_per_page", Constants.ITEMS_PER_PAGE), Constants.ITEMS_PER_PAGE),1);//can't make the preference page honor a max value
       	alternateBackground      = mPrefs.getBoolean("alternate_backgrounds",false);
        highlightUserQuote       = mPrefs.getBoolean("user_quotes", true);
        highlightUsername        = mPrefs.getBoolean("user_highlight", true);
        inlineYoutube            = mPrefs.getBoolean("inline_youtube", false);
        debugMode            	 = mPrefs.getBoolean("debug_mode", false);
        wrapThreadTitles		 = mPrefs.getBoolean("wrap_thread_titles", true);
        showAllSpoilers			 = mPrefs.getBoolean("show_all_spoilers", false);
        threadInfo_Author		 = mPrefs.getBoolean("threadinfo_author", false);
        threadInfo_Killed		 = mPrefs.getBoolean("threadinfo_killed", false);
        threadInfo_Page		 	 = mPrefs.getBoolean("threadinfo_pages", true);
        threadInfo_Tag		 	 = mPrefs.getBoolean("threadinfo_tag", true);
        imgurThumbnails			 = mPrefs.getString("imgur_thumbnails", "d");
        newThreadsFirst			 = mPrefs.getBoolean("new_threads_first", false);
        preferredFont			 = mPrefs.getString("preferred_font", "default");
        icon_theme				 = mPrefs.getString("selected_theme", (Constants.isWidescreen(mContext)?"light":"dark"));//TODO update for proper dynamic tablet shit
        upperNextArrow		     = mPrefs.getBoolean("upper_next_arrow", false);
        sendUsernameInReport	 = mPrefs.getBoolean("send_username_in_report", true);
        staticThreadView	 	 = mPrefs.getBoolean("static_thread_view", true);
       	 //TODO: I have never seen this before oh god
	}
}
