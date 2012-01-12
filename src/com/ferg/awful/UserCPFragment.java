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
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.os.Build;
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
import com.ferg.awful.thread.AwfulDisplayItem;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulPagedItem;
import com.ferg.awful.thread.AwfulThread;
import com.ferg.awful.thread.AwfulDisplayItem.DISPLAY_TYPE;

public class UserCPFragment extends DialogFragment implements AwfulUpdateCallback {
    private static final String TAG = "UserCPActivity";

    private ImageButton mHome;
    private ImageButton mPrivateMessage;
    private ListView mBookmarkList;
    private TextView mTitle;
    private SharedPreferences mPrefs;
    private ImageButton mRefresh;
    
    
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
                    break;
                default:
                    super.handleMessage(aMsg);
            }
        }
    };

    private Messenger mMessenger = new Messenger(mHandler);
    private ForumContentsCallback mForumLoaderCallback = new ForumContentsCallback(mHandler, Constants.USERCP_ID);

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
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        
        View result = aInflater.inflate(R.layout.user_cp, aContainer, false);

        mBookmarkList = (ListView) result.findViewById(R.id.bookmark_list);

        if (AwfulActivity.useLegacyActionbar()) {
            View actionbar = ((ViewStub) result.findViewById(R.id.actionbar)).inflate();
            mHome          = (ImageButton) actionbar.findViewById(R.id.home);
            mPrivateMessage = (ImageButton) actionbar.findViewById(R.id.pm_button);
            mTitle         = (TextView) actionbar.findViewById(R.id.title);
            mRefresh       = (ImageButton) actionbar.findViewById(R.id.refresh);
        } else if (((AwfulActivity) getActivity()).isTablet()) {
            View actionbar = ((ViewStub) result.findViewById(R.id.actionbar_blank)).inflate();
            mTitle         = (TextView) actionbar.findViewById(R.id.title);
        }
        
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
        mBookmarkList.setBackgroundColor(mPrefs.getInt("default_post_background_color", getResources().getColor(R.color.background)));
        mBookmarkList.setCacheColorHint(mPrefs.getInt("default_post_background_color", getResources().getColor(R.color.background)));

      //TODO mBookmarkList.setAdapter(adapt);
        registerForContextMenu(mBookmarkList);
    }

    @Override
    public void onStart() {
        super.onStart();
        
        // When coming from the desktop shortcut we won't have login cookies
        boolean loggedIn = NetworkUtils.restoreLoginCookies(getActivity());
        
        getActivity().getContentResolver().registerContentObserver(AwfulThread.CONTENT_URI, true, mForumLoaderCallback);

        if (!loggedIn) {
            startActivityForResult(new Intent().setClass(getActivity(), AwfulLoginActivity.class), 0);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
      //TODO adapt.refresh();
    }
        
    @Override
    public void onStop() {
        super.onStop();
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
      //TODO AwfulDisplayItem selected = (AwfulDisplayItem) adapt.getItem(((AdapterContextMenuInfo) aMenuInfo).position);
        
        inflater.inflate(R.menu.thread_longpress, aMenu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) aItem.getMenuInfo();
      //TODO AwfulThread thread = (AwfulThread) adapt.getItem(info.position);
    	//TODO if(thread == null || thread.getType() != DISPLAY_TYPE.THREAD){
    	//TODO 	return false;
    	//TODO }
        switch (aItem.getItemId()) {
            case R.id.first_page:
            	//TODO Intent viewThread = new Intent().setClass(getActivity(), ThreadDisplayActivity.class).putExtra(Constants.THREAD, thread.getID()).putExtra(Constants.PAGE, 1);
            	//TODO startActivity(viewThread);
                return true;
            case R.id.last_page:
            	//TODO Intent viewThread2 = new Intent().setClass(getActivity(), ThreadDisplayActivity.class).putExtra(Constants.THREAD, thread.getID()).putExtra(Constants.PAGE, thread.getLastPage());
            	//TODO startActivity(viewThread2);
                return true;
            case R.id.thread_bookmark:
            	//TODO adapt.toggleBookmark(thread.getID());
            	//TODO adapt.refresh();
                return true;
            case R.id.mark_thread_unread:
            	//TODO adapt.markThreadUnread(thread.getID());
            	//TODO adapt.refresh();
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
                case R.id.refresh:
                	//TODO adapt.refresh();
                    break;
            }
        }
    };

    private AdapterView.OnItemClickListener onThreadSelected = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            AwfulThread thread = (AwfulThread) mBookmarkList.getAdapter().getItem(aPosition);

            Log.i(TAG, "Thread ID: " + Integer.toString(thread.getID()));
            Intent viewThread = new Intent().setClass(getActivity(), ThreadDisplayActivity.class);
            viewThread.putExtra(Constants.THREAD_ID, thread.getID());

            startActivity(viewThread);
        }
    };

    @Override
    public void dataUpdate(boolean pageChange, Bundle extras) {
        if(pageChange && this.isAdded() && mBookmarkList!= null && mBookmarkList.getCount() >0){
            mBookmarkList.setSelection(0);
        }
    }

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
	public void onServiceConnected() {
		// TODO Auto-generated method stub
		
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
	
	private class ForumContentsCallback extends ContentObserver implements LoaderManager.LoaderCallbacks<Cursor> {
		private int mId;
        public ForumContentsCallback(Handler handler, int id) {
			super(handler);
			mId = id;
		}

		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
			mId = aId;
            return new CursorLoader(getActivity(), AwfulThread.CONTENT_URI, AwfulProvider.ThreadProjection, AwfulThread.FORUM_ID+"=?", new String[]{Integer.toString(mId)}, null);
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
        	getActivity().getSupportLoaderManager().restartLoader(mId, null, this);
        }
    }
}
