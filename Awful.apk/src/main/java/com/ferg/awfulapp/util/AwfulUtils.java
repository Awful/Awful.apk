package com.ferg.awfulapp.util;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

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
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.thread.AwfulEmote;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by matt on 9/11/13.
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

    public static double getScreenSizeInInch(Context cont) {
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
        rowCount += cr.delete(AwfulThread.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP + " < datetime('now','-7 days')", null);
        rowCount += cr.delete(AwfulPost.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP + " < datetime('now','-7 days')", null);
        rowCount += cr.delete(AwfulThread.CONTENT_URI_UCP, AwfulProvider.UPDATED_TIMESTAMP + " < datetime('now','-7 days')", null);
        rowCount += cr.delete(AwfulEmote.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP + " < datetime('now','-7 days')", null);
        Log.i("AwfulTrimDB", "Trimming DB older than 7 days, culled: " + rowCount);
    }

    public static ABaseTransformer getViewPagerTransformer() {
        HashMap<String, ABaseTransformer> transformerMap = new HashMap<String, ABaseTransformer>();
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

    public static String determineCSS(int forumId) {
        AwfulPreferences prefs = AwfulPreferences.getInstance();
        String userTheme = prefs.theme;

        String[] baseThemes = prefs.getContext().getResources().getStringArray(R.array.schemas_values);
        int[] specialForums = new int[]{Constants.FORUM_ID_YOSPOS, Constants.FORUM_ID_FYAD, Constants.FORUM_ID_FYAD_SUB, Constants.FORUM_ID_BYOB, Constants.FORUM_ID_COOL_CREW};

        if (!(prefs.forceForumThemes && contains(specialForums, forumId)) && !Arrays.asList(baseThemes).contains(userTheme)) {
            if (AwfulUtils.isMarshmallow()) {
                File css = new File(Environment.getExternalStorageDirectory() + "/awful/" + userTheme);
                int permissionCheck = ContextCompat.checkSelfPermission(prefs.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
                if (permissionCheck != PackageManager.PERMISSION_GRANTED || !(css.exists() && css.isFile() && css.canRead())) {
                    //We don't have permission to get the custom css, but also no time to wait for the user to press a button, so you're shit outta luck
                    if (userTheme.contains(".dark")) {
                        userTheme = "dark.css";
                    } else {
                        userTheme = "default.css";
                    }
                    Toast.makeText(prefs.getContext(), R.string.no_file_permission_theme, Toast.LENGTH_LONG).show();
                    return "file:///android_asset/css/" + userTheme;
                }
            }
            return "file:///" + Environment.getExternalStorageDirectory() + "/awful/" + userTheme;
        } else if (prefs.forceForumThemes) {
            switch (forumId) {
                case (Constants.FORUM_ID_FYAD):
                case (Constants.FORUM_ID_FYAD_SUB):
                    return "file:///android_asset/css/fyad.css";
                case (Constants.FORUM_ID_BYOB):
                case (Constants.FORUM_ID_COOL_CREW):
                    return "file:///android_asset/css/byob.css";
                case (Constants.FORUM_ID_YOSPOS):
                    if (prefs.amberDefaultPos) {
                        return "file:///android_asset/css/amberpos.css";
                    } else {
                        return "file:///android_asset/css/yospos.css";
                    }
                default:
                    return "file:///android_asset/css/" + userTheme;
            }
        } else {
            return "file:///android_asset/css/" + userTheme;
        }
    }

    public static boolean contains(int[] intArray, int value){
        for(int cur:intArray){
            if(cur == value){
                return true;
            }
        }
        return false;
    }
}
