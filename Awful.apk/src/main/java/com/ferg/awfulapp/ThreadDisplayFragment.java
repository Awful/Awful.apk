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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.text.TextUtils;
import android.util.Log;
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
import com.crashlytics.android.Crashlytics;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.Keys;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.AwfulTheme;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.BookmarkRequest;
import com.ferg.awfulapp.task.CloseOpenRequest;
import com.ferg.awfulapp.task.IgnoreRequest;
import com.ferg.awfulapp.task.MarkLastReadRequest;
import com.ferg.awfulapp.task.PostRequest;
import com.ferg.awfulapp.task.ProfileRequest;
import com.ferg.awfulapp.task.RedirectTask;
import com.ferg.awfulapp.task.ReportRequest;
import com.ferg.awfulapp.task.SinglePostRequest;
import com.ferg.awfulapp.task.VoteRequest;
import com.ferg.awfulapp.thread.AwfulAction;
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
import com.ferg.awfulapp.widget.MinMaxNumberPicker;
import com.ferg.awfulapp.widget.PageBar;
import com.ferg.awfulapp.widget.PagePicker;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Uses intent extras:
 *  TYPE - STRING ID - DESCRIPTION
 *	int - Constants.THREAD_ID - id number for that thread
 *	int - Constants.THREAD_PAGE - page number to load
 *
 *  Can also handle an HTTP intent that refers to an SA showthread.php? url.
 */
public class ThreadDisplayFragment extends AwfulFragment implements SwipyRefreshLayout.OnRefreshListener {

	private static final String THREAD_HTML_KEY = "threadHtml";
	private static final String SCROLL_POSITION_KEY = "scroll_position";
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
	private static final int FIRST_PAGE = 1;

	// TODO: fix this it's all over the place, getting assigned as 1 in loadThread etc - maybe it should default to FIRST_PAGE?
	/** Current thread's last page */
	private int mLastPage = 0;
	private int mParentForumId = 0;
	private boolean threadClosed = false;
	private boolean threadBookmarked = false;
    private boolean threadArchived = false;
	private boolean threadOpenClose = false;

    private boolean keepScreenOn = false;
	//oh god i'm replicating core android functionality, this is a bad sign.
    private final LinkedList<AwfulStackEntry> backStack = new LinkedList<>();
	private boolean bypassBackStack = false;

    private String mTitle = null;
	private String mPostJump = "";
	private int savedScrollPosition = 0;
	
	private ShareActionProvider shareProvider;

    private ForumsIndexActivity parentActivity;
    
    private final ThreadDisplayFragment mSelf = this;




    private String bodyHtml = "";
	private final HashMap<String,String> ignorePostsHtml = new HashMap<>();
    private AsyncTask<Void, Void, String> redirect = null;
	private Uri downloadLink;

	private final ThreadContentObserver mThreadObserver = new ThreadContentObserver(mHandler);

	{
        TAG = "ThreadDisplayFragment";
    }


	@Override
    public void onAttach(Context context) {
        super.onAttach(context);
        parentActivity = (ForumsIndexActivity) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if(savedInstanceState != null){
        	if (DEBUG) Log.d(TAG, "Loading from savedInstanceState");
            if(savedInstanceState.containsKey(THREAD_HTML_KEY)){
                bodyHtml = savedInstanceState.getString(THREAD_HTML_KEY);
            }
    		savedScrollPosition = savedInstanceState.getInt(SCROLL_POSITION_KEY, 0);
        }else{
            Intent data = getActivity().getIntent();
        	if (data.getData() != null && data.getScheme().equals("http")) {
                AwfulURL url = AwfulURL.parse(data.getDataString());
        		mPostJump = url.getFragment().replaceAll("\\D", "");
                switch(url.getType()){
                case THREAD:
                	if(url.isRedirect()){
                		startPostRedirect(url.getURL(mPrefs.postPerPage));
                	}else{
                        setThreadId((int) url.getId());
                        setPage((int) url.getPage(mPrefs.postPerPage));
                	}
                	break;
                case POST:
                	startPostRedirect(url.getURL(mPrefs.postPerPage));
                	break;
				case INDEX:
					displayForumIndex();
					break;
                }

            }
        }
         
        mPostLoaderCallback = new PostLoaderManager();
        mThreadLoaderCallback = new ThreadDataCallback();
        
        if(getThreadId() > 0 && TextUtils.isEmpty(bodyHtml)){
        	syncThread();
        }
    }
//--------------------------------
    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
		View result;
		try {
			result = inflateView(R.layout.thread_display, aContainer, aInflater);
		} catch (InflateException e) {
			if (webViewIsMissing(e)) {
				return null;
			} else {
				throw e;
			}
		}

		pageBar = (PageBar) result.findViewById(R.id.page_bar);
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

		mThreadView = (AwfulWebView) result.findViewById(R.id.thread);
        initThreadViewProperties();

		mUserPostNotice = (TextView) result.findViewById(R.id.thread_userpost_notice);
		refreshProbationBar();

		// FAB
		mFAB  = (FloatingActionButton) result.findViewById(R.id.just_post);
		mFAB.setOnClickListener(onButtonClick);
		mFAB.setVisibility(View.GONE);

		return result;
	}


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mSRL = (SwipyRefreshLayout) view.findViewById(R.id.thread_swipe);
		mSRL.setColorSchemeResources(ColorProvider.getSRLProgressColors(null));
		mSRL.setProgressBackgroundColor(ColorProvider.getSRLBackgroundColor(null));
		mSRL.setEnabled(!mPrefs.disablePullNext);
    }


    @Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState);
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
		Log.w(TAG, "Can't inflate thread view, WebView package is updating?:\n");
		e.printStackTrace();
		new AlertBuilder()
				.setIcon(R.drawable.ic_error)
				.setTitle(R.string.web_view_missing_alert_title)
				.setSubtitle(R.string.web_view_missing_alert_message)
				.show();
		return true;
	}



	private WebViewClient threadWebViewClient = new WebViewClient() {
		@Override
		public void onPageFinished(WebView view, String url) {
			setProgress(100);
			if (mThreadView != null && bodyHtml != null && !bodyHtml.isEmpty()) {
				mThreadView.refreshPageContents(true);
			}
		}


		@Override
		public boolean shouldOverrideUrlLoading(WebView aView, String aUrl) {
			AwfulURL aLink = AwfulURL.parse(aUrl);
			switch (aLink.getType()) {
				case FORUM:
					displayForum(aLink.getId(), aLink.getPage());
					break;
				case THREAD:
					if (aLink.isRedirect()) {
						startPostRedirect(aLink.getURL(mPrefs.postPerPage));
					} else {
						pushThread((int) aLink.getId(), (int) aLink.getPage(), aLink.getFragment().replaceAll("\\D", ""));
					}
					break;
				case POST:
					startPostRedirect(aLink.getURL(mPrefs.postPerPage));
					break;
				case EXTERNAL:
					if (mPrefs.alwaysOpenUrls) {
						startUrlIntent(aUrl);
					} else {
						showUrlMenu(aUrl);
					}
					break;
				case INDEX:
					displayForumIndex();
					break;
			}
			return true;
		}
	};


	private void initThreadViewProperties() {
		if (mThreadView == null) {
			Log.w(TAG, "initThreadViewProperties called for null WebView");
			return;
		}
		mThreadView.setWebViewClient(threadWebViewClient);
		mThreadView.setWebChromeClient(new LoggingWebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    super.onProgressChanged(view, newProgress);
                    setProgress(newProgress / 2 + 50);//second half of progress bar
                }
        });
        mThreadView.setJavascriptHandler(clickInterface);

        refreshSessionCookie();
		mThreadView.setContent(getBlankPage());

		mThreadView.setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength)  {
				enqueueDownload(Uri.parse(url));
			}
		});
	}

	private void updatePageBar() {
		if(pageBar != null){
			pageBar.updatePagePosition(getPage(), getLastPage());
		}
		if (getActivity() != null) {
			invalidateOptionsMenu();
		}
		if (mThreadView != null) {
			mSRL.setOnRefreshListener(mPrefs.disablePullNext ? null : this);
		}
	}


    @Override
    public void onResume() {
        super.onResume();
		if(mThreadView != null){
			mThreadView.onResume();
			mThreadView.refreshPageContents(false);
		}
        getActivity().getContentResolver().registerContentObserver(AwfulThread.CONTENT_URI, true, mThreadObserver);
        refreshInfo();
    }

    
	@Override
	public void onPageVisible() {
        if(mThreadView != null){
			mThreadView.onResume();
        	mThreadView.setKeepScreenOn(keepScreenOn);
        }
        if(parentActivity != null && mParentForumId != 0){
			parentActivity.setNavIds(mParentForumId, getThreadId());
        }
	}

	@Override
	public void onPageHidden() {
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
		NetworkUtils.cancelRequests(PostRequest.REQUEST_TAG);
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
        	cookieMonster.setCookie(Constants.COOKIE_DOMAIN, NetworkUtils.getCookieString(Constants.COOKIE_NAME_SESSIONID));
        	cookieMonster.setCookie(Constants.COOKIE_DOMAIN, NetworkUtils.getCookieString(Constants.COOKIE_NAME_SESSIONHASH));
        	cookieMonster.setCookie(Constants.COOKIE_DOMAIN, NetworkUtils.getCookieString(Constants.COOKIE_NAME_USERID));
        	cookieMonster.setCookie(Constants.COOKIE_DOMAIN, NetworkUtils.getCookieString(Constants.COOKIE_NAME_PASSWORD));
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
		MenuItem openClose = menu.findItem(R.id.close);
		if(openClose != null){
			openClose.setVisible(threadOpenClose);
			openClose.setTitle((threadClosed?getString(R.string.thread_open):getString(R.string.thread_close)));
		}
		MenuItem find = menu.findItem(R.id.find);
		if(find != null){
			find.setVisible(true);
		}
		MenuItem reply = menu.findItem(R.id.reply);
		if(reply != null){
			reply.setVisible(mPrefs.noFAB);
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
			case R.id.close:
				toggleCloseThread();
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
    			copyThreadURL(null);
    			break;
    		case R.id.find:
				if (mThreadView != null) {
					this.mThreadView.showFindDialog(null, true);
				}
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
    		default:
    			return super.onOptionsItemSelected(item);
    		}

    		return true;
    	}


	/**
	 * Get a URL that links to a particular thread.
	 *
	 * @param postId An optional post ID, appended as the URL's fragment
	 * @return the full URL
	 */
	@NonNull
	private String generateThreadUrl(@Nullable String postId) {
		Uri.Builder builder = Uri.parse(Constants.FUNCTION_THREAD).buildUpon()
				.appendQueryParameter(Constants.PARAM_THREAD_ID, String.valueOf(getThreadId()))
				.appendQueryParameter(Constants.PARAM_PAGE, String.valueOf(getPage()))
				.appendQueryParameter(Constants.PARAM_PER_PAGE, String.valueOf(mPrefs.postPerPage));
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
	private String generatePostUrl(@NonNull String postId) {
		return Uri.parse(Constants.FUNCTION_THREAD).buildUpon()
				.appendQueryParameter(Constants.PARAM_GOTO, Constants.VALUE_POST)
				.appendQueryParameter(Constants.PARAM_POST_ID, postId)
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
	protected Intent createShareIntent(@Nullable String url) {
		Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain");
		if (url == null) {
			// we're sharing the current thread - we can add the title in here
			intent.putExtra(Intent.EXTRA_SUBJECT, mTitle);
			url = generateThreadUrl(null);
		}
		return intent.putExtra(Intent.EXTRA_TEXT, url);
	}



	/**
	 * Copy a thread's URL to the clipboard
	 * @param postId	An optional post ID, used as the url's fragment
     */
	protected void copyThreadURL(@Nullable String postId) {
		String clipLabel = getString(R.string.copy_url) + getPage();
		String clipText  = generateThreadUrl(postId);
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
				.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						queueRequest(new VoteRequest(activity, getThreadId(), item)
								.build(ThreadDisplayFragment.this, new AwfulRequest.AwfulResultCallback<Void>() {
									@Override
									public void success(Void result) {
										new AlertBuilder().setTitle(R.string.vote_succeeded)
												.setSubtitle(R.string.vote_succeeded_sub)
												.setIcon(R.drawable.ic_mood)
												.show();
									}


									@Override
									public void failure(VolleyError error) {
									}
								}));
					}
				}).show();
	}


	/**
	 * Add a user to the ignore list.
	 *
	 * @param userId The awful ID of the user
	 */
	protected void ignoreUser(@NonNull final String userId) {
		final Activity activity = getActivity();
		if (mPrefs.ignoreFormkey == null) {
			queueRequest(new ProfileRequest(activity, null).build());
		}
		if (mPrefs.showIgnoreWarning) {

			DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == AlertDialog.BUTTON_NEUTRAL) {
						// cancel future alerts if the user clicks the "don't warn" option
						mPrefs.setPreference(Keys.SHOW_IGNORE_WARNING, false);
					}
					doIgnoreUser(activity, userId);
				}
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
	private void doIgnoreUser(@NonNull Context context, @NonNull String userId) {
		//we don't care about status callbacks for this, so we use the build() that doesn't do callbacks
		queueRequest(new IgnoreRequest(context, userId).build());
	}


    /**
	 * Toggle a user as marked or unmarked.
     */
	protected void toggleMarkUser(String username){
        if(mPrefs.markedUsers.contains(username)){
            mPrefs.unmarkUser(username);
        }else{
            mPrefs.markUser(username);
        }
	}


	/**
	 * Toggle between displaying a single user's posts, or all posts
	 * @param aPostId	The ID of the post to display, if toggling filtering off
	 * @param aUserId	The ID of the user whose posts we're showing, if toggling on
	 * @param aUsername	The username of the user, if toggling on
     */
	// TODO: refactor this and the methods it calls - it's so weird
	protected void toggleUserPosts(String aPostId, String aUserId, String aUsername){
		if(postFilterUserId != null){
			showAllPosts(aPostId);
		}else{
			showUsersPosts(Integer.parseInt(aUserId), aUsername);
		}
	}


	/**
	 * Display a dialog to report a post
	 *
	 * @param postId	The ID of the bad post
     */
	protected void reportUser(final String postId){
		final EditText reportReason = new EditText(this.getActivity());

		new AlertDialog.Builder(this.getActivity())
		  .setTitle("Report inappropriate post")
		  .setMessage("Did this post break the forum rules? If so, please report it by clicking below. If you would like to add any comments explaining why you submitted this post, please do so here:")
		  .setView(reportReason)
		  .setPositiveButton("Report", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
		      String reason = reportReason.getText().toString();
		      queueRequest(new ReportRequest(getActivity(), postId, reason).build(ThreadDisplayFragment.this, new AwfulRequest.AwfulResultCallback<String>() {
                  @Override
                  public void success(String result) {
					  new AlertBuilder().setTitle(result).setIcon(R.drawable.ic_mood).show();
                  }

                  @Override
                  public void failure(VolleyError error) {
					  new AlertBuilder().setTitle(error.getMessage()).setIcon(R.drawable.ic_mood).show();

                  }
              }));
		    }
		  })
		  .setNegativeButton(R.string.cancel, null)
		  .show();
	}
	
    @Override
    public void onSaveInstanceState(Bundle outState){
    	super.onSaveInstanceState(outState);
    	if(DEBUG) Log.d(TAG,"onSaveInstanceState");
        if(bodyHtml != null && bodyHtml.length() > 0){
			outState.putString(THREAD_HTML_KEY, bodyHtml);
        }
    	if(mThreadView != null){
    		outState.putInt(SCROLL_POSITION_KEY, mThreadView.getScrollY());
    	}
    }


    /**
	 * Reload the current thread page
	 */
    private void syncThread() {
		final Activity activity = getActivity();
        if( activity != null) {
			// cancel pending post loading requests
			NetworkUtils.cancelRequests(PostRequest.REQUEST_TAG);
        	bodyHtml = "";
			// call this with cancelOnDestroy=false to retain the request's specific type tag
			final int pageNumber = getPage();
			int userId = postFilterUserId == null ? BLANK_USER_ID : postFilterUserId;
			queueRequest(new PostRequest(activity, getThreadId(), pageNumber, userId)
					.build(this, new AwfulRequest.AwfulResultCallback<Integer>() {
				@Override
				public void success(Integer result) {
					refreshInfo();
					if (result == pageNumber) {
						setProgress(75);
						refreshPosts();
					} else {
						Log.w(TAG, "Page mismatch: " + pageNumber + " - " + result);
					}
				}

				@Override
				public void failure(VolleyError error) {
					if (null != error.getMessage() && error.getMessage().startsWith("java.net.ProtocolException: Too many redirects")) {
						Log.e(TAG, "Error: " + error.getMessage() + "\nFailed to sync thread - You are now LOGGED OUT");
						NetworkUtils.clearLoginCookies(activity);
						activity.startActivity(new Intent().setClass(activity, AwfulLoginActivity.class));
					}
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
	protected void markLastRead(int index) {
		new AlertBuilder().setTitle(R.string.mark_last_read_progress)
				.setSubtitle(R.string.please_wait_subtext)
				.setIcon(R.drawable.ic_visibility)
				.show();

		queueRequest(new MarkLastReadRequest(getActivity(), getThreadId(), index)
				.build(null, new AwfulRequest.AwfulResultCallback<Void>() {
			@Override
			public void success(Void result) {
				if(getActivity() != null){
					new AlertBuilder().setTitle(R.string.mark_last_read_success)
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
		mPrefs.amberDefaultPos = !mPrefs.amberDefaultPos;
		mPrefs.setPreference(Keys.AMBER_DEFAULT_POS, mPrefs.amberDefaultPos);
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
                    new AlertBuilder().fromError(new AwfulError()).show();
                    return;
                }

                AwfulURL result = AwfulURL.parse(url);
                if (postUrl.contains(Constants.VALUE_LASTPOST)) {
                    //This is a workaround for how the forums handle the perPage value with goto=lastpost.
                    //The redirected url is lacking the perpage=XX value.
                    //We just override the assumed (40) with the number we requested when starting the redirect.
                    //I gotta ask chooch to fix this at some point.
                    result.setPerPage(mPrefs.postPerPage);
                }
                if (result.getType() == TYPE.THREAD) {
					int threadId = (int) result.getId();
					int threadPage = (int) result.getPage(mPrefs.postPerPage);
					String postJump = result.getFragment().replaceAll("\\D", "");
					if (bypassBackStack) {
                        openThread(threadId, threadPage, postJump);
                    } else {
                        pushThread(threadId, threadPage, postJump);
                    }
                } else if (result.getType() == TYPE.INDEX) {
                    activity.displayForumIndex();
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

		new PagePicker(activity, getLastPage(), getPage(), new MinMaxNumberPicker.ResultListener() {
			@Override
			public void onButtonPressed(int button, int resultValue) {
				if (button == DialogInterface.BUTTON_POSITIVE) {
					goToPage(resultValue);
				}
			}
		}).show();
	}


	@Override
    public void onActivityResult(int aRequestCode, int aResultCode, Intent aData) {
    	if (DEBUG) Log.d(TAG, String.format("onActivityResult - request code: %d, result: %d", aRequestCode, aResultCode));
        // If we're here because of a post result, refresh the thread
        switch (aRequestCode) {
            case PostReplyFragment.REQUEST_POST:
            		bypassBackStack = true;
            	if(aResultCode == PostReplyFragment.RESULT_POSTED){
            		startPostRedirect(AwfulURL.threadLastPage(getThreadId(), mPrefs.postPerPage).getURL(mPrefs.postPerPage));
            	}else if(aResultCode > 100){//any result >100 it is a post id we edited
					// TODO: >100 is a bit too magical
            		startPostRedirect(AwfulURL.post(aResultCode, mPrefs.postPerPage).getURL(mPrefs.postPerPage));
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
		int currentPage = getPage();
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
    private final View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
				case R.id.just_post:
					displayPostReplyDialog();
					break;
            }
        }
    };

    private void displayPostReplyDialog() {
        displayPostReplyDialog(getThreadId(), -1, AwfulMessage.TYPE_NEW_REPLY);
    }
	private void toggleCloseThread(){
		queueRequest(new CloseOpenRequest(getActivity(), getThreadId()).build(mSelf, new AwfulRequest.AwfulResultCallback<Void>() {
			@Override
			public void success(Void result) {
				threadClosed = !threadClosed;
			}

			@Override
			public void failure(VolleyError error) {
				Log.e(TAG, (threadClosed?"Thread closing":"Thread reopening")+" failed");
			}
		}));
	}

	private void populateThreadView(ArrayList<AwfulPost> aPosts) {
		if (mThreadView == null) {
			Log.w(TAG, "populateThreadView called with null WebView");
			return;
		}
		updateUiElements();

        try {

            String html = AwfulThread.getHtml(aPosts, AwfulPreferences.getInstance(getActivity()), getPage(), mLastPage, mParentForumId, threadClosed);
            refreshSessionCookie();
            bodyHtml = html;
			mThreadView.refreshPageContents(true);
            setProgress(100);
        } catch (Exception e) {
        	e.printStackTrace();
            // If we've already left the activity the webview may still be working to populate,
            // just log it
        }
        if (DEBUG) Log.d(TAG, String.format("Finished populateThreadView with %d posts", aPosts.size()));
    }
    
	@Override
	public void onRefresh(SwipyRefreshLayoutDirection swipyRefreshLayoutDirection) {
		if(swipyRefreshLayoutDirection == SwipyRefreshLayoutDirection.TOP){
			refresh();
		}else{
			turnPage(true);
		}
	}

	private final ClickInterface clickInterface = new ClickInterface();

	private class ClickInterface extends WebViewJsInterface {

        @JavascriptInterface
        public void onMoreClick(final String aPostId, final String aUsername, final String aUserId, final String lastReadUrl, final boolean editable, final boolean isAdminOrMod, final boolean isPlat) {
			PostActionsFragment postActions = new PostActionsFragment();
			postActions.setTitle("Select an Action");
			postActions.setParent(mSelf);
			postActions.setPostId(aPostId);
			postActions.setUsername(aUsername);
			postActions.setUserId(aUserId);
			postActions.setThreadId(getThreadId());
			postActions.setLastReadUrl(lastReadUrl);
			postActions.setActions(AwfulAction.getPostActions(aUsername, editable, isAdminOrMod, isPlat));

			postActions.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
			postActions.show(mSelf.getFragmentManager(), "Post Actions");
		}


		@Override
		protected void setCustomPreferences(Map<String, String> preferences) {
			// TODO: 23/01/2017 add methods so you can't mess with the map directly
			preferences.put("postjumpid", mPostJump);
			preferences.put("scrollPosition", Integer.toString(savedScrollPosition));
		}

		@JavascriptInterface
		public String getBodyHtml(){
			return bodyHtml;
		}

		@JavascriptInterface
		public String getIgnorePostHtml(String id){
			return ignorePostsHtml.get(id);
		}

        @JavascriptInterface
        public String getPostJump(){
            return mPostJump;
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
						Log.w(TAG,"Loading Single post #"+ignorePost+" failed");
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
			((ForumsIndexActivity)mSelf.getAwfulActivity()).reenableSwipe();
		}

    }

	
	private void showUrlMenu(final String url) {
		if (url == null) {
			Log.w(TAG, "Passed null URL to #showUrlMenu!");
			return;
		}
		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager == null) {
			Log.w(TAG, "showUrlMenu called but can't get FragmentManager!");
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
			isImage = (StringUtils.indexOfAny(lastSegment, ".jpg", ".jpeg", ".png", ".gif") != -1
					&& !StringUtils.contains(lastSegment, ".gifv"))
					|| (lastSegment.equals("attachment.php") && path.getHost().equals("forums.somethingawful.com"));
			isGif = StringUtils.contains(lastSegment, ".gif")
					&& !StringUtils.contains(lastSegment, ".gifv");
		}

		////////////////////////////////////////////////////////////////////////
        // TODO: 28/04/2017 remove all this when Crashlytics #717 is fixed
		if (AwfulApplication.crashlyticsEnabled()) {
			Crashlytics.setString("Menu for URL:", url);
			Crashlytics.setInt("Thread ID", getThreadId());
			Crashlytics.setInt("Page", getPage());

			FragmentActivity activity = getActivity();
			Crashlytics.setBool("Activity exists", activity != null);
			if (activity != null) {
				String state = "Activity:";
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
					state += (activity.isDestroyed()) ? "IS_DESTROYED " : "";
				}
				state += (activity.isFinishing()) ? "IS_FINISHING" : "";
				state += (activity.isChangingConfigurations()) ? "IS_CHANGING_CONFIGURATIONS" : "";
				Crashlytics.setString("Activity state:", state);
			}
			Crashlytics.setBool("Thread display fragment resumed", isResumed());
			Crashlytics.setBool("Thread display fragment attached", isAdded());
			Crashlytics.setBool("Thread display fragment removing", isRemoving());
		}
        ////////////////////////////////////////////////////////////////////////

		PostActionsFragment postActions = new PostActionsFragment();
		postActions.setTitle(url);
		postActions.setParent(mSelf);
		postActions.setUrl(url);
		postActions.setActions(AwfulAction.getURLActions(url, isImage, isGif));

		postActions.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
		postActions.show(fragmentManager, "Link Actions");
	}

	protected void showImageInline(String url){
		if(mThreadView != null){
			mThreadView.runJavascript(String.format("showInlineImage('%s')", url));
		}
	}

	protected void enqueueDownload(Uri link) {
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

	protected void copyToClipboard(String text){
		safeCopyToClipboard("Copied URL", text, null);
	}

	protected void startUrlIntent(String url){
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		PackageManager pacMan = getActivity().getPackageManager();
		List<ResolveInfo> res = pacMan.queryIntentActivities(browserIntent,
				PackageManager.MATCH_DEFAULT_ONLY);
		if (res.size() > 0) {
			browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getActivity().startActivity(browserIntent);
		} else {
			String[] split = url.split(":");
			new AlertBuilder().setTitle("Cannot open link:")
					.setSubtitle("No application found for protocol" + (split.length > 0 ? ": " + split[0] : "."))
					.show();
		}
	}

	protected void displayImage(String text){
		Intent intent = new Intent(this.getContext(),ImageViewActivity.class);
		intent.putExtra(Constants.ZOOM_URL, text);
		startActivity(intent);

	}
	
	@Override
	public void onPreferenceChange(AwfulPreferences mPrefs, String key) {
		super.onPreferenceChange(mPrefs, key);
		if(DEBUG) Log.i(TAG,"onPreferenceChange"+((key != null)?":"+key:""));
        if(null != getAwfulActivity() && pageBar != null){
		    getAwfulActivity().setPreferredFont(pageBar.getTextView());
			pageBar.setTextColour(ColorProvider.ACTION_BAR_TEXT.getColor());
		}

		if(mThreadView != null){
			mThreadView.setBackgroundColor(Color.TRANSPARENT);
            mThreadView.runJavascript(String.format("changeFontFace('%s')", mPrefs.preferredFont));
            mThreadView.getSettings().setDefaultFontSize(mPrefs.postFontSizeDip);
            mThreadView.getSettings().setDefaultFixedFontSize(mPrefs.postFixedFontSizeDip);

			if("marked_users".equals(key)){
				mThreadView.runJavascript(String.format("updateMarkedUsers('%s')", TextUtils.join(",", mPrefs.markedUsers)));
			}
		}
		clickInterface.updatePreferences();
		if(mFAB != null) {
			mFAB.setVisibility((mPrefs.noFAB?View.GONE:View.VISIBLE));
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
		setPage(aPage);
		updateUiElements();
		mPostJump = "";
		showBlankPage();
		syncThread();
	}


	/**
	 * Get an empty page structure, themed according to the thread's parent forum
	 * @return	The basic page HTML, with no post content
     */
	private String getBlankPage(){
		return AwfulThread.getContainerHtml(mPrefs, getParentForumId());
	}

    private int getLastPage() {
        return mLastPage;
    }

    private int getThreadId() {
        return parentActivity.getThreadId();
    }
	
	private int getPage() {
		if(parentActivity != null){
			return parentActivity.getThreadPage();
		}
        return 0;
	}
	private void setPage(int aPage){
		parentActivity.setThread(null, aPage);
	}
	private void setThreadId(int aThreadId){
        parentActivity.setThread(aThreadId, null);
	}


	/**
	 * Show posts filtered to a specific user
	 * @param id	the user's ID
	 * @param name	the user's username
     */
	private void showUsersPosts(int id, String name){
		// TODO: legend has it this doesn't work and shows other people's posts if the page isn't full
		pageBeforeFiltering = getPage();
		setPostFiltering(id, name);
		setPage(FIRST_PAGE);
		mLastPage = FIRST_PAGE;
		mPostJump = "";
        refresh();
	}


	/**
	 * Clear filtering added by {@link #showUsersPosts(int, String)} and return to a specific post
	 * @param postId	The ID of the post to navigate to
     */
	private void showAllPosts(String postId){
		if(TextUtils.isEmpty(postId) || postId.length() < 3){
			setPostFiltering(null, null);
			setPage(pageBeforeFiltering);
			mLastPage = 0;
			mPostJump = "";
			refresh();
		}else{
			showBlankPage();
	        openThread(AwfulURL.parse(generatePostUrl(postId)));
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
		bodyHtml = "";
		if(mThreadView != null){
			mThreadView.refreshPageContents(true);
		}
	}


    private class PostLoaderManager implements LoaderManager.LoaderCallbacks<Cursor> {
		private final static String sortOrder = AwfulThread.STICKY + " " + AwfulPost.POST_INDEX + " ASC";
        private final static String selection = AwfulPost.THREAD_ID + "=? AND " + AwfulPost.POST_INDEX + ">=? AND " + AwfulPost.POST_INDEX + "<?";
        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            int index = AwfulPagedItem.pageToIndex(getPage(), mPrefs.postPerPage, 0);
            Log.i(TAG, String.format("Displaying thread: %d index: %d page: %d per page: %d", getThreadId(), index, getPage(), mPrefs.postPerPage));
            return new CursorLoader(getActivity(),
            						AwfulPost.CONTENT_URI,
            						AwfulProvider.PostProjection,
            						selection,
            						AwfulProvider.int2StrArray(getThreadId(), index, index+mPrefs.postPerPage),
            						sortOrder);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.i(TAG, String.format("Load finished, page:%d, populating: %d", getPage(), aData.getCount()));
        	setProgress(90);
        	if(aData.isClosed()){
        		return;
        	}
        	if(mThreadView != null){
        		populateThreadView(AwfulPost.fromCursor(getActivity(), aData));
        	}
			savedScrollPosition = 0;
        }

        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        }
    }

	// TODO: fix race condition, see AwfulFragment#setTitle
    
    private class ThreadDataCallback implements LoaderManager.LoaderCallbacks<Cursor> {

        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            return new CursorLoader(getActivity(), ContentUris.withAppendedId(AwfulThread.CONTENT_URI, getThreadId()), 
            		AwfulProvider.ThreadProjection, null, null, null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.i(TAG,"Thread title finished, populating.");
        	if(aData.getCount() >0 && aData.moveToFirst()){
        		mLastPage = AwfulPagedItem.indexToPage(aData.getInt(aData.getColumnIndex(AwfulThread.POSTCOUNT)),mPrefs.postPerPage);
				threadClosed = aData.getInt(aData.getColumnIndex(AwfulThread.LOCKED))>0;
				threadOpenClose = aData.getInt(aData.getColumnIndex(AwfulThread.CAN_OPEN_CLOSE))>0;
        		threadBookmarked = aData.getInt(aData.getColumnIndex(AwfulThread.BOOKMARKED))>0;
				threadArchived = aData.getInt(aData.getColumnIndex(AwfulThread.ARCHIVED))>0;
				mTitle = aData.getString(aData.getColumnIndex(AwfulThread.TITLE));
        		mParentForumId = aData.getInt(aData.getColumnIndex(AwfulThread.FORUM_ID));
				if(mParentForumId != 0 && mThreadView != null){
					mThreadView.runJavascript(String.format("changeCSS('%s')", AwfulTheme.forForum(mParentForumId).getCssPath()));
				}

				parentActivity.setNavIds(mParentForumId, getThreadId());
				setTitle(mTitle);

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
					mFAB.setVisibility((mPrefs.noFAB || threadClosed || threadArchived)?View.GONE:View.VISIBLE);
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
        	if(DEBUG) Log.i(TAG,"Thread Data update.");
        	refreshInfo();
        }
    }


    /**
	 * Refresh the displayed thread's data (bookmarked, locked etc.)
	 * @see ThreadDataCallback
	 */
	private void refreshInfo() {
		restartLoader(Constants.THREAD_INFO_LOADER_ID, null, mThreadLoaderCallback);
	}


	/**
	 * Refresh the posts displayed on the current page.
	 * @see PostLoaderManager
	 */
	private void refreshPosts(){
		restartLoader(Constants.POST_LOADER_ID, null, mPostLoaderCallback);
	}


	@Override
	public void setTitle(@NonNull String title){
		mTitle = title;
		super.setTitle(title);
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


	/**
	 * Open a specific page in a thread, jumping to a specific post
	 * @param id		The thread's ID
	 * @param page		The number of the page to open
	 * @param postJump	An optional URL fragment representing the post ID to jump to
     */
	public void openThread(int id, int page, @Nullable String postJump){
    	clearBackStack();
    	loadThread(id, page, postJump, true);
	}


	/**
	 * Open a specific thread represented in an AwfulURL
     */
	public void openThread(AwfulURL url) {
		// TODO: fix this mPrefs stuff, get it initialised somewhere consistent in the lifecycle, preferably in AwfulFragment
		if(mPrefs == null){
			mPrefs = AwfulPreferences.getInstance(getAwfulActivity(), this);
		}
		// TODO: validate the AwfulURL, e.g. make sure it's the correct type
		if(url == null){
			Toast.makeText(this.getActivity(), "Error occurred: URL was empty", Toast.LENGTH_LONG).show();
			return;
		}
    	clearBackStack();
    	if(url.isRedirect()){
    		startPostRedirect(url.getURL(mPrefs.postPerPage));
    	}else{
    		loadThread((int) url.getId(), (int) url.getPage(mPrefs.postPerPage), url.getFragment(), true);
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
		setPostFiltering(null, null);
    	mLastPage = FIRST_PAGE;
		mPostJump = postJump != null ? postJump : "";
		updateUiElements();
		showBlankPage();
    	if(getActivity() != null){
			setPage(page);
			setThreadId(id);
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
			backStack.addFirst(new AwfulStackEntry(getThreadId(), getPage(), mThreadView.getScrollY()));
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
		new AlertBuilder().setTitle(keepScreenOn? "Screen stays on" :"Screen turns itself off").show();
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
