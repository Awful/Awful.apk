/**
 * *****************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 * <p/>
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
 * <p/>
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

package com.ferg.awfulapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.ferg.awfulapp.announcements.AnnouncementsManager;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.dialog.ChangelogDialog;
import com.ferg.awfulapp.dialog.LogOutDialog;
import com.ferg.awfulapp.messages.PmManager;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.Keys;
import com.ferg.awfulapp.preferences.SettingsActivity;
import com.ferg.awfulapp.sync.SyncManager;
import com.ferg.awfulapp.thread.AwfulURL;
import com.ferg.awfulapp.util.AwfulUtils;
import com.ferg.awfulapp.widget.ToggleViewPager;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

//import com.ToxicBakery.viewpager.transforms.*;

public class ForumsIndexActivity extends AwfulActivity
        implements PmManager.Listener, AnnouncementsManager.AnnouncementListener, PagerCallbacks {
    protected static final String TAG = "ForumsIndexActivity";

    private static final int DEFAULT_HIDE_DELAY = 300;

    private static final int MESSAGE_HIDING = 0;
    private static final int MESSAGE_VISIBLE_CHANGE_IN_PROGRESS = 1;

    private boolean skipLoad = false;
    private boolean isTablet;
    private AwfulURL url = new AwfulURL();

    private ForumsPagerController forumsPager;
    private View mDecorView;
    private Toolbar mToolbar;
    private NavigationDrawer navigationDrawer;

    private static final int NO_PAGER_ITEM = -1;

    private GestureDetector mImmersionGestureDetector = null;
    private boolean mIgnoreFling;
    private String mThreadPost="";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forum_index_activity);

        ToggleViewPager viewPager = findViewById(R.id.forum_index_pager);
        // TODO: 04/11/2017 passing activity in - do after create?
        forumsPager = new ForumsPagerController(viewPager, mPrefs, this, this, savedInstanceState);
        mToolbar = findViewById(R.id.awful_toolbar);
        setSupportActionBar(mToolbar);
        setActionBar();
        navigationDrawer = new NavigationDrawer(this, mToolbar, mPrefs);
//        updateNavigationDrawer();

        isTablet = AwfulUtils.isTablet(this);
        int focusedPagerItem;
        if (savedInstanceState != null) {
            focusedPagerItem = savedInstanceState.getInt("viewPage", NO_PAGER_ITEM);
        } else {
            focusedPagerItem = parseNewIntent(getIntent());
        }
        forumsPager.setCurrentPagerItem(focusedPagerItem);

        checkIntentExtras();
        setupImmersion();

        PmManager.registerListener(this);
        AnnouncementsManager.getInstance().registerListener(this);
    }

    @Override
    public void onNewPm(@NonNull String messageUrl, @NonNull final String sender, final int unreadCount) {
        // TODO: 16/08/2016 probably best to put this in a method that the menu option calls too
        final Intent pmIntent = new Intent().setClass(this, PrivateMessageActivity.class);
        Uri uri = Uri.parse(messageUrl);
        if (uri != null) {
            pmIntent.setData(uri);
        }
        runOnUiThread(() -> {
            String message = "Private message from %s\n(%d unread)";
            Snackbar.make(mToolbar, String.format(Locale.getDefault(), message, sender, unreadCount), Snackbar.LENGTH_LONG)
                    .setDuration(3000)
                    .setAction("View", view -> startActivity(pmIntent))
                    .show();
        });
    }

    @Override
    public void onAnnouncementsUpdated(int newCount, int oldUnread, int oldRead, boolean isFirstUpdate) {
        if (isFirstUpdate || newCount > 0) {
            Resources res = getResources();
            // only show one of 'new announcements' or 'unread announcements', ignoring read ones
            // (only notify about unread for the first update after opening the app, to remind the user)
            if (newCount > 0) {
                showAnnouncementSnackbar(res.getQuantityString(R.plurals.numberOfNewAnnouncements, newCount, newCount));
            } else if (oldUnread > 0) {
                showAnnouncementSnackbar(res.getQuantityString(R.plurals.numberOfOldUnreadAnnouncements, oldUnread, oldUnread));
            }
        }
    }

    private void showAnnouncementSnackbar(String message) {
        Snackbar.make(mToolbar, message, Snackbar.LENGTH_LONG)
                .setDuration(3000)
                .setAction("View", click -> AnnouncementsManager.getInstance().showAnnouncements(this))
                .show();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        navigationDrawer.getDrawerToggle().syncState();
    }


    @SuppressLint("NewApi")
    private void setupImmersion() {
        if (AwfulUtils.isKitKat() && mPrefs.immersionMode) {
            mDecorView = getWindow().getDecorView();

            mDecorView.setOnSystemUiVisibilityChangeListener(
                    flags -> {
                        boolean visible = (flags & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
                        if (visible) {
                            // Add some delay so the act of swiping to bring system UI into view doesn't turn it back off
                            mHideHandler.removeMessages(MESSAGE_VISIBLE_CHANGE_IN_PROGRESS);
                            mHideHandler.sendEmptyMessageDelayed(MESSAGE_VISIBLE_CHANGE_IN_PROGRESS, 800);
                            mIgnoreFling = true;
                        }
                    });

            mImmersionGestureDetector = new GestureDetector(this,
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                            if (mIgnoreFling) return true;

                            boolean visible = (mDecorView.getSystemUiVisibility()
                                    & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
                            if (visible) {
                                hideSystemUi();
                            }

                            return true;
                        }
                    });
            showSystemUi();
        }
    }


    private void updateNavigationDrawer() {
        if (navigationDrawer != null) {
            // display details for the currently open thread - if there isn't one, show the current forum instead
            ThreadDisplayFragment threadFragment = forumsPager.getThreadDisplayFragment();
            if (threadFragment != null) {
                int threadId = threadFragment.getThreadId();
                int parentForumId = threadFragment.getParentForumId();
                navigationDrawer.setCurrentForumAndThread(parentForumId, threadId);
            } else {
                int forumId = forumsPager.getForumDisplayFragment().getForumId();
                navigationDrawer.setCurrentForumAndThread(forumId, null);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment events
    ///////////////////////////////////////////////////////////////////////////


    void onThreadChange() {
        updateNavigationDrawer();
    }

    void onForumChange() {
        updateNavigationDrawer();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Navigation events
    ///////////////////////////////////////////////////////////////////////////


    /** Display the user's bookmarks */
    public void showBookmarks() {
        startActivity(new Intent().setClass(this, ForumsIndexActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Constants.FORUM_ID, Constants.USERCP_ID)
                .putExtra(Constants.FORUM_PAGE, ForumDisplayFragment.FIRST_PAGE));
    }


    public void showLogout() {
        new LogOutDialog(this).show();
    }

    /** Display the announcements */
    public void showAnnouncements() {
        AnnouncementsManager.getInstance().showAnnouncements(ForumsIndexActivity.this);
    }

    /** Display the user's PMs */
    public void showPrivateMessages() {
        startActivity(new Intent().setClass(this, PrivateMessageActivity.class));
    }

    /** Display the forum search */
    public void showSearch() {
        Intent intent = BasicActivity.Companion.intentFor(SearchFragment.class, this, getString(R.string.search_forums_activity_title));
        startActivity(intent);
    }

    /** Display the app settings */
    public void showSettings() {
        startActivity(new Intent().setClass(this, SettingsActivity.class));
    }


    /**
     * Page to the thread view. If it's not currently showing the expected thread, load the first page of it.
     *
     * @param expectedThreadId  the page that should be shown
     * @param parentForumId     the ID of the thread's parent forum
     */
    public void showThreadView(int expectedThreadId, int parentForumId) {
        if (forumsPager.getThreadDisplayFragment().getThreadId() != expectedThreadId) {
            displayThread(expectedThreadId, ThreadDisplayFragment.FIRST_PAGE, parentForumId, ForumDisplayFragment.FIRST_PAGE, true);
        } else {
            forumsPager.setCurrentPagerItem(2);
        }
    }


    /**
     * Page to the forum/threadlist view. If it's not currently showing the expected forum, load the first page of it.
     * @param expectedForumId the forum that should be shown
     */
    public void showForumView(int expectedForumId) {
        if (forumsPager.getForumDisplayFragment().getForumId() != expectedForumId) {
            displayForum(expectedForumId, ForumDisplayFragment.FIRST_PAGE);
        } else {
            forumsPager.setCurrentPagerItem(1);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////



    @Override
    @SuppressLint("NewApi")
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (AwfulUtils.isKitKat() && mPrefs.immersionMode) {
            super.dispatchTouchEvent(e);
            return mImmersionGestureDetector.onTouchEvent(e);
        }

        return super.dispatchTouchEvent(e);
    }

    private final Handler mHideHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_HIDING) {
                hideSystemUi();
            } else if (msg.what == MESSAGE_VISIBLE_CHANGE_IN_PROGRESS) {
                mIgnoreFling = false;
            }
        }
    };

    /**
     * Hide the system UI.
     * @param delayMillis - delay in milliseconds before hiding.
     */
    public void delayedHide(int delayMillis) {
        mHideHandler.removeMessages(MESSAGE_HIDING);
        mHideHandler.sendEmptyMessageDelayed(MESSAGE_HIDING, delayMillis);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (AwfulUtils.isKitKat() && mPrefs.immersionMode) {
            // When the window loses focus (e.g. the action overflow is shown),
            // cancel any pending hide action. When the window gains focus,
            // hide the system UI.
            if (hasFocus) {
                delayedHide(DEFAULT_HIDE_DELAY);
            } else {
                mHideHandler.removeMessages(0);
            }
        }
    }

    @SuppressLint("NewApi")
    private void showSystemUi() {
        if (AwfulUtils.isKitKat() && mPrefs.immersionMode) {
            mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @SuppressLint("NewApi")
    private void hideSystemUi() {
        if (AwfulUtils.isKitKat() && mPrefs.immersionMode) {
            mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        // TODO: 15/01/2018 rework this so it performs the correct operation (e.g. display thread X page Y) without storing state here e.g. mThreadId
        super.onNewIntent(intent);
        if (DEBUG) Log.e(TAG, "onNewIntent");
        setIntent(intent);
        int initialPage = parseNewIntent(intent);
        /*
            see if there's a pager page in the intent - if so, set it as current
            open the current forum at the current page (may have been set)
            open the current url if thread/post, or
                if there's a valid thread ID in the extras, open that
         */
        forumsPager.setCurrentPagerItem(initialPage);
        forumsPager.openForum(tempForumId, tempForumPage);
        if (url.isThread() || url.isPost()) {
            forumsPager.openThread(url);
        } else if (intent.getIntExtra(Constants.THREAD_ID, ThreadDisplayFragment.NULL_THREAD_ID) > 0) {
            if (DEBUG) Log.e(TAG, "else: "+mThreadPost);
            forumsPager.openThread(tempThreadId, tempThreadPage, mThreadPost, true);
        }
    }

    private volatile int tempForumId       = Constants.USERCP_ID;
    private volatile int tempForumPage     = 1;
    private volatile int tempThreadId      = ThreadDisplayFragment.NULL_THREAD_ID;
    private volatile int tempThreadPage    = 1;

    private int parseNewIntent(Intent intent) {
        int focusedPagerItem = NO_PAGER_ITEM;
        int forumId     = getIntent().getIntExtra(Constants.FORUM_ID, tempForumId);
        int forumPage   = getIntent().getIntExtra(Constants.FORUM_PAGE, tempForumPage);
        int threadId    = getIntent().getIntExtra(Constants.THREAD_ID, tempThreadId);
        int threadPage  = getIntent().getIntExtra(Constants.THREAD_PAGE, tempThreadPage);
        mThreadPost = getIntent().getStringExtra(Constants.THREAD_FRAGMENT);

        if (forumId == 2) {//workaround for old userCP ID, ugh. the old id still appears if someone created a bookmark launch shortcut prior to b23
            forumId = Constants.USERCP_ID;//should never have used 2 as a hard-coded forum-id, what a horror.
        }

        boolean displayIndex = false;
        String scheme = (getIntent().getData() == null) ? null : getIntent().getData().getScheme();
        if ("http".equals(scheme) || "https".equals(scheme)) {
            // parse a URL
            url = AwfulURL.parse(getIntent().getDataString());
            switch (url.getType()) {
                case FORUM:
                    // forum URL - treat page and ID as forum ones
                    forumId = (int) url.getId();
                    forumPage = (int) url.getPage();
                    break;
                case THREAD:
                    // thread URL - treat page and ID as thread ones
                    if (!url.isRedirect()) {
                        threadPage = (int) url.getPage();
                        threadId = (int) url.getId();
                    }
                    break;
                case POST:
                    break;
                case INDEX:
                    // basically show the first pager page (this is the only thing that forces it)
                    displayIndex = true;
                    break;
                default:
            }
        }

        // TODO: 16/01/2018 removed internal state for forum/thread IDs and pages here - make sure it's handled somehow!

        if (displayIndex) {
            // literally just shows pager page 0
            displayForumIndex();
        }
        // if we have a forum ID/URL, show page 1 for phones, otherwise 0
        if (intent.getIntExtra(Constants.FORUM_ID, 0) > 1 || url.isForum()) {
            focusedPagerItem = isTablet ? 0 : 1;
        } else {
            // something to do with not loading the forum on create if we're on a phone?
            skipLoad = !isTablet;
        }
        // oh hey ACTUALLY if we have a valid thread ID/url or a redirect, always show page 2
        if (intent.getIntExtra(Constants.THREAD_ID, ThreadDisplayFragment.NULL_THREAD_ID) > 0 || url.isRedirect() || url.isThread()) {
            focusedPagerItem = 2;
        }
        return focusedPagerItem;
    }

    @Override
    protected void onResume() {
        super.onResume();

        int versionCode = 0;
        try {
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // check if this is the first run, and if so show the 'welcome' dialog
        if (mPrefs.alertIDShown == 0) {
            new AlertDialog.Builder(this).
                    setTitle(getString(R.string.alert_title_1))
                    .setMessage(getString(R.string.alert_message_1))
                    .setPositiveButton(getString(R.string.alert_ok), (dialog, which) -> dialog.dismiss())
                    .setNegativeButton(getString(R.string.alert_settings), (dialog, which) -> {
                        dialog.dismiss();
                        showSettings();
                    })
                    .show();
            mPrefs.setPreference(Keys.ALERT_ID_SHOWN, 1);
        } else if (mPrefs.lastVersionSeen != versionCode) {
            Log.i(TAG, String.format("App version changed from %d to %d - showing changelog", mPrefs.lastVersionSeen, versionCode));
            ChangelogDialog.show(this);
            mPrefs.setPreference(Keys.LAST_VERSION_SEEN, versionCode);
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("viewPage", forumsPager.getCurrentPagerItem());
        forumsPager.onSaveInstanceState(outState);
    }


    private void checkIntentExtras() {
        if (getIntent().hasExtra(Constants.SHORTCUT)) {
            if (getIntent().getBooleanExtra(Constants.SHORTCUT, false)) {
                displayForum(Constants.USERCP_ID, 1);
            }
        }
    }


    @Override
    public void onPageChanged(int pageNum, @NotNull AwfulFragment pageFragment) {
        // I don't know if #isAdded is necessary after calling #instantiateItem (instead of #getItem
        // which just creates a new fragment object), but I'm trying to fix a bug I can't reproduce
        // where these fragment methods crash because they have no activity yet
        if (pageFragment.isAdded()) {
            setActionbarTitle(pageFragment.getTitle(), null);
            setProgress(pageFragment.getProgressPercent());
        }
    }


    @Override
    public void setActionbarTitle(String aTitle, Object requestor) {
        if (requestor != null && requestor instanceof AwfulFragment) {
            //This will only honor the request if the requestor is the currently active view.
            if (forumsPager.isFragmentVisible((AwfulFragment) requestor)) {
                super.setActionbarTitle(aTitle, requestor);
            } else {
                if (DEBUG) Log.i(TAG, "Failed setActionbarTitle: " + aTitle + " - " + requestor.toString());
            }
        } else {
            super.setActionbarTitle(aTitle, requestor);
        }
    }

    @Override
    public void onBackPressed() {
        // in order of precedence: close the nav drawer, tell the current fragment to go back, tell the pager to go back
        if (navigationDrawer.close()) {
            return;
        }

        AwfulFragment currentFragment = forumsPager.getCurrentFragment();
        if (currentFragment != null && currentFragment.onBackPressed()) {
            return;
        }
        if (forumsPager.goBackOnePage()) {
            return;
        }

        super.onBackPressed();
    }

    @Override
    public void displayForum(int id, int page) {
        Log.d(TAG, "displayForum " + id);
        forumsPager.openForum(id, page);
    }


    @Override
    public void displayThread(int id, int page, int forumId, int forumPg, boolean forceReload) {
        Log.d(TAG, "displayThread " + id + " " + forumId);
        if (forumsPager != null) {
            forumsPager.openThread(id, page, "", forceReload);
        } else {
            // TODO: 23/11/2017 is this ever called? Surely the pager should always have a fragment when required
            super.displayThread(id, page, forumId, forumPg, forceReload);
        }
    }


    @Override
    public void displayUserCP() {
        displayForum(Constants.USERCP_ID, 1);
    }


    @Override
    public void displayForumIndex() {
        // TODO: replace this with an enum call to show FORUM
        forumsPager.setCurrentPagerItem(0);
    }


    @Override
    protected void onActivityResult(int request, int result, Intent intent) {
        if (DEBUG) Log.e(TAG, "onActivityResult: " + request + " result: " + result);
        super.onActivityResult(request, result, intent);
        if (request == Constants.LOGIN_ACTIVITY_REQUEST && result == Activity.RESULT_OK) {
            SyncManager.sync(this);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        AwfulFragment pagerItem = forumsPager.getCurrentFragment();
        if (mPrefs.volumeScroll && pagerItem != null && pagerItem.attemptVolumeScroll(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onPreferenceChange(AwfulPreferences prefs,String key) {
        super.onPreferenceChange(prefs, key);
        forumsPager.onPreferenceChange(prefs);
        if (prefs.immersionMode && mDecorView == null) {
            setupImmersion();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.e(TAG,"onConfigurationChanged()");
        forumsPager.onConfigurationChange(mPrefs);
        AwfulFragment currentFragment = forumsPager.getCurrentFragment();
        if (currentFragment != null) {
            currentFragment.onConfigurationChanged(newConfig);
        }
        navigationDrawer.getDrawerToggle().onConfigurationChanged(newConfig);
    }


    public void preventSwipe() {
        forumsPager.setSwipeEnabled(false);
    }

    public void reenableSwipe() {
        runOnUiThread(() -> forumsPager.reenableSwipe());
    }


    @Override
    protected void onStop() {
        super.onStop();
    }
}
