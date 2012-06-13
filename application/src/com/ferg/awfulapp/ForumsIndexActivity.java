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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ViewGroup;

import com.actionbarsherlock.view.MenuItem;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.widget.AwfulFragmentPagerAdapter;
import com.ferg.awfulapp.widget.AwfulFragmentPagerAdapter.AwfulPagerFragment;
import com.ferg.awfulapp.widget.AwfulViewPager;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class ForumsIndexActivity extends AwfulActivity {

    private boolean DEVELOPER_MODE = false;
    private static final String TAG = "ForumsIndexActivity";

    private boolean mSecondPane;
    private ForumsIndexFragment mIndexFragment = null;
    private ForumDisplayFragment mForumFragment = null;
    private ThreadDisplayFragment mThreadFragment = null;
    private boolean skipLoad = false;
    
    
    private AwfulViewPager mViewPager;
    private ForumPagerAdapter pagerAdapter;
    
    private int mForumId = 0;
    private int mThreadId = 0;
    private int mThreadPage = 1;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mForumId = getIntent().getIntExtra(Constants.FORUM_ID, 0);
        mThreadId = getIntent().getIntExtra(Constants.THREAD_ID, 0);
        mThreadPage = getIntent().getIntExtra(Constants.THREAD_PAGE, 1);
        
        if(mForumId < 1){
        	skipLoad = true;
        }
        
        setContentView(R.layout.forum_index_activity);
        mSecondPane = (findViewById(R.id.content)!= null);
        setSupportProgressBarIndeterminateVisibility(false);
        
        if(isDualPane()){
	        mIndexFragment = (ForumsIndexFragment) getSupportFragmentManager().findFragmentById(R.id.forums_index);
	        if(mForumId > 0){
	        	setContentPane(mForumId);
	        }
        }else{
        	mViewPager = (AwfulViewPager) findViewById(R.id.forum_index_pager);
        	mViewPager.setOffscreenPageLimit(2);
        	pagerAdapter = new ForumPagerAdapter(getSupportFragmentManager());
        	pagerAdapter.addFragment(ForumsIndexFragment.newInstance());
        	pagerAdapter.addFragment(ForumDisplayFragment.newInstance(mForumId, skipLoad));
        	pagerAdapter.addFragment(ThreadDisplayFragment.newInstance(mThreadId, mThreadPage));
	        mViewPager.setAdapter(pagerAdapter);
	        mViewPager.setOnPageChangeListener(pagerAdapter);
	        if(mForumId > 0){
	        	mViewPager.setCurrentItem(1);
	        }else{
	        	mForumId = Constants.USERCP_ID;
	        }
	        if(mThreadId > 0){
	        	mViewPager.setCurrentItem(2);
	        }
	        Uri data = getIntent().getData();
	        if(data != null && (data.getLastPathSegment().contains("usercp") || data.getLastPathSegment().contains("forumdisplay") || data.getLastPathSegment().contains("bookmarkthreads"))){
	        	mViewPager.setCurrentItem(1);
	        }
        }
        if(mIndexFragment != null && mForumId > 0){
        	mIndexFragment.setSelected(mForumId);
        }
        
        setActionBar();
        
        checkIntentExtras();
    }


	private void checkIntentExtras() {
        if (getIntent().hasExtra(Constants.SHORTCUT)) {
            if (getIntent().getBooleanExtra(Constants.SHORTCUT, false)) {
            	setContentPane(Constants.USERCP_ID);
            }
        }
    }
    
    public class ForumPagerAdapter extends AwfulFragmentPagerAdapter implements AwfulViewPager.OnPageChangeListener{
    	private ArrayList<AwfulPagerFragment> fragList;
    	private AwfulPagerFragment visible;
		public ForumPagerAdapter(FragmentManager fm) {
			super(fm);
			fragList = new ArrayList<AwfulPagerFragment>(3);
		}
		
		public void addFragment(AwfulPagerFragment frag){
			fragList.add(frag);
			notifyDataSetChanged();
		}

		@Override
		public AwfulPagerFragment getItem(int position) {
			return fragList.get(position);
		}
		
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			Log.e(TAG,"INSTANTIATING TAB:"+position);
			Fragment frag = (Fragment) super.instantiateItem(container, position);
			switch(position){
			case 0:
				mIndexFragment = (ForumsIndexFragment) frag;
				break;
			case 1:
				mForumFragment = (ForumDisplayFragment) frag;
				break;
			case 2:
				mThreadFragment = (ThreadDisplayFragment) frag;
				break;
			default:
				Log.e(TAG,"TAB COUNT OUT OF BOUNDS");
			}
			return frag;
		}

		@Override
		public int getItemPosition(Object object) {
			int pos = fragList.indexOf(object);
			if(pos < 0){
				return AwfulFragmentPagerAdapter.POSITION_NONE;
			}else{
				return pos;
			}
		}

		@Override
		public int getCount() {
			return fragList.size();
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageSelected(int arg0) {
			if(visible != null){
				visible.onPageHidden();
			}
			AwfulPagerFragment apf = getItem(arg0);
			if(apf != null){
				setActionbarTitle(apf.getTitle(), null);
				apf.onPageVisible();
			}
			visible = apf;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			// TODO Auto-generated method stub
			super.destroyItem(container, position, object);
			Log.e(TAG,"DESTROY TAB: "+position);
		}
    	
    }
    
    @Override
	public void setActionbarTitle(String aTitle, Object requestor) {
    	if(requestor != null && mViewPager != null){
    		//This will only honor the request if the requestor is the currently active view.
    		if(pagerAdapter.getItem(mViewPager.getCurrentItem()).equals(requestor)){
		    		super.setActionbarTitle(aTitle, requestor);
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
    	setContentPane(id);
    	if (!isDualPane()) {
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


	public void setContentPane(int aForumId) {
    	mForumId = aForumId;
        if(mForumFragment == null && isDualPane()){
            ForumDisplayFragment fragment = ForumDisplayFragment.newInstance(aForumId, false);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        	transaction.add(R.id.content, fragment);
            transaction.commit();
        	mForumFragment = fragment;
        }else if(mForumFragment != null){
        	mForumFragment.openForum(aForumId, 1);
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

    private void startTVActivity() {
        Intent shim = new Intent(this, ForumsTVActivity.class);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            shim.putExtras(extras);
        }

        startActivity(shim);
        finish();
    }
    
    @Override
    public void displayThread(int id, int page, int forumId, int forumPg){
    	if(mViewPager != null){
    		mThreadId = id;
    		mThreadPage = page;
    		if(mThreadFragment != null){
    			mThreadFragment.openThread(id, page);
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
    
}

