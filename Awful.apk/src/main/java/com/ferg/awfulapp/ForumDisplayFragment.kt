/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
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

import android.content.ContentUris
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.database.ContentObserver
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ListView
import com.android.volley.VolleyError
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.constants.Constants.USERCP_ID
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.AwfulProvider
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.provider.DatabaseHelper
import com.ferg.awfulapp.service.ThreadCursorAdapter
import com.ferg.awfulapp.task.*
import com.ferg.awfulapp.thread.AwfulForum
import com.ferg.awfulapp.thread.AwfulPagedItem
import com.ferg.awfulapp.thread.AwfulThread
import com.ferg.awfulapp.thread.AwfulURL
import com.ferg.awfulapp.thread.AwfulURL.TYPE
import com.ferg.awfulapp.widget.MinMaxNumberPicker
import com.ferg.awfulapp.widget.PageBar
import com.ferg.awfulapp.widget.PagePicker
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection
import timber.log.Timber
import java.util.*

/**
 * Uses intent extras:
 * TYPE - STRING ID - DESCRIPTION
 * int - Constants.FORUM_ID - id number for the forum
 * int - Constants.FORUM_PAGE - page number to load
 *
 * Can also handle an HTTP intent that refers to an SA forumdisplay.php? url.
 */
class ForumDisplayFragment : AwfulFragment(), SwipyRefreshLayout.OnRefreshListener {

    private var mListView: ListView? = null
    private var mPageBar: PageBar? = null

    private var lastPage = 1
    private var mTitle: String? = null
    private var skipLoad = false

    private var loadFailed = false

    private var lastRefresh: Long = 0

    lateinit private var mCursorAdapter: ThreadCursorAdapter
    private val mForumLoaderCallback: ForumContentsCallback by lazy { ForumContentsCallback(mHandler) }
    private val mForumDataCallback: ForumDataCallback by lazy { ForumDataCallback(mHandler) }

    /**
     * Set the current Forum ID.
     * Falls back to the Bookmarks forum for invalid ID values
     * @param forumId   the ID to switch to
     */
    var forumId: Int = 0
        private set(forumId) {
            field = if (forumId < 1) USERCP_ID else forumId
        }

    /**
     * Set the current page number.
     * This will be bound to the valid range (between the first and last page).
     */
    var page = 1
        private set(pageNumber) {
            field = Math.max(FIRST_PAGE, Math.min(pageNumber, lastPage))
        }


    private val onThreadSelected = AdapterView.OnItemClickListener { aParent, aView, aPosition, aId ->
        // TODO: 04/06/2017 why is all this in a threadlist click listener? We know it's a thread! It's not a forum!
        val row = mCursorAdapter?.getRow(aId)
        if (row != null && row.getColumnIndex(AwfulThread.BOOKMARKED) > -1) {
           Timber.i("Thread ID: " + java.lang.Long.toString(aId))
            val unreadPage = AwfulPagedItem.getLastReadPage(row.getInt(row.getColumnIndex(AwfulThread.UNREADCOUNT)),
                    row.getInt(row.getColumnIndex(AwfulThread.POSTCOUNT)),
                    mPrefs.postPerPage,
                    row.getInt(row.getColumnIndex(AwfulThread.HAS_VIEWED_THREAD)))
            viewThread(aId.toInt(), unreadPage)
        } else if (row != null && row.getColumnIndex(AwfulForum.PARENT_ID) > -1) {
            displayForumContents(aId.toInt())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = false
        setHasOptionsMenu(true)

        arguments?.let { args ->
            forumId = args.getInt(ARG_KEY_FORUM_ID)
            page = args.getInt(ARG_KEY_PAGE_NUMBER)
            skipLoad = args.getBoolean(ARG_KEY_SKIP_LOAD)
            Timber.d(String.format("onCreate: set forumID to %d, set page to %d", forumId, page))
        }
    }


    override fun onCreateView(aInflater: LayoutInflater, aContainer: ViewGroup?, aSavedState: Bundle?): View? {
        val result = inflateView(R.layout.forum_display, aContainer, aInflater)
        mListView = result.findViewById<View>(R.id.forum_list) as ListView

        // page bar
        mPageBar = result.findViewById<View>(R.id.page_bar) as PageBar
        mPageBar?.setListener(object : PageBar.PageBarCallbacks {
            override fun onPageNavigation(nextPage: Boolean) {
                goToPage(page + if (nextPage) 1 else -1)
            }

            override fun onRefreshClicked() {
                syncForum()
            }

            override fun onPageNumberClicked() {
                selectForumPage()
            }
        })
        awfulActivity?.setPreferredFont(mPageBar!!.textView)
        updatePageBar()
        refreshProbationBar()

        return result
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: move P2R stuff into AwfulFragment
        mSRL = view.findViewById(R.id.forum_swipe)
        mSRL?.setOnRefreshListener(this)
        mSRL?.setColorSchemeResources(*ColorProvider.getSRLProgressColors(null))
        mSRL?.setProgressBackgroundColor(ColorProvider.getSRLBackgroundColor(null))
    }

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)

        /*
            What happens in here, as far as I can tell, in order of precedence
            - Use saved instance state
            - If current forum is 1+, do nothing (it's assuming data has been set?)
            - IF CURRENT FORUM is < 1:
            -  use ACTIVITY's INTENT's url data if it exists and it's for a FORUM
            -  use BUNDLED INTENT's forum ID and page (assumed to exist)
            -  use ACTIVITY's INTENT and get the forum ID and page from that, defaulting to Bookmarks and/or page 1 if either are missing
         */

        // TODO: clean up whatever rube goldberg initialisation is happening here
        // I don't think any of this gets called? I can't see any saved state coming back in (it gets saved though),
        // and the forum ID and page are already set to valid values (>0) in onCreate - fragment is NOT being retained

        if (aSavedState != null) {
            // if there's saved state, load that
            Timber.i("Restoring savedInstanceState!")
            forumId = aSavedState.getInt(Constants.FORUM_ID, forumId)
            page = aSavedState.getInt(Constants.FORUM_PAGE, FIRST_PAGE)
        } else if (forumId < 1) {
            Timber.d("onActivityCreated: mForumId is less than 1 ($forumId), messing with intents and bundles")
            // otherwise if forumID is uninitialised/invalid (which is never true now, it's always set and validated for values <1)
            val intent = activity!!.intent
            val forumIdFromIntent = intent.getIntExtra(Constants.FORUM_ID, forumId)
            forumId = forumIdFromIntent
            page = intent.getIntExtra(Constants.FORUM_PAGE, page)
            Timber.d(String.format("onActivityCreated: activity's intent args - got id: %b, got page: %b",
                    intent.hasExtra(Constants.FORUM_ID), intent.hasExtra(Constants.FORUM_PAGE)))
            // I think these are fallbacks if the following doesn't work?

            val args = arguments
            val urldata = intent.data

            // if the activity has a data URI, get that and set the forum and page... again
            if (urldata != null) {
                Timber.d("onActivityCreated: got URL data from ACTIVITY's intent")
                val aurl = AwfulURL.parse(intent.dataString)
                if (aurl.type == TYPE.FORUM) {
                    Timber.d("onActivityCreated: URL is for a forum, that's for us")
                    forumId = aurl.id.toInt()
                    page = aurl.page.toInt()
                } else {
                    Timber.d("onActivityCreated: URL was not for a forum, I guess we ignore it then")
                }
            } else if (args != null) {
                Timber.d("onActivityCreated: no URL data, but we have fragment arguments")
                // default to page 1 of bookmarks if we have no data URI and we have fragment args
                forumId = args.getInt(Constants.FORUM_ID, USERCP_ID)
                page = args.getInt(Constants.FORUM_PAGE, FIRST_PAGE)
                Timber.d("onActivityCreated: original fragment args - got id: %b, got page: %b",
                        args.get(Constants.FORUM_ID) != null, args.get(Constants.FORUM_PAGE) != null)
            }
        } else {
            Timber.d("onActivityCreated: got method call, but there was no savedInstanceState and forum ID was already set")
        }


        mCursorAdapter = ThreadCursorAdapter(activity as AwfulActivity?, null, this)
        mListView?.adapter = mCursorAdapter
        mListView?.onItemClickListener = onThreadSelected

        updateColors()
        registerForContextMenu(mListView!!)
    }


    // TODO: pull this out as a shared method/widget in AwfulFragment
    fun updatePageBar() {
        mPageBar?.updatePagePosition(page, lastPage)
    }

    override fun onResume() {
        super.onResume()
        updateColors()
        activity?.contentResolver?.registerContentObserver(AwfulForum.CONTENT_URI, true, mForumDataCallback)
        activity?.contentResolver?.registerContentObserver(AwfulThread.CONTENT_URI, true, mForumLoaderCallback)
        if (skipLoad || !isVisible) skipLoad = false // only skip the first time
        else syncForumsIfStale()
        refreshInfo()
    }

    override fun onPageVisible() {
        // TODO: find out how this relates to onResume / onStart , it's the same code
        // TODO: this can be called before the fragment's views have been inflated, e.g. bookmark widget -> viewpager#onPageSelected -> (create fragment) -> onPageVisible
        updateColors()
        syncForumsIfStale()
        refreshInfo()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Timber.d("onSaveInstanceState: saving instance state - forumId: %d, page: %d", forumId, page)
        outState.putInt(Constants.FORUM_PAGE, page)
        outState.putInt(Constants.FORUM_ID, forumId)
    }

    override fun cancelNetworkRequests() {
        super.cancelNetworkRequests()
        NetworkUtils.cancelRequests(ThreadListRequest.REQUEST_TAG)
    }

    override fun onStop() {
        super.onStop()
        // TODO: cancel network reqs?
        closeLoaders()
    }


    override fun onCreateContextMenu(aMenu: ContextMenu, aView: View, aMenuInfo: ContextMenuInfo) {
        super.onCreateContextMenu(aMenu, aView, aMenuInfo)
        if (aMenuInfo is AdapterContextMenuInfo) {
            val inflater = activity?.menuInflater
            val row = mCursorAdapter.getRow(aMenuInfo.id)
            if (row != null && row.getInt(row.getColumnIndex(AwfulThread.BOOKMARKED)) > -1) {
                inflater?.inflate(R.menu.thread_longpress, aMenu)
                if (row.getInt(row.getColumnIndex(AwfulThread.BOOKMARKED)) < 1 || !mPrefs.coloredBookmarks) {
                    aMenu.findItem(R.id.thread_bookmark_color)?.let { color ->
                        color.isEnabled = false
                        color.isVisible = false
                    }
                }
            }
        }
    }

    override fun onContextItemSelected(aItem: android.view.MenuItem): Boolean {
        val info = aItem.menuInfo as AdapterContextMenuInfo
        when (aItem.itemId) {
            R.id.first_page -> {
                viewThread(info.id.toInt(), 1)
                return true
            }
            R.id.last_page -> {
                val lastPage = AwfulPagedItem.indexToPage(mCursorAdapter.getInt(info.id, AwfulThread.POSTCOUNT), mPrefs.postPerPage)
                viewThread(info.id.toInt(), lastPage)
                return true
            }
            R.id.go_to_page -> {
                val maxPage = AwfulPagedItem.indexToPage(mCursorAdapter.getInt(info.id, AwfulThread.POSTCOUNT), mPrefs.postPerPage)
                selectThreadPage(info.id.toInt(), maxPage)
                return true
            }
            R.id.mark_thread_unread -> {
                markUnread(info.id.toInt())
                return true
            }
            R.id.thread_bookmark -> {
                toggleThreadBookmark(info.id.toInt(), (mCursorAdapter.getInt(info.id, AwfulThread.BOOKMARKED) + 1) % 2 > 0)
                return true
            }
            R.id.thread_bookmark_color -> {
                toggleBookmarkColor(info.id.toInt(), mCursorAdapter.getInt(info.id, AwfulThread.BOOKMARKED))
                return true
            }
            R.id.copy_url_thread -> {
                copyUrl(info.id.toInt())
                return true
            }
        }
        return false
    }


    /**
     * Show the dialog to open a thread at a specific page.
     *
     * @param threadId The ID of the thread to open
     * @param maxPage   The last page of the thread
     */
    private fun selectThreadPage(threadId: Int, maxPage: Int) {
        // TODO: this would be better if it got the thread's last page itself
        PagePicker(activity, maxPage, maxPage, MinMaxNumberPicker.ResultListener { button, resultValue ->
            if (button == DialogInterface.BUTTON_POSITIVE) {
                viewThread(threadId, resultValue)
            }
        }).show()
    }

    private fun selectForumPage() {
        PagePicker(activity, lastPage, page, MinMaxNumberPicker.ResultListener { button, resultValue ->
            if (button == DialogInterface.BUTTON_POSITIVE) {
                goToPage(resultValue)
            }
        }).show()
    }

    private fun viewThread(id: Int, page: Int) {
        displayThread(id, page, forumId, page, true)
    }

    private fun copyUrl(id: Int) {
        val clipLabel = String.format(Locale.US, "Thread #%d", id)
        val clipText = Constants.FUNCTION_THREAD + "?" + Constants.PARAM_THREAD_ID + "=" + id
        safeCopyToClipboard(clipLabel, clipText, R.string.copy_url_success)
    }

    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        super.onPreferenceChange(mPrefs, key)
        if (mPageBar != null) {
            awfulActivity!!.setPreferredFont(mPageBar!!.textView)
        }
        updateColors()
        mListView?.invalidate()
        mListView?.invalidateViews()
    }


    private fun goToPage(pageNumber: Int) {
        page = pageNumber
        updatePageBar()
        refreshProbationBar()
        // interrupt any scrolling animation and jump to the top of the page
        mListView?.smoothScrollBy(0, 0)
        mListView?.setSelection(0)
        // display the chosen page (may be cached), then update its contents
        refreshInfo()
        syncForum()
    }

    fun openForum(id: Int, page: Int) {
        // do nothing if we're already looking at this page
        if (id == forumId && page == this.page) {
            return
        }
        closeLoaders()
        forumId = id
        this.page = page
        updateColors()
        lastPage = 0
        lastRefresh = 0
        loadFailed = false
        if (activity != null) {
            (activity as ForumsIndexActivity).setNavIds(forumId, null)
            activity?.invalidateOptionsMenu()
            refreshInfo()
            syncForum()
        }
    }

    fun syncForum() {
        if (activity != null && forumId > 0) {
            // cancel pending thread list loading requests
            NetworkUtils.cancelRequests(ThreadListRequest.REQUEST_TAG)
            // call this with cancelOnDestroy=false to retain the request's specific type tag
            queueRequest(ThreadListRequest(activity, forumId, page).build(this,
                    object : AwfulRequest.AwfulResultCallback<Void> {
                        override fun success(result: Void?) {
                            lastRefresh = System.currentTimeMillis()
                            // TODO: what does this even do
                            //                            mRefreshBar.setColorFilter(0);
                            //                            mToggleSidebar.setColorFilter(0);
                            loadFailed = false
                            refreshInfo()
                            mListView?.setSelectionAfterHeaderView()
                        }

                        override fun failure(error: VolleyError) {
                            if (error.message != null && error.message!!.startsWith("java.net.ProtocolException: Too many redirects")) {
                                Timber.e("Error: ${error.message} \nFailed to sync thread list - You are now LOGGED OUT")
                                NetworkUtils.clearLoginCookies(awfulActivity)
                                awfulActivity?.startActivity(Intent().setClass(awfulActivity, AwfulLoginActivity::class.java))
                            }
                            refreshInfo()
                            lastRefresh = System.currentTimeMillis()
                            loadFailed = true
                            mListView?.setSelectionAfterHeaderView()
                        }
                    }
            ), false)

        }
    }

    private fun syncForumsIfStale() {
        val currentTime = System.currentTimeMillis() - 1000 * 60 * 5
        if (!loadFailed && lastRefresh < currentTime) {
            syncForum()
        }
    }

    private fun markUnread(id: Int) {
        queueRequest(MarkUnreadRequest(activity, id).build(this, object : AwfulRequest.AwfulResultCallback<Void> {
            override fun success(result: Void) {
                AlertBuilder().setTitle(R.string.mark_unread_success)
                        .setIcon(R.drawable.ic_check_circle)
                        .show()
                refreshInfo()
            }

            override fun failure(error: VolleyError) {
                refreshInfo()
            }
        }))
    }

    // TODO: move the bookmark toggle/cycle code into these methods and out of the menu handler

    /** Set Bookmark status.
     * @param id Thread ID
     * @param add true to add bookmark, false to remove.
     */
    private fun toggleThreadBookmark(id: Int, add: Boolean) {
        queueRequest(BookmarkRequest(activity, id, add).build(this, object : AwfulRequest.AwfulResultCallback<Void> {
            override fun success(result: Void) {
                refreshInfo()
            }

            override fun failure(error: VolleyError) {
                refreshInfo()
            }
        }))
    }

    /** Toggle Bookmark color status.
     * @param id Thread ID
     */
    private fun toggleBookmarkColor(id: Int, bookmarkStatus: Int) {
        val cr = this.awfulApplication!!.contentResolver
        if (bookmarkStatus == 3) {
            queueRequest(BookmarkColorRequest(activity, id).build(this, object : AwfulRequest.AwfulResultCallback<Void> {

                override fun success(result: Void) {}

                override fun failure(error: VolleyError) {}
            }))
        }
        queueRequest(BookmarkColorRequest(activity, id).build(this, object : AwfulRequest.AwfulResultCallback<Void> {
            override fun success(result: Void) {
                val cv = ContentValues()
                cv.put(AwfulThread.BOOKMARKED, (if (bookmarkStatus == 3) bookmarkStatus + 2 else bookmarkStatus + 1) % 4)
                cr.update(AwfulThread.CONTENT_URI, cv, AwfulThread.ID + "=?", AwfulProvider.int2StrArray(id))
                refreshInfo()
            }

            override fun failure(error: VolleyError) {
                refreshInfo()
            }
        }))
    }

    override fun onRefresh(swipyRefreshLayoutDirection: SwipyRefreshLayoutDirection) {
        syncForum()
    }

    private inner class ForumContentsCallback(handler: Handler) : ContentObserver(handler), LoaderManager.LoaderCallbacks<Cursor> {

        override fun onCreateLoader(aId: Int, aArgs: Bundle?): Loader<Cursor> {
            Timber.d("Creating forum cursor: $forumId")
            // TODO: move this query code into a provider class
            val isBookmarks = forumId == USERCP_ID
            val thisPageIndex = AwfulPagedItem.forumPageToIndex(page)
            val nextPageIndex = AwfulPagedItem.forumPageToIndex(page + 1)

            // set up some cursor query stuff, depending on whether this is a normal forum or the bookmarks one
            val contentUri = if (isBookmarks) AwfulThread.CONTENT_URI_UCP else AwfulThread.CONTENT_URI

            val selection: String
            val selectionArgs: Array<String>
            if (isBookmarks) {
                selection = String.format("%s.%s>=? AND %s.%s<?",
                        DatabaseHelper.TABLE_UCP_THREADS, AwfulThread.INDEX, DatabaseHelper.TABLE_UCP_THREADS, AwfulThread.INDEX)
                selectionArgs = AwfulProvider.int2StrArray(thisPageIndex, nextPageIndex)
            } else {
                selection = String.format("%s=? AND %s>=? AND %s<?",
                        AwfulThread.FORUM_ID, AwfulThread.INDEX, AwfulThread.INDEX)
                selectionArgs = AwfulProvider.int2StrArray(forumId, thisPageIndex, nextPageIndex)
            }

            val sortNewFirst = isBookmarks && mPrefs.newThreadsFirstUCP || !isBookmarks && mPrefs.newThreadsFirstForum
            val sortOrder = if (sortNewFirst) AwfulThread.HAS_NEW_POSTS + " DESC, " + AwfulThread.INDEX else AwfulThread.INDEX

            return CursorLoader(activity!!, contentUri, AwfulProvider.ThreadProjection, selection, selectionArgs, sortOrder)
        }

        override fun onLoadFinished(aLoader: Loader<Cursor>, aData: Cursor?) {
            Timber.d("Forum contents finished, populating")
            if (aData != null && !aData.isClosed && aData.moveToFirst()) {
                mCursorAdapter.swapCursor(aData)
            } else {
                mCursorAdapter.swapCursor(null)
            }
        }

        override fun onLoaderReset(arg0: Loader<Cursor>) {
            Timber.d("ForumContentsCallback - onLoaderReset")
            mCursorAdapter.swapCursor(null)
        }
    }

    // TODO: fix race condition, see AwfulFragment#setTitle

    private inner class ForumDataCallback(handler: Handler) : ContentObserver(handler), LoaderManager.LoaderCallbacks<Cursor> {

        override fun onCreateLoader(aId: Int, aArgs: Bundle?): Loader<Cursor> {
            Timber.d("Creating forum title cursor: $forumId")
            return CursorLoader(activity!!, ContentUris.withAppendedId(AwfulForum.CONTENT_URI, forumId.toLong()),
                    AwfulProvider.ForumProjection, null, null, null)
        }

        override fun onLoadFinished(aLoader: Loader<Cursor>, aData: Cursor) {
            Timber.v("Forum title finished, populating: " + aData.count)
            if (!aData.isClosed && aData.moveToFirst()) {
                mTitle = aData.getString(aData.getColumnIndex(AwfulForum.TITLE))
                lastPage = aData.getInt(aData.getColumnIndex(AwfulForum.PAGE_COUNT))
                setTitle(mTitle!!)
            }

            updatePageBar()
            refreshProbationBar()
        }

        override fun onLoaderReset(aLoader: Loader<Cursor>) {}
        override fun onChange(selfChange: Boolean) {}
    }

    private fun refreshInfo() {
        if (activity != null) {
            restartLoader(Constants.FORUM_THREADS_LOADER_ID, null, mForumLoaderCallback)
            restartLoader(Constants.FORUM_LOADER_ID, null, mForumDataCallback)
        }
    }

    private fun closeLoaders() {
        //FIXME:
        try {
            if (activity != null) {
                loaderManager.destroyLoader(Constants.FORUM_THREADS_LOADER_ID)
                loaderManager.destroyLoader(Constants.FORUM_LOADER_ID)
            }
        } catch (npe: NullPointerException) {
            Timber.e(Arrays.toString(npe.stackTrace))
        }
    }

    public override fun getTitle() = mTitle

    override fun doScroll(down: Boolean): Boolean {
        val scrollAmount = (mListView?.height ?: 0) / 2
        mListView?.smoothScrollBy(if (down) scrollAmount else -scrollAmount, 400)
        return true
    }


    private fun updateColors() {
        mPageBar?.setTextColour(ColorProvider.ACTION_BAR_TEXT.color)
        if (mListView == null) return
        val backgroundColor = ColorProvider.BACKGROUND.getColor(forumId)
        mListView?.setBackgroundColor(backgroundColor)
        mListView?.cacheColorHint = backgroundColor
    }

    companion object {
        val ARG_KEY_FORUM_ID = "forum ID"
        val ARG_KEY_PAGE_NUMBER = "page number"
        val ARG_KEY_SKIP_LOAD = "skip load"
        val FIRST_PAGE = 1

        fun getInstance(forumId: Int, pageNum: Int, skipLoad: Boolean): ForumDisplayFragment {
            val fragment = ForumDisplayFragment()
            val args = Bundle()
            // TODO: should these use the Constants constants that saveInstanceState etc. uses?
            args.putInt(ARG_KEY_FORUM_ID, forumId)
            args.putInt(ARG_KEY_PAGE_NUMBER, pageNum)
            args.putBoolean(ARG_KEY_SKIP_LOAD, skipLoad)
            fragment.arguments = args
            return fragment
        }
    }
}
