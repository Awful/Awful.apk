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

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.dialog.LogOutDialog;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.provider.AwfulProvider;
import com.ferg.awful.service.AwfulCursorAdapter;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulPagedItem;
import com.ferg.awful.thread.AwfulThread;
import com.ferg.awful.widget.NumberPicker;

public class ForumDisplayFragment extends ListFragment implements AwfulUpdateCallback {
    private static final String TAG = "ThreadsActivity";
    
    private ImageButton mUserCp;
    private TextView mTitle;
    private ImageButton mRefresh;
    private ImageButton mRefreshBar;
    private ImageButton mNextPage;
    private ImageButton mPrevPage;
    private TextView mPageCountText;

    private AwfulPreferences mPrefs;
    
    private Cursor[] combinedCursors = new Cursor[2];
    
    private int mForumId;
    private int mPage = 1;
    private int mLastPage = 1;

    public static ForumDisplayFragment newInstance(int aForum) {
        ForumDisplayFragment fragment = new ForumDisplayFragment();

        fragment.setForumId(aForum);

        return fragment;
    }
    

	private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message aMsg) {
        	AwfulSyncService.debugLogReceivedMessage(mForumId, aMsg);
            switch (aMsg.what) {
	        	case AwfulSyncService.MSG_GRAB_IMAGE:
	        		if(isResumed() && isVisible()){
	        			getListView().invalidateViews();
	        		}
	        		break;
                case AwfulSyncService.MSG_SYNC_FORUM:
            		if(aMsg.arg1 == AwfulSyncService.Status.OKAY){
            			if(getActivity() != null){
            				getLoaderManager().restartLoader(getLoaderId(), null, mForumLoaderCallback);
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
    private AwfulCursorAdapter mCursorAdapter;
    private Messenger mMessenger = new Messenger(mHandler);
    private ForumContentsCallback mForumLoaderCallback = new ForumContentsCallback();
    private SubforumsCallback mSubforumLoaderCallback = new SubforumsCallback();
    private ForumDataCallback mForumDataCallback = new ForumDataCallback(mHandler);

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    protected int getLoaderId() {
    	//loader ID conflicts suck.
    	//this is terrible and I know it.
		return getForumId()+1000;
	}
	@Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        View result = aInflater.inflate(R.layout.forum_display, aContainer, false);

        if (AwfulActivity.useLegacyActionbar()) {
            View actionbar = ((ViewStub) result.findViewById(R.id.actionbar)).inflate();
            mTitle         = (TextView) actionbar.findViewById(R.id.title);
            mUserCp        = (ImageButton) actionbar.findViewById(R.id.user_cp);
            mRefresh       = (ImageButton) actionbar.findViewById(R.id.refresh_top);

            mTitle.setMovementMethod(new ScrollingMovementMethod());
        }

		mPageCountText = (TextView) result.findViewById(R.id.page_count);
		mNextPage = (ImageButton) result.findViewById(R.id.next_page);
		mPrevPage = (ImageButton) result.findViewById(R.id.prev_page);
        mRefreshBar  = (ImageButton) result.findViewById(R.id.refresh);
		if(mPrevPage != null){
			mNextPage.setOnClickListener(onButtonClick);
			mPrevPage.setOnClickListener(onButtonClick);
			mRefreshBar.setOnClickListener(onButtonClick);
			updatePageBar();
		}
		
        mPrefs = new AwfulPreferences(getActivity());
        return result;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

        setRetainInstance(true);
        
        String c2pForumID = null;
        // we're receiving an intent from an outside link (say, ChromeToPhone). Let's check to see
        // if we have a URL from such a link.
        if (getActivity().getIntent().getData() != null && getActivity().getIntent().getData().getScheme().equals("http")) {
            c2pForumID = getActivity().getIntent().getData().getQueryParameter("forumid");
        }
        
        mForumId = getActivity().getIntent().getIntExtra(Constants.FORUM, mForumId);
        
        if (c2pForumID != null) {
        	mForumId = Integer.parseInt(c2pForumID);
        }

        mCursorAdapter = new AwfulCursorAdapter((AwfulActivity) getActivity(), null, getForumId());
        setListAdapter(mCursorAdapter);
        getListView().setOnItemClickListener(onThreadSelected);
        getListView().setBackgroundColor(mPrefs.postBackgroundColor);
        getListView().setCacheColorHint(mPrefs.postBackgroundColor);

        if (AwfulActivity.useLegacyActionbar()) {
        
            mUserCp.setOnClickListener(onButtonClick);
            mRefresh.setOnClickListener(onButtonClick);
        }
        registerForContextMenu(getListView());
        ((AwfulActivity) getActivity()).registerSyncService(mMessenger, getForumId());
    }

	public void updatePageBar(){
		if(mPageCountText != null){
			mPageCountText.setText("Page " + getPage() + "/" + (getLastPage()>0?getLastPage():"?"));
			if (getPage() <= 1) {
				mPrevPage.setVisibility(View.INVISIBLE);
			} else {
				mPrevPage.setVisibility(View.VISIBLE);
			}
	
			if (getPage() == getLastPage()) {
	            mNextPage.setVisibility(View.GONE);
	            mRefreshBar.setVisibility(View.VISIBLE);
			} else {
	            mNextPage.setVisibility(View.VISIBLE);
	            mRefreshBar.setVisibility(View.GONE);
			}
		}
	}

    private boolean isTablet() {
        if (getActivity() != null) {
            return ((AwfulActivity) getActivity()).isTablet();
        }

        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
		getLoaderManager().restartLoader(getLoaderId(), null, mForumLoaderCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulForum.CONTENT_URI, true, mForumDataCallback);
		refreshInfo();
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
    @Override
    public void onResume() {
        super.onResume();
        syncForum();
    }    
    @Override
    public void onStop() {
        super.onStop();
        getLoaderManager().destroyLoader(getLoaderId());
        getLoaderManager().destroyLoader(Constants.FORUM_LOADER_ID);
        getLoaderManager().destroyLoader(Constants.SUBFORUM_LOADER_ID);
		getActivity().getContentResolver().unregisterContentObserver(mForumDataCallback);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((AwfulActivity) getActivity()).unregisterSyncService(mMessenger, getForumId());
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(menu.size() == 0){
            inflater.inflate(R.menu.forum_display_menu, menu);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
	        case R.id.user_cp:
	            startActivity(new Intent().setClass(getActivity(), UserCPActivity.class));
	            return true;
            case R.id.settings:
                startActivity(new Intent().setClass(getActivity(), SettingsActivity.class));
                return true;
            case R.id.logout:
                new LogOutDialog(getActivity()).show();
                return true;
            case R.id.refresh:
                syncForum();
                return true;
            case R.id.go_to:
                displayPagePicker();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu aMenu, View aView, ContextMenuInfo aMenuInfo) {
        super.onCreateContextMenu(aMenu, aView, aMenuInfo);
        if(aMenuInfo instanceof AdapterContextMenuInfo){
	        MenuInflater inflater = getActivity().getMenuInflater();
	        AdapterContextMenuInfo info = (AdapterContextMenuInfo) aMenuInfo;
	        switch(mCursorAdapter.getType(info.position)){
	           case R.layout.forum_item:
	           	break;
	           case R.layout.thread_item:
	              inflater.inflate(R.menu.thread_longpress, aMenu);
	          	break;
	       }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) aItem.getMenuInfo();
        switch (aItem.getItemId()) {
            case R.id.first_page:
            	Intent viewThread = new Intent().setClass(getActivity(), ThreadDisplayActivity.class).putExtra(Constants.THREAD_ID, info.id).putExtra(Constants.PAGE, 1);
            	startActivity(viewThread);
                return true;
            case R.id.last_page:
        		int lastPage = AwfulPagedItem.indexToPage(mCursorAdapter.getInt(info.position, AwfulThread.POSTCOUNT), mPrefs.postPerPage);
        		Intent viewThread2 = new Intent().setClass(getActivity(), ThreadDisplayActivity.class)
        										 .putExtra(Constants.THREAD_ID, info.id)
        										 .putExtra(Constants.PAGE, lastPage);
            	startActivity(viewThread2);
                return true;
            case R.id.mark_thread_unread:
            	markUnread((int) info.id);
                return true;
            case R.id.thread_bookmark:
            	toggleThreadBookmark((int)info.id, (mCursorAdapter.getInt(info.position, AwfulThread.BOOKMARKED)+1)%2);
                return true;
            case R.id.copy_url_thread:
            	copyUrl((int) info.id);
                return true;
        }

        return false;
    }

    private void copyUrl(int id) {
		StringBuffer url = new StringBuffer();
		url.append(Constants.FUNCTION_THREAD);
		url.append("?");
		url.append(Constants.PARAM_THREAD_ID);
		url.append("=");
		url.append(id);
		
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ClipboardManager clipboard = (ClipboardManager) this.getActivity().getSystemService(
					Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText(String.format("Thread #%d", id), url.toString());
			clipboard.setPrimaryClip(clip);

			Toast successToast = Toast.makeText(this.getActivity().getApplicationContext(),
					getString(R.string.copy_url_success), Toast.LENGTH_SHORT);
			successToast.show();
		} else {
			AlertDialog.Builder alert = new AlertDialog.Builder(this.getActivity());

			alert.setTitle("URL");

			final EditText input = new EditText(this.getActivity());
			input.setText(url.toString());
			alert.setView(input);

			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.dismiss();
				}
			});

			alert.show();
		}
	}

	private void displayPagePicker() {
        final NumberPicker jumpToText = new NumberPicker(getActivity());
        jumpToText.setRange(1, getLastPage());
        jumpToText.setCurrent(getPage());
        new AlertDialog.Builder(getActivity())
            .setTitle("Jump to Page")
            .setView(jumpToText)
            .setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface aDialog, int aWhich) {
                        try {
                            int pageInt = jumpToText.getCurrent();
                            if (pageInt > 0 && pageInt <= getLastPage()) {
                                goToPage(pageInt);
                            }
                        } catch (NumberFormatException e) {
                            Log.d(TAG, "Not a valid number: " + e.toString());
                            Toast.makeText(getActivity(),
                                R.string.invalid_page, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.d(TAG, e.toString());
                        }
                    }
                })
            .setNegativeButton("Cancel", null)
            .show();
    }



	private View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.user_cp:
                    startActivity(new Intent().setClass(getActivity(), UserCPActivity.class));
                    break;
                case R.id.refresh_top:
                case R.id.refresh:
                	syncForum();
                    break;
                case R.id.next_page:
                	goToPage(getPage()+1);
                    break;
                case R.id.prev_page:
                	goToPage(getPage()-1);
                    break;
            }
        }
    };


    private AdapterView.OnItemClickListener onThreadSelected = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            switch(mCursorAdapter.getType(aPosition)) {
                case R.layout.thread_item:
                    Intent viewThread = new Intent().setClass(getActivity(), ThreadDisplayActivity.class);
                    Log.i(TAG, "Thread ID: " + Long.toString(aId));
                    viewThread.putExtra(Constants.THREAD_ID, (int) aId);
                    int unreadPage = AwfulPagedItem.getLastReadPage(mCursorAdapter.getInt(aPosition, AwfulThread.UNREADCOUNT),
                    												mCursorAdapter.getInt(aPosition, AwfulThread.POSTCOUNT),
                    												mPrefs.postPerPage);
                    viewThread.putExtra(Constants.PAGE, unreadPage);
                    startActivity(viewThread);
                    break;
                    
                case R.layout.forum_item:
                    displayForumContents((int) aId);
                    break;
            }
        }
    };

    private void displayForumContents(int aId) {

    	if(getActivity() != null){
    		if (getActivity() instanceof ForumsTabletActivity) {
	        	((ForumsTabletActivity) getActivity()).setContentPane(aId);
	        } else {
	            Intent viewForum = new Intent().setClass(getActivity(), ForumDisplayActivity.class);
	            viewForum.putExtra(Constants.FORUM, aId);
	            startActivity(viewForum);
	        }
    	}
    }

    
    @Override
    public void loadingFailed() {
        Log.e(TAG, "Loading failed.");
        if (AwfulActivity.useLegacyActionbar()) {
            mRefresh.setVisibility(View.VISIBLE);
            mRefresh.setAnimation(null);
            mRefresh.setImageResource(android.R.drawable.ic_dialog_alert);
            mRefresh.startAnimation(mFlashingAnimation);
        } else {
        	if(getActivity() != null){
        		getActivity().setProgressBarIndeterminateVisibility(false);
        	}
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
            mRefresh.startAnimation(mLoadingAnimation);
        } else {
        	if(getActivity() != null){
        		getActivity().setProgressBarIndeterminateVisibility(true);
        	}
        }
    }

    @Override
    public void loadingSucceeded() {
        if (isAdded()) {
            if (AwfulActivity.useLegacyActionbar()) {
                mRefresh.setAnimation(null);
                mRefresh.setVisibility(View.GONE);
            } else {
            	if(getActivity() != null){
            		getActivity().setProgressBarIndeterminateVisibility(false);
            	}
            }
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
	public void onPreferenceChange(AwfulPreferences prefs) {
		if(getListView()!=null){
	        getListView().setBackgroundColor(prefs.postBackgroundColor);
	        if(getListView().getChildCount() >4){//shitty workaround for: http://code.google.com/p/android/issues/detail?id=9775
	        	getListView().setCacheColorHint(prefs.postBackgroundColor);
	        }
		}
	}
	
	public int getForumId(){
		return mForumId;
	}
	
	public int getPage(){
		return mPage;
	}

    private int getLastPage() {
		return mLastPage;
	}
    

	private void goToPage(int pageInt) {
		if(pageInt > 0 && pageInt <= mLastPage){
			mPage = pageInt;
			updatePageBar();
			syncForum();
		}
	}
	
    private void setForumId(int aForum) {
		mForumId = aForum;
	}

	private void syncForum() {
        ((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_SYNC_FORUM, getForumId(), getPage());
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
	
	private class ForumContentsCallback implements LoaderManager.LoaderCallbacks<Cursor> {

		@Override
		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
        	Log.v(TAG,"Creating forum cursor: "+getForumId());
            return new CursorLoader(getActivity(), 
            						AwfulThread.CONTENT_URI, 
            						AwfulProvider.ThreadProjection, 
            						AwfulThread.FORUM_ID+"=? AND "+AwfulThread.INDEX+">=? AND "+AwfulThread.INDEX+"<?", 
            						AwfulProvider.int2StrArray(getForumId(),AwfulPagedItem.pageToIndex(getPage()),AwfulPagedItem.pageToIndex(getPage()+1)),
            						AwfulThread.INDEX);
        }

		@Override
        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"Forum contents finished, populating: "+aData.getCount());
        	//mCursorAdapter.swapCursor(aData);
        	combinedCursors[1] = aData;
        	if(combinedCursors[0]!=null && combinedCursors[1]!=null){
	        	MergeCursor mc = new MergeCursor(combinedCursors);
	        	mCursorAdapter.swapCursor(mc);
        	}
        }

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			mCursorAdapter.swapCursor(null);
			combinedCursors[1]=null;
		}
    }
	
	private class SubforumsCallback implements LoaderManager.LoaderCallbacks<Cursor> {

		@Override
		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
        	Log.v(TAG,"Creating subforum cursor: "+aId);
            return new CursorLoader(getActivity(), 
            						AwfulForum.CONTENT_URI, 
            						AwfulProvider.ForumProjection, 
            						AwfulForum.PARENT_ID+"=?", 
            						AwfulProvider.int2StrArray(getForumId()),
            						AwfulForum.INDEX);
        }

		@Override
        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"Forum contents finished, populating: "+aData.getCount());
        	combinedCursors[0] = aData;
        	if(combinedCursors[0]!=null && combinedCursors[1]!=null){
	        	MergeCursor mc = new MergeCursor(combinedCursors);
				if(mCursorAdapter != null){
					mCursorAdapter.swapCursor(mc);
				}
        	}
        }

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			if(mCursorAdapter != null){
					mCursorAdapter.swapCursor(null);
			}
			combinedCursors[0]=null;
		}
    }
	
	private class ForumDataCallback extends ContentObserver implements LoaderManager.LoaderCallbacks<Cursor> {

        public ForumDataCallback(Handler handler) {
			super(handler);
		}

		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            return new CursorLoader(getActivity(), ContentUris.withAppendedId(AwfulForum.CONTENT_URI, getForumId()), 
            		AwfulProvider.ForumProjection, null, null, null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"Forum title finished, populating: "+aData.getCount());
        	if(!aData.isClosed() && aData.moveToFirst()){
                String title = aData.getString(aData.getColumnIndex(AwfulForum.TITLE));

                if (AwfulActivity.useLegacyActionbar()) {
                	if(mTitle != null){
                		mTitle.setText(Html.fromHtml(title));
                	}
                } else {
                	if(getActivity() != null){
                		((AwfulActivity) getActivity()).setActionbarTitle(title);
                	}
                }

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
        	refreshInfo();
        }
    }
	
	private void refreshInfo(){
		if(getActivity() != null){
	    	getLoaderManager().restartLoader(Constants.FORUM_LOADER_ID, null, mForumDataCallback);
	    	getLoaderManager().restartLoader(Constants.SUBFORUM_LOADER_ID, null, mSubforumLoaderCallback);
		}
	}

}
