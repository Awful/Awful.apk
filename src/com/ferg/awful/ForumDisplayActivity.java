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

import org.htmlcleaner.TagNode;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.list.ForumArrayAdapter;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulSubforum;
import com.ferg.awful.thread.AwfulThread;

public class ForumDisplayActivity extends AwfulActivity {
    private static final String TAG = "ThreadsActivity";

	private AwfulSubforum mForum;
    private FetchThreadsTask mFetchTask;

    private ImageButton mUserCp;
	private ImageButton mNext;
    private ListView mThreadList;
    private ForumArrayAdapter mThreadAdapter;
    private ProgressDialog mDialog;
    private SharedPreferences mPrefs;
    private TextView mTitle;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forum_display);
		
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mThreadAdapter = new ForumArrayAdapter(this);
        
        mThreadList = (ListView) findViewById(R.id.forum_list);
        mTitle      = (TextView) findViewById(R.id.title);
        mUserCp     = (ImageButton) findViewById(R.id.user_cp);
        mNext       = (ImageButton) findViewById(R.id.next_page);

        mThreadList.setOnScrollListener(new EndlessScrollListener());
        mThreadList.setAdapter(mThreadAdapter);
        mThreadList.setOnItemClickListener(onThreadSelected);

        mForum = (AwfulSubforum) getIntent().getParcelableExtra(Constants.FORUM);
        if(mForum == null) {
        	// This is normally a failure condition, except if we're receiving an
        	// intent from an outside link (say, ChromeToPhone). Let's check to see
        	// if we have a URL from such a link.
        	if (getIntent().getData() != null && getIntent().getData().getScheme().equals("http")) {
        		mForum = new AwfulSubforum();
        		mForum.setForumId(getIntent().getData().getQueryParameter("forumid"));
        	} else {
        		// no dice
        		Log.e(TAG, "Cannot display null forum");
        		finish();
        	}
        }
        
        final ArrayList<AwfulThread> retainedThreadList = (ArrayList<AwfulThread>) getLastNonConfigurationInstance();

        if (retainedThreadList == null || retainedThreadList.size() == 0) {
        	mFetchTask = new FetchThreadsTask();
        	mFetchTask.execute(mForum.getForumId());
        } else {
            mThreadAdapter.setThreads(retainedThreadList);
        }
        
        // We might not be able to set this here if we're getting it from
        // a link and not a ForumsIndexActivity
        if(mForum.getTitle() != null) {
        	mTitle.setText(Html.fromHtml(mForum.getTitle()));
        }
        
        mUserCp.setOnClickListener(onButtonClick);
		mNext.setOnClickListener(onButtonClick);
    }
    
    @Override
    public void onPause() {
        super.onPause();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mFetchTask != null) {
            mFetchTask.cancel(true);
        }
    }
        
    @Override
    public void onStop() {
        super.onStop();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mFetchTask != null) {
            mFetchTask.cancel(true);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mFetchTask != null) {
            mFetchTask.cancel(true);
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
    		case R.id.settings:
    			startActivity(new Intent().setClass(this, SettingsActivity.class));
    			return true;
            case R.id.logout:
                NetworkUtils.clearLoginCookies(this);
                startActivityForResult(new Intent().setClass(this, AwfulLoginActivity.class), 0);
                return true;
            case R.id.refresh:
                mFetchTask = new FetchThreadsTask();
                mFetchTask.execute(mForum.getForumId());
                return true;
			default:
				return super.onOptionsItemSelected(item);
    	}
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mThreadAdapter.getThreads();
    }

    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.user_cp:
                    startActivity(new Intent().setClass(ForumDisplayActivity.this, UserCPActivity.class));
                    break;
				case R.id.next_page:
                    if (mForum.getCurrentPage() != mForum.getLastPage()) {
                    	mForum.setCurrentPage(mForum.getCurrentPage() + 1);
                        mFetchTask = new FetchThreadsTask(mForum.getCurrentPage());
                        mFetchTask.execute(mForum.getForumId());
                    }
					break;
            }
        }
    };

    private class FetchThreadsTask extends AsyncTask<String, Void, ArrayList<AwfulThread>> {
		private int mPage;

		public FetchThreadsTask() {}

		public FetchThreadsTask(int aPage) {
			mPage = aPage;
		}

        public void onPreExecute() {
            mDialog = ProgressDialog.show(ForumDisplayActivity.this, "Loading", 
                "Hold on...", true);
        }

        public ArrayList<AwfulThread> doInBackground(String... aParams) {
        	long time = System.currentTimeMillis();
            ArrayList<AwfulThread> result = new ArrayList<AwfulThread>();

            if (!isCancelled()) {
                try {
                    TagNode threads = null;

                    if (mPage == 0) {
                        threads = AwfulThread.getForumThreads(aParams[0]);
                    } else {
                        threads = AwfulThread.getForumThreads(aParams[0], mPage);
                    }

                    result = AwfulThread.parseForumThreads(threads);
                    //TODO: On the C2P path, we need to get the forum title here too
                    
                    // Now that we have the page number list for the current forum we can
                    // populate it
                    if (mForum.getCurrentPage() == 0) {
                        mForum.parsePageNumbers(threads);

                        Log.i(TAG, Integer.toString(mForum.getCurrentPage()));
                        Log.i(TAG, Integer.toString(mForum.getLastPage()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }

            Log.e(TAG, "Total Process Time: "+(System.currentTimeMillis()-time));
            return result;
        }

        public void onPostExecute(ArrayList<AwfulThread> aResult) {
            if (!isCancelled()) {
            	//TODO: We need to set the forum title
            	
                mThreadAdapter.addThreads(aResult);

                mDialog.dismiss();
            }
        }
    }

	private AdapterView.OnItemClickListener onThreadSelected = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            ForumArrayAdapter adapter = (ForumArrayAdapter) mThreadList.getAdapter();
            
            switch(adapter.getItemType(aPosition)) {
            case THREAD:
            	AwfulThread thread = (AwfulThread) adapter.getItem(aPosition);
                Intent viewThread = new Intent().setClass(ForumDisplayActivity.this, ThreadDisplayActivity.class);
                viewThread.putExtra(Constants.THREAD, thread);
                startActivity(viewThread);
                break;
                
            case SUB_FORUM:
            	AwfulForum forum = (AwfulForum) adapter.getItem(aPosition);
                Intent viewForum = new Intent().setClass(ForumDisplayActivity.this, ForumDisplayActivity.class);
                viewForum.putExtra(Constants.FORUM, forum);
                startActivity(viewForum);
                break;
            }
            
		}
	};

    private class EndlessScrollListener implements OnScrollListener {
    	private int visibleThreshold = 5;
    	private int currentPage = 0;
    	private int previousTotal = 0;
    	private boolean loading = true;
    	
    	public EndlessScrollListener() {
    	}
    	
    	public void onScroll(AbsListView view, int firstVisibleItem,
    			int visibleItemCount, int totalItemCount) {
    		if (loading) {
    			if (totalItemCount > previousTotal) {
    				loading = false;
    				previousTotal = totalItemCount;
    				currentPage++;
    			}
    		}
    		
    		if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
    			//load new items in background and add them
    			loading = true;
				if (mForum.getCurrentPage() != mForum.getLastPage()) {
					mForum.setCurrentPage(mForum.getCurrentPage() + 1);
					new FetchThreadsTask(mForum.getCurrentPage()).execute(mForum.getForumId());
				}
    		}
    	}

    	public void onScrollStateChanged(AbsListView view, int scrollState) {
    		
    	}
    }
}
