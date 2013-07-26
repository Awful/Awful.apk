package com.ferg.awfulapp.provider;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.preferences.AwfulPreferences;

public class ColorProvider {

	public static int getTextColor(AwfulPreferences aPrefs){	

		if(aPrefs.theme.endsWith("dark.css")){
			return aPrefs.getResources().getColor(R.color.dark_default_post_font);
		}
		if(aPrefs.theme.endsWith("yospos.css")){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if(aPrefs.theme.endsWith("amberpos.css")){
			return aPrefs.getResources().getColor(R.color.amberpos_default_post_font);
		}
		if(aPrefs.theme.endsWith("classic.css")){
			return aPrefs.getResources().getColor(R.color.default_post_font);
		}
		return aPrefs.getResources().getColor(R.color.default_post_font);
	}
	
	public static int getAltTextColor(AwfulPreferences aPrefs){

		if(aPrefs.theme.endsWith("dark.css")){
			return aPrefs.getResources().getColor(R.color.dark_secondary_post_font);
		}
		if(aPrefs.theme.endsWith("yospos.css")){
			return aPrefs.getResources().getColor(R.color.yospos_secondary_post_font);
		}
		if(aPrefs.theme.endsWith("amberpos.css")){
			return aPrefs.getResources().getColor(R.color.amberpos_secondary_post_font);
		}
		if(aPrefs.theme.endsWith("classic.css")){
			return aPrefs.getResources().getColor(R.color.secondary_post_font);
		}
		return aPrefs.getResources().getColor(R.color.secondary_post_font);
	}
	
	public static int getBackgroundColor(AwfulPreferences aPrefs){
		
		if(aPrefs.theme.endsWith("dark.css")){
			return aPrefs.getResources().getColor(R.color.dark_background);
		}
		if(aPrefs.theme.endsWith("yospos.css")){
			return aPrefs.getResources().getColor(R.color.yospos_background);
		}
		if(aPrefs.theme.endsWith("amberpos.css")){
			return aPrefs.getResources().getColor(R.color.amberpos_background);
		}
		if(aPrefs.theme.endsWith("classic.css")){
			return aPrefs.getResources().getColor(R.color.background);
		}
		return aPrefs.getResources().getColor(R.color.background);
	}

	public static int getUnreadColor(AwfulPreferences aPrefs){

		if(aPrefs.theme.endsWith("dark.css")){
			return aPrefs.getResources().getColor(R.color.unread_posts);
		}
		if(aPrefs.theme.endsWith("yospos.css")){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if(aPrefs.theme.endsWith("amberpos.css")){
			return aPrefs.getResources().getColor(R.color.amberpos_default_post_font);
		}
		if(aPrefs.theme.endsWith("classic.css")){
			return aPrefs.getResources().getColor(R.color.unread_posts);
		}
		return aPrefs.getResources().getColor(R.color.unread_posts);
	}

	public static int getUnreadColorDim(AwfulPreferences aPrefs){

		if(aPrefs.theme.endsWith("dark.css")){
			return aPrefs.getResources().getColor(R.color.unread_posts_dim);
		}
		if(aPrefs.theme.endsWith("yospos.css")){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if(aPrefs.theme.endsWith("amberpos.css")){
			return aPrefs.getResources().getColor(R.color.amberpos_default_post_font);
		}
		if(aPrefs.theme.endsWith("classic.css")){
			return aPrefs.getResources().getColor(R.color.unread_posts_dim);
		}
		return aPrefs.getResources().getColor(R.color.unread_posts_dim);
	}
	
	public static int getUnreadColorFont(AwfulPreferences aPrefs){

		if(aPrefs.theme.endsWith("dark.css")){
			return aPrefs.getResources().getColor(R.color.unread_posts_counter);
		}
		if(aPrefs.theme.endsWith("yospos.css")){
			return aPrefs.getResources().getColor(R.color.yospos_background);
		}
		if(aPrefs.theme.endsWith("amberpos.css")){
			return aPrefs.getResources().getColor(R.color.amberpos_background);
		}
		if(aPrefs.theme.endsWith("classic.css")){
			return aPrefs.getResources().getColor(R.color.unread_posts_counter);
		}
		return aPrefs.getResources().getColor(R.color.unread_posts_counter);
	}
	
	public static int getActionbarColor(AwfulPreferences aPrefs){

		if(aPrefs.theme.endsWith("dark.css")){
			return aPrefs.getResources().getColor(R.color.dark_background);
		}
		if(aPrefs.theme.endsWith("yospos.css")){
			return aPrefs.getResources().getColor(R.color.yospos_background);
		}
		if(aPrefs.theme.endsWith("amberpos.css")){
			return aPrefs.getResources().getColor(R.color.amberpos_background);
		}
		if(aPrefs.theme.endsWith("classic.css")){
			return aPrefs.getResources().getColor(R.color.actionbar_color);
		}
		return aPrefs.getResources().getColor(R.color.actionbar_color);
	}
	
	public static int getActionbarFontColor(AwfulPreferences aPrefs){

		if(aPrefs.theme.endsWith("dark.css")){
			return aPrefs.getResources().getColor(R.color.dark_default_post_font);
		}
		if(aPrefs.theme.endsWith("yospos.css")){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if(aPrefs.theme.endsWith("amberpos.css")){
			return aPrefs.getResources().getColor(R.color.amberpos_default_post_font);
		}
		if(aPrefs.theme.endsWith("classic.css")){
			return aPrefs.getResources().getColor(R.color.actionbar_font_color);
		}
		return aPrefs.getResources().getColor(R.color.actionbar_font_color);
	}
	
	public static int getProgressbarColor(AwfulPreferences aPrefs){

		if(aPrefs.theme.endsWith("dark.css")){
			return aPrefs.getResources().getColor(android.R.color.holo_blue_light);
		}
		if(aPrefs.theme.endsWith("yospos.css")){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if(aPrefs.theme.endsWith("amberpos.css")){
			return aPrefs.getResources().getColor(R.color.amberpos_default_post_font);
		}
		if(aPrefs.theme.endsWith("classic.css")){
			return aPrefs.getResources().getColor(android.R.color.holo_blue_light);
		}
		return aPrefs.getResources().getColor(android.R.color.holo_blue_light);
	}
	
}
