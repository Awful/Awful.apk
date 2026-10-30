package com.ferg.awfulapp

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.webkit.WebView
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.ferg.awfulapp.BasicActivity.Companion.intentFor
import com.ferg.awfulapp.announcements.AnnouncementsManager
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.BooleanPreference
import com.ferg.awfulapp.search.SearchFragment
import com.ferg.awfulapp.sync.SyncManager
import com.ferg.awfulapp.util.AwfulUtils
import com.jakewharton.threetenabp.AndroidThreeTen
import timber.log.Timber.Forest.d
import timber.log.Timber.Forest.i
import java.io.File
import java.util.concurrent.TimeUnit

class AwfulApplication : Application() {
    companion object {
        private const val APP_STATE_PREFERENCES = "app_state_prefs"
        /**
         * Get the SharedPreferences used for storing basic app state.
         * These are separate from the default shared preferences, and won't trigger onPreferenceChange callbacks.
         * Used for storing misc app data, separate from user preferences, so onPreferenceChange callbacks aren't triggered
         *
         * @see AwfulPreferences.AwfulPreferenceUpdate.onPreferenceChange
         */
        @JvmStatic
        var appStatePrefs: SharedPreferences? = null
            private set

        /**
         * Stores the user agent used by web views in this application, which is required to be
         * initialized early on to correctly handle Cloudflare captcha situations.
         *
         * If the user is affected by Cloudflare captchas, the user-agents of all HTTP requests coming
         * from Awful must be synchronized. Setting a custom user-agent does not work, because
         * Cloudflare only allows mainstream browser user-agents.
         */
        lateinit var AWFUL_USER_AGENT: String
        @JvmStatic
        fun getAwfulUserAgent(): String {
            return AWFUL_USER_AGENT
        }
    }

    /**
     * Instantiates an ephemeral WebView which is only used to retrieve the default user agent
     * of web views on this system.
     *
     * @return User-Agent string of WebView instances on this system.
     */
    val webViewUserAgent: String by lazy {
        when {
            Constants.DEBUG -> "Microsoft Outlook 15.0.4833"
            else -> WebView(applicationContext).settings.userAgentString
        }
    }

    override fun onCreate() {
        super.onCreate()

        AWFUL_USER_AGENT = this.webViewUserAgent

        // initialize the AwfulPreferences singleton first since a lot of things rely on it for a Context
        val mPref = AwfulPreferences.getInstance(this)

        appStatePrefs = this.getSharedPreferences(APP_STATE_PREFERENCES, MODE_PRIVATE)
        mPref.setPreference(BooleanPreference.PROBATION_IGNORE, false)

        NetworkUtils.init(this)
        AndroidThreeTen.init(this)
        AnnouncementsManager.init()
        FontManager.createInstance(mPref, assets)
        updatePlatinumShortcuts(this)

        val hoursSinceInstall = this.hoursSinceInstall

        i("App installed %d hours ago", hoursSinceInstall)

        if (Constants.DEBUG) {
            d("*\n*\n*Debug active\n*\n*")
            /*
			This checks destroyed cursors aren't left open, and crashes (with a log) if it finds one
			Really this is here to avoid introducing any more leaks, since there are some issues with
			too many open cursors
			*/
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )
        }

        SyncManager.sync(this)
    }

    private fun updatePlatinumShortcuts(context: Context) {
        if (!AwfulUtils.isAtLeast(Build.VERSION_CODES.N_MR1)) {
            return
        }

        val shortcuts = ArrayList<ShortcutInfoCompat?>()
        if (!AwfulPreferences.getInstance().hasPlatinum) {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
            return
        }
        val pms = ShortcutInfoCompat.Builder(context, "pms").apply {
            setShortLabel(context.getString(R.string.private_message))
            setLongLabel(context.getString(R.string.private_message))
            setIcon(IconCompat.createWithResource(context, R.drawable.ic_inbox_black))
            setIntent(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.EMPTY,
                    context,
                    PrivateMessageActivity::class.java
                ).putExtra("navigation event", "nav_show_private_messages")
            )
        }.build()
        shortcuts.add(pms)

        val search = ShortcutInfoCompat.Builder(context, "search").apply {
            setShortLabel(context.resources.getString(R.string.search))
            setLongLabel(context.resources.getString(R.string.search))
            setIcon(IconCompat.createWithResource(context, R.drawable.ic_search_black))
            setIntent(
                intentFor(
                    SearchFragment::class.java,
                    context,
                    context.getString(R.string.search_forums_activity_title)
                ).setAction(Intent.ACTION_VIEW)
            )
        }.build()
        shortcuts.add(search)
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    private val hoursSinceInstall: Long
        /**
         * @return how long it's been since the app was updated
         */
        get() {
            var hoursSinceInstall = Long.MAX_VALUE
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val millisSinceInstall = System.currentTimeMillis() - packageInfo.lastUpdateTime
                hoursSinceInstall = TimeUnit.HOURS.convert(millisSinceInstall, TimeUnit.MILLISECONDS)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            return hoursSinceInstall
        }

    override fun getCacheDir(): File? {
        i("getCacheDir(): %s", super.getCacheDir())
        return super.getCacheDir()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level != TRIM_MEMORY_UI_HIDDEN && level != TRIM_MEMORY_BACKGROUND) {
            NetworkUtils.clearImageCache()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        NetworkUtils.clearImageCache()
    }
}
