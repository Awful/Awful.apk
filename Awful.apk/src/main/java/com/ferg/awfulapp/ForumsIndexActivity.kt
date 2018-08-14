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
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v7.widget.Toolbar
import android.view.KeyEvent
import android.view.View.*
import com.ferg.awfulapp.NavigationEvent.*
import com.ferg.awfulapp.NavigationEvent.Companion.parse
import com.ferg.awfulapp.announcements.AnnouncementsManager
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.dialog.Changelog
import com.ferg.awfulapp.messages.PmManager
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.Keys
import com.ferg.awfulapp.sync.SyncManager
import timber.log.Timber
import java.util.*


class ForumsIndexActivity :
        AwfulActivity(),
        PmManager.Listener,
        AnnouncementsManager.AnnouncementListener,
        PagerCallbacks {

    private lateinit var forumsPager: ForumsPagerController
    private lateinit var toolbar: Toolbar
    private lateinit var navigationDrawer: NavigationDrawer

    private var activityInitialized = false

    private val handler by lazy { Handler() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forum_index_activity)

        val viewPager: SwipeLockViewPager = findViewById(R.id.forum_index_pager)
        forumsPager = ForumsPagerController(viewPager, mPrefs, this, this, savedInstanceState)

        toolbar = findViewById(R.id.awful_toolbar)
        setSupportActionBar(toolbar)
        setUpActionBar()

        setupImmersion()

        navigationDrawer = NavigationDrawer(this, toolbar, mPrefs)
        activityInitialized = true
        updateNavDrawer()

        PmManager.registerListener(this)
        AnnouncementsManager.getInstance().registerListener(this)

        // only handle the Activity's intent when it's first created, or that navigation event will be fired whenever the activity is restored
        if (savedInstanceState == null) {
            handleIntent(intent)
        }
        showChangelogIfRequired()
    }

    override fun onNewPm(messageUrl: String, sender: String, unreadCount: Int) {
        val showPmEvent = NavigationEvent.ShowPrivateMessages(Uri.parse(messageUrl))
        runOnUiThread {
            val message = "Private message from %s\n(%d unread)"
            makeSnackbar(String.format(Locale.getDefault(), message, sender, unreadCount), showPmEvent)
        }
    }

    override fun onAnnouncementsUpdated(newCount: Int, oldUnread: Int, oldRead: Int, isFirstUpdate: Boolean) {
        // only show one of 'new announcements' or 'unread announcements', ignoring read ones
        // (only notify about unread for the first update after opening the app, to remind the user)
        val hasNewAnnouncements = newCount > 0
        if (isFirstUpdate || hasNewAnnouncements) {
            if (hasNewAnnouncements) {
                val message = resources.getQuantityString(R.plurals.numberOfNewAnnouncements, newCount, newCount)
                makeSnackbar(message, NavigationEvent.Announcements)
            } else if (oldUnread > 0) {
                val message = resources.getQuantityString(R.plurals.numberOfOldUnreadAnnouncements, oldUnread, oldUnread)
                makeSnackbar(message, NavigationEvent.Announcements)
            }
        }
    }

    private fun makeSnackbar(message: String, event: NavigationEvent = NavigationEvent.MainActivity) {
        Snackbar.make(toolbar, message, Snackbar.LENGTH_LONG)
                .setDuration(3000)
                .setAction("View") { navigate(event) }
                .show()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        navigationDrawer.drawerToggle.syncState()
    }


    /**
     *  Display details about the current thread and forum in the nav bar
     *
     *  Shows the thread, parent forum, and forums root
     */
    private fun updateNavDrawer() {
        if(!activityInitialized) return
        val threadFragment = forumsPager.getThreadDisplayFragment()
        val forumFragment = forumsPager.getForumDisplayFragment()

        val forumId = threadFragment?.parentForumId ?: forumFragment?.forumId
        val threadId = threadFragment?.threadId

        navigationDrawer.setCurrentForumAndThread(forumId, threadId)
    }


    private fun updateTitle() {
        if(!activityInitialized) return
        super.setActionbarTitle(forumsPager.getVisibleFragmentTitle())
    }

    override fun setActionbarTitle(title: String) {
        throw RuntimeException("Don't set this directly!")
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment events
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Notify the activity that something about a pager fragment has changed, so it can update appropriately.
     *
     * This could be extended by passing the fragment in if necessary, or adding methods for each
     * page (onThreadChanged etc) - but right now this is all we need.
     */
    fun onPageContentChanged() {
        updateNavDrawer()
        updateTitle()
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }


    /**
     * Parse and process an intent, e.g. to handle a navigation event.
     */
    private fun handleIntent(intent: Intent) {
        val parsed = intent.parse()
        Timber.i("Parsed intent as %s", parsed.toString())
        navigate(parsed)
    }


    override fun handleNavigation(event: NavigationEvent): Boolean {
        return when(event) {
            is MainActivity ->
                true
            is ForumIndex ->
                true.also { forumsPager.currentPagerItem = Pages.ForumIndex }
            is Bookmarks, is NavigationEvent.Forum, is Thread, is Url ->
                true.also { forumsPager.navigate(event) }
            is ReAuthenticate ->
                true.also { Authentication.reAuthenticate(this) }
            else -> false
        }
    }


    private fun showChangelogIfRequired() {
        val versionCode = BuildConfig.VERSION_CODE
        val lastVersionCode = mPrefs.lastVersionSeen

        if (lastVersionCode != versionCode) {
            Timber.i("App version changed from %d to %d - showing changelog", lastVersionCode, versionCode)
            Changelog.showDialog(this, 1)
            mPrefs.setPreference(Keys.LAST_VERSION_SEEN, versionCode)
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        forumsPager.onSaveInstanceState(outState)
    }


    override fun onPageChanged(page: Pages, pageFragment: AwfulFragment) {
        // I don't know if #isAdded is necessary after calling #instantiateItem (instead of #getItem
        // which just creates a new fragment object), but I'm trying to fix a bug I can't reproduce
        // where these fragment methods crash because they have no activity yet
        Timber.i("onPageChanged: page %s fragment %s", page, pageFragment)
        updateTitle()
        if (pageFragment.isAdded) {
            setProgress(pageFragment.progressPercent)
        }
    }


    override fun onBackPressed() {
        // in order of precedence: close the nav drawer, tell the current fragment to go back, tell the pager to go back
        if (navigationDrawer.close() || forumsPager.onBackPressed()) return
        super.onBackPressed()
    }


    override fun onActivityResult(request: Int, result: Int, intent: Intent?) {
        super.onActivityResult(request, result, intent)
        if (request == Constants.LOGIN_ACTIVITY_REQUEST && result == Activity.RESULT_OK) {
            Timber.i("Result from login activity: successful login - calling sync")
            SyncManager.sync(this)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val pagerItem = forumsPager.getCurrentFragment()
        return if (mPrefs.volumeScroll && pagerItem?.attemptVolumeScroll(event) == true) {
            true
        } else super.dispatchKeyEvent(event)
    }

    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        super.onPreferenceChange(prefs, key)
        forumsPager.onPreferenceChange(prefs)
        setupImmersion()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        forumsPager.onConfigurationChange(mPrefs)
        forumsPager.getCurrentFragment()?.onConfigurationChanged(newConfig)
        navigationDrawer.drawerToggle.onConfigurationChanged(newConfig)
    }

    fun preventSwipe() {
        forumsPager.setSwipeEnabled(false)
    }

    fun allowSwipe() {
        runOnUiThread { forumsPager.setSwipeEnabled(true) }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        Timber.d("Activity focus changed (has focus: $hasFocus)")
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) setupImmersion()
    }


    /**
     * Set the Activity's UI flags according to the current immersive mode preferences.
     * This will also act as a reset if these flags have been changed, e.g. for a temporary fullscreen mode.
     */
    private fun setupImmersion() {
        val immersionFlags = SYSTEM_UI_FLAG_FULLSCREEN or SYSTEM_UI_FLAG_HIDE_NAVIGATION or SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = if (mPrefs.immersionMode) immersionFlags else {
            // show the UI after a delay - without this the nav/status bars remain invisible after returning from
            // immersion mode (e.g. fullscreen video) and the flag does nothing if you just set it here.
            // The setting code seems to fire at some random time though, SOMEHOW, instead of when the runnable is executed
            handler.postDelayed( { Timber.d("Showing nav and status bars"); window.decorView.systemUiVisibility = SYSTEM_UI_FLAG_VISIBLE }, 3000)
            // default to LAYOUT STABLE to avoid layout movement after returning from e.g. fullscreen videos
            SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }
}