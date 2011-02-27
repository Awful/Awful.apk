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
import java.util.HashMap;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.thread.AwfulForum;

public class ForumsIndexActivity extends Activity {
    private static final String TAG = "LoginActivity";

    private LoadForumsTask mLoadTask;

    private ImageButton mUserCp;
    private ListView mForumList;
	private ProgressDialog mDialog;
    private SharedPreferences mPrefs;
    private TextView mTitle;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forum_index);
		
        mPrefs = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);

        mForumList = (ListView) findViewById(R.id.forum_list);
        mTitle     = (TextView) findViewById(R.id.title);
        mUserCp    = (ImageButton) findViewById(R.id.user_cp);

        mTitle.setText(getString(R.string.forums_title));
        mUserCp.setOnClickListener(onButtonClick);

		boolean loggedIn = NetworkUtils.restoreLoginCookies(this);

		if (loggedIn) {
			mLoadTask = new LoadForumsTask();
            mLoadTask.execute();
		} else {
			startActivityForResult(new Intent().setClass(this, AwfulLoginActivity.class), 0);
		}
    }
    
    @Override
    public void onPause() {
        super.onPause();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mLoadTask != null) {
            mLoadTask.cancel(true);
        }
    }
        
    @Override
    public void onStop() {
        super.onStop();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mLoadTask != null) {
            mLoadTask.cancel(true);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mLoadTask != null) {
            mLoadTask.cancel(true);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// The only activity we call for result is login
    	// Odds are we want to refresh whether or not it was successful
    	
    	// But we do need to make sure we aren't already in the middle of a refresh
    	if(mDialog == null || !mDialog.isShowing()) {
    		mLoadTask = new LoadForumsTask();
            mLoadTask.execute();
    	}
    }

    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.user_cp:
                    startActivity(new Intent().setClass(ForumsIndexActivity.this, UserCPActivity.class));
                    break;
            }
        }
    };

    private class LoadForumsTask extends AsyncTask<Void, Void, ArrayList<AwfulForum>> {
        public void onPreExecute() {
            mDialog = ProgressDialog.show(ForumsIndexActivity.this, "Loading", 
                "Hold on...", true);
        }

        public ArrayList<AwfulForum> doInBackground(Void... aParams) {
            ArrayList<AwfulForum> result = new ArrayList<AwfulForum>();
            if (!isCancelled()) {
                try {
                    result = AwfulForum.getForums();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }
            return result;
        }

        public void onPostExecute(ArrayList<AwfulForum> aResult) {
            if (!isCancelled()) {
                mForumList.setAdapter(new AwfulForumAdapter(ForumsIndexActivity.this, 
                            R.layout.forum_item, aResult));

                mForumList.setOnItemClickListener(onForumSelected);

                mDialog.dismiss();
            }
        }
    }

	private AdapterView.OnItemClickListener onForumSelected = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            AwfulForumAdapter adapter = (AwfulForumAdapter) mForumList.getAdapter();
            AwfulForum forum = adapter.getItem(aPosition);

            Intent viewForum = new Intent().setClass(ForumsIndexActivity.this, ForumDisplayActivity.class);
            viewForum.putExtra(Constants.FORUM, forum);

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
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.forum_index_options, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.logout:
    		NetworkUtils.clearLoginCookies(this);
    		startActivityForResult(new Intent().setClass(this, AwfulLoginActivity.class), 0);
    	case R.id.refresh:
    		mLoadTask = new LoadForumsTask();
            mLoadTask.execute();
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
}
