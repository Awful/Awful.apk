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
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.service.ThreadCursorAdapter;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.BookmarkColorRequest;
import com.ferg.awfulapp.task.BookmarkRequest;
import com.ferg.awfulapp.task.MarkUnreadRequest;
import com.ferg.awfulapp.task.ThreadListRequest;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.thread.AwfulPagedItem;
import com.ferg.awfulapp.thread.AwfulThread;
import com.ferg.awfulapp.thread.AwfulURL;
import com.ferg.awfulapp.thread.AwfulURL.TYPE;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import java.util.Date;

/**
 * Uses intent extras:
 *  TYPE - STRING ID - DESCRIPTION
 *	int - Constants.FORUM_ID - id number for the forum
 *	int - Constants.FORUM_PAGE - page number to load
 *
 *  Can also handle an HTTP intent that refers to an SA forumdisplay.php? url.
 */
public class ForumDisplayFragment extends AwfulFragment implements SwipyRefreshLayout.OnRefreshListener {
    
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
    
    private long lastRefresh = 0;

    public ForumDisplayFragment(int forumId, int page, boolean skip) {
        super();
        if(forumId < 1){
            mForumId = Constants.USERCP_ID;
        }else{
            mForumId = forumId;
        }
        mPage = page;
        skipLoad = skip;
        TAG = "ForumDisplayFragment";
    }

    public ForumDisplayFragment() {
        super();
        TAG = "ForumDisplayFragment";
    }

	private ThreadCursorAdapter mCursorAdapter;
    private ForumContentsCallback mForumLoaderCallback = new ForumContentsCallback(mHandler);
    private ForumDataCallback mForumDataCallback = new ForumDataCallback(mHandler);


    @Override
    public void onAttach(Activity aActivity) {
        super.onAttach(aActivity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState); if(DEBUG) Log.e(TAG,"onCreate");
		setRetainInstance(true);
        setHasOptionsMenu(true);
    }
	@Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        View result = inflateView(R.layout.forum_display, aContainer, aInflater);
    	mListView = (ListView) result.findViewById(R.id.forum_list);
        mPageCountText = (TextView) result.findViewById(R.id.page_count);
		getAwfulActivity().setPreferredFont(mPageCountText);
		mNextPage = (ImageButton) result.findViewById(R.id.next_page);
		mPrevPage = (ImageButton) result.findViewById(R.id.prev_page);
		mRefreshBar  = (ImageButton) result.findViewById(R.id.refresh);
		mToggleSidebar = (ImageButton) result.findViewById(R.id.toggle_sidebar);
		mToggleSidebar.setOnClickListener(onButtonClick);
		mNextPage.setOnClickListener(onButtonClick);
		mPrevPage.setOnClickListener(onButtonClick);
		mRefreshBar.setOnClickListener(onButtonClick);
		mPageCountText.setOnClickListener(onButtonClick);
		updatePageBar();
		mProbationBar = result.findViewById(R.id.probationbar);
		mProbationMessage = (TextView) result.findViewById(R.id.probation_message);
		mProbationButton  = (ImageButton) result.findViewById(R.id.go_to_LC);

		updateProbationBar();
		
        return result;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mSRL = (SwipyRefreshLayout) view.findViewById(R.id.forum_swipe);
        mSRL.setOnRefreshListener(this);
        mSRL.setColorSchemeResources(
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light,
                android.R.color.holo_blue_bright);
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
        
        updateColors();
        
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
    public void onResume() {
        super.onResume();
		updateColors();
        getActivity().getContentResolver().registerContentObserver(AwfulForum.CONTENT_URI, true, mForumDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulThread.CONTENT_URI, true, mForumLoaderCallback);
        if(skipLoad || !isFragmentVisible()){
        	skipLoad = false;//only skip the first time
        }else{
        	syncForumsIfStale();
        }
        refreshInfo();
    }
	
	@Override
	public void onPageVisible() {
		updateColors();
		syncForumsIfStale();
//		if(mP2RAttacher != null){
//			mP2RAttacher.setPullFromBottom(false);
//		}
        refreshInfo();
	}

	@Override
	public void onPageHidden() {

	}

    @Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState); if(DEBUG) Log.e(TAG,"onSaveInstanceState");
        outState.putInt(Constants.FORUM_PAGE, getPage());
    	outState.putInt(Constants.FORUM_ID, getForumId());
	}

    @Override
    protected void cancelNetworkRequests() {
        super.cancelNetworkRequests();
        NetworkUtils.cancelRequests(ThreadListRequest.REQUEST_TAG);
    }

    @Override
    public void onStop() {
        super.onStop();
        closeLoaders();
    }


    @Override
    public void onCreateContextMenu(ContextMenu aMenu, View aView, ContextMenuInfo aMenuInfo) {
        super.onCreateContextMenu(aMenu, aView, aMenuInfo);
        if(aMenuInfo instanceof AdapterContextMenuInfo){
	        android.view.MenuInflater inflater = getActivity().getMenuInflater();
	        AdapterContextMenuInfo info = (AdapterContextMenuInfo) aMenuInfo;
	        Cursor row = mCursorAdapter.getRow(info.id);
            if(row != null && row.getInt(row.getColumnIndex(AwfulThread.BOOKMARKED))>-1) {
	              inflater.inflate(R.menu.thread_longpress, aMenu);
	              if(row.getInt(row.getColumnIndex(AwfulThread.BOOKMARKED))<1 || !mPrefs.coloredBookmarks){
	            	  MenuItem bookmarkColor = aMenu.findItem(R.id.thread_bookmark_color);
	            	  if(bookmarkColor != null){
	            		  bookmarkColor.setEnabled(false);
	            		  bookmarkColor.setVisible(false);
	            	  }
	              }
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
            case R.id.go_to_page:
                int maxPage = AwfulPagedItem.indexToPage(mCursorAdapter.getInt(info.id, AwfulThread.POSTCOUNT), mPrefs.postPerPage);
                selectPage((int) info.id, maxPage);
                return true;
            case R.id.mark_thread_unread:
            	markUnread((int) info.id);
                return true;
            case R.id.thread_bookmark:
            	toggleThreadBookmark((int)info.id, (mCursorAdapter.getInt(info.id, AwfulThread.BOOKMARKED)+1)%2>0);
                return true;
            case R.id.thread_bookmark_color:
            	toggleBookmarkColor((int)info.id, (mCursorAdapter.getInt(info.id, AwfulThread.BOOKMARKED)));
                return true;
            case R.id.copy_url_thread:
            	copyUrl((int) info.id);
                return true;
        }

        return false;
    }

    //TODO: combine with displayPagePicker()
    private void selectPage(final int threadId, final int maxPage) {
        View NumberPickerView = this.getActivity().getLayoutInflater().inflate(R.layout.number_picker, null);
        final NumberPicker NumberPicker = (NumberPicker) NumberPickerView.findViewById(R.id.pagePicker);
        NumberPicker.setMinValue(1);
        NumberPicker.setMaxValue(maxPage);
        NumberPicker.setValue(maxPage);
        Button NumberPickerMin = (Button) NumberPickerView.findViewById(R.id.min);
        NumberPickerMin.setText(Integer.toString(1));
        NumberPickerMin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NumberPicker.setValue(1);
            }
        });
        Button NumberPickerMax = (Button) NumberPickerView.findViewById(R.id.max);
        NumberPickerMax.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NumberPicker.setValue(maxPage);
            }
        });
        NumberPickerMax.setText(Integer.toString(maxPage));
        new AlertDialog.Builder(getActivity())
                .setTitle("Jump to Page")
                .setView(NumberPickerView)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface aDialog, int aWhich) {
                                try {
                                    int pageInt = NumberPicker.getValue();
                                    if (pageInt > 0 && pageInt <= maxPage) {
                                        viewThread(threadId, pageInt);
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

    private void viewThread(int id, int page){
    	displayThread(id, page, getForumId(), getPage(), true);
    }

    private void copyUrl(int id) {
        String clipLabel = String.format("Thread #%d", id);
        String clipText  = Constants.FUNCTION_THREAD + "?" + Constants.PARAM_THREAD_ID + "=" + id;
        safeCopyToClipboard(clipLabel, clipText, R.string.copy_url_success);
    }

	private void displayPagePicker() {
        View NumberPickerView = this.getActivity().getLayoutInflater().inflate(R.layout.number_picker, null);
        final NumberPicker NumberPicker = (NumberPicker) NumberPickerView.findViewById(R.id.pagePicker);
        NumberPicker.setMinValue(1);
        NumberPicker.setMaxValue(getLastPage());
        NumberPicker.setValue(getPage());
        Button NumberPickerMin = (Button) NumberPickerView.findViewById(R.id.min);
        NumberPickerMin.setText(Integer.toString(1));
        NumberPickerMin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NumberPicker.setValue(1);
            }
        });
        Button NumberPickerMax = (Button) NumberPickerView.findViewById(R.id.max);
        NumberPickerMax.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NumberPicker.setValue(getLastPage());
            }
        });
        NumberPickerMax.setText(Integer.toString(getLastPage()));
        new AlertDialog.Builder(getActivity())
            .setTitle("Jump to Page")
            .setView(NumberPickerView)
            .setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface aDialog, int aWhich) {
                        try {
                            int pageInt = NumberPicker.getValue();
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
	public void onPreferenceChange(AwfulPreferences prefs, String key) {
		super.onPreferenceChange(mPrefs, key);
		getAwfulActivity().setPreferredFont(mPageCountText);	
		updateColors();
        if(null != mListView) {
            mListView.invalidate();
            mListView.invalidateViews();
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
            // interrupt any scrolling animation and jump to the top of the page
            mListView.smoothScrollBy(0,0);
            mListView.setSelection(0);
            // display the chosen page (may be cached), then update its contents
			refreshInfo();
			syncForum();
		}
	}
	
    private void setForumId(int aForum) {
		mForumId = aForum;
		if(mPrefs != null && mPrefs.forceForumThemes && (mForumId == Constants.FORUM_ID_YOSPOS || mForumId == Constants.FORUM_ID_BYOB)){
			onPreferenceChange(mPrefs, null);
		}
	}
    
    public void openForum(int id, int page){
        if(id == mForumId && page == mPage){
            return;
        }
    	closeLoaders();
    	setForumId(id);//if the fragment isn't attached yet, just set the values and let the lifecycle handle it
    	updateColors();
    	mPage = page;
    	mLastPage = 0;
    	lastRefresh = 0;
    	loadFailed = false;
    	if(getActivity() != null){
			((ForumsIndexActivity) getActivity()).setNavIds(id, null);
			getActivity().invalidateOptionsMenu();
			refreshInfo();
			syncForum();
    	}
    }

	public void syncForum() {
		if(getActivity() != null && getForumId() > 0){
            // cancel pending thread list loading requests
            NetworkUtils.cancelRequests(ThreadListRequest.REQUEST_TAG);
            // call this with cancelOnDestroy=false to retain the request's specific type tag
            queueRequest(new ThreadListRequest(getActivity(), getForumId(), getPage()).build(this,
                    new AwfulRequest.AwfulResultCallback<Void>() {
                        @Override
                        public void success(Void result) {
                            lastRefresh = System.currentTimeMillis();
                            mRefreshBar.setColorFilter(0);
                            mToggleSidebar.setColorFilter(0);
                            loadFailed = false;
                            refreshInfo();
                            mListView.setSelectionAfterHeaderView();
                        }

                        @Override
                        public void failure(VolleyError error) {
                            if(null != error.getMessage() && error.getMessage().startsWith("java.net.ProtocolException: Too many redirects")){
                                Log.e(TAG, "Error: "+error.getMessage());
                                Log.e(TAG, "!!!Failed to sync thread list - You are now LOGGED OUT");
                                NetworkUtils.clearLoginCookies(getAwfulActivity());
                                getAwfulActivity().startActivity(new Intent().setClass(getAwfulActivity(), AwfulLoginActivity.class));
                            }
                            refreshInfo();
                            lastRefresh = System.currentTimeMillis();
                            loadFailed = true;
                            mListView.setSelectionAfterHeaderView();
                        }
                    }
            ), false);

		}
    }
	
	public void syncForumsIfStale() {
		long currentTime = System.currentTimeMillis()-(1000*60*5);
		if(!loadFailed && lastRefresh < currentTime){
			syncForum();
		}
	}
	
	private void markUnread(int id) {
        queueRequest(new MarkUnreadRequest(getActivity(), id).build(this, new AwfulRequest.AwfulResultCallback<Void>() {
            @Override
            public void success(Void result) {
                displayAlert(R.string.mark_unread_success, 0, R.attr.iconMenuLoadSuccess);
                refreshInfo();
            }

            @Override
            public void failure(VolleyError error) {
                refreshInfo();
            }
        }));
    }
	
	public boolean isBookmark(){
		return getForumId()==Constants.USERCP_ID;
	}
	
	/** Set Bookmark status.
	 * @param id Thread ID
	 * @param add true to add bookmark, false to remove.
	 */
    private void toggleThreadBookmark(int id, boolean add) {
        queueRequest(new BookmarkRequest(getActivity(), id, add).build(this, new AwfulRequest.AwfulResultCallback<Void>() {
            @Override
            public void success(Void result) {
                refreshInfo();
            }

            @Override
            public void failure(VolleyError error) {
                refreshInfo();
            }
        }));
    }
    
	/** Toggle Bookmark color status.
	 * @param id Thread ID
	 */
    private void toggleBookmarkColor(final int id, final int bookmarkStatus) {
    	final ContentResolver cr = this.getAwfulApplication().getContentResolver();
    	if(bookmarkStatus==3){
    		queueRequest(new BookmarkColorRequest(getActivity(), id).build(this, new AwfulRequest.AwfulResultCallback<Void>() {

				@Override
				public void success(Void result) {}

				@Override
				public void failure(VolleyError error) {}
    		}));
    	}
        queueRequest(new BookmarkColorRequest(getActivity(), id).build(this, new AwfulRequest.AwfulResultCallback<Void>() {
            @Override
            public void success(Void result) {
            	ContentValues cv = new ContentValues();
                cv.put(AwfulThread.BOOKMARKED, ((bookmarkStatus==3)?bookmarkStatus+2:bookmarkStatus+1)%4);
                cr.update(AwfulThread.CONTENT_URI, cv, AwfulThread.ID+"=?", AwfulProvider.int2StrArray(id));
            	refreshInfo();
            }

            @Override
            public void failure(VolleyError error) {
                refreshInfo();
            }
        }));
    }

    @Override
    public void onRefresh(SwipyRefreshLayoutDirection swipyRefreshLayoutDirection) {
        syncForum();
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
			if(DEBUG) Log.e(TAG,"Forum contents finished, populating");
        	if(aData != null && !aData.isClosed() && aData.moveToFirst()){
            	mCursorAdapter.swapCursor(aData);
        	}else{
            	mCursorAdapter.swapCursor(null);
        	}
        }

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			if(DEBUG) Log.e(TAG,"ForumContentsCallback - onLoaderReset");
			mCursorAdapter.swapCursor(null);
		}
		
        @Override
        public void onChange (boolean selfChange){
        	if(DEBUG) Log.e(TAG,"Thread List update.");
        }
    }
	
	private class ForumDataCallback extends ContentObserver implements LoaderManager.LoaderCallbacks<Cursor> {

        public ForumDataCallback(Handler handler) {
			super(handler);
		}

		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            if(DEBUG) Log.e(TAG,"Creating forum title cursor: "+getForumId());
            return new CursorLoader(getActivity(), ContentUris.withAppendedId(AwfulForum.CONTENT_URI, getForumId()), 
            		AwfulProvider.ForumProjection, null, null, null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	if(DEBUG) Log.v(TAG,"Forum title finished, populating: "+aData.getCount());
        	if(aData != null && !aData.isClosed() && aData.moveToFirst()){
                mTitle = aData.getString(aData.getColumnIndex(AwfulForum.TITLE));
                mParentForumId = aData.getInt(aData.getColumnIndex(AwfulForum.PARENT_ID));
            	if(getActivity() != null){
            		getAwfulActivity().setActionbarTitle(mTitle, ForumDisplayFragment.this);
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
        	if(DEBUG) Log.i(TAG,"Thread Data update.");
        }
    }
	
	private void refreshInfo(){
		if(getActivity() != null){
			restartLoader(Constants.FORUM_THREADS_LOADER_ID, null, mForumLoaderCallback);
	    	restartLoader(Constants.FORUM_LOADER_ID, null, mForumDataCallback);
		}
	}
	
	private void closeLoaders(){
        //FIXME:
        try {
            if (getActivity() != null) {
                getLoaderManager().destroyLoader(Constants.FORUM_THREADS_LOADER_ID);
                getLoaderManager().destroyLoader(Constants.FORUM_LOADER_ID);
            }
        }catch(NullPointerException npe){
            Log.e(TAG,npe.getStackTrace().toString());
        }
	}
	
	public String getTitle(){
		return mTitle;
	}
    
	public void skipLoad(boolean skip) {
		skipLoad = skip;
	}

	@Override
	public String getInternalId() {
		return TAG;
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
	
	private void updateColors(){
		if(mListView != null){
	        if(mPrefs.forceForumThemes && mForumId == Constants.FORUM_ID_YOSPOS){
	            mListView.setBackgroundColor(ColorProvider.getBackgroundColor(ColorProvider.YOSPOS));
	            mListView.setCacheColorHint(ColorProvider.getBackgroundColor(ColorProvider.YOSPOS));
            }else if(mPrefs.forceForumThemes && (mForumId == Constants.FORUM_ID_FYAD || mForumId == Constants.FORUM_ID_FYAD_SUB) ){
                mListView.setBackgroundColor(ColorProvider.getBackgroundColor(ColorProvider.FYAD));
                mListView.setCacheColorHint(ColorProvider.getBackgroundColor(ColorProvider.FYAD));
            }else if(mPrefs.forceForumThemes && (mForumId == Constants.FORUM_ID_BYOB || mForumId == Constants.FORUM_ID_COOL_CREW) ){
                mListView.setBackgroundColor(ColorProvider.getBackgroundColor(ColorProvider.BYOB));
                mListView.setCacheColorHint(ColorProvider.getBackgroundColor(ColorProvider.BYOB));
            }else{
	            mListView.setBackgroundColor(ColorProvider.getBackgroundColor());
	            mListView.setCacheColorHint(ColorProvider.getBackgroundColor());
	        }
		}
//		if(aq != null){
//			if(mPrefs.forceForumThemes && mForumId == Constants.FORUM_ID_YOSPOS){
//				aq.find(R.id.pagebar).backgroundColor(ColorProvider.getActionbarColor(ColorProvider.YOSPOS));
//				aq.find(R.id.page_indicator).backgroundColor(ColorProvider.getActionbarFontColor(ColorProvider.YOSPOS));
//            }else if(mPrefs.forceForumThemes && (mForumId == Constants.FORUM_ID_FYAD || mForumId == Constants.FORUM_ID_FYAD_SUB) ){
//                aq.find(R.id.pagebar).backgroundColor(ColorProvider.getActionbarColor(ColorProvider.FYAD));
//                aq.find(R.id.page_indicator).backgroundColor(ColorProvider.getActionbarFontColor(ColorProvider.FYAD));
//            }else if(mPrefs.forceForumThemes && (mForumId == Constants.FORUM_ID_BYOB || mForumId == Constants.FORUM_ID_COOL_CREW) ){
//                aq.find(R.id.pagebar).backgroundColor(ColorProvider.getActionbarColor(ColorProvider.BYOB));
//                aq.find(R.id.page_indicator).backgroundColor(ColorProvider.getActionbarFontColor(ColorProvider.BYOB));
//            }else{
//				aq.find(R.id.pagebar).backgroundColor(ColorProvider.getActionbarColor());
//				aq.find(R.id.page_indicator).backgroundColor(ColorProvider.getActionbarFontColor());
//			}
//		}
		if(mPageCountText != null){
			mPageCountText.setTextColor(ColorProvider.getActionbarFontColor());
		}
	}	

}
