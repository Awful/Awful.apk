package com.ferg.awfulapp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.StrictMode
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.crashlytics.android.Crashlytics
import com.ferg.awfulapp.announcements.AnnouncementsManager
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.sync.SyncManager
import com.jakewharton.threetenabp.AndroidThreeTen
import io.fabric.sdk.android.Fabric
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class AwfulApplication : Application(), AwfulPreferences.AwfulPreferenceUpdate {

    lateinit private var mPref: AwfulPreferences
    private val fonts = HashMap<String, Typeface>()
    private var currentFont: Typeface? = null

    val fontList: Array<String>
        get() {
            if (fonts.size == 0) {
                processFonts()
            }
            val keys = fonts.keys
            return keys.toTypedArray()
        }

    override fun onCreate() {
        super.onCreate()
        // initialise the AwfulPreferences singleton first since a lot of things rely on it for a Context
        mPref = AwfulPreferences.getInstance(this, this)
        appStatePrefs = this.getSharedPreferences(APP_STATE_PREFERENCES, Context.MODE_PRIVATE)


        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())

            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build())
        } else {
            Fabric.with(this, Crashlytics())
            Timber.plant(CrashlyticsReportingTree())
            if (mPref.sendUsernameInReport) {
                Crashlytics.setUserName(mPref.username)
            }
        }

        NetworkUtils.init(this)
        AndroidThreeTen.init(this)
        AnnouncementsManager.init()
        onPreferenceChange(mPref, null)

        // work out how long it's been since the app was updated
        var hoursSinceInstall = java.lang.Long.MAX_VALUE
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val millisSinceInstall = System.currentTimeMillis() - packageInfo.lastUpdateTime
            hoursSinceInstall = TimeUnit.HOURS.convert(millisSinceInstall, TimeUnit.MILLISECONDS)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        SyncManager.sync(this)
    }


    private fun setFontFromPreference(textView: TextView, baseFlags: Int) {
        var flags = baseFlags
        flags = if (flags < 0 && textView.typeface != null) {
            textView.typeface.style
        } else {
            Typeface.NORMAL
        }
        if (fonts.size == 0) {
            processFonts()
        }
        if (currentFont != null) {
            if (mPref.preferredFont.contains("mono")) {
                when (flags) {
                    Typeface.BOLD -> textView.setTypeface(currentFont, Typeface.BOLD)
                    Typeface.ITALIC -> textView.setTypeface(currentFont, Typeface.ITALIC)
                    Typeface.BOLD_ITALIC -> textView.setTypeface(currentFont, Typeface.BOLD_ITALIC)
                    Typeface.NORMAL -> textView.setTypeface(currentFont, Typeface.NORMAL)
                    else -> textView.setTypeface(currentFont, Typeface.NORMAL)
                }
            } else {
                textView.setTypeface(currentFont, flags)
            }
        }
    }

    private fun setFontFromPreferenceRecurse(viewGroup: ViewGroup, flags: Int) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is TextView) {
                setFontFromPreference(child, flags)
            } else if (child is ViewGroup) {
                setFontFromPreferenceRecurse(child, flags)
            }
        }
    }

    fun setPreferredFont(view: View, flags: Int) {
        if (view is TextView) {
            setFontFromPreference(view, flags)
        } else if (view is ViewGroup) {
            setFontFromPreferenceRecurse(view, flags)
        }
    }

    override fun onPreferenceChange(prefs: AwfulPreferences?, key: String?) {
        currentFont = fonts[mPref.preferredFont]
        Timber.i("FONT SELECTED: %i", mPref.preferredFont)
    }

    private fun processFonts() {
        fonts.clear()
        fonts.put("default", Typeface.defaultFromStyle(Typeface.NORMAL))
        try {
            val files = assets.list("fonts")
            for (file in files) {
                val fileName = "fonts/" + file
                fonts.put(fileName, Typeface.createFromAsset(assets, fileName))
                Timber.v("Processed Font: $fileName")
            }
        } catch (e: IOException) {
            Timber.e(e)
        } catch (e: RuntimeException) {
            Timber.e(e)
        }

        onPreferenceChange(mPref, null)
    }

    override fun getCacheDir(): File {
        Timber.v("getCacheDir(): %s", super.getCacheDir())
        return super.getCacheDir()
    }


    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level != Application.TRIM_MEMORY_UI_HIDDEN && level != Application.TRIM_MEMORY_BACKGROUND) {
            NetworkUtils.clearImageCache()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        NetworkUtils.clearImageCache()
    }


    companion object {
        /**
         * Get the SharedPreferences used for storing basic app state.
         * These are separate from the default shared preferences, and won't trigger onPreferenceChange callbacks.
         * @see AwfulPreferences.AwfulPreferenceUpdate.onPreferenceChange
         */
        lateinit var appStatePrefs: SharedPreferences
        private val APP_STATE_PREFERENCES = "app_state_prefs"
    }
}
