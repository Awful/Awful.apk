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

import android.content.ContentUris;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.dialog.LogOutDialog;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.provider.AwfulProvider;
import com.ferg.awful.service.AwfulCursorAdapter;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulPagedItem;
import com.ferg.awful.thread.AwfulThread;

public class UserCPFragment extends DialogFragment implements AwfulUpdateCallback {
    private static final String TAG = "UserCPActivity";

    private ImageButton mHome;
    private ImageButton mPrivateMessage;
    private ListView mBookmarkList;
    private TextView mTitle;
    private AwfulPreferences mPrefs;
    private ImageButton mRefresh;
    private ImageButton mRefreshBar;
    private ImageButton mNextPage;
    private ImageButton mPrevPage;
    private TextView mPageCountText;
    
    private int mPage = 1;
    private int mLastPage = 0;
    private int mId = Constants.USERCP_ID;
    
    
    private AwfulCursorAdapter mCursorAdapter;

    public static UserCPFragment newInstance(boolean aModal) {
        UserCPFragment fragment = new UserCPFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putBoolean(Constants.MODAL, aModal);

        fragment.setArguments(args);

        fragment.setShowsDialog(false);
        fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);

        return fragment;
    }
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message aMsg) {
            switch (aMsg.what) {
                case AwfulSyncService.MSG_SYNC_FORUM:
            		if(aMsg.arg1 == AwfulSyncService.Status.OKAY){
                		getActivity().getSupportLoaderManager().restartLoader(mId, null, mForumLoaderCallback);
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

    private Messenger mMessenger = new Messenger(mHandler);
    private ForumContentsCallback mForumLoaderCallback = new ForumContentsCallback(mHandler, Constants.USERCP_ID);
    private ForumDataCallback mForumDataCallback = new ForumDataCallback(mHandler);

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        new Thread(new Runnable() {
            public void run() {
                GoogleAnalyticsTracker.getInstance().trackPageView("/UserCPFragment");
                GoogleAnalyticsTracker.getInstance().dispatch();
            }
        }).start();

        setHasOptionsMenu(true);
    }
        
    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.settings, false);
        mPrefs = new AwfulPreferences(getActivity());
        
        View result = aInflater.inflate(R.layout.user_cp, aContainer, false);

        mBookmarkList = (ListView) result.findViewById(R.id.bookmark_list);

        if (AwfulActivity.useLegacyActionbar()) {
            View actionbar = ((ViewStub) result.findViewById(R.id.actionbar)).inflate();
            mHome          = (ImageButton) actionbar.findViewById(R.id.home);
            mPrivateMessage = (ImageButton) actionbar.findViewById(R.id.pm_button);
            mTitle         = (TextView) actionbar.findViewById(R.id.title);
            mRefresh       = (ImageButton) actionbar.findViewById(R.id.refresh_top);
        } else if (((AwfulActivity) getActivity()).isTablet()) {
            View actionbar = ((ViewStub) result.findViewById(R.id.actionbar_blank)).inflate();
            mTitle         = (TextView) actionbar.findViewById(R.id.title);
        }
		mPageCountText = (TextView) result.findViewById(R.id.page_count);
		mNextPage = (ImageButton) result.findViewById(R.id.next_page);
		mPrevPage = (ImageButton) result.findViewById(R.id.prev_page);
        mRefreshBar  = (ImageButton) result.findViewById(R.id.refresh);
		
		mNextPage.setOnClickListener(onButtonClick);
		mPrevPage.setOnClickListener(onButtonClick);
		mRefreshBar.setOnClickListener(onButtonClick);
		updatePageBar();
        
        return result;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

        setRetainInstance(true);

        if (AwfulActivity.useLegacyActionbar()) {
            mTitle.setText(getString(R.string.user_cp));

            mHome.setOnClickListener(onButtonClick);
            mRefresh.setOnClickListener(onButtonClick);
            mPrivateMessage.setOnClickListener(onButtonClick);
        } else if (((AwfulActivity) getActivity()).isTablet()) {
            mTitle.setText(getString(R.string.user_cp));
        }

        mBookmarkList.setOnItemClickListener(onThreadSelected);
        mBookmarkList.setBackgroundColor(mPrefs.postBackgroundColor);
        mBookmarkList.setCacheColorHint(mPrefs.postBackgroundColor);
        mCursorAdapter = new AwfulCursorAdapter(getActivity(), null);
        mBookmarkList.setAdapter(mCursorAdapter);
        registerForContextMenu(mBookmarkList);
    }

    @Override
    public void onStart() {
        super.onStart();

        ((AwfulActivity) getActivity()).registerSyncService(mMessenger, mId);
        // When coming from the desktop shortcut we won't have login cookies
        boolean loggedIn = NetworkUtils.restoreLoginCookies(getActivity());

		getActivity().getSupportLoaderManager().restartLoader(mId, null, mForumLoaderCallback);
		getActivity().getSupportLoaderManager().restartLoader(-98, null, mForumDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulThread.CONTENT_URI_UCP, true, mForumLoaderCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulForum.CONTENT_URI, true, mForumDataCallback);

        if (!loggedIn) {
            startActivityForResult(new Intent().setClass(getActivity(), AwfulLoginActivity.class), 0);
        }
        syncThreads();
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }
        
    @Override
    public void onStop() {
        super.onStop();
        ((AwfulActivity) getActivity()).unregisterSyncService(mMessenger, mId);
		getActivity().getSupportLoaderManager().destroyLoader(mId);
		getActivity().getSupportLoaderManager().destroyLoader(-98);
        getActivity().getContentResolver().unregisterContentObserver(mForumLoaderCallback);
		getActivity().getContentResolver().unregisterContentObserver(mForumDataCallback);
        
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        if(mBookmarkList != null){
        	mBookmarkList.setAdapter(null);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu aMenu, View aView, ContextMenuInfo aMenuInfo) {
        super.onCreateContextMenu(aMenu, aView, aMenuInfo);

        MenuInflater inflater = getActivity().getMenuInflater();
        
        inflater.inflate(R.menu.thread_longpress, aMenu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) aItem.getMenuInfo();
        switch (aItem.getItemId()) {
            case R.id.first_page:
            	Intent viewThread = new Intent().setClass(getActivity(), ThreadDisplayActivity.class).putExtra(Constants.THREAD, (int)info.id).putExtra(Constants.PAGE, 1);
            	startActivity(viewThread);
                return true;
            case R.id.last_page:
            	Intent viewThread2 = new Intent().setClass(getActivity(), ThreadDisplayActivity.class)
            									 .putExtra(Constants.THREAD, (int)info.id)
            									 .putExtra(Constants.PAGE, AwfulPagedItem.indexToPage(mCursorAdapter.getInt(info.position, AwfulThread.POSTCOUNT), mPrefs.postPerPage));
            	startActivity(viewThread2);
                return true;
            case R.id.thread_bookmark:
            	toggleThreadBookmark((int)info.id,(mCursorAdapter.getInt(info.position, AwfulThread.BOOKMARKED)+1)%2);
                return true;
            case R.id.mark_thread_unread:
            	markUnread((int)info.id);
                return true;
        }

        return false;
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(menu.size() == 0){
            inflater.inflate(R.menu.user_cp_menu, menu);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.pm:
            	startActivity(new Intent().setClass(getActivity(), PrivateMessageActivity.class));
                break;
            case R.id.settings:
                startActivity(new Intent().setClass(getActivity(), SettingsActivity.class));
                return true;
            case R.id.logout:
                new LogOutDialog(getActivity()).show();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private boolean isTablet() {
        if (getActivity() != null) {
            return ((AwfulActivity) getActivity()).isTablet();
        }

        return false;
    }

    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.home:
                    startActivity(new Intent().setClass(getActivity(), ForumsIndexActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    break;
                case R.id.pm_button:
                    startActivity(new Intent().setClass(getActivity(), PrivateMessageActivity.class));
                    break;
                case R.id.refresh_top:
                case R.id.refresh:
                	syncThreads();
                    break;
                case R.id.next_page:
                	goToPage(mPage+1);
                    break;
                case R.id.prev_page:
                	goToPage(mPage-1);
                    break;
            }
        }
    };

    private AdapterView.OnItemClickListener onThreadSelected = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            
            Log.i(TAG, "Thread ID: " + Long.toString(aId));
            Intent viewThread = new Intent().setClass(getActivity(), ThreadDisplayActivity.class);
            viewThread.putExtra(Constants.THREAD_ID, (int)aId);
            int unreadPage = AwfulPagedItem.getLastReadPage(mCursorAdapter.getInt(aPosition, AwfulThread.UNREADCOUNT),
															mCursorAdapter.getInt(aPosition, AwfulThread.POSTCOUNT),
															mPrefs.postPerPage);
            viewThread.putExtra(Constants.PAGE, unreadPage);

            startActivity(viewThread);
        }
    };
    
    @Override
    public void loadingFailed() {
        Log.e(TAG, "Loading failed.");
        if (AwfulActivity.useLegacyActionbar()) {
            mRefresh.setVisibility(View.VISIBLE);
            mRefresh.setAnimation(null);
            mRefresh.setImageResource(android.R.drawable.ic_dialog_alert);
          //TODO mRefresh.startAnimation(adapt.getBlinkingAnimation());
        }
        if(getActivity() != null){
        	Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void loadingStarted() {
        if (AwfulActivity.useLegacyActionbar()) {
            mRefresh.setVisibility(View.VISIBLE);
            mRefresh.setImageResource(R.drawable.ic_menu_refresh);
          //TODO mRefresh.startAnimation(adapt.getRotateAnimation());
        }
    }

    @Override
    public void loadingSucceeded() {
        if (AwfulActivity.useLegacyActionbar()) {
            mRefresh.setAnimation(null);
            mRefresh.setVisibility(View.GONE);
        }
    }

	@Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		if(mBookmarkList != null){
			mBookmarkList.setBackgroundColor(prefs.postBackgroundColor);
			if(mBookmarkList.getChildCount() >4){//shitty workaround for: http://code.google.com/p/android/issues/detail?id=9775
				mBookmarkList.setCacheColorHint(prefs.postBackgroundColor);
	        }
		}
	}

	private void goToPage(int pageInt) {
		if(pageInt > 0 && pageInt <= mLastPage){
			mPage = pageInt;
			updatePageBar();
			syncThreads();
		}
	}
	
	public void updatePageBar(){
		mPageCountText.setText("Page " + mPage + "/" + (mLastPage>0?mLastPage:"?"));
		if (mPage <= 1) {
			mPrevPage.setVisibility(View.INVISIBLE);
		} else {
			mPrevPage.setVisibility(View.VISIBLE);
		}

		if (mPage == mLastPage) {
            mNextPage.setVisibility(View.GONE);
            mRefreshBar.setVisibility(View.VISIBLE);
		} else {
            mNextPage.setVisibility(View.VISIBLE);
            mRefreshBar.setVisibility(View.GONE);
		}
	}
	

    private void syncThreads() {
        ((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_SYNC_FORUM,mId,mPage);
    }
	
	private void markUnread(int id) {
        ((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_MARK_UNREAD,id,0);
    }
	
	/** Set Bookmark status.
	 * @param id Thread ID
	 * @param addRemove 1 to add bookmark, 0 to remove.
	 */
    private void toggleThreadBookmark(int id, int addRemove) {
        ((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_SET_BOOKMARK,id,addRemove);
    }
	
	private class ForumContentsCallback extends ContentObserver implements LoaderManager.LoaderCallbacks<Cursor> {
		private int mId;
        public ForumContentsCallback(Handler handler, int id) {
			super(handler);
			mId = id;
		}

		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
			Log.i(TAG,"Load Cursor.");
			mId = aId;
            return new CursorLoader(getActivity(), 
            						AwfulThread.CONTENT_URI_UCP, 
            						AwfulProvider.ThreadProjection, 
            						AwfulProvider.TABLE_UCP_THREADS+"."+AwfulThread.INDEX+">=? AND "+AwfulProvider.TABLE_UCP_THREADS+"."+AwfulThread.INDEX+"<?", 
            						AwfulProvider.int2StrArray(AwfulPagedItem.pageToIndex(mPage),AwfulPagedItem.pageToIndex(mPage+1)), 
            						AwfulThread.INDEX);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"Thread title finished, populating.");
        	if(aData.moveToFirst()){
        		mCursorAdapter.swapCursor(aData);
        	}
        }
        
        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        	mCursorAdapter.swapCursor(null);
        }
        
        @Override
        public void onChange (boolean selfChange){
        	Log.i(TAG,"Thread Data update: "+mId);
        	if(getActivity() != null){
        		getActivity().getSupportLoaderManager().restartLoader(mId, null, this);
        	}
        }
    }
	

	
	private class ForumDataCallback extends ContentObserver implements LoaderManager.LoaderCallbacks<Cursor> {

        public ForumDataCallback(Handler handler) {
			super(handler);
		}

		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            return new CursorLoader(getActivity(), ContentUris.withAppendedId(AwfulForum.CONTENT_URI, Constants.USERCP_ID), 
            		AwfulProvider.ForumProjection, null, null, null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"Forum title finished, populating: "+aData.getCount());
        	if(aData.moveToFirst()){
        		mLastPage = aData.getInt(aData.getColumnIndex(AwfulForum.PAGE_COUNT));
        		updatePageBar();
        	}
        }
        
        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        }

        @Override
        public void onChange (boolean selfChange){
        	Log.e(TAG,"Thread Data update.");
        	getActivity().getSupportLoaderManager().restartLoader(-98, null, this);
        }
    }
}
