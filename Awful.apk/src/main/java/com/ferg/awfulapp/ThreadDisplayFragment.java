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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.ContentUris;
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
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.Keys;
import com.ferg.awfulapp.provider.AwfulProvider;
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
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Uses intent extras:
 *  TYPE - STRING ID - DESCRIPTION
 *	int - Constants.THREAD_ID - id number for that thread
 *	int - Constants.THREAD_PAGE - page number to load
 *
 *  Can also handle an HTTP intent that refers to an SA showthread.php? url.
 */
public class ThreadDisplayFragment extends AwfulFragment implements SwipyRefreshLayout.OnRefreshListener {

    private PostLoaderManager mPostLoaderCallback;
    private ThreadDataCallback mThreadLoaderCallback;
    
    private ImageButton mNextPage;
    private ImageButton mPrevPage;
    private ImageButton mRefreshBar;
    private TextView mPageCountText;
	private TextView mUserPostNotice;
    
    private View mProbationBar;
	private TextView mProbationMessage;
	private ImageButton mProbationButton;

	private FloatingActionButton mFAB;

    private WebView mThreadView;

    private int mUserId = 0;
    private String mPostByUsername;
    private int mLastPage = 0;
    private int mParentForumId = 0;
    private boolean threadClosed = false;
    private boolean threadBookmarked = false;
	private boolean threadArchived = false;
	private boolean threadOpenClose = false;
    
    private boolean keepScreenOn = false;
    
    //oh god i'm replicating core android functionality, this is a bad sign.
    private LinkedList<AwfulStackEntry> backStack = new LinkedList<AwfulStackEntry>();
	private boolean bypassBackStack = false;
    
    private int scrollCheckMinBound = -1;
    private int scrollCheckMaxBound = -1;
    private int[] scrollCheckBounds = null;

    private static final int buttonSelectedColor = 0x8033b5e5;//0xa0ff7f00;

    private String mTitle = null;
    
	private String mPostJump = "";
	private int savedPage = 0;//for reverting from "Find posts by"
	private int savedScrollPosition = 0;
	
	private ShareActionProvider shareProvider;

    private ForumsIndexActivity parent;
    
    private ThreadDisplayFragment mSelf = this;




    private String bodyHtml = "";
	private HashMap<String,String> ignorePostsHtml = new HashMap<>();
    private AsyncTask<Void, Void, String> redirect = null;
	private Uri downloadLink;

	public ThreadDisplayFragment() {
        super();
        TAG = "ThreadDisplayFragment";
    }

    private ThreadContentObserver mThreadObserver = new ThreadContentObserver(mHandler);



	private WebViewClient callback = new WebViewClient(){

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        	if(DEBUG) Log.e(TAG, "Opening Connection: "+url);
            return null;
        }

        @Override
		public void onPageFinished(WebView view, String url) {
			Log.e(TAG, "PageFinished");
			setProgress(100);
            if(bodyHtml != null && bodyHtml.length() > 0){
                mThreadView.loadUrl("javascript:loadpagehtml()");
            }
		}


		public void onLoadResource(WebView view, String url) {
			Log.i(TAG,"onLoadResource: "+url);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView aView, String aUrl) {
			AwfulURL alink = AwfulURL.parse(aUrl);
			switch(alink.getType()){
			case FORUM:
				displayForum(alink.getId(), alink.getPage());
				break;
			case THREAD:
				if(alink.isRedirect()){
					startPostRedirect(alink.getURL(mPrefs.postPerPage));
				}else{
					pushThread((int)alink.getId(),(int)alink.getPage(),alink.getFragment().replaceAll("\\D", ""));
				}
				break;
			case POST:
				startPostRedirect(alink.getURL(mPrefs.postPerPage));
				break;
			case EXTERNAL:
				if(mPrefs.alwaysOpenUrls){
					startUrlIntent(aUrl);
				}else{
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

    @Override
    public void onAttach(Activity aActivity) {
        super.onAttach(aActivity); Log.e(TAG, "onAttach");
        parent = (ForumsIndexActivity) aActivity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState); Log.e(TAG, "onCreate");
        setHasOptionsMenu(true);
        if(savedInstanceState != null){
        	Log.w(TAG, "Loading from savedInstanceState");
            if(savedInstanceState.containsKey("threadHtml")){
                bodyHtml = savedInstanceState.getString("threadHtml");
            }
    		savedScrollPosition = savedInstanceState.getInt("scroll_position", 0);
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
    	if(DEBUG) Log.e(TAG, "onCreateView");
    	
        View result = inflateView(R.layout.thread_display, aContainer, aInflater);


		mPageCountText = (TextView) result.findViewById(R.id.page_count);
		mPageCountText.setOnClickListener(onButtonClick);
		getAwfulActivity().setPreferredFont(mPageCountText);

		mNextPage = (ImageButton) result.findViewById(R.id.next_page);
		mNextPage.setOnClickListener(onButtonClick);
		mPrevPage = (ImageButton) result.findViewById(R.id.prev_page);
		mPrevPage.setOnClickListener(onButtonClick);
        mRefreshBar = (ImageButton) result.findViewById(R.id.refresh);
		mRefreshBar.setOnClickListener(onButtonClick);
		mThreadView = (WebView) result.findViewById(R.id.thread);
        initThreadViewProperties();
		mProbationBar = result.findViewById(R.id.probationbar);
		mProbationMessage = (TextView) result.findViewById(R.id.probation_message);
		mProbationButton  = (ImageButton) result.findViewById(R.id.go_to_LC);
		mUserPostNotice = (TextView) result.findViewById(R.id.thread_userpost_notice);
		mFAB  = (FloatingActionButton) result.findViewById(R.id.just_post);
		mFAB.setOnClickListener(onButtonClick);
		mFAB.setVisibility(View.GONE);
		updateProbationBar();

		return result;
	}


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mSRL = (SwipyRefreshLayout) view.findViewById(R.id.thread_swipe);
        mSRL.setColorSchemeResources(ColorProvider.getSRLProgressColor());
		mSRL.setProgressBackgroundColor(ColorProvider.getSRLBackgroundColor());
		if(mPrefs.disablePullNext){
			mSRL.setEnabled(false);
		}
    }


    @Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState); Log.e(TAG, "onActivityCreated");
		updatePageBar();
		updateProbationBar();
	}

	private void initThreadViewProperties() {
		mThreadView.resumeTimers();
		mThreadView.setWebViewClient(callback);
		//mThreadView.setBackgroundColor(ColorProvider.getBackgroundColor());
		mThreadView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		mThreadView.getSettings().setJavaScriptEnabled(true);
		mThreadView.getSettings().setRenderPriority(RenderPriority.LOW);
        mThreadView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
        mThreadView.getSettings().setDefaultFontSize(mPrefs.postFontSizeDip);
        mThreadView.getSettings().setDefaultFixedFontSize(mPrefs.postFixedFontSizeDip);
        if(DEBUG && AwfulUtils.isKitKat()) {
			WebView.setWebContentsDebuggingEnabled(true);
		}
		if(AwfulUtils.isLollipop()){
			mThreadView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
		}
		if (mPrefs.inlineYoutube || mPrefs.inlineWebm || mPrefs.inlineVines) {//YOUTUBE SUPPORT BLOWS
			mThreadView.getSettings().setPluginState(PluginState.ON_DEMAND);
		}
		if ( AwfulUtils.isAtLeast(Build.VERSION_CODES.JELLY_BEAN_MR1) && (mPrefs.inlineWebm || mPrefs.inlineVines)) {
			mThreadView.getSettings().setMediaPlaybackRequiresUserGesture(false);
		}
		if (mPrefs.inlineTweets && AwfulUtils.isJellybean()) {
			mThreadView.getSettings().setAllowUniversalAccessFromFileURLs(true);
			mThreadView.getSettings().setAllowFileAccessFromFileURLs(true);
			mThreadView.getSettings().setAllowFileAccess(true);
			mThreadView.getSettings().setAllowContentAccess(true);
		}

		if (!mPrefs.enableHardwareAcceleration) {
			mThreadView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
		}


		mThreadView.setWebChromeClient(new WebChromeClient() {
			public boolean onConsoleMessage(ConsoleMessage message) {
				if(DEBUG) Log.d("Web Console", message.message() + " -- From line " + message.lineNumber() + " of " + message.sourceId());
				return true;
			}

			@Override
			public void onCloseWindow(WebView window) {
				super.onCloseWindow(window);
				if(DEBUG) Log.e(TAG,"onCloseWindow");
			}

			@Override
			public boolean onCreateWindow(WebView view, boolean isDialog,
					boolean isUserGesture, Message resultMsg) {
				if(DEBUG) Log.e(TAG,"onCreateWindow"+(isDialog?" isDialog":"")+(isUserGesture?" isUserGesture":""));
				return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
			}

			@Override
			public boolean onJsTimeout() {
				if(DEBUG) Log.e(TAG,"onJsTimeout");
				return super.onJsTimeout();
			}

			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				super.onProgressChanged(view, newProgress);
				if(DEBUG) Log.e(TAG,"onProgressChanged: "+newProgress);
				setProgress(newProgress/2+50);//second half of progress bar
			}
		});

        mThreadView.addJavascriptInterface(clickInterface, "listener");

        refreshSessionCookie();
        mThreadView.loadDataWithBaseURL(Constants.BASE_URL + "/", getBlankPage(), "text/html", "utf-8", null);
	}
	
	public void updatePageBar(){
		mPageCountText.setText("Page " + getPage() + "/" + (getLastPage() > 0 ? getLastPage() : "?"));
		if(getActivity() != null){
			invalidateOptionsMenu();
		}
		mRefreshBar.setVisibility(View.VISIBLE);
		mPrevPage.setVisibility(View.VISIBLE);
		mNextPage.setVisibility(View.VISIBLE);
		if (getPage() <= 1) {
			mPrevPage.setImageResource(R.drawable.ic_refresh);
			mPrevPage.setVisibility(View.VISIBLE);
			mRefreshBar.setVisibility(View.INVISIBLE);
		} else {
			mPrevPage.setImageResource(R.drawable.ic_arrow_back);
		}

		if (getPage() == getLastPage()) {
			mNextPage.setImageResource(R.drawable.ic_refresh);
			mRefreshBar.setVisibility(View.INVISIBLE);
		} else {
			mNextPage.setImageResource(R.drawable.ic_arrow_forward);
		}

        if(mThreadView != null){
            if(mPrefs.disablePullNext){
                mSRL.setOnRefreshListener(null);
            }else{
                mSRL.setOnRefreshListener(this);
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
				Intent openThread = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.FUNCTION_BANLIST + '?' + Constants.PARAM_USER_ID + "=" + mPrefs.userId));
				startActivity(openThread);
			}
		});
	}
    

    @Override
    public void onResume() {
        super.onResume(); if(DEBUG) Log.e(TAG, "Resume");
        resumeWebView();
		if(mThreadView != null){
			mThreadView.loadUrl("javascript:loadpagehtml(true)");
		}
        getActivity().getContentResolver().registerContentObserver(AwfulThread.CONTENT_URI, true, mThreadObserver);
        refreshInfo();

//        if(isFragmentVisible() && mP2RAttacher != null){
//            mP2RAttacher.setPullFromBottom(true);
//        }
    }

    @SuppressLint("NewApi")
    public void resumeWebView(){
    	if(getActivity() != null){
	        if (mThreadView == null) {
	            //recreateWebview();
	        }else{
	            mThreadView.onResume();
	            mThreadView.resumeTimers();
	        }
    	}
    }
    
	@Override
	public void onPageVisible() {
        resumeWebView();
        if(mThreadView != null){
        	mThreadView.setKeepScreenOn(keepScreenOn);
        }
        if(parent != null && mParentForumId != 0){
			parent.setNavIds(mParentForumId, getThreadId());
        }
	}

	@Override
	public void onPageHidden() {
        pauseWebView();
        if(mThreadView != null){
        	mThreadView.setKeepScreenOn(false);
        }
	}
	
    @Override
    public void onPause() {
        super.onPause(); if(DEBUG) Log.e(TAG, "onPause");
        getActivity().getContentResolver().unregisterContentObserver(mThreadObserver);
        getLoaderManager().destroyLoader(Constants.THREAD_INFO_LOADER_ID);
        pauseWebView();
    }

    @SuppressLint("NewApi")
    private void pauseWebView(){
        if (mThreadView != null) {
        	mThreadView.pauseTimers();
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

    
    public synchronized void refreshSessionCookie(){
        if(mThreadView != null){
        	if(DEBUG) Log.e(TAG,"SETTING COOKIES");
        	CookieSyncManager.createInstance(getActivity());
        	CookieManager ckiemonster = CookieManager.getInstance();
        	ckiemonster.removeAllCookie();
        	ckiemonster.setCookie(Constants.COOKIE_DOMAIN, NetworkUtils.getCookieString(Constants.COOKIE_NAME_SESSIONID));
        	ckiemonster.setCookie(Constants.COOKIE_DOMAIN, NetworkUtils.getCookieString(Constants.COOKIE_NAME_SESSIONHASH));
        	ckiemonster.setCookie(Constants.COOKIE_DOMAIN, NetworkUtils.getCookieString(Constants.COOKIE_NAME_USERID));
        	ckiemonster.setCookie(Constants.COOKIE_DOMAIN, NetworkUtils.getCookieString(Constants.COOKIE_NAME_PASSWORD));
        	CookieSyncManager.getInstance().sync();
        }
    }
 
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	if(DEBUG) Log.e(TAG, "onCreateOptionsMenu");
    	menu.clear();
    	if(menu.size() == 0){
    		inflater.inflate(R.menu.post_menu, menu);
        	MenuItem share = menu.findItem(R.id.share_thread);
        	if(share != null && MenuItemCompat.getActionProvider(share) instanceof ShareActionProvider){
        		shareProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(share);
        		shareProvider.setShareIntent(createShareIntent());
        	}
    	}
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    	if(DEBUG) Log.e(TAG, "onPrepareOptionsMenu");
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
    
    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if(DEBUG) Log.e(TAG, "onOptionsItemSelected");
        switch(item.getItemId()) {
			case R.id.close:
				toggleCloseThread();
				break;
			case R.id.reply:
				displayPostReplyDialog();
				break;
            case R.id.next_page:
            	goToPage(getPage() + 1);
                break;
    		case R.id.rate_thread:
    			rateThread();
    			break;
    		case R.id.copy_url:
    			copyThreadURL(null);
    			break;
    		case R.id.find:
    			this.mThreadView.showFindDialog(null, true);
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

	private String generateThreadUrl(String postId){
    	StringBuilder url = new StringBuilder();
		url.append(Constants.FUNCTION_THREAD);
		url.append("?");
		url.append(Constants.PARAM_THREAD_ID);
		url.append("=");
		url.append(getThreadId());
		url.append("&");
		url.append(Constants.PARAM_PAGE);
		url.append("=");
		url.append(getPage());
		url.append("&");
		url.append(Constants.PARAM_PER_PAGE);
		url.append("=");
		url.append(mPrefs.postPerPage);
		if (postId != null) {
			url.append("#");
			url.append("post");
			url.append(postId);
		}
		return url.toString();
    }
	
	private String generatePostUrl(String postId){
		return Constants.FUNCTION_THREAD + "?" + Constants.PARAM_GOTO + "=" + Constants.VALUE_POST + "&" + Constants.PARAM_POST_ID + "=" + postId;
    }
    
    private Intent createShareIntent(){
    	return new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, mTitle).putExtra(Intent.EXTRA_TEXT, generateThreadUrl(null));
    }
    
    protected Intent createShareIntent(String url){
    	return new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url);
    }

	protected void copyThreadURL(String postId) {
		String clipLabel = this.getText(R.string.copy_url).toString() + getPage();
		String clipText  = generateThreadUrl(postId);
		safeCopyToClipboard(clipLabel, clipText, R.string.copy_url_success);
	}

	private void rateThread() {

		final CharSequence[] items = { "1", "2", "3", "4", "5" };

		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		builder.setTitle("Rate this thread");
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				queueRequest(new VoteRequest(getActivity(), getThreadId(), item).build(ThreadDisplayFragment.this, new AwfulRequest.AwfulResultCallback<Void>() {
					@Override
					public void success(Void result) {
						displayAlert(R.string.vote_succeeded, R.string.vote_succeeded_sub, R.drawable.ic_mood);
					}

					@Override
					public void failure(VolleyError error) {
					}
				}));
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	
	protected void ignoreUser(final String aUserId) {
		if(mPrefs.ignoreFormkey == null){
            queueRequest(new ProfileRequest(getActivity(), null).build());
		}
		if(mPrefs.showIgnoreWarning){
		AlertDialog ignoreDialog = new AlertDialog.Builder(getAwfulActivity()).create();
		ignoreDialog.setButton(AlertDialog.BUTTON_POSITIVE, getActivity().getString(R.string.confirm), new android.content.DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
                queueRequest(new IgnoreRequest(getActivity(), aUserId).build());//we don't care about status callbacks for this, so we use the build() that doesn't do callbacks
			}
		});
		ignoreDialog.setButton(AlertDialog.BUTTON_NEGATIVE,getActivity().getString(R.string.cancel), (android.content.DialogInterface.OnClickListener) null);
		ignoreDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getActivity().getString(R.string.dont_show_again), new android.content.DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
                try{
					mPrefs.setPreference(Keys.SHOW_IGNORE_WARNING, false);
                }catch(Exception e){
                    e.printStackTrace();
                }
                queueRequest(new IgnoreRequest(getActivity(), aUserId).build());//we don't care about status callbacks for this, so we use the build() that doesn't do callbacks
			}
		});
		ignoreDialog.setTitle(R.string.ignore_title);
		ignoreDialog.setMessage(getActivity().getString(R.string.ignore_message));
		ignoreDialog.show();
		
		}else{
            queueRequest(new IgnoreRequest(getActivity(), aUserId).build());//we don't care about status callbacks for this, so we use the build() that doesn't do callbacks
		}
	}
    
	protected void toggleMarkUser(String username){
        if(mPrefs.markedUsers.contains(username)){
            mPrefs.unmarkUser(username);
        }else{
            mPrefs.markUser(username);
        }
	}

	protected void toggleUserPosts(String aPostId, String aUserId, String aUsername){
		if(mUserId >0){
			deselectUser(aPostId);
		}else{
			selectUser(Integer.parseInt(aUserId), aUsername);
		}
	}
	
	protected void reportUser(final String postid){
		final EditText reportReason = new EditText(this.getActivity());

		new AlertDialog.Builder(this.getActivity())
		  .setTitle("Report inappropriate post")
		  .setMessage("Did this post break the forum rules? If so, please report it by clicking below. If you would like to add any comments explaining why you submitted this post, please do so here:")
		  .setView(reportReason)
		  .setPositiveButton("Report", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
		      String reason = reportReason.getText().toString();
		      queueRequest(new ReportRequest(getActivity(), postid, reason).build(ThreadDisplayFragment.this, new AwfulRequest.AwfulResultCallback<String>() {
                  @Override
                  public void success(String result) {
                      displayAlert(result, R.drawable.ic_mood);
                  }

                  @Override
                  public void failure(VolleyError error) {
                      displayAlert(error.getMessage(), R.drawable.ic_mood);
                  }
              }));
		    }
		  })
		  .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
		    }
		  })
		  .show(); 
	}
	
    @Override
    public void onSaveInstanceState(Bundle outState){
    	super.onSaveInstanceState(outState);
    	if(DEBUG) Log.v(TAG,"onSaveInstanceState");
        if(bodyHtml != null && bodyHtml.length() > 0){
            outState.putString("threadHtml", bodyHtml);
        }
    	if(mThreadView != null){
    		outState.putInt("scroll_position", mThreadView.getScrollY());
    	}
    }
    
    private void syncThread() {
        if(getActivity() != null){
			// cancel pending post loading requests
			NetworkUtils.cancelRequests(PostRequest.REQUEST_TAG);
        	bodyHtml = "";
			// call this with cancelOnDestroy=false to retain the request's specific type tag
            queueRequest(new PostRequest(getActivity(), getThreadId(), getPage(), mUserId).build(this, new AwfulRequest.AwfulResultCallback<Integer>() {
				@Override
				public void success(Integer result) {
					refreshInfo();
					if (result == getPage()) {
						setProgress(75);
						refreshPosts();
						mNextPage.setColorFilter(0);
						mPrevPage.setColorFilter(0);
						mRefreshBar.setColorFilter(0);
					} else {
						Log.e(TAG, "Page mismatch: " + getPage() + " - " + result);
					}
				}

				@Override
				public void failure(VolleyError error) {
					if (null != error.getMessage() && error.getMessage().startsWith("java.net.ProtocolException: Too many redirects")) {
						Log.e(TAG, "Error: " + error.getMessage());
						Log.e(TAG, "!!!Failed to sync thread - You are now LOGGED OUT");
						NetworkUtils.clearLoginCookies(getAwfulActivity());
						getAwfulActivity().startActivity(new Intent().setClass(getAwfulActivity(), AwfulLoginActivity.class));
					}
					refreshInfo();
					refreshPosts();
					mNextPage.setColorFilter(0);
					mPrevPage.setColorFilter(0);
					mRefreshBar.setColorFilter(0);
				}
			}), false);
        }
    }
    
    protected void markLastRead(int index) {
        displayAlert(R.string.mark_last_read_progress, R.string.please_wait_subtext, R.drawable.ic_visibility);
        queueRequest(new MarkLastReadRequest(getActivity(), getThreadId(), index).build(null, new AwfulRequest.AwfulResultCallback<Void>() {
            @Override
            public void success(Void result) {
                displayAlert(R.string.mark_last_read_success, 0, R.drawable.ic_visibility);
                refreshInfo();
                refreshPosts();
            }

            @Override
            public void failure(VolleyError error) {

            }
        }));
    }

	private void toggleThreadBookmark() {
		if(getActivity() != null){
			queueRequest(new BookmarkRequest(getActivity(), getThreadId(), !threadBookmarked).build(this, new AwfulRequest.AwfulResultCallback<Void>() {
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

	private void toggleYospos() {
		mPrefs.amberDefaultPos = !mPrefs.amberDefaultPos;
		mPrefs.setPreference(Keys.AMBER_DEFAULT_POS, mPrefs.amberDefaultPos);
		mThreadView.loadUrl("javascript:changeCSS('"+AwfulUtils.determineCSS(mParentForumId)+"')");
	}
    
    private void startPostRedirect(final String postUrl) {
        if(getActivity() != null){
            if(redirect != null){
                redirect.cancel(false);
            }
            setProgress(50);
            redirect = new RedirectTask(postUrl){
                @Override
                protected void onPostExecute(String url) {
                    if(!isCancelled()){
                        if(url != null){
                            AwfulURL result = AwfulURL.parse(url);
                            if(postUrl.contains(Constants.VALUE_LASTPOST)){
                                //This is a workaround for how the forums handle the perPage value with goto=lastpost.
                                //The redirected url is lacking the perpage=XX value.
                                //We just override the assumed (40) with the number we requested when starting the redirect.
                                //I gotta ask chooch to fix this at some point.
                                result.setPerPage(mPrefs.postPerPage);
                            }
                            if(result.getType() == TYPE.THREAD){
                                if(bypassBackStack){
                                    openThread((int) result.getId(), (int) result.getPage(mPrefs.postPerPage), result.getFragment().replaceAll("\\D", ""));
                                }else{
                                    pushThread((int) result.getId(), (int) result.getPage(mPrefs.postPerPage), result.getFragment().replaceAll("\\D", ""));
                                }
                            }
							if(result.getType() == TYPE.INDEX) {
								getAwfulActivity().displayForumIndex();
							}
                        }else{
                            displayAlert(new AwfulError());
                        }
                        redirect = null;
                        bypassBackStack = false;
                        setProgress(100);
                    }
                }
            }.execute();
        }
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
                            displayAlert(R.string.invalid_page);
                        } catch (Exception e) {
                            Log.d(TAG, e.toString());
                        }
                    }
                })
            .setNegativeButton("Cancel", null)
            .show();
        
    }
    
    @Override
    public void onActivityResult(int aRequestCode, int aResultCode, Intent aData) {
    	Log.e(TAG,"onActivityResult: " + aRequestCode+" result: "+aResultCode);
        // If we're here because of a post result, refresh the thread
        switch (aRequestCode) {
            case PostReplyFragment.REQUEST_POST:
            	if(aResultCode == PostReplyFragment.RESULT_POSTED){
            		//startPostRedirect(Constants.FUNCTION_THREAD+"?goto=lastpost&threadid="+getThreadId()+"&perpage="+mPrefs.postPerPage);
            		bypassBackStack = true;
            		startPostRedirect(AwfulURL.threadLastPage(getThreadId(), mPrefs.postPerPage).getURL(mPrefs.postPerPage));
            	}else if(aResultCode > 100){//any result >100 it is a post id we edited
            		//startPostRedirect(Constants.FUNCTION_THREAD+"?goto=post&postid="+aResultCode+"&perpage="+mPrefs.postPerPage);
            		bypassBackStack = true;
            		startPostRedirect(AwfulURL.post(aResultCode, mPrefs.postPerPage).getURL(mPrefs.postPerPage));
            	}
                break;
        }
    }

    public void refresh() {
    	if(mThreadView != null){
            bodyHtml = "";
            mThreadView.loadUrl("javascript:loadpagehtml()");
    		//mThreadView.loadData(getBlankPage(), "text/html", "utf-8");
    	}
        syncThread();
    }

    public void nextPageClick() {
        if (getPage() == getLastPage()) {
            refresh();
        } else {
            goToPage(getPage() + 1);
        }
    }

    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
				case R.id.next_page:
					nextPageClick();
					break;
                case R.id.prev_page:
                	if (getPage() <= 1) {
            			refresh();
            		} else {
                    	goToPage(getPage() - 1);
            		}
                    break;
                case R.id.refresh:
                	refresh();
                    break;
				case R.id.page_count:
					displayPagePicker();
					break;
                case R.id.toggle_sidebar:
                    if (getPage() == getLastPage()) {
                        refresh();
                    } else {
                        goToPage(getPage() + 1);
                    }
                	break;
				case R.id.just_post:
					displayPostReplyDialog();
					break;
            }
        }
    };

    public void displayPostReplyDialog() {
        displayPostReplyDialog(getThreadId(), -1, AwfulMessage.TYPE_NEW_REPLY);
    }
	protected void toggleCloseThread(){
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

    @SuppressWarnings("unused")
	private void populateThreadView(ArrayList<AwfulPost> aPosts) {
		updatePageBar();
		updateProbationBar();

        try {

            String html = AwfulThread.getHtml(aPosts, AwfulPreferences.getInstance(getActivity()), getPage(), mLastPage, mParentForumId, threadClosed);
            refreshSessionCookie();
            bodyHtml = html;
            mThreadView.loadUrl("javascript:loadpagehtml()");
            setProgress(100);
            //mThreadView.loadDataWithBaseURL(Constants.BASE_URL + "/", html, "text/html", "utf-8", null);
        } catch (Exception e) {
        	e.printStackTrace();
            // If we've already left the activity the webview may still be working to populate,
            // just log it
        }
        Log.i(TAG,"Finished populateThreadView, posts:"+aPosts.size());
    }
    
    private ClickInterface clickInterface = new ClickInterface();

	@Override
	public void onRefresh(SwipyRefreshLayoutDirection swipyRefreshLayoutDirection) {
		if(swipyRefreshLayoutDirection == SwipyRefreshLayoutDirection.TOP){
			refresh();
		}else{
			if(getPage() < mLastPage){
				goToPage(getPage()+1);
			}else{
				refresh();
			}
		}
	}


	private class ClickInterface {

        public ClickInterface(){
        	this.preparePreferences();
        }
        
        HashMap<String,String> preferences;


        @JavascriptInterface
        public void onMoreClick(final String aPostId, final String aUsername, final String aUserId, final String lastreadurl, final boolean editable, final boolean isAdminOrMod, final boolean isPlat) {
			PostActionsFragment postActions = new PostActionsFragment();
			postActions.setTitle("Select an Action");
			postActions.setParent(mSelf);
			postActions.setPostId(aPostId);
			postActions.setUsername(aUsername);
			postActions.setUserId(aUserId);
			postActions.setThreadId(getThreadId());
			postActions.setLastReadUrl(lastreadurl);
			postActions.setActions(AwfulAction.getPostActions(aUsername, editable, isAdminOrMod, isPlat));

			postActions.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
			postActions.show(mSelf.getFragmentManager(), "Post Actions");
		}

        @JavascriptInterface
        public void debugMessage(final String msg) {
        	Log.e(TAG, "Awful DEBUG: " + msg);
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
            return AwfulUtils.determineCSS(mParentForumId);
        }
        
        private void preparePreferences(){
        	AwfulPreferences aPrefs = AwfulPreferences.getInstance();
        	
            preferences = new HashMap<String,String>();
            preferences.clear();
            preferences.put("username", aPrefs.username);
			preferences.put("youtubeHighlight", "#ff00ff");
			preferences.put("showSpoilers", Boolean.toString(aPrefs.showAllSpoilers));
			preferences.put("postFontSize", Integer.toString(aPrefs.postFontSizePx));
			preferences.put("postcolor", ColorProvider.convertToARGB(ColorProvider.getTextColor()));
			preferences.put("backgroundcolor", ColorProvider.convertToARGB(ColorProvider.getBackgroundColor()));
			preferences.put("linkQuoteColor", ColorProvider.convertToARGB(aPrefs.getResources().getColor(R.color.link_quote)));
			preferences.put("highlightUserQuote", Boolean.toString(aPrefs.highlightUserQuote));
			preferences.put("highlightUsername", Boolean.toString(aPrefs.highlightUsername));
			preferences.put("inlineTweets", Boolean.toString(aPrefs.inlineTweets));
			preferences.put("inlineWebm", Boolean.toString(aPrefs.inlineWebm));
			preferences.put("autostartWebm", Boolean.toString(aPrefs.autostartWebm));
			preferences.put("inlineVines", Boolean.toString(aPrefs.inlineVines));
			preferences.put("postjumpid", mPostJump);
			preferences.put("scrollPosition", Integer.toString(savedScrollPosition));
            preferences.put("disableGifs", Boolean.toString(aPrefs.disableGifs));
            preferences.put("hideSignatures", Boolean.toString(aPrefs.hideSignatures));
            preferences.put("disablePullNext",Boolean.toString(aPrefs.disablePullNext));
        }

		@JavascriptInterface
		public void loadIgnoredPost(final String ignorePost){
			if(getActivity() != null){
				queueRequest(new SinglePostRequest(getActivity(), ignorePost).build(mSelf, new AwfulRequest.AwfulResultCallback<String>() {
					@Override
					public void success(String result) {
						ignorePostsHtml.put(ignorePost,result);
						mThreadView.loadUrl("javascript:insertIgnoredPost('"+ignorePost+"')");
					}

					@Override
					public void failure(VolleyError error) {
						Log.e(TAG,"Loading Single post #"+ignorePost+" failed");
					}
				}));
			}
		}

		@JavascriptInterface
		public String getPreference(String preference) {
			return preferences.get(preference);
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

		boolean isImage = false;
		boolean isGif = false;
		// TODO: parsing fails on magic webdev urls like http://tpm2016.zoffix.com/#/40
		// it thinks the # is the start of the ref section of the url, so the Path for that url is '/'
		String lastSegment = Uri.parse(url).getLastPathSegment();
		// null-safe path checking (there may be no path segments, e.g. a link to a domain name)
		if (lastSegment != null) {
			lastSegment = lastSegment.toLowerCase();
			// using 'contains' instead of 'ends with' in case of any url suffix shenanigans, like twitter's ".jpg:large"
			isImage = StringUtils.indexOfAny(lastSegment, ".jpg", ".jpeg", ".png", ".gif") != -1
					&& !StringUtils.contains(lastSegment, ".gifv");
			isGif = StringUtils.contains(lastSegment, ".gif")
					&& !StringUtils.contains(lastSegment, ".gifv");
		}

		PostActionsFragment postActions = new PostActionsFragment();
		postActions.setTitle(url);
		postActions.setParent(mSelf);
		postActions.setUrl(url);
		postActions.setActions(AwfulAction.getURLActions(url, isImage, isGif));

		postActions.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
		postActions.show(mSelf.getFragmentManager(), "Link Actions");
	}

	protected void showImageInline(String url){
		if(mThreadView != null){
			mThreadView.loadUrl("javascript:showInlineImage('"+url+"')");
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
		request.setShowRunningNotification(true);
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, link.getLastPathSegment());
		request.allowScanningByMediaScanner();
		DownloadManager dlMngr= (DownloadManager) getAwfulActivity().getSystemService(AwfulActivity.DOWNLOAD_SERVICE);
		dlMngr.enqueue(request);
	}

	protected void copyToClipboard(String text){
		safeCopyToClipboard("Copied URL", text, null);
	}

	protected void startUrlIntent(String url){
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		PackageManager pacman = getActivity().getPackageManager();
		List<ResolveInfo> res = pacman.queryIntentActivities(browserIntent,
				PackageManager.MATCH_DEFAULT_ONLY);
		if (res.size() > 0) {
			browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getActivity().startActivity(browserIntent);
		} else {
			String[] split = url.split(":");
			displayAlert("Cannot open link:","No application found for protocol" + (split.length > 0 ? ": " + split[0] : "."));
		}
	}
	
	@Override
	public void onPreferenceChange(AwfulPreferences mPrefs, String key) {
		super.onPreferenceChange(mPrefs, key);
		if(DEBUG) Log.i(TAG,"onPreferenceChange"+((key != null)?":"+key:""));
        if(null != getAwfulActivity()){
		    getAwfulActivity().setPreferredFont(mPageCountText);
        }
		if(mPageCountText != null){
			mPageCountText.setTextColor(ColorProvider.getActionbarFontColor());
		}
		if(mThreadView != null){
			mThreadView.setBackgroundColor(Color.TRANSPARENT);
			//mThreadView.loadUrl("javascript:changeCSS('"+AwfulUtils.determineCSS(mParentForumId)+"')");
            mThreadView.loadUrl("javascript:changeFontFace('"+mPrefs.preferredFont+"')");
            mThreadView.getSettings().setDefaultFontSize(mPrefs.postFontSizeDip);
            mThreadView.getSettings().setDefaultFixedFontSize(mPrefs.postFixedFontSizeDip);

			if("marked_users".equals(key)){
				mThreadView.loadUrl("javascript:updateMarkedUsers('"+TextUtils.join(",",mPrefs.markedUsers)+"')");
			}
		}
		if(clickInterface != null){
			clickInterface.preparePreferences();
		}
		if(mFAB != null) {
			mFAB.setVisibility((mPrefs.noFAB?View.GONE:View.VISIBLE));
		}
	}

	public void setPostJump(String postID) {
		mPostJump = postID;
	}
	
	public void goToPage(int aPage){
		if(aPage > 0 && aPage <= getLastPage()){
			setPage(aPage);
			updatePageBar();
			updateProbationBar();
			mPostJump = "";
            bodyHtml = "";
			if(mThreadView != null){
                mThreadView.loadUrl("javascript:loadpagehtml()");
			}
	        syncThread();
		}
	}
	
	private String getBlankPage(){
		return AwfulThread.getContainerHtml(mPrefs, getParentForumId());
	}

    public int getLastPage() {
        return mLastPage;
    }

    public int getThreadId() {
        return parent.getThreadId();
    }
	
	public int getPage() {
        return parent.getThreadPage();
	}
	public void setPage(int aPage){
		parent.setThread(null, aPage);
	}
	public void setThreadId(int aThreadId){
        parent.setThread(aThreadId, null);
	}
	
	public void selectUser(int id, String name){
		savedPage = getPage();
		mUserId = id;
        mPostByUsername = name;
		setPage(1);
		mLastPage = 1;
		mPostJump = "";
        bodyHtml = "";
		if(mThreadView != null){
            this.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mThreadView.loadUrl("javascript:loadpagehtml()");
                }
            });

		}
        syncThread();
	}
	
	public void deselectUser(String postId){
        bodyHtml = "";
        if(mThreadView != null){
            this.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
					mUserPostNotice.setVisibility(View.GONE);
                    mThreadView.loadUrl("javascript:loadpagehtml()");
                }
            });

        }
		if(TextUtils.isEmpty(postId) || postId.length() < 3){
			mUserId = 0;
            mPostByUsername = null;
			setPage(savedPage);
			mLastPage = 0;
			mPostJump = "";
			syncThread();
		}else{
	        openThread(AwfulURL.parse(generatePostUrl(postId)));
		}
	}

    private class PostLoaderManager implements LoaderManager.LoaderCallbacks<Cursor> {
        private final static String sortOrder = AwfulPost.POST_INDEX + " ASC";
        private final static String selection = AwfulPost.THREAD_ID + "=? AND " + AwfulPost.POST_INDEX + ">=? AND " + AwfulPost.POST_INDEX + "<?";
        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            int index = AwfulPagedItem.pageToIndex(getPage(), mPrefs.postPerPage, 0);
            Log.v(TAG,"Displaying thread: "+getThreadId()+" index: "+index+" page: "+getPage()+" perpage: "+mPrefs.postPerPage);
            return new CursorLoader(getActivity(),
            						AwfulPost.CONTENT_URI,
            						AwfulProvider.PostProjection,
            						selection,
            						AwfulProvider.int2StrArray(getThreadId(), index, index+mPrefs.postPerPage),
            						sortOrder);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.i(TAG,"Load finished, page:"+getPage()+", populating: "+aData.getCount());
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
    
    private class ThreadDataCallback implements LoaderManager.LoaderCallbacks<Cursor> {

        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            return new CursorLoader(getActivity(), ContentUris.withAppendedId(AwfulThread.CONTENT_URI, getThreadId()), 
            		AwfulProvider.ThreadProjection, null, null, null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"Thread title finished, populating.");
        	if(aData.getCount() >0 && aData.moveToFirst()){
        		mLastPage = AwfulPagedItem.indexToPage(aData.getInt(aData.getColumnIndex(AwfulThread.POSTCOUNT)),mPrefs.postPerPage);
				threadClosed = aData.getInt(aData.getColumnIndex(AwfulThread.LOCKED))>0;
				threadOpenClose = aData.getInt(aData.getColumnIndex(AwfulThread.CAN_OPEN_CLOSE))>0;
        		threadBookmarked = aData.getInt(aData.getColumnIndex(AwfulThread.BOOKMARKED))>0;
                threadArchived = aData.getInt(aData.getColumnIndex(AwfulThread.ARCHIVED))>0;
        		mParentForumId = aData.getInt(aData.getColumnIndex(AwfulThread.FORUM_ID));
				if(mParentForumId != 0 && mThreadView != null){
					mThreadView.loadUrl("javascript:changeCSS('"+AwfulUtils.determineCSS(mParentForumId)+"')");
				}

				parent.setNavIds(mParentForumId, getThreadId());
                setTitle(aData.getString(aData.getColumnIndex(AwfulThread.TITLE)));
        		updatePageBar();
        		updateProbationBar();
                if(mUserId > 0 && !TextUtils.isEmpty(mPostByUsername)){

					mUserPostNotice.setVisibility(View.VISIBLE);
					mUserPostNotice.setText("Viewing posts by " + mPostByUsername + " in this thread,\nPress the back button to return.");
					mUserPostNotice.setTextColor(ColorProvider.getTextColor());
					mUserPostNotice.setBackgroundColor(ColorProvider.getBackgroundColor());
                }else{
					mUserPostNotice.setVisibility(View.GONE);
                }
        		if(shareProvider != null){
        			shareProvider.setShareIntent(createShareIntent());
        		}
                invalidateOptionsMenu();
				mFAB.setVisibility((mPrefs.noFAB || threadClosed || threadArchived)?View.GONE:View.VISIBLE);
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
    
	public void refreshInfo() {
		restartLoader(Constants.THREAD_INFO_LOADER_ID, null, mThreadLoaderCallback);
	}
	
	public void refreshPosts(){
		restartLoader(Constants.POST_LOADER_ID, null, mPostLoaderCallback);
	}
	
	public void setTitle(String title){
		mTitle = title;
		if(getActivity() != null && mTitle != null){
            getAwfulActivity().setActionbarTitle(getTitle(), this);
		}
	}
	
	public String getTitle(){
		return mTitle;
	}

	public int getParentForumId() {
		return mParentForumId;
	}
	public void openThread(int id, int page){
    	clearBackStack();
    	loadThread(id, page, "");
	}
	public void openThread(int id, int page, String postJump){
    	clearBackStack();
    	loadThread(id, page, postJump);
	}
	public void openThread(AwfulURL url) {
    	clearBackStack();
    	if(url == null){
    		Toast.makeText(this.getActivity(), "Error occoured: URL was empty", Toast.LENGTH_LONG).show();
    	}
    	if(mPrefs == null){
    		mPrefs = AwfulPreferences.getInstance(getAwfulActivity(), this);
    	}
    	if(url.isRedirect()){
    		startPostRedirect(url.getURL(mPrefs.postPerPage));
    	}else{
    		loadThread((int) url.getId(), (int) url.getPage(mPrefs.postPerPage), url.getFragment());
    	}
	}
	
	private void loadThread(int id, int page, String postJump) {
        if(parent == null){
            return;
        }
    	if(getActivity() != null){
	        getLoaderManager().destroyLoader(Constants.THREAD_INFO_LOADER_ID);
	        getLoaderManager().destroyLoader(Constants.POST_LOADER_ID);
    	}
        setPage(page);
    	setThreadId(id);
    	//if the fragment isn't attached yet, just set the values and let the lifecycle handle it
		mUserId = 0;
        mPostByUsername = null;
    	bodyHtml = "";
    	mLastPage = 1;
    	if(postJump != null){
    		mPostJump = postJump;
    	}else{
    		mPostJump = "";
    	}
		updatePageBar();
		updateProbationBar();
    	if(getActivity() != null){
    		if(mThreadView != null){
                mThreadView.loadUrl("javascript:loadpagehtml()");
    		}
			refreshInfo();
			syncThread();
    	}
	}
	
	private void loadThread(AwfulStackEntry thread) {
    	if(getActivity() != null){
	        getLoaderManager().destroyLoader(Constants.THREAD_INFO_LOADER_ID);
	        getLoaderManager().destroyLoader(Constants.POST_LOADER_ID);
    	}
    	setThreadId(thread.id);//if the fragment isn't attached yet, just set the values and let the lifecycle handle it
		mUserId = 0;
        mPostByUsername = null;
    	setPage(thread.page);
        bodyHtml = "";
    	mLastPage = 1;
    	mPostJump = "";
    	savedScrollPosition = thread.scrollPos;
		updatePageBar();
		updateProbationBar();
    	if(getActivity() != null){
    		if(mThreadView != null){
                mThreadView.loadUrl("javascript:loadpagehtml()");
    		}
			refreshInfo();
			refreshPosts();
    	}
	}
	
	private static class AwfulStackEntry{
		public int id, page, scrollPos;
		public AwfulStackEntry(int threadId, int pageNum, int scrollPosition){
			id = threadId; page = pageNum; scrollPos = scrollPosition;
		}
	}
	
	private void pushThread(int id, int page, String postJump){
		if(mThreadView != null && getThreadId() != 0){
			backStack.addFirst(new AwfulStackEntry(getThreadId(), getPage(), mThreadView.getScrollY()));
		}
		loadThread(id, page, postJump);
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
		}else if(mUserId > 0){
            deselectUser(null);
            return true;
        }else{
			return false;
		}
	}


	@Override
	protected boolean doScroll(boolean down) {
		if (down) {
			mThreadView.pageDown(false);
		} else {
			mThreadView.pageUp(false);
		}
		return true;
	}


	private void toggleScreenOn() {
    	keepScreenOn = !keepScreenOn;
    	mThreadView.setKeepScreenOn(keepScreenOn);

        //TODO icon
		displayAlert( keepScreenOn? "Screen stays on" :"Screen turns itself off");
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
