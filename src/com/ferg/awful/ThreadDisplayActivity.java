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

import com.ferg.awful.constants.Constants;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;

import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.service.AwfulServiceConnection.ThreadListAdapter;

public class ThreadDisplayActivity extends AwfulActivity {
    private static final String TAG = "ThreadDisplayActivities";
    private int threadid;//thread id for this activity, since we will only use this activity with a single thread
    private ThreadDisplayFragment display;//no need to juggle fragments here
    private ThreadListAdapter adapt;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        if (!AwfulActivity.useLegacyActionbar()) {
            requestWindowFeature(Window.FEATURE_ACTION_BAR);
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        setContentView(R.layout.thread_display_activity);

        configureAdapter(savedInstanceState);
    }

    private void setActionBar() {
        ActionBar action = getActionBar();
        action.setBackgroundDrawable(getResources().getDrawable(R.drawable.bar));
        action.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!AwfulActivity.useLegacyActionbar()) {
            setActionBar();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                returnHome();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void returnHome() {
        finish();
        Intent i = new Intent(this, ForumsIndexActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    public void setThreadTitle(String aTitle) {
        if (!AwfulActivity.useLegacyActionbar()) {
            ActionBar action = getActionBar();
            action.setTitle(Html.fromHtml(aTitle).toString());
        }
    }

    public void refreshThread() {
        getFragment().refresh();
    }

    protected void configureAdapter(Bundle aSavedState) {
        display = getFragment();
        String c2pThreadID = null;
        String c2pPostPerPage = null;
        String c2pPage = null;
        String c2pURLFragment = null;

        // We may be getting thread info from ChromeToPhone so handle that here
        if (getIntent().getData() != null && getIntent().getData().getScheme().equals("http")) {
            c2pThreadID = getIntent().getData().getQueryParameter("threadid");
            c2pPostPerPage = getIntent().getData().getQueryParameter("perpage");
            c2pPage = getIntent().getData().getQueryParameter("pagenumber");
            c2pURLFragment = getIntent().getData().getEncodedFragment();
        }

        threadid = getIntent().getIntExtra(Constants.THREAD, 0);
        int loadPage = getIntent().getIntExtra(Constants.PAGE, 0);
        if (c2pThreadID != null) {
            threadid = Integer.parseInt(c2pThreadID);
        }

        if (aSavedState != null) {
            adapt = getServiceConnection().createThreadAdapter(threadid, display, display.getSavedPage());
        } else {

            if (c2pPage != null) {
            	int page = Integer.parseInt(c2pPage);
        		AwfulPreferences pref = new AwfulPreferences(this);
            	if(c2pPostPerPage != null && c2pPostPerPage.matches("\\d+")){
            		int ppp = Integer.parseInt(c2pPostPerPage);
            		if(pref.postPerPage != ppp){
            			page = (int) Math.ceil((double)(page*ppp)/pref.postPerPage);
            			Log.e("TDA", "thread request: "+threadid+" ppp:"+ppp+"Loading page: "+page);
            		}
            	}else{
            		if(pref.postPerPage != Constants.ITEMS_PER_PAGE){
            			page = (int) Math.ceil((page*Constants.ITEMS_PER_PAGE)/(double)pref.postPerPage);
            			Log.e("TDA", "thread request: "+threadid+" ppp:40(default) Loading page: "+page);
            		}
            	}
            	if(c2pURLFragment != null && c2pURLFragment.startsWith("post")){
            		display.setPostJump(c2pURLFragment.replaceAll("\\D", ""));
            	}
            	adapt = getServiceConnection().createThreadAdapter(threadid, display, page);
            }else{
            	if(loadPage >0){
            		adapt = getServiceConnection().createThreadAdapter(threadid, display, loadPage);
            	}else{
            		adapt = getServiceConnection().createThreadAdapter(threadid, display);
            	}
            }
        }

        display.setListAdapter(adapt);
    }

    private ThreadDisplayFragment getFragment() {
        return (ThreadDisplayFragment) getSupportFragmentManager().findFragmentById(R.id.thread_fragment);
    }
}
