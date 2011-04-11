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
import android.preference.PreferenceManager;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v4.app.ListFragment;

import org.htmlcleaner.TagNode;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.list.ForumArrayAdapter;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulThread;

public class UserCPFragment extends ListFragment {
    private static final String TAG = "ThreadsActivity";

    private FetchThreadsTask mFetchTask;
    private ImageButton mHome;
	private ProgressDialog mDialog;
    private SharedPreferences mPrefs;
    private TextView mTitle;

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        View result = aInflater.inflate(R.layout.user_cp, aContainer, false);

        mTitle      = (TextView) result.findViewById(R.id.title);
        mHome       = (ImageButton) result.findViewById(R.id.home);

        return result;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

        setHasOptionsMenu(true);
        setRetainInstance(true);
		
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mTitle.setText(getString(R.string.user_cp));
		mHome.setOnClickListener(onButtonClick);

		setListAdapter(new ForumArrayAdapter(getActivity()));

		getListView().setOnItemClickListener(onThreadSelected);
    }

    @Override
    public void onStart() {
        super.onStart();
		
        // When coming from the desktop shortcut we won't have login cookies
		boolean loggedIn = NetworkUtils.restoreLoginCookies(getActivity());

		if (loggedIn) {
            final ArrayList<AwfulThread> retainedThreadsList = null;

            if (retainedThreadsList == null) {
                mFetchTask = new FetchThreadsTask();
                mFetchTask.execute();
            } else {
                ((ForumArrayAdapter) getListAdapter()).setThreads(retainedThreadsList);
            }
		} else {
			startActivityForResult(new Intent().setClass(getActivity(), AwfulLoginActivity.class), 0);
		}
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.forum_index_options, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    		case R.id.settings:
    			startActivity(new Intent().setClass(getActivity(), SettingsActivity.class));
    			return true;
            case R.id.logout:
                NetworkUtils.clearLoginCookies(getActivity());
                startActivityForResult(new Intent().setClass(getActivity(), AwfulLoginActivity.class), 0);
                break;
            case R.id.refresh:
                mFetchTask = new FetchThreadsTask();
                mFetchTask.execute();
                break;
			default:
				return super.onOptionsItemSelected(item);
    	}

        return true;
    }

    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.home:
                    startActivity(new Intent().setClass(getActivity(), ForumsIndexActivity.class));
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
            mDialog = ProgressDialog.show(getActivity(), "Loading", 
                    "Hold on...", true);
        }

        public ArrayList<AwfulThread> doInBackground(String... aParams) {
            ArrayList<AwfulThread> result = new ArrayList<AwfulThread>();

            if (!isCancelled()) {
                try {
                    TagNode threads = null;

                    threads = AwfulThread.getUserCPThreads();

                    result = AwfulThread.parseForumThreads(threads);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }

            return result;
        }

        public void onPostExecute(ArrayList<AwfulThread> aResult) {
            if (!isCancelled()) {
            	((ForumArrayAdapter) getListAdapter()).setThreads(aResult);

                mDialog.dismiss();
            }
        }
    }

	private AdapterView.OnItemClickListener onThreadSelected = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            AwfulThread thread = (AwfulThread) getListAdapter().getItem(aPosition);

            Intent viewThread = new Intent().setClass(getActivity(), ThreadDisplayActivity.class);
            viewThread.putExtra(Constants.THREAD, thread);

            startActivity(viewThread);
		}
	};
}
