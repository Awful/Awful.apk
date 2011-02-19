package com.ferg.awful;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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
    }

    private View.OnClickListener onLoginClick = new View.OnClickListener() {
        public void onClick(View aView) {
            String username = mUsername.getText().toString();
            String password = mPassword.getText().toString();

            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString(Constants.PREF_USERNAME, username);
            editor.putString(Constants.PREF_PASSWORD, password);
            editor.commit();

            startActivity(new Intent().setClass(AwfulLoginActivity.this, ThreadDisplayActivity.class));
        }
    };
}
