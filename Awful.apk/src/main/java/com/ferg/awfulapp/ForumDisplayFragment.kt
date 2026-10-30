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
import android.database.Cursor
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.android.volley.VolleyError
import com.ferg.awfulapp.NavigationEvent.Bookmarks
import com.ferg.awfulapp.NavigationEvent.SearchForums
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.BooleanPreference
import com.ferg.awfulapp.preferences.StringSetPreference
import com.ferg.awfulapp.provider.AwfulProvider
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.provider.DatabaseHelper
import com.ferg.awfulapp.search.SearchFilter
import com.ferg.awfulapp.service.ThreadCursorAdapter
import com.ferg.awfulapp.task.AwfulRequest.AwfulResultCallback
import com.ferg.awfulapp.task.BookmarkColorRequest
import com.ferg.awfulapp.task.BookmarkRequest
import com.ferg.awfulapp.task.MarkUnreadRequest
import com.ferg.awfulapp.task.ThreadListRequest
import com.ferg.awfulapp.task.ThreadListRequest.Companion.REQUEST_TAG
import com.ferg.awfulapp.thread.AwfulForum
import com.ferg.awfulapp.thread.AwfulPagedItem
import com.ferg.awfulapp.thread.AwfulThread
import com.ferg.awfulapp.widget.MinMaxNumberPicker
import com.ferg.awfulapp.widget.PageBar
import com.ferg.awfulapp.widget.PageBar.PageBarCallbacks
import com.ferg.awfulapp.widget.PagePicker
import timber.log.Timber.Forest.d
import timber.log.Timber.Forest.i
import timber.log.Timber.Forest.w
import java.util.Locale
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.collections.MutableSet
import kotlin.collections.mutableSetOf
import kotlin.math.max
import kotlin.math.min

/**
 * Uses intent extras:
 * TYPE - STRING ID - DESCRIPTION
 * int - Constants.FORUM_ID - id number for the forum
 * int - Constants.FORUM_PAGE - page number to load
 * 
 * Can also handle an HTTP intent that refers to an SA forumdisplay.php? url.
 */
class ForumDisplayFragment : AwfulFragment(), NavigationEventHandler {


    companion object {
        const val KEY_FORUM_ID: String = "forum ID"
        const val KEY_PAGE_NUMBER: String = "page number"
        const val KEY_SKIP_LOAD: String = "skip load"
        const val NULL_FORUM_ID: Int = 0
        const val FIRST_PAGE: Int = 1
        fun getInstance(forumId: Int, pageNum: Int, skipLoad: Boolean): ForumDisplayFragment {
            val fragment = ForumDisplayFragment()
            fragment.forumId = forumId
            fragment.page = pageNum
            fragment.skipLoad = skipLoad
            return fragment
        }
    }
    private var mListView: ListView? = null

    private var mPageBar: PageBar? = null

    private var currentForumId = 0
    private var currentPage = 0

    private var lastPage: Int = FIRST_PAGE
    private var mTitle: String? = null
    private var skipLoad = false

    private var loadFailed = false

    private var lastRefresh: Long = 0

    private var mCursorAdapter: ThreadCursorAdapter? = null
    private val mForumLoaderCallback = ForumContentsCallback()
    private val mForumDataCallback = ForumDataCallback()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onCreateView(
        aInflater: LayoutInflater,
        aContainer: ViewGroup?,
        aSavedState: Bundle?
    ): View {
        val result = inflateView(R.layout.forum_display, aContainer, aInflater)
        mListView = result.findViewById(R.id.forum_list)

        // page bar
        mPageBar = result.findViewById(R.id.page_bar)
        mPageBar?.let {
            it.setListener(object : PageBarCallbacks {
                override fun onPageNavigation(nextPage: Boolean) {
                    goToPage(page + (if (nextPage) 1 else -1))
                }

                override fun onRefreshClicked() {
                    syncForum()
                }

                override fun onPageNumberClicked() {
                    selectForumPage()
                }
            })
            awfulActivity?.setPreferredFont(it.textView)
        }

        updatePageBar()

        refreshProbationBar()

        awfulActivity?.setPreferredFont(result)
        return result
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refreshLayout = view.findViewById(R.id.forum_swipe)
        refreshLayout?.onRefresh = { syncForum() }
    }

    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)

        if (aSavedState != null) {
            currentForumId = aSavedState.getInt(KEY_FORUM_ID)
            currentPage = aSavedState.getInt(KEY_PAGE_NUMBER)
            skipLoad = aSavedState.getBoolean(KEY_SKIP_LOAD)
            i(
                "restored state - forumID: %d, page %d, skipLoad: %b",
                currentForumId,
                currentPage,
                skipLoad
            )
        }

        mCursorAdapter = ThreadCursorAdapter(activity as AwfulActivity, null, this)
        mListView?.let {
            it.adapter = mCursorAdapter
            it.onItemClickListener = onThreadSelected
            // TODO: save and restore scroll position - probably need to do the listview trick (get top item, and scroll offset from that) and save it as a deferred value, i.e. on load if there's a scroll value pending, do it and clear it
            updateColors()
            registerForContextMenu(it)
        }
    }


    // TODO: pull this out as a shared method/widget in AwfulFragment
    fun updatePageBar() {
        mPageBar?.updatePagePosition(this.page, this.lastPage)
    }

    override fun onResume() {
        super.onResume()
        updateColors()
        if (skipLoad || !isVisible) {
            skipLoad = false //only skip the first time
        } else {
            syncForumsIfStale()
        }
        refreshInfo()
    }

    override fun setAsFocusedPage() {
        // TODO: find out how this relates to onResume / onStart , it's the same code
        // TODO: this can be called before the fragment's views have been inflated, e.g. bookmark widget -> viewpager#onPageSelected -> (create fragment) -> onPageVisible
        updateColors()
        syncForumsIfStale()
        refreshInfo()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        d(
            "onSaveInstanceState: saving instance state - forumId: %d, page: %d, skipLoad: %b",
            currentForumId,
            currentPage,
            skipLoad
        )
        outState.putInt(KEY_FORUM_ID, currentForumId)
        outState.putInt(KEY_PAGE_NUMBER, currentPage)
        outState.putBoolean(KEY_SKIP_LOAD, skipLoad)
    }

    override fun cancelNetworkRequests() {
        super.cancelNetworkRequests()
        NetworkUtils.cancelRequests(REQUEST_TAG)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.forum_display_fragment, menu)

        val postThread = menu.findItem(R.id.post_thread)
        postThread.isVisible = this.forumId != Constants.USERCP_ID
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.post_thread -> {
                displayPostThreadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun displayPostThreadDialog() {
        if (AwfulPreferences.getInstance().postWarningAccepted) {
            displayPostThreadDialog(this.forumId)
            return
        }
        val awfulAct = awfulActivity ?: return
        AlertDialog.Builder(awfulAct)
            .setIcon(R.drawable.ic_gavel_dark_24dp)
            .setTitle("Warning")
            .setMessage(R.string.post_warning)
            .setPositiveButton(
                "I accept"
            ) { _: DialogInterface?, _: Int ->
                displayPostThreadDialog(this.forumId)
                AwfulPreferences.getInstance()
                    .setPreference(BooleanPreference.POST_WARNING_ACCEPTED, true)
            }
            .setNegativeButton(
                "Nope"
            ) { dialog: DialogInterface?, _: Int -> dialog?.dismiss() }
            .setCancelable(false)
            .show()
    }

    override fun onCreateContextMenu(aMenu: ContextMenu, aView: View, aMenuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(aMenu, aView, aMenuInfo)
        if (aMenuInfo is AdapterContextMenuInfo) {
            val inflater = requireActivity().menuInflater
            val row = mCursorAdapter?.getRow(aMenuInfo.id)
            if (row != null && row.getInt(row.getColumnIndexOrThrow(AwfulThread.BOOKMARKED)) > -1) {
                inflater.inflate(R.menu.thread_longpress, aMenu)
                if (row.getInt(row.getColumnIndexOrThrow(AwfulThread.BOOKMARKED)) < 1 || !prefs.coloredBookmarks) {
                    val bookmarkColor = aMenu.findItem(R.id.thread_bookmark_color)
                    if (bookmarkColor != null) {
                        bookmarkColor.isEnabled = false
                        bookmarkColor.isVisible = false
                    }
                }
            }
        }
    }

    override fun onContextItemSelected(aItem: MenuItem): Boolean {
        val info = aItem.menuInfo as AdapterContextMenuInfo?
        val threadId = info!!.id.toInt()
        when (aItem.itemId) {
            R.id.first_page -> {
                viewThread(threadId, 1)
                return true
            }

            R.id.last_page -> {
                val lastPage = AwfulPagedItem.indexToPage(
                    mCursorAdapter!!.getInt(
                        threadId.toLong(),
                        AwfulThread.POSTCOUNT
                    ), prefs.postPerPage
                )
                viewThread(threadId, lastPage)
                return true
            }

            R.id.go_to_page -> {
                val maxPage = AwfulPagedItem.indexToPage(
                    mCursorAdapter!!.getInt(
                        threadId.toLong(),
                        AwfulThread.POSTCOUNT
                    ), prefs.postPerPage
                )
                selectThreadPage(threadId, maxPage)
                return true
            }

            R.id.mark_thread_unread -> {
                markUnread(threadId)
                return true
            }

            R.id.thread_bookmark -> {
                toggleThreadBookmark(
                    threadId,
                    (mCursorAdapter!!.getInt(threadId.toLong(), AwfulThread.BOOKMARKED) + 1) % 2 > 0
                )
                return true
            }

            R.id.thread_bookmark_color -> {
                toggleBookmarkColor(
                    threadId,
                    (mCursorAdapter!!.getInt(threadId.toLong(), AwfulThread.BOOKMARKED))
                )
                return true
            }

            R.id.search_thread -> {
                val threadFilter =
                    SearchFilter(SearchFilter.FilterType.ThreadId, threadId.toString())
                navigate(SearchForums(threadFilter))
                return true
            }

            R.id.copy_url_thread -> {
                copyUrl(threadId)
                return true
            }

            R.id.toggle_hidden_thread -> {
                toggleHiddenThread(threadId)
                refreshInfo()
                return true
            }
        }

        return false
    }

    /**
     * Toggles whether to display this thread or not
     * 
     * @param threadId  The thread ID to hide or show
     */
    private fun toggleHiddenThread(threadId: Int) {
        val hiddenThreadIds = prefs.getPreference(StringSetPreference.HIDDEN_THREAD_IDS, mutableSetOf())
        val newSet: MutableSet<String?> = hiddenThreadIds.toMutableSet() // not allowed to mutate original set
        val id = threadId.toString()
        if (!newSet.remove(id)) {
            newSet.add(id)
        }
        prefs.setPreference(StringSetPreference.HIDDEN_THREAD_IDS, newSet)
    }

    /**
     * Show the dialog to open a thread at a specific page.
     * 
     * @param threadId The ID of the thread to open
     * @param maxPage   The last page of the thread
     */
    private fun selectThreadPage(threadId: Int, maxPage: Int) {
        // TODO: this would be better if it got the thread's last page itself
        PagePicker(
            requireActivity(),
            maxPage,
            maxPage,
            object : MinMaxNumberPicker.ResultListener {
                override fun onButtonPressed(button: Int, resultValue: Int) {
                    if (button == DialogInterface.BUTTON_POSITIVE) {
                        viewThread(threadId, resultValue)
                    }
                }
            }).show()
    }

    private fun selectForumPage() {
        PagePicker(
            requireActivity(),
            this.lastPage,
            this.page, object : MinMaxNumberPicker.ResultListener {
                override fun onButtonPressed(button: Int, resultValue: Int) {
                    if (button == DialogInterface.BUTTON_POSITIVE) {
                        goToPage(resultValue)
                    }
                }
            }).show()
    }

    private fun viewThread(id: Int, page: Int) {
        navigate(NavigationEvent.Thread(id, page, null))
    }

    private fun copyUrl(id: Int) {
        val clipLabel = String.format(Locale.US, "Thread #%d", id)
        val clipText = Constants.FUNCTION_THREAD + "?" + Constants.PARAM_THREAD_ID + "=" + id
        safeCopyToClipboard(clipLabel, clipText, R.string.copy_url_success)
    }


    private val onThreadSelected: OnItemClickListener = object : OnItemClickListener {
        override fun onItemClick(
            aParent: AdapterView<*>?,
            aView: View?,
            aPosition: Int,
            aId: Long
        ) {
            // TODO: 04/06/2017 why is all this in a thread list click listener? We know it's a thread! It's not a forum!
            val row = mCursorAdapter?.getRow(aId)
            if (row != null && row.getColumnIndexOrThrow(AwfulThread.BOOKMARKED) > -1) {
                i("Thread ID: %s", aId)
                if (prefs.hiddenThreadIds?.contains(aId.toString()) == true) {
                    return
                }
                val unreadPage = AwfulPagedItem.getLastReadPage(
                    row.getInt(row.getColumnIndexOrThrow(AwfulThread.UNREADCOUNT)),
                    row.getInt(row.getColumnIndexOrThrow(AwfulThread.POSTCOUNT)),
                    prefs.postPerPage,
                    row.getInt(row.getColumnIndexOrThrow(AwfulThread.HAS_VIEWED_THREAD))
                )
                viewThread(aId.toInt(), unreadPage)
            } else if (row != null && row.getColumnIndex(AwfulForum.PARENT_ID) > -1) {
                navigate(NavigationEvent.Forum(aId.toInt(), null))
            }
        }
    }

    override fun onPreferenceChange(preferences: AwfulPreferences, key: String?) {
        super.onPreferenceChange(preferences, key)
        mPageBar?.let{
            awfulActivity?.setPreferredFont(it.textView)
        }
        updateColors()
        mListView?.invalidate()
        mListView?.invalidateViews()
    }

    var forumId: Int
        get() = currentForumId
        /**
         * Set the current Forum ID.
         *
         * Falls back to the Bookmarks forum for invalid ID values
         * @param forumId   the ID to switch to
         */
        private set(forumId) {
            currentForumId =
                if (forumId < 1) Constants.USERCP_ID else forumId
        }


    var page: Int
        get() = currentPage
        /**
         * Set the current page number.
         *
         * This will be bound to the valid range (between the first and last page).
         */
        private set(pageNumber) {
            currentPage = max(
                FIRST_PAGE,
                min(pageNumber, this.lastPage)
            )
        }


    private fun goToPage(pageNumber: Int) {
        this.page = pageNumber
        updatePageBar()
        refreshProbationBar()
        // interrupt any scrolling animation and jump to the top of the page
        mListView?.smoothScrollBy(0, 0)
        mListView?.setSelection(0)
        // display the chosen page (may be cached), then update its contents
        refreshInfo()
        syncForum()
    }


    override fun handleNavigation(event: NavigationEvent): Boolean {
        if (event is Bookmarks) {
            openForum(Constants.USERCP_ID, null)
            return true
        } else if (event is NavigationEvent.Forum) {
            openForum(event.id, event.page)
            return true
        }
        return false
    }


    private fun openForum(id: Int, page: Int?) {
        // do nothing if we're already looking at this page (or if no page specified)
        if (id == currentForumId && (page == null || page == currentPage)) {
            return
        }
        this.forumId = id
        this.page = page ?: FIRST_PAGE
        updateColors()
        this.lastPage = 0
        lastRefresh = 0
        loadFailed = false
        if (activity != null) {
            (activity as ForumsIndexActivity).onPageContentChanged()
        }
        invalidateOptionsMenu()
        refreshInfo()
        syncForum()
    }

    fun syncForum() {
        if (activity != null && this.forumId > 0) {
            // cancel pending thread list loading requests
            NetworkUtils.cancelRequests(REQUEST_TAG)
            // call this with cancelOnDestroy=false to retain the request's specific type tag
            queueRequest(
                ThreadListRequest(requireActivity(), this.forumId, this.page).build(
                    this,
                    object : AwfulResultCallback<Void?> {
                        override fun success(result: Void?) {
                            lastRefresh = System.currentTimeMillis()
                            loadFailed = false
                            refreshLayout?.finishedLoading()
                            refreshInfo()
                            mListView?.setSelectionAfterHeaderView()
                        }

                        override fun failure(error: VolleyError?) {
                            CaptchaActivity.handleCaptchaChallenge(requireActivity(), error!!)
                            w("Failed to sync thread list!")
                            refreshInfo()
                            lastRefresh = System.currentTimeMillis()
                            loadFailed = true
                            mListView?.setSelectionAfterHeaderView()
                        }
                    }
                ), false)
        }
    }

    fun syncForumsIfStale() {
        val currentTime = System.currentTimeMillis() - (1000 * 60 * 5)
        if (!loadFailed && lastRefresh < currentTime) {
            syncForum()
        }
    }

    private fun markUnread(id: Int) {
        queueRequest(
            MarkUnreadRequest(requireActivity(), id).build(
                this,
                object : AwfulResultCallback<Void?> {
                    override fun success(result: Void?) {
                        alertView.setTitle(R.string.mark_unread_success)
                            .setIcon(R.drawable.ic_check_circle)
                            .show()
                        refreshInfo()
                    }

                    override fun failure(error: VolleyError?) {
                        refreshInfo()
                    }
                })
        )
    }

    // TODO: move the bookmark toggle/cycle code into these methods and out of the menu handler
    /** Set Bookmark status.
     * @param id Thread ID
     * @param add true to add bookmark, false to remove.
     */
    private fun toggleThreadBookmark(id: Int, add: Boolean) {
        queueRequest(
            BookmarkRequest(requireActivity(), id, add).build(
                this,
                object : AwfulResultCallback<Void?> {
                    override fun success(result: Void?) {
                        refreshInfo()
                    }

                    override fun failure(error: VolleyError?) {
                        refreshInfo()
                    }
                })
        )
    }

    /** Toggle Bookmark color status.
     * @param id Thread ID
     */
    private fun toggleBookmarkColor(id: Int, bookmarkStatus: Int) {
        val cr = awfulApplication?.contentResolver
        if (bookmarkStatus == 6) {
            queueRequest(
                BookmarkColorRequest(requireActivity(), id).build(
                    this,
                    object : AwfulResultCallback<Void?> {
                        override fun success(result: Void?) {}

                        override fun failure(error: VolleyError?) {}
                    })
            )
        }
        queueRequest(
            BookmarkColorRequest(requireActivity(), id).build(
                this,
                object : AwfulResultCallback<Void?> {
                    override fun success(result: Void?) {
                        val cv = ContentValues()
                        cv.put(
                            AwfulThread.BOOKMARKED,
                            (if (bookmarkStatus == 6) bookmarkStatus + 2 else bookmarkStatus + 1) % 7
                        )
                        cr?.update(
                            AwfulThread.CONTENT_URI,
                            cv,
                            AwfulThread.ID + "=?",
                            AwfulProvider.int2StrArray(id)
                        )
                        refreshInfo()
                    }

                    override fun failure(error: VolleyError?) {
                        refreshInfo()
                    }
                })
        )
    }

    private inner class ForumContentsCallback : LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(aId: Int, aArgs: Bundle?): Loader<Cursor?> {
            i("Creating forum cursor: %s", this@ForumDisplayFragment.forumId)
            // TODO: move this query code into a provider class
            val isBookmarks = (this@ForumDisplayFragment.forumId == Constants.USERCP_ID)
            val thisPageIndex = AwfulPagedItem.forumPageToIndex(this@ForumDisplayFragment.page)
            val nextPageIndex = AwfulPagedItem.forumPageToIndex(this@ForumDisplayFragment.page + 1)

            // set up some cursor query stuff, depending on whether this is a normal forum or the bookmarks one
            val contentUri = if (isBookmarks) AwfulThread.CONTENT_URI_UCP else AwfulThread.CONTENT_URI
            var selection = if (isBookmarks) {
                String.format(
                    "%s.%s>=? AND %s.%s<?",
                    DatabaseHelper.TABLE_UCP_THREADS,
                    AwfulThread.INDEX,
                    DatabaseHelper.TABLE_UCP_THREADS,
                    AwfulThread.INDEX
                )
            } else {
                String.format(
                    "%s=? AND %s>=? AND %s<?",
                    AwfulThread.FORUM_ID, AwfulThread.INDEX, AwfulThread.INDEX
                )
            }

            if (!prefs.showHiddenThreads) {
                selection += String.format(
                    " AND %s NOT IN (%s)",
                    DatabaseHelper.TABLE_THREADS + "." + AwfulThread.ID,
                    prefs.hiddenThreadIds?.joinToString(",")
                )
            }
            val selectionArgs = if (isBookmarks) {
                AwfulProvider.int2StrArray(thisPageIndex, nextPageIndex)
            } else {
                AwfulProvider.int2StrArray(this@ForumDisplayFragment.forumId, thisPageIndex, nextPageIndex)
            }
            val sortNewFirst =
                (isBookmarks && prefs.newThreadsFirstUCP) || (!isBookmarks && prefs.newThreadsFirstForum)
            val sortOrder: String?
            if (sortNewFirst) {
                val secondarySort =
                    if (isBookmarks) AwfulThread.LAST_POST_DATE + " DESC" else AwfulThread.INDEX
                sortOrder = AwfulThread.HAS_NEW_POSTS + " DESC, " + secondarySort
            } else {
                sortOrder =
                    if (isBookmarks) AwfulThread.LAST_POST_DATE + " DESC" else AwfulThread.INDEX
            }

            return CursorLoader(
                requireActivity(),
                contentUri,
                AwfulProvider.ThreadProjection,
                selection,
                selectionArgs,
                sortOrder
            )
        }

        override fun onLoadFinished(aLoader: Loader<Cursor?>, aData: Cursor?) {
            i("Forum contents finished, populating")
            if (aData != null && !aData.isClosed && aData.moveToFirst()) {
                mCursorAdapter?.swapCursor(aData)
            } else {
                mCursorAdapter?.swapCursor(null)
            }
        }

        override fun onLoaderReset(arg0: Loader<Cursor?>) {
            i("ForumContentsCallback - onLoaderReset")
            mCursorAdapter?.swapCursor(null)
        }
    }


    private inner class ForumDataCallback : LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(aId: Int, aArgs: Bundle?): Loader<Cursor?> {
            i("Creating forum title cursor: %s", this@ForumDisplayFragment.forumId)
            return CursorLoader(
                requireActivity(),
                ContentUris.withAppendedId(AwfulForum.CONTENT_URI, this@ForumDisplayFragment.forumId.toLong()),
                AwfulProvider.ForumProjection,
                null,
                null,
                null
            )
        }

        override fun onLoadFinished(aLoader: Loader<Cursor?>, aData: Cursor?) {
            if (aData != null && !aData.isClosed && aData.moveToFirst()) {
                i("Forum title finished, populating: %s", aData.count)
                mTitle = aData.getString(aData.getColumnIndexOrThrow(AwfulForum.TITLE))
                this@ForumDisplayFragment.lastPage = aData.getInt(aData.getColumnIndexOrThrow(AwfulForum.PAGE_COUNT))
                val activity = (activity as ForumsIndexActivity?)
                activity?.onPageContentChanged()
            }

            updatePageBar()
            refreshProbationBar()
        }

        override fun onLoaderReset(aLoader: Loader<Cursor?>) {
        }
    }

    private fun refreshInfo() {
        if (activity != null) {
            restartLoader(Constants.FORUM_THREADS_LOADER_ID, null, mForumLoaderCallback)
            restartLoader(Constants.FORUM_LOADER_ID, null, mForumDataCallback)
        }
    }


    override fun getTitle(): String? {
        return mTitle
    }


    override fun doScroll(down: Boolean): Boolean {
        val list = mListView ?: return false
        val scrollAmount = list.height / 2
        list.smoothScrollBy((if (down) scrollAmount else -scrollAmount), 400)
        return true
    }


    private fun updateColors() {
        mPageBar?.setTextColour(ColorProvider.ACTION_BAR_TEXT.color)
        val backgroundColor = ColorProvider.BACKGROUND.getColor(currentForumId)
        mListView?.setBackgroundColor(backgroundColor)
        mListView?.cacheColorHint = backgroundColor
        refreshLayout?.spinnerTint = ColorProvider.getARLActiveColor(currentForumId)
        refreshLayout?.spinnerTintInert = ColorProvider.getARLInertColor(currentForumId)
    }
}
