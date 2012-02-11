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
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.Log;
import android.view.*;

import android.support.v4.app.FragmentTransaction;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import com.ferg.awful.preferences.AwfulPreferences;

public class ThreadDisplayActivity extends AwfulActivity {
    private static final String TAG = "ThreadDisplayActivities";
    
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
        } else if (!AwfulActivity.useLegacyActionbar()) {
            requestWindowFeature(Window.FEATURE_ACTION_BAR);
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        setContentView(R.layout.thread_display_activity);
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

    public void displayPostReplyDialog() {
        getFragment().displayPostReplyDialog();
    }

    public void refreshThread() {
    	ThreadDisplayFragment frag = getFragment();
    	if(frag != null){
    		getFragment().refresh();
    	}
    }

    private ThreadDisplayFragment getFragment() {
        return (ThreadDisplayFragment) getSupportFragmentManager().findFragmentById(R.id.thread_display_fragment);
    }

    public void refreshInfo() {
    	ThreadDisplayFragment frag = getFragment();
    	if(frag != null){
    		getFragment().refreshInfo();
    	}
	}
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v(TAG,"Orientation change");
    }
}
