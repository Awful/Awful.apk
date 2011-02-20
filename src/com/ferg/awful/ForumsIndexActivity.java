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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import com.ferg.awful.async.ImageDownloader;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulPost;
import com.ferg.awful.thread.AwfulThread;

public class ForumsIndexActivity extends Activity {
    private static final String TAG = "LoginActivity";

	private final ImageDownloader mImageDownloader = new ImageDownloader();

    private ListView mForumList;
	private ProgressDialog mDialog;
    private SharedPreferences mPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forum_index);
		
        mPrefs = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);

        mForumList = (ListView) findViewById(R.id.forum_list);

		String username = mPrefs.getString(Constants.PREF_USERNAME, null);
		String password = mPrefs.getString(Constants.PREF_PASSWORD, null);

		if (username == null || password == null) {
			startActivity(new Intent().setClass(this, AwfulLoginActivity.class));
		} else {
			new LoginTask().execute(username, password);
		}
    }

    private class LoginTask extends AsyncTask<String, Void, ArrayList<AwfulForum>> {
        public void onPreExecute() {
            mDialog = ProgressDialog.show(ForumsIndexActivity.this, "Loading", 
                "Hold on...", true);
        }

        public ArrayList<AwfulForum> doInBackground(String... aParams) {
            ArrayList<AwfulForum> result = new ArrayList<AwfulForum>();

            HashMap<String, String> params = new HashMap<String, String>();
			Log.i(TAG, aParams[0]);
			Log.i(TAG, aParams[1]);
            params.put(Constants.PARAM_USERNAME, aParams[0]);
            params.put(Constants.PARAM_PASSWORD, aParams[1]);
            params.put(Constants.PARAM_ACTION, "login");
            
            try {
                NetworkUtils.post(Constants.FUNCTION_LOGIN, params);

                result = AwfulForum.getForums();
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, e.toString());
            }

            return result;
        }

        public void onPostExecute(ArrayList<AwfulForum> aResult) {
            mForumList.setAdapter(new AwfulForumAdapter(ForumsIndexActivity.this, 
                        R.layout.forum_item, aResult));

            mForumList.setOnItemClickListener(onForumSelected);

            mDialog.dismiss();
        }
    }

	private AdapterView.OnItemClickListener onForumSelected = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            AwfulForumAdapter adapter = (AwfulForumAdapter) mForumList.getAdapter();
            AwfulForum forum = adapter.getItem(aPosition);

            Intent viewForum = new Intent().setClass(ForumsIndexActivity.this, ForumDisplayActivity.class);
			Log.i(TAG, forum.getForumId());
            viewForum.putExtra(Constants.FORUM_ID, forum.getForumId());

            startActivity(viewForum);
		}
	};

    public class AwfulForumAdapter extends ArrayAdapter<AwfulForum> {
        private ArrayList<AwfulForum> mForums;
        private int mViewResource;
        private LayoutInflater mInflater;

        public AwfulForumAdapter(Context aContext, int aViewResource, ArrayList<AwfulForum> aForums) {
            super(aContext, aViewResource, aForums);

            mInflater     = LayoutInflater.from(aContext);
            mForums       = aForums;
            mViewResource = aViewResource;
        }

        @Override
        public View getView(int aPosition, View aConvertView, ViewGroup aParent) {
            View inflatedView = aConvertView;

            if (inflatedView == null) {
                inflatedView = mInflater.inflate(mViewResource, null);
            }

			AwfulForum current = getItem(aPosition);

			TextView title   = (TextView) inflatedView.findViewById(R.id.title);
			TextView subtext = (TextView) inflatedView.findViewById(R.id.subtext);

			title.setText(Html.fromHtml(current.getTitle()));
			subtext.setText(current.getSubtext());

            return inflatedView;
        }
    }
}
