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
import android.content.Intent;
import android.os.Bundle;
import android.view.*;

import android.support.v4.app.FragmentTransaction;

import com.example.google.tv.leftnavbar.LeftNavBar;
import com.example.google.tv.leftnavbar.LeftNavBarService;
import com.example.google.tv.leftnavbar.R;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class ForumsTVActivity extends AwfulActivity {

    private static final String TAG = "ForumsTVActivity";

    private LeftNavBar mLeftNavBar;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        new Thread(new Runnable() {
            public void run() {
                GoogleAnalyticsTracker.getInstance().trackPageView("/ForumsTVActivity");
                GoogleAnalyticsTracker.getInstance().dispatch();
            }
        }).start();

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.forum_index_activity);

        LeftNavBar bar = (LeftNavBarService.instance()).getLeftNavBar(this);
        bar.setBackgroundDrawable(getResources().getDrawable(R.drawable.bar_tv));

        setupBar();
    }
    
    private LeftNavBar getLeftNavBar() {
        if (mLeftNavBar == null) {
            mLeftNavBar = new LeftNavBar(this);
            mLeftNavBar.setOnClickHomeListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // This is called when the app icon is selected in the left navigation bar
                    // Doing nothing.
                }
            });
        }
        
        return mLeftNavBar;
    }

    private void setupBar() {
        LeftNavBar bar = getLeftNavBar();
        bar.setCustomView(R.layout.action_options);
        bar.setTitle(R.string.app_name);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitleBackground(getResources().getDrawable(R.drawable.bar));
        bar.setShowHideAnimationEnabled(true);
        bar.setDisplayOptions(
            LeftNavBar.DISPLAY_AUTO_EXPAND|
            ActionBar.DISPLAY_SHOW_HOME|
            LeftNavBar.DISPLAY_USE_LOGO_WHEN_EXPANDED|
            ActionBar.DISPLAY_SHOW_CUSTOM
        );

        ViewGroup optionsContainer = (ViewGroup) bar.getCustomView();

        for (int i = 0; i < optionsContainer.getChildCount(); i++) {
            optionsContainer.getChildAt(i).setOnClickListener(onActionItemClick);
        }
    }

    private View.OnClickListener onActionItemClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.user_cp:
                    displayUserCP();
                    break;
                case R.id.pm:
                    startActivity(new Intent(ForumsTVActivity.this, PrivateMessageActivity.class));
                    break;
            }
        }
    };

    public void displayUserCP() {
        UserCPFragment.newInstance(true).show(getSupportFragmentManager(), "user_control_panel_dialog");
    }
}
