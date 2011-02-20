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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import com.ferg.awful.async.ImageDownloader;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.thread.AwfulPost;
import com.ferg.awful.thread.AwfulThread;

public class ThreadDisplayActivity extends Activity {
    private static final String TAG = "ThreadDisplayActivity";

	private final ImageDownloader mImageDownloader = new ImageDownloader();

    private ListView mPostList;
	private ProgressDialog mDialog;
    private SharedPreferences mPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		
        mPrefs = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);

        mPostList = (ListView) findViewById(R.id.thread_posts);

        String threadId = getIntent().getStringExtra(Constants.THREAD_ID);

        new FetchThreadTask().execute(threadId);
    }

    private class FetchThreadTask extends AsyncTask<String, Void, AwfulThread> {
        public void onPreExecute() {
            mDialog = ProgressDialog.show(ThreadDisplayActivity.this, "Loading", 
                "Hold on...", true);
        }

        public AwfulThread doInBackground(String... aParams) {
            AwfulThread result = null;
            
            try {
                result = AwfulThread.getThread(aParams[0], 1);
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, e.toString());
            }

            return result;
        }

        public void onPostExecute(AwfulThread aResult) {
            mPostList.setAdapter(new AwfulPostAdapter(ThreadDisplayActivity.this, 
                        R.layout.post_item, aResult.getPosts()));

            mDialog.dismiss();
        }
    }

    public class AwfulPostAdapter extends ArrayAdapter<AwfulPost> {
        private ArrayList<AwfulPost> mPosts;
        private int mViewResource;
        private LayoutInflater mInflater;

        public AwfulPostAdapter(Context aContext, int aViewResource, ArrayList<AwfulPost> aPosts) {
            super(aContext, aViewResource, aPosts);

            mInflater     = LayoutInflater.from(aContext);
            mPosts        = aPosts;
            mViewResource = aViewResource;
        }

        @Override
        public View getView(int aPosition, View aConvertView, ViewGroup aParent) {
            View inflatedView = aConvertView;

            if (inflatedView == null) {
                inflatedView = mInflater.inflate(mViewResource, null);
            }

            AwfulPost current = getItem(aPosition);

            TextView username = (TextView) inflatedView.findViewById(R.id.username);
            TextView postDate = (TextView) inflatedView.findViewById(R.id.post_date);
            TextView postBody = (TextView) inflatedView.findViewById(R.id.postbody);
            ImageView avatar  = (ImageView) inflatedView.findViewById(R.id.avatar);

            username.setText(current.getUsername());
            postDate.setText("Posted on " + current.getDate());
            postBody.setText(Html.fromHtml(current.getContent()));

            // TODO: Why is this crashing when using the cache? Seems to be gif related.
            mImageDownloader.download(current.getAvatar(), avatar);

            return inflatedView;
        }
    }
}
