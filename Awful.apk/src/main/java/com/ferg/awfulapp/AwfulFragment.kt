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
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.VolleyError
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.task.AwfulRequest
import com.ferg.awfulapp.util.AwfulError
import com.ferg.awfulapp.widget.AlertView
import com.ferg.awfulapp.widget.AwfulProgressBar
import com.ferg.awfulapp.widget.ProbationBar
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection.BOTH
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection.TOP
import timber.log.Timber

abstract class AwfulFragment : Fragment(), AwfulPreferences.AwfulPreferenceUpdate, AwfulRequest.ProgressListener<Any> {
    protected var TAG = "AwfulFragment"

    protected val prefs: AwfulPreferences by lazy { AwfulPreferences.getInstance(context!!, this) }
    protected val handler: Handler by lazy { Handler() }
    protected val alertView: AlertView by lazy { AlertView(activity) }

    protected var swipyLayout: SwipyRefreshLayout? = null
    private var progressBar: AwfulProgressBar? = null
    private var probationBar: ProbationBar? = null

    var progressPercent = 100


    val awfulActivity
        get() = activity as AwfulActivity?

    protected val awfulApplication
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
            Timber.d("setTitle: setting for %s - DISABLED FOR FRAGMENTS", this.javaClass.simpleName)
//            it.setActionbarTitle(title)
        }
    }

    abstract fun getTitle(): String?

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (activity !is AwfulActivity) {
            throw IllegalStateException("AwfulFragment - parent activity must extend AwfulActivity!")
        }
    }

    protected fun inflateView(resId: Int, container: ViewGroup?, inflater: LayoutInflater): View {
        val v = inflater.inflate(resId, container, false)

        // set up the probation and progress bar, if we have them - use this ID when adding to a layout!
        progressBar = v.findViewById(R.id.progress_bar)
        probationBar = v.findViewById(R.id.probation_bar)
        probationBar?.setListener { goToLeperColony() }
        return v
    }

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        onPreferenceChange(prefs, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        this.cancelNetworkRequests()
        handler.removeCallbacksAndMessages(null)
        prefs.unregisterCallback(this)
    }

    override fun onDetach() {
        super.onDetach()
        this.cancelNetworkRequests()
        handler.removeCallbacksAndMessages(null)
    }

    protected fun displayForumIndex() {
        awfulActivity?.showForumIndex()
    }

    protected fun displayThread(id: Int, page: Int? = null, postJump: String? = null, forceReload: Boolean) {
        awfulActivity?.showThread(id, page, postJump, forceReload)
    }

    protected fun displayForum(forumId: Int, page: Int? = null) {
        awfulActivity?.showForum(forumId, page)
    }

    fun displayPostReplyDialog(threadId: Int, postId: Int, type: Int) {
        awfulActivity?.apply { runOnUiThread { showPostComposer(threadId, type, postId) }}
    }

    protected fun setProgress(percent: Int) {
        progressPercent = percent
        if (progressPercent > 0) {
            swipyLayout?.isRefreshing = false
        }
        progressBar?.setProgress(percent, activity)
    }

    protected fun makeToast(@StringRes text: Int, length: Int = Toast.LENGTH_LONG) {
        makeToast(getString(text), length)
    }

    protected fun makeToast(text: String, length: Int = Toast.LENGTH_LONG) {
        activity?.let { Toast.makeText(it, text, length).show() }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Probation bar
    ///////////////////////////////////////////////////////////////////////////

    protected fun refreshProbationBar() {
        probationBar?.setProbation(if (prefs.isOnProbation) prefs.probationTime else null)
    }

    // Open the Leper Colony page - call this when the user clicks the probation button
    private fun goToLeperColony() {
        val openThread = Intent(Intent.ACTION_VIEW, Uri.parse("""${Constants.FUNCTION_BANLIST}?${Constants.PARAM_USER_ID}=${prefs.userId}"""))
        startActivity(openThread)
    }


    ///////////////////////////////////////////////////////////////////////////
    // Reacting to request progress
    ///////////////////////////////////////////////////////////////////////////

    override fun requestStarted(req: AwfulRequest<Any>) {
        // P2R Library is ... awful - part 1
        swipyLayout?.direction = TOP
        swipyLayout?.isRefreshing = true
    }

    override fun requestUpdate(req: AwfulRequest<Any>, percent: Int) {
        setProgress(percent)
    }

    override fun requestEnded(req: AwfulRequest<Any>, error: VolleyError?) {
        // P2R Library is ... awful - part 2
        swipyLayout?.isRefreshing = false
        swipyLayout?.direction = if (this is ThreadDisplayFragment) BOTH else TOP

        when (error) {
            is AwfulError -> alertView.show(error)
            is VolleyError -> alertView.setTitle(R.string.loading_failed).setIcon(R.drawable.ic_error).show()
        }
    }

    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        swipyLayout?.apply {
            val dpHeight = with(this@AwfulFragment.resources.displayMetrics) { heightPixels / density }
            setDistanceToTriggerSync(Math.round(prefs.p2rDistance * dpHeight))
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
        request?.let {
            if (cancelOnDestroy) request.tag = this
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
        return with(event) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (action == KeyEvent.ACTION_DOWN) {
                    doScroll(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                } else {
                    true
                }
            } else false
        }
    }


    /**
     * Perform a scroll action, e.g. in response to a volume scroll event.
     *
     * Does nothing by default, override this and return true to handle it.
     * @param down  true to scroll down, false for up
     * @return      return true to consume this scroll event
     */
    protected open fun doScroll(down: Boolean) = false

    protected fun restartLoader(id: Int, data: Bundle?, callback: LoaderManager.LoaderCallbacks<out Any>) {
        activity?.let {
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

        val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, clipText)

        fun handle(e: Exception): Boolean {
            alertView.setTitle("Unable to copy to clipboard!")
                    .setSubtitle("Another app has locked access, you may need to reboot")
                    .setIcon(R.drawable.ic_error).show()
            Timber.e(e, "Clipboard exception")
            return false
        }

        return try {
            clipboard.primaryClip = clip
            successMessageId?.let {
                alertView.setTitle(successMessageId).setIcon(R.drawable.ic_insert_link_dark).show()
            }
            true
        } catch (e: IllegalArgumentException) {
            handle(e)
        } catch (e: SecurityException) {
            handle(e)
        } catch (e: IllegalStateException) {
            handle(e)
        }
    }
}