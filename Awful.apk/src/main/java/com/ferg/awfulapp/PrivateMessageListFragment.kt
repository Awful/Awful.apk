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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import androidx.core.view.isEmpty
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.android.volley.VolleyError
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.AwfulProvider
import com.ferg.awfulapp.service.AwfulCursorAdapter
import com.ferg.awfulapp.task.AwfulRequest.AwfulResultCallback
import com.ferg.awfulapp.task.PMListRequest
import com.ferg.awfulapp.thread.AwfulForum
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.util.AwfulUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ferg.awfulapp.widget.AwfulRefreshLayout
import timber.log.Timber.Forest.tag
import timber.log.Timber.Forest.w


class PrivateMessageListFragment : AwfulFragment() {
    companion object {
        private const val TAG = "PrivateMessageList"

        const val FOLDER_INBOX: Int = Constants.PRIVATE_MESSAGE_DEFAULT_FOLDER
        const val FOLDER_SENT: Int = Constants.PRIVATE_MESSAGE_SENT_FOLDER
    }
    private var mPMList: ListView? = null

    private var isAllMessages = false

    private var mCursorAdapter: AwfulCursorAdapter? = null
    private val mPMDataCallback = PMIndexCallback(handler)

    private var mSRL: AwfulRefreshLayout? = null

    private var mFAB: FloatingActionButton? = null

    private var currentFolder: Int = FOLDER_INBOX

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        aInflater: LayoutInflater,
        aContainer: ViewGroup?,
        aSavedState: Bundle?
    ): View {
        super.onCreateView(aInflater, aContainer, aSavedState)

        val result = inflateView(
            R.layout.private_message_list_fragment,
            aContainer,
            aInflater
        )


        mPMList = result.findViewById<View?>(R.id.message_listview) as ListView


        mFAB = result.findViewById<View?>(R.id.just_pm) as FloatingActionButton
        mFAB?.setOnClickListener(onButtonClick)
        mFAB?.visibility = (if (prefs.noFAB) View.GONE else View.VISIBLE)

        awfulActivity?.setPreferredFont(result)
        return result
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mSRL = view.findViewById<View?>(R.id.pm_swipe) as AwfulRefreshLayout
        mSRL?.let {
            it.onLoadMore = ::loadAllMessages
            it.onRefresh = ::syncPMs
        }
    }

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)


        mPMList?.onItemClickListener = onPMSelected

        mCursorAdapter = AwfulCursorAdapter(activity as AwfulActivity, null, this)
        mPMList?.adapter = mCursorAdapter
    }

    override fun onStart() {
        super.onStart()
        restartLoader(Constants.PRIVATE_MESSAGE_THREAD, null, mPMDataCallback)
        requireActivity().contentResolver
            .registerContentObserver(AwfulForum.CONTENT_URI, true, mPMDataCallback)
        syncPMs()
        setActionBarTitle(getTitle())
    }

    private fun syncPMs(loadAll: Boolean = isAllMessages) {
        if (activity != null) {
            queueRequest(
                PMListRequest(requireActivity(), currentFolder, loadAll).build(
                    this,
                    object : AwfulResultCallback<Void?> {
                        override fun success(result: Void?) {
                            restartLoader(Constants.PRIVATE_MESSAGE_THREAD, null, mPMDataCallback)
                            mSRL?.finishedLoading()
                            mPMList?.setSelectionAfterHeaderView()
                        }

                        override fun failure(error: VolleyError?) {
                            w("Failed to sync PMs! Error: %s", error?.message)
                            // TODO: 28/01/2018 might be able to remove this everywhere - it's being set in AwfulFragment#onRequestEnded
                            mSRL?.finishedLoading()
                        }
                    })
            )
        }
    }

    override fun onStop() {
        super.onStop()
        requireActivity().supportLoaderManager.destroyLoader(Constants.PRIVATE_MESSAGE_THREAD)
        requireActivity().contentResolver.unregisterContentObserver(mPMDataCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (menu.isEmpty()) {
            inflater.inflate(R.menu.private_message_list, menu)
        }

        val newPM = menu.findItem(R.id.new_pm)
        if (null != newPM) {
            newPM.isVisible = prefs.noFAB
        }
        val sendPM = menu.findItem(R.id.send_pm)
        if (null != sendPM) {
            sendPM.isVisible = AwfulUtils.isTablet(requireActivity())
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.new_pm -> if (activity is PrivateMessageActivity) {
                (activity as PrivateMessageActivity).showMessage(null, 0)
            }

            R.id.refresh -> syncPMs()
            R.id.toggle_folder -> {
                currentFolder = if (currentFolder == FOLDER_INBOX) FOLDER_SENT else FOLDER_INBOX
                setActionBarTitle(getTitle())
                changeIcon(item)
                syncPMs()
            }

            R.id.settings -> awfulActivity?.navigate(NavigationEvent.Settings())
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

    private val onButtonClick: View.OnClickListener = View.OnClickListener { aView ->
        when (aView.id) {
            R.id.just_pm -> if (activity is PrivateMessageActivity) {
                (activity as PrivateMessageActivity).showMessage(null, 0)
            }

            R.id.new_pm -> startActivity(
                Intent().setClass(
                    requireActivity(),
                    MessageDisplayActivity::class.java
                )
            )

            R.id.refresh -> syncPMs()
        }
    }

    private val onPMSelected: OnItemClickListener =
        OnItemClickListener { aParent, aView, aPosition, aId ->
            if (activity is PrivateMessageActivity) {
                (activity as PrivateMessageActivity).showMessage(null, aId.toInt())
            } else {
                startActivity(
                    Intent(activity, MessageDisplayActivity::class.java).putExtra(
                        Constants.PARAM_PRIVATE_MESSAGE_ID, aId.toInt()
                    )
                )
            }
        }

    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        super.onPreferenceChange(prefs, key)
        if ("no_fab" == key) {
            mFAB?.visibility = (if (prefs.noFAB) View.GONE else View.VISIBLE)
            invalidateOptionsMenu()
        }
    }

    private inner class PMIndexCallback(handler: Handler?) : ContentObserver(handler),
        LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(aId: Int, aArgs: Bundle?): Loader<Cursor?> {
            tag(Companion.TAG).i("Load PM Cursor.")
            return CursorLoader(
                requireActivity(),
                AwfulMessage.CONTENT_URI,
                AwfulProvider.PMProjection,
                AwfulMessage.FOLDER + "=?",
                AwfulProvider.int2StrArray(currentFolder),
                AwfulMessage.ID + " DESC"
            )
        }

        override fun onLoadFinished(aLoader: Loader<Cursor>, aData: Cursor) {
            if (aData != null) {
                tag(Companion.TAG).v("PM load finished, populating: %s", aData.count)
            }
            mCursorAdapter?.swapCursor(aData)
        }

        override fun onLoaderReset(aLoader: Loader<Cursor>) {
            mCursorAdapter?.swapCursor(null)
        }

        override fun onChange(selfChange: Boolean) {
            tag(Companion.TAG).i("PM Data update.")
            restartLoader(Constants.PRIVATE_MESSAGE_THREAD, null, this)
        }
    }


    override fun getTitle(): String {
        return when (currentFolder) {
            FOLDER_INBOX -> "Inbox"
            FOLDER_SENT -> "Sent"
            else -> "Messages"
        }
    }

    fun loadAllMessages() {
        isAllMessages = true
        syncPMs()
    }
}
