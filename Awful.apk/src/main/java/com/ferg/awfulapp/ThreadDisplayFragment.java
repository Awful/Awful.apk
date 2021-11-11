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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ferg.awfulapp.search.SearchFilter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.widget.ShareActionProvider;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.CookieController;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.popupmenu.PostContextMenu;
import com.ferg.awfulapp.popupmenu.UrlContextMenu;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.Keys;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.AwfulTheme;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.BookmarkRequest;
import com.ferg.awfulapp.task.IgnoreRequest;
import com.ferg.awfulapp.task.ImageSizeRequest;
import com.ferg.awfulapp.task.MarkLastReadRequest;
import com.ferg.awfulapp.task.RedirectTask;
import com.ferg.awfulapp.task.RefreshUserProfileRequest;
import com.ferg.awfulapp.task.ReportRequest;
import com.ferg.awfulapp.task.SinglePostRequest;
import com.ferg.awfulapp.task.ThreadLockUnlockRequest;
import com.ferg.awfulapp.task.ThreadPageRequest;
import com.ferg.awfulapp.task.VoteRequest;
import com.ferg.awfulapp.thread.AwfulHtmlPage;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPagedItem;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;
import com.ferg.awfulapp.thread.AwfulURL;
import com.ferg.awfulapp.thread.AwfulURL.TYPE;
import com.ferg.awfulapp.util.AwfulError;
import com.ferg.awfulapp.util.AwfulUtils;
import com.ferg.awfulapp.webview.AwfulWebView;
import com.ferg.awfulapp.webview.LoggingWebChromeClient;
import com.ferg.awfulapp.webview.WebViewJsInterface;
import com.ferg.awfulapp.widget.PageBar;
import com.ferg.awfulapp.widget.PagePicker;
import com.ferg.awfulapp.widget.WebViewSearchBar;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Uses intent extras:
 *  TYPE - STRING ID - DESCRIPTION
 *	int - Constants.THREAD_ID - id number for that thread
 *	int - Constants.THREAD_PAGE - page number to load
 *
 *  Can also handle an HTTP intent that refers to an SA showthread.php? url.
 */
public class ThreadDisplayFragment extends AwfulFragment implements NavigationEventHandler, SwipyRefreshLayout.OnRefreshListener {

	private static final String THREAD_ID_KEY = "thread_id";
	private static final String THREAD_PAGE_KEY = "thread_page";
	private static final String SCROLL_POSITION_KEY = "scroll_position";
	private static final String KEEP_SCREEN_ON_KEY = "screen_stays_on";
	private PostLoaderManager mPostLoaderCallback;
    private ThreadDataCallback mThreadLoaderCallback;

	/*
		Potentially null views, if layout inflation failed (i.e. the WebView package is updating)
	 */
	@Nullable
	private PageBar pageBar = null;
	@Nullable
	private TextView mUserPostNotice;
	@Nullable
	private FloatingActionButton mFAB = null;
	@Nullable
    private AwfulWebView mThreadView = null;

	/** An optional ID to only display posts by a specific user */
    private Integer postFilterUserId = null;
	/** The username to display when filtering by a specific user */
    private String postFilterUsername;
	/** Stores the page the user was on before enabling filtering, so they can jump back */
	private int pageBeforeFiltering = 0;

	private static final int BLANK_USER_ID = 0;
	public static final int FIRST_PAGE = 1;


	private int currentPage = FIRST_PAGE;
	public static final int NULL_THREAD_ID = 0;
	private int currentThreadId = NULL_THREAD_ID;
	
	// TODO: fix this it's all over the place, getting assigned as 1 in loadThread etc - maybe it should default to FIRST_PAGE?
	/** Current thread's last page */
	private int mLastPage = 0;
	private int mParentForumId = 0;
	private boolean threadLocked = false;
	private boolean threadBookmarked = false;
    private boolean threadArchived = false;
	private boolean threadLockableUnlockable = false;

    private boolean keepScreenOn = false;
	//oh god i'm replicating core android functionality, this is a bad sign.
    private final LinkedList<AwfulStackEntry> backStack = new LinkedList<>();
	private boolean bypassBackStack = false;

    private String mTitle = null;
	private String postJump = "";
	private int savedScrollPosition = 0;
	/** Whether the currently displayed page represents a full page of posts */
	private boolean displayingFullPage = false;
	
	private ShareActionProvider shareProvider;

    private ForumsIndexActivity parentActivity;
    
    private final ThreadDisplayFragment mSelf = this;

    @Nullable
	private NavigationEvent pendingNavigation = null;


	private final HashMap<String,String> ignorePostsHtml = new HashMap<>();
    private AsyncTask<Void, Void, String> redirect = null;
	private Uri downloadLink;

	private final ThreadContentObserver mThreadObserver = new ThreadContentObserver(getHandler());



    @Override
    public View onCreateView(@NonNull LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
		try {
			return inflateView(R.layout.thread_display, aContainer, aInflater);
		} catch (InflateException e) {
			if (webViewIsMissing(e)) {
				return null;
			} else {
				throw e;
			}
		}
	}


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

		pageBar = view.findViewById(R.id.page_bar);
		pageBar.setListener(new PageBar.PageBarCallbacks() {
			@Override
			public void onPageNavigation(boolean nextPage) {
				turnPage(nextPage);
			}

			@Override
			public void onRefreshClicked() {
				refresh();
			}

			@Override
			public void onPageNumberClicked() {
				displayPagePicker();
			}
		});
		getAwfulActivity().setPreferredFont(pageBar.getTextView());

		if (savedInstanceState != null) {
			// setting this before the thread view is initialised, so it will reflect the stored state
			keepScreenOn = savedInstanceState.getBoolean(KEEP_SCREEN_ON_KEY);
		}
		mThreadView = view.findViewById(R.id.thread);
		initThreadViewProperties();

		mUserPostNotice = view.findViewById(R.id.thread_userpost_notice);
		refreshProbationBar();

		mFAB = view.findViewById(R.id.just_post);
		mFAB.setOnClickListener(onButtonClick);
		mFAB.hide();

        setAllowedSwipeRefreshDirections(SwipyRefreshLayoutDirection.BOTH);
        setSwipyLayout(view.findViewById(R.id.thread_swipe));
		getSwipyLayout().setColorSchemeResources(ColorProvider.getSRLProgressColors(null));
		getSwipyLayout().setProgressBackgroundColor(ColorProvider.getSRLBackgroundColor(null));
		getSwipyLayout().setEnabled(!getPrefs().disablePullNext);
    }


    @Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState);

        setHasOptionsMenu(true);
        parentActivity = (ForumsIndexActivity) getActivity();
        mPostLoaderCallback = new PostLoaderManager();
        mThreadLoaderCallback = new ThreadDataCallback();

		// if a navigation event is pending, we don't care about any saved state - just do the navigation
		if (pendingNavigation != null) {
			Timber.d("Activity attached: found pending navigation event, going there");
			NavigationEvent event = pendingNavigation;
			pendingNavigation = null;
			navigate(event);
			return;
		}

		boolean loadFromCache = false;
		if (aSavedState != null) {
			// restoring old state - we have a thread ID and page
			// TODO: 04/05/2017 post filtering state isn't restored properly - need to do filtering AND maintain filtered page/position AND recreate the backstack/'go back' UI
			Timber.i("Restoring fragment - loading cached posts from database");
			setThreadId(aSavedState.getInt(THREAD_ID_KEY, NULL_THREAD_ID));
			setPageNumber(aSavedState.getInt(THREAD_PAGE_KEY, FIRST_PAGE));
			// TODO: 04/05/2017 saved scroll position doesn't seem to actually get used to set the position?
			savedScrollPosition = aSavedState.getInt(SCROLL_POSITION_KEY, 0);
			loadFromCache = true;
		}
		// no valid thread ID means do nothing I guess? If the intent that created the activity+fragments didn't request a thread
		if (getThreadId() <= 0) {
			return;
		}
		// if we recreated the fragment (and had a valid thread ID) we just want to load the cached page data,
		// so we get the same state as before (we don't want to reload the page and e.g. have all the posts marked as seen)
		if(loadFromCache) {
			refreshPosts();
			refreshInfo();
		} else {
			syncThread();
		}
		updateUiElements();
	}


	/**
	 * Check if an InflateException is caused by a missing WebView.
	 * <p>
	 * Also displays a message for the user.
	 *
	 * @param e the exception thrown when inflating the layout
	 * @return true if the WebView is missing
	 */
	private boolean webViewIsMissing(InflateException e) {
		String message = e.getMessage();
		//noinspection SpellCheckingInspection
		if (message == null || !message.toLowerCase().contains("webview")) {
			return false;
		}
		Timber.w("Can't inflate thread view, WebView package is updating?:\n");
		e.printStackTrace();
		getAlertView()
				.setIcon(R.drawable.ic_error)
				.setTitle(R.string.web_view_missing_alert_title)
				.setSubtitle(R.string.web_view_missing_alert_message)
				.show();
		return true;
	}



	private WebViewClient threadWebViewClient = new WebViewClient() {

		@Override
		public boolean shouldOverrideUrlLoading(WebView aView, String aUrl) {
			AwfulURL aLink = AwfulURL.parse(aUrl);
			switch (aLink.getType()) {
				case FORUM:
					navigate(new NavigationEvent.Forum((int) aLink.getId(), (int) aLink.getPage()));
					break;
				case THREAD:
					if (aLink.isRedirect()) {
						startPostRedirect(aLink.getURL(getPrefs().postPerPage));
					} else {
						pushThread((int) aLink.getId(), (int) aLink.getPage(), aLink.getFragment().replaceAll("\\D", ""));
					}
					break;
				case POST:
					startPostRedirect(aLink.getURL(getPrefs().postPerPage));
					break;
				case EXTERNAL:
					if (getPrefs().alwaysOpenUrls) {
						startUrlIntent(aUrl);
					} else {
						showUrlMenu(aUrl);
					}
					break;
				case INDEX:
					navigate(NavigationEvent.ForumIndex.INSTANCE);
					break;
			}
			return true;
		}
	};


	private void initThreadViewProperties() {
		if (mThreadView == null) {
			Timber.w("initThreadViewProperties called for null WebView");
			return;
		}
		mThreadView.setWebViewClient(threadWebViewClient);
		mThreadView.setWebChromeClient(new LoggingWebChromeClient(mThreadView) {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    super.onProgressChanged(view, newProgress);
                    setProgress(newProgress / 2 + 50);//second half of progress bar
                }
        });
        mThreadView.setJavascriptHandler(clickInterface);

        refreshSessionCookie();
		Timber.d("Setting up WebView container HTML");
		mThreadView.setContent(getBlankPage());
		mThreadView.setKeepScreenOn(keepScreenOn);

		mThreadView.setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength)  {
				enqueueDownload(Uri.parse(url));
			}
		});
	}

	private void updatePageBar() {
		if(pageBar != null){
			pageBar.updatePagePosition(getPageNumber(), getLastPage());
		}
		if (getActivity() != null) {
			invalidateOptionsMenu();
		}
		if (mThreadView != null) {
			getSwipyLayout().setOnRefreshListener(getPrefs().disablePullNext ? null : this);
		}
	}


    @Override
    public void onResume() {
        super.onResume();
		if(mThreadView != null){
			mThreadView.onResume();
		}
        getActivity().getContentResolver().registerContentObserver(AwfulThread.CONTENT_URI, true, mThreadObserver);
        refreshInfo();
    }

    
	@Override
	public void setAsFocusedPage() {
        if(mThreadView != null){
			mThreadView.onResume();
        	mThreadView.setKeepScreenOn(keepScreenOn);
        }
	}

	@Override
	public void setAsBackgroundPage() {
        if(mThreadView != null){
        	mThreadView.setKeepScreenOn(false);
			mThreadView.onPause();
		}
	}

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getContentResolver().unregisterContentObserver(mThreadObserver);
        getLoaderManager().destroyLoader(Constants.THREAD_INFO_LOADER_ID);
		if (mThreadView != null) {
			mThreadView.onPause();
		}
	}

	@Override
	protected void cancelNetworkRequests() {
		super.cancelNetworkRequests();
		NetworkUtils.cancelRequests(ThreadPageRequest.Companion.getREQUEST_TAG());
	}


    @Override
    public void onDestroy() {
        super.onDestroy();
        getLoaderManager().destroyLoader(Constants.POST_LOADER_ID);
    }

    // TODO: fix deprecated warnings
    private synchronized void refreshSessionCookie(){
        if(mThreadView != null){
        	CookieSyncManager.createInstance(getActivity());
        	CookieManager cookieMonster = CookieManager.getInstance();
        	cookieMonster.removeAllCookie();
        	cookieMonster.setCookie(Constants.COOKIE_DOMAIN, CookieController.getCookieString(Constants.COOKIE_NAME_SESSIONID));
        	cookieMonster.setCookie(Constants.COOKIE_DOMAIN, CookieController.getCookieString(Constants.COOKIE_NAME_SESSIONHASH));
        	cookieMonster.setCookie(Constants.COOKIE_DOMAIN, CookieController.getCookieString(Constants.COOKIE_NAME_USERID));
        	cookieMonster.setCookie(Constants.COOKIE_DOMAIN, CookieController.getCookieString(Constants.COOKIE_NAME_PASSWORD));
        	cookieMonster.setAcceptThirdPartyCookies(mThreadView, true);
        	CookieSyncManager.getInstance().sync();
        }
    }
 
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	menu.clear();
    	if(menu.size() == 0){
    		inflater.inflate(R.menu.post_menu, menu);
        	MenuItem share = menu.findItem(R.id.share_thread);
        	if(share != null && MenuItemCompat.getActionProvider(share) instanceof ShareActionProvider){
        		shareProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(share);
        		shareProvider.setShareIntent(createShareIntent(null));
        	}
    	}
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if(menu == null || getActivity() == null){
            return;
        }
		MenuItem lockUnlock = menu.findItem(R.id.lock_unlock);
		if(lockUnlock != null){
			lockUnlock.setVisible(threadLockableUnlockable);
			lockUnlock.setTitle((threadLocked ?getString(R.string.thread_unlock):getString(R.string.thread_lock)));
		}
		MenuItem find = menu.findItem(R.id.find);
		if(find != null){
			find.setVisible(true);
		}
		MenuItem reply = menu.findItem(R.id.reply);
		if(reply != null){
			reply.setVisible(getPrefs().noFAB);
		}
        MenuItem bk = menu.findItem(R.id.bookmark);
        if(bk != null){
            if(threadArchived){
                bk.setTitle(getString(R.string.bookmarkarchived));
            }else{
                bk.setTitle((threadBookmarked? getString(R.string.unbookmark):getString(R.string.bookmark)));
            }
            bk.setEnabled(!threadArchived);
        }
		MenuItem screen = menu.findItem(R.id.keep_screen_on);
		if(screen != null){
			screen.setChecked(keepScreenOn);
		}
		MenuItem yospos = menu.findItem(R.id.yospos);
		if(yospos != null){
			yospos.setVisible(mParentForumId == Constants.FORUM_ID_YOSPOS);
		}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
			case R.id.lock_unlock:
				showThreadLockUnlockDialog();
				break;
			case R.id.reply:
				displayPostReplyDialog();
				break;
            case R.id.next_page:
            	turnPage(true);
                break;
    		case R.id.rate_thread:
    			rateThread();
    			break;
    		case R.id.copy_url:
    			copyThreadURL(null, postFilterUserId);
    			break;
    		case R.id.find:
				((WebViewSearchBar) item.getActionView()).setWebView(mThreadView);
				break;
    		case R.id.keep_screen_on:
    			this.toggleScreenOn();
                item.setChecked(!item.isChecked());
    			break;
            case R.id.bookmark:
                toggleThreadBookmark();
                break;
			case R.id.yospos:
				toggleYospos();
				break;
			case R.id.show_self:
				showUsersPosts(getPrefs().userId, getPrefs().username);
				break;
			case R.id.search_this_thread:
				SearchFilter threadFilter = new SearchFilter(SearchFilter.FilterType.ThreadId, Integer.toString(currentThreadId));
				navigate(new NavigationEvent.SearchForums(threadFilter));
				return true;
    		default:
    			return super.onOptionsItemSelected(item);
    		}

    		return true;
    	}


	/**
	 * Get a URL that links to a particular thread.
	 *
	 * @param postId An optional post ID, appended as the URL's fragment
	 * @param userId An optional user ID, appended as a query parameter
	 * @return the full URL
	 */
	@NonNull
	private String generateThreadUrl(@Nullable Integer postId, @Nullable Integer userId) {
		Uri.Builder builder = Uri.parse(Constants.FUNCTION_THREAD).buildUpon()
				.appendQueryParameter(Constants.PARAM_THREAD_ID, String.valueOf(getThreadId()))
				.appendQueryParameter(Constants.PARAM_PAGE, String.valueOf(getPageNumber()))
				.appendQueryParameter(Constants.PARAM_PER_PAGE, String.valueOf(getPrefs().postPerPage));
		if (userId != null) {
			builder.appendQueryParameter(Constants.PARAM_USER_ID, String.valueOf(userId));
		}
		if (postId != null) {
			builder.fragment("post" + postId);
		}
		return builder.toString();
	}


	/**
	 * Get a URL that links to a particular post.
	 *
	 * @param postId The ID of the post to link to
	 * @return the full URL
	 */
	@NonNull
	private String generatePostUrl(int postId) {
		return Uri.parse(Constants.FUNCTION_THREAD).buildUpon()
				.appendQueryParameter(Constants.PARAM_GOTO, Constants.VALUE_POST)
				.appendQueryParameter(Constants.PARAM_POST_ID, Integer.toString(postId))
				.toString();
	}


	/**
	 * Get a share intent for a url.
	 * <p/>
	 * If url is null, a link to the current thread will be generated.
	 *
	 * @param url The url to share
	 */
	@NonNull
	public Intent createShareIntent(@Nullable String url) {
		Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain");
		if (url == null) {
			// we're sharing the current thread - we can add the title in here
			intent.putExtra(Intent.EXTRA_SUBJECT, mTitle);
			url = generateThreadUrl(null, postFilterUserId);
		}
		return intent.putExtra(Intent.EXTRA_TEXT, url);
	}



	/**
	 * Copy a thread's URL to the clipboard
	 * @param postId    An optional post ID, used as the url's fragment
	 * @param userId    An optional user ID, appended to the url as a parameter
	 */
	public void copyThreadURL(@Nullable Integer postId, @Nullable Integer userId) {
		String clipLabel = getString(R.string.copy_url) + getPageNumber();
		String clipText  = generateThreadUrl(postId, userId);
		safeCopyToClipboard(clipLabel, clipText, R.string.copy_url_success);
	}


	/**
	 * Display a thread-rating dialog.
	 *
	 * This handles the network request to submit the vote, and user feedback.
	 */
	private void rateThread() {
		final CharSequence[] items = {"1", "2", "3", "4", "5"};
		final Activity activity = this.getActivity();

		new AlertDialog.Builder(activity)
				.setTitle("Rate this thread")
				.setItems(items, (dialog, item) -> queueRequest(new VoteRequest(activity, getThreadId(), item+1)
                        .build(ThreadDisplayFragment.this, new AwfulRequest.AwfulResultCallback<Void>() {
                            @Override
                            public void success(Void result) {
                                getAlertView().setTitle(R.string.vote_succeeded)
                                        .setSubtitle(R.string.vote_succeeded_sub)
                                        .setIcon(R.drawable.ic_mood)
                                        .show();
                            }


                            @Override
                            public void failure(VolleyError error) {
                            }
                        }))).show();
	}


	/**
	 * Add a user to the ignore list.
	 *
	 * @param userId The awful ID of the user
	 */
	public void ignoreUser(int userId) {
		final Activity activity = getActivity();
		if (getPrefs().ignoreFormkey == null) {
			queueRequest(new RefreshUserProfileRequest(activity).build());
		}
		if (getPrefs().showIgnoreWarning) {

			DialogInterface.OnClickListener onClickListener = (dialog, which) -> {
                if (which == AlertDialog.BUTTON_NEUTRAL) {
                    // cancel future alerts if the user clicks the "don't warn" option
                    getPrefs().setPreference(Keys.SHOW_IGNORE_WARNING, false);
                }
                doIgnoreUser(activity, userId);
            };

			new AlertDialog.Builder(activity)
			.setPositiveButton(R.string.confirm, onClickListener)
			.setNeutralButton(R.string.dont_show_again, onClickListener)
			.setNegativeButton(R.string.cancel, null)
			.setTitle(R.string.ignore_title)
			.setMessage(R.string.ignore_message)
			.show();
		} else {
			doIgnoreUser(activity, userId);
		}
	}


	/**
	 * Carry out the ignore user request
     */
	private void doIgnoreUser(@NonNull Context context, int userId) {
		//we don't care about status callbacks for this, so we use the build() that doesn't do callbacks
		queueRequest(new IgnoreRequest(context, userId).build());
	}


    /**
	 * Toggle a user as marked or unmarked.
     */
	public void toggleMarkUser(String username){
        if(getPrefs().markedUsers.contains(username)){
            getPrefs().unmarkUser(username);
        }else{
            getPrefs().markUser(username);
        }
	}


	/**
	 * Toggle between displaying a single user's posts, or all posts
	 * @param aPostId	The ID of the post to display, if toggling filtering off
	 * @param aUserId	The ID of the user whose posts we're showing, if toggling on
	 * @param aUsername	The username of the user, if toggling on
     */
	// TODO: refactor this and the methods it calls - it's so weird
	public void toggleUserPosts(int aPostId, int aUserId, String aUsername){
		if(postFilterUserId != null){
			showAllPosts(aPostId);
		}else{
			showUsersPosts(aUserId, aUsername);
		}
	}


	/**
	 * Display a dialog to report a post
	 *
	 * @param postId	The ID of the bad post
     */
	public void reportUser(int postId){
		final EditText reportReason = new EditText(this.getActivity());

		new AlertDialog.Builder(this.getActivity())
		  .setTitle("Report inappropriate post")
		  .setMessage("Did this post break the forum rules? If so, please report it by clicking below. If you would like to add any comments explaining why you submitted this post, please do so here:")
		  .setView(reportReason)
		  .setPositiveButton("Report", (dialog, whichButton) -> {
            String reason = reportReason.getText().toString();
            queueRequest(new ReportRequest(getActivity(), postId, reason).build(ThreadDisplayFragment.this, new AwfulRequest.AwfulResultCallback<String>() {
                @Override
                public void success(String result) {
                    getAlertView().setTitle(result).setIcon(R.drawable.ic_mood).show();
                }

                @Override
                public void failure(VolleyError error) {
                    getAlertView().setTitle(error.getMessage()).setIcon(R.drawable.ic_mood).show();

                }
            }));
          })
		  .setNegativeButton(R.string.cancel, null)
		  .show();
	}
	
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState){
    	super.onSaveInstanceState(outState);
    	Timber.d("onSaveInstanceState - storing thread ID, page number and scroll position");
        outState.putInt(THREAD_ID_KEY, getThreadId());
        outState.putInt(THREAD_PAGE_KEY, getPageNumber());
    	if(mThreadView != null){
    		outState.putInt(SCROLL_POSITION_KEY, mThreadView.getScrollY());
    	}
    	outState.putBoolean(KEEP_SCREEN_ON_KEY, keepScreenOn);
    }


    /**
	 * Reload the current thread page
	 */
    private void syncThread() {
		final Activity activity = getActivity();
        if (activity != null) {
			Timber.i("Syncing - reloading from site (thread %d, page %d) to update DB", getThreadId(), getPageNumber());
			// cancel pending post loading requests
			NetworkUtils.cancelRequests(ThreadPageRequest.Companion.getREQUEST_TAG());
			// call this with cancelOnDestroy=false to retain the request's specific type tag
			final int pageNumber = getPageNumber();
			int userId = postFilterUserId == null ? BLANK_USER_ID : postFilterUserId;
			queueRequest(new ThreadPageRequest(activity, getThreadId(), pageNumber, userId)
					.build(this, new AwfulRequest.AwfulResultCallback<Void>() {
				@Override
				public void success(Void result) {
					refreshInfo();
					setProgress(75);
					refreshPosts();
				}

				@Override
				public void failure(VolleyError error) {
					Timber.w("Failed to sync thread! Error: %s", error.getMessage());
					refreshInfo();
					refreshPosts();
				}
			}), false);
        }
    }


	/**
	 * Mark a post as the last read in this thread.
	 * <p/>
	 * This takes an attribute in the HTML called <code>data-idx</code>, which is basically
	 * an enumeration of the posts in the thread.
	 *
	 * @param index The <code>data-idx</code> value of the post.
	 */
	public void markLastRead(int index) {
		getAlertView().setTitle(R.string.mark_last_read_progress)
				.setSubtitle(R.string.please_wait_subtext)
				.setIcon(R.drawable.ic_visibility)
				.show();

		queueRequest(new MarkLastReadRequest(getActivity(), getThreadId(), index)
				.build(null, new AwfulRequest.AwfulResultCallback<Void>() {
			@Override
			public void success(Void result) {
				if(getActivity() != null){
					getAlertView().setTitle(R.string.mark_last_read_success)
							.setIcon(R.drawable.ic_visibility)
							.show();
					refreshInfo();
					refreshPosts();
				}
			}


			@Override
			public void failure(VolleyError error) {

			}
		}));
	}


	/**
	 * Toggle this thread's bookmarked status.
	 */
	private void toggleThreadBookmark() {
		Activity activity = getActivity();
		if(activity != null){
			queueRequest(new BookmarkRequest(activity, getThreadId(), !threadBookmarked)
					.build(this, new AwfulRequest.AwfulResultCallback<Void>() {
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
	}


	/**
	 * Toggle between amberPOS and greenPOS, refreshing the display.
	 */
	private void toggleYospos() {
		getPrefs().amberDefaultPos = !getPrefs().amberDefaultPos;
		getPrefs().setPreference(Keys.AMBER_DEFAULT_POS, getPrefs().amberDefaultPos);
		if (mThreadView != null) {
			mThreadView.runJavascript(String.format("changeCSS('%s')", AwfulTheme.forForum(mParentForumId).getCssPath()));
		}
	}


    /**
	 * Reload with a new URL
	 * @param postUrl	The URL of the post we should land on
     */
	private void startPostRedirect(final String postUrl) {
		final AwfulActivity activity = getAwfulActivity();
		if (activity == null) {
			return;
		}
		if (redirect != null) {
            redirect.cancel(false);
        }
		setProgress(50);
		redirect = new RedirectTask(postUrl) {
            @Override
            protected void onPostExecute(String url) {
                if (isCancelled()) {
                    return;
                } else if (url == null) {
                    getAlertView().show(new AwfulError());
                    return;
                }

                AwfulURL result = AwfulURL.parse(url);
                if (postUrl.contains(Constants.VALUE_LASTPOST)) {
                    //This is a workaround for how the forums handle the perPage value with goto=lastpost.
                    //The redirected url is lacking the perpage=XX value.
                    //We just override the assumed (40) with the number we requested when starting the redirect.
                    //I gotta ask chooch to fix this at some point.
                    result.setPerPage(getPrefs().postPerPage);
                }
                if (result.getType() == TYPE.THREAD) {
					int threadId = (int) result.getId();
					int threadPage = (int) result.getPage(getPrefs().postPerPage);
					String postJump = result.getFragment();
					if (bypassBackStack) {
                        openThread(threadId, threadPage, postJump);
                    } else {
                        pushThread(threadId, threadPage, postJump);
                    }
                } else if (result.getType() == TYPE.INDEX) {
                    activity.navigate(NavigationEvent.ForumIndex.INSTANCE);
                }
                redirect = null;
                bypassBackStack = false;
                setProgress(100);
            }
        }.execute();
	}


	/**
	 * Show the page picker dialog, and handle user input and navigation.
	 */
	private void displayPagePicker() {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}

		new PagePicker(activity, getLastPage(), getPageNumber(), (button, resultValue) -> {
            if (button == DialogInterface.BUTTON_POSITIVE) {
                goToPage(resultValue);
            }
        }).show();
	}


	@Override
    public void onActivityResult(int aRequestCode, int aResultCode, Intent aData) {
    	Timber.d("onActivityResult - request code: %d, result: %d", aRequestCode, aResultCode);
        // If we're here because of a post result, refresh the thread
        switch (aRequestCode) {
            case PostReplyFragment.REQUEST_POST:
            		bypassBackStack = true;
            	if(aResultCode == PostReplyFragment.RESULT_POSTED){
            		startPostRedirect(AwfulURL.threadLastPage(getThreadId(), getPrefs().postPerPage).getURL(getPrefs().postPerPage));
            	}else if(aResultCode > 100){//any result >100 it is a post id we edited
					// TODO: >100 is a bit too magical
            		startPostRedirect(AwfulURL.post(aResultCode, getPrefs().postPerPage).getURL(getPrefs().postPerPage));
            	}
                break;
        }
    }


	/**
	 * Refresh the page
	 */
    private void refresh() {
		showBlankPage();
        syncThread();
    }


	/**
	 * Load the next or previous page.
	 *
	 * The current page will reload if there is no next/previous page to move to.
	 */
    private void turnPage(boolean forwards) {
		int currentPage = getPageNumber();
		int limit = forwards ? getLastPage() : FIRST_PAGE;
		if (currentPage == limit) {
            refresh();
        } else {
            goToPage(currentPage + (forwards ? 1 : -1));
        }
    }


	/**
	 * General click listener for thread view widgets
	 */
    private final View.OnClickListener onButtonClick = aView -> {
        switch (aView.getId()) {
            case R.id.just_post:
                displayPostReplyDialog();
                break;
        }
    };

    private void displayPostReplyDialog() {
        displayPostReplyDialog(getThreadId(), -1, AwfulMessage.TYPE_NEW_REPLY);
    }


	/**
	 * Show a dialog that allows the user to lock or unlock the current thread, as appropriate.
	 */
	private void showThreadLockUnlockDialog() {
    	new AlertDialog.Builder(getActivity())
				.setTitle(getString(threadLocked ? R.string.thread_unlock : R.string.thread_lock) + "?")
				.setPositiveButton(R.string.alert_ok, (dialogInterface, i) -> toggleThreadLock())
				.setNegativeButton(R.string.cancel, null)
				.show();
	}


	/**
	 * Trigger a request to toggle the current thread's locked/unlocked state.
	 */
	private void toggleThreadLock(){
		queueRequest(new ThreadLockUnlockRequest(getActivity(), getThreadId()).build(mSelf, new AwfulRequest.AwfulResultCallback<Void>() {
			@Override
			public void success(Void result) {
			    // TODO: maybe this should trigger a thread data refresh instead, update everything from the source
				threadLocked = !threadLocked;
			}

			@Override
			public void failure(VolleyError error) {
				Timber.e(String.format("Couldn\'t %s this thread", threadLocked ? "unlock" : "lock"));
			}
		}));
	}

	private void populateThreadView(ArrayList<AwfulPost> aPosts) {
		if (mThreadView == null) {
			Timber.w("populateThreadView called with null WebView");
			return;
		}
		updateUiElements();

        try {
            Timber.d("populateThreadView: displaying %d posts", aPosts.size());
            String html = AwfulHtmlPage.getThreadHtml(aPosts, AwfulPreferences.getInstance(getActivity()), getPageNumber(), mLastPage);
            refreshSessionCookie();
			mThreadView.setBodyHtml(html);
			displayingFullPage = aPosts.size() >= getPrefs().postPerPage; // shouldn't ever be > but just to be safe
            setProgress(100);
        } catch (Exception e) {
            // If we've already left the activity the webview may still be working to populate,
            // just log it
            Timber.e(e, "populateThreadView: display failed");
        }

    }
    
	@Override
	public void onRefresh(SwipyRefreshLayoutDirection swipyRefreshLayoutDirection) {
		if (swipyRefreshLayoutDirection == SwipyRefreshLayoutDirection.TOP) {
			// no page turn when swiping at the top of the page
			refresh();
		} else if (!displayingFullPage) {
			// always refresh if there could be more posts
			refresh();
		} else {
			turnPage(true);
		}
	}

	private final ClickInterface clickInterface = new ClickInterface();

	public String getPostJump() {
		return postJump;
	}

	private void setPostJump(@NonNull String postJump) {
		// TODO: this strips out any prefix (so it handles prefixed fragments AND bare IDs) and adds the required prefix to all. Might be better to handle this in AwfulURL?
		this.postJump = "post" + postJump.replaceAll("\\D", "");
	}

	private class ClickInterface extends WebViewJsInterface {

        @JavascriptInterface
        public void onMoreClick(final String aPostId, final String aUsername, final String aUserId, final String lastReadUrl, final boolean editable, final boolean isAdminOrMod, final boolean isPlat) {
			PostContextMenu postActions = PostContextMenu.newInstance(getThreadId(), Integer.parseInt(aPostId),
					Integer.parseInt(lastReadUrl), editable, aUsername, Integer.parseInt(aUserId), isPlat, isAdminOrMod, postFilterUserId);
			postActions.setTargetFragment(ThreadDisplayFragment.this, -1);
			postActions.show(mSelf.getFragmentManager(), "Post Actions");
		}


		@Override
		protected void setCustomPreferences(Map<String, String> preferences) {
			// TODO: 23/01/2017 add methods so you can't mess with the map directly
			preferences.put("postjumpid", postJump);
			preferences.put("scrollPosition", Integer.toString(savedScrollPosition));
		}

		@JavascriptInterface
		public String getIgnorePostHtml(String id){
			return ignorePostsHtml.get(id);
		}

        @JavascriptInterface
        public String getPostJump(){
            return postJump;
        }

        @JavascriptInterface
        public String getCSS(){
            return AwfulTheme.forForum(mParentForumId).getCssPath();
        }


		@JavascriptInterface
		public void loadIgnoredPost(final String ignorePost){
			if(getActivity() != null){
				queueRequest(new SinglePostRequest(getActivity(), ignorePost).build(mSelf, new AwfulRequest.AwfulResultCallback<String>() {
					@Override
					public void success(String result) {
						ignorePostsHtml.put(ignorePost,result);
						if (mThreadView != null) {
							mThreadView.runJavascript(String.format("insertIgnoredPost('%s')", ignorePost));
						}
					}

					@Override
					public void failure(VolleyError error) {
						Timber.w("Failed to load ignored post #" + ignorePost);
					}
				}));
			}
		}

		@JavascriptInterface
		public void haltSwipe() {
			((ForumsIndexActivity)mSelf.getAwfulActivity()).preventSwipe();
		}
		@JavascriptInterface
		public void resumeSwipe() {
			((ForumsIndexActivity)mSelf.getAwfulActivity()).allowSwipe();
		}

		@JavascriptInterface
		public void popupText(String text) {
			Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
		}

		@JavascriptInterface
		public void openUrlMenu(String url) {
			showUrlMenu(url);
		}
    }

	
	private void showUrlMenu(final String url) {
		if (url == null) {
			Timber.w("Passed null URL to #showUrlMenu!");
			return;
		}
		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager == null) {
			Timber.w("showUrlMenu called but can't get FragmentManager!");
			return;
		}
		if (fragmentManager.isStateSaved()) {
			// probably got a javascript callback after the fragment was stopped,
			// easiest to just let them tap for the menu again when they come back
			return;
		}

		boolean isImage = false;
		boolean isGif = false;
		// TODO: parsing fails on magic webdev urls like http://tpm2016.zoffix.com/#/40
		// it thinks the # is the start of the ref section of the url, so the Path for that url is '/'
		Uri path = Uri.parse(url);
		String lastSegment = path.getLastPathSegment();
		// null-safe path checking (there may be no path segments, e.g. a link to a domain name)
		if (lastSegment != null) {
			lastSegment = lastSegment.toLowerCase();
			// using 'contains' instead of 'ends with' in case of any url suffix shenanigans, like twitter's ".jpg:large"
            // TODO: 08/08/2019 make general functions for identifying images etc since we need to do this in multiple places
			isImage = (StringUtils.indexOfAny(lastSegment, ".jpg", ".jpeg", ".png", ".gif", ".webp") != -1
					&& !StringUtils.contains(lastSegment, ".gifv"))
					|| (lastSegment.equals("attachment.php") && path.getHost().equals("forums.somethingawful.com"));
			isGif = StringUtils.contains(lastSegment, ".gif")
					&& !StringUtils.contains(lastSegment, ".gifv");
		}

		UrlContextMenu linkActions = UrlContextMenu.newInstance(url, isImage, isGif, isGif ? "Getting file size" : null);

		if (isGif || !AwfulPreferences.getInstance().canLoadImages()) {
			queueRequest(new ImageSizeRequest(url, result -> {
				if (linkActions == null) {
					return;
				}
				String size = result == null ? "unknown" : Formatter.formatShortFileSize(getContext(), result);
				linkActions.setSubheading(String.format("Size: %s", size));
			}));
		}
		linkActions.setTargetFragment(ThreadDisplayFragment.this, -1);
		linkActions.show(fragmentManager, "Link Actions");
	}

	public void showImageInline(String url){
		if(mThreadView != null){
			mThreadView.runJavascript(String.format("showInlineImage('%s')", url));
		}
	}

	public void enqueueDownload(Uri link) {
		if(AwfulUtils.isMarshmallow()){
			int permissionCheck = ContextCompat.checkSelfPermission(this.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
			if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
				downloadLink = link;
				requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.AWFUL_PERMISSION_WRITE_EXTERNAL_STORAGE);
				return;
			}
		}
		Request request = new Request(link);
		request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		if(link.getLastPathSegment().equals("attachment.php") && link.getHost().equals("forums.somethingawful.com")){
			request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "attachment.png");
			request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(Constants.COOKIE_DOMAIN));
		}else{
			request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, link.getLastPathSegment());
		}
		request.allowScanningByMediaScanner();
		DownloadManager dlManager = (DownloadManager) getAwfulActivity().getSystemService(AwfulActivity.DOWNLOAD_SERVICE);
		dlManager.enqueue(request);
	}

	public void copyToClipboard(String text){
		safeCopyToClipboard("Copied URL", text, null);
		getAlertView()
				.setTitle(R.string.copy_url_success)
				.setIcon(R.drawable.ic_insert_link)
				.show();
	}

	public void startUrlIntent(String url){
		Uri intentUri = Uri.parse(url);
		try {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, intentUri);
			browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getActivity().startActivity(browserIntent);
		} catch (ActivityNotFoundException error) {
			getAlertView().setTitle("Cannot open link:")
					.setSubtitle("None of your apps want to open this " + intentUri.getScheme() + ":\\\\ link. Try installing an app that is less picky")
					.show();
		}

	}

	public void displayImage(String url){
		Intent intent = BasicActivity.Companion.intentFor(ImageViewFragment.class, getActivity(), "");
		intent.putExtra(ImageViewFragment.EXTRA_IMAGE_URL, url);
		startActivity(intent);
	}
	
	@Override
	public void onPreferenceChange(AwfulPreferences mPrefs, String key) {
		super.onPreferenceChange(mPrefs, key);
		Timber.i("onPreferenceChange" + ((key != null) ? ":"  +key  :""));
        if(null != getAwfulActivity() && pageBar != null){
		    getAwfulActivity().setPreferredFont(pageBar.getTextView());
			pageBar.setTextColour(ColorProvider.ACTION_BAR_TEXT.getColor());
		}

		if(mThreadView != null){
			mThreadView.setBackgroundColor(Color.TRANSPARENT);
            mThreadView.runJavascript(String.format("changeFontFace('%s')", mPrefs.preferredFont));
            mThreadView.getSettings().setDefaultFontSize(mPrefs.postFontSizeSp);
            mThreadView.getSettings().setDefaultFixedFontSize(mPrefs.postFixedFontSizeSp);

			if("marked_users".equals(key)){
				mThreadView.runJavascript(String.format("updateMarkedUsers('%s')", TextUtils.join(",", mPrefs.markedUsers)));
			}
		}
		clickInterface.updatePreferences();
		if(mFAB != null) {
			if (mPrefs.noFAB) {
				mFAB.hide();
			} else {
				mFAB.show();
			}
		}
	}


	/**
	 * Update any UI elements that need to be refreshed.
	 */
	private void updateUiElements() {
		// TODO: probably more things can be put in here, there's a lot to unravel
		updatePageBar();
		refreshProbationBar();
	}


	/**
	 * Load a specific page in the current thread.
	 *
	 * This method does nothing if the page number is not valid (i.e. between {@link #FIRST_PAGE} and the last page).
	 * @param aPage	a page number for this thread
     */
	private void goToPage(int aPage){
		if (aPage <= 0 || aPage > getLastPage()) {
			return;
		}
		setPageNumber(aPage);
		updateUiElements();
		setPostJump("");
		showBlankPage();
		syncThread();
	}


	/**
	 * Get an empty page structure, themed according to the thread's parent forum
	 * @return	The basic page HTML, with no post content
     */
	private String getBlankPage(){
		return AwfulHtmlPage.getContainerHtml(getPrefs(), getParentForumId(), true);
	}

    private int getLastPage() {
        return mLastPage;
    }

    public int getThreadId() {
        return currentThreadId;
    }
	
	public int getPageNumber() {
		return currentPage;
	}
	private void setPageNumber(int aPage){
		currentPage = aPage;
	}
	private void setThreadId(int aThreadId){
		currentThreadId = aThreadId;
		if (getActivity() != null) {
			((ForumsIndexActivity) getActivity()).onPageContentChanged();
		}
	}


	/**
	 * Show posts filtered to a specific user
	 * @param id	the user's ID
	 * @param name	the user's username
     */
	private void showUsersPosts(int id, String name){
		// TODO: legend has it this doesn't work and shows other people's posts if the page isn't full
		pageBeforeFiltering = getPageNumber();
		setPostFiltering(id, name);
		setPageNumber(FIRST_PAGE);
		mLastPage = FIRST_PAGE;
		setPostJump("");
        refresh();
	}


	/**
	 * Clear filtering added by {@link #showUsersPosts(int, String)} and return to a specific post
	 * @param postId	The ID of the post to navigate to
     */
	private void showAllPosts(@Nullable Integer postId){
		if (postId != null) {
			showBlankPage();
	        openThread(AwfulURL.parse(generatePostUrl(postId)));
		} else {
			setPostFiltering(null, null);
			setPageNumber(pageBeforeFiltering);
			mLastPage = 0;
			setPostJump("");
			refresh();
		}
	}


	/**
	 * Set or unset the "show user's posts" filtering state.
	 * @param userId	The ID of the user to filter to, or null for no filtering
	 * @param username	The username of the user. If ID is null, this is ignored
     */
	private void setPostFiltering(@Nullable Integer userId, @Nullable String username) {
		postFilterUserId = userId;
		postFilterUsername = (userId == null) ? null : username;
	}


	/**
	 * Clear the thread display, e.g. to show a blank page before loading new content
	 */
	private void showBlankPage() {
		if(mThreadView != null){
			mThreadView.setBodyHtml(null);
		}
	}


    private class PostLoaderManager implements LoaderManager.LoaderCallbacks<Cursor> {
        private final static String sortOrder = AwfulPost.POST_INDEX + " ASC";
        private final static String selection = AwfulPost.THREAD_ID + "=? AND " + AwfulPost.POST_INDEX + ">=? AND " + AwfulPost.POST_INDEX + "<?";
        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            int index = AwfulPagedItem.pageToIndex(getPageNumber(), getPrefs().postPerPage, 0);
            Timber.i("Loading page %d of thread %d from database\nStart index is %d with %d posts per page",
                    getPageNumber(), getThreadId(), index, getPrefs().postPerPage);
            return new CursorLoader(getActivity(),
            						AwfulPost.CONTENT_URI,
            						AwfulProvider.PostProjection,
            						selection,
            						AwfulProvider.int2StrArray(getThreadId(), index, index+ getPrefs().postPerPage),
            						sortOrder);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	setProgress(90);
        	if(aData.isClosed()){
        		return;
        	}
        	if(mThreadView != null){
        		populateThreadView(AwfulPost.fromCursor(getActivity(), aData));
        	}
			// TODO: 04/05/2017 sometimes you don't want this resetting, e.g. restoring fragment state
			savedScrollPosition = 0;
        }

        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        }
    }


    private class ThreadDataCallback implements LoaderManager.LoaderCallbacks<Cursor> {

        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            return new CursorLoader(getActivity(), ContentUris.withAppendedId(AwfulThread.CONTENT_URI, getThreadId()), 
            		AwfulProvider.ThreadProjection, null, null, null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Timber.i("Loaded thread metadata, updating fragment state and UI");
        	if(aData.getCount() >0 && aData.moveToFirst()){
        		mLastPage = AwfulPagedItem.indexToPage(aData.getInt(aData.getColumnIndex(AwfulThread.POSTCOUNT)), getPrefs().postPerPage);
				threadLocked = aData.getInt(aData.getColumnIndex(AwfulThread.LOCKED))>0;
				threadLockableUnlockable = aData.getInt(aData.getColumnIndex(AwfulThread.CAN_OPEN_CLOSE))>0;
        		threadBookmarked = aData.getInt(aData.getColumnIndex(AwfulThread.BOOKMARKED))>0;
				threadArchived = aData.getInt(aData.getColumnIndex(AwfulThread.ARCHIVED))>0;
				mTitle = aData.getString(aData.getColumnIndex(AwfulThread.TITLE));
        		mParentForumId = aData.getInt(aData.getColumnIndex(AwfulThread.FORUM_ID));
				if(mParentForumId != 0 && mThreadView != null){
					mThreadView.runJavascript(String.format("changeCSS('%s')", AwfulTheme.forForum(mParentForumId).getCssPath()));
				}

				parentActivity.onPageContentChanged();

				updateUiElements();
				if (mUserPostNotice != null) {
					if (postFilterUserId != null) {
						mUserPostNotice.setVisibility(View.VISIBLE);
						mUserPostNotice.setText(String.format("Viewing posts by %s in this thread,\nPress the back button to return.", postFilterUsername));
						mUserPostNotice.setTextColor(ColorProvider.PRIMARY_TEXT.getColor());
						mUserPostNotice.setBackgroundColor(ColorProvider.BACKGROUND.getColor());
					} else {
						mUserPostNotice.setVisibility(View.GONE);
					}
				}
        		if(shareProvider != null){
        			shareProvider.setShareIntent(createShareIntent(null));
        		}
                invalidateOptionsMenu();
				if (mFAB != null) {
					if (getPrefs().noFAB || threadLocked || threadArchived) {
						mFAB.hide();
					} else {
						mFAB.show();
					}
				}
        	}
        }
        
        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        }
    }
    private class ThreadContentObserver extends ContentObserver {
        public ThreadContentObserver(Handler aHandler) {
            super(aHandler);
        }
        @Override
        public void onChange (boolean selfChange){
        	Timber.i("Thread metadata has been updated - forcing refresh");
        	refreshInfo();
        }
    }


    /**
	 * Refresh the displayed thread's data (bookmarked, locked etc.)
	 *
	 * This loads from the database, and reflects the last cached status of the thread.
	 * To actually download current data from the site call {@link #syncThread()} instead.
	 * @see ThreadDataCallback
	 */
	private void refreshInfo() {
		restartLoader(Constants.THREAD_INFO_LOADER_ID, null, mThreadLoaderCallback);
	}


	/**
	 * Refresh the posts displayed, according to current setting (thread ID, page etc.)
	 *
	 * This loads from the database, and reflects the last cached view of the thread.
	 * To actually download updated data (including changes in posts' viewed status) call
	 * {@link #syncThread()} instead.
	 * @see PostLoaderManager
	 */
	private void refreshPosts(){
		restartLoader(Constants.POST_LOADER_ID, null, mPostLoaderCallback);
	}


	public void setTitle(@NonNull String title){
		mTitle = title;
		parentActivity.onPageContentChanged();
	}


	@Override
	public String getTitle(){
		return mTitle;
	}


	/**
	 * Get the current thread's parent forum's ID.
	 *
	 * @return the parent forum's ID, or 0 if something went wrong
     */
	public int getParentForumId() {
		return mParentForumId;
	}


	@Override
	public boolean handleNavigation(@NotNull NavigationEvent event) {
		// need to check if the fragment is attached to the activity - if not, defer any handled events until it is attached
		if (event instanceof NavigationEvent.Thread) {
			if (!isAdded()) {
				deferNavigation(event);
			} else {
				NavigationEvent.Thread thread = (NavigationEvent.Thread) event;
				// if we're currently displaying this thread, and no page was specified (i.e. it's
				// a "show this thread" navigation) then we don't need to do anything
				if (thread.getId() != currentThreadId || thread.getPage() != null) {
					openThread(thread.getId(), thread.getPage(), thread.getPostJump());
				}
			}
			return true;
		} else if (event instanceof NavigationEvent.Url) {
			if (!isAdded()) {
				deferNavigation(event);
			} else {
				NavigationEvent.Url url = (NavigationEvent.Url) event;
				openThread(url.getUrl());
			}
			return true;
		}
		return false;
	}

	/**
	 * Store a navigation event for handling when this fragment is attached to the activity
	 */
	private void deferNavigation(@NonNull NavigationEvent event) {
		Timber.d("Deferring navigation event(%s) - isAdded = %b", event, isAdded());
		pendingNavigation = event;
	}


	/**
	 * Open a thread, jumping to a specific page and post if required.
	 * @param id          The thread's ID
	 * @param page        An optional page to display, otherwise it defaults to the first page
	 * @param postJump    An optional URL fragment representing the post ID to jump to
	 */
	private void openThread(int id, @Nullable Integer page, @Nullable String postJump){
		Timber.i("Opening thread (old/new) ID:%d/%d, PAGE:%s/%s, JUMP:%s/%s",
				getThreadId(), id, getPageNumber(), page, getPostJump(), postJump);
		clearBackStack();
		int threadPage = (page == null) ? FIRST_PAGE : page;
    	loadThread(id, threadPage, postJump, true);
	}


	/**
	 * Open a specific thread represented in an AwfulURL
     */
	private void openThread(AwfulURL url) {
		// TODO: fix this prefs stuff, get it initialised somewhere consistent in the lifecycle, preferably in AwfulFragment
		// TODO: validate the AwfulURL, e.g. make sure it's the correct type
		if(url == null){
			Toast.makeText(this.getActivity(), "Error occurred: URL was empty", Toast.LENGTH_LONG).show();
			return;
		}
    	clearBackStack();
    	if(url.isRedirect()){
    		startPostRedirect(url.getURL(getPrefs().postPerPage));
    	}else{
    		loadThread((int) url.getId(), (int) url.getPage(getPrefs().postPerPage), url.getFragment(), true);
    	}
	}


	/**
	 * Load the thread represented in an AwfulStackEntry
	 */
	private void loadThread(@NonNull AwfulStackEntry thread) {
		loadThread(thread.id, thread.page, null, false);
	}


	/**
	 * Actually load the new thread
	 * @param id		The thread's ID
	 * @param page		The number of the page to display
	 * @param postJump	An optional URL fragment representing the post ID to jump to
     */
	private void loadThread(int id, int page, @Nullable String postJump, boolean fullSync) {
		setThreadId(id);
		setPageNumber(page);
		this.setPostJump(postJump != null ? postJump : "");
		setPostFiltering(null, null);
		mLastPage = FIRST_PAGE;
		updateUiElements();
		showBlankPage();
		if(getActivity() != null){
			getLoaderManager().destroyLoader(Constants.THREAD_INFO_LOADER_ID);
			getLoaderManager().destroyLoader(Constants.POST_LOADER_ID);
			refreshInfo();
			// TODO: shouldn't every load do a sync?
			if (fullSync) {
				syncThread();
			} else {
				refreshPosts();
			}
    	}
	}

	private static class AwfulStackEntry{
		public final int id;
		public final int page;
		public final int scrollPos;
		public AwfulStackEntry(int threadId, int pageNum, int scrollPosition){
			id = threadId; page = pageNum; scrollPos = scrollPosition;
		}
	}
	
	private void pushThread(int id, int page, String postJump){
		if(mThreadView != null && getThreadId() != 0){
			backStack.addFirst(new AwfulStackEntry(getThreadId(), getPageNumber(), mThreadView.getScrollY()));
		}
		loadThread(id, page, postJump, true);
	}
	
	private void popThread(){
		loadThread(backStack.removeFirst());
	}
	
	private void clearBackStack(){
		backStack.clear();
	}
	
	private int backStackCount(){
		return backStack.size();
	}
	
	@Override
	public boolean onBackPressed() {
		if(backStackCount() > 0){
			popThread();
			return true;
		}else if(postFilterUserId != null){
            showAllPosts(null);
            return true;
        }else{
			return false;
		}
	}


	@Override
	protected boolean doScroll(boolean down) {
		if (mThreadView == null) {
			return false;
		} else if (down) {
			mThreadView.pageDown(false);
		} else {
			mThreadView.pageUp(false);
		}
		return true;
	}


	private void toggleScreenOn() {
    	keepScreenOn = !keepScreenOn;
		if (mThreadView != null) {
			mThreadView.setKeepScreenOn(keepScreenOn);
		}

		//TODO icon
		getAlertView().setTitle(keepScreenOn? "Screen stays on" :"Screen turns itself off").show();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case Constants.AWFUL_PERMISSION_WRITE_EXTERNAL_STORAGE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					if(downloadLink != null)
					enqueueDownload(downloadLink);
				} else {
					Toast.makeText(getActivity(), R.string.no_file_permission_download, Toast.LENGTH_LONG).show();
				}
				downloadLink = null;
				break;
			}
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}
}
