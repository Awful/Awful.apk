/**
 * *****************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the software nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
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
 * *****************************************************************************
 */

package com.ferg.awfulapp;

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

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.LoginRequest;
import com.ferg.awfulapp.task.SendEditRequest;

import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;

import java.util.HashMap;

public class AwfulLoginActivity extends AwfulActivity {
    private static final String TAG = "LoginActivity";

    private Button mLogin;
    private EditText mUsername;
    private EditText mPassword;

    private ProgressDialog mDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login);

        NetworkUtils.logCookies();

        mLogin = (Button) findViewById(R.id.login);
        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mPassword.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginClick();
                }
                if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
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
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        if (isLoggedIn()) {
            Log.e(TAG, "Already logged in! Closing AwfulLoginActivity!");
            this.finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    private void loginClick() {
        final String username = NetworkUtils.encodeHtml(mUsername.getText().toString());
        final String password = NetworkUtils.encodeHtml(mPassword.getText().toString());

        mDialog = ProgressDialog.show(AwfulLoginActivity.this, "Logging In", "Hold on...", true);
        final AwfulLoginActivity self = this;
        NetworkUtils.queueRequest(new LoginRequest(this, username, password).build(null, new AwfulRequest.AwfulResultCallback<Boolean>() {
            @Override
            public void success(Boolean result) {
                onLoginSuccess();
            }

            @Override
            public void failure(VolleyError error) {
                // Volley sometimes generates NetworkErrors with no response set, or wraps them
                NetworkResponse response = error.networkResponse;
                if (response == null) {
                    Throwable cause = error.getCause();
                    if (cause != null && cause instanceof VolleyError) {
                        response = ((VolleyError) cause).networkResponse;
                    }
                }
                if (response != null && response.statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                    Boolean result = NetworkUtils.saveLoginCookies(getApplicationContext());
                    if(result){
                        AwfulPreferences prefs = AwfulPreferences.getInstance(getApplicationContext());
                        prefs.setStringPreference("username", username);
                        onLoginSuccess();
                    }else{
                        onLoginFailed();
                    }
                } else {
                    onLoginFailed();
                }
            }

            private void onLoginSuccess() {
                mDialog.dismiss();
                Toast.makeText(AwfulLoginActivity.this, R.string.login_succeeded, Toast.LENGTH_SHORT).show();
                setResult(Activity.RESULT_OK);
                self.finish();
            }
            private  void onLoginFailed(){
                mDialog.dismiss();
                Toast.makeText(AwfulLoginActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                setResult(Activity.RESULT_CANCELED);
            }
        }));
    }

    private View.OnClickListener onLoginClick = new View.OnClickListener() {
        public void onClick(View aView) {
            loginClick();
        }
    };
}
