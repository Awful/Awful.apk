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
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ferg.awful.async.DrawableManager;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.thread.AwfulPost;
import com.ferg.awful.thread.AwfulThread;

public class ThreadDisplayActivity extends Activity {
    private static final String TAG = "ThreadDisplayActivity";

	private final DrawableManager mImageDownloader = new DrawableManager();

	private AwfulThread mThread;

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

        mThread = (AwfulThread) getIntent().getParcelableExtra(Constants.THREAD);

        new FetchThreadTask().execute(mThread);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.post_menu, menu);

		return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
			case R.id.go_back:
				if (mThread.getCurrentPage() != 1) {
					new FetchThreadTask(mThread.getCurrentPage() - 1).execute(mThread);
				}
				break;
			case R.id.go_forward:
				if (mThread.getCurrentPage() != mThread.getLastPage()) {
					new FetchThreadTask(mThread.getCurrentPage() + 1).execute(mThread);
				}
				break;
			case R.id.usercp:
			case R.id.go_to:
			default:
				return super.onOptionsItemSelected(item);
    	}

		return true;
    }

    private class FetchThreadTask extends AsyncTask<AwfulThread, Void, AwfulThread> {
		private int mPage;

		public FetchThreadTask() {}

		public FetchThreadTask(int aPage) {
			mPage = aPage;
		}

        public void onPreExecute() {
            mDialog = ProgressDialog.show(ThreadDisplayActivity.this, "Loading", 
                "Hold on...", true);
        }

        public AwfulThread doInBackground(AwfulThread... aParams) {
            try {
				if (mPage == 0) {
					if (aParams[0].getUnreadCount() > 0) {
						aParams[0].getThreadPosts();
					} else {
						aParams[0].getThreadPosts(1);
					}
				} else {
					aParams[0].getThreadPosts(mPage);
				}
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, e.toString());
            }

            return aParams[0];
        }

        public void onPostExecute(AwfulThread aResult) {
			mThread = aResult;

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

        private class ViewHolder {
        	public TextView username;
        	public TextView postDate;
        	public TextView postBody;
        	public ImageView avatar;
        	public ViewHolder(View v) {
        		username = (TextView) v.findViewById(R.id.username);
        		postDate = (TextView) v.findViewById(R.id.post_date);
        		postBody = (TextView) v.findViewById(R.id.postbody);
        		avatar = (ImageView) v.findViewById(R.id.avatar);
        	}
        }
        
        @Override
        public View getView(int aPosition, View aConvertView, ViewGroup aParent) {
            View inflatedView = aConvertView;
            ViewHolder viewHolder = null;
            
            if (inflatedView == null) {
                inflatedView = mInflater.inflate(mViewResource, null);
                viewHolder = new ViewHolder(inflatedView);
                inflatedView.setTag(viewHolder);
                viewHolder.postBody.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
            	viewHolder = (ViewHolder) inflatedView.getTag();
            }

            AwfulPost current = getItem(aPosition);

            viewHolder.username.setText(current.getUsername());
            viewHolder.postDate.setText("Posted on " + current.getDate());
            viewHolder.postBody.setText(Html.fromHtml(current.getContent()));

            // TODO: Why is this crashing when using the cache? Seems to be gif related.
            // Note: ImageDownloader changed since that todo was written; not sure if it's still an issue
            mImageDownloader.fetchDrawableOnThread(current.getAvatar(), viewHolder.avatar);

            return inflatedView;
        }
    }
}
