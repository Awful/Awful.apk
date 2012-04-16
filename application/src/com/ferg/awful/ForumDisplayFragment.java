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
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
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

/**
 * Uses intent extras:
 *  TYPE - STRING ID - DESCRIPTION
 *	int - Constants.FORUM_ID - id number for the forum
 *	int - Constants.FORUM_PAGE - page number to load
 *
 *  Can also handle an HTTP intent that refers to an SA forumdisplay.php? url.
 */
public class ForumDisplayFragment extends AwfulFragment implements AwfulUpdateCallback {
    private static final String TAG = "ThreadsActivity";
    
    private ListView mListView;
    private ImageButton mRefreshBar;
    private ImageButton mNextPage;
    private ImageButton mPrevPage;
    private ImageButton mMoveUp;
    private TextView mPageCountText;
    private TextView mSecondaryTitle;
	private ImageButton mToggleSidebar;
    
    private Cursor[] combinedCursors = new Cursor[2];
    
    private int mForumId;
    private int mPage = 1;
    private int mLastPage = 1;
    private String mTitle;
    private boolean skipLoad = false;
    private int mParentForumId = 0;
    
    private static final int buttonSelectedColor = 0x8033b5e5;//0xa0ff7f00;
    
    private long lastRefresh;

    public static ForumDisplayFragment newInstance(int aForum, boolean skipLoad) {
        ForumDisplayFragment fragment = new ForumDisplayFragment();

        Bundle args = new Bundle();
        args.putInt(Constants.FORUM_ID, aForum);
        fragment.setArguments(args);
        
        fragment.skipLoad(skipLoad);//we don't care about persisting this

        return fragment;
    }


	private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message aMsg) {
        	AwfulSyncService.debugLogReceivedMessage(mForumId, aMsg);
            switch (aMsg.what) {
	        	case AwfulSyncService.MSG_GRAB_IMAGE:
	        		if(isResumed() && isVisible()){
	        			mListView.invalidateViews();
	        		}
	        		break;
                case AwfulSyncService.MSG_SYNC_FORUM:
            		if(aMsg.arg1 == AwfulSyncService.Status.OKAY){
            			if(getActivity() != null){
            				getLoaderManager().restartLoader(getLoaderId(), null, mForumLoaderCallback);
            				lastRefresh = System.currentTimeMillis();
            			}
            			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO){
	                    	mRefreshBar.setColorFilter(0);
	                    	mToggleSidebar.setColorFilter(0);
            			}
            			loadingSucceeded();
            		}else if(aMsg.arg1 == AwfulSyncService.Status.ERROR){
            			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO){
	                    	mRefreshBar.setColorFilter(0);
	                    	mToggleSidebar.setColorFilter(0);
            			}
            			loadingFailed();
            		}else if(aMsg.arg1 == AwfulSyncService.Status.WORKING){
            			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO){
	                    	mRefreshBar.setColorFilter(buttonSelectedColor);
	                    	mToggleSidebar.setColorFilter(buttonSelectedColor);
            			}
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
    private ForumContentsCallback mForumLoaderCallback = new ForumContentsCallback(mHandler);
    private SubforumsCallback mSubforumLoaderCallback = new SubforumsCallback();
    private ForumDataCallback mForumDataCallback = new ForumDataCallback(mHandler);


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if(getActivity() instanceof ForumsIndexActivity){
            setHasOptionsMenu(true);
        }
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

        mListView = (ListView) result.findViewById(R.id.forum_list);
        mListView.setDrawingCacheEnabled(true);
        
        mPageCountText = (TextView) result.findViewById(R.id.page_count);
		getAwfulActivity().setPreferredFont(mPageCountText);
		mNextPage = (ImageButton) result.findViewById(R.id.next_page);
		mPrevPage = (ImageButton) result.findViewById(R.id.prev_page);
		mRefreshBar  = (ImageButton) result.findViewById(R.id.refresh);
		mToggleSidebar = (ImageButton) result.findViewById(R.id.toggle_sidebar);
		mToggleSidebar.setOnClickListener(onButtonClick);
        mToggleSidebar.setImageResource(R.drawable.ic_menu_load);
		mNextPage.setOnClickListener(onButtonClick);
		mPrevPage.setOnClickListener(onButtonClick);
		mRefreshBar.setOnClickListener(onButtonClick);
		mPageCountText.setOnClickListener(onButtonClick);
		if(getAwfulActivity() instanceof ThreadDisplayActivity){
			result.findViewById(R.id.secondary_title_bar).setVisibility(View.VISIBLE);
			mSecondaryTitle = (TextView) result.findViewById(R.id.second_titlebar);
			mMoveUp = (ImageButton) result.findViewById(R.id.move_up);
			mMoveUp.setOnClickListener(onButtonClick);
		}
		updatePageBar();
        return result;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

        setRetainInstance(false);

        Bundle args = getArguments();
        if(args != null){
        	mForumId = args.getInt(Constants.FORUM_ID, 0);
        }
        
        //parsing forum id
        if(mForumId <1){//we might already have it from newInstance(id), that value overrides the intent values
        	
        	//if not, try the intent first.
        	mForumId = getActivity().getIntent().getIntExtra(Constants.FORUM_ID, mForumId);
        	mPage = getActivity().getIntent().getIntExtra(Constants.FORUM_PAGE, mPage);
        	
        	//then try to see if we got a url intent (from browser/awful/chrome2phone/ect)
        	Uri data = getActivity().getIntent().getData();
            if (data != null && data.getScheme().equals("http")) {
            	if(data.getLastPathSegment().contains("usercp") || data.getLastPathSegment().contains("bookmarkthreads")){
            		mForumId = Constants.USERCP_ID;
            	}else{
                    String urlForumID = null;
                    urlForumID = getActivity().getIntent().getData().getQueryParameter("forumid");
        	        if (urlForumID != null) {
        	        	mForumId = Integer.parseInt(urlForumID);
        	        }
            	}
                String urlPage = data.getQueryParameter("pagenumber");
                if(urlPage != null){
    	        	mPage = Integer.parseInt(urlPage);
                }
            }
        }
        

        mCursorAdapter = new AwfulCursorAdapter((AwfulActivity) getActivity(), null, getForumId(), getActivity() instanceof ThreadDisplayActivity);
        mListView.setAdapter(mCursorAdapter);
        mListView.setOnItemClickListener(onThreadSelected);
        mListView.setBackgroundColor(mPrefs.postBackgroundColor);
        mListView.setCacheColorHint(mPrefs.postBackgroundColor);
        
        registerForContextMenu(mListView);
    }

	public void updatePageBar(){
		if(mPageCountText != null){
			mPageCountText.setText("Page " + getPage() + "/" + (getLastPage()>0?getLastPage():"?"));
    		mRefreshBar.setVisibility(View.VISIBLE);
            mNextPage.setVisibility(View.VISIBLE);
			mPrevPage.setVisibility(View.VISIBLE);
			mToggleSidebar.setVisibility(View.INVISIBLE);
			if (getPage() <= 1) {
				mPrevPage.setVisibility(View.GONE);
				mToggleSidebar.setVisibility(View.GONE);
			}
			if (getPage() == getLastPage()) {
	            mNextPage.setVisibility(View.GONE);
	            if(getPage() != 1){
		    		mRefreshBar.setVisibility(View.GONE);
					mToggleSidebar.setVisibility(View.VISIBLE);//this is acting as a refresh button
	            }else{
	    			mToggleSidebar.setVisibility(View.INVISIBLE);//if we are at page 1/1, we already have the refresh button on the other side
	            }
			}
			
			if(mMoveUp != null){
				if(mForumId != 0){
					mMoveUp.setVisibility(View.VISIBLE);
				}else{
					mMoveUp.setVisibility(View.INVISIBLE);
				}
			}

			
		}
	}

    @Override
    public void onStart() {
        super.onStart(); Log.e(TAG, "Start");
    }
    
    @Override
    public void onResume() {
        super.onResume(); Log.e(TAG, "Resume");
        getAwfulActivity().registerSyncService(mMessenger, getForumId());
        getActivity().getContentResolver().registerContentObserver(AwfulForum.CONTENT_URI, true, mForumDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulThread.CONTENT_URI, true, mForumLoaderCallback);
		refreshInfo();
        if(skipLoad){
        	skipLoad = false;//only skip the first time
        }else{
        	syncForumsIfStale();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause(); Log.e(TAG, "Pause");
        getAwfulActivity().unregisterSyncService(mMessenger, getForumId());
        getLoaderManager().destroyLoader(getLoaderId());
        getLoaderManager().destroyLoader(Constants.FORUM_LOADER_ID);
        getLoaderManager().destroyLoader(Constants.SUBFORUM_LOADER_ID);
		getActivity().getContentResolver().unregisterContentObserver(mForumLoaderCallback);
		getActivity().getContentResolver().unregisterContentObserver(mForumDataCallback);
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
    public void onDetach() {
        super.onDetach(); Log.e(TAG, "Detach");
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(menu.size() == 0){
        	inflater.inflate(R.menu.forum_display_menu, menu);
        }
    }
    
    @Override
	public void onPrepareOptionsMenu(Menu menu) {
		MenuItem ucp = menu.findItem(R.id.user_cp);
		if(ucp != null){
			if(mForumId == Constants.USERCP_ID){
				ucp.setIcon(R.drawable.ic_menu_home);
				ucp.setTitle(R.string.forums_title);
			}else{
				ucp.setIcon(R.drawable.ic_menu_bookmarks);
				ucp.setTitle(R.string.user_cp);
			}
		}
	}
    
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
	        case R.id.user_cp:
	        	if(getForumId() != Constants.USERCP_ID){
	        		displayForumContents(Constants.USERCP_ID);
	        	}else{
	        		displayForumIndex();
	        	}
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
	        android.view.MenuInflater inflater = getActivity().getMenuInflater();
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
    public boolean onContextItemSelected(android.view.MenuItem aItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) aItem.getMenuInfo();
        switch (aItem.getItemId()) {
            case R.id.first_page:
            	viewThread((int) info.id,1);
                return true;
            case R.id.last_page:
        		int lastPage = AwfulPagedItem.indexToPage(mCursorAdapter.getInt(info.position, AwfulThread.POSTCOUNT), mPrefs.postPerPage);
            	viewThread((int) info.id,lastPage);
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
    
    private void viewThread(int id, int page){
    	displayThread(id, page, getForumId(), getPage());
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
                case R.id.move_up:
                	displayForumContents(mParentForumId);
                    break;
                case R.id.toggle_sidebar://this switches between being a refresh button and being hidden depending on page number.
                case R.id.refresh:
                	syncForum();
                    break;
                case R.id.next_page:
                	goToPage(getPage()+1);
                    break;
                case R.id.prev_page:
                	goToPage(getPage()-1);
                    break;
                case R.id.page_count:
                	displayPagePicker();
                	break;
            }
        }
    };


    private AdapterView.OnItemClickListener onThreadSelected = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            switch(mCursorAdapter.getType(aPosition)) {
                case R.layout.thread_item:
                    Log.i(TAG, "Thread ID: " + Long.toString(aId));
                    int unreadPage = AwfulPagedItem.getLastReadPage(mCursorAdapter.getInt(aPosition, AwfulThread.UNREADCOUNT),
                    												mCursorAdapter.getInt(aPosition, AwfulThread.POSTCOUNT),
                    												mPrefs.postPerPage);
                    viewThread((int) aId, unreadPage);
                    break;
                    
                case R.layout.forum_item:
                    displayForumContents((int) aId);
                    break;
            }
        }
    };
    
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
	public void onPreferenceChange(AwfulPreferences prefs) {
		super.onPreferenceChange(mPrefs);
		getAwfulActivity().setPreferredFont(mPageCountText);
		if(mListView!=null){
			mListView.setBackgroundColor(prefs.postBackgroundColor);
			mListView.setCacheColorHint(prefs.postBackgroundColor);
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
			refreshInfo();
			syncForum();
		}
	}
	
    private void setForumId(int aForum) {
		mForumId = aForum;
	}
    
    public void openForum(int id, int page){
    	if(getActivity() != null){
	        getAwfulActivity().unregisterSyncService(mMessenger, getForumId());
	    	getLoaderManager().destroyLoader(getLoaderId());
    	}
    	setForumId(id);//if the fragment isn't attached yet, just set the values and let the lifecycle handle it
    	mPage = page;
    	mLastPage = 0;
    	lastRefresh = 0;
    	if(getActivity() != null){
	    	mCursorAdapter = new AwfulCursorAdapter((AwfulActivity) getActivity(), null, getForumId(), getActivity() instanceof ThreadDisplayActivity);
	    	if(mListView != null){//if listview doesn't exist yet, we don't need to set the adapter, it'll happen during the lifecycle.
	    		mListView.setAdapter(mCursorAdapter);
	    	}
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
				getActivity().invalidateOptionsMenu();
			}
	        getAwfulActivity().registerSyncService(mMessenger, getForumId());
			getLoaderManager().restartLoader(getLoaderId(), null, mForumLoaderCallback);
			refreshInfo();
			syncForum();
    	}
    }

	public void syncForum() {
		if(getAwfulActivity() != null && getForumId() > 0){
			getAwfulActivity().sendMessage(AwfulSyncService.MSG_SYNC_FORUM, getForumId(), getPage());
		}
    }
	
	public void syncForumsIfStale() {
		if(lastRefresh < System.currentTimeMillis()-(1000*60*5)){
			syncForum();
		}
	}
	
	private void markUnread(int id) {
        getAwfulActivity().sendMessage(AwfulSyncService.MSG_MARK_UNREAD,id,0);
    }
	
	public boolean isBookmark(){
		return getForumId()==Constants.USERCP_ID;
	}
	
	/** Set Bookmark status.
	 * @param id Thread ID
	 * @param addRemove 1 to add bookmark, 0 to remove.
	 */
    private void toggleThreadBookmark(int id, int addRemove) {
        getAwfulActivity().sendMessage(AwfulSyncService.MSG_SET_BOOKMARK,id,addRemove);
    }
	
	private class ForumContentsCallback extends ContentObserver implements LoaderManager.LoaderCallbacks<Cursor> {

		public ForumContentsCallback(Handler handler) {
			super(handler);
		}

		@Override
		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
        	Log.v(TAG,"Creating forum cursor: "+getForumId());
        	if(isBookmark()){
	        	return new CursorLoader(getActivity(), 
						AwfulThread.CONTENT_URI_UCP, 
						AwfulProvider.ThreadProjection, 
						AwfulProvider.TABLE_UCP_THREADS+"."+AwfulThread.INDEX+">=? AND "+AwfulProvider.TABLE_UCP_THREADS+"."+AwfulThread.INDEX+"<?", 
						AwfulProvider.int2StrArray(AwfulPagedItem.pageToIndex(getPage()),AwfulPagedItem.pageToIndex(getPage()+1)), 
						(mPrefs.newThreadsFirst? AwfulThread.UNREADCOUNT+" DESC" :AwfulThread.INDEX));
        	}else{
	            return new CursorLoader(getActivity(), 
	            						AwfulThread.CONTENT_URI, 
	            						AwfulProvider.ThreadProjection, 
	            						AwfulThread.FORUM_ID+"=? AND "+AwfulThread.INDEX+">=? AND "+AwfulThread.INDEX+"<?", 
	            						AwfulProvider.int2StrArray(getForumId(),AwfulPagedItem.pageToIndex(getPage()),AwfulPagedItem.pageToIndex(getPage()+1)),
	            						AwfulThread.INDEX);
        	}
        }

		@Override
        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"Forum contents finished, populating: "+aData.getCount());
        	//mCursorAdapter.swapCursor(aData);
        	combinedCursors[1] = aData;
			if(mCursorAdapter != null){
	        	if(combinedCursors[0]!=null && combinedCursors[1]!=null){
					mCursorAdapter.swapCursor(new MergeCursor(combinedCursors));
				}else{
					mCursorAdapter.swapCursor(aData);
				}
			}
        }

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			combinedCursors[1]=null;
			if(combinedCursors[0]!=null){
				mCursorAdapter.swapCursor(combinedCursors[0]);
			}else{
				mCursorAdapter.swapCursor(null);
			}
		}
		
        @Override
        public void onChange (boolean selfChange){
        	Log.i(TAG,"Thread List update.");
        	refreshInfo();
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
			if(mCursorAdapter != null){
				if(combinedCursors[0]!=null && combinedCursors[1]!=null){//only load a new cursor if one still exists.
					mCursorAdapter.swapCursor(new MergeCursor(combinedCursors));
				}else{
					mCursorAdapter.swapCursor(combinedCursors[0]);
				}
			}
        }

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			combinedCursors[0]=null;
			if(combinedCursors[1]!=null){//only load a new cursor if one still exists.
				mCursorAdapter.swapCursor(combinedCursors[1]);
			}else{
				mCursorAdapter.swapCursor(null);
			}
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
                mTitle = aData.getString(aData.getColumnIndex(AwfulForum.TITLE));
                mParentForumId = aData.getInt(aData.getColumnIndex(AwfulForum.PARENT_ID));
            	if(getActivity() != null){
            		getAwfulActivity().setActionbarTitle(mTitle, ForumDisplayFragment.this);
            	}
            	if(mSecondaryTitle != null){
            		mSecondaryTitle.setText(Html.fromHtml(mTitle));
            	}
        		mLastPage = aData.getInt(aData.getColumnIndex(AwfulForum.PAGE_COUNT));
        	}

			if(mForumId == 0 && mSecondaryTitle != null){
				mSecondaryTitle.setText(R.string.forums_title);
			}
			updatePageBar();
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
			getLoaderManager().restartLoader(getLoaderId(), null, mForumLoaderCallback);
	    	getLoaderManager().restartLoader(Constants.FORUM_LOADER_ID, null, mForumDataCallback);
	    	getLoaderManager().restartLoader(Constants.SUBFORUM_LOADER_ID, null, mSubforumLoaderCallback);
		}
	}
	
	public String getTitle(){
		return mTitle;
	}
    
	public void skipLoad(boolean skip) {
		skipLoad = skip;
	}

}
