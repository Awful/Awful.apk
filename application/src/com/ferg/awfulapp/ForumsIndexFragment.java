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


import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
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
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshExpandableListView;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;

public class ForumsIndexFragment extends AwfulFragment implements AwfulUpdateCallback {
    protected static String TAG = "ForumsIndex";
    private long lastUpdateTime = System.currentTimeMillis();//This will be replaced with the correct time when we get the cursor.
    private boolean DEBUG = false;
    
    private boolean loadFailed = false;

    public static ForumsIndexFragment newInstance() {
        return new ForumsIndexFragment();
    }
    
    private TreeViewList mForumTree;
//    private PullToRefreshExpandableListView mForumList;
    
    private AwfulTreeListAdapter mTreeAdapter;
	private InMemoryTreeStateManager<ForumEntry> dataManager;
	
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

        View result = inflateView(R.layout.forum_index, aContainer, aInflater);
        
        mForumTree = (TreeViewList) result.findViewById(R.id.index_tree_view);
        
        mForumTree.setBackgroundColor(mPrefs.postBackgroundColor);
        mForumTree.setCacheColorHint(mPrefs.postBackgroundColor);

//        mForumList = (PullToRefreshExpandableListView) result.findViewById(R.id.forum_list);
//        mForumList.setDrawingCacheEnabled(true);
//        
//        mForumList.setBackgroundColor(mPrefs.postBackgroundColor);
//        mForumList.getRefreshableView().setCacheColorHint(mPrefs.postBackgroundColor);
//        mForumList.getRefreshableView().setOnChildClickListener(onForumSelected);
//        mForumList.getRefreshableView().setOnGroupClickListener(onParentForumSelected);
//        mForumList.getRefreshableView().setOnItemLongClickListener(onForumLongclick);
//        mForumList.setOnRefreshListener(new OnRefreshListener<ExpandableListView>() {
//			
//			@Override
//			public void onRefresh(PullToRefreshBase<ExpandableListView> refreshView) {
//				syncForums();
//			}
//		});
//        mForumList.setDisableScrollingWhileRefreshing(false);
//        mForumList.setMode(Mode.PULL_DOWN_TO_REFRESH);
//        mForumList.setPullLabel("Pull to Refresh");
//        mForumList.setReleaseLabel("Release to Refresh");
//        mForumList.setRefreshingLabel("Loading...");
        return result;
    }

	@Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);
        dataManager = new InMemoryTreeStateManager<ForumEntry>();
        dataManager.setVisibleByDefault(false);
        mTreeAdapter = new AwfulTreeListAdapter(getActivity(), dataManager);
        mForumTree.setAdapter(mTreeAdapter);
    }

    @Override
    public void onStart() {
        super.onStart(); if(DEBUG) Log.e(TAG, "Start");
    }
    
    @Override
    public void onResume() {
        super.onResume(); if(DEBUG) Log.e(TAG, "Resume");
		getActivity().getSupportLoaderManager().restartLoader(Constants.FORUM_INDEX_LOADER_ID, null, mForumLoaderCallback);
    }

	@Override
	public void onPageVisible() {
		if(getActivity() != null){
			getLoaderManager().restartLoader(Constants.FORUM_INDEX_LOADER_ID, null, mForumLoaderCallback);
		}
	}

	@Override
	public void onPageHidden() {
		
	}
	
    @Override
    public void onPause() {
        super.onPause(); if(DEBUG) Log.e(TAG, "Pause");
		getActivity().getSupportLoaderManager().destroyLoader(Constants.FORUM_INDEX_LOADER_ID);
    }
        
    @Override
    public void onStop() {
        super.onStop(); if(DEBUG) Log.e(TAG, "Stop");
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView(); if(DEBUG) Log.e(TAG, "DestroyView");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy(); if(DEBUG) Log.e(TAG, "Destroy");
    }

	@Override
	public void onDetach() {
		super.onDetach(); if(DEBUG) Log.e(TAG, "Detach");
	}

    public void displayUserCP() {
    	if (getActivity() != null) {
            getAwfulActivity().displayForum(Constants.USERCP_ID, 1);
        }
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	menu.clear();
    	if(menu.size() == 0){
    		inflater.inflate(R.menu.forum_index_options, menu);
    	}
    }
    
    @Override
    public void onPrepareOptionsMenu(Menu menu){
        MenuItem pm = menu.findItem(R.id.pm);
        Log.i(TAG, "Menu!!!!");
        if(pm != null){
            pm.setEnabled(mPrefs.hasPlatinum);
            pm.setVisible(mPrefs.hasPlatinum);
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
    public void loadingFailed(Message aMsg) {
    	super.loadingFailed(aMsg);
        Log.e(TAG, "Loading failed.");
		if(aMsg.obj == null && getActivity() != null){
			Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
		}
//    	mForumList.onRefreshComplete();
//    	mForumList.setLastUpdatedLabel("Loading Failed!");
    	loadFailed = true;
    }
    
    @Override
	public void loadingSucceeded(Message aMsg) {
		super.loadingSucceeded(aMsg);
		setProgress(100);
		getLoaderManager().restartLoader(Constants.FORUM_INDEX_LOADER_ID, null, mForumLoaderCallback);
//    	mForumList.onRefreshComplete();
//    	mForumList.setLastUpdatedLabel("Updated @ "+new SimpleDateFormat("h:mm a").format(new Date()));
	}
    
	@Override
	public void onPreferenceChange(AwfulPreferences mPrefs) {
		super.onPreferenceChange(mPrefs);
//		if(mForumList != null){
//			mForumList.setBackgroundColor(mPrefs.postBackgroundColor);
//			mForumList.getRefreshableView().setCacheColorHint(mPrefs.postBackgroundColor);
//			mForumList.setTextColor(mPrefs.postFontColor, mPrefs.postFontColor2);
			if(mTreeAdapter != null){
				mTreeAdapter.notifyDataSetChanged();
			}
//		}
	}
	
	private void syncForumsIfStale() {
		if(getActivity() != null && lastUpdateTime < System.currentTimeMillis()-(60000*1440*7)){
			getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_SYNC_INDEX,Constants.FORUM_INDEX_ID,0);
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
        		int dateIndex = aData.getColumnIndex(AwfulProvider.UPDATED_TIMESTAMP);
        		if(aData.moveToLast() && dateIndex > -1){
        			String timestamp = aData.getString(dateIndex);
        			Timestamp upDate = new Timestamp(System.currentTimeMillis());
        			if(timestamp != null && timestamp.length()>5){
            			upDate = Timestamp.valueOf(timestamp);
        			}
        			lastUpdateTime = upDate.getTime();
        			syncForumsIfStale();
//        	        mForumList.setLastUpdatedLabel("Updated "+new SimpleDateFormat("E @ h:mm a").format(upDate));
        		}
        		mTreeAdapter.setCursor(aData);
        	}
        }

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			Log.e(TAG,"resetLoader: "+arg0.getId());
			mTreeAdapter.setCursor(null);
		}
    }
	
	public static class ForumEntry{
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
	
	public static void parseForumTree(ArrayList<ForumEntry> primaryForums, SparseArray<ForumEntry> forumMap, Cursor data){
		primaryForums.clear();
		forumMap.clear();
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
					primaryForums.add(forum);
				}
				forumMap.put(forum.id, forum);
			}while(data.moveToNext());
			//do subforums after parent forums, in case we have subforums out of order, which will happen
			for(ForumEntry sub : tmpSubforums){
				ForumEntry parent = forumMap.get(sub.parentId);
				if(parent != null){
					parent.subforums.add(sub);
				}
			}
			tmpSubforums.clear();
		}
	}
	
	public static ArrayList<ForumEntry> updateForumTree(ArrayList<ForumEntry> primaryForums, SparseArray<ForumEntry> forumMap, Cursor data){
		ArrayList<ForumEntry> newForums = new ArrayList<ForumEntry>();
		if(data != null && !data.isClosed() && data.moveToFirst()){
			if(data.getCount() < forumMap.size()){
				primaryForums.clear();
				forumMap.clear();
			}
			LinkedList<ForumEntry> tmpSubforums = new LinkedList<ForumEntry>();
			do{
				if(data.getInt(data.getColumnIndex(AwfulForum.ID)) <= 0){
					continue;
				}
				ForumEntry current = forumMap.get(data.getInt(data.getColumnIndex(AwfulForum.ID)));
				if(current != null){
					current.parentId = data.getInt(data.getColumnIndex(AwfulForum.PARENT_ID));
					current.title = data.getString(data.getColumnIndex(AwfulForum.TITLE));
					current.subtitle = data.getString(data.getColumnIndex(AwfulForum.SUBTEXT));
					current.tagUrl = data.getString(data.getColumnIndex(AwfulForum.TAG_URL));
				}else{
					ForumEntry forum = new ForumEntry(data.getInt(data.getColumnIndex(AwfulForum.ID)),
												  data.getInt(data.getColumnIndex(AwfulForum.PARENT_ID)),
												  data.getString(data.getColumnIndex(AwfulForum.TITLE)),
												  data.getString(data.getColumnIndex(AwfulForum.SUBTEXT)),
												  data.getString(data.getColumnIndex(AwfulForum.TAG_URL))
												  );
					if(forum.parentId != 0){
						tmpSubforums.add(forum);
					}else{
						primaryForums.add(forum);
					}
					forumMap.put(forum.id, forum);
					newForums.add(forum);
				}
			}while(data.moveToNext());
			//do subforums after parent forums, in case we have subforums out of order, which will happen
			for(ForumEntry sub : tmpSubforums){
				ForumEntry parent = forumMap.get(sub.parentId);
				if(parent != null){
					parent.subforums.add(sub);
				}
			}
			tmpSubforums.clear();
		}
		return newForums;
	}
	
	private class AwfulTreeListAdapter extends AbstractTreeViewAdapter<ForumEntry>{
		private ArrayList<ForumEntry> parentForums = new ArrayList<ForumEntry>();
		private SparseArray<ForumEntry> forumsMap = new SparseArray<ForumEntry>();
		private TreeBuilder<ForumEntry> builder;
		private LayoutInflater inf;
		private AQuery rowAq;

		public AwfulTreeListAdapter(Activity activity, TreeStateManager<ForumEntry> stateManager) {
			super(activity, stateManager, 5);
			rowAq = new AQuery((Context)activity);//don't let aquery think we are using an actual activity, we will recycle in rows as we generate them
			inf = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			builder = new TreeBuilder<ForumsIndexFragment.ForumEntry>(stateManager);
		}
		
		public void setCursor(Cursor data){
			if(forumsMap.size() == 0){
				parseForumTree(parentForums, forumsMap, data);
				builder.clear();
	        	for(ForumEntry parent : parentForums){
	        		builder.sequentiallyAddNextNode(parent, 0);
	        		addChildren(builder, parent, 1);
	        	}
			}else{
				ArrayList<ForumEntry> newForums = updateForumTree(parentForums, forumsMap, data);
				for(ForumEntry item : newForums){
					if(item.parentId == 0){
						builder.sequentiallyAddNextNode(item, 0);
					}else{
						ForumEntry parent = forumsMap.get(item.parentId);
						if(parent != null){
							builder.addRelation(parent, item);
						}
					}
				}
			}
        	if(forumsMap.size() < 5 && !loadFailed){
        		syncForums();
        	}
		}
		
		private void addChildren(TreeBuilder<ForumEntry> builder, ForumEntry parent, int level){
			Log.e(TAG, level+" - Adding children for #"+parent.id+" - "+parent.title+" - "+parent.subforums.size());
    		for(ForumEntry child : parent.subforums){
        		builder.sequentiallyAddNextNode(child, level);
        		addChildren(builder, child, level+1);
            	dataManager.collapseChildren(parent);
    		}
		}

		@Override
		public long getItemId(int position) {
			return getTreeId(position).id;
		}

		@Override
		public View getNewChildView(TreeNodeInfo<ForumEntry> treeNodeInfo) {
			ForumEntry data = treeNodeInfo.getId();
			View row = inf.inflate(R.layout.thread_item, null, false);
			AwfulForum.getExpandableForumView(row,
							   rowAq,
							   mPrefs,
							   data,
							   selectedId > -1 && selectedId == data.id,
							   false);
			getAwfulActivity().setPreferredFont(row);
			return row;
		}

		@Override
		public View updateView(View row, TreeNodeInfo<ForumEntry> treeNodeInfo) {
			ForumEntry data = treeNodeInfo.getId();
			if(row == null){
				row = inf.inflate(R.layout.thread_item, null, false);
			}
			AwfulForum.getExpandableForumView(row,
							   rowAq,
							   mPrefs,
							   data,
							   selectedId > -1 && selectedId == data.id,
							   false);
			getAwfulActivity().setPreferredFont(row);
			return row;
		}

		@Override
		public Object getItem(int position) {
			return getTreeId(position);
		}

		@Override
		public void handleItemClick(View view, Object id) {
			ForumEntry data = (ForumEntry) id;
			//dataManager.expandDirectChildren(data);
			//Log.i(TAG, view+" - Clicked: "+data.id+" - "+data.title);
			displayForum(data.id, 1);
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
	
	@Override
	public boolean canSplitscreen() {
		return Constants.isWidescreen(getActivity());
	}
}
