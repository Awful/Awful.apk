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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.androidquery.AQuery;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.dialog.LogOutDialog;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.widget.AwfulHeaderTransformer;

import pl.polidea.treeview.*;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher.Options;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.AbsListViewDelegate;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

public class ForumsIndexFragment extends AwfulFragment implements AwfulUpdateCallback, PullToRefreshAttacher.OnRefreshListener {
    
    private int selectedForum = 0;

    public static ForumsIndexFragment newInstance() {
        return new ForumsIndexFragment();
    }
    
    private TreeViewList mForumTree;
//    private PullToRefreshExpandableListView mForumList;
    
    private AwfulTreeListAdapter mTreeAdapter;
	private InMemoryTreeStateManager<ForumEntry> dataManager;
    
	private View mProbationBar;
	private TextView mProbationMessage;
	private ImageButton mProbationButton;
	
	
    private ForumContentsCallback mForumLoaderCallback = new ForumContentsCallback();

    public ForumsIndexFragment() {
        TAG = "ForumsIndex";
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState); if(DEBUG) Log.e(TAG, "onCreate"+(savedInstanceState != null?" + saveState":""));
        setHasOptionsMenu(true);
        setRetainInstance(false);
    }

    @Override
    public void onAttach(Activity aActivity) {
        super.onAttach(aActivity); if(DEBUG) Log.e(TAG, "onAttach");
    	mP2RAttacher = this.getAwfulActivity().getPullToRefreshAttacher();
    }

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        if(DEBUG) Log.e(TAG, "onCreateView");
        View result = inflateView(R.layout.forum_index, aContainer, aInflater);
        
        mForumTree = (TreeViewList) result.findViewById(R.id.index_pull_tree_view);
        if(mP2RAttacher != null){
            mP2RAttacher.addRefreshableView(mForumTree,new AbsListViewDelegate(), this);
            mP2RAttacher.setPullFromBottom(false);
        	mP2RAttacher.setEnabled(true);
        }
        mForumTree.setBackgroundColor(ColorProvider.getBackgroundColor(mPrefs));
        mForumTree.setCacheColorHint(ColorProvider.getBackgroundColor(mPrefs));

		mProbationBar = (View) result.findViewById(R.id.probationbar);
		mProbationMessage = (TextView) result.findViewById(R.id.probation_message);
		mProbationButton  = (ImageButton) result.findViewById(R.id.go_to_LC);
		updateProbationBar();
        return result;
    }

	@Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState); if(DEBUG) Log.e(TAG, "Start");
        dataManager = new InMemoryTreeStateManager<ForumEntry>();
        dataManager.setVisibleByDefault(false);
        mTreeAdapter = new AwfulTreeListAdapter(getActivity(), dataManager);
        mForumTree.setAdapter(mTreeAdapter);
        syncForums();
    }

    @Override
    public void onStart() {
        super.onStart(); if(DEBUG) Log.e(TAG, "Start");
    }
    
    @Override
    public void onResume() {
        super.onResume(); if(DEBUG) Log.e(TAG, "Resume");
		getActivity().getSupportLoaderManager().restartLoader(Constants.FORUM_INDEX_LOADER_ID, null, mForumLoaderCallback);
		updateProbationBar();
    }

	@Override
	public void onPageVisible() {
		if(DEBUG) Log.e(TAG, "onPageVisible");
		if(getActivity() != null){
			getLoaderManager().restartLoader(Constants.FORUM_INDEX_LOADER_ID, null, mForumLoaderCallback);
		}
	}
	
	@Override
	public void onPageHidden() {
		if(DEBUG) Log.e(TAG, "onPageHidden");
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
    }
    
    @Override
	public void loadingSucceeded(Message aMsg) {
		super.loadingSucceeded(aMsg);
		setProgress(100);
		getLoaderManager().restartLoader(Constants.FORUM_INDEX_LOADER_ID, null, mForumLoaderCallback);
	}
    
	@Override
	public void onPreferenceChange(AwfulPreferences mPrefs) {
		super.onPreferenceChange(mPrefs);
		if(mForumTree != null){
			mForumTree.setBackgroundColor(ColorProvider.getBackgroundColor(mPrefs));
			mForumTree.setCacheColorHint(ColorProvider.getBackgroundColor(mPrefs));
			if(dataManager != null){
				dataManager.refresh();
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
        	if(aData != null && !aData.isClosed() && aData.moveToFirst()){
            	Log.v(TAG,"Index cursor: "+aData.getCount());
        		int dateIndex = aData.getColumnIndex(AwfulProvider.UPDATED_TIMESTAMP);
        		if(aData.getCount() > 10 && aData.move(8) && dateIndex > -1){
        			String timestamp = aData.getString(dateIndex);
        			Timestamp upDate = new Timestamp(System.currentTimeMillis());
        			if(timestamp != null && timestamp.length()>5){
            			upDate = Timestamp.valueOf(timestamp);
        			}
        		}
        		mTreeAdapter.setCursor(aData);
        	}
			updateProbationBar();
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
	
	public static void updateForumTree(ArrayList<ForumEntry> primaryForums, SparseArray<ForumEntry> forumMap, Cursor data){
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
			updateForumTree(parentForums, forumsMap, data);
			builder.clear();
        	for(ForumEntry parent : parentForums){
        		builder.sequentiallyAddNextNode(parent, 0);
        		addChildren(builder, parent, 1);
        	}
        	if(selectedForum > 0){
        		ForumEntry forum = forumsMap.get(selectedForum);
        		//iterate up the tree and show children as we go.
        		//it's recursive to catch subsubforums in games
        		while(forum != null && forum.parentId > 0 && forumsMap.get(forum.parentId) != null){
        			forum = forumsMap.get(forum.parentId);
        			dataManager.expandDirectChildren(forum);
        		}
        	}
		}
		
		private void addChildren(TreeBuilder<ForumEntry> builder, ForumEntry parent, int level){
			//if(DEBUG) Log.e(TAG, level+" - Adding children for #"+parent.id+" - "+parent.title+" - "+parent.subforums.size());
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
			View row = inf.inflate(R.layout.forum_item, null, false);
			AwfulForum.getExpandableForumView(row,
							   rowAq,
							   mPrefs,
							   data,
							   selectedForum > 0 && selectedForum == data.id,
							   false);
			getAwfulActivity().setPreferredFont(row);
			return row;
		}

		@Override
		public View updateView(View row, TreeNodeInfo<ForumEntry> treeNodeInfo) {
			ForumEntry data = treeNodeInfo.getId();
			if(row == null){
				row = inf.inflate(R.layout.forum_item, null, false);
			}
			AwfulForum.getExpandableForumView(row,
							   rowAq,
							   mPrefs,
							   data,
							   selectedForum > 0 && selectedForum == data.id,
							   false);
			getAwfulActivity().setPreferredFont(row);
			updateProbationBar();
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
			selectedForum = data.id;
			displayForum(data.id, 1);
			dataManager.refresh();
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
	public boolean canSplitscreen() {
		return Constants.isWidescreen(getActivity());
	}
	
	@Override
	public String getInternalId() {
		return TAG;
	}
	
	@Override
	public boolean canScrollX(int x, int y) {
		if(mPrefs.lockScrolling){
			return true;
		}
		return false;
	}

	@Override
	public boolean volumeScroll(KeyEvent event) {
		int action = event.getAction();
	    int keyCode = event.getKeyCode();    
	        switch (keyCode) {
	        case KeyEvent.KEYCODE_VOLUME_UP:
	            if (action == KeyEvent.ACTION_DOWN) {
	            	mForumTree.smoothScrollBy(-mForumTree.getHeight()/2, 0);
	            }
	            return true;
	        case KeyEvent.KEYCODE_VOLUME_DOWN:
	            if (action == KeyEvent.ACTION_DOWN) {
	            	mForumTree.smoothScrollBy(mForumTree.getHeight()/2, 0);
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

	@Override
	public void onRefreshStarted(View view) {
		syncForums();
	}
}
