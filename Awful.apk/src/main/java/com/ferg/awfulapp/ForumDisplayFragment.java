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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.provider.DatabaseHelper;
import com.ferg.awfulapp.search.SearchFilter;
import com.ferg.awfulapp.service.ThreadCursorAdapter;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.BookmarkColorRequest;
import com.ferg.awfulapp.task.BookmarkRequest;
import com.ferg.awfulapp.task.MarkUnreadRequest;
import com.ferg.awfulapp.task.ThreadListRequest;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.thread.AwfulPagedItem;
import com.ferg.awfulapp.thread.AwfulThread;
import com.ferg.awfulapp.widget.PageBar;
import com.ferg.awfulapp.widget.PagePicker;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import timber.log.Timber;

import static com.ferg.awfulapp.constants.Constants.USERCP_ID;

/**
 * Uses intent extras:
 *  TYPE - STRING ID - DESCRIPTION
 *	int - Constants.FORUM_ID - id number for the forum
 *	int - Constants.FORUM_PAGE - page number to load
 *
 *  Can also handle an HTTP intent that refers to an SA forumdisplay.php? url.
 */
public class ForumDisplayFragment extends AwfulFragment implements SwipyRefreshLayout.OnRefreshListener, NavigationEventHandler {

    public static final String KEY_FORUM_ID = "forum ID";
    public static final String KEY_PAGE_NUMBER = "page number";
    public static final String KEY_SKIP_LOAD = "skip load";
    public static final int NULL_FORUM_ID = 0;
    public static final int FIRST_PAGE = 1;
    private ListView mListView;

    private PageBar mPageBar;

    private int currentForumId;
    private int currentPage;
    private int mLastPage = FIRST_PAGE;
    private String mTitle;
    private boolean skipLoad = false;

    private boolean loadFailed = false;

    private long lastRefresh = 0;

    public static ForumDisplayFragment getInstance(int forumId, int pageNum, boolean skipLoad) {
        ForumDisplayFragment fragment = new ForumDisplayFragment();
        fragment.setForumId(forumId);
        fragment.setPage(pageNum);
        fragment.skipLoad = skipLoad;
        return fragment;
    }


	private ThreadCursorAdapter mCursorAdapter;
    private ForumContentsCallback mForumLoaderCallback = new ForumContentsCallback();
    private ForumDataCallback mForumDataCallback = new ForumDataCallback();


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


	@Override
    public View onCreateView(@NonNull LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        View result = inflateView(R.layout.forum_display, aContainer, aInflater);
    	mListView = result.findViewById(R.id.forum_list);

        // page bar
        mPageBar = result.findViewById(R.id.page_bar);
        mPageBar.setListener(new PageBar.PageBarCallbacks() {
            @Override
            public void onPageNavigation(boolean nextPage) {
                goToPage(getPage() + (nextPage ? 1 : -1));
            }

            @Override
            public void onRefreshClicked() {
                syncForum();
            }

            @Override
            public void onPageNumberClicked() {
                selectForumPage();
            }
        });
        getAwfulActivity().setPreferredFont(mPageBar.getTextView());
        updatePageBar();

		refreshProbationBar();

        return result;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TODO: move P2R stuff into AwfulFragment
        setSwipyLayout(view.findViewById(R.id.forum_swipe));
        getSwipyLayout().setOnRefreshListener(this);
        getSwipyLayout().setColorSchemeResources(ColorProvider.getSRLProgressColors(null));
        getSwipyLayout().setProgressBackgroundColor(ColorProvider.getSRLBackgroundColor(null));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            currentForumId = savedInstanceState.getInt(KEY_FORUM_ID);
            currentPage = savedInstanceState.getInt(KEY_PAGE_NUMBER);
            skipLoad = savedInstanceState.getBoolean(KEY_SKIP_LOAD);
            Timber.i("restored state - forumID: %d, page %d, skipLoad: %b", currentForumId, currentPage, skipLoad);
        }

        mCursorAdapter = new ThreadCursorAdapter((AwfulActivity) getActivity(), null, this);
        mListView.setAdapter(mCursorAdapter);
        mListView.setOnItemClickListener(onThreadSelected);
        // TODO: save and restore scroll position - probably need to do the listview trick (get top item, and scroll offset from that) and save it as a deferred value, i.e. on load if there's a scroll value pending, do it and clear it
        updateColors();
        registerForContextMenu(mListView);
    }


    // TODO: pull this out as a shared method/widget in AwfulFragment
	public void updatePageBar(){
        mPageBar.updatePagePosition(getPage(), getLastPage());
	}

    @Override
    public void onResume() {
        super.onResume();
		updateColors();
        if(skipLoad || !isVisible()){
        	skipLoad = false;//only skip the first time
        }else{
        	syncForumsIfStale();
        }
        refreshInfo();
    }

	@Override
	public void setAsFocusedPage() {
        // TODO: find out how this relates to onResume / onStart , it's the same code
        // TODO: this can be called before the fragment's views have been inflated, e.g. bookmark widget -> viewpager#onPageSelected -> (create fragment) -> onPageVisible
		updateColors();
		syncForumsIfStale();
        refreshInfo();
	}


    @Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
        Timber.d("onSaveInstanceState: saving instance state - forumId: %d, page: %d, skipLoad: %b", currentForumId, currentPage, skipLoad);
        outState.putInt(KEY_FORUM_ID, currentForumId);
        outState.putInt(KEY_PAGE_NUMBER, currentPage);
        outState.putBoolean(KEY_SKIP_LOAD, skipLoad);
    }

    @Override
    protected void cancelNetworkRequests() {
        super.cancelNetworkRequests();
        NetworkUtils.cancelRequests(ThreadListRequest.Companion.getREQUEST_TAG());
    }

    @Override
    public void onStop() {
        super.onStop();
        // TODO: cancel network reqs?
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
	              if(row.getInt(row.getColumnIndex(AwfulThread.BOOKMARKED))<1 || !getPrefs().coloredBookmarks){
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
        int threadId = (int) info.id;
        switch (aItem.getItemId()) {
            case R.id.first_page:
            	viewThread(threadId,1);
                return true;
            case R.id.last_page:
                int lastPage = AwfulPagedItem.indexToPage(mCursorAdapter.getInt(threadId, AwfulThread.POSTCOUNT), getPrefs().postPerPage);
                viewThread(threadId,lastPage);
                return true;
            case R.id.go_to_page:
                int maxPage = AwfulPagedItem.indexToPage(mCursorAdapter.getInt(threadId, AwfulThread.POSTCOUNT), getPrefs().postPerPage);
                selectThreadPage(threadId, maxPage);
                return true;
            case R.id.mark_thread_unread:
            	markUnread(threadId);
                return true;
            case R.id.thread_bookmark:
            	toggleThreadBookmark(threadId, (mCursorAdapter.getInt(threadId, AwfulThread.BOOKMARKED)+1)%2>0);
                return true;
            case R.id.thread_bookmark_color:
            	toggleBookmarkColor(threadId, (mCursorAdapter.getInt(threadId, AwfulThread.BOOKMARKED)));
                return true;
            case R.id.search_thread:
                SearchFilter threadFilter = new SearchFilter(SearchFilter.FilterType.ThreadId, Integer.toString(threadId));
                navigate(new NavigationEvent.SearchForums(threadFilter));
                return true;
            case R.id.copy_url_thread:
            	copyUrl(threadId);
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
        new PagePicker(getActivity(), maxPage, maxPage, (button, resultValue) -> {
            if (button == DialogInterface.BUTTON_POSITIVE) {
                viewThread(threadId, resultValue);
            }
        }).show();
    }

    private void selectForumPage() {
        new PagePicker(getActivity(), getLastPage(), getPage(), (button, resultValue) -> {
            if (button == DialogInterface.BUTTON_POSITIVE) {
                goToPage(resultValue);
            }
        }).show();
    }

    private void viewThread(int id, int page){
    	navigate(new NavigationEvent.Thread(id, page, null));
    }

    private void copyUrl(int id) {
        String clipLabel = String.format(Locale.US, "Thread #%d", id);
        String clipText  = Constants.FUNCTION_THREAD + "?" + Constants.PARAM_THREAD_ID + "=" + id;
        safeCopyToClipboard(clipLabel, clipText, R.string.copy_url_success);
    }


    private AdapterView.OnItemClickListener onThreadSelected = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            // TODO: 04/06/2017 why is all this in a threadlist click listener? We know it's a thread! It's not a forum!
            Cursor row = mCursorAdapter.getRow(aId);
            if(row != null && row.getColumnIndex(AwfulThread.BOOKMARKED)>-1) {
                    Timber.i("Thread ID: " + aId);
                    int unreadPage = AwfulPagedItem.getLastReadPage(row.getInt(row.getColumnIndex(AwfulThread.UNREADCOUNT)),
                    												row.getInt(row.getColumnIndex(AwfulThread.POSTCOUNT)),
                    												getPrefs().postPerPage,
                    												row.getInt(row.getColumnIndex(AwfulThread.HAS_VIEWED_THREAD)));
                    viewThread((int) aId, unreadPage);
            }else if(row != null && row.getColumnIndex(AwfulForum.PARENT_ID)>-1){
                navigate(new NavigationEvent.Forum((int) aId, null));
            }
        }
    };

	@Override
	public void onPreferenceChange(AwfulPreferences prefs, String key) {
		super.onPreferenceChange(getPrefs(), key);
        if(mPageBar != null) {
            getAwfulActivity().setPreferredFont(mPageBar.getTextView());
        }
		updateColors();
        if(null != mListView) {
            mListView.invalidate();
            mListView.invalidateViews();
        }
	}

	public int getForumId(){
		return currentForumId;
	}


    /**
     * Set the current page number.
     *
     * This will be bound to the valid range (between the first and last page).
     */
    private void setPage(int pageNumber) {
        currentPage = Math.max(FIRST_PAGE, Math.min(pageNumber, getLastPage()));
    }

	public int getPage(){
		return currentPage;
	}

    private int getLastPage() {
		return mLastPage;
	}


	private void goToPage(int pageNumber) {
        setPage(pageNumber);
        updatePageBar();
        refreshProbationBar();
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
        currentForumId = (forumId < 1) ? USERCP_ID : forumId;
	}



    @Override
    public boolean handleNavigation(@NotNull NavigationEvent event) {
        if (event instanceof NavigationEvent.Bookmarks) {
            openForum(Constants.USERCP_ID, null);
            return true;
        } else if (event instanceof NavigationEvent.Forum) {
            NavigationEvent.Forum forum = (NavigationEvent.Forum) event;
            openForum(forum.getId(), forum.getPage());
            return true;
        }
        return false;
    }


    private void openForum(int id, @Nullable Integer page){
        // do nothing if we're already looking at this page (or if no page specified)
        if (id == currentForumId && (page == null || page == currentPage)) {
            return;
        }
    	setForumId(id);
        setPage(page == null ? FIRST_PAGE : page);
        updateColors();
    	mLastPage = 0;
    	lastRefresh = 0;
    	loadFailed = false;
        if (getActivity() != null) {
            ((ForumsIndexActivity) getActivity()).onPageContentChanged();
        }
        invalidateOptionsMenu();
        refreshInfo();
        syncForum();
    }

	public void syncForum() {
		if(getActivity() != null && getForumId() > 0){
            // cancel pending thread list loading requests
            NetworkUtils.cancelRequests(ThreadListRequest.Companion.getREQUEST_TAG());
            // call this with cancelOnDestroy=false to retain the request's specific type tag
            queueRequest(new ThreadListRequest(getActivity(), getForumId(), getPage()).build(this,
                    new AwfulRequest.AwfulResultCallback<Void>() {
                        @Override
                        public void success(Void result) {
                            lastRefresh = System.currentTimeMillis();
                            // TODO: what does this even do
//                            mRefreshBar.setColorFilter(0);
//                            mToggleSidebar.setColorFilter(0);
                            loadFailed = false;
                            refreshInfo();
                            mListView.setSelectionAfterHeaderView();
                        }

                        @Override
                        public void failure(VolleyError error) {
                            Timber.w("Failed to sync thread list!");
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
                getAlertView().setTitle(R.string.mark_unread_success)
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
    	if(bookmarkStatus==6){
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
                cv.put(AwfulThread.BOOKMARKED, ((bookmarkStatus==6)?bookmarkStatus+2:bookmarkStatus+1)%7);
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

    private class ForumContentsCallback implements LoaderManager.LoaderCallbacks<Cursor> {

		@NonNull
        @Override
		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
			Timber.i("Creating forum cursor: "+getForumId());
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
                        DatabaseHelper.TABLE_UCP_THREADS, AwfulThread.INDEX, DatabaseHelper.TABLE_UCP_THREADS, AwfulThread.INDEX);
                selectionArgs = AwfulProvider.int2StrArray(thisPageIndex, nextPageIndex);
            } else {
                selection = String.format("%s=? AND %s>=? AND %s<?",
                        AwfulThread.FORUM_ID, AwfulThread.INDEX, AwfulThread.INDEX);
                selectionArgs = AwfulProvider.int2StrArray(getForumId(), thisPageIndex, nextPageIndex);
            }

            boolean sortNewFirst = (isBookmarks && getPrefs().newThreadsFirstUCP) || (!isBookmarks && getPrefs().newThreadsFirstForum);
            String sortOrder = sortNewFirst ? AwfulThread.HAS_NEW_POSTS + " DESC, " + AwfulThread.INDEX : AwfulThread.INDEX;

            return new CursorLoader(getActivity(), contentUri, AwfulProvider.ThreadProjection, selection, selectionArgs, sortOrder);
        }

		@Override
        public void onLoadFinished(@NonNull Loader<Cursor> aLoader, Cursor aData) {
			Timber.i("Forum contents finished, populating");
        	if(aData != null && !aData.isClosed() && aData.moveToFirst()){
            	mCursorAdapter.swapCursor(aData);
        	}else{
            	mCursorAdapter.swapCursor(null);
        	}
        }

		@Override
		public void onLoaderReset(@NonNull Loader<Cursor> arg0) {
			Timber.i("ForumContentsCallback - onLoaderReset");
			mCursorAdapter.swapCursor(null);
		}
    }


	private class ForumDataCallback implements LoaderManager.LoaderCallbacks<Cursor> {

		@NonNull
        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            Timber.i("Creating forum title cursor: "+getForumId());
            return new CursorLoader(getActivity(), ContentUris.withAppendedId(AwfulForum.CONTENT_URI, getForumId()),
            		AwfulProvider.ForumProjection, null, null, null);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> aLoader, Cursor aData) {
            if (aData != null && !aData.isClosed() && aData.moveToFirst()) {
                Timber.i("Forum title finished, populating: " + aData.getCount());
                mTitle = aData.getString(aData.getColumnIndex(AwfulForum.TITLE));
                mLastPage = aData.getInt(aData.getColumnIndex(AwfulForum.PAGE_COUNT));
                ForumsIndexActivity activity = ((ForumsIndexActivity) getActivity());
                if (activity != null) {
                    activity.onPageContentChanged();
                }
            }

			updatePageBar();
			refreshProbationBar();
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> aLoader) {
        }
    }

	private void refreshInfo(){
		if(getActivity() != null){
			restartLoader(Constants.FORUM_THREADS_LOADER_ID, null, mForumLoaderCallback);
	    	restartLoader(Constants.FORUM_LOADER_ID, null, mForumDataCallback);
		}
	}


	public String getTitle(){
		return mTitle;
	}


    @Override
    protected boolean doScroll(boolean down) {
        int scrollAmount = mListView.getHeight() / 2;
        mListView.smoothScrollBy((down ? scrollAmount : -scrollAmount), 400 );
        return true;
    }


    private void updateColors() {
        if (mPageBar != null) {
            mPageBar.setTextColour(ColorProvider.ACTION_BAR_TEXT.getColor());
        }
        if (mListView == null) {
            return;
        }
        int backgroundColor = ColorProvider.BACKGROUND.getColor(currentForumId);
        mListView.setBackgroundColor(backgroundColor);
        mListView.setCacheColorHint(backgroundColor);
    }

}
