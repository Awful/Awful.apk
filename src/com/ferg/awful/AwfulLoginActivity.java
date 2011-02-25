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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.ferg.awful.constants.Constants;

public class AwfulLoginActivity extends Activity {
    private static final String TAG = "LoginActivity";

    private Button mLogin;
    private EditText mUsername;
    private EditText mPassword;
    private SharedPreferences mPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mPrefs = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);

        mLogin = (Button) findViewById(R.id.login);
        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);

        mLogin.setOnClickListener(onLoginClick);
        
        final ImageView image = (ImageView) findViewById(R.id.dealwithit); 
        image.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((AnimationDrawable) image.getDrawable()).start();
			}
		});
    }

    private View.OnClickListener onLoginClick = new View.OnClickListener() {
        public void onClick(View aView) {
            String username = mUsername.getText().toString();
            String password = mPassword.getText().toString();

            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString(Constants.PREF_USERNAME, username);
            editor.putString(Constants.PREF_PASSWORD, password);
            editor.commit();

            startActivity(new Intent().setClass(AwfulLoginActivity.this, ForumsIndexActivity.class));
        }
    };
}
