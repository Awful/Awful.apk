/********************************************************************************
 * Copyright (c) 2012, Matthew Shepard
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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;

public class PrivateMessageActivity extends AwfulActivity implements MessageFragment.PrivateMessageCallbacks {

    private static final String MESSAGE_FRAGMENT_TAG = "message fragment";

    private View pane_two;
    private String pmIntentID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.private_message_activity);
        setMPrefs(AwfulPreferences.getInstance(this, this));

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        setUpActionBar();

        Uri data = getIntent().getData();
        if (data != null) {
            pmIntentID = data.getQueryParameter(Constants.PARAM_PRIVATE_MESSAGE_ID);
        }

        pane_two = findViewById(R.id.fragment_pane_two);
        setContentPane();
    }

    public void setContentPane() {
        if (getSupportFragmentManager().findFragmentById(R.id.fragment_pane) == null) {
            PrivateMessageListFragment fragment = new PrivateMessageListFragment();

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_pane, fragment);
            int pmid = 0;
            if (pmIntentID != null && pmIntentID.matches("\\d+")) {
                pmid = Integer.parseInt(pmIntentID);
            }
            if (pane_two == null) {
                if (pmid > 0) {//this should only trigger if we intercept 'private.php?pmid=XXXXX' and we are not on a tablet.
                    startActivity(new Intent().setClass(this, MessageDisplayActivity.class).putExtra(Constants.PARAM_PRIVATE_MESSAGE_ID, pmid));
                }
            }

            transaction.commit();
        }
    }

    public void showMessage(String name, int id) {
        if (pane_two != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_pane_two, new MessageFragment(name, id), MESSAGE_FRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent().setClass(this, MessageDisplayActivity.class);
            if (name != null) {
                intent.putExtra(Constants.PARAM_USERNAME, name);
            } else {
                intent.putExtra(Constants.PARAM_PRIVATE_MESSAGE_ID, id);
            }
            startActivity(intent);
        }
    }


    @Override
    public void onMessageClosed() {
        // remove the message fragment, if it exists
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment messageFragment = fragmentManager.findFragmentByTag(MESSAGE_FRAGMENT_TAG);
        if (messageFragment != null) {
            fragmentManager.beginTransaction().remove(messageFragment).commit();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                returnHome();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void returnHome() {
        finish();
        navigate(NavigationEvent.MainActivity.INSTANCE);
    }
}
