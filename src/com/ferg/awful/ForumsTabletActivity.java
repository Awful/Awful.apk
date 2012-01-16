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

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Window;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import com.ferg.awful.constants.Constants;

public class ForumsTabletActivity extends AwfulActivity {

    private static final String TAG = "ForumsTabletActivity";

    private boolean mContent;
    private ForumsIndexFragment mIndexFragment = null;
    private ForumDisplayFragment mFragment = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        new Thread(new Runnable() {
            public void run() {
                GoogleAnalyticsTracker.getInstance().trackPageView("/ForumsTabletActivity");
                GoogleAnalyticsTracker.getInstance().dispatch();
            }
        }).start();

        setContentView(R.layout.forum_index_activity);

        mContent = (findViewById(R.id.content)!= null);
        mIndexFragment = (ForumsIndexFragment) getSupportFragmentManager().findFragmentById(R.id.forums_index);

        setActionBar();

        checkIntentExtras();
    }

    private void setActionBar() {
        ActionBar action = getActionBar();
        action.setBackgroundDrawable(getResources().getDrawable(R.drawable.bar));
    }

    private void checkIntentExtras() {
        if (getIntent().hasExtra(Constants.SHORTCUT)) {
            if (getIntent().getBooleanExtra(Constants.SHORTCUT, false)) {
                	mIndexFragment.displayUserCP();
            }
        }
    }

    public void onResume(){
        super.onResume();
    }

    public void onPause(){
        super.onPause();
    }

    public void onDestroy(){
        super.onDestroy();
    }

    public boolean isDualPane() {
        return mContent;
    }

    public void setContentPane(int aForumId) {
        ForumDisplayFragment fragment = 
            ForumDisplayFragment.newInstance(aForumId);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_NONE);
        if(mFragment == null){
        	transaction.add(R.id.content, fragment);
        }else{
        	transaction.replace(R.id.content, fragment);
        }
    	mFragment = fragment;
        transaction.commit();
    }
}
