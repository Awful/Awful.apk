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

package com.ferg.awful;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.ferg.awful.constants.Constants;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class ThreadDisplayActivity extends AwfulActivity implements OnClickListener {
    private static final String TAG = "ThreadDisplayActivities";
    private ForumDisplayFragment sidebarFrag;
    private ThreadDisplayFragment mainWindowFrag;
    private boolean sidebarVisible;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Thread(new Runnable() {
            public void run() {
                GoogleAnalyticsTracker.getInstance().trackPageView("/ThreadDisplayActivity");
                GoogleAnalyticsTracker.getInstance().dispatch();
            }
        }).start();
        
        setActionBar();

        setContentView(R.layout.thread_display_activity);
        mainWindowFrag = getFragment();
        sidebarFrag = (ForumDisplayFragment) getSupportFragmentManager().findFragmentById(R.id.forum_display_fragment);
        sidebarFrag.skipLoad(true);
        findViewById(R.id.sidebar_toggle_button).setOnClickListener(this);
    	FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    	ft.hide(sidebarFrag);
		ft.commit();
		sidebarVisible = false;
        if(!Constants.isWidescreen(this)){
    		findViewById(R.id.sidebar_toggle).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            	navigateUp();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void navigateUp() {
        displayForum(mainWindowFrag.getParentForumId(), 1);
    }

    public void displayPostReplyDialog() {
    	if(mainWindowFrag != null){
    		mainWindowFrag.displayPostReplyDialog();
    	}
    }

    public void refreshThread() {
    	if(mainWindowFrag != null){
    		mainWindowFrag.refresh();
    	}
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	if(mainWindowFrag != null){
    		mainWindowFrag.onCreateOptionsMenu(menu, getSupportMenuInflater());
    		return true;
    	}
		return super.onCreateOptionsMenu(menu);
	}

	private ThreadDisplayFragment getFragment() {
        return (ThreadDisplayFragment) getSupportFragmentManager().findFragmentById(R.id.thread_display_fragment);
    }

    public void refreshInfo() {
    	if(mainWindowFrag != null){
    		mainWindowFrag.refreshInfo();
    	}
	}
    
    public boolean isDualPane(){
    	return Constants.isWidescreen(this);
    }
    
    public void toggleSidebar(){
    	if(isDualPane() && sidebarFrag != null){
    		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    		if(sidebarVisible){
    			ft.hide(sidebarFrag);
    		}else{
    			ft.show(sidebarFrag);
    			sidebarFrag.syncForumsIfStale();
    		}
    		ft.commit();
    		sidebarVisible = !sidebarVisible;
    	}
    }
    
    @Override
	public void setActionbarTitle(String aTitle, AwfulFragment requestor) {
    	if(requestor instanceof ThreadDisplayFragment){//only switch title for the thread, not the sidebar
    		super.setActionbarTitle(aTitle, requestor);
    	}
	}

	@Override
	public void displayThread(int id, int page, int forumId, int forumPage) {
    	if(mainWindowFrag != null){
    		mainWindowFrag.openThread(id, page);
    	}else{
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			mainWindowFrag = ThreadDisplayFragment.newInstance(id, page);
			ft.replace(R.id.thread_display_fragment, mainWindowFrag);
			ft.commit();
    	}
	}
	
	@Override
    public void displayForum(int id, int page){
		if(isDualPane() && sidebarFrag != null){
    		if(!sidebarVisible){
    			toggleSidebar();
    		}
    		sidebarFrag.openForum(id, page);
    	}
    }

	public boolean isSidebarVisible() {
		return sidebarVisible;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if(Constants.isWidescreen(newConfig)){
    		findViewById(R.id.sidebar_toggle).setVisibility(View.VISIBLE);
		}else{
			if(sidebarVisible){
				toggleSidebar();
			}
    		findViewById(R.id.sidebar_toggle).setVisibility(View.GONE);
		}
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.sidebar_toggle_button:
			toggleSidebar();
			break;
		}
	}
	
	
	
}
