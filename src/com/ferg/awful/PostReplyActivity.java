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

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.reply.Reply;

public class PostReplyActivity extends AwfulActivity {
    private static final String TAG = "PostReplyActivity";

	public static final int RESULT_POSTED = 1;

    private FetchFormCookieTask mFetchCookieTask;
    private FetchFormKeyTask mFetchKeyTask;
    private SubmitReplyTask mSubmitTask;

    private Button mSubmit;
    private EditText mMessage;
	private ProgressDialog mDialog;
    private SharedPreferences mPrefs;
	private TextView mTitle;

	private String mThreadId;
    private String mFormCookie;
	private String mFormKey;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post_reply);

        mSubmit  = (Button) findViewById(R.id.submit_button);
        mMessage = (EditText) findViewById(R.id.post_message);
		mTitle   = (TextView) findViewById(R.id.title);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        Intent caller = getIntent();

		mThreadId = caller.getStringExtra(Constants.THREAD);
		
		mTitle.setText(getString(R.string.post_reply));

        // If we're quoting a post, add it to the message box
        if (caller.hasExtra(Constants.QUOTE)) {
            String quoteText = caller.getStringExtra(Constants.QUOTE).replaceAll("&quot;", "\"");
            mMessage.setText(quoteText);
	    mMessage.setSelection(quoteText.length());
        }

        mSubmit.setOnClickListener(onSubmitClick);
    }

    @Override
    public void onResume() {
        super.onResume();

        mFormKey = mPrefs.getString(Constants.FORM_KEY, null);
        if (mFormKey == null) {
            mFetchKeyTask = new FetchFormKeyTask();
            mFetchKeyTask.execute(mThread.getThreadId());
        } else {
            mFetchCookieTask = new FetchFormCookieTask();
            mFetchCookieTask.execute(mThread.getThreadId());
        }
        
		// We'll enable it once we have a formkey and cookie
		mSubmit.setEnabled(false);
    }
    
    @Override
    public void onPause() {
        super.onPause();

        cleanupTasks();
    }
        
    @Override
    public void onStop() {
        super.onStop();

        cleanupTasks();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        cleanupTasks();
    }

    private void cleanupTasks() {
        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mFetchCookieTask != null) {
            mFetchCookieTask.cancel(true);
        }

        if (mFetchKeyTask != null) {
            mFetchKeyTask.cancel(true);
        }
        
        if (mSubmitTask != null) {
            mSubmitTask.cancel(true);
        }
    }

    private View.OnClickListener onSubmitClick = new View.OnClickListener() {
        public void onClick(View aView) {
            boolean editing = getIntent().getBooleanExtra(Constants.EDITING, false);

			mSubmitTask = new SubmitReplyTask(editing);

            if (editing) {
                mSubmitTask.execute(mMessage.getText().toString(), 
                        mFormKey, mFormCookie, mThread.getThreadId(), getIntent().getStringExtra(Constants.POST_ID));
            } else {
                mSubmitTask.execute(mMessage.getText().toString(), 
                        mFormKey, mFormCookie, mThread.getThreadId());
            }
        }
    };

	private class SubmitReplyTask extends AsyncTask<String, Void, Void> {
        private boolean mEditing;

        public SubmitReplyTask(boolean aEditing) {
            mEditing = aEditing;
        }

		public void onPreExecute() {
            mDialog = ProgressDialog.show(PostReplyActivity.this, "Posting", 
                "Hopefully it didn't suck...", true);
        }

        public Void doInBackground(String... aParams) {
            if (!isCancelled()) {
                try {
                    if (mEditing) {
                        Log.i(TAG, "Editing!!");
                        Reply.edit(aParams[0], aParams[1], aParams[2], aParams[3], aParams[4]);
                    } else {
                        Reply.post(aParams[0], aParams[1], aParams[2], aParams[3]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }
            return null;
        }

        public void onPostExecute(Void aResult) {
            if (!isCancelled()) {
                mDialog.dismiss();

				PostReplyActivity.this.setResult(RESULT_POSTED);
				PostReplyActivity.this.finish();
            }
        }
    }

    // Fetches the user's Form Key if we haven't already gotten it.  This should
    // only occur once for any user, and we'll store it in a user preference
    // after that.
    private class FetchFormKeyTask extends AsyncTask<String, Void, String> {
        public String doInBackground(String... aParams) {
            String result = null;

            if (!isCancelled()) {
                try {
                    result = Reply.getFormKey(aParams[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }

            return result;
        }

        public void onPostExecute(String aResult) {
            if (!isCancelled()) {
                if (aResult.length() > 0) {
                    mFormKey = aResult;

                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putString(Constants.FORM_KEY, mFormKey);
                    editor.commit();

                    mFetchCookieTask = new FetchFormCookieTask();
                    mFetchCookieTask.execute(mThread.getThreadId());
                }
            }
        }
	}

    // Fetches the form cookie.  This is necessary every time we post, otherwise
    // the post will fail silently for roughly 5 minutes after posting one time.
    private class FetchFormCookieTask extends AsyncTask<String, Void, String> {
        public String doInBackground(String... aParams) {
            String result = null;

            if (!isCancelled()) {
                try {
                    result = Reply.getFormCookie(aParams[0]);
                    Log.i(TAG, "Form cookie: " + result);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }

            return result;
        }

        public void onPostExecute(String aResult) {
            if (!isCancelled()) {
                if (aResult.length() > 0) {
                    Log.i(TAG, aResult);

                    mFormCookie = aResult;

                    if (mFormKey != null) {
                        mSubmit.setEnabled(true);
                    }
                }
            }
        }
	}
}
