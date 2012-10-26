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

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ViewGroup;

import com.actionbarsherlock.view.MenuItem;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulURL;
import com.ferg.awfulapp.thread.AwfulURL.TYPE;
import com.ferg.awfulapp.widget.AwfulFragmentPagerAdapter;
import com.ferg.awfulapp.widget.AwfulFragmentPagerAdapter.AwfulPagerFragment;
import com.ferg.awfulapp.widget.AwfulViewPager;

public class ForumsIndexActivity extends AwfulActivity {

    private boolean DEVELOPER_MODE = false;
    private boolean DEBUG = false;
    private static final String TAG = "ForumsIndexActivity";

    private boolean mSecondPane;
    private ForumsIndexFragment mIndexFragment = null;
    private ForumDisplayFragment mForumFragment = null;
    private ThreadDisplayFragment mThreadFragment = null;
    private PostReplyFragment mReplyFragment = null;
    private boolean skipLoad = false;
    private AwfulURL url = new AwfulURL();
    
    private Handler mHandler = new Handler();
    
    
    private AwfulViewPager mViewPager;
    private ForumPagerAdapter pagerAdapter;
    
    private int mForumId = 0;
    private int mForumPage = 1;
    private int mThreadId = 0;
    private int mThreadPage = 1;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
        	mForumId = savedInstanceState.getInt(Constants.FORUM_ID,0);
        	mForumPage = savedInstanceState.getInt(Constants.FORUM_PAGE,1);
        	mThreadId = savedInstanceState.getInt(Constants.THREAD_ID,0);
        	mThreadPage = savedInstanceState.getInt(Constants.THREAD_PAGE,1);
        }else{
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
       				mForumId = getIntent().getIntExtra(Constants.FORUM_ID, 0);
                    mForumPage = getIntent().getIntExtra(Constants.FORUM_PAGE, 1);
                    mThreadId = getIntent().getIntExtra(Constants.THREAD_ID, 0);
                    mThreadPage = getIntent().getIntExtra(Constants.THREAD_PAGE, 1);
                    if(mForumId == 2){//workaround for old userCP ID, ugh.
                    	mForumId = Constants.USERCP_ID;//should never have used 2 as a hard-coded forum-id, what a horror.
                    }
        		}
        	}else{
                mForumId = getIntent().getIntExtra(Constants.FORUM_ID, 0);
                mForumPage = getIntent().getIntExtra(Constants.FORUM_PAGE, 1);
                mThreadId = getIntent().getIntExtra(Constants.THREAD_ID, 0);
                mThreadPage = getIntent().getIntExtra(Constants.THREAD_PAGE, 1);
                if(mForumId == 2){//workaround for old userCP ID, ugh.
                	mForumId = Constants.USERCP_ID;//should never have used 2 as a hard-coded forum-id, what a horror.
                }
        	}
        }
        
        if(mForumId < 1){
        	skipLoad = true;
        }
        
        setContentView(R.layout.forum_index_activity);
        mSecondPane = (findViewById(R.id.content)!= null);
        setSupportProgressBarIndeterminateVisibility(false);
        
        if(isDualPane()){
	        mIndexFragment = (ForumsIndexFragment) getSupportFragmentManager().findFragmentById(R.id.forums_index);
	        if(mForumId > 0){
	        	setContentPane(mForumId, mForumPage);
	        }
        	if(url.getType() == TYPE.THREAD || url.isRedirect()){
        		startActivity(new Intent(this, ThreadDisplayActivity.class).setData(getIntent().getData()));
        	}else if(mThreadId > 0){
        		startActivity(new Intent(this, ThreadDisplayActivity.class).putExtra(Constants.THREAD_ID, mThreadId).putExtra(Constants.THREAD_PAGE, mThreadPage));
        	}
        }else{
        	mViewPager = (AwfulViewPager) findViewById(R.id.forum_index_pager);
        	mViewPager.setOffscreenPageLimit(2);
        	pagerAdapter = new ForumPagerAdapter(getSupportFragmentManager());
        	pagerAdapter.addFragment(ForumsIndexFragment.newInstance());
        	pagerAdapter.addFragment(ForumDisplayFragment.newInstance(mForumId, mForumPage, skipLoad));
        	if(url.isRedirect()){
        		pagerAdapter.addFragment(new ThreadDisplayFragment());
        	}else if(mThreadId > 0){
        		pagerAdapter.addFragment(ThreadDisplayFragment.newInstance(mThreadId, mThreadPage));
        	}
	        mViewPager.setAdapter(pagerAdapter);
	        mViewPager.setOnPageChangeListener(pagerAdapter);
	        if(mForumId > 0){
	        	mViewPager.setCurrentItem(1);
	        }else{
	        	mForumId = Constants.USERCP_ID;
	        }
	        if(mThreadId > 0 || url.isRedirect()){
	        	mViewPager.setCurrentItem(2);
	        }
	        Uri data = getIntent().getData();
	        if(data != null && data.getLastPathSegment() != null && (data.getLastPathSegment().contains("usercp") || data.getLastPathSegment().contains("forumdisplay") || data.getLastPathSegment().contains("bookmarkthreads"))){
	        	mViewPager.setCurrentItem(1);
	        }
        }
        if(mIndexFragment != null && mForumId > 0){
        	mIndexFragment.setSelected(mForumId);
        }
        
        setActionBar();
        
        checkIntentExtras();
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
            	setContentPane(Constants.USERCP_ID, 1);
            }
        }
    }
    
    public class ForumPagerAdapter extends AwfulFragmentPagerAdapter implements AwfulViewPager.OnPageChangeListener{
    	public ForumPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		private AwfulPagerFragment visible;
		

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			if(DEBUG) Log.i(TAG,"INSTANTIATING TAB:"+position);
			Fragment frag = (Fragment) super.instantiateItem(container, position);
			if(frag instanceof ForumsIndexFragment){
				mIndexFragment = (ForumsIndexFragment) frag;
			}
			if(frag instanceof ForumDisplayFragment){
				mForumFragment = (ForumDisplayFragment) frag;
			}
			if(frag instanceof ThreadDisplayFragment){
				mThreadFragment = (ThreadDisplayFragment) frag;
			}
			if(frag instanceof PostReplyFragment){
				mReplyFragment = (PostReplyFragment) frag;
			}
			return frag;
		}

		@Override
		public void onPageSelected(int arg0) {
			if(DEBUG) Log.i(TAG,"onPageSelected: "+arg0);
			if(visible != null){
				visible.onPageHidden();
			}
			AwfulPagerFragment apf = getItem(arg0);
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
		protected Fragment resolveConflict(int position, Fragment oldFrag, Fragment newFrag) {
			return newFrag;//just dump the old fragment and replace it
		}
    	
    }
    
    @Override
	public void setActionbarTitle(String aTitle, Object requestor) {
    	if(requestor != null && mViewPager != null){
    		//This will only honor the request if the requestor is the currently active view.
    		if(pagerAdapter.getItem(mViewPager.getCurrentItem()).equals(requestor)){
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
    		if(!pagerAdapter.getItem(mViewPager.getCurrentItem()).onBackPressed()){
    			mViewPager.setCurrentItem(mViewPager.getCurrentItem()-1);
    		}
    	}else{
    		finish();
    	}
    }

    public boolean isDualPane() {
        return mSecondPane;
    }
    
    @Override
    public void displayForum(int id, int page){
    	setContentPane(id, page);
    	if (mViewPager != null) {
    		closeTempWindows();
    		mViewPager.setCurrentItem(1);
        }
    }

    @Override
	public void displayQuickBrowser(String url) {
    	if(mViewPager != null){
    		AwfulPagerFragment apf = pagerAdapter.getItem(pagerAdapter.getCount()-1);
    		if(apf instanceof AwfulWebFragment){
    			((AwfulWebFragment) apf).loadUrl(url);
    			mViewPager.setCurrentItem(pagerAdapter.getCount()-1);
    		}else{
    			pagerAdapter.addFragment(AwfulWebFragment.newInstance(url));
    			mViewPager.setCurrentItem(pagerAdapter.getCount()-1);
    		}
    	}else{
    		super.displayQuickBrowser(url);
    	}
	}

    /*revert slider reply window I guess.
	public void displayReplyWindow(int threadId, int postId, int type) {
    	if(mViewPager != null){
    		if(mReplyFragment != null){
    			//TODO multiquote stuff
    			//mReplyFragment.multiQuote(postId);
    			mReplyFragment.newReply(threadId, postId, type);
    			pagerAdapter.addFragment(mReplyFragment);
    			Log.e(TAG,"Reusing existing reply: "+threadId+" - "+postId+" - "+ type);
    		}else{
    	    	Bundle args = new Bundle();
    	        args.putInt(Constants.THREAD_ID, threadId);
    	        args.putInt(Constants.EDITING, type);
    	        args.putInt(Constants.POST_ID, postId);
    			Log.e(TAG,"New reply: "+threadId+" - "+postId+" - "+ type);
    			pagerAdapter.addFragment(PostReplyFragment.newInstance(args));
    		}
    		mHandler.post(new Runnable(){
				@Override
				public void run() {
					//so it seems if you setCurrentItem() while an ActionbarSherlock submenu is visible, you'll crash. good to know.
	    			mViewPager.setCurrentItem(pagerAdapter.getCount()-1);
				}
    		});
    	}else{
    		super.displayReplyWindow(threadId, postId, type);
    	}
	}
     */
    
    private void closeTempWindows(){
    	if(mViewPager != null){
    		for(int ix=0; ix<pagerAdapter.getCount(); ix++){
    			if(pagerAdapter.getItem(ix) instanceof ThreadDisplayFragment){//close anything after the thread display fragment
    				while(pagerAdapter.getCount() > ix+1){
    					pagerAdapter.deletePage(ix+1);
    				}
    			}
    		}
    	}
    }
    
	@Override
	public void fragmentClosing(AwfulFragment fragment) {
		if(pagerAdapter != null){
			pagerAdapter.deleteFragment(fragment);
		}
		if(fragment instanceof PostReplyFragment && mThreadFragment != null){
			mThreadFragment.onActivityResult(PostReplyFragment.REQUEST_POST, 0, null);
		}
	}


	@Override
	public boolean isFragmentVisible(AwfulFragment awfulFragment) {
		if(awfulFragment != null && mViewPager != null && pagerAdapter != null){
			return awfulFragment.equals(pagerAdapter.getItem(mViewPager.getCurrentItem())); 
		}
		return true;
	}


	public void setContentPane(int aForumId, int aPage) {
    	mForumId = aForumId;
        if(mForumFragment == null && isDualPane()){
            ForumDisplayFragment fragment = ForumDisplayFragment.newInstance(aForumId, aPage, false);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        	transaction.add(R.id.content, fragment);
            transaction.commit();
        	mForumFragment = fragment;
        }else if(mForumFragment != null){
        	mForumFragment.openForum(aForumId, aPage);
        }
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
    		closeTempWindows();
    		mThreadId = id;
    		mThreadPage = page;
    		if(mThreadFragment != null){
    			mThreadFragment.openThread(id, page);
    		}else{
    			pagerAdapter.addFragment(ThreadDisplayFragment.newInstance(id, page));
    		}
    		mViewPager.setCurrentItem(2);
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
	public void fragmentMessage(String type, String contents) {
		if(pagerAdapter != null){
			for(AwfulPagerFragment f : pagerAdapter){
				f.fragmentMessage(type, contents);
			}
		}else{
			if(mIndexFragment != null){
				mIndexFragment.fragmentMessage(type, contents);
			}
			if(mForumFragment != null){
				mForumFragment.fragmentMessage(type, contents);
			}
		}
	}
    
    
}

