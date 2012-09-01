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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.androidquery.AQuery;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.dialog.LogOutDialog;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulForum;

public class ForumsIndexFragment extends AwfulFragment implements AwfulUpdateCallback {
    protected static String TAG = "ForumsIndex";
    private ExpandableListView mForumList;
    private boolean mIsSidebar;

    public static ForumsIndexFragment newInstance() {
        return new ForumsIndexFragment();
    }
    
    private AwfulExpandableListAdapter mCursorAdapter;
    private ForumContentsCallback mForumLoaderCallback = new ForumContentsCallback();
    
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(false);
    }

    @Override
    public void onAttach(Activity aActivity) {
        super.onAttach(aActivity);
    }

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        View result = aInflater.inflate(R.layout.forum_index, aContainer, false);

        mForumList = (ExpandableListView) result.findViewById(R.id.forum_list);
        mForumList.setDrawingCacheEnabled(true);
        
        mForumList.setBackgroundColor(mPrefs.postBackgroundColor);
        mForumList.setCacheColorHint(mPrefs.postBackgroundColor);
        mForumList.setOnChildClickListener(onForumSelected);
        mForumList.setOnGroupClickListener(onParentForumSelected);
        mForumList.setOnItemLongClickListener(onForumLongclick);
        return result;
    }

    private boolean isSideBar() {
    	if(getActivity() instanceof ForumsIndexActivity){
    		return ((ForumsIndexActivity)getAwfulActivity()).isDualPane();
    	}
    	return false;
	}

	@Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);
        mIsSidebar = isSideBar();
        mCursorAdapter = new AwfulExpandableListAdapter(getActivity());
        mForumList.setAdapter(mCursorAdapter);
    }

    @Override
    public void onStart() {
        super.onStart(); Log.e(TAG, "Start");
    }
    
    @Override
    public void onResume() {
        super.onResume(); Log.e(TAG, "Resume");
		getActivity().getSupportLoaderManager().restartLoader(Constants.FORUM_INDEX_ID, null, mForumLoaderCallback);
    }

	@Override
	public void onPageVisible() {
		getLoaderManager().restartLoader(Constants.FORUM_INDEX_ID, null, mForumLoaderCallback);
	}

	@Override
	public void onPageHidden() {
		
	}
	
    @Override
    public void onPause() {
        super.onPause(); Log.e(TAG, "Pause");
		getActivity().getSupportLoaderManager().destroyLoader(Constants.FORUM_INDEX_ID);
    }
        
    @Override
    public void onStop() {
        super.onStop(); Log.e(TAG, "Stop");
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView(); Log.e(TAG, "DestroyView");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy(); Log.e(TAG, "Destroy");
    }

	@Override
	public void onDetach() {
		super.onDetach(); Log.e(TAG, "Detach");
	}

    private OnChildClickListener onForumSelected = new OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v,	int groupPosition, int childPosition, long id) {
			Log.e(TAG, "gpos: "+groupPosition+"cpos: "+childPosition+" id: "+id);
            // If we've got two panes (tablet) then set the content pane, otherwise
            // push an activity as normal
        	setSelected((int) id);
        	mForumList.invalidateViews();
            if (getActivity() != null) {
                getAwfulActivity().displayForum((int) id, 1);
            }
            return true;
        }
    };
    
    private OnGroupClickListener onParentForumSelected = new OnGroupClickListener() {
        @Override
        public boolean onGroupClick(ExpandableListView parent, View v,	int groupPosition, long id) {
			Log.e(TAG, "gpos: "+groupPosition+" id: "+id);
            // If we've got two panes (tablet) then set the content pane, otherwise
            // push an activity as normal
        	setSelected((int) id);
        	mForumList.invalidateViews();
            if (getActivity() != null) {
                getAwfulActivity().displayForum((int) id, 1);
            }
            return true;
        }
    };
    
    private OnItemLongClickListener onForumLongclick = new OnItemLongClickListener(){

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View v,
				int position, long id) {
			Log.e(TAG, "pos: "+position+" id: "+id+" unpId: "+ExpandableListView.getPackedPositionGroup(id)+" "+ExpandableListView.getPackedPositionChild(id));
			
			if(ExpandableListView.getPackedPositionChild(id) < 0){
				int gpos = mCursorAdapter.getGroupPosition(ExpandableListView.getPackedPositionGroup(id));
				if(mForumList.isGroupExpanded(gpos)){
					mForumList.collapseGroup(gpos);
				}else{
					mForumList.expandGroup(gpos);
				}
			}
			return true;
		}
    	
    };

    public void displayUserCP() {
    	if (getActivity() != null) {
            getAwfulActivity().displayForum(Constants.USERCP_ID, 1);
        }
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forum_index_options, menu);
    }
    
    @Override
    public void onPrepareOptionsMenu(Menu menu){
        MenuItem pm = menu.findItem(R.id.pm);
        Log.i(TAG, "Menu!!!!");

        pm.setEnabled(mPrefs.hasPlatinum);
        pm.setVisible(mPrefs.hasPlatinum);
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
    public void loadingFailed(Message aMsg) {
    	super.loadingFailed(aMsg);
        Log.e(TAG, "Loading failed.");
    	if(getActivity() != null){
        	Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
	public void loadingSucceeded(Message aMsg) {
		super.loadingSucceeded(aMsg);
		getLoaderManager().restartLoader(Constants.FORUM_INDEX_ID, null, mForumLoaderCallback);
	}
    
	@Override
	public void onPreferenceChange(AwfulPreferences mPrefs) {
		super.onPreferenceChange(mPrefs);
		if(mForumList != null){
			mForumList.setBackgroundColor(mPrefs.postBackgroundColor);
			mForumList.setCacheColorHint(mPrefs.postBackgroundColor);
			if(mCursorAdapter != null){
				mCursorAdapter.notifyDataSetChanged();
			}
		}
	}
	
	private void syncForums() {
		if(getActivity() != null){
			getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_SYNC_INDEX,Constants.FORUM_INDEX_ID,0);
		}
    }
	
	private class ForumContentsCallback implements LoaderManager.LoaderCallbacks<Cursor> {

		@Override
		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
			Log.i(TAG,"Load Index Cursor");
            return new CursorLoader(getActivity(), AwfulForum.CONTENT_URI, AwfulProvider.ForumProjection, null, null, AwfulForum.INDEX);
        }

		@Override
        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"Index cursor: "+aData.getCount());
        	if(aData.moveToFirst() && !aData.isClosed()){
    			mCursorAdapter.setCursor(aData);
        	}
        }

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			Log.e(TAG,"resetLoader: "+arg0.getId());
			mCursorAdapter.setCursor(null);
		}
    }
	
	public class ForumEntry{
		public int id;
		public int parentId;
		public String title;
		public String subtitle;
		public String tagUrl;
		public ArrayList<ForumEntry> subforums = new ArrayList<ForumEntry>();
		public ForumEntry(int aId, int parent, String aTitle, String aSubtitle, String aTagUrl){
			id = aId; parentId = parent; title = aTitle; subtitle = aSubtitle; tagUrl = aTagUrl;
		}
	}
	
	private class AwfulExpandableListAdapter extends BaseExpandableListAdapter {
		private LayoutInflater inf;
		private AQuery rowAq;
		private ArrayList<ForumEntry> parentForums = new ArrayList<ForumEntry>();
		private HashMap<Integer, ForumEntry> forums = new HashMap<Integer, ForumEntry>();
		
		public AwfulExpandableListAdapter(Context parent){
			rowAq = new AQuery(parent);
			inf = (LayoutInflater) parent.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		public void setCursor(Cursor data){
			parentForums.clear();
			forums.clear();
			if(data != null && !data.isClosed() && data.moveToFirst()){
				LinkedList<ForumEntry> tmpSubforums = new LinkedList<ForumEntry>();
				do{
					if(data.getInt(data.getColumnIndex(AwfulForum.ID)) <= 0){
						continue;
					}
					ForumEntry forum = new ForumEntry(data.getInt(data.getColumnIndex(AwfulForum.ID)),
													  data.getInt(data.getColumnIndex(AwfulForum.PARENT_ID)),
													  data.getString(data.getColumnIndex(AwfulForum.TITLE)),
													  data.getString(data.getColumnIndex(AwfulForum.SUBTEXT)),
													  data.getString(data.getColumnIndex(AwfulForum.TAG_URL))
													  );
					if(forum.parentId != 0){
						tmpSubforums.add(forum);
					}else{
						parentForums.add(forum);
					}
					forums.put(forum.id, forum);
				}while(data.moveToNext());
				//do subforums after parent forums, in case we have subforums out of order
				for(ForumEntry sub : tmpSubforums){
					ForumEntry parent = forums.get(sub.parentId);
					if(parent != null){
						parent.subforums.add(sub);
					}
				}
				tmpSubforums.clear();
	        	if(parentForums.size() < 5){
	        		syncForums();
	        	}
			}
			notifyDataSetChanged();
		}
		
		public ForumEntry getForum(int id){
			return forums.get(id);
		}
		
		public int getGroupPosition(int id){
			return parentForums.indexOf(forums.get(id));
		}
		
		@Override
		public ForumEntry getChild(int groupPosition, int childPosition) {
			if(parentForums.size() < 1){
				return null;
			}
			return parentForums.get(groupPosition).subforums.get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			if(parentForums.size() < 1){
				return 0;
			}
			return parentForums.get(groupPosition).subforums.get(childPosition).id;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			if(convertView == null || convertView.getId() == R.layout.thread_item){
				convertView = inf.inflate(R.layout.thread_item, parent, false);
			}
			ForumEntry forum = parentForums.get(groupPosition).subforums.get(childPosition);
			AwfulForum.getExpandableForumView(convertView,
							   rowAq,
							   mPrefs,
							   forum,
							   selectedId > -1 && selectedId == forum.id,
							   false);
			getAwfulActivity().setPreferredFont(convertView);
			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			if(parentForums.size() < 1){
				return 0;
			}
			return parentForums.get(groupPosition).subforums.size();
		}

		@Override
		public ForumEntry getGroup(int groupPosition) {
			return parentForums.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return parentForums.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			if(parentForums.size() < 1){
				return 0;
			}
			return parentForums.get(groupPosition).id;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			if(convertView == null || convertView.getId() == R.layout.thread_item){
				convertView = inf.inflate(R.layout.thread_item, parent, false);
			}
			ForumEntry forum = parentForums.get(groupPosition);
			AwfulForum.getExpandableForumView(convertView,
							   rowAq,
							   mPrefs,
							   forum,
							   selectedId > -1 && selectedId == forum.id,
							   forum.subforums.size() > 0);
			getAwfulActivity().setPreferredFont(convertView);
			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
		
	}
	
	private int selectedId = -1;
	public void setSelected(int id){
		selectedId = id;
	}
	
	public int getSelected(){
		return selectedId;
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
}
