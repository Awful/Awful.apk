package com.ferg.awfulapp;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.StrictMode;
import android.webkit.WebView;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.ferg.awfulapp.announcements.AnnouncementsManager;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.sync.SyncManager;
import com.jakewharton.threetenabp.AndroidThreeTen;

import java.io.File;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class AwfulApplication extends Application {
    private static final String APP_STATE_PREFERENCES = "app_state_prefs";
    /**
     * Used for storing misc app data, separate from user preferences, so onPreferenceChange callbacks aren't triggered
     */
    private static SharedPreferences appStatePrefs;
    private static boolean crashlyticsEnabled = false;

    /**
     * Stores the user agent used by web views in this application, which is required to be
     * initialised early on to correctly handle Cloudflare captcha situations.
     *
     * If the user is affected by Cloudflare captchas, the user-agents of all HTTP requests coming
     * from Awful must be synchronised. Setting a custom user-agent does not work, because
     * Cloudflare only allows mainstream browser user-agents.
     */
    private static String AWFUL_USER_AGENT = null;

    public static String getAwfulUserAgent() {
        return AWFUL_USER_AGENT;
    }

    /**
     * Instantiates an ephemeral WebView which is only used to retrieve the default user agent
     * of web views on this system.
     *
     * @return User-Agent string of WebView instances on this system.
     */
    private String getWebViewUserAgent() {
        WebView view = new WebView(getApplicationContext());
        return view.getSettings().getUserAgentString();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        AWFUL_USER_AGENT = getWebViewUserAgent();

        // initialise the AwfulPreferences singleton first since a lot of things rely on it for a Context
        AwfulPreferences mPref = AwfulPreferences.getInstance(this);

        appStatePrefs = this.getSharedPreferences(APP_STATE_PREFERENCES, MODE_PRIVATE);

        NetworkUtils.init(this);
        AndroidThreeTen.init(this);
        AnnouncementsManager.init();
        FontManager.createInstance(mPref, getAssets());

        long hoursSinceInstall = getHoursSinceInstall();

        // enable Crashlytics on non-debug builds, or debug builds that have been installed for a while
        crashlyticsEnabled = !BuildConfig.DEBUG || hoursSinceInstall > 4;

        if (crashlyticsEnabled) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();

            if (mPref.sendUsernameInReport)
                crashlytics.setUserId(mPref.username);
        } else {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);
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
