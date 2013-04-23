package com.ferg.awfulapp.provider;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.preferences.AwfulPreferences;

public class ColorProvider {

	public static int getTextColor(AwfulPreferences aPrefs){	

		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.dark_default_post_font);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.amberpos_default_post_font);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.default_post_font);
		}
		return aPrefs.getResources().getColor(R.color.default_post_font);
	}
	
	public static int getAltTextColor(AwfulPreferences aPrefs){

		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.dark_secondary_post_font);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_secondary_post_font);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.amberpos_secondary_post_font);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.secondary_post_font);
		}
		return aPrefs.getResources().getColor(R.color.secondary_post_font);
	}
	
	public static int getBackgroundColor(AwfulPreferences aPrefs){
		
		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.dark_background);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_background);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.amberpos_background);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.background);
		}
		return aPrefs.getResources().getColor(R.color.background);
	}

	public static int getUnreadColor(AwfulPreferences aPrefs){

		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.unread_posts);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.amberpos_default_post_font);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.unread_posts);
		}
		return aPrefs.getResources().getColor(R.color.unread_posts);
	}

	public static int getUnreadColorDim(AwfulPreferences aPrefs){

		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.unread_posts_dim);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.amberpos_default_post_font);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.unread_posts_dim);
		}
		return aPrefs.getResources().getColor(R.color.unread_posts_dim);
	}
	
	public static int getUnreadColorFont(AwfulPreferences aPrefs){

		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.unread_posts_counter);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_background);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.amberpos_background);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.unread_posts_counter);
		}
		return aPrefs.getResources().getColor(R.color.unread_posts_counter);
	}
	
	public static int getActionbarColor(AwfulPreferences aPrefs){

		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.dark_background);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_background);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.amberpos_background);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.actionbar_color);
		}
		return aPrefs.getResources().getColor(R.color.actionbar_color);
	}
	
	public static int getActionbarFontColor(AwfulPreferences aPrefs){

		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.dark_default_post_font);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.amberpos_default_post_font);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.actionbar_font_color);
		}
		return aPrefs.getResources().getColor(R.color.actionbar_font_color);
	}
	
	public static int getProgressbarColor(AwfulPreferences aPrefs){

		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(android.R.color.holo_blue_light);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.amberpos_default_post_font);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(android.R.color.holo_blue_light);
		}
		return aPrefs.getResources().getColor(android.R.color.holo_blue_light);
	}
	
}
