package com.ferg.awfulapp;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.StrictMode;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.ferg.awfulapp.announcements.AnnouncementsManager;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.sync.SyncManager;
import com.jakewharton.threetenabp.AndroidThreeTen;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class AwfulApplication extends Application implements AwfulPreferences.AwfulPreferenceUpdate {
    private static final String APP_STATE_PREFERENCES = "app_state_prefs";
    /**
     * Used for storing misc app data, separate from user preferences, so onPreferenceChange callbacks aren't triggered
     */
    private static SharedPreferences appStatePrefs;
    private static boolean crashlyticsEnabled = false;

    private AwfulPreferences mPref;
    private static final String FONT_PATH = "fonts";
    private final Map<String, Typeface> fonts = new HashMap<>();
    private Typeface currentFont;

    @Override
    public void onCreate() {
        super.onCreate();
        // initialise the AwfulPreferences singleton first since a lot of things rely on it for a Context
        mPref = AwfulPreferences.getInstance(this, this);
        appStatePrefs = this.getSharedPreferences(APP_STATE_PREFERENCES, MODE_PRIVATE);

        NetworkUtils.init(this);
        AndroidThreeTen.init(this);
        AnnouncementsManager.init();
        buildFontList();

        long hoursSinceInstall = getHoursSinceInstall();

        // enable Crashlytics on non-debug builds, or debug builds that have been installed for a while
        crashlyticsEnabled = !BuildConfig.DEBUG || hoursSinceInstall > 4;

        if (crashlyticsEnabled) {
            Fabric.with(this, new Crashlytics());

            if (mPref.sendUsernameInReport)
                Crashlytics.setUserName(mPref.username);
        }

        Timber.plant(crashlyticsEnabled ? new CrashlyticsReportingTree() : new Timber.DebugTree());

        Timber.i("App installed %d hours ago", hoursSinceInstall);

        if (Constants.DEBUG) {
            Timber.d("*\n*\n*Debug active\n*\n*");
			/*
			This checks destroyed cursors aren't left open, and crashes (with a log) if it finds one
			Really this is here to avoid introducing any more leaks, since there are some issues with
			too many open cursors
			*/
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }

        SyncManager.sync(this);
    }

    /**
     * @return how long it's been since the app was updated
     */
    private long getHoursSinceInstall() {
        long hoursSinceInstall = Long.MAX_VALUE;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            long millisSinceInstall = System.currentTimeMillis() - packageInfo.lastUpdateTime;
            hoursSinceInstall = TimeUnit.HOURS.convert(millisSinceInstall, TimeUnit.MILLISECONDS);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return hoursSinceInstall;
    }

    /**
     * Returns true if the Crashlytics singleton has been initialised and can be used.
     */
    public static boolean crashlyticsEnabled() {
        return crashlyticsEnabled;
    }

    /**
     * Get the SharedPreferences used for storing basic app state.
     * <p>
     * These are separate from the default shared preferences, and won't trigger onPreferenceChange callbacks.
     *
     * @see AwfulPreferences.AwfulPreferenceUpdate#onPreferenceChange(AwfulPreferences, String)
     */
    public static SharedPreferences getAppStatePrefs() {
        return appStatePrefs;
    }

    private void setTextViewTypefaceToCurrentFont(TextView textView, int textStyle) {
        if (textStyle < 0 || textStyle > 3) {
            textStyle = textView.getTypeface() == null ? Typeface.NORMAL :
                    textView.getTypeface().getStyle();
        }

        if (currentFont != null)
            textView.setTypeface(currentFont, textStyle);
    }

    public void setPreferredFont(View view, int flags) {
        if (view instanceof TextView)
            setTextViewTypefaceToCurrentFont((TextView) view, flags);
        else if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;

            for (int i = 0; i < viewGroup.getChildCount(); i++)
                setPreferredFont(viewGroup.getChildAt(i), flags);
        }
    }

    @Override
    public void onPreferenceChange(AwfulPreferences prefs, String key) {
        setCurrentFont(prefs.preferredFont);
    }

    private void setCurrentFont(String fontName) {
        currentFont = fonts.get(fontName);

        if (currentFont != null)
            Timber.i("Font Selected: %s", fontName);
        else
            Timber.e("Couldn't select font: %s", fontName);
    }

    public String[] getFontList() {
        Timber.i("Font list: %s", fonts.keySet());
        return fonts.keySet().toArray(new String[0]);
    }

    private void buildFontList() {
        fonts.clear();
        fonts.put("default", Typeface.defaultFromStyle(Typeface.NORMAL));

        String[] files = null;

        try {
            files = getAssets().list(FONT_PATH);
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }

        if (files != null) {
            for (String file : files) {
                String fileName = String.format("%s/%s", FONT_PATH, file);
                fonts.put(fileName, Typeface.createFromAsset(getAssets(), fileName));
                Timber.i("Processed Font: %s", fileName);
            }
        }

        setCurrentFont(mPref.preferredFont);
    }

    @Override
    public File getCacheDir() {
        Timber.i("getCacheDir(): %s", super.getCacheDir());
        return super.getCacheDir();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level != Application.TRIM_MEMORY_UI_HIDDEN && level != Application.TRIM_MEMORY_BACKGROUND) {
            NetworkUtils.clearImageCache();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        NetworkUtils.clearImageCache();
    }
}
