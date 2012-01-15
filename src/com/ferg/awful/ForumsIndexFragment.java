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

import java.util.HashMap;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.dialog.LogOutDialog;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.provider.AwfulProvider;
import com.ferg.awful.service.AwfulCursorAdapter;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulForum;

public class ForumsIndexFragment extends Fragment implements AwfulUpdateCallback {
    private static final String TAG = "ForumsIndex";

    private ImageButton mUserCp;
    private ImageButton mPM;
    private TextView mPMcount;
    private ExpandableListView mForumList;
    private TextView mTitle;
    private ImageButton mRefresh;
    
    private int unreadPMCount;

    private AwfulPreferences mPrefs;
    
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message aMsg) {
            switch (aMsg.what) {
                case AwfulSyncService.MSG_SYNC_INDEX:
            		if(aMsg.arg1 == AwfulSyncService.Status.OKAY){
                		getActivity().getSupportLoaderManager().restartLoader(Constants.FORUM_INDEX_ID, null, mForumLoaderCallback);
            			loadingSucceeded();
            		}else if(aMsg.arg1 == AwfulSyncService.Status.ERROR){
            			loadingFailed();
            		}else if(aMsg.arg1 == AwfulSyncService.Status.WORKING){
            			loadingStarted();
            		}
                    break;
                default:
                    super.handleMessage(aMsg);
            }
        }
    };
    private AwfulTreeAdapter mCursorAdapter;
    private Messenger mMessenger = new Messenger(mHandler);
    private ForumContentsCallback mForumLoaderCallback = new ForumContentsCallback();
    
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        View result = aInflater.inflate(R.layout.forum_index, aContainer, false);

        mForumList = (ExpandableListView) result.findViewById(R.id.forum_list);

        if (AwfulActivity.useLegacyActionbar()) {
            View actionbar = ((ViewStub) result.findViewById(R.id.actionbar)).inflate();
            mTitle         = (TextView) actionbar.findViewById(R.id.title);
            mUserCp        = (ImageButton) actionbar.findViewById(R.id.user_cp);
            mPM        = (ImageButton) actionbar.findViewById(R.id.pm_button);
            mPMcount        = (TextView) actionbar.findViewById(R.id.pm_count);
            mRefresh       = (ImageButton) actionbar.findViewById(R.id.refresh);
        }
        
        mPrefs = new AwfulPreferences(getActivity());
        
        mForumList.setBackgroundColor(mPrefs.postBackgroundColor);
        mForumList.setCacheColorHint(mPrefs.postBackgroundColor);
        mForumList.setOnChildClickListener(onForumSelected);
        mForumList.setOnGroupClickListener(onParentForumSelected);
        mForumList.setOnItemLongClickListener(onForumLongclick);
        return result;
    }

    private boolean isTablet() {
        return ((AwfulActivity) getActivity()).isTablet();
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

        setRetainInstance(true);
        
        if (AwfulActivity.useLegacyActionbar()) {
            mTitle.setText(getString(R.string.forums_title));
            mUserCp.setOnClickListener(onButtonClick);
            mPM.setOnClickListener(onButtonClick);
            mRefresh.setOnClickListener(onButtonClick);
        }
        mCursorAdapter = new AwfulTreeAdapter(getActivity());
        mForumList.setAdapter(mCursorAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((AwfulActivity) getActivity()).registerSyncService(mMessenger, Constants.FORUM_INDEX_ID);

        boolean loggedIn = NetworkUtils.restoreLoginCookies(getActivity());
        if (loggedIn) {
            Log.e(TAG, "Cookie Loaded!");
        } else {
            startActivityForResult(new Intent().setClass(getActivity(), AwfulLoginActivity.class), 0);
        }
		getActivity().getSupportLoaderManager().restartLoader(Constants.FORUM_INDEX_ID, null, mForumLoaderCallback);
        syncForums();
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
    @Override
    public void onResume() {
        super.onResume();
    }
        
    @Override
    public void onStop() {
        super.onStop();
        ((AwfulActivity) getActivity()).unregisterSyncService(mMessenger, Constants.FORUM_INDEX_ID);
		getActivity().getSupportLoaderManager().destroyLoader(Constants.FORUM_INDEX_ID);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // The only activity we call for result is login
        // Odds are we want to refresh whether or not it was successful
        
        //refresh
    	syncForums();
    }

    private OnChildClickListener onForumSelected = new OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v,	int groupPosition, int childPosition, long id) {
            // If we've got two panes (tablet) then set the content pane, otherwise
            // push an activity as normal
            if (getActivity() instanceof ForumsTabletActivity && ((ForumsTabletActivity) getActivity()).isDualPane()) {
                ((ForumsTabletActivity) getActivity()).setContentPane((int) id);
            } else {
                startForumActivity((int) id);
            }
            return true;
        }
    };
    
    private OnGroupClickListener onParentForumSelected = new OnGroupClickListener() {
        @Override
        public boolean onGroupClick(ExpandableListView parent, View v,	int groupPosition, long id) {
            // If we've got two panes (tablet) then set the content pane, otherwise
            // push an activity as normal
            if (getActivity() instanceof ForumsTabletActivity && ((ForumsTabletActivity) getActivity()).isDualPane()) {
                ((ForumsTabletActivity) getActivity()).setContentPane((int) id);
            } else {
                startForumActivity((int) id);
            }
            return true;
        }
    };
    
    private OnItemLongClickListener onForumLongclick = new OnItemLongClickListener(){

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View v,
				int position, long id) {
			Log.e(TAG, "pos: "+position+" id: "+id+" unpId: "+ExpandableListView.getPackedPositionGroup(id)+" "+ExpandableListView.getPackedPositionChild(id) );
			if(ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_GROUP){
				int gId = ExpandableListView.getPackedPositionGroup(id);
				int gPos = mCursorAdapter.getGroupPosition(gId);
				if(mForumList.isGroupExpanded(gPos)){
					mForumList.collapseGroup(gPos);
				}else{
					mForumList.expandGroup(gPos);
				}
				return true;
			}
			return false;
		}
    	
    };

    private void startForumActivity(int aForumId) {
        Intent viewForum = new Intent().setClass(getActivity(), ForumDisplayActivity.class);
        viewForum.putExtra(Constants.FORUM, aForumId);
        startActivity(viewForum);
    }

    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.user_cp:
                    displayUserCP();
                    break;
                case R.id.pm_button:
                    startActivity(new Intent(getActivity(), PrivateMessageActivity.class));
                    break;
                case R.id.refresh:
                	syncForums();
                    break;
            }
        }
    };

    public void displayUserCP() {
        if (!isTablet()) {
            startActivity(new Intent().setClass(getActivity(), UserCPActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } else {
            UserCPFragment.newInstance(true).show(getFragmentManager(), "user_control_panel_dialog");
        }
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(menu.size() == 0){
            inflater.inflate(R.menu.forum_index_options, menu);
        }
    }
    
    @Override
    public void onPrepareOptionsMenu(Menu menu){
		MenuItem pm = menu.findItem(R.id.pm);
    	if(unreadPMCount >0){
            pm.setTitle(Integer.toString(unreadPMCount)+" Unread PM(s)");
            if(!AwfulActivity.useLegacyActionbar()){
            	pm.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            }
    	}else{
            if(!AwfulActivity.useLegacyActionbar()){
                pm.setTitle("");
            }else{
                pm.setTitle("Private Messages");
            }
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.user_cp:
                displayUserCP();
                break;
            case R.id.pm:
            	startActivity(new Intent().setClass(getActivity(), PrivateMessageActivity.class));
                break;
            case R.id.settings:
                startActivity(new Intent().setClass(getActivity(), SettingsActivity.class));
                break;
            case R.id.logout:
                new LogOutDialog(getActivity()).show();
                break;
            case R.id.refresh:
            	syncForums();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void loadingFailed() {
        Log.e(TAG, "Loading failed.");
        if (AwfulActivity.useLegacyActionbar()) {
            mRefresh.setVisibility(View.VISIBLE);
            mRefresh.setAnimation(null);
            mRefresh.setImageResource(android.R.drawable.ic_dialog_alert);
          //TODO mRefresh.startAnimation(adapt.getBlinkingAnimation());
        } else {
            getActivity().setProgressBarIndeterminateVisibility(false);
        }

        Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void loadingStarted() {
        if (AwfulActivity.useLegacyActionbar()) {
            mRefresh.setVisibility(View.VISIBLE);
            mRefresh.setImageResource(R.drawable.ic_menu_refresh);
          //TODO mRefresh.startAnimation(adapt.getRotateAnimation());
        } else {
            getActivity().setProgressBarIndeterminateVisibility(true);
        }
    }
    @Override
    public void loadingSucceeded() {
        if (AwfulActivity.useLegacyActionbar()) {
            mRefresh.setAnimation(null);
            mRefresh.setVisibility(View.GONE);
        } else {
            getActivity().setProgressBarIndeterminateVisibility(false);
        }
    }
    
	@Override
	public void onPreferenceChange(AwfulPreferences mPrefs) {
		if(mForumList != null){
			mForumList.setBackgroundColor(mPrefs.postBackgroundColor);
			if(mForumList.getChildCount() >4){//shitty workaround for: http://code.google.com/p/android/issues/detail?id=9775
				mForumList.setCacheColorHint(mPrefs.postBackgroundColor);
	        }
		}
	}
	
	private void syncForums() {
        ((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_SYNC_INDEX,Constants.FORUM_INDEX_ID,0);
    }
	
	private class ForumContentsCallback implements LoaderManager.LoaderCallbacks<Cursor> {

		@Override
		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
			Log.i(TAG,"Load Cursor.");
            return new CursorLoader(getActivity(), AwfulForum.CONTENT_URI, AwfulProvider.ForumProjection, AwfulForum.PARENT_ID+"=?", new String[]{Integer.toString(aId)}, AwfulForum.INDEX);
        }

		@Override
        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"Thread title finished, populating.");
        	if(aData.moveToFirst()){
        		int parent = aData.getInt(aData.getColumnIndex(AwfulForum.PARENT_ID));
        		if(parent == 0){
        			mCursorAdapter.setGroupCursor(aData);
        		}else{
        			int groupId = mCursorAdapter.getGroupPosition(parent);
        			if(groupId >0){
        				mCursorAdapter.setChildrenCursor(groupId, aData);
        			}
        		}
        	}
        }

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			mCursorAdapter.setGroupCursor(null);
		}
    }
	
	private class AwfulTreeAdapter extends CursorTreeAdapter{
		private LayoutInflater inf;
		public AwfulTreeAdapter(Context context) {
			super(null, context, false);
			inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		public int getGroupPosition(int parent) {
			Cursor groupCursor = getCursor();
			if(groupCursor!=null && groupCursor.moveToFirst()){
				int column = groupCursor.getColumnIndex(AwfulForum.ID);
				do{
					if(groupCursor.getInt(column) == parent){
						return groupCursor.getPosition();
					}
				}while(groupCursor.moveToNext());
			}
			return -1;
		}

		@Override
		protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
			AwfulForum.getView(view, mPrefs, cursor);
		}

		@Override
		protected void bindGroupView(View view, Context context, Cursor cursor,
				boolean isExpanded) {
			AwfulForum.getView(view, mPrefs, cursor);
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			int parentId = groupCursor.getInt(groupCursor.getColumnIndex(AwfulForum.ID));
			getActivity().getSupportLoaderManager().restartLoader(parentId, null, mForumLoaderCallback);
			return null;
		}

		@Override
		protected View newChildView(Context context, Cursor cursor,
				boolean isLastChild, ViewGroup parent) {
			View row = inf.inflate(R.layout.forum_item, parent, false);
			AwfulForum.getView(row, mPrefs, cursor);
			return row;
		}

		@Override
		protected View newGroupView(Context context, Cursor cursor,
				boolean isExpanded, ViewGroup parent) {
			View row = inf.inflate(R.layout.forum_item, parent, false);
			AwfulForum.getView(row, mPrefs, cursor);
			return row;
		}
		
	}
}
