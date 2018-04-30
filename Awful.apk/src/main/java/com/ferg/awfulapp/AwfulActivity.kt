package com.ferg.awfulapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo.*
import android.graphics.Typeface
import android.net.Uri
import android.net.http.HttpResponseCache
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import com.ferg.awfulapp.announcements.AnnouncementsManager
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.constants.Constants.LOGIN_ACTIVITY_REQUEST
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
    private var customActivityTitle: TextView? = null
    // TODO: this is a var but honestly why does any activity need to replace it with their own copy? It's a singleton - if they're doing it for the callbacks, just use the register method
    var mPrefs: AwfulPreferences = AwfulPreferences.getInstance()


    //
    // Lifecycle
    //


    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.i("*** onCreate")
        mPrefs.registerCallback(this)
        updateTheme()
        super.onCreate(savedInstanceState)
    }

    @CallSuper
    override fun onStart() {
        Timber.i("*** onStart")
        super.onStart()
    }

    @CallSuper
    override fun onResume() {
        Timber.i("*** onResume")
        super.onResume()
        updateOrientation()
        when {
            Authentication.isUserLoggedIn() -> with(mPrefs) {
                if (ignoreFormkey == null || userTitle == null) ProfileRequest(this@AwfulActivity).sendBlind()
                if (ignoreFormkey == null) FeatureRequest(this@AwfulActivity).sendBlind()
            }
        // TODO: this interferes with the code in AwfulFragment#reAuthenticate - i.e. it forces "return to this activity" behaviour, but fragments may want to return to the main activity.
        // And the activity isn't always the authority, e.g. using BasicActivity which is a plain container where all the specific behaviour is handled by its fragment. It's awkward
            this !is AwfulLoginActivity -> showLogIn(returnToMainActivity = false)
        }
    }

    private fun AwfulRequest<*>.sendBlind() = NetworkUtils.queueRequest(this.build())


    @CallSuper
    override fun onPause() {
        Timber.i("*** onPause")
        super.onPause()
    }

    @CallSuper
    @SuppressLint("NewApi")
    override fun onStop() {
        Timber.i("*** onStop")
        super.onStop()
        HttpResponseCache.getInstalled()?.flush()
    }


    @CallSuper
    override fun onDestroy() {
        Timber.i("*** onDestroy")
        super.onDestroy()
        mPrefs.unregisterCallback(this)
    }


    @CallSuper
    override fun onActivityResult(request: Int, result: Int, intent: Intent?) {
        Timber.i("onActivityResult: $request result: $result")
        super.onActivityResult(request, result, intent)
        supportFragmentManager.fragments.forEach { it.onActivityResult(request, result, intent) }
        if (request == LOGIN_ACTIVITY_REQUEST && result == Activity.RESULT_CANCELED) {
            Timber.w("Result from login activity - cancelled, closing app")
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
    open fun setActionbarTitle(title: String) {
        Timber.d("Setting action bar title: %s", title)
        supportActionBar?.apply { customActivityTitle = customView as TextView }
        with(customActivityTitle) {
            if (this == null || title.isEmpty()) {
                Timber.w("FAILED setActionbarTitle - $title")
            } else {
                text = title
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
            "portrait" -> SCREEN_ORIENTATION_PORTRAIT
            "landscape" -> SCREEN_ORIENTATION_LANDSCAPE
            "sensor" -> SCREEN_ORIENTATION_SENSOR
            else -> SCREEN_ORIENTATION_UNSPECIFIED
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

    open fun navigate(event: NavigationEvent) = startActivity(event.getIntent(applicationContext))

    /**
     * Log out, and show the login activity.
     *
     * If [returnToMainActivity] is true, after logging in the user will be directed to the main
     * activity, which receives the login activity result, otherwise the user returns to the current
     * activity. This is useful if e.g. the user jumps straight into their private messages, receives
     * a logged out error, and logs in - you can either update the private messages view, or kick them
     * to the main activity.
     */
    fun showLogIn(returnToMainActivity: Boolean = false) {
        if (returnToMainActivity) {
            startActivity(NavigationEvent.ReAuthenticate.getIntent(applicationContext))
        } else {
            Authentication.reAuthenticate(this)
        }
    }

    fun showLogoutDialog() = LogOutDialog().show(supportFragmentManager, "logout dialog")

    /** Display the announcements  */
    fun showAnnouncements() = AnnouncementsManager.getInstance().showAnnouncements(this)

    /**
     * Display the user's PMs
     *
     * @param openMessageUri a Uri to a private message you want to open, parsed from a new PM link on the site
     */
    fun showPrivateMessages(openMessageUri: Uri? = null) =
    // TODO: rework this so the message ID is parsed from the link, and we just pass that around internally
            Intent().apply {
                setClass(this@AwfulActivity, PrivateMessageActivity::class.java)
                openMessageUri?.let(::setData)
            }.let(::startActivity)

    /**
     * Display the post/reply/edit composer.
     *
     * @param threadId the ID of the thread the post is in
     * @param postType a post/reply/edit constant, see types in AwfulMessage
     * @param sourcePostId if we're editing or quoting a post, this is its ID
     */
    fun showPostComposer(threadId: Int, postType: Int, sourcePostId: Int) {
        // TODO: this should probably all be refactored into types like the NavigationEvents (maybe even rolled in with them) - discrete Posting events with the specific associated data for each
        startActivityForResult(
                Intent(this, PostReplyActivity::class.java)
                        .putExtra(Constants.REPLY_THREAD_ID, threadId)
                        .putExtra(Constants.EDITING, postType)
                        .putExtra(Constants.REPLY_POST_ID, sourcePostId),
                PostReplyFragment.REQUEST_POST
        )
    }


    companion object {
        val DEBUG = Constants.DEBUG
    }
}

