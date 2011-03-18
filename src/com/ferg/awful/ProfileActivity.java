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

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.webkit.WebView;

import com.ferg.awful.async.DrawableManager;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.user.Profile;

public class ProfileActivity extends Activity {
    private static final String TAG = "ProfileActivity";

    private FetchProfileTask mFetchTask;

	private ProgressDialog mDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        mFetchTask = new FetchProfileTask();
        mFetchTask.execute(getIntent().getStringExtra(Constants.PARAM_USER_ID));
    }
    
    @Override
    public void onPause() {
        super.onPause();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mFetchTask != null) {
            mFetchTask.cancel(true);
        }
    }
        
    @Override
    public void onStop() {
        super.onStop();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mFetchTask != null) {
            mFetchTask.cancel(true);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mFetchTask != null) {
            mFetchTask.cancel(true);
        }
    }

    private void displayProfile(Profile aProfile) {
        TextView title    = (TextView) findViewById(R.id.title);
        WebView info      = (WebView) findViewById(R.id.profile_body);

        title.setText(aProfile.getUsername());

        info.loadData(aProfile.getInfo(), "text/html", "utf-8");
    }

	private class FetchProfileTask extends AsyncTask<String, Void, Profile> {
		public void onPreExecute() {
            mDialog = ProgressDialog.show(ProfileActivity.this, "Loading", 
                "Hold on...", true);
        }

        public Profile doInBackground(String... aParams) {
            Profile result = null;

            if (!isCancelled()) {
                result = Profile.withId(aParams[0]);
            }

            return result;
        }

        public void onPostExecute(Profile aResult) {
            if (!isCancelled()) {
                mDialog.dismiss();

                displayProfile(aResult);
            }
        }
    }
}
