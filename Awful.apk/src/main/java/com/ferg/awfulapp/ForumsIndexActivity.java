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
import android.support.annotation.Nullable;
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
import com.ferg.awfulapp.messages.PmManager;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.Keys;
import com.ferg.awfulapp.sync.SyncManager;
import com.ferg.awfulapp.util.AwfulUtils;
import com.ferg.awfulapp.widget.ToggleViewPager;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import timber.log.Timber;

//import com.ToxicBakery.viewpager.transforms.*;

public class ForumsIndexActivity extends AwfulActivity
        implements PmManager.Listener, AnnouncementsManager.AnnouncementListener, PagerCallbacks {
    protected static final String TAG = "ForumsIndexActivity";

    private static final int DEFAULT_HIDE_DELAY = 300;
    private static final int MESSAGE_HIDING = 0;
    private static final int MESSAGE_VISIBLE_CHANGE_IN_PROGRESS = 1;

    private ForumsPagerController forumsPager;
    private View mDecorView;
    private Toolbar mToolbar;
    private NavigationDrawer navigationDrawer;

    private GestureDetector mImmersionGestureDetector = null;
    private boolean mIgnoreFling;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forum_index_activity);

        ToggleViewPager viewPager = findViewById(R.id.forum_index_pager);
        forumsPager = new ForumsPagerController(viewPager, getMPrefs(), this, this, savedInstanceState);
        mToolbar = findViewById(R.id.awful_toolbar);
        setSupportActionBar(mToolbar);
        setUpActionBar();
        navigationDrawer = new NavigationDrawer(this, mToolbar, getMPrefs());
        updateNavigationDrawer();

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
        if (AwfulUtils.isKitKat() && getMPrefs().immersionMode) {
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
        // TODO: 17/01/2018 what is this actually meant to display? The current thread and the forum (including Bookmarks) that it's in? Or the current state of each page?
        if (navigationDrawer != null) {
            // display details for the currently open thread - if there isn't one, show the current forum instead
            ThreadDisplayFragment threadFragment = forumsPager.getThreadDisplayFragment();
            ForumDisplayFragment forumFragment = forumsPager.getForumDisplayFragment();
            if (threadFragment != null) {
                int threadId = threadFragment.getThreadId();
                int parentForumId = threadFragment.getParentForumId();
                navigationDrawer.setCurrentForumAndThread(parentForumId, threadId);
            } else if (forumFragment != null){
                int forumId = forumFragment.getForumId();
                navigationDrawer.setCurrentForumAndThread(forumId, null);
            }
        }
    }


    private void updateTitle() {
        if (forumsPager != null) {
            setActionbarTitle(forumsPager.getVisibleFragmentTitle());
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment events
    ///////////////////////////////////////////////////////////////////////////


    void onThreadChange() {
        updateNavigationDrawer();
        updateTitle();
    }

    void onForumChange() {
        updateNavigationDrawer();
        updateTitle();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Navigation events
    ///////////////////////////////////////////////////////////////////////////


    @Override
    public void showForum(int id, @Nullable Integer page) {
        Timber.d("displayForum %s", id);
        forumsPager.openForum(id, page);
    }


    @Override
    public void showThread(int id, @Nullable Integer page, @Nullable String postJump, boolean forceReload) {
        Timber.d("displayThread %s", id);
        if (forumsPager != null) {
            forumsPager.openThread(id, page, "", forceReload);
        } else {
            Timber.w("!!! no forums pager - can't open thread");
        }
    }


    @Override
    public void showBookmarks() {
        showForum(Constants.USERCP_ID, null);
    }


    @Override
    public void showForumIndex() {
        // TODO: replace this with an enum call to show FORUM
        forumsPager.setCurrentPagerItem(0);
    }


    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////



    @Override
    @SuppressLint("NewApi")
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (AwfulUtils.isKitKat() && getMPrefs().immersionMode) {
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
        if (AwfulUtils.isKitKat() && getMPrefs().immersionMode) {
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
        if (AwfulUtils.isKitKat() && getMPrefs().immersionMode) {
            mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @SuppressLint("NewApi")
    private void hideSystemUi() {
        if (AwfulUtils.isKitKat() && getMPrefs().immersionMode) {
            mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        NavigationEvent parsed = NavigationEvent.Companion.parse(intent);
        Timber.i("Parsed intent as %s", parsed.toString());

        if (parsed instanceof NavigationEvent.ForumIndex) {
            showForumIndex();
        } else if (parsed instanceof NavigationEvent.Bookmarks) {
            showBookmarks();
        } else if (parsed instanceof NavigationEvent.Forum) {
            NavigationEvent.Forum forum = (NavigationEvent.Forum) parsed;
            showForum(forum.getId(), forum.getPage());
        } else if (parsed instanceof NavigationEvent.Thread) {
            NavigationEvent.Thread thread = (NavigationEvent.Thread) parsed;
            showThread(thread.getId(), thread.getPage(), thread.getPostJump(), true);
        } else if (parsed instanceof NavigationEvent.Url) {
            NavigationEvent.Url url = (NavigationEvent.Url) parsed;
            forumsPager.openThread(url.getUrl());
        }
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
        if (getMPrefs().alertIDShown == 0) {
            new AlertDialog.Builder(this).
                    setTitle(getString(R.string.alert_title_1))
                    .setMessage(getString(R.string.alert_message_1))
                    .setPositiveButton(getString(R.string.alert_ok), (dialog, which) -> dialog.dismiss())
                    .setNegativeButton(getString(R.string.alert_settings), (dialog, which) -> {
                        dialog.dismiss();
                        showSettings();
                    })
                    .show();
            getMPrefs().setPreference(Keys.ALERT_ID_SHOWN, 1);
        } else if (getMPrefs().lastVersionSeen != versionCode) {
            Log.i(TAG, String.format("App version changed from %d to %d - showing changelog", getMPrefs().lastVersionSeen, versionCode));
            ChangelogDialog.show(this);
            getMPrefs().setPreference(Keys.LAST_VERSION_SEEN, versionCode);
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        forumsPager.onSaveInstanceState(outState);
    }



    @Override
    public void onPageChanged(int pageNum, @NotNull AwfulFragment pageFragment) {
        // I don't know if #isAdded is necessary after calling #instantiateItem (instead of #getItem
        // which just creates a new fragment object), but I'm trying to fix a bug I can't reproduce
        // where these fragment methods crash because they have no activity yet
        Timber.d("onPageChanged: page %s fragment %s", pageNum, pageFragment);
        updateTitle();
        if (pageFragment.isAdded()) {
            setProgress(pageFragment.getProgressPercent());
        }
    }

// TODO: 18/01/2018 rework this so the title gets set when a) a fragment updates (send an event) or b) the pager changes, and the activity can decide what to do (maybe a combo title for tablet mode) - right now it's called multiple times
    // TODO: 18/01/2018 also if you have a thread open, and you go back to Bookmarks and then select Bookmarks from the nav drawer, the title is set by the thread fragment (viewpager page doesn't change so that doesn't fire the usual update)
    // TODO: ALSO it depends on whether the open thread is listed on the current Bookmarks page (think it's the update to the thread data triggering a thread fragment reload and pushing the title)
//    @Override
//    public void setActionbarTitle(@NotNull String aTitle) {
//        if (requester != null && requester instanceof AwfulFragment) {
//            //This will only honor the request if the requestor is the currently active view.
//            if (forumsPager.isFragmentVisible((AwfulFragment) requester)) {
//                super.setActionbarTitle(aTitle);
//            } else {
//                if (Companion.getDEBUG()) Log.i(TAG, "Failed setActionbarTitle: " + aTitle + " - " + requester.toString());
//            }
//        } else {
//            super.setActionbarTitle(aTitle);
//        }
//    }

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
    protected void onActivityResult(int request, int result, Intent intent) {
        super.onActivityResult(request, result, intent);
        if (request == Constants.LOGIN_ACTIVITY_REQUEST && result == Activity.RESULT_OK) {
            SyncManager.sync(this);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        AwfulFragment pagerItem = forumsPager.getCurrentFragment();
        if (getMPrefs().volumeScroll && pagerItem != null && pagerItem.attemptVolumeScroll(event)) {
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
        forumsPager.onConfigurationChange(getMPrefs());
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
