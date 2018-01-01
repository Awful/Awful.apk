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


import android.content.Intent
import android.database.ContentObserver
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.widget.SwipeRefreshLayout
import android.view.*
import android.widget.AdapterView
import android.widget.ListView
import com.android.volley.VolleyError
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.SettingsActivity
import com.ferg.awfulapp.provider.AwfulProvider
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.service.AwfulCursorAdapter
import com.ferg.awfulapp.task.AwfulRequest
import com.ferg.awfulapp.task.PMListRequest
import com.ferg.awfulapp.thread.AwfulForum
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.util.AwfulUtils
import timber.log.Timber

class PrivateMessageListFragment : AwfulFragment(), SwipeRefreshLayout.OnRefreshListener {

    lateinit private var mCursorAdapter: AwfulCursorAdapter
    private val mPMDataCallback = PMIndexCallback(mHandler)

    lateinit private var mPMList: ListView
    lateinit private var refreshLayout: SwipeRefreshLayout
    lateinit private var mFAB: FloatingActionButton

    private var currentFolder = FOLDER_INBOX

    private val onButtonClick = View.OnClickListener { aView ->
        when (aView.id) {
            R.id.just_pm -> if (activity is PrivateMessageActivity) {
                (activity as PrivateMessageActivity).showMessage(null, 0)
            }
            R.id.new_pm -> startActivity(Intent().setClass(activity!!, MessageDisplayActivity::class.java))
            R.id.refresh -> syncPMs()
        }
    }

    private val onPMSelected = AdapterView.OnItemClickListener { _, _, _, aId ->
        if (activity is PrivateMessageActivity) {
            (activity as PrivateMessageActivity).showMessage(null, aId.toInt())
        } else {
            startActivity(Intent(activity, MessageDisplayActivity::class.java).putExtra(Constants.PARAM_PRIVATE_MESSAGE_ID, aId.toInt()))
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onCreateView(aInflater: LayoutInflater, aContainer: ViewGroup?, aSavedState: Bundle?): View? {
        super.onCreateView(aInflater, aContainer, aSavedState)

        mPrefs = AwfulPreferences.getInstance(this.activity)

        val result = aInflater.inflate(R.layout.private_message_list_fragment, aContainer, false)

        mPMList = result.findViewById(R.id.message_listview)
        mFAB = result.findViewById(R.id.just_pm)
        mFAB.setOnClickListener(onButtonClick)
        mFAB.visibility = if (mPrefs.noFAB) View.GONE else View.VISIBLE

        return result
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refreshLayout = view.findViewById(R.id.pm_swipe)
        refreshLayout.setOnRefreshListener(this)
        refreshLayout.setColorSchemeResources(*ColorProvider.getSRLProgressColors(null))
        refreshLayout.setProgressBackgroundColorSchemeResource(ColorProvider.getSRLBackgroundColor(null))
    }

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)

        mPMList.onItemClickListener = onPMSelected
        mCursorAdapter = AwfulCursorAdapter(activity as AwfulActivity?, null, this)
        mPMList.adapter = mCursorAdapter
    }

    override fun onStart() {
        super.onStart()
        restartLoader(Constants.PRIVATE_MESSAGE_THREAD, null, mPMDataCallback)
        activity?.contentResolver?.registerContentObserver(AwfulForum.CONTENT_URI, true, mPMDataCallback)
        syncPMs()
        setTitle(getTitle())
    }

    private fun syncPMs() {
        refreshLayout.isRefreshing = true
        if (activity != null) {
            queueRequest(PMListRequest(activity, currentFolder).build(this, object : AwfulRequest.AwfulResultCallback<Void> {
                override fun success(result: Void?) {
                    restartLoader(Constants.PRIVATE_MESSAGE_THREAD, null, mPMDataCallback)
                    refreshLayout.isRefreshing = false
                    mPMList.setSelectionAfterHeaderView()
                }

                override fun failure(error: VolleyError) {
                    if (null != error.message && error.message!!.startsWith("java.net.ProtocolException: Too many redirects")) {
                        Timber.e("Error: ${error.message}")
                        Timber.e("!!!Failed to sync PMs - You are now LOGGED OUT")
                        NetworkUtils.clearLoginCookies(awfulActivity)
                        awfulActivity!!.startActivity(Intent().setClass(awfulActivity!!, AwfulLoginActivity::class.java))
                    }
                    refreshLayout.isRefreshing = false
                }
            }))
        }
    }


    override fun onStop() {
        super.onStop()
        activity?.supportLoaderManager?.destroyLoader(Constants.PRIVATE_MESSAGE_THREAD)
        activity?.contentResolver?.unregisterContentObserver(mPMDataCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        if (menu!!.size() == 0) {
            inflater!!.inflate(R.menu.private_message_list, menu)
        }

        val newPM = menu.findItem(R.id.new_pm)
        newPM?.isVisible = mPrefs.noFAB

        val sendPM = menu.findItem(R.id.send_pm)
        sendPM?.isVisible = AwfulUtils.isTablet(activity)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.new_pm -> if (activity is PrivateMessageActivity) {
                (activity as PrivateMessageActivity).showMessage(null, 0)
            }
            R.id.refresh -> syncPMs()
            R.id.toggle_folder -> {
                currentFolder = if (currentFolder == FOLDER_INBOX) FOLDER_SENT else FOLDER_INBOX
                setTitle(getTitle())
                changeIcon(item)
                syncPMs()
            }
            R.id.settings -> startActivity(Intent().setClass(activity!!, SettingsActivity::class.java))
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun changeIcon(item: MenuItem) {
        if (currentFolder == FOLDER_SENT) {
            item.setIcon(R.drawable.ic_inbox)
        } else {
            item.setIcon(R.drawable.ic_drawer_outbox)
        }
    }

    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        super.onPreferenceChange(prefs, key)
        if ("no_fab" == key) {
            mFAB.visibility = if (prefs.noFAB) View.GONE else View.VISIBLE
            invalidateOptionsMenu()
        }
    }

    private inner class PMIndexCallback(handler: Handler) : ContentObserver(handler), LoaderManager.LoaderCallbacks<Cursor> {

        override fun onCreateLoader(aId: Int, aArgs: Bundle?): Loader<Cursor> {
            Timber.i("Load PM Cursor.")
            return CursorLoader(activity!!,
                    AwfulMessage.CONTENT_URI,
                    AwfulProvider.PMProjection,
                    AwfulMessage.FOLDER + "=?",
                    AwfulProvider.int2StrArray(currentFolder),
                    AwfulMessage.ID + " DESC")
        }

        override fun onLoadFinished(aLoader: Loader<Cursor>, aData: Cursor?) {
            if (aData != null) {
                Timber.v("PM load finished, populating: ${aData.count}")
            }
            mCursorAdapter.swapCursor(aData)
        }

        override fun onLoaderReset(aLoader: Loader<Cursor>) {
            mCursorAdapter.swapCursor(null)
        }

        override fun onChange(selfChange: Boolean) {
            Timber.i("PM Data update.")
            restartLoader(Constants.PRIVATE_MESSAGE_THREAD, null, this)
        }
    }


    override fun getTitle(): String {
        when (currentFolder) {
            FOLDER_INBOX -> return "Inbox"
            FOLDER_SENT -> return "Sent"
        }
        return "Messages"
    }


    override fun onRefresh() {
        syncPMs()
    }

    companion object {
        val FOLDER_INBOX = 0
        val FOLDER_SENT = -1
    }
}
