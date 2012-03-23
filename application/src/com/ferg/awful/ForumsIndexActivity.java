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

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;

import com.actionbarsherlock.app.ActionBar;
import com.ferg.awful.constants.Constants;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class ForumsIndexActivity extends AwfulActivity {

    private boolean DEVELOPER_MODE = false;
    private static final String TAG = "ForumsIndexActivity";

    private boolean mContent;
    private ForumsIndexFragment mIndexFragment = null;
    private ForumDisplayFragment mFragment = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (DEVELOPER_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        
        new Thread(new Runnable() {
            public void run() {
                GoogleAnalyticsTracker.getInstance().trackPageView("/ForumsIndexActivity");
                GoogleAnalyticsTracker.getInstance().dispatch();
            }
        }).start();
        if (isTV()) {
            startTVActivity();
        }else{
	        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	        setContentView(R.layout.forum_index_activity);
	
	        mContent = (findViewById(R.id.content)!= null);
	        mIndexFragment = (ForumsIndexFragment) getSupportFragmentManager().findFragmentById(R.id.forums_index);
	
	        setActionBar();
	
	        checkIntentExtras();
        }
    }

    private void setActionBar() {
        ActionBar action = getSupportActionBar();
        action.setBackgroundDrawable(getResources().getDrawable(R.drawable.bar));
    }

    private void checkIntentExtras() {
        if (getIntent().hasExtra(Constants.SHORTCUT)) {
            if (getIntent().getBooleanExtra(Constants.SHORTCUT, false)) {
            	if(isDualPane()){
            		setContentPane(Constants.USERCP_ID);
            	}else{
            		mIndexFragment.displayUserCP();
            	}
            }
        }
    }

    public boolean isDualPane() {
        return mContent;
    }

    public void setContentPane(int aForumId) {
        ForumDisplayFragment fragment = 
            ForumDisplayFragment.newInstance(aForumId);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if(mFragment == null){
        	transaction.add(R.id.content, fragment);
        }else{
        	transaction.remove(mFragment);
        	transaction.add(R.id.content, fragment);
        }
    	mFragment = fragment;
        transaction.commit();
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
}

