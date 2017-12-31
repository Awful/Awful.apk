/********************************************************************************
 * Copyright (c) 2012, Matthew Shepard
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the software nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ferg.awfulapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.annotation.CallSuper
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.text.TextUtils
import android.view.*
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.VolleyError
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.task.AwfulRequest
import com.ferg.awfulapp.util.AwfulError
import com.ferg.awfulapp.widget.AwfulProgressBar
import com.ferg.awfulapp.widget.ProbationBar
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection.BOTH
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection.TOP
import timber.log.Timber

abstract class AwfulFragment : Fragment(), ProgressListener, AwfulPreferences.AwfulPreferenceUpdate {
    protected var TAG = "AwfulFragment"

    lateinit protected var mPrefs: AwfulPreferences
    lateinit private var mProgressBar: AwfulProgressBar
    lateinit private var probationBar: ProbationBar

    protected var mSRL: SwipyRefreshLayout? = null

    var progressPercent = 100

    protected val mHandler = Handler()


    val awfulActivity: AwfulActivity?
        get() = activity as AwfulActivity?

    protected val awfulApplication: AwfulApplication?
        get() = awfulActivity?.application as AwfulApplication?

    /** Get this fragment's display title  */
    /**
     * Set the actionbar's title.
     * @param title The text to set as the title
     */
    // TODO: fix race condition in ForumDisplayFragment and ThreadDisplayFragment - both restart their loaders in onResume,
    // both of those set the actionbar title - even in phone mode where only one is visible. Whichever loads last sets the actionbar text
    @CallSuper
    open fun setTitle(title: String) {
        awfulActivity?.let {
            Timber.d("setTitle: setting for %s", this.javaClass.simpleName)
            it.setActionbarTitle(title, this)
        }
    }
    abstract fun getTitle() : String?

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (activity !is AwfulActivity) {
            throw IllegalStateException("AwfulFragment - parent activity must extend AwfulActivity!")
        }
        mPrefs = AwfulPreferences.getInstance(context, this)
    }

    protected fun inflateView(resId: Int, container: ViewGroup?, inflater: LayoutInflater): View {
        val v = inflater.inflate(resId, container, false)
        mProgressBar = v.findViewById(R.id.progress_bar)

        // set up the probation bar, if we have one - use this ID when adding to a layout!
        probationBar = v.findViewById(R.id.probation_bar)
        probationBar.setListener { goToLeperColony() }

        return v
    }

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        onPreferenceChange(mPrefs, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        this.cancelNetworkRequests()
        mHandler.removeCallbacksAndMessages(null)
        mPrefs.unregisterCallback(this)
    }

    override fun onDetach() {
        super.onDetach()
        this.cancelNetworkRequests()
        mHandler.removeCallbacksAndMessages(null)
    }

    protected fun displayForumIndex() {
        awfulActivity?.displayForumIndex()
    }

    protected fun displayForumContents(aId: Int) {
        awfulActivity?.displayForum(aId, 1)
    }

    protected fun displayThread(aId: Int, aPage: Int, forumId: Int, forumPage: Int, forceReload: Boolean) {
        awfulActivity?.displayThread(aId, aPage, forumId, forumPage, forceReload)
    }

    protected fun displayForum(forumId: Long, page: Long) {
        awfulActivity?.displayForum(forumId.toInt(), page.toInt())
    }

    fun displayPostReplyDialog(threadId: Int, postId: Int, type: Int) {
        awfulActivity?.runOnUiThread {
            if (activity != null) {
                startActivityForResult(
                        Intent(activity, PostReplyActivity::class.java)
                                .putExtra(Constants.REPLY_THREAD_ID, threadId)
                                .putExtra(Constants.EDITING, type)
                                .putExtra(Constants.REPLY_POST_ID, postId),
                        PostReplyFragment.REQUEST_POST)
            }
        }
    }

    protected fun setProgress(percent: Int) {
        progressPercent = percent
        if (progressPercent > 0) {
            mSRL?.isRefreshing = false
        }
        mProgressBar.setProgress(percent, activity)
    }

    protected fun makeToast(@StringRes text: Int, length: Int = Toast.LENGTH_LONG) {
        makeToast(getString(text), length)
    }

    protected fun makeToast(text: String, length: Int = Toast.LENGTH_LONG) {
        if (activity != null) {
            Toast.makeText(activity, text, length).show()
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Probation bar
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Refresh the probation bar's visual state
     */
    protected fun refreshProbationBar() {
        probationBar.setProbation(if (mPrefs.isOnProbation) mPrefs.probationTime else null)
    }

    /**
     * Open the Leper Colony page - call this when the user clicks the probation button
     */
    private fun goToLeperColony() {
        val openThread = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.FUNCTION_BANLIST + '?' + Constants.PARAM_USER_ID + "=" + mPrefs.userId))
        startActivity(openThread)
    }


    ///////////////////////////////////////////////////////////////////////////
    // Reacting to request progress
    ///////////////////////////////////////////////////////////////////////////

    override fun requestStarted(req: AwfulRequest<*>) {
            // P2R Library is ... awful - part 1
            mSRL?.direction = TOP
            mSRL?.isRefreshing = true
    }

    override fun requestUpdate(req: AwfulRequest<*>, percent: Int) {
        setProgress(percent)
    }

    override fun requestEnded(req: AwfulRequest<*>, error: VolleyError?) {
        mSRL?.isRefreshing = false
        // P2R Library is ... awful - part 2
        mSRL?.direction = if (this is ThreadDisplayFragment) BOTH else TOP

        if (error is AwfulError) {
            AlertBuilder().fromError((error as AwfulError?)!!).show()
        } else if (error != null) {
            AlertBuilder().setTitle(R.string.loading_failed).show()
        }
    }

    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        if (mSRL != null) {
            val displayMetrics = this.resources.displayMetrics
            val dpHeight = displayMetrics.heightPixels / displayMetrics.density

            mSRL?.setDistanceToTriggerSync(Math.round(prefs.p2rDistance * dpHeight))
        }
    }

    open fun onBackPressed() = false

    /**
     * Queue a network [Request].
     * Set true to tag the request with the fragment, so it will be cancelled
     * when the fragment is destroyed. Set false if you want to retain the request's
     * default tag, e.g. so pending [com.ferg.awfulapp.task.PostRequest]s can
     * be cancelled when starting a new one.
     * @param request           A Volley request
     * @param cancelOnDestroy   Whether to tag with the fragment and automatically cancel
     */
    @JvmOverloads
    fun queueRequest(request: Request<*>?, cancelOnDestroy: Boolean = false) {
        if (request != null) {
            if (cancelOnDestroy) {
                request.tag = this
            }
            NetworkUtils.queueRequest(request)
        }
    }

    protected open fun cancelNetworkRequests() {
        NetworkUtils.cancelRequests(this)
    }

    protected fun invalidateOptionsMenu() {
        awfulActivity?.invalidateOptionsMenu()
    }

    open fun onPageVisible() {}
    open fun onPageHidden() {}


    /**
     * Try to handle a KeyEvent as a volume scroll action.
     * @param event The event to handle
     * @return      true if the event was consumed
     */
    fun attemptVolumeScroll(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode

        return if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                doScroll(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
            } else {
                true
            }
        } else false
    }


    /**
     * Perform a scroll action, e.g. in response to a volume scroll event.
     *
     * Does nothing by default, override this and return true to handle it.
     * @param down  true to scroll down, false for up
     * @return      return true to consume this scroll event
     */
    protected open fun doScroll(down: Boolean) = false


    /**
     * Builds and displays alert toasts
     */
    protected inner class AlertBuilder {
        private var title = ""
        private var subtitle = ""
        @DrawableRes
        private var iconResId = 0
        private var animation: Animation? = null

        fun setTitle(@StringRes title: Int): AlertBuilder {
            if (activity != null) {
                this.title = getString(title)
            }
            return this
        }

        fun setTitle(title: String?): AlertBuilder {
            this.title = title ?: ""
            return this
        }

        fun setSubtitle(@StringRes subtitle: Int): AlertBuilder {
            this.subtitle = getString(subtitle)
            return this
        }

        fun setSubtitle(subtitle: String?): AlertBuilder {
            this.subtitle = subtitle ?: ""
            return this
        }

        fun setIcon(@DrawableRes iconResId: Int): AlertBuilder {
            this.iconResId = iconResId
            return this
        }

        fun setIconAnimation(animation: Animation?): AlertBuilder {
            this.animation = animation
            return this
        }

        fun fromError(error: AwfulError): AlertBuilder {
            setTitle(error.message)
            setSubtitle(error.subMessage)
            setIcon(error.iconResource)
            setIconAnimation(error.iconAnimation)
            return this
        }

        fun show() {
            activity?.runOnUiThread {
                displayAlertInternal(title, subtitle, iconResId, animation)
            }
        }
    }


    private fun displayAlertInternal(title: String, subtext: String, iconRes: Int, animate: Animation?) {
        val activity = activity ?: return
        val inflater = activity.layoutInflater
        val popup = inflater.inflate(R.layout.alert_popup,
                activity.findViewById<View>(R.id.alert_popup_root) as ViewGroup)
        val popupTitle = popup.findViewById<View>(R.id.popup_title) as TextView
        popupTitle.text = title
        val popupSubTitle = popup.findViewById<View>(R.id.popup_subtitle) as TextView
        if (TextUtils.isEmpty(subtext)) {
            popupSubTitle.visibility = View.GONE
        } else {
            popupSubTitle.visibility = View.VISIBLE
            popupSubTitle.text = subtext
        }
        if (iconRes != 0) {
            val popupIcon = popup.findViewById<View>(R.id.popup_icon) as ImageView
            if (animate != null) {
                popupIcon.setImageResource(iconRes)
                popupIcon.startAnimation(animate)
            } else {
                popupIcon.setImageResource(iconRes)
            }
        }
        val toast = Toast(awfulApplication)
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
        toast.duration = Toast.LENGTH_LONG
        toast.view = popup
        toast.show()
    }

    protected fun restartLoader(id: Int, data: Bundle?, callback: LoaderManager.LoaderCallbacks<out Any>) {
        if (activity != null) {
            Timber.d("loader id is $id")
            loaderManager.restartLoader(id, data, callback)
        }
    }


    /**
     * Utility method to safely handle clipboard copying.
     * A [bug in 4.3](https://code.google.com/p/android/issues/detail?id=58043)
     * means that clipboard writes can throw runtime exceptions if another app has registered
     * as a listener. This method catches them and displays an error message for the user.
     *
     * @param label             The [ClipData]'s label
     * @param clipText          The [ClipData]'s text
     * @param successMessageId  If supplied, a success message popup will be displayed
     * @return                  false if the copy failed
     */
    protected fun safeCopyToClipboard(label: String,
                                      clipText: String,
                                      @StringRes successMessageId: Int?): Boolean {
        val clipboard = this.activity!!.getSystemService(
                Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, clipText)
        try {
            clipboard.primaryClip = clip
            if (successMessageId != null) {
                AlertBuilder().setTitle(successMessageId)
                        .setIcon(R.drawable.ic_insert_link_dark)
                        .show()
            }
            return true
        } catch (e: IllegalArgumentException) {
            AlertBuilder().setTitle("Unable to copy to clipboard!")
                    .setSubtitle("Another app has locked access, you may need to reboot")
                    .setIcon(R.drawable.ic_error)
                    .show()
            e.printStackTrace()
            return false
        } catch (e: SecurityException) {
            AlertBuilder().setTitle("Unable to copy to clipboard!").setSubtitle("Another app has locked access, you may need to reboot").setIcon(R.drawable.ic_error).show()
            e.printStackTrace()
            return false
        } catch (e: IllegalStateException) {
            AlertBuilder().setTitle("Unable to copy to clipboard!").setSubtitle("Another app has locked access, you may need to reboot").setIcon(R.drawable.ic_error).show()
            e.printStackTrace()
            return false
        }
    }

    companion object {
        protected val DEBUG = Constants.DEBUG
    }
}
