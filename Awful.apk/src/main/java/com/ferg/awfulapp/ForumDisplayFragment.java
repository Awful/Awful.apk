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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

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
import com.ferg.awfulapp.widget.MinMaxNumberPicker;
import com.ferg.awfulapp.widget.PagePicker;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static com.ferg.awfulapp.constants.Constants.USERCP_ID;

/**
 * Uses intent extras:
 *  TYPE - STRING ID - DESCRIPTION
 *	int - Constants.FORUM_ID - id number for the forum
 *	int - Constants.FORUM_PAGE - page number to load
 *
 *  Can also handle an HTTP intent that refers to an SA forumdisplay.php? url.
 */
public class ForumDisplayFragment extends AwfulFragment implements SwipyRefreshLayout.OnRefreshListener {

    public static final String ARG_KEY_FORUM_ID = "forum ID";
    public static final String ARG_KEY_PAGE_NUMBER = "page number";
    public static final String ARG_KEY_SKIP_LOAD = "skip load";
    public static final int FIRST_PAGE = 1;
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

    private boolean loadFailed = false;

    private long lastRefresh = 0;

    public static ForumDisplayFragment getInstance(int forumId, int pageNum, boolean skipLoad) {
        ForumDisplayFragment fragment = new ForumDisplayFragment();
        Bundle args = new Bundle();
        // TODO: should these use the Constants constants that saveInstanceState etc. uses?
        args.putInt(ARG_KEY_FORUM_ID, forumId);
        args.putInt(ARG_KEY_PAGE_NUMBER, pageNum);
        args.putBoolean(ARG_KEY_SKIP_LOAD, skipLoad);
        fragment.setArguments(args);
        return fragment;
    }

    public ForumDisplayFragment() {
        super();
        TAG = "ForumDisplayFragment";
    }

	private ThreadCursorAdapter mCursorAdapter;
    private ForumContentsCallback mForumLoaderCallback = new ForumContentsCallback(mHandler);
    private ForumDataCallback mForumDataCallback = new ForumDataCallback(mHandler);


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
		setRetainInstance(false);
        setHasOptionsMenu(true);

        Bundle args = getArguments();
        setForumId(args.getInt(ARG_KEY_FORUM_ID));
        setPage(args.getInt(ARG_KEY_PAGE_NUMBER));
        skipLoad = args.getBoolean(ARG_KEY_SKIP_LOAD);
        Log.d(TAG, String.format("onCreate: set forumID to %d, set page to %d", mForumId, mPage));
    }


	@Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        View result = inflateView(R.layout.forum_display, aContainer, aInflater);
    	mListView = (ListView) result.findViewById(R.id.forum_list);

        // page bar
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

        // probation bar
		mProbationBar = result.findViewById(R.id.probationbar);
		mProbationMessage = (TextView) result.findViewById(R.id.probation_message);
		mProbationButton  = (ImageButton) result.findViewById(R.id.go_to_LC);
		updateProbationBar();

        return result;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TODO: move P2R stuff into AwfulFragment
        mSRL = (SwipyRefreshLayout) view.findViewById(R.id.forum_swipe);
        mSRL.setOnRefreshListener(this);
        mSRL.setColorSchemeResources(ColorProvider.getSRLProgressColor());
        mSRL.setProgressBackgroundColor(ColorProvider.getSRLBackgroundColor());
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

        /*
            What happens in here, as far as I can tell, in order of precedence
            - Use saved instance state
            - If current forum is 1+, do nothing (it's assuming data has been set?)
            - IF CURRENT FORUM is < 1:
            -  use ACTIVITY's INTENT's url data if it exists and it's for a FORUM
            -  use BUNDLED INTENT's forum ID and page (assumed to exist)
            -  use ACTIVITY's INTENT and get the forum ID and page from that, defaulting to Bookmarks and/or page 1 if either are missing
         */

        // TODO: clean up whatever rube goldberg initialisation is happening here
        // I don't think any of this gets called? I can't see any saved state coming back in (it gets saved though),
        // and the forum ID and page are already set to valid values (>0) in onCreate - fragment is NOT being retained

    	if(aSavedState != null){
            // if there's saved state, load that
            Log.i(TAG,"Restoring savedInstanceState!");
            setForumId(aSavedState.getInt(Constants.FORUM_ID, mForumId));
            setPage(aSavedState.getInt(Constants.FORUM_PAGE, FIRST_PAGE));
        }else if(mForumId < 1){
            Log.d(TAG, "onActivityCreated: mForumId is less than 1 (" + mForumId + "), messing with intents and bundles");
            // otherwise if forumID is uninitialised/invalid (which is never true now, it's always set and validated for values <1)
            Intent intent = getActivity().getIntent();
            int forumIdFromIntent = intent.getIntExtra(Constants.FORUM_ID, mForumId);
            setForumId(forumIdFromIntent);
            setPage(intent.getIntExtra(Constants.FORUM_PAGE, mPage));
            Log.d(TAG, String.format("onActivityCreated: activity's intent args - got id: %b, got page: %b",
                    intent.hasExtra(Constants.FORUM_ID), intent.hasExtra(Constants.FORUM_PAGE)));
            // I think these are fallbacks if the following doesn't work?

	        Bundle args = getArguments();
	    	Uri urldata = intent.getData();

            // if the activity has a data URI, get that and set the forum and page... again
	        if(urldata != null){
                Log.d(TAG, "onActivityCreated: got URL data from ACTIVITY's intent");
                AwfulURL aurl = AwfulURL.parse(intent.getDataString());
	        	if(aurl.getType() == TYPE.FORUM){
                    Log.d(TAG, "onActivityCreated: URL is for a forum, that's for us");
                    setForumId((int) aurl.getId());
                    setPage((int) aurl.getPage());
                } else {
                    Log.d(TAG, "onActivityCreated: URL was not for a forum, I guess we ignore it then");
                }
	        }else if(args != null){
                Log.d(TAG, "onActivityCreated: no URL data, but we have fragment arguments");
                // default to page 1 of bookmarks if we have no data URI and we have fragment args
                setForumId(args.getInt(Constants.FORUM_ID, USERCP_ID));
                setPage(args.getInt(Constants.FORUM_PAGE, FIRST_PAGE));
                Log.d(TAG, String.format("onActivityCreated: original fragment args - got id: %b, got page: %b",
                        args.get(Constants.FORUM_ID) != null, args.get(Constants.FORUM_PAGE) != null));
            }
    	} else {
            Log.d(TAG, "onActivityCreated: got method call, but there was no savedInstanceState and forum ID was already set");
        }


        mCursorAdapter = new ThreadCursorAdapter((AwfulActivity) getActivity(), null, this);
        mListView.setAdapter(mCursorAdapter);
        mListView.setOnItemClickListener(onThreadSelected);

        updateColors();

        registerForContextMenu(mListView);
    }


    // TODO: pull this out as a shared method/widget in AwfulFragment
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


    // TODO: move the probation bar into AwfulFragment as an optional view/widget, put updateProbeBar in there too
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
        if(skipLoad || !isVisible()){
        	skipLoad = false;//only skip the first time
        }else{
        	syncForumsIfStale();
        }
        refreshInfo();
    }

	@Override
	public void onPageVisible() {
        // TODO: find out how this relates to onResume / onStart , it's the same code
		updateColors();
		syncForumsIfStale();
        refreshInfo();
	}


    @Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        Log.d(TAG, String.format("onSaveInstanceState: saving instance state - forumId: %d, page: %d", getForumId(), getPage()));
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
        // TODO: cancel network reqs?
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
                selectThreadPage((int) info.id, maxPage);
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


    /**
     * Show the dialog to open a thread at a specific page.
     *
     * @param threadId The ID of the thread to open
     * @param maxPage   The last page of the thread
     */
    private void selectThreadPage(final int threadId, final int maxPage) {
        // TODO: this would be better if it got the thread's last page itself
        new PagePicker(getActivity(), maxPage, maxPage, new MinMaxNumberPicker.ResultListener() {
            @Override
            public void onButtonPressed(int button, int resultValue) {
                if (button == DialogInterface.BUTTON_POSITIVE) {
                    viewThread(threadId, resultValue);
                }
            }
        }).show();
    }

    private void selectForumPage() {
        new PagePicker(getActivity(), getLastPage(), getPage(), new MinMaxNumberPicker.ResultListener() {
            @Override
            public void onButtonPressed(int button, int resultValue) {
                if (button == DialogInterface.BUTTON_POSITIVE) {
                    goToPage(resultValue);
                }
            }
        }).show();
    }

    private void viewThread(int id, int page){
    	displayThread(id, page, getForumId(), getPage(), true);
    }

    private void copyUrl(int id) {
        String clipLabel = String.format(Locale.US, "Thread #%d", id);
        String clipText  = Constants.FUNCTION_THREAD + "?" + Constants.PARAM_THREAD_ID + "=" + id;
        safeCopyToClipboard(clipLabel, clipText, R.string.copy_url_success);
    }


    // TODO: this is the page nav bar - move it to AwfulFragment
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
                	selectForumPage();
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


    /**
     * Set the current page number.
     *
     * This will be bound to the valid range (between the first and last page).
     */
    private void setPage(int pageNumber) {
        mPage = Math.max(FIRST_PAGE, Math.min(pageNumber, getLastPage()));
    }

	public int getPage(){
		return mPage;
	}

    private int getLastPage() {
		return mLastPage;
	}


	private void goToPage(int pageNumber) {
        setPage(pageNumber);
        updatePageBar();
        updateProbationBar();
        // interrupt any scrolling animation and jump to the top of the page
        mListView.smoothScrollBy(0,0);
        mListView.setSelection(0);
        // display the chosen page (may be cached), then update its contents
        refreshInfo();
        syncForum();
	}

    /**
     * Set the current Forum ID.
     *
     * Falls back to the Bookmarks forum for invalid ID values
     * @param forumId   the ID to switch to
     */
    private void setForumId(int forumId) {
        mForumId = (forumId < 1) ? USERCP_ID : forumId;
	}

    public void openForum(int id, int page){
        // do nothing if we're already looking at this page
        if(id == mForumId && page == mPage){
            return;
        }
    	closeLoaders();
    	setForumId(id);
        setPage(page);
        updateColors();
    	mLastPage = 0;
    	lastRefresh = 0;
    	loadFailed = false;
    	if(getActivity() != null){
			((ForumsIndexActivity) getActivity()).setNavIds(mForumId, null);
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
                                Log.e(TAG, "Error: " + error.getMessage() + "\nFailed to sync thread list - You are now LOGGED OUT");
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
                new AlertBuilder().setTitle(R.string.mark_unread_success)
                        .setIcon(R.drawable.ic_check_circle)
                        .show();
                refreshInfo();
            }

            @Override
            public void failure(VolleyError error) {
                refreshInfo();
            }
        }));
    }

    // TODO: move the bookmark toggle/cycle code into these methods and out of the menu handler

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
            // TODO: move this query code into a provider class
            boolean isBookmarks = (getForumId() == USERCP_ID);
            int thisPageIndex = AwfulPagedItem.forumPageToIndex(getPage());
            int nextPageIndex = AwfulPagedItem.forumPageToIndex(getPage() + 1);

            // set up some cursor query stuff, depending on whether this is a normal forum or the bookmarks one
            Uri contentUri = isBookmarks ? AwfulThread.CONTENT_URI_UCP : AwfulThread.CONTENT_URI;

            String selection;
            String[] selectionArgs;
            if (isBookmarks) {
                selection = String.format("%s.%s>=? AND %s.%s<?",
                        AwfulProvider.TABLE_UCP_THREADS, AwfulThread.INDEX, AwfulProvider.TABLE_UCP_THREADS, AwfulThread.INDEX);
                selectionArgs = AwfulProvider.int2StrArray(thisPageIndex, nextPageIndex);
            } else {
                selection = String.format("%s=? AND %s>=? AND %s<?",
                        AwfulThread.FORUM_ID, AwfulThread.INDEX, AwfulThread.INDEX);
                selectionArgs = AwfulProvider.int2StrArray(getForumId(), thisPageIndex, nextPageIndex);
            }

            boolean sortNewFirst = (isBookmarks && mPrefs.newThreadsFirstUCP) || (!isBookmarks && mPrefs.newThreadsFirstForum);
            String sortOrder = sortNewFirst ? AwfulThread.HAS_NEW_POSTS + " DESC, " + AwfulThread.INDEX : AwfulThread.INDEX;

            return new CursorLoader(getActivity(), contentUri, AwfulProvider.ThreadProjection, selection, selectionArgs, sortOrder);
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
                mLastPage = aData.getInt(aData.getColumnIndex(AwfulForum.PAGE_COUNT));
                setTitle(mTitle);
        	}

			updatePageBar();
			updateProbationBar();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        }

        @Override
        public void onChange (boolean selfChange){

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
            Log.e(TAG, Arrays.toString(npe.getStackTrace()));
        }
	}

	public String getTitle(){
		return mTitle;
	}


    @Override
    protected boolean doScroll(boolean down) {
        int scrollAmount = mListView.getHeight() / 2;
        mListView.smoothScrollBy(0, down ? scrollAmount : -scrollAmount);
        return true;
    }


    // TODO: maybe refactor ColorProvider so it does the forum ID -> provider constant translation itself
    private void updateColors() {
        if (mPageCountText != null) {
            mPageCountText.setTextColor(ColorProvider.getActionbarFontColor());
        }
        if (mListView == null) {
            return;
        }
        String colorProvider = null;
        // if we're forcing forum themes, see if we need a specific color provider
        if (mPrefs.forceForumThemes) {
            switch (mForumId) {
                case Constants.FORUM_ID_YOSPOS:
                    colorProvider = ColorProvider.YOSPOS;
                    break;
                case Constants.FORUM_ID_FYAD:
                case Constants.FORUM_ID_FYAD_SUB:
                    colorProvider = ColorProvider.FYAD;
                    break;
                case Constants.FORUM_ID_BYOB:
                case Constants.FORUM_ID_COOL_CREW:
                    colorProvider = ColorProvider.BYOB;
                    break;
            }
        }
        int backgroundColor = ColorProvider.getBackgroundColor(colorProvider);
        mListView.setBackgroundColor(backgroundColor);
        mListView.setCacheColorHint(backgroundColor);
    }

}
