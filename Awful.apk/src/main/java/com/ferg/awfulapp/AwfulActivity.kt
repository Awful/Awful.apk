package com.ferg.awfulapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
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
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.SettingsActivity
import com.ferg.awfulapp.provider.AwfulTheme
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.task.AwfulRequest
import com.ferg.awfulapp.task.FeatureRequest
import com.ferg.awfulapp.task.ProfileRequest
import com.ferg.awfulapp.thread.AwfulURL
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

    open fun showForumIndex() = startActivity(NavigationEvent.ForumIndex.getIntent(applicationContext))

    open fun showBookmarks() = startActivity(NavigationEvent.Bookmarks.getIntent(applicationContext))

    open fun showForum(id: Int, page: Int? = null) =
            startActivity(NavigationEvent.Forum(id, page).getIntent(applicationContext))

    open fun showThread(id: Int, page: Int? = null, postJump: String? = null, forceReload: Boolean) =
            startActivity(NavigationEvent.Thread(id, page, postJump).getIntent(applicationContext))

    fun showLogoutDialog() = LogOutDialog(this).show()

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

    /** Display the forum search  */
    fun showSearch() =
            startActivity(BasicActivity.intentFor(SearchFragment::class.java, this, getString(R.string.search_forums_activity_title)))

    /** Display the app settings  */
    fun showSettings() =
            startActivity(Intent().setClass(this, SettingsActivity::class.java))

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
                PostReplyFragment.REQUEST_POST)
    }


    companion object {
        val DEBUG = Constants.DEBUG
    }
}

/**
 * Represents the navigation events we handle within the app, and any associated data for each.
 */
sealed class NavigationEvent {

    object ReAuthenticate : NavigationEvent()
    object Bookmarks : NavigationEvent()
    object ForumIndex : NavigationEvent()
    data class Thread(val id: Int, val page: Int? = null, val postJump: String? = null) : NavigationEvent()
    data class Forum(val id: Int, val page: Int? = null) : NavigationEvent()
    data class Url(val url: AwfulURL) : NavigationEvent()

    // TODO: use specific type constants for each intent type - so bookmarks has an extra TYPE = BOOKMARKS, not just a forum ID that matches the bookmarks ID

    fun getIntent(context: Context): Intent =
            Intent().setClass(context, ForumsIndexActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .apply {
                        when (this@NavigationEvent) {
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
                            is ReAuthenticate -> putExtra(TYPE_RE_AUTHENTICATE, true)
                        }
                    }


    companion object {

        private const val TYPE_RE_AUTHENTICATE = "re-auth"

        /**
         * Parse an intent as one of the navigation events we handle. Defaults to [ForumIndex]
         */
        fun Intent.parse(): NavigationEvent {
            parseUrl()?.let { return it }
            return when {
                hasExtra(TYPE_RE_AUTHENTICATE) ->
                    ReAuthenticate
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

        /**
         * Attempt to parse an AwfulURL from an intent, converting to a NavigationEvent.
         *
         * Returns null if a valid URL couldn't be found.
         */
        private fun Intent.parseUrl(): NavigationEvent? {
            data?.scheme.apply {
                if (!equals("http") && !equals("https")) return null
            }
            with(AwfulURL.parse(dataString)) {
                // this mirrors the old behaviour in ForumsIndexActivity - basically we need to
                // hand the URL over to the ThreadDisplayFragment if it's a post or a redirecting thread.
                return when {
                // TODO: if it's a post, we're meant to pass the actual url through TDF.openThread(url) - let's not do that and just handle it here
                    isPost || (isThread && isRedirect) ->
                        Url(this)
                // TODO: can we spot null pages here? OR at least default ones?
                    isForum ->
                        Forum(id.toInt(), page.toInt())
                // TODO: can we get the fragment?
                    isThread ->
                        Thread(id.toInt(), page.toInt())
                    isForumIndex ->
                        ForumIndex
                    else -> null
                }
            }
        }

        private fun Intent.getIntExtra(name: String) = if (hasExtra(name)) getIntExtra(name, -12345) else null
    }
}
