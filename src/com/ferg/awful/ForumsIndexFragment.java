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
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.support.v4.app.Fragment;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulSubforum;

public class ForumsIndexFragment extends Fragment {
    private static final String TAG = "ForumsIndex";

    private LoadForumsTask mLoadTask;

    private ImageButton mUserCp;
    private ExpandableListView mForumList;
	private ProgressDialog mDialog;
    private SharedPreferences mPrefs;
    private TextView mTitle;
    private int mDefaultPostFontColor;
    private int mDefaultPostBackgroundColor;
    private int mDefaultPostBackground2Color;

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        View result = aInflater.inflate(R.layout.forum_index, aContainer, false);

        mForumList = (ExpandableListView) result.findViewById(R.id.forum_list);
        mTitle     = (TextView) result.findViewById(R.id.title);
        mUserCp    = (ImageButton) result.findViewById(R.id.user_cp);
        
        PreferenceManager.setDefaultValues(getActivity(), R.xml.settings, false);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        
        mDefaultPostFontColor = mPrefs.getInt("default_post_font_color", getResources().getColor(R.color.default_post_font));
        mDefaultPostBackgroundColor = mPrefs.getInt("default_post_background_color", getResources().getColor(R.color.background));
        mDefaultPostBackground2Color = mPrefs.getInt("default_post_background2_color", getResources().getColor(R.color.background2));
        mForumList.setBackgroundColor(mDefaultPostBackgroundColor);
        mForumList.setCacheColorHint(mDefaultPostBackgroundColor);
        return result;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

        setHasOptionsMenu(true);
        setRetainInstance(true);
		


        mTitle.setText(getString(R.string.forums_title));
        mUserCp.setOnClickListener(onButtonClick);
    }

    @Override
    public void onStart() {
        super.onStart();

		boolean loggedIn = NetworkUtils.restoreLoginCookies(getActivity());

		if (loggedIn) {
            Log.i(TAG, "Starting task!");
			mLoadTask = new LoadForumsTask();
            mLoadTask.execute();
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                    startActivity(new Intent().setClass(getActivity(), UserCPActivity.class));
                    break;
            }
        }
    };

    private class LoadForumsTask extends AsyncTask<Void, Void, ArrayList<AwfulForum>> {
        public void onPreExecute() {
            mDialog = ProgressDialog.show(getActivity(), "Loading", 
                "Hold on...", true);
        }

        public ArrayList<AwfulForum> doInBackground(Void... aParams) {
            ArrayList<AwfulForum> result = new ArrayList<AwfulForum>();
            if (!isCancelled()) {
                try {
                    result = AwfulForum.getForums(getActivity());
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }
            return result;
        }

        public void onPostExecute(ArrayList<AwfulForum> aResult) {
            if (!isCancelled()) {
                mForumList.setAdapter(new AwfulForumAdapter(getActivity(), aResult));

                mForumList.setOnChildClickListener(onForumSelected);

                mDialog.dismiss();
            }
        }
    }

	private ExpandableListView.OnChildClickListener onForumSelected = new ExpandableListView.OnChildClickListener() {
		public boolean onChildClick(ExpandableListView aParent, View aView, int aGroupPosition, 
                int aChildPosition, long aId) 
        {
            AwfulForumAdapter adapter = (AwfulForumAdapter) mForumList.getExpandableListAdapter();
            AwfulSubforum forum = (AwfulSubforum) adapter.getChild(aGroupPosition, aChildPosition);

            Intent viewForum = new Intent().setClass(getActivity(), ForumDisplayActivity.class);
            viewForum.putExtra(Constants.FORUM, forum);

            startActivity(viewForum);

            return true;
		}
	};

    public class AwfulForumAdapter extends BaseExpandableListAdapter {
        private ArrayList<AwfulForum> mForums;
        private LayoutInflater mInflater;

        public AwfulForumAdapter(Context aContext, ArrayList<AwfulForum> aForums) {
            mInflater     = LayoutInflater.from(aContext);
            mForums       = aForums;
        }

        @Override
        public View getChildView(int aGroupPosition, int aChildPosition, boolean isLastChild, 
                View aConvertView, ViewGroup aParent) 
        {
            View inflatedView = aConvertView;

            if (inflatedView == null) {
                inflatedView = mInflater.inflate(R.layout.subforum_item, null);
            }

			AwfulSubforum current = (AwfulSubforum) getChild(aGroupPosition, aChildPosition);
			inflatedView.setBackgroundColor(mDefaultPostBackground2Color);
			TextView title   = (TextView) inflatedView.findViewById(R.id.title);
			title.setText(Html.fromHtml(current.getTitle()));
			title.setTextColor(mDefaultPostFontColor);
            return inflatedView;
        }

        @Override
        public View getGroupView(int aGroupPosition, boolean isExpanded, View aConvertView, ViewGroup aParent) {
            View inflatedView = aConvertView;

            if (inflatedView == null) {
                inflatedView = mInflater.inflate(R.layout.forum_item, null);
            }

			final AwfulForum current = (AwfulForum) getGroup(aGroupPosition);

			TextView title            = (TextView) inflatedView.findViewById(R.id.title);
			TextView subtext          = (TextView) inflatedView.findViewById(R.id.subtext);
			RelativeLayout viewButton = (RelativeLayout) inflatedView.findViewById(R.id.parent_button);

			title.setText(Html.fromHtml(current.getTitle()));
			title.setTextColor(mDefaultPostFontColor);
			subtext.setText(current.getSubtext());
			subtext.setTextColor(mDefaultPostBackground2Color);

            viewButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View aView) {
                    Intent viewForum = new Intent().setClass(getActivity(), 
                        ForumDisplayActivity.class);
                    viewForum.putExtra(Constants.FORUM, current);

                    startActivity(viewForum);
                }
            });

            return inflatedView;
        }

        public Object getChild(int aGroupPosition, int aChildPosition) {
            return mForums.get(aGroupPosition).getSubforums().get(aChildPosition);
        }

        public long getChildId(int aGroupPosition, int aChildPosition) {
            return aChildPosition;
        }

        public int getChildrenCount(int aGroupPosition) {
            return mForums.get(aGroupPosition).getSubforums().size();
        }

        public Object getGroup(int aGroupPosition) {
            return mForums.get(aGroupPosition);
        }

        public long getGroupId(int aGroupPosition) {
            return aGroupPosition;
        }

        public int getGroupCount() {
            return mForums.size();
        }

        public boolean isChildSelectable(int aGroupPosition, int aChildPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return true;
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
            return true;
    	case R.id.refresh:
    		mLoadTask = new LoadForumsTask();
            mLoadTask.execute();
            return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
}
