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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.LoaderManager;
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
import android.webkit.*;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebSettings.RenderPriority;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.ColorPickerPreference;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.task.*;
import com.ferg.awfulapp.thread.*;
import com.ferg.awfulapp.thread.AwfulURL.TYPE;
import com.ferg.awfulapp.util.AwfulError;
import com.ferg.awfulapp.util.AwfulGifStripper;
import com.ferg.awfulapp.util.AwfulUtils;
import com.ferg.awfulapp.widget.NumberPicker;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.WebViewDelegate;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

/**
 * Uses intent extras:
 *  TYPE - STRING ID - DESCRIPTION
 *	int - Constants.THREAD_ID - id number for that thread
 *	int - Constants.THREAD_PAGE - page number to load
 *
 *  Can also handle an HTTP intent that refers to an SA showthread.php? url.
 */
public class ThreadDisplayFragment extends AwfulFragment implements PullToRefreshAttacher.OnRefreshListener {
    private static final boolean OUTPUT_HTML = false;

    private PostLoaderManager mPostLoaderCallback;
    private ThreadDataCallback mThreadLoaderCallback;
    
    private ImageButton mNextPage;
    private ImageButton mPrevPage;
    private ImageButton mRefreshBar;
    private TextView mPageCountText;
    
    private View mProbationBar;
	private TextView mProbationMessage;
	private ImageButton mProbationButton;

    private WebView mThreadView;

    private int mUserId = 0;
    private String mPostByUsername;
    private int mLastPage = 0;
    private int mParentForumId = 0;
    private boolean threadClosed = false;
    private boolean threadBookmarked = false;
    private boolean threadArchived = false;
    
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
    private AsyncTask<Void, Void, String> redirect = null;

    public ThreadDisplayFragment() {
        super();
        TAG = "ThreadDisplayFragment";
    }

    private ThreadContentObserver mThreadObserver = new ThreadContentObserver(mHandler);

    
	
	private WebViewClient callback = new WebViewClient(){
        
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        	if(DEBUG) Log.e(TAG, "Opening Connection: "+url);
            if(mPrefs.disableGifs && url != null && url.endsWith(".gif") && !url.contains("ytimg.") && !url.contains("i.imgur.com")){
                    return new WebResourceResponse("image/png","png", new AwfulGifStripper(url, mSelf));
            }
            return null;
        }

        @Override
		public void onPageFinished(WebView view, String url) {
			Log.e(TAG,"PageFinished");
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

        
		mPageCountText = aq.find(R.id.page_count).clicked(onButtonClick).getTextView();
		getAwfulActivity().setPreferredFont(mPageCountText);
		mNextPage = (ImageButton) aq.find(R.id.next_page).clicked(onButtonClick).getView();
		mPrevPage = (ImageButton) aq.find(R.id.prev_page).clicked(onButtonClick).getView();
        mRefreshBar  = (ImageButton) aq.find(R.id.refresh).clicked(onButtonClick).getView();
		mThreadView = (WebView) result.findViewById(R.id.thread);
        initThreadViewProperties();
		mProbationBar = (View) result.findViewById(R.id.probationbar);
		mProbationMessage = (TextView) result.findViewById(R.id.probation_message);
		mProbationButton  = (ImageButton) result.findViewById(R.id.go_to_LC);
		updateProbationBar();

		return result;
	}

	@Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState); Log.e(TAG, "onActivityCreated");
        mP2RAttacher = this.getAwfulActivity().getPullToRefreshAttacher();
        if(mP2RAttacher != null){
            mP2RAttacher.addRefreshableView(mThreadView,new WebViewDelegate(), this);
            mP2RAttacher.setPullFromBottom(true);
            mP2RAttacher.setEnabled(true);
        }

		updatePageBar();
		updateProbationBar();
	}

	private void initThreadViewProperties() {
		mThreadView.resumeTimers();
		mThreadView.setWebViewClient(callback);
		mThreadView.setBackgroundColor(ColorProvider.getBackgroundColor());
		mThreadView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		mThreadView.getSettings().setJavaScriptEnabled(true);
		mThreadView.getSettings().setRenderPriority(RenderPriority.LOW);
        mThreadView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
        mThreadView.getSettings().setDefaultFontSize(mPrefs.postFontSizeDip);
        if(mPrefs.inlineYoutube){//YOUTUBE SUPPORT BLOWS
        	mThreadView.getSettings().setPluginState(PluginState.ON_DEMAND);
        }

		if (AwfulUtils.isHoneycomb()) {
			mThreadView.getSettings().setEnableSmoothTransition(true);
			if(!mPrefs.enableHardwareAcceleration){
				mThreadView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
			}
		}

		mThreadView.setWebChromeClient(new WebChromeClient() {
			public void onConsoleMessage(String message, int lineNumber, String sourceID) {
				if(DEBUG) Log.d("Web Console", message + " -- From line " + lineNumber + " of " + sourceID);
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
		mPageCountText.setText("Page " + getPage() + "/" + (getLastPage()>0?getLastPage():"?"));
		if(getActivity() != null){
			invalidateOptionsMenu();
		}
		mRefreshBar.setVisibility(View.VISIBLE);
		mPrevPage.setVisibility(View.VISIBLE);
		mNextPage.setVisibility(View.VISIBLE);
		if (getPage() <= 1) {
			mPrevPage.setImageResource(R.drawable.ic_actionbar_load);
			mPrevPage.setVisibility(View.VISIBLE);
			mRefreshBar.setVisibility(View.INVISIBLE);
		} else {
			mPrevPage.setImageResource(R.drawable.ic_menu_arrowleft);
		}

		if (getPage() == getLastPage()) {
			mNextPage.setImageResource(R.drawable.ic_actionbar_load);
			mRefreshBar.setVisibility(View.INVISIBLE);
		} else {
			mNextPage.setImageResource(R.drawable.ic_menu_arrowright);
		}

        if(mThreadView != null){
        	if(mP2RAttacher == null){
        		mP2RAttacher = this.getAwfulActivity().getPullToRefreshAttacher();
        	}
            if(mPrefs.disablePullNext){
                mP2RAttacher.setEnabled(false);
            }else{
                mP2RAttacher.setEnabled(true);
//                if(getPage() < mLastPage){
//                    footer.setPullLabel("Pull for Next Page...");
//                    footer.setReleaseLabel("Release for Next Page...");
//                    footer.setLoadingDrawable(getResources().getDrawable(R.drawable.grey_inline_arrowup));
//                }else{
//                    footer.setPullLabel("Pull to refresh...");
//                    footer.setReleaseLabel("Release to refresh...");
//                    footer.setLoadingDrawable(getResources().getDrawable(R.drawable.grey_inline_load));
//                }
            }
//            mThreadWindow.setHeaderBackgroundColor(mPrefs.postBackgroundColor2);
//            mThreadWindow.setTextColor(ColorProvider.getTextColor(mPrefs), ColorProvider.getAltTextColor(mPrefs));
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
        super.onStart(); if(DEBUG) Log.e(TAG, "onStart");
    }
    

    @Override
    public void onResume() {
        super.onResume(); if(DEBUG) Log.e(TAG, "Resume");
        resumeWebView();
        getActivity().getContentResolver().registerContentObserver(AwfulThread.CONTENT_URI, true, mThreadObserver);
        refreshInfo();
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
        if(mP2RAttacher != null){
        	mP2RAttacher.setPullFromBottom(true);
        }
	}

	@Override
	public void onPageHidden() {
        pauseWebView();
        if(mThreadView != null){
        	mThreadView.setKeepScreenOn(false);
        }
        if(mP2RAttacher != null){
        	mP2RAttacher.setPullFromBottom(false);
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
    public void onStop() {
        super.onStop(); if(DEBUG) Log.e(TAG, "onStop");
    }
    
    @Override
    public void onDestroyView(){
    	super.onDestroyView(); if(DEBUG) Log.e(TAG, "onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy(); if(DEBUG) Log.e(TAG, "onDestroy");
        getLoaderManager().destroyLoader(Constants.POST_LOADER_ID);
    }

    @Override
    public void onDetach() {
        super.onDetach(); if(DEBUG) Log.e(TAG, "onDetach");
    }

    
    public void refreshSessionCookie(){
        if(mThreadView != null){
        	if(DEBUG) Log.e(TAG,"SETTING COOKIES");
        	CookieSyncManager.createInstance(getActivity());
        	CookieManager ckiemonster = CookieManager.getInstance();
        	ckiemonster.removeSessionCookie();
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
        if(menu == null){
            return;
        }
        MenuItem nextArrow = menu.findItem(R.id.next_page);
        if(nextArrow != null){
        	nextArrow.setVisible(mPrefs.upperNextArrow);
        }
        MenuItem find = menu.findItem(R.id.find);
        if(find != null){
            find.setVisible(AwfulUtils.isHoneycomb());
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
        MenuItem re = menu.findItem(R.id.reply);
        if(re != null){
        	re.setEnabled(!threadClosed && !mPrefs.isOnProbation());
        	if(threadClosed){
        		re.setTitle("Thread Locked");
        	}else {
        		re.setTitle(R.string.post_reply);
        	}
        }
    }
    
    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if(DEBUG) Log.e(TAG, "onOptionsItemSelected");
        switch(item.getItemId()) {
            case R.id.next_page:
            	goToPage(getPage() + 1);
                break;
            case R.id.reply:
                displayPostReplyDialog();
                break;
            case R.id.usercp:
                displayUserCP();
                break;
            case R.id.go_to:
                displayPagePicker();
                break;
            case R.id.refresh:
                refresh();
                break;
            case R.id.settings:
                startActivity(new Intent().setClass(getActivity(), SettingsActivity.class));
                break;
            case R.id.bookmark:
            	toggleThreadBookmark();
                break;
    		case R.id.rate_thread:
    			rateThread();
    			break;
    		case R.id.copy_url:
    			copyThreadURL(null);
    			break;
    		case R.id.find:
                //Find button is hidden in onPrepareOptionsMenu for anything pre-Honeycomb
    			this.mThreadView.showFindDialog(null, true);
    			break;
    		case R.id.keep_screen_on:
    			this.toggleScreenOn();
    			break;
    		case R.id.thread_actions:
    			if(!AwfulUtils.isHoneycomb()){
    				fuckPreAPI11Forever();
    			}
    		default:
    			return super.onOptionsItemSelected(item);
    		}

    		return true;
    	}

	private void fuckPreAPI11Forever() {
        final CharSequence[] mThreadItems = {
        		getString(R.string.post_reply),
        		getString(R.string.bookmark),
        		getString(R.string.rate_thread),
        		getString(R.string.copy_url),
        		getString(R.string.share_thread),
        		getString(R.string.refresh),
        		getString(R.string.keep_screen_on),
            };
		
		
        	new AlertDialog.Builder(getActivity())
            .setTitle("Select an Action")
            .setItems(mThreadItems, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface aDialog, int aItem) {
                	switch(aItem) {
                    case 0:
                        displayPostReplyDialog();
                        break;

                    case 1:
                    	toggleThreadBookmark();
                        break;
            		case 2:
            			rateThread();
            			break;
            		case 3:
            			copyThreadURL(null);
            			break;
            		case 4:
            			startActivity(createShareIntent());
                    case 5:
                        refresh();
                        break;
            		case 6:
            			toggleScreenOn();
            			break;
                	}
                }
            })
            .show();
	}

	private String generateThreadUrl(String postId){
    	StringBuffer url = new StringBuffer();
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
    	StringBuffer url = new StringBuffer();
		url.append(Constants.FUNCTION_THREAD);
		url.append("?");
		url.append(Constants.PARAM_GOTO);
		url.append("=");
		url.append(Constants.VALUE_POST);
		url.append("&");
		url.append(Constants.PARAM_POST_ID);
		url.append("=");
		url.append(postId);
		return url.toString();
    }
    
    private Intent createShareIntent(){
    	return new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, mTitle).putExtra(Intent.EXTRA_TEXT, generateThreadUrl(null));
    }
    
    private Intent createShareIntent(String url){
    	return new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url);
    }

	private void copyThreadURL(String postId) {
		String url = generateThreadUrl(postId);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ClipboardManager clipboard = (ClipboardManager) this.getActivity().getSystemService(
					Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText(this.getText(R.string.copy_url).toString() + getPage(), url);
			clipboard.setPrimaryClip(clip);

			displayAlert(R.string.copy_url_success, 0, R.drawable.ic_menu_link);
		} else {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) this.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(url);
            displayAlert(R.string.copy_url_success, 0, R.drawable.ic_menu_link);
		}
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
                        displayAlert(R.string.vote_succeeded, R.string.vote_succeeded_sub, R.drawable.ic_menu_emote);
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

	
	private void ignoreUser(final String aUserId) {
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
                    mPrefs.setBooleanPreference("show_ignore_warning", false);
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
    
	private void toggleMarkUser(String username){
		if(AwfulUtils.isHoneycomb()){
			if(mPrefs.markedUsers.contains(username)){
				mPrefs.unmarkUser(username);
			}else{
				mPrefs.markUser(username);
			}
		}else{
			//TODO:Hide this shit
			displayAlert(R.string.not_available_on_your_version, 0, R.drawable.ic_menu_load_fail);
		}
	}
	
	private void reportUser(final String postid){
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
                      displayAlert(result, R.drawable.ic_menu_emote);
                  }

                  @Override
                  public void failure(VolleyError error) {
                      displayAlert(error.getMessage(), R.drawable.ic_menu_emote);
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
        	bodyHtml = "";
            queueRequest(new PostRequest(getActivity(), getThreadId(), getPage(), mUserId).build(this, new AwfulRequest.AwfulResultCallback<Integer>() {
                @Override
                public void success(Integer result) {
                    refreshInfo();
                    if(result == getPage()){
                        setProgress(75);
                        refreshPosts();
                        mNextPage.setColorFilter(0);
                        mPrevPage.setColorFilter(0);
                        mRefreshBar.setColorFilter(0);
                    }else{
                        Log.e(TAG, "Page mismatch: "+getPage()+" - "+result);
                    }
                }

                @Override
                public void failure(VolleyError error) {
                    refreshInfo();
                    refreshPosts();
                    mNextPage.setColorFilter(0);
                    mPrevPage.setColorFilter(0);
                    mRefreshBar.setColorFilter(0);
                }
            }), true);
        }
    }

    private void cancelOldSync(){
        if(getActivity() != null){
            cancelNetworkRequests();
        }
    }
    
    private void markLastRead(int index) {
        displayAlert(R.string.mark_last_read_progress, R.string.please_wait_subtext, R.drawable.ic_menu_lastread);
        queueRequest(new MarkLastReadRequest(getActivity(), getThreadId(), index).build(null, new AwfulRequest.AwfulResultCallback<Void>() {
            @Override
            public void success(Void result) {
                displayAlert(R.string.mark_last_read_success, 0, R.drawable.ic_menu_lastread);
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
    
    private void startPostRedirect(String postUrl) {
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
                            if(result.getType() == TYPE.THREAD){
                                if(bypassBackStack){
                                    openThread((int) result.getId(), (int) result.getPage(mPrefs.postPerPage), result.getFragment().replaceAll("\\D", ""));
                                }else{
                                    pushThread((int) result.getId(), (int) result.getPage(mPrefs.postPerPage), result.getFragment().replaceAll("\\D", ""));
                                }
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

    private void displayUserCP() {
    	getAwfulActivity().displayForum(Constants.USERCP_ID, 1);
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

    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.next_page:
            		if (getPage() == getLastPage()) {
            			refresh();
            		} else {
                    	goToPage(getPage() + 1);
            		}
                    break;
                case R.id.prev_page:
                	if (getPage() <= 1) {
            			refresh();
            		} else {
                    	goToPage(getPage() - 1);
            		}
                    break;
                case R.id.reply:
                    displayPostReplyDialog();
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
            }
        }
    };

    public void displayPostReplyDialog() {
        displayPostReplyDialog(getThreadId(), -1, AwfulMessage.TYPE_NEW_REPLY);
    }

    @SuppressWarnings("unused")
	private void populateThreadView(ArrayList<AwfulPost> aPosts) {
		updatePageBar();
		updateProbationBar();

        try {

            String html = AwfulThread.getHtml(aPosts, AwfulPreferences.getInstance(getActivity()), getPage(), mLastPage, mParentForumId, threadClosed);
            if(OUTPUT_HTML && Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)){
            	Toast.makeText(getActivity(), "OUTPUTTING DEBUG HTML", Toast.LENGTH_LONG).show();
            	FileOutputStream out = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "awful-thread-"+getThreadId()+"-"+getPage()+".html"));
            	out.write(html.replaceAll("file:///android_res/", "").replaceAll("file:///android_asset/", "").getBytes());
            	out.close();
            }
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
	public void onRefreshStarted(View view) {
        if(getPage() < mLastPage){
            goToPage(getPage()+1);
        }else{
            refresh();
        }		
	}



    private class ClickInterface {
        public static final int SEND_PM  = 0;
        public static final int REPORT_POST = 1;
        public static final int COPY_URL = 2;
        public static final int USER_POSTS = 3;
        public static final int MARK_USER = 4;
        public static final int IGNORE_USER = 5;

        public ClickInterface(){
        	this.preparePreferences();
        }
        
        HashMap<String,String> preferences;
		
        final CharSequence[] mPostItems = {
            "Copy Post URL",
            "Read Posts by this User",
            "Mark/Unmark this User",
            "Ignore User"
        };
        
        final CharSequence[] mPlatPostItems = {
                "Send Private Message",
                "Report Post",
                "Copy Post URL",
                "Read Posts by this User",
                "Mark/Unmark this User",
                "Ignore User"
            };

        @JavascriptInterface
        public void onQuoteClick(final String aPostId) {
        	onQuoteClickInt(Integer.parseInt(aPostId));
        }
        
        //name it differently to avoid ambiguity on the JS interface
        @JavascriptInterface
        public void onQuoteClickInt(final int aPostId){
            displayPostReplyDialog(getThreadId(), aPostId, AwfulMessage.TYPE_QUOTE);
        }

        @JavascriptInterface
        public void onLastReadClick(final String index) {
        	markLastRead(Integer.parseInt(index));
        }
        
        //name it differently to avoid ambiguity on the JS interface
        @JavascriptInterface
        public void onLastReadClickInt(final int index) {
        	markLastRead(index);
        }

        @JavascriptInterface
        public void onSendPMClick(final String aUsername) {
        	startActivity(new Intent(getActivity(), MessageDisplayActivity.class).putExtra(Constants.PARAM_USERNAME, aUsername));
        }

        // Post ID is the item tapped
        @JavascriptInterface
        public void onEditClick(final String aPostId) {
        	onEditClickInt(Integer.parseInt(aPostId));
        }

        //name it differently to avoid ambiguity on the JS interface
        @JavascriptInterface
        public void onEditClickInt(final int aPostId) {
            displayPostReplyDialog(getThreadId(), aPostId, AwfulMessage.TYPE_EDIT);
        }

        @JavascriptInterface
        public void onMoreClick(final String aPostId, final String aUsername, final String aUserId) {
        	new AlertDialog.Builder(getActivity())
            .setTitle("Select an Action")
            .setItems(mPrefs.hasPlatinum?mPlatPostItems:mPostItems, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface aDialog, int aItem) {
                    onPostActionItemSelected(mPrefs.hasPlatinum?aItem:aItem+2, aPostId, aUsername, aUserId);
                }
            })
            .show();
        }

        @JavascriptInterface
        public void onCopyUrlClick(final String aPostId) {
        	copyThreadURL(aPostId);
        }

        @JavascriptInterface
        public void debugMessage(final String msg) {
        	Log.e(TAG,"Awful DEBUG: "+msg);
        }

        @JavascriptInterface
		public void onIgnoreUserClick(final String aUserId) {
			ignoreUser(aUserId);
		}

        @JavascriptInterface
        public void addCodeBounds(final String minBound, final String maxBound){
        	int min = Integer.parseInt(minBound);
        	int max = Integer.parseInt(maxBound);
        	if(min < scrollCheckMinBound || scrollCheckMinBound < 0){
        		scrollCheckMinBound = min;
        	}
        	if(max > scrollCheckMaxBound || scrollCheckMaxBound < 0){
        		scrollCheckMaxBound = max;
        	}
        	Log.i(TAG,"Register pre block: "+min+" - "+max+" - new min: "+scrollCheckMinBound+" new max: "+scrollCheckMaxBound);
        	//this array is going to be accessed very often during touch events, arraylist has too much processing overhead
        	if(scrollCheckBounds == null){
        		scrollCheckBounds = new int[2];
        	}else{
        		//GOOGLE DIDN'T ADD Arrays.copyOf TILL API 9 fuck
        		//scrollCheckBounds = Arrays.copyOf(scrollCheckBounds, scrollCheckBounds.length+2);
        		int[] newScrollCheckBounds = new int[scrollCheckBounds.length+2];
        		for(int x = 0;x<scrollCheckBounds.length;x++){
        			newScrollCheckBounds[x]=scrollCheckBounds[x];
        		}
        		scrollCheckBounds = newScrollCheckBounds;
        	}
        	scrollCheckBounds[scrollCheckBounds.length-2] = min;
        	scrollCheckBounds[scrollCheckBounds.length-1] = max;
        	Arrays.sort(scrollCheckBounds);
        }

        @JavascriptInterface
        public String getBodyHtml(){
            return bodyHtml;
        }

        @JavascriptInterface
        public String getPostJump(){
            return mPostJump;
        }

        @JavascriptInterface
        public String getCSS(){
            return determineCSS();
        }
        
        private void preparePreferences(){
        	AwfulPreferences aPrefs = AwfulPreferences.getInstance();
        	
            preferences = new HashMap<String,String>();
            preferences.clear();
            preferences.put("username", aPrefs.username);
			preferences.put("youtubeHighlight", "#ff00ff");
			preferences.put("showSpoilers", Boolean.toString(aPrefs.showAllSpoilers));
			preferences.put("postFontSize", Integer.toString(aPrefs.postFontSizePx));
			preferences.put("postcolor", ColorPickerPreference.convertToARGB(ColorProvider.getTextColor()));
			preferences.put("backgroundcolor", ColorPickerPreference.convertToARGB(ColorProvider.getBackgroundColor()));
			preferences.put("linkQuoteColor", ColorPickerPreference.convertToARGB(aPrefs.getResources().getColor(R.color.link_quote)));
			preferences.put("highlightUserQuote", Boolean.toString(aPrefs.highlightUserQuote));
			preferences.put("highlightUsername", Boolean.toString(aPrefs.highlightUsername));
			preferences.put("postjumpid", mPostJump);
			preferences.put("scrollPosition", Integer.toString(savedScrollPosition));
			preferences.put("disableGifs", Boolean.toString(aPrefs.disableGifs));
        }
        
        @JavascriptInterface
        public String getPreference(String preference) {
            return preferences.get(preference);
        }

    }
    
	private void onPostActionItemSelected(int aItem, String aPostId, String aUsername, String aUserId) {
		switch(aItem){
		case ClickInterface.SEND_PM:
        	startActivity(new Intent(getActivity(), MessageDisplayActivity.class).putExtra(Constants.PARAM_USERNAME, aUsername));
            //MessageFragment.newInstance(aUsername, 0).show(getFragmentManager(), "new_private_message_dialog");
			break;
		case ClickInterface.COPY_URL:
        	copyThreadURL(aPostId);
			break;
		case ClickInterface.USER_POSTS:
			if(mUserId >0){
        		deselectUser(aPostId);
        	}else{
        		selectUser(Integer.parseInt(aUserId), aUsername);
        	}
			break;
		case ClickInterface.IGNORE_USER:
        	ignoreUser(aUserId);
			break;
		case ClickInterface.MARK_USER:
	    	toggleMarkUser(aUsername);
			break;
		case ClickInterface.REPORT_POST:
			reportUser(aPostId);
			break;
		}
	}

	private String[] gBImageUrlMenuItems = new String[]{
			"Download Image",
			"Show Image Inline",
			"Open URL",
			"Copy URL",
			"Share URL",
			"Always Open URL"
	};
	
	private String[] imageUrlMenuItems = new String[]{
			"Show Image Inline",
			"Open URL",
			"Copy URL",
			"Share URL",
			"Always Open URL"
	};
	
	private String[] urlMenuItems = new String[]{
			"Open URL",
			"Copy URL",
			"Share URL",
			"Always Open URL",
	};
	
	
	private void showUrlMenu(final String url){
		final Uri link = Uri.parse(url);
		final boolean isImage = link != null && link.getLastPathSegment() != null && (link.getLastPathSegment().contains(".jpg") 
				|| link.getLastPathSegment().contains(".jpeg") 
				|| link.getLastPathSegment().contains(".png") 
				|| link.getLastPathSegment().contains(".gif")
				);
    	new AlertDialog.Builder(getActivity())
        .setTitle(url)
        .setItems((isImage? AwfulUtils.isGingerbread()?gBImageUrlMenuItems:imageUrlMenuItems:urlMenuItems), new DialogInterface.OnClickListener() {
        	       	
        	
            public void onClick(DialogInterface aDialog, int aItem) {
            	switch(aItem+(isImage? AwfulUtils.isGingerbread()?0:1:2)){
            	case 0:
        			Request request = new Request(link);
        			if (!AwfulUtils.isHoneycomb()) {
        				request.setShowRunningNotification(true);  
        			} else {
        				request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        			}
        			request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, link.getLastPathSegment());
        			request.allowScanningByMediaScanner();
        			DownloadManager dlMngr= (DownloadManager) getAwfulActivity().getSystemService(getAwfulActivity().DOWNLOAD_SERVICE);
        	        dlMngr.enqueue(request);
        			break;
            	case 1:
        			if(mThreadView != null){
        				mThreadView.loadUrl("javascript:showInlineImage('"+url+"')");
        			}
        			break;
            	case 2:
        			startUrlIntent(url);
        			break;
            	case 3:
            		copyToClipboard(url);
        			displayAlert(R.string.copy_url_success, 0, R.drawable.ic_menu_link);
        			break;
            	case 4:
            		startActivity(createShareIntent(url));
            		break;
            	case 5:
        			mPrefs.setBooleanPreference("always_open_urls", true);
        			startUrlIntent(url);
        			break;
            	}
            }
        }).show();
	}
	
	private void copyToClipboard(String text){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("Copied URL", text);
			clipboard.setPrimaryClip(clip);
		} else {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) this.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(text);
		}
	}
	
	private void startUrlIntent(String url){
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
	public void onPreferenceChange(AwfulPreferences mPrefs) {
		super.onPreferenceChange(mPrefs);
		getAwfulActivity().setPreferredFont(mPageCountText);
		aq.find(R.id.pagebar).backgroundColor(ColorProvider.getActionbarColor());
		if(mPageCountText != null){
			mPageCountText.setTextColor(ColorProvider.getActionbarFontColor());
		}
		if(mThreadView != null){
			mThreadView.setBackgroundColor(ColorProvider.getBackgroundColor());
            mThreadView.loadUrl("javascript:changeCSS('"+determineCSS()+"')");
            mThreadView.getSettings().setDefaultFontSize(mPrefs.postFontSizeDip);
		}
		if(clickInterface != null){
			clickInterface.preparePreferences();
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
            cancelOldSync();
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
		parent.setThreadPage(aPage);
	}
	public void setThreadId(int aThreadId){
        parent.setThreadId(aThreadId);
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
            mThreadView.loadUrl("javascript:loadpagehtml()");
		}
        syncThread();
	}
	
	public void deselectUser(String postId){
        bodyHtml = "";
        aq.find(R.id.thread_userpost_notice).gone();
		if(mThreadView != null){
            mThreadView.loadUrl("javascript:loadpagehtml()");
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
            refreshSessionCookie();
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
        		threadBookmarked = aData.getInt(aData.getColumnIndex(AwfulThread.BOOKMARKED))>0;
                threadArchived = aData.getInt(aData.getColumnIndex(AwfulThread.ARCHIVED))>0;
        		mParentForumId = aData.getInt(aData.getColumnIndex(AwfulThread.FORUM_ID));
        		setTitle(aData.getString(aData.getColumnIndex(AwfulThread.TITLE)));
        		updatePageBar();
        		updateProbationBar();
                if(mUserId > 0 && !TextUtils.isEmpty(mPostByUsername)){
                    aq.find(R.id.thread_userpost_notice).visible().text("Viewing posts by "+mPostByUsername+" in this thread,\nPress the back button to return.").textColor(ColorProvider.getTextColor()).backgroundColor(ColorProvider.getBackgroundColor());
                }else{
                    aq.find(R.id.thread_userpost_notice).gone();
                }
        		if(shareProvider != null){
        			shareProvider.setShareIntent(createShareIntent());
        		}
                invalidateOptionsMenu();
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
        	if(DEBUG) Log.e(TAG,"Thread Data update.");
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
    	if(url.isRedirect()){
    		startPostRedirect(url.getURL(mPrefs.postPerPage));
    	}else{
    		loadThread((int)url.getId(), (int)url.getPage(mPrefs.postPerPage), url.getFragment());
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
    	setThreadId(id);//if the fragment isn't attached yet, just set the values and let the lifecycle handle it
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

//	@Override
//	public boolean canScrollX(int x, int y) {
//		if(mPrefs.lockScrolling){
//			return true;
//		}
//		if(mThreadView == null || scrollCheckBounds == null){
//			return false;
//		}
//		y = y+mThreadView.getScrollY()+mThreadView.getTop();
//		if(y > scrollCheckMaxBound || y < scrollCheckMinBound){
//			return false;
//		}
//		for(int ix = 0; ix < scrollCheckBounds.length-1;ix+=2){
//			if(y > scrollCheckBounds[ix] && y < scrollCheckBounds[ix+1]){
//				return true;
//			}
//		}
//		return false;
//	}


	@Override
	public String getInternalId() {
		return TAG;
	}
	
	public boolean volumeScroll(KeyEvent event) {
	    int action = event.getAction();
	    int keyCode = event.getKeyCode();    
	        switch (keyCode) {
	        case KeyEvent.KEYCODE_VOLUME_UP:
	            if (action == KeyEvent.ACTION_DOWN) {
	                mThreadView.pageUp(false);   
	            }
	            return true;
	        case KeyEvent.KEYCODE_VOLUME_DOWN:
	            if (action == KeyEvent.ACTION_DOWN) {
	            	mThreadView.pageDown(false);
	            }
	            return true;
	        default:
	            return false;
	        }
	} 
	
    
    private void toggleScreenOn() {
    	keepScreenOn = !keepScreenOn;
    	mThreadView.setKeepScreenOn(keepScreenOn);

        //TODO icon
		displayAlert( keepScreenOn? "Screen stays on" :"Screen turns itself off");
	}
    
    @Override
    public void onLowMemory() {
    	super.onLowMemory();
    	mThreadView.freeMemory();
    }
    
    private String determineCSS(){
        File css = new File(Environment.getExternalStorageDirectory()+"/awful/"+mPrefs.theme);
        if(!(mPrefs.forceForumThemes && mParentForumId == Constants.FORUM_ID_YOSPOS) && css.exists() && css.isFile() && css.canRead()){
        	return "file:///"+Environment.getExternalStorageDirectory()+"/awful/"+mPrefs.theme;
        }else if(mPrefs.forceForumThemes){
        	switch(mParentForumId){
				//TODO: No FYAD theme yet        	
//    			case(26):
//	    			return "file:///android_asset/css/fyad.css");
        		//RIP BYOB
//        		case(208):
//        			return "file:///android_asset/css/byob.css";
        		case(219):
        			return "file:///android_asset/css/yospos.css";
        		default:
        			return "file:///android_asset/css/"+mPrefs.theme;
        	}
        }else{
            return "file:///android_asset/css/"+mPrefs.theme;
        }
    }

}
