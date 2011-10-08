package com.ferg.awful.preferences;

import com.ferg.awful.R;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.service.AwfulServiceConnection;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

/**
 * This class acts as a convenience wrapper and simple cache for commonly used preference values. 
 * Any changes made to primitive values will not carry over or affect the saved preferences.
 *
 */
public class AwfulPreferences implements OnSharedPreferenceChangeListener {
	private SharedPreferences mPrefs;
	private Context parent;
	private AwfulServiceConnection mPrefChangeCallback;
	
    public String username;
	public int postFontSize;
	public int postFontColor;
	public int postFontColor2;
	public int postBackgroundColor;
	public int postBackgroundColor2;
	public int postReadBackgroundColor;
	public int postReadBackgroundColor2;
	public int postOPColor;
	public int postLinkQuoteColor;
	public boolean imagesEnabled;
	public boolean alternateBackground;
	public int postPerPage;
    public boolean highlightUserQuote;
    public boolean highlightUsername;
    public boolean inlineYoutube;
    public boolean wrapThreadTitles;

	/**
	 * Constructs a new AwfulPrefernences object, registers preference change listener, and updates values.
	 * @param context
	 */
	public AwfulPreferences(Context context) {
		parent = context;
		mPrefChangeCallback = null;
		PreferenceManager.setDefaultValues(parent, R.xml.settings, false);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(parent);
		mPrefs.registerOnSharedPreferenceChangeListener(this);
		updateValues(mPrefs);
	}

	public void unRegisterListener(){
		mPrefChangeCallback = null;
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	public SharedPreferences getPrefs(){
		return mPrefs;
	}
	
	public void registerCallback(AwfulServiceConnection asc){
		mPrefChangeCallback = asc;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		updateValues(prefs);
		if(mPrefChangeCallback != null){
			mPrefChangeCallback.sharedPreferenceChange();
		}
	}

	private void updateValues(SharedPreferences prefs) {
        username                 = mPrefs.getString("username", "Username");
		postFontSize             = mPrefs.getInt("default_post_font_size", 22);
		postFontColor            = mPrefs.getInt("default_post_font_color", parent.getResources().getColor(R.color.default_post_font));
		postFontColor2           = mPrefs.getInt("secondary_post_font_color", parent.getResources().getColor(R.color.secondary_post_font));
      	postBackgroundColor      = mPrefs.getInt("default_post_background_color", parent.getResources().getColor(R.color.background));
       	postBackgroundColor2     = mPrefs.getInt("alternative_post_background_color", parent.getResources().getColor(R.color.alt_background));
    	postReadBackgroundColor  = mPrefs.getInt("read_post_background_color", parent.getResources().getColor(R.color.background_read));
    	postReadBackgroundColor2 = mPrefs.getInt("alternative_read_post_background_color", parent.getResources().getColor(R.color.alt_background_read));
    	postOPColor              = mPrefs.getInt("op_post_color", parent.getResources().getColor(R.color.op_post));
    	postLinkQuoteColor       = mPrefs.getInt("link_quote_color", parent.getResources().getColor(R.color.link_quote));
        imagesEnabled            = mPrefs.getBoolean("images_enabled", true);
        postPerPage              = mPrefs.getInt("post_per_page", Constants.ITEMS_PER_PAGE);
       	alternateBackground      = mPrefs.getBoolean("alternate_backgrounds",false);
        highlightUserQuote       = mPrefs.getBoolean("user_quotes", true);
        highlightUsername        = mPrefs.getBoolean("user_highlight", true);
        inlineYoutube            = mPrefs.getBoolean("inline_youtube", false);
        wrapThreadTitles		 = mPrefs.getBoolean("wrap_thread_titles", true);
       	 //TODO: I have never seen this before oh god
	}
}
