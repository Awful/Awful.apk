package com.ferg.awfulapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.http.HttpResponseCache
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import com.ferg.awfulapp.constants.Constants
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
 * Convenience class to avoid having to call a configurator's lifecycle methods everywhere. This
 * class should avoid implementing things directly; the ActivityConfigurator does that job.
 *
 * Most Activities in this awful app should extend this guy; that will provide things like locking
 * orientation according to user preference.
 *
 * This class also provides a few helper methods for grabbing preferences and the like.
 */
abstract class AwfulActivity : AppCompatActivity(), AwfulPreferences.AwfulPreferenceUpdate {
    private var mConf: ActivityConfigurator? = null
    private var loggedIn = false
    private var mTitleView: TextView? = null
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
        startActivityForResult(Intent(this, AwfulLoginActivity::class.java), Constants.LOGIN_ACTIVITY_REQUEST)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.i("*** onCreate")
        mPrefs.registerCallback(this)
        setCurrentTheme()
        super.onCreate(savedInstanceState)
        mConf = ActivityConfigurator(this)
        mConf!!.onCreate()
    }

    override fun onStart() {
        Timber.i("*** onStart")
        super.onStart()
        mConf!!.onStart()
    }

    override fun onResume() {
        Timber.i("*** onResume")
        super.onResume()
        mConf!!.onResume()
        // check login state when coming back into use, instead of in onCreate
        loggedIn = NetworkUtils.restoreLoginCookies(application)
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
        mConf!!.onPause()
    }

    @SuppressLint("NewApi")
    override fun onStop() {
        Timber.i("*** onStop")
        super.onStop()
        mConf!!.onStop()
        HttpResponseCache.getInstalled()?.flush()
    }

    override fun onDestroy() {
        Timber.i("*** onDestroy")
        super.onDestroy()
        mConf!!.onDestroy()
        mPrefs.unregisterCallback(this)
    }


    override fun onActivityResult(request: Int, result: Int, intent: Intent) {
        Timber.i(TAG, "onActivityResult: $request result: $result")
        super.onActivityResult(request, result, intent)
        if (request == Constants.LOGIN_ACTIVITY_REQUEST && result == Activity.RESULT_CANCELED) {
            finish()
        }
    }

    protected fun setActionBar() {
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setCustomView(R.layout.actionbar_title)
            mTitleView = customView as TextView
            mTitleView!!.movementMethod = ScrollingMovementMethod()
            updateActionbarTheme()
            setDisplayShowCustomEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun updateActionbarTheme() {
        supportActionBar?.apply {
            mTitleView?.let { title ->
                title.setTextColor(ColorProvider.ACTION_BAR_TEXT.color)
                setPreferredFont(title, Typeface.NORMAL)
            }
        }
    }

    open fun displayUserCP() = displayForum(Constants.USERCP_ID, 1)

    open fun displayThread(id: Int, page: Int, forumId: Int, forumPage: Int, forceReload: Boolean) {
        startActivity(Intent().setClass(this, ForumsIndexActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Constants.THREAD_ID, id)
                .putExtra(Constants.THREAD_PAGE, page)
                .putExtra(Constants.FORUM_ID, forumId)
                .putExtra(Constants.FORUM_PAGE, forumPage))
    }

    open fun displayForum(id: Int, page: Int) {
        startActivity(Intent().setClass(this, ForumsIndexActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Constants.FORUM_ID, id)
                .putExtra(Constants.FORUM_PAGE, page))
    }

    open fun displayForumIndex() {
        startActivity(Intent().setClass(this, ForumsIndexActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    // TODO: this is janky with the !! everywhere, it can probably be refactored?
    open fun setActionbarTitle(aTitle: String?, requester: Any?) {
        supportActionBar?.apply { mTitleView = customView as TextView }
        if (mTitleView != null && !aTitle.isNullOrEmpty()) {
            mTitleView!!.text = aTitle
            mTitleView!!.scrollTo(0, 0)
        } else {
            Timber.w("FAILED setActionbarTitle - $aTitle")
        }
    }

    @JvmOverloads
    fun setPreferredFont(view: View?, flags: Int = -1) =
            view?.let { (application as AwfulApplication).setPreferredFont(view, flags) }


    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        Timber.i("Key changed: ${key!!}")
        if ("theme" == key || "page_layout" == key) {
            setCurrentTheme()
            afterThemeChange()
        }
        updateActionbarTheme()
    }


    protected fun setCurrentTheme() = setTheme(AwfulTheme.forForum(null).themeResId)

    private fun afterThemeChange() = recreate()

    override fun getCacheDir(): File {
        Timber.i(TAG, "getCacheDir(): ${super.getCacheDir()}")
        return super.getCacheDir()
    }

    var TAG = "AwfulActivity"
    companion object {
        val DEBUG = Constants.DEBUG
    }
}
