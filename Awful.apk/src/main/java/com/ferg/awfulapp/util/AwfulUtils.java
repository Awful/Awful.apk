package com.ferg.awfulapp.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;

//import com.ToxicBakery.viewpager.transforms.ABaseTransformer;
//import com.ToxicBakery.viewpager.transforms.AccordionTransformer;
//import com.ToxicBakery.viewpager.transforms.BackgroundToForegroundTransformer;
//import com.ToxicBakery.viewpager.transforms.CubeInTransformer;
//import com.ToxicBakery.viewpager.transforms.CubeOutTransformer;
//import com.ToxicBakery.viewpager.transforms.DefaultTransformer;
//import com.ToxicBakery.viewpager.transforms.DepthPageTransformer;
//import com.ToxicBakery.viewpager.transforms.FlipHorizontalTransformer;
//import com.ToxicBakery.viewpager.transforms.FlipVerticalTransformer;
//import com.ToxicBakery.viewpager.transforms.ForegroundToBackgroundTransformer;
//import com.ToxicBakery.viewpager.transforms.RotateDownTransformer;
//import com.ToxicBakery.viewpager.transforms.RotateUpTransformer;
//import com.ToxicBakery.viewpager.transforms.StackTransformer;
//import com.ToxicBakery.viewpager.transforms.TabletTransformer;
//import com.ToxicBakery.viewpager.transforms.ZoomInTransformer;
//import com.ToxicBakery.viewpager.transforms.ZoomOutSlideTransformer;
//import com.ToxicBakery.viewpager.transforms.ZoomOutTranformer;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.thread.AwfulEmote;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by matt on 9/11/13.
 */
public class AwfulUtils {
    /**
	 * I guess this should really be named "isGingerbreadOrAbove()" but I suck at function naming.
	 * @return
	 */
	public static boolean isGingerbread(){
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
	}

    public static boolean isHoneycomb(){
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

    public static boolean isICS(){
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	}

    public static boolean isJellybean(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean isKitKat(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static boolean isKitKatOnly(){
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT;
    }

    public static boolean isLollipop(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean isWidescreen(Context cont){
		if(cont != null){
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2){
				return cont.getResources().getConfiguration().screenWidthDp >= Constants.WIDESCREEN_DPI;
			}else{
				return (cont.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_XLARGE) > 0;
			}
		}else{
			return false;
		}
	}

    public static boolean isWidescreen(Configuration newConfig) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2){
			return newConfig.screenWidthDp >= Constants.WIDESCREEN_DPI;
		}else{
			return (newConfig.screenLayout & Configuration.SCREENLAYOUT_SIZE_XLARGE) > 0;
		}
	}

    /**
	 * Similar to isWidescreen(), except will check to see if this device is large enough to be considered widescreen (700dp) in either orientation, even if it isn't in the current orientation.
	 * @param cont
	 * @return True if either width or height is large enough to count as widescreen.
	 */
	public static boolean canBeWidescreen(Context cont){
		if(cont != null){
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2){
				return cont.getResources().getConfiguration().screenWidthDp >= Constants.WIDESCREEN_DPI || cont.getResources().getConfiguration().screenHeightDp >= Constants.WIDESCREEN_DPI;
			}else{
				return (cont.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_XLARGE) > 0;
			}
		}else{
			return false;
		}
	}

    /**
	 * Parse an int from the given string, falling back to the provided number in case of failure.
	 * @param number String containing the int to be parsed.
	 * @param fallback Number to return if parsing fails.
	 * @return Either the parsed number or the fallback.
	 */
	public static int safeParseInt(String number, int fallback){
		try{
			return Integer.parseInt(number);
		}catch(NumberFormatException nfe){
			return fallback;
		}
	}

    /**
	 * Parse an long from the given string, falling back to the provided number in case of failure.
	 * @param number String containing the long to be parsed.
	 * @param fallback Number to return if parsing fails.
	 * @return Either the parsed number or the fallback.
	 */
	public static long safeParseLong(String number, long fallback){
		try{
			return Long.parseLong(number);
		}catch(NumberFormatException nfe){
			return fallback;
		}
	}

    public static String LogE(String tag, String message) {
		Log.e(tag, message);
		return message;
	}

    public static float getPixelsFromDIP(Context context, int dipValue) {
		if(context != null){
			return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, context.getResources().getDisplayMetrics());
		}
		return dipValue;
	}

    public static void trimDbEntries(ContentResolver cr){
        int rowCount = 0;
        rowCount += cr.delete(AwfulThread.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-7 days')", null);
        rowCount += cr.delete(AwfulPost.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-7 days')", null);
        rowCount += cr.delete(AwfulThread.CONTENT_URI_UCP, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-7 days')", null);
        rowCount += cr.delete(AwfulEmote.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-7 days')", null);
        Log.i("AwfulTrimDB","Trimming DB older than 7 days, culled: "+rowCount);
    }

//    public static ABaseTransformer getViewPagerTransformer(){
//        HashMap<String,ABaseTransformer> transformerMap = new HashMap<String,ABaseTransformer>();
//        transformerMap.put("Default", new DefaultTransformer());
//        transformerMap.put("Accordion", new AccordionTransformer());
//        transformerMap.put("BackgroundToForeground", new BackgroundToForegroundTransformer());
//        transformerMap.put("CubeIn", new CubeInTransformer());
//        transformerMap.put("CubeOut", new CubeOutTransformer());
//        transformerMap.put("DepthPage", new DepthPageTransformer());
//        transformerMap.put("FlipHorizontal", new FlipHorizontalTransformer());
//        transformerMap.put("FlipVertical", new FlipVerticalTransformer());
//        transformerMap.put("ForegroundToBackground", new ForegroundToBackgroundTransformer());
//        transformerMap.put("RotateDown", new RotateDownTransformer());
//        transformerMap.put("RotateUp", new RotateUpTransformer());
//        transformerMap.put("Stack", new StackTransformer());
//        transformerMap.put("Tablet", new TabletTransformer());
//        transformerMap.put("ZoomIn", new ZoomInTransformer());
//        transformerMap.put("ZoomOutSlide", new ZoomOutSlideTransformer());
//        transformerMap.put("ZoomOut", new ZoomOutTranformer());
//
//
//        return transformerMap.get(AwfulPreferences.getInstance().transformer);
//    }
}
