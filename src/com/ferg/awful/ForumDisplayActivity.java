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
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.thread.AwfulThread;

public class ForumDisplayActivity extends Activity {
    private static final String TAG = "ThreadsActivity";

    private ListView mThreadList;
	private ProgressDialog mDialog;
    private SharedPreferences mPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forum_display);
		
        mPrefs = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);

        mThreadList = (ListView) findViewById(R.id.forum_list);

		String forumId = getIntent().getStringExtra(Constants.FORUM_ID);

        new FetchThreadsTask().execute(forumId);
    }

    private class FetchThreadsTask extends AsyncTask<String, Void, ArrayList<AwfulThread>> {
        public void onPreExecute() {
            mDialog = ProgressDialog.show(ForumDisplayActivity.this, "Loading", 
                "Hold on...", true);
        }

        public ArrayList<AwfulThread> doInBackground(String... aParams) {
            ArrayList<AwfulThread> result = new ArrayList<AwfulThread>();

            try {
                result = AwfulThread.getForumThreads(aParams[0]);
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, e.toString());
            }

            return result;
        }

        public void onPostExecute(ArrayList<AwfulThread> aResult) {
            mThreadList.setAdapter(new AwfulThreadAdapter(ForumDisplayActivity.this, 
                        R.layout.thread_item, aResult));

            mThreadList.setOnItemClickListener(onThreadSelected);

            mDialog.dismiss();
        }
    }

	private AdapterView.OnItemClickListener onThreadSelected = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            AwfulThreadAdapter adapter = (AwfulThreadAdapter) mThreadList.getAdapter();
            AwfulThread thread = adapter.getItem(aPosition);

            Intent viewThread = new Intent().setClass(ForumDisplayActivity.this, ThreadDisplayActivity.class);
            viewThread.putExtra(Constants.THREAD, thread);

            startActivity(viewThread);
		}
	};

    public class AwfulThreadAdapter extends ArrayAdapter<AwfulThread> {
        private ArrayList<AwfulThread> mThreads;
        private int mViewResource;
        private LayoutInflater mInflater;

        public AwfulThreadAdapter(Context aContext, int aViewResource, ArrayList<AwfulThread> aThreads) {
            super(aContext, aViewResource, aThreads);

            mInflater     = LayoutInflater.from(aContext);
            mThreads        = aThreads;
            mViewResource = aViewResource;
        }

        @Override
        public View getView(int aPosition, View aConvertView, ViewGroup aParent) {
            View inflatedView = aConvertView;

            if (inflatedView == null) {
                inflatedView = mInflater.inflate(mViewResource, null);
            }

            AwfulThread current = getItem(aPosition);

            TextView title       = (TextView) inflatedView.findViewById(R.id.title);
            TextView author      = (TextView) inflatedView.findViewById(R.id.author);
            TextView unreadCount = (TextView) inflatedView.findViewById(R.id.unread_count);
            ImageView sticky     = (ImageView) inflatedView.findViewById(R.id.sticky_icon);

            title.setText(Html.fromHtml(current.getTitle()));
            author.setText("Author: " + current.getAuthor());
            unreadCount.setText(Integer.toString(current.getUnreadCount()));

            if (current.isSticky()) {
                sticky.setImageResource(R.drawable.sticky);
            } else {
                sticky.setImageDrawable(null);
            }

            return inflatedView;
        }
    }
}
