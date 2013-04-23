package com.ferg.awfulapp.preferences;

import com.ferg.awfulapp.R;

public class ColorProvider {

	public static int getTextColor(AwfulPreferences aPrefs){	
		if("default.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.default_post_font);
		}
		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.dark_default_post_font);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.default_post_font);
		}
		return 0;
	}
	
	public static int getAltTextColor(AwfulPreferences aPrefs){
		if("default.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.secondary_post_font);
		}
		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.dark_secondary_post_font);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_secondary_post_font);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_secondary_post_font);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.secondary_post_font);
		}
		return 0;
	}
	
	public static int getBackgroundColor(AwfulPreferences aPrefs){
		if("default.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.background);
		}
		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.dark_background);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_background);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_background);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.background);
		}
		return 0;
	}

	public static int getUnreadColor(AwfulPreferences aPrefs){
		if("default.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.unread_posts);
		}
		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.unread_posts);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.unread_posts);
		}
		return 0;
	}

	public static int getUnreadColorDim(AwfulPreferences aPrefs){
		if("default.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.unread_posts_dim);
		}
		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.unread_posts_dim);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_op_post);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_op_post);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.unread_posts_dim);
		}
		return 0;
	}
	
	public static int getUnreadColorFont(AwfulPreferences aPrefs){
		if("default.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.unread_posts_counter);
		}
		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.unread_posts_counter);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_background);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_background);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.unread_posts_counter);
		}
		return 0;
	}
	
	public static int getActionbarColor(AwfulPreferences aPrefs){
		if("default.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.actionbar_color);
		}
		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.dark_background);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_background);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_background);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.actionbar_color);
		}
		return 0;
	}
	
	public static int getActionbarFontColor(AwfulPreferences aPrefs){
		if("default.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.actionbar_font_color);
		}
		if("dark.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.dark_default_post_font);
		}
		if("yospos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if("amberpos.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.yospos_default_post_font);
		}
		if("classic.css".equals(aPrefs.theme)){
			return aPrefs.getResources().getColor(R.color.actionbar_font_color);
		}
		return 0;
	}
	
}
