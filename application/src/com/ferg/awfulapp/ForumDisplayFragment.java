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
import android.app.AlertDialog;
import android.content.*;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.dialog.LogOutDialog;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.service.AwfulCursorAdapter;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.service.ThreadCursorAdapter;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.thread.AwfulPagedItem;
import com.ferg.awfulapp.thread.AwfulThread;
import com.ferg.awfulapp.thread.AwfulURL;
import com.ferg.awfulapp.thread.AwfulURL.TYPE;
import com.ferg.awfulapp.widget.AwfulHeaderTransformer;
import com.ferg.awfulapp.widget.NumberPicker;

import java.text.SimpleDateFormat;
import java.util.Date;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher.Options;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.AbsListViewDelegate;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.WebViewDelegate;

/**
 * Uses intent extras:
 *  TYPE - STRING ID - DESCRIPTION
 *	int - Constants.FORUM_ID - id number for the forum
 *	int - Constants.FORUM_PAGE - page number to load
 *
 *  Can also handle an HTTP intent that refers to an SA forumdisplay.php? url.
 */
public class ForumDisplayFragment extends AwfulFragment implements AwfulUpdateCallback, PullToRefreshAttacher.OnRefreshListener {
    
    private ListView mListView;
    private ImageButton mRefreshBar;
    private ImageButton mNextPage;
    private ImageButton mPrevPage;
    private TextView mPageCountText;
	private ImageButton mToggleSidebar;
	
	private View mProbationBar;
	private TextView mProbationMessage;
	private ImageButton mProbationButton;
    
    private int mForumId;
    private int mPage = 1;
    private int mLastPage = 1;
    private String mTitle;
    private boolean skipLoad = false;
    private int mParentForumId = 0;
    
    private boolean loadFailed = false;
    
    private static final int buttonSelectedColor = 0x8033b5e5;//0xa0ff7f00;//TODO move to theme area once we rewrite themes.
    
    private long lastRefresh = 0;

    public static ForumDisplayFragment newInstance(int aForum, int page, boolean skipLoad) {
        ForumDisplayFragment fragment = new ForumDisplayFragment();
        if(aForum < 1){
        	aForum = Constants.USERCP_ID;
        }
        Bundle args = new Bundle();
        args.putInt(Constants.FORUM_ID, aForum);
        fragment.setArguments(args);
        
        fragment.skipLoad(skipLoad);//we don't care about persisting this

        return fragment;
    }

    public ForumDisplayFragment() {
        TAG = "ForumDisplayFragment";
    }

	private ThreadCursorAdapter mCursorAdapter;
    private ForumContentsCallback mForumLoaderCallback = new ForumContentsCallback(mHandler);
    private ForumDataCallback mForumDataCallback = new ForumDataCallback(mHandler);


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState); if(DEBUG) Log.e(TAG,"onCreate");
		setRetainInstance(true);
        setHasOptionsMenu(!(getActivity() instanceof ThreadDisplayActivity));
    }
	@Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        View result = inflateView(R.layout.forum_display, aContainer, aInflater);
    	mListView = (ListView) result.findViewById(R.id.forum_list);
        //mListView.setDrawingCacheEnabled(true);

        if(mP2RAttacher != null){
            mP2RAttacher.addRefreshableView(mListView,new AbsListViewDelegate(), this);
            mP2RAttacher.setPullFromBottom(false);
        	mP2RAttacher.setEnabled(true);
        }
        mPageCountText = (TextView) result.findViewById(R.id.page_count);
		getAwfulActivity().setPreferredFont(mPageCountText);
		mNextPage = (ImageButton) result.findViewById(R.id.next_page);
		mPrevPage = (ImageButton) result.findViewById(R.id.prev_page);
		mRefreshBar  = (ImageButton) result.findViewById(R.id.refresh);
		mToggleSidebar = (ImageButton) result.findViewById(R.id.toggle_sidebar);
		mToggleSidebar.setOnClickListener(onButtonClick);
        mToggleSidebar.setImageResource(R.drawable.ic_actionbar_load);
		mNextPage.setOnClickListener(onButtonClick);
		mPrevPage.setOnClickListener(onButtonClick);
		mRefreshBar.setOnClickListener(onButtonClick);
		mPageCountText.setOnClickListener(onButtonClick);
		if(getAwfulActivity() instanceof ThreadDisplayActivity){
			View v = result.findViewById(R.id.secondary_title_bar);
			if(v != null){
				v.setVisibility(View.VISIBLE);
				aq.find(R.id.move_up).clicked(onButtonClick);
			}
		}
		updatePageBar();
		mProbationBar = (View) result.findViewById(R.id.probationbar);
		mProbationMessage = (TextView) result.findViewById(R.id.probation_message);
		mProbationButton  = (ImageButton) result.findViewById(R.id.go_to_LC);
		updateProbationBar();
		
        return result;
    }
	
	@Override
	public void onAttach(Activity aActivity) {
		super.onAttach(aActivity);
    	mP2RAttacher = this.getAwfulActivity().getPullToRefreshAttacher();
	}

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

    	if(aSavedState != null){
        	Log.i(TAG,"Restoring state!");
        	mForumId = aSavedState.getInt(Constants.FORUM_ID, mForumId);
        	mPage = aSavedState.getInt(Constants.FORUM_PAGE, 1);
        }else if(mForumId < 1){
	    	mForumId = getActivity().getIntent().getIntExtra(Constants.FORUM_ID, mForumId);
	    	mPage = getActivity().getIntent().getIntExtra(Constants.FORUM_PAGE, mPage);
	    	
	        Bundle args = getArguments();
	    	Uri urldata = getActivity().getIntent().getData();
        
	        if(urldata != null){
	        	AwfulURL aurl = AwfulURL.parse(getActivity().getIntent().getDataString());
	        	if(aurl.getType() == TYPE.FORUM){
	        		mForumId = (int) aurl.getId();
	        		mPage = (int) aurl.getPage();
	        	}
	        }else if(args != null){
	        	mForumId = args.getInt(Constants.FORUM_ID, Constants.USERCP_ID);
	        	mPage = args.getInt(Constants.FORUM_PAGE, 1);
	        }
    	}
        

        mCursorAdapter = new ThreadCursorAdapter((AwfulActivity) getActivity(), null, this);
        mListView.setAdapter(mCursorAdapter);
        mListView.setOnItemClickListener(onThreadSelected);
        mListView.setBackgroundColor(ColorProvider.getBackgroundColor(mPrefs));
        mListView.setCacheColorHint(ColorProvider.getBackgroundColor(mPrefs));

        
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
			
			if(mForumId != 0){
				aq.find(R.id.move_up).visible();
			}else{
				aq.find(R.id.move_up).invisible();
			}
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
    public void onStart() {
        super.onStart();
		refreshInfo();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        getActivity().getContentResolver().registerContentObserver(AwfulForum.CONTENT_URI, true, mForumDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulThread.CONTENT_URI, true, mForumLoaderCallback);
        if(skipLoad || !isFragmentVisible()){
        	skipLoad = false;//only skip the first time
        }else{
        	syncForumsIfStale();
        }
    }
	
	@Override
	public void onPageVisible() {
		syncForumsIfStale();
	}
	
	@Override
	public void onPageHidden() {
		
	}
    
    @Override
    public void onPause() {
        super.onPause();
    }
    
    @Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState); if(DEBUG) Log.e(TAG,"onSaveInstanceState");
        outState.putInt(Constants.FORUM_PAGE, getPage());
    	outState.putInt(Constants.FORUM_ID, getForumId());
	}
    
	@Override
    public void onStop() {
        super.onStop(); if(DEBUG) Log.e(TAG, "Stop");
        closeLoaders();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView(); if(DEBUG) Log.e(TAG, "DestroyView");
    }
    
    @Override
    public void onDetach() {
        super.onDetach(); if(DEBUG) Log.e(TAG, "Detach");
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	menu.clear();
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
				ucp.setIcon(R.drawable.ic_interface_bookmarks);
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
	        Cursor row = mCursorAdapter.getRow(info.id);
            if(row != null && row.getColumnIndex(AwfulThread.BOOKMARKED)>-1) {
	              inflater.inflate(R.menu.thread_longpress, aMenu);
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
        		int lastPage = AwfulPagedItem.indexToPage(mCursorAdapter.getInt(info.id, AwfulThread.POSTCOUNT), mPrefs.postPerPage);
            	viewThread((int) info.id,lastPage);
                return true;
            case R.id.mark_thread_unread:
            	markUnread((int) info.id);
                return true;
            case R.id.thread_bookmark:
            	toggleThreadBookmark((int)info.id, (mCursorAdapter.getInt(info.id, AwfulThread.BOOKMARKED)+1)%2);
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
                            Log.e(TAG, "Not a valid number: " + e.toString());
                            Toast.makeText(getActivity(),
                                R.string.invalid_page, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
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
        	Cursor row = mCursorAdapter.getRow(aId);
            if(row != null && row.getColumnIndex(AwfulThread.BOOKMARKED)>-1) {
                    Log.i(TAG, "Thread ID: " + Long.toString(aId));
                    int unreadPage = AwfulPagedItem.getLastReadPage(row.getInt(row.getColumnIndex(AwfulThread.UNREADCOUNT)),
                    												row.getInt(row.getColumnIndex(AwfulThread.POSTCOUNT)),
                    												mPrefs.postPerPage,
                    												row.getInt(row.getColumnIndex(AwfulThread.HAS_VIEWED_THREAD)));
                    viewThread((int) aId, unreadPage);
            }else if(row != null && row.getColumnIndex(AwfulForum.PARENT_ID)>-1){
                    displayForumContents((int) aId);
            }
        }
    };
    
    @Override
    public void loadingFailed(Message aMsg) {
    	super.loadingFailed(aMsg);
        Log.e(TAG, "Loading failed.");
		if(aMsg.obj == null && getActivity() != null){
			Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
		}
		lastRefresh = System.currentTimeMillis();
		loadFailed = true;
    }

    @Override
	public void loadingSucceeded(Message aMsg) {
		super.loadingSucceeded(aMsg);
		
		switch (aMsg.what) {
    	case AwfulSyncService.MSG_GRAB_IMAGE:
    		if(isResumed() && isVisible()){
    			mListView.invalidate();
    		}
    		break;
        case AwfulSyncService.MSG_SYNC_FORUM:
    			getLoaderManager().restartLoader(Constants.FORUM_THREADS_LOADER_ID, null, mForumLoaderCallback);
    			lastRefresh = System.currentTimeMillis();
                mRefreshBar.setColorFilter(0);
                mToggleSidebar.setColorFilter(0);
    	    	loadFailed = false;
    	    	break;
		}
	}
	
	@Override
	public void loadingUpdate(Message aMsg) {
		super.loadingUpdate(aMsg);
		switch (aMsg.what) {
			case AwfulSyncService.MSG_SYNC_FORUM:
		        mRefreshBar.setColorFilter(buttonSelectedColor);
		        mToggleSidebar.setColorFilter(buttonSelectedColor);
				break;
		}
	}
	@Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		super.onPreferenceChange(mPrefs);
		getAwfulActivity().setPreferredFont(mPageCountText);
		if(mListView!=null){
			mListView.setBackgroundColor(ColorProvider.getBackgroundColor(prefs));
			mListView.setCacheColorHint(ColorProvider.getBackgroundColor(prefs));
		}
		aq.find(R.id.pagebar).backgroundColor(ColorProvider.getActionbarColor(prefs));
		aq.find(R.id.page_indicator).backgroundColor(ColorProvider.getActionbarFontColor(prefs));
		if(mPageCountText != null){
			mPageCountText.setTextColor(ColorProvider.getActionbarFontColor(prefs));
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
			updateProbationBar();
			refreshInfo();
			syncForum();
		}
	}
	
    private void setForumId(int aForum) {
		mForumId = aForum;
	}
    
    public void openForum(int id, int page){
        if(id == mForumId && page == mPage){
            return;
        }
    	closeLoaders();
    	setForumId(id);//if the fragment isn't attached yet, just set the values and let the lifecycle handle it
    	mPage = page;
    	mLastPage = 0;
    	lastRefresh = 0;
    	loadFailed = false;
    	if(getActivity() != null){
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
				getActivity().invalidateOptionsMenu();
			}
			refreshInfo();
			syncForum();
    	}
    }

	public void syncForum() {
		if(getAwfulActivity() != null && getForumId() > 0){
			getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_SYNC_FORUM, getForumId(), getPage());
		}
    }
	
	public void syncForumsIfStale() {
		long currentTime = System.currentTimeMillis()-(1000*60*5);
		if(!loadFailed && lastRefresh < currentTime){
			syncForum();
		}
	}
	
	private void markUnread(int id) {
        getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_MARK_UNREAD,id,0);
    }
	
	public boolean isBookmark(){
		return getForumId()==Constants.USERCP_ID;
	}
	
	/** Set Bookmark status.
	 * @param id Thread ID
	 * @param addRemove 1 to add bookmark, 0 to remove.
	 */
    private void toggleThreadBookmark(int id, int addRemove) {
        getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_SET_BOOKMARK,id,addRemove);
    }
	
	private class ForumContentsCallback extends ContentObserver implements LoaderManager.LoaderCallbacks<Cursor> {

		public ForumContentsCallback(Handler handler) {
			super(handler);
		}

		@Override
		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
			if(DEBUG) Log.e(TAG,"Creating forum cursor: "+getForumId());
        	if(isBookmark()){
	        	return new CursorLoader(getActivity(), 
						AwfulThread.CONTENT_URI_UCP, 
						AwfulProvider.ThreadProjection, 
						AwfulProvider.TABLE_UCP_THREADS+"."+AwfulThread.INDEX+">=? AND "+AwfulProvider.TABLE_UCP_THREADS+"."+AwfulThread.INDEX+"<?", 
						AwfulProvider.int2StrArray(AwfulPagedItem.forumPageToIndex(getPage()),AwfulPagedItem.forumPageToIndex(getPage()+1)), 
						(mPrefs.newThreadsFirstUCP? AwfulThread.HAS_NEW_POSTS+" DESC, " + AwfulThread.INDEX :AwfulThread.INDEX));
        	}else{
	            return new CursorLoader(getActivity(), 
	            						AwfulThread.CONTENT_URI, 
	            						AwfulProvider.ThreadProjection, 
	            						AwfulThread.FORUM_ID+"=? AND "+AwfulThread.INDEX+">=? AND "+AwfulThread.INDEX+"<?", 
	            						AwfulProvider.int2StrArray(getForumId(),AwfulPagedItem.forumPageToIndex(getPage()),AwfulPagedItem.forumPageToIndex(getPage()+1)),
	            						(mPrefs.newThreadsFirstForum? AwfulThread.HAS_NEW_POSTS+" DESC, " + AwfulThread.INDEX :AwfulThread.INDEX));
        	}
        }

		@Override
        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
			if(DEBUG) Log.e(TAG,"Forum contents finished, populating: "+aData.getCount());
        	if(!aData.isClosed() && aData.moveToFirst()){
            	mCursorAdapter.swapCursor(aData);
            	//mPullRefreshListView.getRefreshableView().setAdapter(mCursorAdapter);
        	}else{
            	mCursorAdapter.swapCursor(null);
        	}
        }

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			if(DEBUG) Log.e(TAG,"ForumContentsCallback - onLoaderReset");
        	//mPullRefreshListView.getRefreshableView().setAdapter(null);
			mCursorAdapter.swapCursor(null);
		}
		
        @Override
        public void onChange (boolean selfChange){
        	if(DEBUG) Log.e(TAG,"Thread List update.");
        	//onChange triggers as the DB updates
        	//but we don't want to trigger if we are in the middle of loading
        	if(getProgressPercent() > 99){
        		refreshInfo();
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
        	if(DEBUG) Log.v(TAG,"Forum title finished, populating: "+aData.getCount());
        	if(!aData.isClosed() && aData.moveToFirst()){
                mTitle = aData.getString(aData.getColumnIndex(AwfulForum.TITLE));
                mParentForumId = aData.getInt(aData.getColumnIndex(AwfulForum.PARENT_ID));
            	if(getActivity() != null){
            		getAwfulActivity().setActionbarTitle(mTitle, ForumDisplayFragment.this);
            	}
    			if(mForumId == 0){
    				aq.find(R.id.second_titlebar).text(R.string.forums_title);
    			}else{
//    				aq.find(R.id.second_titlebar).text(mTitle);
    				aq.find(R.id.second_titlebar).text(Html.fromHtml(mTitle));
    			}
        		mLastPage = aData.getInt(aData.getColumnIndex(AwfulForum.PAGE_COUNT));
        	}

			updatePageBar();
			updateProbationBar();
        }
        
        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        }

        @Override
        public void onChange (boolean selfChange){
        	if(DEBUG) Log.e(TAG,"Thread Data update.");
        	//onChange triggers as the DB updates
        	//but we don't want to trigger if we are in the middle of loading
        	if(getProgressPercent() > 99){
        		refreshInfo();
        	}
        }
    }
	
	private void refreshInfo(){
		if(getActivity() != null){
			getLoaderManager().restartLoader(Constants.FORUM_THREADS_LOADER_ID, null, mForumLoaderCallback);
	    	getLoaderManager().restartLoader(Constants.FORUM_LOADER_ID, null, mForumDataCallback);
		}
	}
	
	private void closeLoaders(){
		if(getActivity() != null){
	        getLoaderManager().destroyLoader(Constants.FORUM_THREADS_LOADER_ID);
	        getLoaderManager().destroyLoader(Constants.FORUM_LOADER_ID);
		}
	}
	
	public String getTitle(){
		return mTitle;
	}
    
	public void skipLoad(boolean skip) {
		skipLoad = skip;
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
	            	mListView.smoothScrollBy(-mListView.getHeight()/2, 0);
	            }
	            return true;
	        case KeyEvent.KEYCODE_VOLUME_DOWN:
	            if (action == KeyEvent.ACTION_DOWN) {
	            	mListView.smoothScrollBy(mListView.getHeight()/2, 0);
	            }
	            return true;
	        default:
	            return false;
	        }
	}

	@Override
	public void onRefreshStarted(View view) {
		syncForum();
	}
}
