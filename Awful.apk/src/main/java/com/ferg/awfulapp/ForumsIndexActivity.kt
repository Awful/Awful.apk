/**
 * *****************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 *
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
 * *****************************************************************************
 */

package com.ferg.awfulapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.view.KeyEvent
import android.view.ViewGroup
import com.ferg.awfulapp.announcements.AnnouncementsManager
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.dialog.ChangelogDialog
import com.ferg.awfulapp.dialog.LogOutDialog
import com.ferg.awfulapp.messages.PmManager
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.Keys
import com.ferg.awfulapp.preferences.SettingsActivity
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.sync.SyncManager
import com.ferg.awfulapp.thread.AwfulURL
import com.ferg.awfulapp.util.AwfulUtils
import com.ferg.awfulapp.widget.ToggleViewPager
import timber.log.Timber
import java.util.*


class ForumsIndexActivity : AwfulActivity(), PmManager.Listener, AnnouncementsManager.AnnouncementListener {
    private var mIndexFragment: ForumsIndexFragment? = null

    private var forumFragment: ForumDisplayFragment? = null
    private var threadFragment: ThreadDisplayFragment? = null
    private var skipLoad = false
    private var isTablet: Boolean = false
    private var url = AwfulURL()

    lateinit private var toolbar: Toolbar
    lateinit private var viewPager: ToggleViewPager
    lateinit private var pagerAdapter: ForumPagerAdapter
    lateinit private var navigationDrawer: NavigationDrawer
    lateinit private var fullscreenCoordinator: FullscreenCoordinator

    @Volatile private var forumID = Constants.USERCP_ID
    @Volatile private var forumPage = 1
    @Volatile var threadId = NULL_THREAD_ID
    @Volatile var threadPage = 1
    private var threadPost = ""


    public override fun onCreate(savedInstanceState: Bundle?) {
        Timber.v("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forum_index_activity)

        viewPager = findViewById(R.id.forum_index_pager)
        toolbar = findViewById(R.id.awful_toolbar)
        navigationDrawer = NavigationDrawer(this, toolbar, mPrefs)
        fullscreenCoordinator = FullscreenCoordinator(this, mPrefs.immersionMode)
        setSupportActionBar(toolbar)

        isTablet = AwfulUtils.isTablet(this)
        val initialPage: Int
        if (savedInstanceState != null) {
            val forumId = savedInstanceState.getInt(Constants.FORUM_ID, forumID)
            val forumPage = savedInstanceState.getInt(Constants.FORUM_PAGE, forumPage)
            setForum(forumId, forumPage)

            val threadPage = savedInstanceState.getInt(Constants.THREAD_PAGE, 1)
            val threadId = savedInstanceState.getInt(Constants.THREAD_ID, NULL_THREAD_ID)
            setThread(threadId, threadPage)

            initialPage = savedInstanceState.getInt("viewPage", NULL_PAGE_ID)
        } else {
            initialPage = parseNewIntent(intent)
        }

        viewPager.setSwipeEnabled(!mPrefs.lockScrolling)
        if (!isTablet && AwfulUtils.isJellybean() && mPrefs.transformer != "Disabled") {
            viewPager.setPageTransformer(true, AwfulUtils.getViewPagerTransformer())
        }
        viewPager.offscreenPageLimit = 2
        if (isTablet) {
            viewPager.pageMargin = 1
            //TODO what color should it use here?
            viewPager.setPageMarginDrawable(ColorDrawable(ColorProvider.ACTION_BAR.color))
        }
        pagerAdapter = ForumPagerAdapter(supportFragmentManager)
        viewPager.adapter = pagerAdapter
        viewPager.addOnPageChangeListener(pagerAdapter)
        if (initialPage >= 0) {
            viewPager.currentItem = initialPage
        }

        checkIntentExtras()

        PmManager.registerListener(this)
        AnnouncementsManager.getInstance().registerListener(this)
    }

    override fun onNewPm(messageUrl: String, sender: String, unreadCount: Int) {
        // TODO: 16/08/2016 probably best to put this in a method that the menu option calls too
        val pmIntent = Intent().setClass(this, PrivateMessageActivity::class.java)
        Uri.parse(messageUrl)?.let {
            pmIntent.data = it
        }
        runOnUiThread {
            val message = "Private message from %s\n(%d unread)"
            Snackbar.make(toolbar, String.format(Locale.getDefault(), message, sender, unreadCount), Snackbar.LENGTH_LONG)
                    .setAction("View") { startActivity(pmIntent) }
                    .show()
        }
    }

    override fun onAnnouncementsUpdated(newCount: Int, oldUnread: Int, oldRead: Int, isFirstUpdate: Boolean) {
        if (isFirstUpdate || newCount > 0) {
            val res = resources
            // only show one of 'new announcements' or 'unread announcements', ignoring read ones
            // (only notify about unread for the first update after opening the app, to remind the user)
            if (newCount > 0) {
                showAnnouncementSnackbar(res.getQuantityString(R.plurals.numberOfNewAnnouncements, newCount, newCount))
            } else if (oldUnread > 0) {
                showAnnouncementSnackbar(res.getQuantityString(R.plurals.numberOfOldUnreadAnnouncements, oldUnread, oldUnread))
            }
        }
    }

    private fun showAnnouncementSnackbar(message: String) {
        Snackbar.make(toolbar, message, Snackbar.LENGTH_LONG)
                .setAction("View") { AnnouncementsManager.getInstance().showAnnouncements(this) }
                .show()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        navigationDrawer.drawerToggle.syncState()
    }


    ///////////////////////////////////////////////////////////////////////////
    // Navigation events
    ///////////////////////////////////////////////////////////////////////////


    /** Display the user's bookmarks  */
    fun showBookmarks() {
        startActivity(Intent().setClass(this, ForumsIndexActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Constants.FORUM_ID, Constants.USERCP_ID)
                .putExtra(Constants.FORUM_PAGE, ForumDisplayFragment.FIRST_PAGE))
    }


    fun showLogout() {
        LogOutDialog(this).show()
    }

    /** Display the announcements  */
    fun showAnnouncements() {
        AnnouncementsManager.getInstance().showAnnouncements(this@ForumsIndexActivity)
    }

    /** Display the user's PMs  */
    fun showPrivateMessages() {
        startActivity(Intent().setClass(this, PrivateMessageActivity::class.java))
    }

    /** Display the forum search  */
    fun showSearch() {
        val intent = BasicActivity.intentFor(SearchFragment::class.java, this, getString(R.string.search_forums_activity_title))
        startActivity(intent)
    }

    /** Display the app settings  */
    fun showSettings() {
        startActivity(Intent().setClass(this, SettingsActivity::class.java))
    }


    /**
     * Page to the thread view. If it's not currently showing the expected thread, load the first page of it.
     *
     * @param expectedThreadId  the page that should be shown
     * @param parentForumId     the ID of the thread's parent forum
     */
    fun showThreadView(expectedThreadId: Int, parentForumId: Int) {
        if (threadFragment?.threadId != expectedThreadId) {
            displayThread(expectedThreadId, ThreadDisplayFragment.FIRST_PAGE, parentForumId, ForumDisplayFragment.FIRST_PAGE, true)
        } else {
            viewPager.currentItem = THREAD_VIEW_FRAGMENT_POSITION
        }
    }

    /**
     * Page to the forum/threadlist view. If it's not currently showing the expected forum, load the first page of it.
     * @param expectedForumId the forum that should be shown
     */
    fun showForumView(expectedForumId: Int) {
        if (forumFragment?.forumId != expectedForumId) {
            displayForum(expectedForumId, ForumDisplayFragment.FIRST_PAGE)
        } else {
            viewPager.currentItem = THREAD_LIST_FRAGMENT_POSITION
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.v("onNewIntent")
        setIntent(intent)
        val initialPage = parseNewIntent(intent)
        if (pagerAdapter.count >= initialPage && initialPage >= 0) {
            viewPager.currentItem = initialPage
        }
        forumFragment?.openForum(forumID, forumPage)

        threadFragment?.apply {
            if (url.isThread || url.isPost) {
                openThread(url)
            } else if (intent.getIntExtra(Constants.THREAD_ID, NULL_THREAD_ID) > 0) {
                Timber.e("else: $threadPost")
                openThread(threadId, threadPage, threadPost)
            }
        }
    }

    private fun parseNewIntent(intent: Intent): Int {
        var initialPage = NULL_PAGE_ID
        var forumId = getIntent().getIntExtra(Constants.FORUM_ID, forumID)
        var forumPage = getIntent().getIntExtra(Constants.FORUM_PAGE, forumPage)
        var threadId = getIntent().getIntExtra(Constants.THREAD_ID, this.threadId)
        var threadPage = getIntent().getIntExtra(Constants.THREAD_PAGE, this.threadPage)
        threadPost = getIntent().getStringExtra(Constants.THREAD_FRAGMENT) ?: ""

        if (forumId == 2) {//workaround for old userCP ID, ugh. the old id still appears if someone created a bookmark launch shortcut prior to b23
            forumId = Constants.USERCP_ID//should never have used 2 as a hard-coded forum-id, what a horror.
        }

        var displayIndex = false
        val scheme = getIntent().data?.scheme
        if ("http" == scheme || "https" == scheme) {
            url = AwfulURL.parse(getIntent().dataString)
            when (url.type) {
                AwfulURL.TYPE.FORUM -> {
                    forumId = url.id.toInt()
                    forumPage = url.page.toInt()
                }
                AwfulURL.TYPE.THREAD -> if (!url.isRedirect) {
                    threadPage = url.page.toInt()
                    threadId = url.id.toInt()
                }
                AwfulURL.TYPE.POST -> { }
                AwfulURL.TYPE.INDEX -> displayIndex = true
            }
        }

        setForum(forumId, forumPage)
        setThread(threadId, threadPage)

        if (displayIndex) displayForumIndex()
        if (intent.getIntExtra(Constants.FORUM_ID, 0) > 1 || url.isForum) {
            initialPage = if (isTablet) 0 else 1
        } else {
            skipLoad = !isTablet
        }
        if (intent.getIntExtra(Constants.THREAD_ID, NULL_THREAD_ID) > 0 || url.isRedirect || url.isThread) {
            initialPage = 2
        }
        return initialPage
    }

    override fun onResume() {
        super.onResume()

        val version = BuildConfig.VERSION_CODE

        // check if this is the first run, and if so show the 'welcome' dialog
        if (mPrefs.alertIDShown == 0) {
            AlertDialog.Builder(this).setTitle(getString(R.string.alert_title_1))
                    .setMessage(getString(R.string.alert_message_1))
                    .setPositiveButton(getString(R.string.alert_ok)) { dialog, _ -> dialog.dismiss() }
                    .setNegativeButton(getString(R.string.alert_settings)) { dialog, _ ->
                        dialog.dismiss()
                        showSettings()
                    }.show()
            mPrefs.setPreference(Keys.ALERT_ID_SHOWN, 1)
        } else if (mPrefs.lastVersionSeen != BuildConfig.VERSION_CODE) {
            Timber.i("App version changed from %d to %d - showing changelog", mPrefs.lastVersionSeen, version)
            ChangelogDialog.show(this)
            mPrefs.setPreference(Keys.LAST_VERSION_SEEN, version)
        }
    }

    override fun onPause() {
        super.onPause()
        forumFragment?.apply {
            setForum(forumId, page)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        fullscreenCoordinator.onFocusChange(hasFocus)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.apply {
            putInt(Constants.FORUM_ID, forumID)
            putInt(Constants.FORUM_PAGE, forumPage)
            putInt(Constants.THREAD_ID, threadId)
            putInt(Constants.THREAD_PAGE, threadPage)
            putInt("viewPage", viewPager.currentItem)
        }
    }


    private fun checkIntentExtras() {
        if (intent.hasExtra(Constants.SHORTCUT)) {
            if (intent.getBooleanExtra(Constants.SHORTCUT, false)) {
                displayForum(Constants.USERCP_ID, 1)
            }
        }
    }


    @Synchronized
    private fun setForum(forumId: Int, page: Int) {
        this.forumID = forumId
        forumPage = page
        setNavIds(this.forumID, null)
    }


    @Synchronized
    fun setThread(threadId: Int?, page: Int?) {
        if (page != null) {
            threadPage = page
        }
        threadId?.let {
            val oldThreadId = this.threadId
            this.threadId = threadId
            if ((oldThreadId < 1 || threadId < 1) && threadId != oldThreadId) {
                pagerAdapter.notifyDataSetChanged()//notify pager adapter so it'll show/hide the thread view
            }
        }
        setNavIds(Constants.USERCP_ID, this.threadId)
    }


    override fun onBackPressed() {
        if (navigationDrawer.close()) return
        if (viewPager.currentItem > 0) {
            if (!(pagerAdapter.getItem(viewPager.currentItem) as AwfulFragment).onBackPressed()) {
                viewPager.currentItem = viewPager.currentItem - 1
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun displayForum(id: Int, page: Int) {
        Timber.i("displayForum $id")
        setForum(id, page)
        setNavIds(id, null)
        forumFragment?.let { frag ->
            frag.openForum(id, page)
            viewPager.currentItem = pagerAdapter.getItemPosition(frag)
        }
    }


    override fun isFragmentVisible(awfulFragment: AwfulFragment?): Boolean {
        if(awfulFragment == null) return false
        if (isTablet) {
            val itemPos = pagerAdapter.getItemPosition(awfulFragment)
            return itemPos == viewPager.currentItem || itemPos == viewPager.currentItem + 1
        } else {
            return pagerAdapter.getItemPosition(awfulFragment) == viewPager.currentItem
        }
    }


    override fun displayThread(id: Int, page: Int, forumId: Int, forumPg: Int, forceReload: Boolean) {
        Timber.i("displayThread $id $forumId")

        threadFragment?.let { threadFrag ->
            if (!forceReload && threadId == id && threadPage == page) {
                setNavIds(threadFrag.parentForumId, NULL_THREAD_ID)
            } else {
                threadFragment?.openThread(id, page, null)
                viewPager.adapter?.notifyDataSetChanged()
            }
            viewPager.currentItem = pagerAdapter.getItemPosition(threadFrag)
        } ?: setThread(id, page)
    }


    override fun displayUserCP() {
        displayForum(Constants.USERCP_ID, 1)
    }

    override fun displayForumIndex() {
        viewPager.currentItem = 0
    }


    override fun onActivityResult(request: Int, result: Int, intent: Intent?) {
        Timber.i("onActivityResult: $request result: $result")
        super.onActivityResult(request, result, intent)
        if (request == Constants.LOGIN_ACTIVITY_REQUEST && result == Activity.RESULT_OK) {
            SyncManager.sync(this)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val pagerItem = pagerAdapter.getItem(viewPager.currentItem) as AwfulFragment?
        return if (mPrefs.volumeScroll && pagerItem != null && pagerItem.attemptVolumeScroll(event)) true
        else super.dispatchKeyEvent(event)
    }

    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        super.onPreferenceChange(prefs, key)
        viewPager.setSwipeEnabled(!prefs.lockScrolling)
        if (!isTablet && AwfulUtils.isJellybean() && prefs.transformer != "Disabled") {
            viewPager.setPageTransformer(true, AwfulUtils.getViewPagerTransformer())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val wasTab = isTablet
        isTablet = AwfulUtils.isTablet(this)
        if (wasTab != isTablet) {
            if (isTablet) {
                viewPager.pageMargin = 1
                //TODO what color should it use here?
                viewPager.setPageMarginDrawable(ColorDrawable(ColorProvider.ACTION_BAR.color))
            } else {
                viewPager.pageMargin = 0
            }
        }
        viewPager.adapter?.getItem(viewPager.currentItem)?.onConfigurationChanged(newConfig)
        navigationDrawer.drawerToggle.onConfigurationChanged(newConfig)
    }


    /**
     * Set the IDs that the navigation view knows about?
     * Updates the navigation menu to reflect these
     * @param forumId   The current forum
     * @param threadId
     */
    @Synchronized
    fun setNavIds(forumId: Int, threadId: Int?) {
        navigationDrawer.setCurrentForumAndThread(forumId, threadId)
    }

    fun preventSwipe() {
        this.viewPager.setSwipeEnabled(false)
    }

    fun reenableSwipe() {
        runOnUiThread {
            if (viewPager.beginFakeDrag()) {
                viewPager.endFakeDrag()
            }
            viewPager.setSwipeEnabled(true)
        }
    }

    inner class ForumPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm), ViewPager.OnPageChangeListener {

        private var visible: AwfulFragment? = null

        override fun onPageSelected(arg0: Int) {
            Timber.i("onPageSelected: $arg0")
            visible?.onPageHidden()
            val apf = instantiateItem(viewPager, arg0) as AwfulFragment
            // I don't know if #isAdded is necessary after calling #instantiateItem (instead of #getItem
            // which just creates a new fragment object), but I'm trying to fix a bug I can't reproduce
            // where these fragment methods crash because they have no activity yet
            if (apf.isAdded) {
                setActionbarTitle(apf.getTitle(), null)
                apf.onPageVisible()
                setProgress(apf.progressPercent)
            }
            visible = apf
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
        override fun onPageScrollStateChanged(state: Int) {}

        override fun getItem(position: Int): Fragment? {
            return when (position) {
                FORUM_LIST_FRAGMENT_POSITION -> mIndexFragment ?: ForumsIndexFragment()
                THREAD_LIST_FRAGMENT_POSITION -> forumFragment ?: ForumDisplayFragment.getInstance(forumID, forumPage, skipLoad)
                THREAD_VIEW_FRAGMENT_POSITION -> threadFragment ?: ThreadDisplayFragment()
                else -> {
                    Timber.e("ERROR: asked for too many fragments in ForumPagerAdapter.getItem")
                    null
                }
            }
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val frag = super.instantiateItem(container, position)
            when (position) {
                FORUM_LIST_FRAGMENT_POSITION -> mIndexFragment = frag as ForumsIndexFragment
                THREAD_LIST_FRAGMENT_POSITION -> forumFragment = frag as ForumDisplayFragment
                THREAD_VIEW_FRAGMENT_POSITION -> threadFragment = frag as ThreadDisplayFragment
            }
            return frag
        }

        override fun getItemPosition(item: Any): Int {
            return when (item) {
                mIndexFragment -> FORUM_LIST_FRAGMENT_POSITION
                forumFragment -> THREAD_LIST_FRAGMENT_POSITION
                threadFragment -> THREAD_VIEW_FRAGMENT_POSITION
                else -> super.getItemPosition(item)
            }
        }

        override fun getPageWidth(position: Int): Float {
            if (isTablet) {
                when (position) {
                    FORUM_LIST_FRAGMENT_POSITION -> return 0.4f
                    THREAD_LIST_FRAGMENT_POSITION -> return 0.6f
                    THREAD_VIEW_FRAGMENT_POSITION -> return 1f
                }
            }
            return super.getPageWidth(position)
        }

        override fun getCount() =  if (threadId < 1) 2 else 3
    }

    companion object {
        val NULL_FORUM_ID = 0
        val NULL_THREAD_ID = 0
        private val NULL_PAGE_ID = -1

        private val FORUM_LIST_FRAGMENT_POSITION = 0
        private val THREAD_LIST_FRAGMENT_POSITION = 1
        private val THREAD_VIEW_FRAGMENT_POSITION = 2
    }
}
