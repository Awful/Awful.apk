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
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.ferg.awfulapp.announcements.AnnouncementsManager;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.dialog.ChangelogDialog;
import com.ferg.awfulapp.dialog.LogOutDialog;
import com.ferg.awfulapp.messages.PmManager;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.Keys;
import com.ferg.awfulapp.preferences.SettingsActivity;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.sync.SyncManager;
import com.ferg.awfulapp.thread.AwfulURL;
import com.ferg.awfulapp.util.AwfulUtils;
import com.ferg.awfulapp.widget.ToggleViewPager;

import java.util.Locale;

//import com.ToxicBakery.viewpager.transforms.*;

public class ForumsIndexActivity extends AwfulActivity
        implements PmManager.Listener, AnnouncementsManager.AnnouncementListener {
    protected static final String TAG = "ForumsIndexActivity";

    private static final int DEFAULT_HIDE_DELAY = 300;

    private static final int MESSAGE_HIDING = 0;
    private static final int MESSAGE_VISIBLE_CHANGE_IN_PROGRESS = 1;
    private ForumsIndexFragment mIndexFragment = null;

    private ForumDisplayFragment mForumFragment = null;
    private ThreadDisplayFragment mThreadFragment = null;
    private boolean skipLoad = false;
    private boolean isTablet;
    private AwfulURL url = new AwfulURL();

    private ToggleViewPager mViewPager;

    private View mDecorView;
    private ForumPagerAdapter pagerAdapter;
    private Toolbar mToolbar;
    private NavigationDrawer navigationDrawer;

    // TODO: 15/01/2018 move these into the respective fragments
    public static final int NULL_FORUM_ID = 0;
    public static final int NULL_THREAD_ID = 0;
    private static final int NULL_PAGE_ID = -1;

    private static final int FORUM_LIST_FRAGMENT_POSITION = 0;
    private static final int THREAD_LIST_FRAGMENT_POSITION = 1;
    private static final int THREAD_VIEW_FRAGMENT_POSITION = 2;

    // TODO: 15/01/2018 remove these state variables, ask the fragments for their current state instead
private volatile int mForumId       = Constants.USERCP_ID;
    private volatile int mForumPage     = 1;
    private volatile int mThreadId      = NULL_THREAD_ID;
    private volatile int mThreadPage    = 1;

    private GestureDetector mImmersionGestureDetector = null;
    private boolean mIgnoreFling;
    private String mThreadPost="";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.e(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forum_index_activity);
        mViewPager = findViewById(R.id.forum_index_pager);
        mToolbar = findViewById(R.id.awful_toolbar);
        setSupportActionBar(mToolbar);
        setActionBar();
        navigationDrawer = new NavigationDrawer(this, mToolbar, mPrefs);
        updateNavigationDrawer();

        isTablet = AwfulUtils.isTablet(this);
        int initialPage;
        if (savedInstanceState != null) {
            initialPage = savedInstanceState.getInt("viewPage", NULL_PAGE_ID);
        } else {
            initialPage = parseNewIntent(getIntent());
        }


        mViewPager.setSwipeEnabled(!mPrefs.lockScrolling);
        if (!isTablet && AwfulUtils.isAtLeast(Build.VERSION_CODES.JELLY_BEAN_MR1) && !mPrefs.transformer.equals("Disabled")) {
            mViewPager.setPageTransformer(true, AwfulUtils.getViewPagerTransformer());
        }
        mViewPager.setOffscreenPageLimit(2);
        if (isTablet) {
            mViewPager.setPageMargin(1);
            //TODO what color should it use here?
            mViewPager.setPageMarginDrawable(new ColorDrawable(ColorProvider.ACTION_BAR.getColor()));
        }
        pagerAdapter = new ForumPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(pagerAdapter);
        mViewPager.setOnPageChangeListener(pagerAdapter);
        if (initialPage >= 0) {
            mViewPager.setCurrentItem(initialPage);
        }

        // TODO: 16/01/2018 this is a hack to instantiate the fragment early and get the heavy stuff set up - probably better to do it somewhere else (viewpager?)
        pagerAdapter.getItem(THREAD_VIEW_FRAGMENT_POSITION);

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
            if (mThreadFragment != null) {
                navigationDrawer.setCurrentForumAndThread(mThreadFragment.getParentForumId(), mThreadFragment.getThreadId());
            } else if (mForumFragment != null) {
                navigationDrawer.setCurrentForumAndThread(mForumFragment.getForumId(), null);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment events
    ///////////////////////////////////////////////////////////////////////////

    // TODO: 15/01/2018 refactor so the individual components send events to the activity, instead of directly interacting with each other

    void onThreadChange() {
        if (navigationDrawer != null) {
            mThreadId = mThreadFragment.getThreadId();
            mThreadPage = mThreadFragment.getPageNumber();
            int parentForumId = mThreadFragment.getParentForumId();
            navigationDrawer.setCurrentForumAndThread(parentForumId, mThreadId);
        }
        if (pagerAdapter != null) {
            pagerAdapter.notifyDataSetChanged();
        }
    }

    void onForumChange() {
        if (navigationDrawer != null) {
            int forumId = mForumFragment.getForumId();
            navigationDrawer.setCurrentForumAndThread(forumId, null);
        }
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
        if (mThreadFragment.getThreadId() != expectedThreadId) {
            displayThread(expectedThreadId, ThreadDisplayFragment.FIRST_PAGE, parentForumId, ForumDisplayFragment.FIRST_PAGE, true);
        } else {
            mViewPager.setCurrentItem(THREAD_VIEW_FRAGMENT_POSITION);
        }
    }


    /**
     * Page to the forum/threadlist view. If it's not currently showing the expected forum, load the first page of it.
     * @param expectedForumId the forum that should be shown
     */
    public void showForumView(int expectedForumId) {
        if (mForumFragment.getForumId() != expectedForumId) {
            displayForum(expectedForumId, ForumDisplayFragment.FIRST_PAGE);
        } else {
            mViewPager.setCurrentItem(THREAD_LIST_FRAGMENT_POSITION);
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
        if (mViewPager != null && pagerAdapter != null && pagerAdapter.getCount() >= initialPage && initialPage >= 0) {
            mViewPager.setCurrentItem(initialPage);
        }
        if (mForumFragment != null) {
            mForumFragment.openForum(mForumId, mForumPage);
        }
        if (mThreadFragment != null) {
            if (url.isThread() || url.isPost()) {
                mThreadFragment.openThread(url);
            } else if (intent.getIntExtra(Constants.THREAD_ID, NULL_THREAD_ID) > 0) {
                if (DEBUG) Log.e(TAG, "else: "+mThreadPost);
                mThreadFragment.openThread(mThreadId, mThreadPage, mThreadPost, false);
            }
        }
    }

    private int parseNewIntent(Intent intent) {
        int initialPage = NULL_PAGE_ID;
        int forumId     = getIntent().getIntExtra(Constants.FORUM_ID, mForumId);
        int forumPage   = getIntent().getIntExtra(Constants.FORUM_PAGE, mForumPage);
        int threadId    = getIntent().getIntExtra(Constants.THREAD_ID, mThreadId);
        int threadPage  = getIntent().getIntExtra(Constants.THREAD_PAGE, mThreadPage);
        mThreadPost = getIntent().getStringExtra(Constants.THREAD_FRAGMENT);

        if (forumId == 2) {//workaround for old userCP ID, ugh. the old id still appears if someone created a bookmark launch shortcut prior to b23
            forumId = Constants.USERCP_ID;//should never have used 2 as a hard-coded forum-id, what a horror.
        }

        boolean displayIndex = false;
        String scheme = (getIntent().getData() == null) ? null : getIntent().getData().getScheme();
        if ("http".equals(scheme) || "https".equals(scheme)) {
            url = AwfulURL.parse(getIntent().getDataString());
            switch (url.getType()) {
                case FORUM:
                    forumId = (int) url.getId();
                    forumPage = (int) url.getPage();
                    break;
                case THREAD:
                    if (!url.isRedirect()) {
                        threadPage = (int) url.getPage();
                        threadId = (int) url.getId();
                    }
                    break;
                case POST:
                    break;
                case INDEX:
                    displayIndex = true;
                    break;
                default:
            }
        }

        mForumId = forumId;
        mForumPage = forumPage;
        mThreadId = threadId;
        mThreadPage = threadPage;

        if (displayIndex) {
            displayForumIndex();
        }
        if (intent.getIntExtra(Constants.FORUM_ID, 0) > 1 || url.isForum()) {
            initialPage = isTablet ? 0 : 1;
        } else {
            skipLoad = !isTablet;
        }
        if (intent.getIntExtra(Constants.THREAD_ID, NULL_THREAD_ID) > 0 || url.isRedirect() || url.isThread()) {
            initialPage = 2;
        }
        return initialPage;
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
        if (mViewPager != null) {
            outState.putInt("viewPage", mViewPager.getCurrentItem());
        }
    }


    private void checkIntentExtras() {
        if (getIntent().hasExtra(Constants.SHORTCUT)) {
            if (getIntent().getBooleanExtra(Constants.SHORTCUT, false)) {
                displayForum(Constants.USERCP_ID, 1);
            }
        }
    }


    public class ForumPagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {
        public ForumPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        private AwfulFragment visible;


        @Override
        public void onPageSelected(int arg0) {
            if (DEBUG) Log.i(TAG, "onPageSelected: " + arg0);
            if (visible != null) {
                visible.onPageHidden();
            }
            AwfulFragment apf = (AwfulFragment) instantiateItem(mViewPager, arg0);
            // I don't know if #isAdded is necessary after calling #instantiateItem (instead of #getItem
            // which just creates a new fragment object), but I'm trying to fix a bug I can't reproduce
            // where these fragment methods crash because they have no activity yet
            if (apf != null && apf.isAdded()) {
                setActionbarTitle(apf.getTitle(), null);
                apf.onPageVisible();
                setProgress(apf.getProgressPercent());
            }
            visible = apf;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case FORUM_LIST_FRAGMENT_POSITION:
                    if (mIndexFragment == null) {
                        mIndexFragment = new ForumsIndexFragment();
                    }
                    return mIndexFragment;
                case THREAD_LIST_FRAGMENT_POSITION:
                    if (mForumFragment == null) {
                        mForumFragment = ForumDisplayFragment.getInstance(mForumId, mForumPage, skipLoad);
                    }
                    return mForumFragment;
                case THREAD_VIEW_FRAGMENT_POSITION:
                    if (mThreadFragment == null) {
                        mThreadFragment = new ThreadDisplayFragment();
                    }
                    return mThreadFragment;
            }
            Log.e(TAG, "ERROR: asked for too many fragments in ForumPagerAdapter.getItem");
            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object frag = super.instantiateItem(container, position);
            switch (position) {
                case FORUM_LIST_FRAGMENT_POSITION:
                    mIndexFragment = (ForumsIndexFragment) frag;
                    break;
                case THREAD_LIST_FRAGMENT_POSITION:
                    mForumFragment = (ForumDisplayFragment) frag;
                    break;
                case THREAD_VIEW_FRAGMENT_POSITION:
                    mThreadFragment = (ThreadDisplayFragment) frag;
                    break;
            }
            return frag;
        }

        @Override
        public int getCount() {
            if (mThreadFragment == null || mThreadFragment.getThreadId() == NULL_THREAD_ID) {
                return 2;
            }
            return 3;
        }

        @Override
        public int getItemPosition(Object object) {
            if (mIndexFragment != null && mIndexFragment.equals(object)) {
                return FORUM_LIST_FRAGMENT_POSITION;
            }
            if (mForumFragment != null && mForumFragment.equals(object)) {
                return THREAD_LIST_FRAGMENT_POSITION;
            }
            if (mThreadFragment != null && mThreadFragment.equals(object)) {
                return THREAD_VIEW_FRAGMENT_POSITION;
            }
            return super.getItemPosition(object);
        }

        @Override
        public float getPageWidth(int position) {
            if (isTablet) {
                switch (position) {
                    case FORUM_LIST_FRAGMENT_POSITION:
                        return 0.4f;
                    case THREAD_LIST_FRAGMENT_POSITION:
                        return 0.6f;
                    case THREAD_VIEW_FRAGMENT_POSITION:
                        return 1f;
                }
            }
            return super.getPageWidth(position);
        }
    }


    @Override
    public void setActionbarTitle(String aTitle, Object requestor) {
        if (requestor != null && mViewPager != null) {
            //This will only honor the request if the requestor is the currently active view.
            if (requestor instanceof AwfulFragment &&  isFragmentVisible((AwfulFragment)requestor)) {
                super.setActionbarTitle(aTitle, requestor);
            } else {
                if (DEBUG)
                    Log.i(TAG, "Failed setActionbarTitle: " + aTitle + " - " + requestor.toString());
            }
        } else {
            super.setActionbarTitle(aTitle, requestor);
        }
    }

    @Override
    public void onBackPressed() {
        if (navigationDrawer.close()) {
            return;
        }
        if (mViewPager != null && mViewPager.getCurrentItem() > 0) {
            if (!((AwfulFragment) pagerAdapter.getItem(mViewPager.getCurrentItem())).onBackPressed()) {
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
            }
        } else {
            super.onBackPressed();
        }

    }

    @Override
    public void displayForum(int id, int page) {
        Log.d(TAG, "displayForum " + id);
        if (mForumFragment != null) {
            mForumFragment.openForum(id, page);
            if (mViewPager != null) {
                mViewPager.setCurrentItem(pagerAdapter.getItemPosition(mForumFragment));
            }
        }
    }


    @Override
    public boolean isFragmentVisible(AwfulFragment awfulFragment) {
        if (awfulFragment != null && mViewPager != null && pagerAdapter != null) {
            if (isTablet) {
                int itemPos = pagerAdapter.getItemPosition(awfulFragment);
                return itemPos == mViewPager.getCurrentItem() || itemPos == mViewPager.getCurrentItem() + 1;
            } else {
                return pagerAdapter.getItemPosition(awfulFragment) == mViewPager.getCurrentItem();
            }
        }
        return false;
    }


    @Override
    public void displayThread(int id, int page, int forumId, int forumPg, boolean forceReload) {
        Log.d(TAG, "displayThread " + id + " " + forumId);
        if (mViewPager != null) {
//            if (mThreadFragment != null) {
//                mThreadFragment.openThread(id, page, null, forceReload);
//            } else {
//                // TODO: 15/01/2018 the fragment should never be null - should be calling a getter that creates it if necessary. The fragment can handle storing the state until it's ready to perform the action
//            }
            // TODO: 16/01/2018 notify data set changed somewhere, preferably where the fragment's created the first time
            mThreadFragment.openThread(id, page, null, forceReload);
            pagerAdapter.notifyDataSetChanged();
            mViewPager.setCurrentItem(THREAD_VIEW_FRAGMENT_POSITION);
        } else {
            super.displayThread(id, page, forumId, forumPg, forceReload);
        }
    }


    @Override
    public void displayUserCP() {
        displayForum(Constants.USERCP_ID, 1);
    }


    @Override
    public void displayForumIndex() {
        if (mViewPager != null) {
            mViewPager.setCurrentItem(0);
        }
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
        AwfulFragment pagerItem = (AwfulFragment) pagerAdapter.getItem(mViewPager.getCurrentItem());
        if (mPrefs.volumeScroll && pagerItem != null && pagerItem.attemptVolumeScroll(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onPreferenceChange(AwfulPreferences prefs,String key) {
        super.onPreferenceChange(prefs, key);
        if (prefs.immersionMode && mDecorView == null) {
            setupImmersion();
        }
        if (mViewPager != null) {
            mViewPager.setSwipeEnabled(!prefs.lockScrolling);
        }
        if (!AwfulUtils.isTablet(this) && AwfulUtils.isAtLeast(Build.VERSION_CODES.JELLY_BEAN_MR1) && !prefs.transformer.equals("Disabled")) {
            mViewPager.setPageTransformer(true, AwfulUtils.getViewPagerTransformer());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.e(TAG,"onConfigurationChanged()");
        boolean oldTab = isTablet;
        isTablet = AwfulUtils.isTablet(this);
        if (oldTab != isTablet && mViewPager != null) {
            if (isTablet) {
                mViewPager.setPageMargin(1);
                //TODO what color should it use here?
                mViewPager.setPageMarginDrawable(new ColorDrawable(ColorProvider.ACTION_BAR.getColor()));
            } else {
                mViewPager.setPageMargin(0);
            }

            mViewPager.setAdapter(pagerAdapter);
        }
        if(mViewPager != null){
            ((ForumPagerAdapter)mViewPager.getAdapter()).getItem(mViewPager.getCurrentItem()).onConfigurationChanged(newConfig);
        }
        navigationDrawer.getDrawerToggle().onConfigurationChanged(newConfig);
    }


    public void preventSwipe() {
        this.mViewPager.setSwipeEnabled(false);
    }

    public void reenableSwipe() {
        runOnUiThread(() -> {
            if (mViewPager.beginFakeDrag()) {
                mViewPager.endFakeDrag();
            }
            mViewPager.setSwipeEnabled(true);
        });
    }


    @Override
    protected void onStop() {
        super.onStop();
    }
}
