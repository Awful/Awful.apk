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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.Log;

import com.ferg.awful.constants.Constants;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class ThreadDisplayActivity extends AwfulActivity {
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
        

        if (isTV()) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setupLeftNavBar(R.layout.thread_action_items, true);
        } else {
            //requestWindowFeature(Window.FEATURE_NO_TITLE);
            requestWindowFeature(Window.FEATURE_ACTION_BAR);
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        }
        setActionBar();

        setContentView(R.layout.thread_display_activity);
        mainWindowFrag = getFragment();
        sidebarFrag = (ForumDisplayFragment) getSupportFragmentManager().findFragmentById(R.id.forum_display_fragment);
        sidebarVisible = sidebarFrag != null;
        if(isDualPane()){
        	//TODO autohide code here
        }
    }

    private void setActionBar() {
        ActionBar action = getSupportActionBar();
        action.setBackgroundDrawable(getResources().getDrawable(R.drawable.bar));
        action.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            	if(isDualPane()){
            		toggleSidebar();
            	}else{
            		navigateUp();
            	}
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void navigateUp() {
        Intent i = new Intent(this, ForumsIndexActivity.class).putExtra(Constants.FORUM_ID, mainWindowFrag.getParentForumId())
        													  .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }

    public void setThreadTitle(String aTitle) {
            ActionBar action = getSupportActionBar();
            action.setTitle(Html.fromHtml(aTitle).toString());
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

    private ThreadDisplayFragment getFragment() {
        return (ThreadDisplayFragment) getSupportFragmentManager().findFragmentById(R.id.thread_display_fragment);
    }

    public void refreshInfo() {
    	if(mainWindowFrag != null){
    		mainWindowFrag.refreshInfo();
    	}
	}
    
    public boolean isDualPane(){
    	return sidebarFrag != null;
    }
    
    public void toggleSidebar(){
    	if(isDualPane() && sidebarFrag != null){
    		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    		if(sidebarVisible){
    			ft.hide(sidebarFrag);
    		}else{
    			ft.show(sidebarFrag);
    		}
    		ft.commit();
    		sidebarVisible = !sidebarVisible;
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
}
