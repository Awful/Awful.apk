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

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageLoader
import com.ferg.awfulapp.announcements.AnnouncementsManager
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.dialog.ChangelogDialog
import com.ferg.awfulapp.dialog.LogOutDialog
import com.ferg.awfulapp.messages.PmManager
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.Keys
import com.ferg.awfulapp.preferences.SettingsActivity
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.provider.StringProvider
import com.ferg.awfulapp.sync.SyncManager
import com.ferg.awfulapp.thread.AwfulURL
import com.ferg.awfulapp.util.AwfulUtils
import com.ferg.awfulapp.widget.ToggleViewPager
import timber.log.Timber
import java.util.*


class ForumsIndexActivity : AwfulActivity(), PmManager.Listener, AnnouncementsManager.AnnouncementListener {
    private var mIndexFragment: ForumsIndexFragment? = null

    private var mForumFragment: ForumDisplayFragment? = null
    private var mThreadFragment: ThreadDisplayFragment? = null
    private var skipLoad = false
    private var isTablet: Boolean = false
    private var url = AwfulURL()

    lateinit private var mViewPager: ToggleViewPager
    lateinit private var pagerAdapter: ForumPagerAdapter
    lateinit private var mToolbar: Toolbar
    lateinit private var mDrawerLayout: DrawerLayout
    lateinit private var mDrawerToggle: ActionBarDrawerToggle

    private var mDecorView: View? = null

    @Volatile private var mNavForumId = Constants.USERCP_ID
    @Volatile private var mNavThreadId = NULL_THREAD_ID
    @Volatile private var mForumId = Constants.USERCP_ID
    @Volatile private var mForumPage = 1
    @Volatile var threadId = NULL_THREAD_ID
    @Volatile var threadPage = 1

    private var mImmersionGestureDetector: GestureDetector? = null
    private var mIgnoreFling: Boolean = false
    private var mThreadPost = ""


    /** Listener for all the navigation drawer menu items  */
    private val navDrawerSelectionListener: NavigationView.OnNavigationItemSelectedListener

    private val mHideHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == MESSAGE_HIDING) {
                hideSystemUi()
            } else if (msg.what == MESSAGE_VISIBLE_CHANGE_IN_PROGRESS) {
                mIgnoreFling = false
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        Timber.v("onCreate")
        super.onCreate(savedInstanceState)
        isTablet = AwfulUtils.isTablet(this)
        val initialPage: Int
        if (savedInstanceState != null) {
            val forumId = savedInstanceState.getInt(Constants.FORUM_ID, mForumId)
            val forumPage = savedInstanceState.getInt(Constants.FORUM_PAGE, mForumPage)
            setForum(forumId, forumPage)

            val threadPage = savedInstanceState.getInt(Constants.THREAD_PAGE, 1)
            val threadId = savedInstanceState.getInt(Constants.THREAD_ID, NULL_THREAD_ID)
            setThread(threadId, threadPage)

            initialPage = savedInstanceState.getInt("viewPage", NULL_PAGE_ID)
        } else {
            initialPage = parseNewIntent(intent)
        }

        setContentView(R.layout.forum_index_activity)

        mViewPager = findViewById(R.id.forum_index_pager)
        mViewPager.setSwipeEnabled(!mPrefs.lockScrolling)
        if (!isTablet && AwfulUtils.isAtLeast(Build.VERSION_CODES.JELLY_BEAN_MR1) && mPrefs.transformer != "Disabled") {
            mViewPager.setPageTransformer(true, AwfulUtils.getViewPagerTransformer())
        }
        mViewPager.offscreenPageLimit = 2
        if (isTablet) {
            mViewPager.pageMargin = 1
            //TODO what color should it use here?
            mViewPager.setPageMarginDrawable(ColorDrawable(ColorProvider.ACTION_BAR.color))
        }
        pagerAdapter = ForumPagerAdapter(supportFragmentManager)
        mViewPager.adapter = pagerAdapter
        mViewPager.addOnPageChangeListener(pagerAdapter)
        if (initialPage >= 0) {
            mViewPager.currentItem = initialPage
        }

        mToolbar = findViewById(R.id.awful_toolbar)
        setSupportActionBar(mToolbar)
        setNavigationDrawer()
        setActionBar()
        checkIntentExtras()
        setupImmersion()

        PmManager.registerListener(this)
        AnnouncementsManager.getInstance().registerListener(this)
    }

    override fun onNewPm(messageUrl: String, sender: String, unreadCount: Int) {
        // TODO: 16/08/2016 probably best to put this in a method that the menu option calls too
        val pmIntent = Intent().setClass(this, PrivateMessageActivity::class.java)
        Uri.parse(messageUrl)?.let { uri ->
            pmIntent.data = uri
        }
        runOnUiThread {
            val message = "Private message from %s\n(%d unread)"
            Snackbar.make(mToolbar, String.format(Locale.getDefault(), message, sender, unreadCount), Snackbar.LENGTH_LONG)
                    .setDuration(3000)
                    .setAction("View") { startActivity(pmIntent) }
                    .show()
        }
    }

    override fun onAnnouncementsUpdated(newCount: Int, oldUnread: Int, oldRead: Int, isFirstUpdate: Boolean) {
        // update the nav drawer, in case it needs to reflect the new announcements
        // TODO: 27/01/2017 maybe the nav drawer should register itself as a listener, if it needs updates
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
        updateNavigationMenu()
    }

    private fun showAnnouncementSnackbar(message: String) {
        Snackbar.make(mToolbar, message, Snackbar.LENGTH_LONG)
                .setDuration(3000)
                .setAction("View") { AnnouncementsManager.getInstance().showAnnouncements(this) }
                .show()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState()
    }


    @SuppressLint("NewApi")
    private fun setupImmersion() {
        if (AwfulUtils.isKitKat() && mPrefs.immersionMode) {
            mDecorView = window.decorView

            mDecorView?.setOnSystemUiVisibilityChangeListener { flags ->
                val visible = flags and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0
                if (visible) {
                    // Add some delay so the act of swiping to bring system UI into view doesn't turn it back off
                    mHideHandler.removeMessages(MESSAGE_VISIBLE_CHANGE_IN_PROGRESS)
                    mHideHandler.sendEmptyMessageDelayed(MESSAGE_VISIBLE_CHANGE_IN_PROGRESS, 800)
                    mIgnoreFling = true
                }
            }

            mImmersionGestureDetector = GestureDetector(this,
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                            if (mIgnoreFling) return true

                            val visible = mDecorView!!.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0
                            if (visible) {
                                hideSystemUi()
                            }

                            return true
                        }
                    })
            showSystemUi()
        }
    }

    init {
        val context = this
        navDrawerSelectionListener = NavigationView.OnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sidebar_index -> displayForumIndex()
                R.id.sidebar_forum -> displayForum(mNavForumId, 1)
                R.id.sidebar_thread -> displayThread(mNavThreadId, threadPage, mNavForumId, mForumPage, false)
                R.id.sidebar_bookmarks -> startActivity(Intent().setClass(context, ForumsIndexActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(Constants.FORUM_ID, Constants.USERCP_ID)
                        .putExtra(Constants.FORUM_PAGE, 1))
                R.id.sidebar_settings -> startActivity(Intent().setClass(context, SettingsActivity::class.java))
                R.id.sidebar_search -> {
                    val intent = BasicActivity.intentFor(SearchFragment::class.java, context, getString(R.string.search_forums_activity_title))
                    startActivity(intent)
                }
                R.id.sidebar_pm -> startActivity(Intent().setClass(context, PrivateMessageActivity::class.java))
                R.id.sidebar_announcements -> AnnouncementsManager.getInstance().showAnnouncements(this@ForumsIndexActivity)
                R.id.sidebar_logout -> LogOutDialog(context).show()
                else -> return@OnNavigationItemSelectedListener false
            }
            mDrawerLayout.closeDrawer(Gravity.START)
            true
        }
    }


    /**
     * Initialise the navigation drawer
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setNavigationDrawer() {
        mDrawerLayout = findViewById(R.id.drawer_layout)
        val navigation: NavigationView = findViewById(R.id.navigation)
        navigation.setNavigationItemSelectedListener(navDrawerSelectionListener)

        mDrawerToggle = ActionBarDrawerToggle(
                this, /* host Activity */
                mDrawerLayout, /* DrawerLayout object */
                mToolbar, /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open, /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        )
        mDrawerLayout.addDrawerListener(mDrawerToggle)

        val nav = navigation.getHeaderView(0)

        // update the navigation drawer header
        val username: TextView = nav.findViewById(R.id.sidebar_username)
        username.text = mPrefs.username

        val avatar: ImageView = nav.findViewById(R.id.sidebar_avatar)
        if (mPrefs.userTitle != null) {
            if (mPrefs.userTitle.isNotEmpty()) {
                NetworkUtils.getImageLoader().get(mPrefs.userTitle, object : ImageLoader.ImageListener {
                    override fun onResponse(response: ImageLoader.ImageContainer, isImmediate: Boolean) {
                        avatar.setImageBitmap(response.bitmap)
                    }

                    override fun onErrorResponse(error: VolleyError) {
                        avatar.setImageResource(R.drawable.frog_icon)
                    }
                })
                if (AwfulUtils.isLollipop()) {
                    avatar.clipToOutline = true
                }
            } else {
                avatar.setImageResource(R.drawable.frog_icon)
                if (AwfulUtils.isLollipop()) {
                    avatar.clipToOutline = false
                }
            }
        }

        updateNavigationMenu()
        mDrawerToggle.syncState()
    }


    /**
     * Update any changeable menu items, e.g. current thread title
     * Synchronized because it gets called by other threads through
     * [.setNavIds]
     */
    @Synchronized private fun updateNavigationMenu() {
        // avoid premature update calls (e.g. through onCreate)
        val navView: NavigationView? = findViewById(R.id.navigation)
        val navMenu = navView?.menu
        val forumItem = navMenu?.findItem(R.id.sidebar_forum)
        val threadItem = navMenu?.findItem(R.id.sidebar_thread)

        // display the current forum title (or not)
        val showForumName = mNavForumId != NULL_FORUM_ID && mNavForumId != Constants.USERCP_ID
        forumItem?.isVisible = showForumName

        val showThreadName = mNavThreadId != NULL_THREAD_ID
        threadItem?.isVisible = showThreadName

        // update the forum and thread titles in the background, to avoid hitting the DB on the UI thread
        // to keep things simple, it also sets the text on anything we just hid, which will be placeholder text if the IDs are invalid
        updateNavMenuText(mNavForumId, mNavThreadId, forumItem, threadItem)

        // private messages - show 'em if you got 'em
        val pmItem = navMenu?.findItem(R.id.sidebar_pm)
        pmItem?.setEnabled(mPrefs.hasPlatinum)?.isVisible = mPrefs.hasPlatinum

        // private messages - show 'em if you got 'em
        val searchItem = navMenu?.findItem(R.id.sidebar_search)
        searchItem?.setEnabled(mPrefs.hasPlatinum)?.isVisible = mPrefs.hasPlatinum

        // show the unread announcement count
        val announcements = navMenu?.findItem(R.id.sidebar_announcements)
        val unread = AnnouncementsManager.getInstance().unreadCount
        announcements?.title = getString(R.string.announcements) + if (unread == 0) "" else " ($unread)"
    }


    /**
     * Update the nav menu's forum and thread titles in the background.
     *
     * This does some DB lookups and hurts the UI thread a lot, so let's async it for now
     * @param forumId       The ID of the forum to lookup
     * @param threadId      The ID of the thread to lookup
     * @param forumItem     An optional menu item to update with the forum name
     * @param threadItem    An optional menu item to update with the thread name
     */
    private fun updateNavMenuText(forumId: Int,
                                  threadId: Int,
                                  forumItem: MenuItem?,
                                  threadItem: MenuItem?) {
        val context = this
        // TODO: Get rid of disgusting async task
        object : AsyncTask<Void, Void, Void>() {

            private var forumName: String? = null
            private var threadName: String? = null

            override fun doInBackground(vararg params: Void): Void? {
                threadName = StringProvider.getThreadName(context, threadId)
                if (forumId >= 0) {
                    forumName = StringProvider.getForumName(context, forumId)
                }
                return null
            }

            override fun onPostExecute(aVoid: Void?) {
                forumItem?.title = forumName
                threadItem?.title = threadName
            }
        }.execute()
    }


    @SuppressLint("NewApi")
    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        if (AwfulUtils.isKitKat() && mPrefs.immersionMode) {
            super.dispatchTouchEvent(e)
            return mImmersionGestureDetector!!.onTouchEvent(e)
        }

        return super.dispatchTouchEvent(e)
    }

    /**
     * Hide the system UI.
     * @param delayMillis - delay in milliseconds before hiding.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeMessages(MESSAGE_HIDING)
        mHideHandler.sendEmptyMessageDelayed(MESSAGE_HIDING, delayMillis.toLong())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (AwfulUtils.isKitKat() && mPrefs.immersionMode) {
            // When the window loses focus (e.g. the action overflow is shown),
            // cancel any pending hide action. When the window gains focus,
            // hide the system UI.
            if (hasFocus) {
                delayedHide(DEFAULT_HIDE_DELAY)
            } else {
                mHideHandler.removeMessages(0)
            }
        }
    }

    @SuppressLint("NewApi")
    private fun showSystemUi() {
        if (AwfulUtils.isKitKat() && mPrefs.immersionMode) {
            mDecorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    @SuppressLint("NewApi")
    private fun hideSystemUi() {
        if (AwfulUtils.isKitKat() && mPrefs.immersionMode) {
            mDecorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }


    override fun onNewIntent(intent: Intent?) {
        Timber.v("onNewIntent")
        super.onNewIntent(intent)

        setIntent(intent)
        val initialPage = parseNewIntent(intent!!)
        if (pagerAdapter.count >= initialPage && initialPage >= 0) {
            mViewPager.currentItem = initialPage
        }
        mForumFragment?.openForum(mForumId, mForumPage)

        if (url.isThread || url.isPost) {
            mThreadFragment?.openThread(url)
        } else if (intent.getIntExtra(Constants.THREAD_ID, NULL_THREAD_ID) > 0) {
            Timber.d("else: $mThreadPost")
            mThreadFragment?.openThread(threadId, threadPage, mThreadPost)
        }
    }

    private fun parseNewIntent(intent: Intent?): Int {
        Timber.v("parseNewIntent")
        var initialPage = NULL_PAGE_ID
        var forumId = getIntent().getIntExtra(Constants.FORUM_ID, mForumId)
        var forumPage = getIntent().getIntExtra(Constants.FORUM_PAGE, mForumPage)
        var threadId = getIntent().getIntExtra(Constants.THREAD_ID, this.threadId)
        var threadPage = getIntent().getIntExtra(Constants.THREAD_PAGE, this.threadPage)
        mThreadPost = getIntent().getStringExtra(Constants.THREAD_FRAGMENT) ?: ""

        if (forumId == 2) {//workaround for old userCP ID, ugh. the old id still appears if someone created a bookmark launch shortcut prior to b23
            forumId = Constants.USERCP_ID//should never have used 2 as a hard-coded forum-id, what a horror.
        }

        var displayIndex = false
        val scheme = if (getIntent().data == null) null else getIntent().data!!.scheme
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
                AwfulURL.TYPE.POST -> {}
                AwfulURL.TYPE.INDEX -> displayIndex = true
            }
        }

        setForum(forumId, forumPage)
        setThread(threadId, threadPage)

        if (displayIndex) {
            displayForumIndex()
        }
        if (intent!!.getIntExtra(Constants.FORUM_ID, 0) > 1 || url.isForum) {
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

        val versionCode = BuildConfig.VERSION_CODE

        // check if this is the first run, and if so show the 'welcome' dialog
        if (mPrefs.alertIDShown == 0) {
            AlertDialog.Builder(this).setTitle(getString(R.string.alert_title_1))
                    .setMessage(getString(R.string.alert_message_1))
                    .setPositiveButton(getString(R.string.alert_ok)) { dialog, _ -> dialog.dismiss() }
                    .setNegativeButton(getString(R.string.alert_settings)) { dialog, _ ->
                        dialog.dismiss()
                        startActivity(Intent().setClass(this@ForumsIndexActivity, SettingsActivity::class.java))
                    }
                    .show()
            mPrefs.setPreference(Keys.ALERT_ID_SHOWN, 1)
        } else if (mPrefs.lastVersionSeen != versionCode) {
            Timber.i("App version changed from %d to %d - showing changelog", mPrefs.lastVersionSeen, versionCode)
            ChangelogDialog.show(this)
            mPrefs.setPreference(Keys.LAST_VERSION_SEEN, versionCode)
        }
    }

    override fun onPause() {
        super.onPause()
        if (mForumFragment != null) {
            setForum(mForumFragment!!.forumId, mForumFragment!!.page)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.apply {
            putInt(Constants.FORUM_ID, mForumId)
            putInt(Constants.FORUM_PAGE, mForumPage)
            putInt(Constants.THREAD_ID, threadId)
            putInt(Constants.THREAD_PAGE, threadPage)
            putInt("viewPage", mViewPager.currentItem)
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
        mForumId = forumId
        mForumPage = page
        setNavIds(mForumId, null)
    }


    @Synchronized
    fun setThread(threadId: Int?, page: Int?) {
        if (page != null) {
            threadPage = page
        }
        if (threadId != null) {
            val oldThreadId = this.threadId
            this.threadId = threadId
            if ((oldThreadId < 1 || threadId < 1) && threadId != oldThreadId) {
                pagerAdapter.notifyDataSetChanged()//notify pager adapter so it'll show/hide the thread view
            }
        }
        setNavIds(mNavForumId, this.threadId)
    }


    inner class ForumPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm), ViewPager.OnPageChangeListener {

        private var visible: AwfulFragment? = null

        override fun onPageSelected(arg0: Int) {
           Timber.i("onPageSelected: $arg0")
            visible?.onPageHidden()

            val apf = instantiateItem(mViewPager, arg0) as AwfulFragment
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
            when (position) {
                FORUM_LIST_FRAGMENT_POSITION -> {
                    if (mIndexFragment == null) {
                        mIndexFragment = ForumsIndexFragment()
                    }
                    return mIndexFragment
                }
                THREAD_LIST_FRAGMENT_POSITION -> {
                    if (mForumFragment == null) {
                        mForumFragment = ForumDisplayFragment.getInstance(mForumId, mForumPage, skipLoad)
                    }
                    return mForumFragment
                }
                THREAD_VIEW_FRAGMENT_POSITION -> {
                    if (mThreadFragment == null) {
                        mThreadFragment = ThreadDisplayFragment()
                    }
                    return mThreadFragment
                }
            }
            Timber.e("ERROR: asked for too many fragments in ForumPagerAdapter.getItem")
            return null
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val frag = super.instantiateItem(container, position)
            when (position) {
                FORUM_LIST_FRAGMENT_POSITION -> mIndexFragment = frag as ForumsIndexFragment
                THREAD_LIST_FRAGMENT_POSITION -> mForumFragment = frag as ForumDisplayFragment
                THREAD_VIEW_FRAGMENT_POSITION -> mThreadFragment = frag as ThreadDisplayFragment
            }
            return frag
        }

        override fun getCount() = if (threadId < 1) 2 else 3

        override fun getItemPosition(item: Any): Int {
            if (mIndexFragment != null && mIndexFragment == item) {
                return FORUM_LIST_FRAGMENT_POSITION
            }
            if (mForumFragment != null && mForumFragment == item) {
                return THREAD_LIST_FRAGMENT_POSITION
            }
            return if (mThreadFragment != null && mThreadFragment == item) {
                THREAD_VIEW_FRAGMENT_POSITION
            } else super.getItemPosition(item)
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
    }


    override fun setActionbarTitle(aTitle: String?, requestor: Any?) {
        if (requestor != null) {
            //This will only honor the request if the requestor is the currently active view.
            if (requestor is AwfulFragment && isFragmentVisible((requestor as AwfulFragment?)!!)) {
                super.setActionbarTitle(aTitle, requestor)
            } else {
                Timber.d("Failed setActionbarTitle: $aTitle - $requestor")
            }
        } else {
            super.setActionbarTitle(aTitle, requestor)
        }
    }

    override fun onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(Gravity.START)) {
            mDrawerLayout.closeDrawers()
        } else {
            if (mViewPager.currentItem > 0) {
                if (!(pagerAdapter.getItem(mViewPager.currentItem) as AwfulFragment).onBackPressed()) {
                    mViewPager.currentItem = mViewPager.currentItem - 1
                }
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun displayForum(id: Int, page: Int) {
        Timber.i("displayForum $id")
        setForum(id, page)
        setNavIds(id, null)
        if (mForumFragment != null) {
            mForumFragment?.openForum(id, page)
            mViewPager.currentItem = pagerAdapter.getItemPosition(mForumFragment!!)
        }
    }


    override fun isFragmentVisible(awfulFragment: AwfulFragment): Boolean {
        return if (isTablet) {
            val itemPos = pagerAdapter.getItemPosition(awfulFragment)
            itemPos == mViewPager.currentItem || itemPos == mViewPager.currentItem + 1
        } else {
            pagerAdapter.getItemPosition(awfulFragment) == mViewPager.currentItem
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (mDrawerToggle.onOptionsItemSelected(item)) {
            true
        } else super.onOptionsItemSelected(item)
    }


    override fun displayThread(id: Int, page: Int, forumId: Int, forumPage: Int, forceReload: Boolean) {
        Timber.d("displayThread $id $forumId")
        if (mThreadFragment != null) {
            if (!forceReload && threadId == id && threadPage == page) {
                setNavIds(mThreadFragment!!.parentForumId, mNavThreadId)
            } else {
                mThreadFragment?.openThread(id, page, null)
                mViewPager.adapter?.notifyDataSetChanged()
            }
        } else {
            setThread(id, page)
        }
        mViewPager.currentItem = pagerAdapter.getItemPosition(mThreadFragment!!)
    }

    override fun displayUserCP() {
        displayForum(Constants.USERCP_ID, 1)
    }

    override fun displayForumIndex() {
        mViewPager.currentItem = 0
    }

    override fun onActivityResult(request: Int, result: Int, intent: Intent?) {
        if (intent == null) return
        Timber.d("onActivityResult: $request result: $result")
        super.onActivityResult(request, result, intent)
        if (request == Constants.LOGIN_ACTIVITY_REQUEST && result == Activity.RESULT_OK) {
            SyncManager.sync(this)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val pagerItem = pagerAdapter.getItem(mViewPager.currentItem) as AwfulFragment?
        return if (mPrefs.volumeScroll && pagerItem != null && pagerItem.attemptVolumeScroll(event)) {
            true
        } else super.dispatchKeyEvent(event)
    }

    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        super.onPreferenceChange(prefs, key)
        if (prefs.immersionMode && mDecorView == null) {
            setupImmersion()
        }
        mViewPager.setSwipeEnabled(!prefs.lockScrolling)

        setNavigationDrawer()
        if (!AwfulUtils.isTablet(this) && AwfulUtils.isAtLeast(Build.VERSION_CODES.JELLY_BEAN_MR1) && prefs.transformer != "Disabled") {
            mViewPager.setPageTransformer(true, AwfulUtils.getViewPagerTransformer())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Timber.v("onConfigurationChanged()")
        val oldTab = isTablet
        isTablet = AwfulUtils.isTablet(this)
        if (oldTab != isTablet) {
            if (isTablet) {
                mViewPager.pageMargin = 1
                //TODO what color should it use here?
                mViewPager.setPageMarginDrawable(ColorDrawable(ColorProvider.ACTION_BAR.color))
            } else {
                mViewPager.pageMargin = 0
            }

            mViewPager.adapter = pagerAdapter
        }
        (mViewPager.adapter as ForumPagerAdapter).getItem(mViewPager.currentItem)?.onConfigurationChanged(newConfig)
        mDrawerToggle.onConfigurationChanged(newConfig)
    }


    /**
     * Set the IDs that the navigation view knows about?
     * Updates the navigation menu to reflect these
     * @param forumId   The current forum
     * @param threadId
     */
    @Synchronized
    fun setNavIds(forumId: Int, threadId: Int?) {
        // if we only get a forumId, clear the thread one so it's not displayed
        mNavForumId = forumId
        mNavThreadId = threadId ?: NULL_THREAD_ID
        updateNavigationMenu()
    }

    fun preventSwipe() {
        this.mViewPager.setSwipeEnabled(false)
    }

    fun reenableSwipe() {
        runOnUiThread {
            if (mViewPager.beginFakeDrag()) {
                mViewPager.endFakeDrag()
            }
            mViewPager.setSwipeEnabled(true)
        }
    }

    companion object {
        val TAG = "ForumsIndexActivity"

        private val DEFAULT_HIDE_DELAY = 300
        private val MESSAGE_HIDING = 0
        private val MESSAGE_VISIBLE_CHANGE_IN_PROGRESS = 1

        private val NULL_FORUM_ID = 0
        private val NULL_THREAD_ID = 0
        private val NULL_PAGE_ID = -1

        private val FORUM_LIST_FRAGMENT_POSITION = 0
        private val THREAD_LIST_FRAGMENT_POSITION = 1
        private val THREAD_VIEW_FRAGMENT_POSITION = 2
    }
}
