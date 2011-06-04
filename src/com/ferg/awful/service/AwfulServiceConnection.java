package com.ferg.awful.service;


import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.DataSetObserver;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import com.ferg.awful.AwfulUpdateCallback;
import com.ferg.awful.R;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.thread.AwfulDisplayItem.DISPLAY_TYPE;
import com.ferg.awful.thread.AwfulPagedItem;
import com.ferg.awful.thread.AwfulThread;

public class AwfulServiceConnection extends BroadcastReceiver implements
		ServiceConnection {

	private static final String TAG = "AwfulServiceAdapter";
	private AwfulService mService;
	private boolean boundState;
	private LayoutInflater inf;
	private ArrayList<AwfulListAdapter> fragments;

	public AwfulServiceConnection(){
		fragments = new ArrayList<AwfulListAdapter>();
	}

	@Override
	public void onServiceConnected(ComponentName cName, IBinder bind) {
		if(bind != null && bind instanceof AwfulService.AwfulBinder){
			boundState = true;
			Log.e(TAG, "service connected!");
			mService = ((AwfulService.AwfulBinder) bind).getService();
			for(AwfulListAdapter la : fragments){
				la.connected();
			}
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName arg0) {
		boundState = false;
		mService = null;
		Log.e(TAG, "service disconnected!");
		for(AwfulListAdapter la : fragments){
			la.disconnected();
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if(boundState && intent.getAction().equalsIgnoreCase(Constants.DATA_UPDATE_BROADCAST) && intent.hasExtra(Constants.DATA_UPDATE_URL)){
			int id = intent.getIntExtra(Constants.DATA_UPDATE_URL, -1);
			Log.e(TAG, "Broadcast Received: id "+id);
			for(AwfulListAdapter la : fragments){
				if(la.currentId == id){
					la.dataUpdate();
					Log.e(TAG, "Broadcast ack: id "+la.currentId);
				}
			}
		}
	}
	public void connect(Context parent){
		if(mService == null && !boundState){
			Log.e(TAG, "connect()");
			parent.bindService(new Intent(parent, AwfulService.class), this, Context.BIND_AUTO_CREATE);
			parent.registerReceiver(this, new IntentFilter(Constants.DATA_UPDATE_BROADCAST));
			inf = LayoutInflater.from(parent);
		}
	}
	public void disconnect(Context parent){
		if(mService != null && boundState){
			Log.e(TAG, "disconnect()");
			parent.unbindService(this);
			parent.unregisterReceiver(this);
			boundState = false;
			mService = null;
		}
	}
	public void fetchThread(int id, int page){
		if(boundState){
			mService.fetchThread(id, page);
		}
	}
	public void fetchForum(int id, int page){
		if(boundState){
			mService.fetchForum(id, page);
		}
	}



	public class AwfulListAdapter extends BaseAdapter implements SectionIndexer {
		private int currentId;
		private int currentPage;
		private DISPLAY_TYPE currentType;
		private AwfulPagedItem state;
		private DataSetObserver mObserver;
		private AwfulUpdateCallback mCallback;
		private boolean lastReadLoaded;
		public AwfulListAdapter(DISPLAY_TYPE viewType, int id, AwfulUpdateCallback frag){
			currentId = id;
			currentPage = 1;
			currentType = viewType;
			mCallback = frag;
			if(boundState){
				loadPage(true);
			}
		}
		public void connected() {
			//this exists to allow graceful caching and reconnection.
			Log.e(TAG, "connected()? "+currentId);
			loadPage(true);
		}
		public void disconnected() {
			if(mObserver != null){
				mObserver.onInvalidated();
			}
		}
		public void dataUpdate() {
			loadPage(false);
			if(mObserver != null){
				mObserver.onChanged();
			}
			mCallback.dataUpdate();
		}
		@Override
		public int getCount() {
			if(state == null){
				return 1;
			}
			Log.e(TAG, "Count: "+state.getChildrenCount(currentPage));
			return state.getChildrenCount(currentPage);
		}

		@Override
		public Object getItem(int ix) {
			if(state == null){
				return null;
			}
			return state.getChild(currentPage, ix);
		}

		@Override
		public long getItemId(int ix) {
			if(state == null){
				return 0;
			}
			return state.getChild(currentPage, ix).getID();
		}

		@Override
		public int getItemViewType(int ix) {
			if(state == null){
				return 0;
			}
			switch(state.getChild(currentPage, ix).getType()){
			case FORUM:
				return 0;
			case THREAD:
				return 1;
			case POST:
				return 2;
			}
			return 0;
		}
		public DISPLAY_TYPE getItemType(int ix){
			return state.getChild(currentPage, ix).getType();
		}

		@Override
		public View getView(int ix, View current, ViewGroup parent) {
			if(state == null){
				return inf.inflate(R.layout.loading, parent, false);
			}
			return state.getChild(currentPage, ix).getView(inf, current, parent);
		}

		@Override
		public int getViewTypeCount() {
			return 3;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isEmpty() {
			//this might seem contradictory, but we want to display a "Loading" message if the page hasn't loaded yet.
			//this may be too much of a hack, so I might revisit the idea.
			return (state != null && state.getChildrenCount(currentPage) == 0);
		}

		@Override
		public void registerDataSetObserver(DataSetObserver ob) {
			if(mObserver != null){
				//is there any case where you would have more than one DataSetObserver? A: nope
				Log.e(TAG, "dataSetObserver overidden!");
			}
			Log.e(TAG, "dataSetObserver set!");
			mObserver = ob;
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			mObserver = null;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int ix) {
			if(state == null){
				return false;
			}
			return state.getChild(currentPage,ix).isEnabled();
		}
		public String getTitle() {
			if(state == null){
				return "Loading...";
			}
			return state.getTitle();
		}

		public void refresh() {
			loadPage(true);
		}

		public void goToPage(int pageInt) {
			if(pageInt < 1){
				pageInt = 1;
			}
			if(currentPage < pageInt && state != null && state instanceof AwfulThread){
				AwfulThread tmp = (AwfulThread) state;
				if(pageInt >= (tmp.getTotalCount()/Constants.ITEMS_PER_PAGE+1)){
					tmp.setUnreadCount(0);
				}else{
					tmp.setUnreadCount(tmp.getTotalCount()-(pageInt-1)*Constants.ITEMS_PER_PAGE);
				}
			}
			lastReadLoaded = true;
			currentPage = pageInt;
			loadPage(true);
		}
		private void loadPage(boolean refresh){
			if(!boundState || mService == null){
				return;
			}
			switch(currentType){
			case FORUM:
				state = mService.getForum(currentId);
				if(refresh){
					fetchForum(currentId, currentPage);
				}
				break;
			case THREAD:
				state = mService.getThread(currentId);
				if(state !=null && !lastReadLoaded){
					Log.e(TAG,"loading lastread id: "+currentId +" page: "+state.getLastReadPage());
					currentPage = state.getLastReadPage();
					lastReadLoaded = true;
				}
				if(refresh){
					fetchThread(currentId, currentPage);
				}
			}
			if(mObserver != null){
				mObserver.onChanged();
			}
		}

		public int getPage() {
			return currentPage;
		}

		public int getLastPage() {
			if(state != null){
				return state.getLastPage();
			}
			return 1;
		}

		public AwfulPagedItem getState() {
			return state;
		}
		public int getLastReadPost() {
			if(state == null || !(state instanceof AwfulThread)){
				return -1;
			}
			return ((AwfulThread) state).getLastReadPost();
		}

		// Section Indexer methods

		@Override
		public int getPositionForSection(int section) {
			return section;
		}
		@Override
		public int getSectionForPosition(int position) {
			return position;
		}
		@Override
		public Object[] getSections() {
			int count = getCount();
			String[] ret = new String[count];
			for(int i=0;i<count;i++) {
				ret[i] = Integer.toString(i+1);
			}
			return ret;
		}
	}



	public AwfulListAdapter createAdapter(DISPLAY_TYPE type, int id, AwfulUpdateCallback forumDisplayFragment) {
		AwfulListAdapter ad =  new AwfulListAdapter(type, id, forumDisplayFragment);
		fragments.add(ad);
		return ad;
	}
}
