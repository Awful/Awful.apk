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

package com.ferg.awfulapp;

import java.util.HashMap;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.task.FeatureRequest;
import com.ferg.awfulapp.task.ProfileRequest;

public class AwfulLoginActivity extends AwfulActivity {
    private static final String TAG = "LoginActivity";

    private LoginTask mLoginTask;

    private Button mLogin;
    private EditText mUsername;
    private EditText mPassword;

    private ProgressDialog mDialog;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login);

        mLogin = (Button) findViewById(R.id.login);
        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mPassword.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_DONE){
					loginClick();
				}
				if(event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER){
					loginClick();
				}
				return false;
			}
		});

        mLogin.setOnClickListener(onLoginClick);

        mUsername.requestFocus();
        
        final ImageView image = (ImageView) findViewById(R.id.dealwithit); 
        image.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((AnimationDrawable) image.getDrawable()).start();
			}
		});
    }

    //Not sure if this needs a @Override since it worked without one
    public void onResume(){
    	super.onResume();
		Log.i(TAG,"onResume");
    	boolean loggedIn = NetworkUtils.restoreLoginCookies(this);
		if (loggedIn) {
			Log.e(TAG,"Already logged in! Closing AwfulLoginActivity!");
			this.finish();
		}
    }
    
    @Override
    public void onPause() {
        super.onPause();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mLoginTask != null) {
            mLoginTask.cancel(true);
        }
    }
        
    @Override
    public void onStop() {
        super.onStop();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mLoginTask != null) {
            mLoginTask.cancel(true);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mLoginTask != null) {
            mLoginTask.cancel(true);
        }
    }

	private void loginClick(){
        String username = mUsername.getText().toString();
        String password = mPassword.getText().toString();

        mLoginTask = new LoginTask();
        mLoginTask.execute(new String[] {username, password});
    }

    private View.OnClickListener onLoginClick = new View.OnClickListener() {
        public void onClick(View aView) {
        	loginClick();
        }
    };
    
    private class LoginTask extends AsyncTask<String, Void, Boolean> {
        public void onPreExecute() {
            mDialog = ProgressDialog.show(AwfulLoginActivity.this, "Logging In", 
                "Hold on...", true);
        }

        public Boolean doInBackground(String... aParams) {
        	boolean result = false;
        	
            if (!isCancelled()) {
                HashMap<String, String> params = new HashMap<String, String>();
                params.put(Constants.PARAM_USERNAME, aParams[0]);
                params.put(Constants.PARAM_PASSWORD, aParams[1]);
                params.put(Constants.PARAM_ACTION, "login");

                try {
                    NetworkUtils.post(Constants.FUNCTION_LOGIN_SSL, params);
                    result = NetworkUtils.saveLoginCookies(AwfulLoginActivity.this);

                    // Write username to preferences for SALR features
                    AwfulPreferences prefs = AwfulPreferences.getInstance(AwfulLoginActivity.this);
                    prefs.setUsername(aParams[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }

            return result;
        }

        public void onPostExecute(Boolean aResult) {
            if (!isCancelled()) {
                boolean succeeded = aResult;

                mDialog.dismiss();

                int loginStatusResource = succeeded ? R.string.login_succeeded : R.string.login_failed;
                Toast.makeText(AwfulLoginActivity.this, loginStatusResource, Toast.LENGTH_SHORT).show();

                if(succeeded) {
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    setResult(Activity.RESULT_CANCELED);
                    // Not finishing - give the user another chance to log in
                }
            }
        }
    }
}
