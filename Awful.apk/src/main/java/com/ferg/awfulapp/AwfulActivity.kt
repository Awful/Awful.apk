package com.ferg.awfulapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.net.http.HttpResponseCache
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.AwfulTheme
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.task.AwfulRequest
import com.ferg.awfulapp.task.FeatureRequest
import com.ferg.awfulapp.task.ProfileRequest
import timber.log.Timber
import java.io.File

/**
 * Created by baka kaba on 18/01/2018.
 *
 * Base Activity class handling theming, orientation changes and access to preferences and callbacks.
 *
 * This is an update of the original AwfulActivity.java, but reworked to simplify the logic and
 * to pull in some code from other components, so it's more focused on how the app uses activities.
 *
 * All activities should inherit from this one, so you get free theming, preference handling and
 * access to app navigation. Call [setUpActionBar] after setting up your Toolbar to apply the standard
 * app style and behaviour.
 */
abstract class AwfulActivity : AppCompatActivity(), AwfulPreferences.AwfulPreferenceUpdate {
    private var loggedIn = false
    private var customActivityTitle: TextView? = null
    // TODO: this is a var but honestly why does any activity need to replace it with their own copy? It's a singleton - if they're doing it for the callbacks, just use the register method
    var mPrefs: AwfulPreferences = AwfulPreferences.getInstance()

    val isLoggedIn: Boolean
        get() {
            if (!loggedIn) {
                loggedIn = NetworkUtils.restoreLoginCookies(application)
            }
            return loggedIn
        }

    private fun reAuthenticate() {
        NetworkUtils.clearLoginCookies(this)
        startActivityForResult(Intent(this, AwfulLoginActivity::class.java), LOGIN_ACTIVITY_REQUEST)
    }


    //
    // Lifecycle
    //


    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.i("*** onCreate")
        mPrefs.registerCallback(this)
        updateTheme()
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        Timber.i("*** onStart")
        super.onStart()
    }

    override fun onResume() {
        Timber.i("*** onResume")
        super.onResume()
        updateOrientation()
        // check login state when coming back into use, instead of in onCreate
        loggedIn = false // reset for the isLoggedIn check
        when {
            isLoggedIn -> with(mPrefs) {
                if (ignoreFormkey == null || userTitle == null) ProfileRequest(this@AwfulActivity).sendBlind()
                if (ignoreFormkey == null) FeatureRequest(this@AwfulActivity).sendBlind()
            }
            this !is AwfulLoginActivity -> reAuthenticate()
        }
    }

    private fun AwfulRequest<*>.sendBlind() = NetworkUtils.queueRequest(this.build())


    override fun onPause() {
        Timber.i("*** onPause")
        super.onPause()
    }

    @SuppressLint("NewApi")
    override fun onStop() {
        Timber.i("*** onStop")
        super.onStop()
        HttpResponseCache.getInstalled()?.flush()
    }

    override fun onDestroy() {
        Timber.i("*** onDestroy")
        super.onDestroy()
        mPrefs.unregisterCallback(this)
    }


    override fun onActivityResult(request: Int, result: Int, intent: Intent) {
        Timber.i("onActivityResult: $request result: $result")
        super.onActivityResult(request, result, intent)
        if (request == LOGIN_ACTIVITY_REQUEST && result == Activity.RESULT_CANCELED) {
            finish()
        }
    }


    //
    // Action bar
    //

    /**
     * Initialises the Action Bar with the app's custom settings, behaviour and theming.
     */
    protected fun setUpActionBar() {
        // TODO: check if this plays nicely with activities without the custom title view, and make it optional if not - having a general "theme the action bar" method would be a good idea
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setCustomView(R.layout.actionbar_title)
            customActivityTitle = customView as TextView
            customActivityTitle?.movementMethod = ScrollingMovementMethod()
            updateActionbarTheme()
            setDisplayShowCustomEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
    }


    /**
     * Set a (non-empty) title for the custom action bar.
     */
    fun setActionbarTitle(aTitle: String) {
        supportActionBar?.apply { customActivityTitle = customView as TextView }
        with(customActivityTitle) {
            if (this == null || aTitle.isEmpty()) {
                Timber.w("FAILED setActionbarTitle - $aTitle")
            } else {
                text = aTitle
                scrollTo(0, 0)
            }
        }
    }


    private fun updateActionbarTheme() {
        supportActionBar?.apply {
            customActivityTitle?.let { title ->
                title.setTextColor(ColorProvider.ACTION_BAR_TEXT.color)
                setPreferredFont(title, Typeface.NORMAL)
            }
        }
    }


    //
    // Preferences and other UI
    //

    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        Timber.i("Key changed: ${key!!}")
        updateOrientation()
        if ("theme" == key || "page_layout" == key) {
            updateTheme()
            afterThemeChange()
        }
        updateActionbarTheme()
    }


    private fun updateOrientation() {
        requestedOrientation = when (mPrefs.orientation) {
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "sensor" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    @JvmOverloads
    fun setPreferredFont(view: View?, flags: Int = -1) =
            view?.let { (application as AwfulApplication).setPreferredFont(view, flags) }

    protected fun updateTheme() = setTheme(AwfulTheme.forForum(null).themeResId)

    // TODO: see if this can be rolled into updateTheme without causing issues
    private fun afterThemeChange() = recreate()


    //
    // Misc
    //

    override fun getCacheDir(): File {
        Timber.i("getCacheDir(): ${super.getCacheDir()}")
        return super.getCacheDir()
    }


    //
    // App navigation
    //

    open fun displayForumIndex() = startActivity(NavigationIntent.ForumIndex.getIntent(applicationContext))

    open fun displayUserCP() = startActivity(NavigationIntent.Bookmarks.getIntent(applicationContext))

    open fun displayForum(id: Int, page: Int) =
            // TODO: allow null page
            startActivity(NavigationIntent.Forum(id, page).getIntent(applicationContext))

    open fun displayThread(id: Int, page: Int, forumId: Int, forumPage: Int, forceReload: Boolean) =
            //TODO: clean up unused params, allow nulls and postJump
            startActivity(NavigationIntent.Thread(id, page, null).getIntent(applicationContext))


    companion object {
        val DEBUG = Constants.DEBUG
    }
}

sealed class NavigationIntent {

    object Bookmarks : NavigationIntent()
    object ForumIndex : NavigationIntent()
    data class Thread(val id: Int, val page: Int? = null, val postJump: String? = null) : NavigationIntent()
    data class Forum(val id: Int, val page: Int? = null) : NavigationIntent()

    // TODO: use specific type constants for each intent type - so bookmarks has an extra TYPE = BOOKMARKS, not just a forum ID that matches the bookmarks ID

    fun getIntent(context: Context): Intent =
            Intent().setClass(context, ForumsIndexActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .apply {
                        when (this@NavigationIntent) {
                            is Thread -> {
                                putExtra(THREAD_ID, id)
                                page?.let { putExtra(THREAD_PAGE, page) }
                                postJump?.let { putExtra(THREAD_FRAGMENT, postJump) }
                            }
                            is Forum -> {
                                putExtra(FORUM_ID, id)
                                page?.let { putExtra(FORUM_PAGE, page) }
                            }
                            is Bookmarks -> putExtra(FORUM_ID, USERCP_ID)
                        }
                    }

    companion object {

        fun Intent.parse(): NavigationIntent {
            return when {
                hasExtra(THREAD_ID) ->
                    Thread(
                            id = getIntExtra(THREAD_ID)!!,
                            page = getIntExtra(THREAD_PAGE),
                            postJump = getStringExtra(THREAD_FRAGMENT)
                    )
                getIntExtra(FORUM_ID) == USERCP_ID ->
                    Bookmarks
                hasExtra(FORUM_ID) ->
                    Forum(
                            id = getIntExtra(FORUM_ID)!!,
                            page = getIntExtra(FORUM_PAGE)
                    )
                else -> ForumIndex
            }
        }

        private fun Intent.getIntExtra(name: String) = if (hasExtra(name)) getIntExtra(name, -12345) else null
    }
}
