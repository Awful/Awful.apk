/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the software nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
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
 *******************************************************************************/

package com.ferg.awfulapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

//import com.ToxicBakery.viewpager.transforms.*;
import com.androidquery.AQuery;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.dialog.LogOutDialog;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.provider.StringProvider;
import com.ferg.awfulapp.thread.AwfulURL;
import com.ferg.awfulapp.util.AwfulUtils;
import com.ferg.awfulapp.widget.DepthPageTransformer;
import com.ferg.awfulapp.widget.ToggleViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.LayoutParams;
import android.widget.TextView;

public class ForumsIndexActivity extends AwfulActivity {
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

    private Handler mHandler = new Handler();

    private ToggleViewPager mViewPager;
    private View mDecorView;
    private ForumPagerAdapter pagerAdapter;

    private Toolbar mToolbar;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private int mNavForumId = Constants.USERCP_ID;
    private int mNavThreadId = 0;
    private int mForumId = Constants.USERCP_ID;
    private int mForumPage = 1;
    private int mThreadId = 0;
    private int mThreadPage = 1;

    private GestureDetector mImmersionGestureDetector = null;
    private boolean mIgnoreFling;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isTablet = AwfulUtils.isWidescreen(this);
        int initialPage = -1;
        if (savedInstanceState != null) {
            mForumId = savedInstanceState.getInt(Constants.FORUM_ID, mForumId);
            mForumPage = savedInstanceState.getInt(Constants.FORUM_PAGE, mForumPage);
            setThreadPage(savedInstanceState.getInt(Constants.THREAD_PAGE,1));
            setThreadId(savedInstanceState.getInt(Constants.THREAD_ID,0));
            initialPage = savedInstanceState.getInt("viewPage",-1);
        } else {
            initialPage = parseNewIntent(getIntent());
        }

        setContentView(R.layout.forum_index_activity);

        mViewPager = (ToggleViewPager) findViewById(R.id.forum_index_pager);
        mViewPager.setSwipeEnabled(!mPrefs.lockScrolling);
        if(!isTablet && !mPrefs.transformer.equals("Disabled")) {
            mViewPager.setPageTransformer(true, AwfulUtils.getViewPagerTransformer());
        }
        mViewPager.setOffscreenPageLimit(2);
        if(isTablet){
            mViewPager.setPageMargin(1);
            //TODO what color should it use here?
            mViewPager.setPageMarginDrawable(new ColorDrawable(ColorProvider.getActionbarColor()));
        }
        pagerAdapter = new ForumPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(pagerAdapter);
        mViewPager.setOnPageChangeListener(pagerAdapter);
        if (initialPage >= 0) {
            mViewPager.setCurrentItem(initialPage);
        }

        mToolbar = (Toolbar) findViewById(R.id.awful_toolbar);
        setSupportActionBar(mToolbar);
        setNavigationDrawer();


        setActionBar();

        checkIntentExtras();

        setupImmersion();
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @SuppressLint("NewApi")
    private void setupImmersion() {
        if (AwfulUtils.isKitKat() && mPrefs.immersionMode) {
            mDecorView = getWindow().getDecorView();

            mDecorView.setOnSystemUiVisibilityChangeListener(
                    new View.OnSystemUiVisibilityChangeListener() {
                        @Override
                        public void onSystemUiVisibilityChange(int flags) {
                            boolean visible = (flags & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
                            if (visible) {
                                // Add some delay so the act of swiping to bring system UI into view doesn't turn it back off
                                mHideHandler.removeMessages(MESSAGE_VISIBLE_CHANGE_IN_PROGRESS);
                                mHideHandler.sendEmptyMessageDelayed(MESSAGE_VISIBLE_CHANGE_IN_PROGRESS, 800);
                                mIgnoreFling = true;
                            }
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


    public void setNavigationDrawer() {
        final Activity self = this;

        TextView navIndex = (TextView) findViewById(R.id.sidebar_index);
        if(null != navIndex){
            navIndex.setVisibility(View.VISIBLE);
            navIndex.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view){
                    displayForumIndex();
                    mDrawerLayout.closeDrawers();
                }
            });
            navIndex.setText("The SA Forums");
        }

        TextView navForum = (TextView) findViewById(R.id.sidebar_forum);
        if(null != navForum){
            if(mNavForumId != 0) {
                navForum.setVisibility(View.VISIBLE);
                navForum.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view){
                        displayForum(mNavForumId, 1);
                        mDrawerLayout.closeDrawers();
                    }
                });
                navForum.setText(StringProvider.getForumName(this, mNavForumId));
            }else{
                navForum.setVisibility(View.GONE);
            }
        }

        TextView navThread = (TextView) findViewById(R.id.sidebar_thread);
        if(null != navThread){
            if(mNavThreadId != 0) {
                Log.d(TAG,"NavThread, Navforum: "+ mNavThreadId+" "+mNavForumId);
                navThread.setVisibility(View.VISIBLE);
                navThread.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        displayThread(mNavThreadId, mThreadPage, mNavForumId, mForumPage,false);

                        mDrawerLayout.closeDrawers();
                    }
                });
                navThread.setText(StringProvider.getThreadName(this, mNavThreadId));
            }else{
                navThread.setVisibility(View.GONE);
            }
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                mToolbar,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        );
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        TextView username = (TextView) findViewById(R.id.sidebar_username);
        if(null != username){
            username.setText(mPrefs.username);
        }
        ImageView avatar = (ImageView) findViewById(R.id.sidebar_avatar);
        if(null != avatar){
            AQuery aq = new AQuery(this);
            if(null != mPrefs.userTitle) {
                if(!("".equals(mPrefs.userTitle))){
                    aq.id(R.id.sidebar_avatar).image(mPrefs.userTitle);
                }else{
                    aq.id(R.id.sidebar_avatar).image(R.drawable.icon);
                }
            }
        }
        TextView logout = (TextView) findViewById(R.id.sidebar_logout);
        if(null != logout){
            logout.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view){
                    mDrawerLayout.closeDrawers();
                    new LogOutDialog(self).show();
                }
            });
            logout.setText(getResources().getText(R.string.logout));
            logout.setVisibility(View.VISIBLE);
        }
        TextView messages = (TextView) findViewById(R.id.sidebar_pm);
        if(null != messages){
            messages.setEnabled(mPrefs.hasPlatinum);
            messages.setVisibility(mPrefs.hasPlatinum?View.VISIBLE:View.GONE);
            messages.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view){
                    mDrawerLayout.closeDrawers();
                    startActivity(new Intent().setClass(self, PrivateMessageActivity.class));
                }
            });
            messages.setText(getResources().getText(R.string.private_message));
        }
        TextView bookmarks = (TextView) findViewById(R.id.sidebar_bookmarks);
        if(null != bookmarks){
            bookmarks.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view){
                    mDrawerLayout.closeDrawers();
                    startActivity(new Intent().setClass(self, ForumsIndexActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .putExtra(Constants.FORUM_ID, Constants.USERCP_ID)
                            .putExtra(Constants.FORUM_PAGE, 1));
                }
            });
            bookmarks.setText(getResources().getText(R.string.user_cp));
            bookmarks.setVisibility((Constants.USERCP_ID == mNavForumId?View.GONE:View.VISIBLE));
        }
        TextView settings = (TextView) findViewById(R.id.sidebar_settings);
        if(null != settings){
            settings.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view){
                    mDrawerLayout.closeDrawers();
                    startActivity(new Intent().setClass(self, SettingsActivity.class));
                }
            });
        }
        settings.setText(getResources().getText(R.string.settings));
        settings.setVisibility(View.VISIBLE);


        mDrawerToggle.syncState();
    }

    @Override
    @SuppressLint("NewApi")
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (AwfulUtils.isKitKat()&& mPrefs.immersionMode) {
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
        super.onNewIntent(intent); if(DEBUG) Log.e(TAG, "onNewIntent");
        setIntent(intent);
        int initialPage = parseNewIntent(intent);
        if(mViewPager != null && pagerAdapter != null && pagerAdapter.getCount() >= initialPage && initialPage >= 0){
            mViewPager.setCurrentItem(initialPage);
        }
        if(mForumFragment != null){
            mForumFragment.openForum(mForumId, mForumPage);
        }
        if(mThreadFragment != null){
            if(url.isThread() || url.isPost()){
                mThreadFragment.openThread(url);
            }else if(intent.getIntExtra(Constants.THREAD_ID, 0) > 0){
                mThreadFragment.openThread(mThreadId, mThreadPage);
            }
        }
    }

    private int parseNewIntent(Intent intent){
        int initialPage = -1;
        mForumId = getIntent().getIntExtra(Constants.FORUM_ID, mForumId);
        mForumPage = getIntent().getIntExtra(Constants.FORUM_PAGE, mForumPage);
        setThreadPage(getIntent().getIntExtra(Constants.THREAD_PAGE, mThreadPage));
        setThreadId(getIntent().getIntExtra(Constants.THREAD_ID, mThreadId));
        if(mForumId == 2){//workaround for old userCP ID, ugh. the old id still appears if someone created a bookmark launch shortcut prior to b23
            mForumId = Constants.USERCP_ID;//should never have used 2 as a hard-coded forum-id, what a horror.
        }
        if(getIntent().getData() != null && getIntent().getData().getScheme().equals("http")){
            url = AwfulURL.parse(getIntent().getDataString());
            switch(url.getType()){
                case FORUM:
                    mForumId = (int) url.getId();
                    mForumPage = (int) url.getPage();
                    break;
                case THREAD:
                    if(!url.isRedirect()){
                        setThreadPage((int) url.getPage());
                        setThreadId((int) url.getId());
                    }
                    break;
                case POST:
                    break;
                default:
            }
        }
        if(intent.getIntExtra(Constants.FORUM_ID,0) > 1 || url.isForum()){
            initialPage = isTablet ? 0 : 1;
        }else{
            skipLoad = !isTablet;
        }
        if(intent.getIntExtra(Constants.THREAD_ID,0) > 0 || url.isRedirect() || url.isThread()){
            initialPage = 2;
        }
        return initialPage;
    }

    @Override
    protected void onResume() {
        super.onResume();
        switch(mPrefs.alertIDShown+1){
            case 1:
                    new AlertDialog.Builder(this).
                            setTitle(getString(R.string.alert_title_1))
                            .setMessage(getString(R.string.alert_message_1))
                            .setPositiveButton(getString(R.string.alert_ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton(getString(R.string.alert_settings), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    startActivity(new Intent().setClass(ForumsIndexActivity.this, SettingsActivity.class));
                                }
                            })
                            .show();
                mPrefs.setIntegerPreference("alert_id_shown", 1);
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mForumFragment != null){
            mForumId = mForumFragment.getForumId();
            mForumPage = mForumFragment.getPage();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(Constants.FORUM_ID, mForumId);
        outState.putInt(Constants.FORUM_PAGE, mForumPage);
        outState.putInt(Constants.THREAD_ID, mThreadId);
        outState.putInt(Constants.THREAD_PAGE, mThreadPage);
        if(mViewPager != null){
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

    public int getThreadId() {
        return mThreadId;
    }

    public int getThreadPage() {
        return mThreadPage;
    }


    public void setThreadId(int threadId) {
        int oldThreadId = mThreadId;
        mThreadId = threadId;
        mNavThreadId = threadId;
        if((oldThreadId < 1 || threadId < 1) && threadId != oldThreadId && pagerAdapter != null){
            pagerAdapter.notifyDataSetChanged();//notify pager adapter so it'll show/hide the thread view
        }
    }

    public void setThreadPage(int page) {
        mThreadPage = page;
    }

    public class ForumPagerAdapter extends FragmentStatePagerAdapter implements ViewPager.OnPageChangeListener{
        public ForumPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        private AwfulFragment visible;


        @Override
        public void onPageSelected(int arg0) {
            if(DEBUG) Log.i(TAG,"onPageSelected: "+arg0);
            if(visible != null){
                visible.onPageHidden();
            }
            AwfulFragment apf = (AwfulFragment) getItem(arg0);
            if(apf != null){
                setActionbarTitle(apf.getTitle(), null);
                apf.onPageVisible();
                setProgress(apf.getProgressPercent());
            }
            visible = apf;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

        @Override
        public void onPageScrollStateChanged(int state) {}

        @Override
        public Fragment getItem(int ix) {
            switch(ix){
                case 0:
                    if(mIndexFragment == null){
                        mIndexFragment = new ForumsIndexFragment();
                    }
                    return mIndexFragment;
                case 1:
                    if(mForumFragment == null){
                        mForumFragment = new ForumDisplayFragment(mForumId, mForumPage, skipLoad);
                    }
                    return mForumFragment;
                case 2:
                    if(mThreadFragment == null){
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
            if(frag instanceof ForumsIndexFragment){
                mIndexFragment = (ForumsIndexFragment) frag;
            }
            if(frag instanceof ForumDisplayFragment){
                mForumFragment = (ForumDisplayFragment) frag;
            }
            if(frag instanceof ThreadDisplayFragment){
                mThreadFragment = (ThreadDisplayFragment) frag;
            }
            return frag;
        }

        @Override
        public int getCount() {
            if(getThreadId() < 1){
                return 2;
            }
            return 3;
        }

        @Override
        public int getItemPosition(Object object) {
            if(mIndexFragment != null && mIndexFragment.equals(object)){
                return 0;
            }
            if(mForumFragment != null && mForumFragment.equals(object)){
                return 1;
            }
            if(mThreadFragment != null && mThreadFragment.equals(object)){
                return 2;
            }
            return super.getItemPosition(object);
        }

        @Override
        public float getPageWidth(int position) {
            if(isTablet){
                switch(position){
                    case 0:
                        return 0.4f;
                    case 1:
                        return 0.6f;
                    case 2:
                        return 1f;
                }
            }
            return super.getPageWidth(position);
        }
    }


    @Override
    public void setActionbarTitle(String aTitle, Object requestor) {
        if(requestor != null && mViewPager != null){
            //This will only honor the request if the requestor is the currently active view.
            if(requestor instanceof AwfulFragment && isFragmentVisible((AwfulFragment) requestor)){
                super.setActionbarTitle(aTitle, requestor);
            }else{
                if(DEBUG) Log.i(TAG,"Failed setActionbarTitle: "+aTitle+" - "+requestor.toString());
            }
        }else{
            super.setActionbarTitle(aTitle, requestor);
        }
    }

    @Override
    public void onBackPressed() {
        if(mViewPager != null && mViewPager.getCurrentItem() > 0){
            if(!((AwfulFragment)pagerAdapter.getItem(mViewPager.getCurrentItem())).onBackPressed()){
                mViewPager.setCurrentItem(mViewPager.getCurrentItem()-1);
            }
        }else{
            super.onBackPressed();
        }
    }

    @Override
    public void displayForum(int id, int page){
        Log.d(TAG,"displayForum "+ id);
        mForumId = id;
        mForumPage = page;
        setNavigationDrawer();
        if(mForumFragment != null){
            mForumFragment.openForum(id, page);
            if (mViewPager != null) {
                mViewPager.setCurrentItem(pagerAdapter.getItemPosition(mForumFragment));
            }
        }
    }


    @Override
    public boolean isFragmentVisible(AwfulFragment awfulFragment) {
        if(awfulFragment != null && mViewPager != null && pagerAdapter != null){
            if(isTablet){
                int itemPos = pagerAdapter.getItemPosition(awfulFragment);
                return  itemPos == mViewPager.getCurrentItem() || itemPos == mViewPager.getCurrentItem()+1;
            }else{
                return pagerAdapter.getItemPosition(awfulFragment) == mViewPager.getCurrentItem();
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                if(mViewPager != null && mViewPager.getCurrentItem() > 0){
//                    if(mViewPager.getCurrentItem() == 2 && mThreadFragment != null && mThreadFragment.getParentForumId() > 0){
//                        displayForum(mThreadFragment.getParentForumId(), 1);
//                    }else{
//                        mViewPager.setCurrentItem(mViewPager.getCurrentItem()-1);
//                    }
//                    return true;
//                }
//                break;
//            default:
//                break;
//        }
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void displayThread(int id, int page, int forumId, int forumPg, boolean forceReload){
        Log.d(TAG,"displayThread "+ id+" " +forumId);
        if(mViewPager != null){
            if(mThreadFragment != null){
                if(!forceReload && getThreadId() == id && getThreadPage() == page){
                    setThreadId(mNavThreadId);

                    setNavForumId(mThreadFragment.getParentForumId());
                    setNavigationDrawer();
                }else{
                    mThreadFragment.openThread(id, page);
                    mViewPager.getAdapter().notifyDataSetChanged();
                }
            }else{
                setThreadPage(page);
                setThreadId(id);
            }
            mViewPager.setCurrentItem(pagerAdapter.getItemPosition(mThreadFragment));
        }else{
            super.displayThread(id, page, forumId, forumPg, forceReload);
        }
    }


    @Override
    public void displayUserCP() {
        displayForum(Constants.USERCP_ID,1);
    }


    @Override
    public void displayForumIndex() {
        if(mViewPager != null){
            mViewPager.setCurrentItem(0);
        }
    }


    @Override
    protected void onActivityResult(int request, int result, Intent intent) {
        if(DEBUG) Log.e(TAG,"onActivityResult: " + request+" result: "+result);
        super.onActivityResult(request, result, intent);
        if(request == Constants.LOGIN_ACTIVITY_REQUEST && result == Activity.RESULT_OK){
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    if(mIndexFragment != null){
                        mIndexFragment.refresh();
                    }
                }
            }, 1000);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(mPrefs.volumeScroll && pagerAdapter.getItem(mViewPager.getCurrentItem()) != null && ((AwfulFragment)pagerAdapter.getItem(mViewPager.getCurrentItem())).volumeScroll(event)){
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onPreferenceChange(AwfulPreferences prefs) {
        super.onPreferenceChange(prefs);
        if(prefs.immersionMode && mDecorView == null){
            setupImmersion();
        }
        if(mViewPager != null){
            mViewPager.setSwipeEnabled(!prefs.lockScrolling);
        }
        if(mDrawerLayout != null){
            ImageView avatar = (ImageView) findViewById(R.id.sidebar_avatar);
            if(null != avatar){
                AQuery aq = new AQuery(this);
                if(null != mPrefs.userTitle) {
                    aq.id(R.id.sidebar_avatar).image(mPrefs.userTitle);
                }else{
                    aq.id(R.id.sidebar_avatar).image(R.drawable.icon);
                }
            }
        }
        mViewPager.setPageTransformer(true, AwfulUtils.getViewPagerTransformer());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean oldTab = isTablet;
        isTablet = AwfulUtils.isWidescreen(this);
        if(oldTab != isTablet && mViewPager != null){
            if(isTablet){
                mViewPager.setPageMargin(1);
                //TODO what color should it use here?
                mViewPager.setPageMarginDrawable(new ColorDrawable(ColorProvider.getActionbarColor()));
            }else{
                mViewPager.setPageMargin(0);
            }

            mViewPager.setAdapter(pagerAdapter);
        }
        mDrawerToggle.onConfigurationChanged(newConfig);

    }

    public void setNavForumId(int forumId){
        this.mNavForumId = forumId;
    }

    public void setNavThreadId(int threadId){
        this.mNavThreadId = threadId;
    }
}