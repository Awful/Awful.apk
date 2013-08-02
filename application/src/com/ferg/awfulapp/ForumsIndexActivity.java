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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulURL;

public class ForumsIndexActivity extends AwfulActivity {
    protected static String TAG = "ForumsIndexActivity";

    private ForumsIndexFragment mIndexFragment = null;
    private ForumDisplayFragment mForumFragment = null;
    private ThreadDisplayFragment mThreadFragment = null;
    private PostReplyFragment mReplyFragment = null;
    private boolean skipLoad = false;
    private boolean isTablet;
    private AwfulURL url = new AwfulURL();
    
    private Handler mHandler = new Handler();
    
    
    private ViewPager mViewPager;
    private ForumPagerAdapter pagerAdapter;
    
    private int mForumId = Constants.USERCP_ID;
    private int mForumPage = 1;
    private int mThreadId = 0;
    private int mThreadPage = 1;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        isTablet = Constants.isWidescreen(this);
        int initialPage = -1;
        if(savedInstanceState != null){
        	mForumId = savedInstanceState.getInt(Constants.FORUM_ID, mForumId);
        	mForumPage = savedInstanceState.getInt(Constants.FORUM_PAGE, mForumPage);
        	mThreadId = savedInstanceState.getInt(Constants.THREAD_ID,0);
        	mThreadPage = savedInstanceState.getInt(Constants.THREAD_PAGE,1);
        }else{
        	initialPage = parseNewIntent(getIntent());
        }
        
        setContentView(R.layout.forum_index_activity);
        setSupportProgressBarIndeterminateVisibility(false);
        
    	mViewPager = (ViewPager) findViewById(R.id.forum_index_pager);
    	mViewPager.setOffscreenPageLimit(2);
    	pagerAdapter = new ForumPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(pagerAdapter);
        mViewPager.setOnPageChangeListener(pagerAdapter);
        if(initialPage >= 0){
        	mViewPager.setCurrentItem(initialPage);
        }
        
        setActionBar();
        
        checkIntentExtras();
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
			}else if(mThreadFragment.getThreadId() != mThreadId || mThreadFragment.getPage() != mThreadPage){
				mThreadFragment.openThread(mThreadId, mThreadPage);
			}
		}
	}
	
	private int parseNewIntent(Intent intent){
        int initialPage = -1;
		mForumId = getIntent().getIntExtra(Constants.FORUM_ID, mForumId);
        mForumPage = getIntent().getIntExtra(Constants.FORUM_PAGE, mForumPage);
        mThreadId = getIntent().getIntExtra(Constants.THREAD_ID, mThreadId);
        mThreadPage = getIntent().getIntExtra(Constants.THREAD_PAGE, mThreadPage);
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
        			mThreadId = (int) url.getId();
        			mThreadPage = (int) url.getPage();
    			}
    			break;
    		case POST:
    			break;
   			default:
    		}
    	}
        if(intent.getIntExtra(Constants.FORUM_ID,0) > 1 || url.isForum()){
        	initialPage = isTablet? 0 : 1;
        }else{
        	skipLoad = true;
        }
        if(intent.getIntExtra(Constants.THREAD_ID,0) > 0 || url.isRedirect() || url.isThread()){
        	initialPage = isTablet? 1 : 2;
        }
        return initialPage;
	}

    @Override
    protected void onResume() {
        super.onResume();
        switch(mPrefs.alertIDShown+1){
            case 1:
                if(Constants.isHoneycomb()){
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
                }
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
        if(mThreadFragment != null){
            mThreadId = mThreadFragment.getThreadId();
            mThreadPage = mThreadFragment.getPage();
        }
    }

    @Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(mForumFragment != null){
			outState.putInt(Constants.FORUM_ID, mForumFragment.getForumId());
			outState.putInt(Constants.FORUM_PAGE, mForumFragment.getPage());
		}
		if(mThreadFragment != null){
			outState.putInt(Constants.THREAD_ID, mThreadFragment.getThreadId());
			outState.putInt(Constants.THREAD_PAGE, mThreadFragment.getPage());
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
        mThreadId = threadId;
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
				setLoadProgress(apf.getProgressPercent());
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
        public int getCount() {
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
        mForumId = id;
        mForumPage = page;
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
			return pagerAdapter.getItemPosition(awfulFragment) == mViewPager.getCurrentItem();
		}
		return true;
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            	if(mViewPager != null && mViewPager.getCurrentItem() > 0){
            		if(mViewPager.getCurrentItem() == 2 && mThreadFragment != null && mThreadFragment.getParentForumId() > 0){
            			displayForum(mThreadFragment.getParentForumId(), 1);
            		}else{
            			mViewPager.setCurrentItem(mViewPager.getCurrentItem()-1);
            		}
                    return true;
            	}
            	break;
    		default:
    			break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void displayThread(int id, int page, int forumId, int forumPg){
    	if(mViewPager != null){
    		mThreadId = id;
    		mThreadPage = page;
    		if(mThreadFragment != null){
    			mThreadFragment.openThread(id, page);
    		}
    		mViewPager.setCurrentItem(pagerAdapter.getItemPosition(mThreadFragment));
    	}else{
    		super.displayThread(id, page, forumId, forumPg);
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
		if(mThreadFragment != null){
			mThreadFragment.onActivityResult(request, result, intent);
		}
	}
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	// TODO Auto-generated method stub
    	if(mPrefs.volumeScroll && pagerAdapter.getItem(mViewPager.getCurrentItem()) != null && ((AwfulFragment)pagerAdapter.getItem(mViewPager.getCurrentItem())).volumeScroll(event)){
    		return true;
    	}
    	return super.dispatchKeyEvent(event);
    }
}

