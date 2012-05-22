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


import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.*;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.*;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.dialog.LogOutDialog;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.thread.AwfulThread;

public class ForumsIndexFragment extends AwfulFragment implements AwfulUpdateCallback {
    private static final String TAG = "ForumsIndex";
    private ExpandableListView mForumList;
    private boolean mIsSidebar;

    public static ForumsIndexFragment newInstance() {
        return new ForumsIndexFragment();
    }
    
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message aMsg) {
        	AwfulSyncService.debugLogReceivedMessage(Constants.FORUM_INDEX_ID, aMsg);
            switch (aMsg.what) {
                case AwfulSyncService.MSG_SYNC_INDEX:
            		if(aMsg.arg1 == AwfulSyncService.Status.OKAY){
            			if(getActivity() != null){
            				getLoaderManager().restartLoader(Constants.FORUM_INDEX_ID, null, mForumLoaderCallback);
            			}
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
        mCursorAdapter = new AwfulTreeAdapter(getActivity());
        mForumList.setAdapter(mCursorAdapter);
    }

    @Override
    public void onStart() {
        super.onStart(); Log.e(TAG, "Start");

        boolean loggedIn = NetworkUtils.restoreLoginCookies(getActivity());
        if (loggedIn) {
            Log.v(TAG, "Cookie Loaded!");
        } else {
            startActivityForResult(new Intent().setClass(getActivity(), AwfulLoginActivity.class), 0);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume(); Log.e(TAG, "Resume");
		getActivity().getSupportLoaderManager().restartLoader(Constants.FORUM_INDEX_ID, null, mForumLoaderCallback);
    }

	@Override
	public void onPageVisible() {
		
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

    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.user_cp:
                    displayUserCP();
                    break;
                case R.id.refresh:
                	syncForums();
                    break;
            }
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
    public void loadingFailed() {
    	super.loadingFailed();
        Log.e(TAG, "Loading failed.");
    	if(getActivity() != null){
        	Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
        }
    }
    
    private static final AlphaAnimation mFlashingAnimation = new AlphaAnimation(1f, 0f);
	private static final RotateAnimation mLoadingAnimation = 
			new RotateAnimation(
					0f, 360f,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
	static {
		mFlashingAnimation.setInterpolator(new LinearInterpolator());
		mFlashingAnimation.setRepeatCount(Animation.INFINITE);
		mFlashingAnimation.setDuration(500);
		mLoadingAnimation.setInterpolator(new LinearInterpolator());
		mLoadingAnimation.setRepeatCount(Animation.INFINITE);
		mLoadingAnimation.setDuration(700);
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
			Log.i(TAG,"Load Index Cursor: "+aId);
            return new CursorLoader(getActivity(), AwfulForum.CONTENT_URI, AwfulProvider.ForumProjection, AwfulForum.PARENT_ID+"=?", new String[]{Integer.toString(aId)}, AwfulForum.INDEX);
        }

		@Override
        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"Index cursor: "+aLoader.getId());
        	if(aData.moveToFirst()){
        		if(aLoader.getId() == 0){
        			mCursorAdapter.setGroupCursor(aData);
        			if(aData.getCount() < 3){
        				syncForums();
        			}
        		}else{
        			int groupId = mCursorAdapter.getGroupPosition(aData.getInt(aData.getColumnIndex(AwfulForum.PARENT_ID)));
        			if(groupId >=0){
        				mCursorAdapter.setChildrenCursor(groupId, aData);
        			}
        		}
        	}else{
                syncForums();
        	}
        }

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			Log.e(TAG,"resetLoader: "+arg0.getId());
			if(arg0.getId() == 0){
    			mCursorAdapter.setGroupCursor(null);
    		}else{
    			int groupId = mCursorAdapter.getGroupPosition(arg0.getId());
    			if(groupId >=0){
    				if(mForumList.isGroupExpanded(groupId)){
    					mForumList.collapseGroup(groupId);
    				}
    				mCursorAdapter.setChildrenCursor(groupId, null);
    			}
    		}
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
			if(groupCursor!=null && !groupCursor.isClosed() && groupCursor.moveToFirst()){
				int column = groupCursor.getColumnIndex(AwfulForum.ID);
				do{
					if(groupCursor.getInt(column) == parent){
						return groupCursor.getPosition();
					}
				}while(groupCursor.moveToNext());
			}else{
				Log.e(TAG, "CLOSED CURSOR! "+parent);
			}
			return -1;
		}

		@Override
		protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
			int id = cursor.getInt(cursor.getColumnIndex(AwfulForum.ID));
			ImageView tag = AwfulForum.getSubforumView(view,
								getAwfulActivity(),
							   mPrefs,
							   cursor,
							   mIsSidebar,
							   selectedId > -1 && selectedId == id);
			if(tag != null){
				Object tagStuff = tag.getTag();
				if(tagStuff instanceof String[]){
					String[] tagFile = (String[]) tagStuff;
					Log.e("AImg","Image download: "+tagFile[0]);
					getAwfulActivity().sendMessage(mReplyTo, AwfulSyncService.MSG_GRAB_IMAGE, Constants.FORUM_INDEX_ID, tagFile[1].hashCode(), tagFile[1]);
					tag.setTag(tagFile[0]);
					imageQueue.put(tagFile[1].hashCode(), tag);
				}else if(tagStuff instanceof String){
					AwfulThread.setBitmap(getAwfulActivity(), tag, imageCache);
				}
			}
			getAwfulActivity().setPreferredFont(view);
		}

		@Override
		protected void bindGroupView(View view, Context context, Cursor cursor,
				boolean isExpanded) {
			int id = cursor.getInt(cursor.getColumnIndex(AwfulForum.ID));
			ImageView tag = AwfulForum.getSubforumView(view,
								getAwfulActivity(),
							   mPrefs,
							   cursor,
							   mIsSidebar,
							   selectedId > -1 && selectedId == id);
			if(tag != null){
				Object tagStuff = tag.getTag();
				if(tagStuff instanceof String[]){
					String[] tagFile = (String[]) tagStuff;
					Log.e("AImg","Image download: "+tagFile[0]);
					getAwfulActivity().sendMessage(mReplyTo, AwfulSyncService.MSG_GRAB_IMAGE, Constants.FORUM_INDEX_ID, tagFile[1].hashCode(), tagFile[1]);
					tag.setTag(tagFile[0]);
					imageQueue.put(tagFile[1].hashCode(), tag);
				}else if(tagStuff instanceof String){
					AwfulThread.setBitmap(getAwfulActivity(), tag, imageCache);
				}
			}
			getAwfulActivity().setPreferredFont(view);
		}

		@Override
		protected View newChildView(Context context, Cursor cursor,
				boolean isLastChild, ViewGroup parent) {
			int id = cursor.getInt(cursor.getColumnIndex(AwfulForum.ID));
			View row = inf.inflate(R.layout.thread_item, parent, false);
			ImageView tag = AwfulForum.getSubforumView(row,
								getAwfulActivity(),
							   mPrefs,
							   cursor,
							   mIsSidebar,
							   selectedId > -1 && selectedId == id);
			if(tag != null){
				Object tagStuff = tag.getTag();
				if(tagStuff instanceof String[]){
					String[] tagFile = (String[]) tagStuff;
					Log.e("AImg","Image download: "+tagFile[0]);
					getAwfulActivity().sendMessage(mReplyTo, AwfulSyncService.MSG_GRAB_IMAGE, Constants.FORUM_INDEX_ID, tagFile[1].hashCode(), tagFile[1]);
					tag.setTag(tagFile[0]);
					imageQueue.put(tagFile[1].hashCode(), tag);
				}else if(tagStuff instanceof String){
					AwfulThread.setBitmap(getAwfulActivity(), tag, imageCache);
				}
			}
			getAwfulActivity().setPreferredFont(row);
			return row;
		}

		@Override
		protected View newGroupView(Context context, Cursor cursor,
				boolean isExpanded, ViewGroup parent) {
			int id = cursor.getInt(cursor.getColumnIndex(AwfulForum.ID));
			View row = inf.inflate(R.layout.thread_item, parent, false);
			ImageView tag = AwfulForum.getSubforumView(row, 
								getAwfulActivity(),
							   mPrefs,
							   cursor,
							   mIsSidebar,
							   selectedId > -1 && selectedId == id);
			if(tag != null){
				Object tagStuff = tag.getTag();
				if(tagStuff instanceof String[]){
					String[] tagFile = (String[]) tagStuff;
					Log.e("AImg","Image download: "+tagFile[0]);
					getAwfulActivity().sendMessage(mReplyTo, AwfulSyncService.MSG_GRAB_IMAGE, Constants.FORUM_INDEX_ID, tagFile[1].hashCode(), tagFile[1]);
					tag.setTag(tagFile[0]);
					imageQueue.put(tagFile[1].hashCode(), tag);
				}else if(tagStuff instanceof String){
					AwfulThread.setBitmap(getAwfulActivity(), tag, imageCache);
				}
			}
			getAwfulActivity().setPreferredFont(row);
			return row;
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			int parentId = groupCursor.getInt(groupCursor.getColumnIndex(AwfulForum.ID));
			Log.v(TAG, "getChildrenCursor "+parentId);
			getLoaderManager().restartLoader(parentId, null, mForumLoaderCallback);
			return null;
		}
		private HashMap<Integer,ImageView> imageQueue = new HashMap<Integer,ImageView>();
		private HashMap<String,Bitmap> imageCache = new HashMap<String,Bitmap>();
		private Handler mImageHandler = new Handler(){

			@Override
			public void handleMessage(Message msg) {
				Log.i(TAG,"Image Message: "+msg.arg1);
				if(msg.arg1 == AwfulSyncService.Status.OKAY && msg.what == AwfulSyncService.MSG_GRAB_IMAGE){
					ImageView imgTag = imageQueue.remove(msg.arg2);
					if(imgTag != null){
						AwfulThread.setBitmap(getActivity(), imgTag, imageCache);
					}
				}
			}
			
		};
		private Messenger mReplyTo = new Messenger(mImageHandler);
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
		return getResources().getString(R.string.forums_title);
	}
}
