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
open class AwfulActivity : AppCompatActivity(), AwfulPreferences.AwfulPreferenceUpdate {

    lateinit private var mConf: ActivityConfigurator
    private var loggedIn = false
    private var mTitleView: TextView? = null

    lateinit protected var mPrefs: AwfulPreferences

    private val awfulApplication: AwfulApplication
        get() = application as AwfulApplication

    val isLoggedIn: Boolean
        get() {
            if (!loggedIn) {
                loggedIn = NetworkUtils.restoreLoginCookies(this.awfulApplication)
            }
            return loggedIn
        }

    private fun reauthenticate() {
        NetworkUtils.clearLoginCookies(this)
        startActivityForResult(Intent(this, AwfulLoginActivity::class.java), Constants.LOGIN_ACTIVITY_REQUEST)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mPrefs = AwfulPreferences.getInstance(this, this)
        setCurrentTheme()
        super.onCreate(savedInstanceState)
        Timber.v("onCreate")
        mConf = ActivityConfigurator(this)
        mConf.onCreate()
    }

    override fun onStart() {
        super.onStart()
        Timber.v("onStart")
        mConf.onStart()
    }

    override fun onResume() {
        super.onResume()
        Timber.v("onResume")
        mConf.onResume()
        // check login state when coming back into use, instead of in onCreate
        loggedIn = NetworkUtils.restoreLoginCookies(this.awfulApplication)
        if (isLoggedIn) {
            if (mPrefs.ignoreFormkey == null || mPrefs.userTitle == null) {
                NetworkUtils.queueRequest(ProfileRequest(this).build(null, null))
            }
            if (mPrefs.ignoreFormkey == null) {
                NetworkUtils.queueRequest(FeatureRequest(this).build(null, null))
            }
            Timber.v("Cookie Loaded!")
        } else {
            if (this !is AwfulLoginActivity) {
                reauthenticate()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.v("onPause")
        mConf.onPause()
    }

    @SuppressLint("NewApi")
    override fun onStop() {
        super.onStop()
        Timber.v("onStop")
        mConf.onStop()
        val cache = HttpResponseCache.getInstalled()
        cache?.flush()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.v("onDestroy")
        mConf.onDestroy()
        mPrefs.unregisterCallback(this)
    }


    override fun onActivityResult(request: Int, result: Int, intent: Intent) {
        super.onActivityResult(request, result, intent)
        Timber.v("onActivityResult: $request result: $result")
        if (request == Constants.LOGIN_ACTIVITY_REQUEST && result == Activity.RESULT_CANCELED) {
            finish()
        }
    }

    protected fun setActionBar() {
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setCustomView(R.layout.actionbar_title)
            mTitleView = customView as TextView
            mTitleView?.movementMethod = ScrollingMovementMethod()
            updateActionbarTheme()
            setDisplayShowCustomEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun updateActionbarTheme() {
        if (supportActionBar != null && mTitleView != null) {
            mTitleView?.setTextColor(ColorProvider.ACTION_BAR_TEXT.color)
            setPreferredFont(mTitleView, Typeface.NORMAL)
        }
    }

    open fun displayUserCP() {
        displayForum(Constants.USERCP_ID, 1)
    }

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

    open fun setActionbarTitle(aTitle: String?, requestor: Any?) {
        val action = supportActionBar
        if (action != null) {
            mTitleView = action.customView as TextView
        }
        if (aTitle != null && mTitleView != null && aTitle.isNotEmpty()) {
            //    		mTitleView.setText(Html.fromHtml(aTitle));
            mTitleView?.text = aTitle
            mTitleView?.scrollTo(0, 0)
        } else {
            Timber.v("FAILED setActionbarTitle - $aTitle")
        }
    }

    @JvmOverloads
    fun setPreferredFont(view: View?, flags: Int = -1) {
        if (application != null && view != null) {
            (application as AwfulApplication).setPreferredFont(view, flags)
        }
    }

    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        Timber.d("Key changed: $key")
        if ("theme" == key || "page_layout" == key) {
            setCurrentTheme()
            afterThemeChange()
        }
        updateActionbarTheme()
    }

    open fun isFragmentVisible(awfulFragment: AwfulFragment) = true

    protected fun setCurrentTheme() {
        setTheme(AwfulTheme.forForum(null).themeResId)
    }


    private fun afterThemeChange() {
        recreate()
    }

    override fun getCacheDir(): File {
        Timber.v("getCacheDir(): " + super.getCacheDir())
        return super.getCacheDir()
    }

    companion object {
        var TAG = "AwfulActivity"
        val DEBUG = Constants.DEBUG
    }
}
