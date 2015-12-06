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

package com.ferg.awfulapp;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.androidquery.AQuery;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.IndexIconRequest;
import com.ferg.awfulapp.task.IndexRequest;
import com.ferg.awfulapp.thread.AwfulForum;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import java.util.Date;

public class ForumsIndexFragment extends AwfulFragment implements SwipyRefreshLayout.OnRefreshListener {


	private static final int[] EMPTY_STATE_SET = {};
	private static final int[] GROUP_EXPANDED_STATE_SET =
			{android.R.attr.state_expanded};
	private static final int[][] GROUP_STATE_SETS = {
			EMPTY_STATE_SET, // 0
			GROUP_EXPANDED_STATE_SET // 1
	};

    private int selectedForum = 0;
    
    private ExpandableListView mForumIndex;
    
    private AwfulTreeListAdapter mTreeAdapter;
    
	private View mProbationBar;
	private TextView mProbationMessage;
	private ImageButton mProbationButton;

    private ForumContentObserver forumObserver;
	
    private ForumContentsCallback mForumLoaderCallback = new ForumContentsCallback();

    public ForumsIndexFragment() {
        TAG = "ForumsIndex";
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState); if(DEBUG) Log.e(TAG, "onCreate"+(savedInstanceState != null?" + saveState":""));
        setHasOptionsMenu(true);
        setRetainInstance(false);
        forumObserver = new ForumContentObserver(mHandler);
    }

    @Override
    public void onAttach(Activity aActivity) {
        super.onAttach(aActivity); if(DEBUG) Log.e(TAG, "onAttach");
    }

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        if(DEBUG) Log.e(TAG, "onCreateView");
        View result = inflateView(R.layout.forum_index, aContainer, aInflater);
        
        mForumIndex = (ExpandableListView) result.findViewById(R.id.index_view);
        mForumIndex.setBackgroundColor(ColorProvider.getBackgroundColor());
        mForumIndex.setCacheColorHint(ColorProvider.getBackgroundColor());

		mProbationBar = result.findViewById(R.id.probationbar);
		mProbationMessage = (TextView) result.findViewById(R.id.probation_message);
		mProbationButton  = (ImageButton) result.findViewById(R.id.go_to_LC);

		updateProbationBar();
        return result;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mSRL = (SwipyRefreshLayout) view.findViewById(R.id.index_swipe);
        mSRL.setOnRefreshListener(this);
        mSRL.setColorSchemeResources(
				android.R.color.holo_green_light,
				android.R.color.holo_orange_light,
				android.R.color.holo_red_light,
				android.R.color.holo_blue_bright);
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);
        mTreeAdapter = new AwfulTreeListAdapter(null, getActivity());
		mForumIndex.setAdapter(mTreeAdapter);
        syncForums();
    }
    
    @Override
    public void onResume() {
        super.onResume();
		restartLoader(Constants.FORUM_INDEX_LOADER_ID, null, mForumLoaderCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulForum.CONTENT_URI, true, forumObserver);
		updateProbationBar();
    }

	@Override
	public void onPageVisible() {
		if(DEBUG) Log.e(TAG, "onPageVisible");
//		if(mP2RAttacher != null){
//			mP2RAttacher.setPullFromBottom(false);
//		}
	}
	
	@Override
	public void onPageHidden() {
		if(DEBUG) Log.e(TAG, "onPageHidden");
	}

    @Override
    public String getInternalId() {
        return TAG;
    }


	@Override
	protected void cancelNetworkRequests() {
		super.cancelNetworkRequests();
		NetworkUtils.cancelRequests(IndexRequest.REQUEST_TAG);
	}

	@Override
    public void onPause() {
        super.onPause();
        getActivity().getContentResolver().unregisterContentObserver(forumObserver);
		getLoaderManager().destroyLoader(Constants.FORUM_INDEX_LOADER_ID);
    }

    public void displayUserCP() {
    	if (getActivity() != null) {
            getAwfulActivity().displayForum(Constants.USERCP_ID, 1);
        }
    }

    
	@Override
	public void onPreferenceChange(AwfulPreferences mPrefs, String key) {
		super.onPreferenceChange(mPrefs, key);
		if(mForumIndex != null){
			mForumIndex.setBackgroundColor(ColorProvider.getBackgroundColor());
			mForumIndex.setCacheColorHint(ColorProvider.getBackgroundColor());
		}
	}
	
	private void syncForums() {
        if(getActivity() != null){
			// cancel pending forum index loading requests
			NetworkUtils.cancelRequests(IndexRequest.REQUEST_TAG);
			// call this with cancelOnDestroy=false to retain the request's specific type tag
            queueRequest(new IndexRequest(getActivity()).build(this, new AwfulRequest.AwfulResultCallback<Void>() {
                @Override
                public void success(Void result) {
                    restartLoader(Constants.FORUM_INDEX_LOADER_ID, null, mForumLoaderCallback);
                }

                @Override
                public void failure(VolleyError error) {
                    if(null != error.getMessage() && error.getMessage().startsWith("java.net.ProtocolException: Too many redirects")){
                        Log.e(TAG, "Error: "+error.getMessage());
						Log.e(TAG, "!!!Failed to sync forum index - You are now LOGGED OUT");
                        NetworkUtils.clearLoginCookies(getAwfulActivity());
                        getAwfulActivity().startActivity(new Intent().setClass(getAwfulActivity(), AwfulLoginActivity.class));
                    }
                    restartLoader(Constants.FORUM_INDEX_LOADER_ID, null, mForumLoaderCallback);
                }
            }), false);
			queueRequest(new IndexIconRequest(getActivity()).build(this, new AwfulRequest.AwfulResultCallback<Void>() {
				public void success(Void result) {
					restartLoader(Constants.FORUM_INDEX_LOADER_ID, null, mForumLoaderCallback);
				}

				public void failure(VolleyError error) {
					restartLoader(Constants.FORUM_INDEX_LOADER_ID, null, mForumLoaderCallback);
				}
			}));
        }
    }

	@Override
	public void onRefresh(SwipyRefreshLayoutDirection swipyRefreshLayoutDirection) {
		syncForums();
	}

	private class ForumContentObserver extends ContentObserver{
        public ForumContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            restartLoader(Constants.FORUM_INDEX_LOADER_ID, null, mForumLoaderCallback);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            restartLoader(Constants.FORUM_INDEX_LOADER_ID, null, mForumLoaderCallback);
        }
    }
	
	private class ForumContentsCallback implements LoaderManager.LoaderCallbacks<Cursor> {

		@Override
		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
			if(DEBUG) Log.i(TAG,"Load Index Cursor");
            return new CursorLoader(getActivity(),
					AwfulForum.CONTENT_URI,
					AwfulProvider.ForumProjection,
					AwfulProvider.TABLE_FORUM+"."+ AwfulForum.PARENT_ID+"== 0",
					null,
					AwfulForum.INDEX);
        }

		@Override
        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	if(aData != null && !aData.isClosed() && aData.moveToFirst()){
            	Log.v(TAG, "Index cursor: " + aData.getCount());
        		if(aData.getCount() > 10){
        			aData.move(8);
        		}
				mTreeAdapter.setGroupCursor(aData);
        	}
			updateProbationBar();
        }

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			Log.e(TAG, "resetLoader: " + arg0.getId());
		}
    }
	
	public static class ForumEntry{
		public int id;
		public int parentId;
		public String title;
		public String subtitle;
		public String tagUrl;
		public ForumEntry(int aId, int parent, String aTitle, String aSubtitle, String aTagUrl){
			id = aId; parentId = parent; title = aTitle; subtitle = aSubtitle; tagUrl = aTagUrl;
		}
	}
	
	private class AwfulTreeListAdapter extends CursorTreeAdapter {
		private LayoutInflater inf;
		private AQuery rowAq;

		public AwfulTreeListAdapter(Cursor cursor, Activity activity) {
			super(cursor, activity);
			rowAq = new AQuery((Context)activity);//don't let aquery think we are using an actual activity, we will recycle in rows as we generate them
			inf = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			CursorLoader cl = new CursorLoader(getActivity(),
					AwfulForum.CONTENT_URI,
					AwfulProvider.ForumProjection,
					AwfulProvider.TABLE_FORUM + "." + AwfulForum.PARENT_ID + "== " + groupCursor.getInt(groupCursor.getColumnIndex(AwfulForum.ID)),
					null,
					AwfulForum.INDEX);
			return cl.loadInBackground();
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v = super.getGroupView(groupPosition, isExpanded, convertView, parent);
			View ind = v.findViewById( R.id.explist_indicator);
			if( ind != null ) {
				ImageView indicator = (ImageView)ind;
				if( getChildrenCount( groupPosition ) < 1 ) {
					indicator.setVisibility( View.INVISIBLE );
				} else {
					indicator.setVisibility( View.VISIBLE );
					int stateSetIndex = ( isExpanded ? 1 : 0) ;
					Drawable drawable = indicator.getDrawable();
					drawable.setState(GROUP_STATE_SETS[stateSetIndex]);
				}
			}
			return v;
		}


		@Override
		protected View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
			View row = inf.inflate(R.layout.forum_item_mainforum, parent, false);
			makeForumEntry(row, cursor, true);
			return row;
		}

		@Override
		protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
			makeForumEntry(view, cursor, true);
		}

		@Override
		protected View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
			View row = inf.inflate(R.layout.forum_item_subforum, null, false);
			makeForumEntry(row, cursor, false);
			return row;
		}

		@Override
		protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
			makeForumEntry(view, cursor, false);

		}

		private void makeForumEntry(View view, Cursor cursor, boolean hasChildren) {
			ForumEntry data = new ForumEntry(cursor.getInt(cursor.getColumnIndex(AwfulForum.ID)),
					cursor.getInt(cursor.getColumnIndex(AwfulForum.PARENT_ID)),
					cursor.getString(cursor.getColumnIndex(AwfulForum.TITLE)),
					cursor.getString(cursor.getColumnIndex(AwfulForum.SUBTEXT)),
					cursor.getString(cursor.getColumnIndex(AwfulForum.TAG_URL))
			);

			AwfulForum.getExpandableForumView(view,
					rowAq,
					mPrefs,
					data,
					selectedForum > 0 && selectedForum == data.id,
					hasChildren);

			View thread = view.findViewById(R.id.thread_box);
			if(null != thread){
				final int id = cursor.getInt(cursor.getColumnIndex(AwfulForum.ID));
				thread.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						selectedForum = id;
						displayForum(id, 1);
					}
				});
			}
			getAwfulActivity().setPreferredFont(view);
		}

	}
	
	@Override
	public String getTitle() {
		if(getActivity() != null){
			return getResources().getString(R.string.forums_title);
		}else{
			return "Forums";
		}
	}

	public void refresh() {
		syncForums();
	}

	@Override
	public boolean volumeScroll(KeyEvent event) {
		int action = event.getAction();
	    int keyCode = event.getKeyCode();    
	        switch (keyCode) {
	        case KeyEvent.KEYCODE_VOLUME_UP:
	            if (action == KeyEvent.ACTION_DOWN) {
	            	mForumIndex.smoothScrollBy(-mForumIndex.getHeight()/2, 0);
	            }
	            return true;
	        case KeyEvent.KEYCODE_VOLUME_DOWN:
	            if (action == KeyEvent.ACTION_DOWN) {
	            	mForumIndex.smoothScrollBy(mForumIndex.getHeight()/2, 0);
	            }
	            return true;
	        default:
	            return false;
	        }
	}
	
	public void updateProbationBar(){
		if(!mPrefs.isOnProbation()){
			mProbationBar.setVisibility(View.GONE);
			return;
		}
		mProbationBar.setVisibility(View.VISIBLE);
		mProbationMessage.setText(String.format(this.getResources().getText(R.string.probation_message).toString(),new Date(mPrefs.probationTime).toLocaleString()));
		mProbationButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent openThread = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.FUNCTION_BANLIST+'?'+Constants.PARAM_USER_ID+"="+mPrefs.userId));
				startActivity(openThread);
			}
		});
	}

}
