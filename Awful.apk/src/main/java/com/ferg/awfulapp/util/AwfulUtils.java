package com.ferg.awfulapp.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.ToxicBakery.viewpager.transforms.ABaseTransformer;
import com.ToxicBakery.viewpager.transforms.AccordionTransformer;
import com.ToxicBakery.viewpager.transforms.BackgroundToForegroundTransformer;
import com.ToxicBakery.viewpager.transforms.CubeInTransformer;
import com.ToxicBakery.viewpager.transforms.CubeOutTransformer;
import com.ToxicBakery.viewpager.transforms.DepthPageTransformer;
import com.ToxicBakery.viewpager.transforms.ForegroundToBackgroundTransformer;
import com.ToxicBakery.viewpager.transforms.RotateDownTransformer;
import com.ToxicBakery.viewpager.transforms.RotateUpTransformer;
import com.ToxicBakery.viewpager.transforms.StackTransformer;
import com.ToxicBakery.viewpager.transforms.TabletTransformer;
import com.ToxicBakery.viewpager.transforms.ZoomInTransformer;
import com.ToxicBakery.viewpager.transforms.ZoomOutSlideTransformer;
import com.ToxicBakery.viewpager.transforms.ZoomOutTranformer;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.DatabaseHelper;
import com.ferg.awfulapp.thread.AwfulEmote;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;

import java.util.HashMap;

/**
 * Created by matt on 9/11/13.
 * <p>
 * General utility functions and access to app resources.
 */
public class AwfulUtils {

    public static boolean isAtLeast(int code) {
        return Build.VERSION.SDK_INT >= code;
    }

    public static boolean isJellybean() {
        return isAtLeast(Build.VERSION_CODES.JELLY_BEAN);
    }

    public static boolean isKitKat() {
        return isAtLeast(Build.VERSION_CODES.KITKAT);
    }

    public static boolean isKitKatOnly() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT;
    }

    public static boolean isLollipop() {
        return isAtLeast(Build.VERSION_CODES.LOLLIPOP);
    }

    public static boolean isMarshmallow() {
        return isAtLeast(Build.VERSION_CODES.M);
    }

    public static boolean isTablet(Context cont, boolean forceCheck) {
        if (!forceCheck) {
            if (AwfulPreferences.getInstance().pageLayout.equals("phone")) {
                return false;
            }
            if (AwfulPreferences.getInstance().pageLayout.equals("tablet")) {
                return true;
            }
        }
        return AwfulUtils.getScreenSizeInInch(cont) >= Constants.TABLET_MIN_SIZE;
    }

    public static boolean isTablet(Context cont) {
        return isTablet(cont, false);
    }

    private static double getScreenSizeInInch(Context cont) {
        Display display = ((WindowManager) cont.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        double x = Math.pow(size.x / dm.xdpi, 2);
        double y = Math.pow(size.y / dm.ydpi, 2);
        return Math.sqrt(x + y);
    }

    /**
     * Parse an int from the given string, falling back to the provided number in case of failure.
     *
     * @param number   String containing the int to be parsed.
     * @param fallback Number to return if parsing fails.
     * @return Either the parsed number or the fallback.
     */
    public static int safeParseInt(String number, int fallback) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }

    /**
     * Parse an long from the given string, falling back to the provided number in case of failure.
     *
     * @param number   String containing the long to be parsed.
     * @param fallback Number to return if parsing fails.
     * @return Either the parsed number or the fallback.
     */
    public static long safeParseLong(String number, long fallback) {
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }

    public static void trimDbEntries(ContentResolver cr) {
        int rowCount = 0;
        for (Uri uri : new Uri[]{AwfulThread.CONTENT_URI, AwfulThread.CONTENT_URI_UCP, AwfulPost.CONTENT_URI, AwfulEmote.CONTENT_URI}) {
            rowCount += cr.delete(uri, DatabaseHelper.UPDATED_TIMESTAMP + " < datetime('now','-7 days')", null);
        }
        Log.i("AwfulTrimDB", "Trimming DB older than 7 days, culled: " + rowCount);
    }

    public static ABaseTransformer getViewPagerTransformer() {
        HashMap<String, ABaseTransformer> transformerMap = new HashMap<>();
        transformerMap.put("Disabled", null);
        transformerMap.put("Accordion", new AccordionTransformer());
        transformerMap.put("BackgroundToForeground", new BackgroundToForegroundTransformer());
        transformerMap.put("CubeIn", new CubeInTransformer());
        transformerMap.put("CubeOut", new CubeOutTransformer());
        transformerMap.put("DepthPage", new DepthPageTransformer());
        transformerMap.put("ForegroundToBackground", new ForegroundToBackgroundTransformer());
        transformerMap.put("RotateDown", new RotateDownTransformer());
        transformerMap.put("RotateUp", new RotateUpTransformer());
        transformerMap.put("Stack", new StackTransformer());
        transformerMap.put("Tablet", new TabletTransformer());
        transformerMap.put("ZoomIn", new ZoomInTransformer());
        transformerMap.put("ZoomOutSlide", new ZoomOutSlideTransformer());
        transformerMap.put("ZoomOut", new ZoomOutTranformer());


        return transformerMap.get(AwfulPreferences.getInstance().transformer);
    }


    public static boolean contains(int[] intArray, int value) {
        for (int cur : intArray) {
            if (cur == value) {
                return true;
            }
        }
        return false;
    }
}
