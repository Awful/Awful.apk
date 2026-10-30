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

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.DialogFragment
import androidx.loader.app.LoaderManager
import com.android.volley.Request
import com.android.volley.VolleyError
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.AwfulPreferences.AwfulPreferenceUpdate
import com.ferg.awfulapp.task.AwfulRequest
import com.ferg.awfulapp.util.AwfulError
import com.ferg.awfulapp.widget.AlertView
import com.ferg.awfulapp.widget.AwfulProgressBar

/**
 * AwfulFragment's red-headed stepchild.
 * Currently only exists for EmoteFragment, and usually falls behind on changes made to AwfulFragment.
 * Welp.
 */
abstract class AwfulDialogFragment : DialogFragment(), ActionMode.Callback,
    AwfulRequest.ProgressListener, AwfulPreferenceUpdate {
    companion object {
        protected var TAG: String = "AwfulFragment"
    }
    protected var alertView: AlertView? = null
        get() {
            if (field == null) {
                field = AlertView(activity)
            }
            return field
        }
        private set

    protected lateinit var prefs: AwfulPreferences
    var progressPercent: Int = 100
        protected set
    private var progressBar: AwfulProgressBar? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is AwfulActivity) {
            Log.e("AwfulFragment", "PARENT ACTIVITY NOT EXTENDING AwfulActivity!")
        }
        prefs = AwfulPreferences.getInstance(this.awfulActivity, this)
    }

    protected fun inflateView(resId: Int, container: ViewGroup?, inflater: LayoutInflater): View {
        val v = inflater.inflate(resId, container, false)
        val progressBar = v.findViewById<View?>(R.id.progress_bar)
        if (progressBar is AwfulProgressBar) {
            this.progressBar = progressBar
        }
        this.awfulActivity.setPreferredFont(v)
        return v
    }

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        onPreferenceChange(prefs, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterCallback(this)
    }


    val awfulActivity: AwfulActivity
        get() = requireActivity() as AwfulActivity

    protected fun setProgress(percent: Int) {
        this.progressPercent = percent
        progressBar?.setProgress(percent, activity)
    }

    protected val isFragmentVisible: Boolean
        get() = isVisible

    protected fun startActionMode() {
        this.awfulActivity.startSupportActionMode(this)
    }

    override fun onPreferenceChange(preferences: AwfulPreferences, key: String?) {
    }

    fun onBackPressed(): Boolean {
        return false
    }

    abstract fun getTitle() : String

    protected fun setTitle(title: String) {
        this.awfulActivity.setActionbarTitle(title)
    }

    open fun volumeScroll(event: KeyEvent?): Boolean {
        return false
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {}

    @JvmOverloads
    fun queueRequest(request: Request<*>?, cancelOnDestroy: Boolean = false) {
        if (request != null) {
            if (cancelOnDestroy) {
                request.tag = this
            }
            NetworkUtils.queueRequest(request)
        }
    }

    protected fun cancelNetworkRequests() {
        NetworkUtils.cancelRequests(this)
    }

    override fun requestStarted(req: AwfulRequest<*>) {
        val aa = this.awfulActivity
        aa.setSupportProgressBarVisibility(false)
        aa.setSupportProgressBarIndeterminateVisibility(true)
    }

    override fun requestUpdate(req: AwfulRequest<*>, percent: Int) {
        setProgress(percent)
    }

    override fun requestEnded(req: AwfulRequest<*>, error: VolleyError?) {
        val aa = this.awfulActivity
        aa.setSupportProgressBarIndeterminateVisibility(false)
        aa.setSupportProgressBarVisibility(false)
        if (error is AwfulError) {
            this.alertView?.show(error)
        } else if (error != null) {
            this.alertView?.let {
                it.setTitle(R.string.loading_failed)
                it.setIcon(R.drawable.ic_error)
                it.show()
            }
        }
    }

    protected fun restartLoader(
        id: Int,
        data: Bundle?,
        callback: LoaderManager.LoaderCallbacks<out Any?>
    ) {
        if (activity != null) {
            loaderManager.restartLoader(id, data, callback)
        }
    }
}
