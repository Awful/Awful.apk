package com.ferg.awfulapp.provider;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.preferences.AwfulPreferences;

public class ColorProvider {
	
	public static final String DEFAULT 	= "default.css";
	public static final String DARK 	= "dark.css";
	public static final String CLASSIC 	= "classic.css";
	public static final String YOSPOS 	= "yospos.css";
	public static final String AMBERPOS = "amberpos.css";

    public static final int[] BOOKMARK_COLORS = {R.color.bookmark_default, R.color.bookmark_orange, R.color.bookmark_red, R.color.bookmark_yellow};
    public static final int[] BOOKMARK_COLORS_DIM = {R.color.bookmark_default_dim, R.color.bookmark_orange_dim, R.color.bookmark_red_dim, R.color.bookmark_yellow_dim};
	
	private static AwfulPreferences prefs = AwfulPreferences.getInstance();

	public static int getTextColor(String theme){
		if(theme == null){
			theme = prefs.theme;
		}
		if(theme.endsWith(DARK)){
			return prefs.getResources().getColor(R.color.dark_default_post_font);
		}
		if(theme.endsWith(YOSPOS)){
			return prefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if(theme.endsWith(AMBERPOS)){
			return prefs.getResources().getColor(R.color.amberpos_default_post_font);
		}
		if(theme.endsWith(CLASSIC)){
			return prefs.getResources().getColor(R.color.default_post_font);
		}
		return prefs.getResources().getColor(R.color.default_post_font);
	}
	
	public static int getTextColor(){
		return getTextColor(null);
	}
	
	public static int getAltTextColor(String theme){
		if(theme == null){
			theme = prefs.theme;
		}
		if(theme.endsWith(DARK)){
			return prefs.getResources().getColor(R.color.dark_secondary_post_font);
		}
		if(theme.endsWith(YOSPOS)){
			return prefs.getResources().getColor(R.color.yospos_secondary_post_font);
		}
		if(theme.endsWith(AMBERPOS)){
			return prefs.getResources().getColor(R.color.amberpos_secondary_post_font);
		}
		if(theme.endsWith(CLASSIC)){
			return prefs.getResources().getColor(R.color.secondary_post_font);
		}
		return prefs.getResources().getColor(R.color.secondary_post_font);
	}
	
	public static int getAltTextColor(){
		return getAltTextColor(null);
	}
	
	public static int getBackgroundColor(String theme){
		if(theme == null){
			theme = prefs.theme;
		}
		if(theme.endsWith(DARK)){
			return prefs.getResources().getColor(R.color.dark_background);
		}
		if(theme.endsWith(YOSPOS)){
			return prefs.getResources().getColor(R.color.yospos_background);
		}
		if(theme.endsWith(AMBERPOS)){
			return prefs.getResources().getColor(R.color.amberpos_background);
		}
		if(theme.endsWith(CLASSIC)){
			return prefs.getResources().getColor(R.color.background);
		}
		return prefs.getResources().getColor(R.color.background);
	}
	
	public static int getBackgroundColor(){
		return getBackgroundColor(null);
	}

	public static int getUnreadColor(String theme, boolean dim, int bookmarked){
        if(prefs.coloredBookmarks && bookmarked > 0 && bookmarked < BOOKMARK_COLORS.length){
            if(dim){
                return prefs.getResources().getColor(BOOKMARK_COLORS_DIM[bookmarked]);
            }else{
                return prefs.getResources().getColor(BOOKMARK_COLORS[bookmarked]);
            }
        }
        if(dim){
            return getUnreadColorDim(theme);
        }
		if(theme == null){
			theme = prefs.theme;
		}
		if(theme.endsWith(DARK)){
			return prefs.getResources().getColor(R.color.bookmark_default);
		}
		if(theme.endsWith(YOSPOS)){
			return prefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if(theme.endsWith(AMBERPOS)){
			return prefs.getResources().getColor(R.color.amberpos_default_post_font);
		}
		if(theme.endsWith(CLASSIC)){
			return prefs.getResources().getColor(R.color.bookmark_default);
		}
		return prefs.getResources().getColor(R.color.bookmark_default);
	}

	public static int getUnreadColorDim(String theme){
		if(theme == null){
			theme = prefs.theme;
		}
		if(theme.endsWith(DARK)){
			return prefs.getResources().getColor(R.color.bookmark_default_dim);
		}
		if(theme.endsWith(YOSPOS)){
			return prefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if(theme.endsWith(AMBERPOS)){
			return prefs.getResources().getColor(R.color.amberpos_default_post_font);
		}
		if(theme.endsWith(CLASSIC)){
			return prefs.getResources().getColor(R.color.bookmark_default_dim);
		}
		return prefs.getResources().getColor(R.color.bookmark_default_dim);
	}
	
	public static int getUnreadColorFont(String theme){
		if(theme == null){
			theme = prefs.theme;
		}
		if(theme.endsWith(DARK)){
			return prefs.getResources().getColor(R.color.unread_posts_counter);
		}
		if(theme.endsWith(YOSPOS)){
			return prefs.getResources().getColor(R.color.yospos_background);
		}
		if(theme.endsWith(AMBERPOS)){
			return prefs.getResources().getColor(R.color.amberpos_background);
		}
		if(theme.endsWith(CLASSIC)){
			return prefs.getResources().getColor(R.color.unread_posts_counter);
		}
		return prefs.getResources().getColor(R.color.unread_posts_counter);
	}
	
	public static int getUnreadColorFont(){
		return getUnreadColorFont(null);
	}
	
	public static int getActionbarColor(String theme){
		if(theme == null){
			theme = prefs.theme;
		}
		if(theme.endsWith(DARK)){
			return prefs.getResources().getColor(R.color.dark_background);
		}
		if(theme.endsWith(YOSPOS)){
			return prefs.getResources().getColor(R.color.yospos_background);
		}
		if(theme.endsWith(AMBERPOS)){
			return prefs.getResources().getColor(R.color.amberpos_background);
		}
		if(theme.endsWith(CLASSIC)){
			return prefs.getResources().getColor(R.color.actionbar_color);
		}
		return prefs.getResources().getColor(R.color.actionbar_color);
	}
	
	public static int getActionbarColor(){
		return getActionbarColor(null);
	}
	
	public static int getActionbarFontColor(String theme){
		if(theme == null){
			theme = prefs.theme;
		}
		if(theme.endsWith(DARK)){
			return prefs.getResources().getColor(R.color.dark_default_post_font);
		}
		if(theme.endsWith(YOSPOS)){
			return prefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if(theme.endsWith(AMBERPOS)){
			return prefs.getResources().getColor(R.color.amberpos_default_post_font);
		}
		if(theme.endsWith(CLASSIC)){
			return prefs.getResources().getColor(R.color.actionbar_font_color);
		}
		return prefs.getResources().getColor(R.color.actionbar_font_color);
	}
	
	public static int getActionbarFontColor(){
		return getActionbarFontColor(null);
	}
	
	public static int getProgressbarColor(String theme){
		if(theme == null){
			theme = prefs.theme;
		}
		if(theme.endsWith(DARK)){
			return prefs.getResources().getColor(R.color.holo_blue_light);
		}
		if(theme.endsWith(YOSPOS)){
			return prefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if(theme.endsWith(AMBERPOS)){
			return prefs.getResources().getColor(R.color.amberpos_default_post_font);
		}
		if(theme.endsWith(CLASSIC)){
			return prefs.getResources().getColor(R.color.holo_blue_light);
		}
		return prefs.getResources().getColor(R.color.holo_blue_light);
	}
	
	public static int getProgressbarColor(){
		return getProgressbarColor(null);
	}
	
}
