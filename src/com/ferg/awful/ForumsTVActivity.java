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
import android.view.View;
import android.view.Window;

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
        bar.setTitle(R.string.app_name);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setTitleBackground(getResources().getDrawable(R.drawable.bar));
        bar.setShowHideAnimationEnabled(true);
        bar.setDisplayOptions(
            LeftNavBar.DISPLAY_AUTO_EXPAND|
            ActionBar.DISPLAY_SHOW_HOME|
            LeftNavBar.DISPLAY_USE_LOGO_WHEN_EXPANDED
        );

        setupTabs();
    }

    private void setupTabs() {
        ActionBar bar = getLeftNavBar();
        bar.removeAllTabs();

        ActionBar.Tab home = bar.newTab().setText(R.string.home).setIcon(R.drawable.ic_action_home)
                .setTabListener(new ActionBar.TabListener() {

            @Override
            public void onTabUnselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {}

            @Override
            public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
                ForumsIndexFragment fragment = ForumsIndexFragment.newInstance();

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.content, fragment);
                transaction.commit();
            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {}
        });

        ActionBar.Tab usercp = bar.newTab().setText(R.string.usercp).setIcon(R.drawable.gear)
                .setTabListener(new ActionBar.TabListener() {

            @Override
            public void onTabUnselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {}

            @Override
            public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
                UserCPFragment fragment = 
                    UserCPFragment.newInstance(false);

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.content, fragment);
                transaction.commit();
            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
            }
        });

        ActionBar.Tab pm = bar.newTab().setText(R.string.private_message).setIcon(R.drawable.ic_action_private_message)
                .setTabListener(new ActionBar.TabListener() {

            @Override
            public void onTabUnselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {}

            @Override
            public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {}

            @Override
            public void onTabReselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {}
        });

        bar.addTab(home, true);
        bar.addTab(usercp, false);
        bar.addTab(pm, false);
    }

    public void displayUserCP() {
        UserCPFragment.newInstance(true).show(getSupportFragmentManager(), "user_control_panel_dialog");
    }
}
