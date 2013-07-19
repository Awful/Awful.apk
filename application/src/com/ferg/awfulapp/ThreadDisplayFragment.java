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
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.*;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebSettings.RenderPriority;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.ColorPickerPreference;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.*;
import com.ferg.awfulapp.thread.AwfulURL.TYPE;
import com.ferg.awfulapp.util.AwfulGifStripper;
import com.ferg.awfulapp.widget.AwfulHeaderTransformer;
import com.ferg.awfulapp.widget.NumberPicker;
import org.json.JSONException;
import org.json.JSONObject;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher.Options;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.AbsListViewDelegate;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.WebViewDelegate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Uses intent extras:
 *  TYPE - STRING ID - DESCRIPTION
 *	int - Constants.THREAD_ID - id number for that thread
 *	int - Constants.THREAD_PAGE - page number to load
 *
 *  Can also handle an HTTP intent that refers to an SA showthread.php? url.
 */
public class ThreadDisplayFragment extends AwfulFragment implements AwfulUpdateCallback, PullToRefreshAttacher.OnRefreshListener {
    private static final boolean OUTPUT_HTML = false;

    private PostLoaderManager mPostLoaderCallback;
    private ThreadDataCallback mThreadLoaderCallback;

    private ImageButton mToggleSidebar;
	private boolean mShowSidebarIcon;
    
    private ImageButton mNextPage;
    private ImageButton mPrevPage;
    private ImageButton mRefreshBar;
    private TextView mPageCountText;
    
    private View mProbationBar;
	private TextView mProbationMessage;
	private ImageButton mProbationButton;

    private WebView mThreadView;
    private ViewGroup mThreadParent;

    private int mUserId = 0;
    private int mLastPage = 0;
    private int mParentForumId = 0;
    private int mReplyDraftSaved = 0;
    private String mDraftTimestamp = null;
    private boolean threadClosed = false;
    private boolean threadBookmarked = false;
    private boolean threadArchived = false;
    private boolean dataLoaded = false;
    
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

    public static ThreadDisplayFragment newInstance(int id, int page) {
		ThreadDisplayFragment fragment = new ThreadDisplayFragment();
        return fragment;
	}

    public ThreadDisplayFragment() {
        TAG = "ThreadDisplayFragment";
    }

    private ThreadContentObserver mThreadObserver = new ThreadContentObserver(mHandler);

    
	
	private WebViewClient callback = new WebViewClient(){
        String lastUrl="";
        
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        	if(DEBUG) Log.e(TAG, "Opening Connection: "+url);
            if(mPrefs.disableGifs && url != null && url.endsWith(".gif")){
                try {
                    if(DEBUG) Log.e(TAG, "Opening Connection: "+url);
                    URL target = new URL(url);
                    URLConnection response = target.openConnection();
                    response.setReadTimeout(5000);
                    response.setConnectTimeout(1000);
                    response.connect();
                    if(DEBUG) Log.e(TAG, "Connected - Type: "+response.getContentType()+" - Encoding: "+response.getContentEncoding());
                    return new WebResourceResponse(response.getContentType(), response.getContentEncoding(), new AwfulGifStripper(response.getInputStream(), target.getFile()));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                	if(!lastUrl.equals(url)){
                        mThreadView.clearCache(true);
                        lastUrl=url;
                        return shouldInterceptRequest(view, url);                		
                	}
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return super.shouldInterceptRequest(view, url);
        }

        @Override
		public void onPageFinished(WebView view, String url) {
			Log.i(TAG,"PageFinished");
			setProgress(100);
			registerPreBlocks();
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
    	mP2RAttacher = this.getAwfulActivity().getPullToRefreshAttacher();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState); Log.e(TAG, "onCreate");
        setHasOptionsMenu(true);
        if(savedInstanceState != null){
        	Log.w(TAG, "Loading from savedInstanceState");
            setThreadId(savedInstanceState.getInt(Constants.THREAD_ID, getThreadId()));
            setPage(savedInstanceState.getInt(Constants.THREAD_PAGE, getPage()));
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
        
        if(getThreadId() > 0 && savedScrollPosition < 1){
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
		mToggleSidebar = (ImageButton) aq.find(R.id.toggle_sidebar).clicked(onButtonClick).getView();
		mNextPage = (ImageButton) aq.find(R.id.next_page).clicked(onButtonClick).getView();
		mPrevPage = (ImageButton) aq.find(R.id.prev_page).clicked(onButtonClick).getView();
        mRefreshBar  = (ImageButton) aq.find(R.id.refresh).clicked(onButtonClick).getView();
		mThreadView = (WebView) result.findViewById(R.id.thread);
        if(mP2RAttacher != null){
            mP2RAttacher.addRefreshableView(mThreadView,new WebViewDelegate(), this);
            mP2RAttacher.setPullFromBottom(true);
        	mP2RAttacher.setEnabled(true);
        }
        mThreadParent = (ViewGroup) result.findViewById(R.id.thread_window);
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
        if(dataLoaded || savedScrollPosition > 0){
        	Log.w(TAG, "Recovering posts");
        	refreshPosts();
        }
        updateSidebarHint(isDualPane(), isSidebarVisible());
		updatePageBar();
		updateProbationBar();
	}

	private void initThreadViewProperties() {
		mThreadView.resumeTimers();
		mThreadView.setWebViewClient(callback);
		mThreadView.setBackgroundColor(ColorProvider.getBackgroundColor(mPrefs));
		mThreadView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		mThreadView.getSettings().setJavaScriptEnabled(true);
		mThreadView.getSettings().setRenderPriority(RenderPriority.LOW);
        mThreadView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
        if(mPrefs.inlineYoutube && Constants.isFroyo()){//YOUTUBE SUPPORT BLOWS
        	mThreadView.getSettings().setPluginState(PluginState.ON_DEMAND);
        }

		if (Constants.isHoneycomb()) {
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

        refreshSessionCookie();
	}
	
	public void updatePageBar(){
		mPageCountText.setText("Page " + getPage() + "/" + (getLastPage()>0?getLastPage():"?"));
		if(getActivity() != null){
			getAwfulActivity().invalidateOptionsMenu();
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
        //recreate that fucking webview if we don't have it yet
		if(mThreadView == null){
            //recreateWebview();
	        if(dataLoaded){
	        	refreshPosts();
	        }
		}
    }
    

    @Override
    public void onResume() {
        super.onResume(); if(DEBUG) Log.e(TAG, "Resume");
        resumeWebView();
        getActivity().getContentResolver().registerContentObserver(AwfulThread.CONTENT_URI, true, mThreadObserver);
        refreshInfo();
    }

    
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
        mP2RAttacher.setPullFromBottom(true);
	}
	
	

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if(mThreadView != null && dataLoaded){
			if(currentProgress > 99){
				registerPreBlocks();
			}
			updateLayoutType();
		}
	}

	@Override
	public void onPageHidden() {
        pauseWebView();
        if(mThreadView != null){
        	mThreadView.setKeepScreenOn(false);
        }
        mP2RAttacher.setPullFromBottom(false);
	}
	
    @Override
    public void onPause() {
        super.onPause(); if(DEBUG) Log.e(TAG, "onPause");
        getActivity().getContentResolver().unregisterContentObserver(mThreadObserver);
        getLoaderManager().destroyLoader(Constants.THREAD_INFO_LOADER_ID);
        pauseWebView();
    }

    private void pauseWebView(){
        if (mThreadView != null) {
        	mThreadView.pauseTimers();
        	mThreadView.onPause();
        }
    }
        
    @Override
    public void onStop() {
        super.onStop(); if(DEBUG) Log.e(TAG, "onStop");
        if (mThreadView != null && !Constants.isICS()) {
        	//SALT THE FUCKING EARTH
            //There are a few bugs with specific 2.x phones where the webview will continue running after pausing (eating a ton of CPU)
            //Burn them to the ground and recreate on resume.
            //destroyWebview();
        }
    }
    
    @Override
    public void onDestroyView(){
    	super.onDestroyView(); if(DEBUG) Log.e(TAG, "onDestroyView");
        //destroyWebview();
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
    
    public boolean isDualPane(){
    	return false;
    }
    
    public boolean isSidebarVisible(){
    	return (getActivity() != null && getActivity() instanceof ThreadDisplayActivity && ((ThreadDisplayActivity)getActivity()).isSidebarVisible());
    }
    
    public void updateLayoutType(){
    	if(mThreadView != null && getActivity() != null){
			if(!mPrefs.threadLayout.equalsIgnoreCase("phone") && (mPrefs.threadLayout.equalsIgnoreCase("tablet") || Constants.isWidescreen(getActivity()))){
				mThreadView.loadUrl("javascript:showTabletUI()");
			}else{
				mThreadView.loadUrl("javascript:showPhoneUI()");
			}
    	}
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) { 
    	if(DEBUG) Log.e(TAG, "onCreateOptionsMenu");
    	menu.clear();
    	if(menu.size() == 0){
    		inflater.inflate(R.menu.post_menu, menu);
        	MenuItem share = menu.findItem(R.id.share_thread);
        	if(share != null && share.getActionProvider() instanceof ShareActionProvider){
        		shareProvider = (ShareActionProvider) share.getActionProvider();
        		shareProvider.setShareIntent(createShareIntent());
        	}
    	}
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    	if(DEBUG) Log.e(TAG, "onCreateOptionsMenu");
        if(menu == null){
            return;
        }
        MenuItem nextArrow = menu.findItem(R.id.next_page);
        if(nextArrow != null){
        	nextArrow.setVisible(mPrefs.upperNextArrow);
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
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
    			this.mThreadView.showFindDialog(null, true);
    			break;
    		case R.id.keep_screen_on:
    			this.toggleScreenOn();
    		default:
    			return super.onOptionsItemSelected(item);
    		}

    		return true;
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

	private void copyThreadURL(String postId) {
		String url = generateThreadUrl(postId);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ClipboardManager clipboard = (ClipboardManager) this.getActivity().getSystemService(
					Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText(this.getText(R.string.copy_url).toString() + getPage(), url);
			clipboard.setPrimaryClip(clip);

			Toast.makeText(this.getActivity().getApplicationContext(), getString(R.string.copy_url_success), Toast.LENGTH_SHORT).show();
		} else {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) this.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(url);
			Toast.makeText(this.getActivity().getApplicationContext(), getString(R.string.copy_url_success), Toast.LENGTH_SHORT).show();
		}
	}

	private void rateThread() {

		final CharSequence[] items = { "1", "2", "3", "4", "5" };

		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		builder.setTitle("Rate this thread");
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				if (getActivity() != null) {
					getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_VOTE, getThreadId(), item);
				}
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	
	private void ignoreUser(final String aUserId) {
		if(mPrefs.ignoreFormkey == null){
			getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_FETCH_PROFILE, 0, 0);
		}
		if(mPrefs.showIgnoreWarning){
		AlertDialog ignoreDialog = new AlertDialog.Builder(getAwfulActivity()).create();
		ignoreDialog.setButton(AlertDialog.BUTTON_POSITIVE, getActivity().getString(R.string.confirm), new android.content.DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_IGNORE_USER, Integer.parseInt(aUserId), 0);
			}
		});
		ignoreDialog.setButton(AlertDialog.BUTTON_NEGATIVE,getActivity().getString(R.string.cancel), (android.content.DialogInterface.OnClickListener) null);
		ignoreDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getActivity().getString(R.string.dont_show_again), new android.content.DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_IGNORE_USER, Integer.parseInt(aUserId), 0);
				try{
				mPrefs.setBooleanPreference("show_ignore_warning", false);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		});
		ignoreDialog.setTitle(R.string.ignore_title);
		ignoreDialog.setMessage(getActivity().getString(R.string.ignore_message));
		ignoreDialog.show();
		
		}else{
			getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_IGNORE_USER, Integer.parseInt(aUserId), 0);
		}
	}
    
    @Override
    public void onSaveInstanceState(Bundle outState){
    	super.onSaveInstanceState(outState);
    	if(DEBUG) Log.v(TAG,"onSaveInstanceState");
        outState.putInt(Constants.THREAD_PAGE, getPage());
    	outState.putInt(Constants.THREAD_ID, getThreadId());
    	if(mThreadView != null){
    		outState.putInt("scroll_position", mThreadView.getScrollY());
    	}
    }
    
    private void syncThread() {
        if(getActivity() != null){
        	dataLoaded = false;
        	getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_SYNC_THREAD, getThreadId(), getPage(), Integer.valueOf(mUserId));
        }
    }

    private void cancelOldSync(){
        if(getActivity() != null){
            getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_CANCEL_SYNC_THREAD, getThreadId(), getPage());
        }
    }
    
    private void markLastRead(int index) {
        if(getActivity() != null){
        	getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_MARK_LASTREAD,getThreadId(),index);
        }
    }

    private void toggleThreadBookmark() {
        if(getActivity() != null){
        	getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_SET_BOOKMARK,getThreadId(),(threadBookmarked?0:1));
        }
    }
    
    private void startPostRedirect(String postUrl) {
        if(getActivity() != null){
        	getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_TRANSLATE_REDIRECT, getThreadId(), 0, postUrl);
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
    		mThreadView.loadData(getBlankPage(), "text/html", "utf-8");
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
                	if(mShowSidebarIcon){
	                	if(getActivity() != null && getActivity() instanceof ThreadDisplayActivity){
	                		((ThreadDisplayActivity)getActivity()).toggleSidebar();
	                	}
                	}else{
                		if (getPage() == getLastPage()) {
                			refresh();
                		} else {
                        	goToPage(getPage() + 1);
                		}
                	}
                	break;
            }
        }
    };

    public void displayPostReplyDialog() {

        if(mReplyDraftSaved >0){
        	displayDraftAlert(mReplyDraftSaved, mDraftTimestamp, getThreadId(), -1, AwfulMessage.TYPE_NEW_REPLY);
        }else{
            displayPostReplyDialog(getThreadId(), -1, AwfulMessage.TYPE_NEW_REPLY);
        }
    }
    
    private void displayDraftAlert(final int replyType, String timeStamp, final  int threadId, final int postId, final int newType) {
    	TextView draftAlertMsg = new TextView(getActivity());
    	if(timeStamp != null){
    	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    	    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    	    try {
				Date d = sdf.parse(timeStamp);
				java.text.DateFormat df = DateFormat.getDateFormat(getActivity());
				df.setTimeZone(TimeZone.getDefault());
				timeStamp = df.format(d);
			} catch (ParseException e) {
				e.printStackTrace();
			}
    	}
    	switch(replyType){
    	case AwfulMessage.TYPE_EDIT:
        	draftAlertMsg.setText("Unsent Edit Found"+(timeStamp != null ? " from "+timeStamp : ""));
    		break;
    	case AwfulMessage.TYPE_QUOTE:
        	draftAlertMsg.setText("Unsent Quote Found"+(timeStamp != null ? " from "+timeStamp : ""));
    		break;
    	case AwfulMessage.TYPE_NEW_REPLY:
        	draftAlertMsg.setText("Unsent Reply Found"+(timeStamp != null ? " from "+timeStamp : ""));
    		break;
    	}
        new AlertDialog.Builder(getActivity())
            .setTitle("Draft Found")
            .setView(draftAlertMsg)
            .setPositiveButton(R.string.draft_alert_keep,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface aDialog, int aWhich) {
                        displayPostReplyDialog(threadId, postId, replyType);
                    }
                })
            .setNegativeButton(R.string.draft_alert_discard, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface aDialog, int aWhich) {
                    ContentResolver cr = getActivity().getContentResolver();
                    cr.delete(AwfulMessage.CONTENT_URI_REPLY, AwfulMessage.ID+"=?", AwfulProvider.int2StrArray(getThreadId()));
                    displayPostReplyDialog(threadId, postId, newType);
                }
            }).setNeutralButton(R.string.draft_discard_only,  new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface aDialog, int aWhich) {
                    ContentResolver cr = getActivity().getContentResolver();
                    cr.delete(AwfulMessage.CONTENT_URI_REPLY, AwfulMessage.ID+"=?", AwfulProvider.int2StrArray(getThreadId()));
                    mReplyDraftSaved = 0;
                }
            })
            .show();
        
    }

    @Override
    public void loadingFailed(Message aMsg) {
    	super.loadingFailed(aMsg);
        if(mThreadView != null){
//        	mThreadView.onRefreshComplete();
        }
        refreshInfo();
		if(aMsg.obj == null && getActivity() != null){
			Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
		}
    	switch (aMsg.what) {
	        case AwfulSyncService.MSG_SYNC_THREAD:
	        	refreshPosts();
	            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO){
	    			mNextPage.setColorFilter(0);
	    			mPrevPage.setColorFilter(0);
	    			mRefreshBar.setColorFilter(0);
	            }
	            break;
	        case AwfulSyncService.MSG_SET_BOOKMARK:
	        	refreshInfo();
	            break;
	        case AwfulSyncService.MSG_MARK_LASTREAD:
	        	refreshInfo();
	            refreshPosts();
	            break;
	        default:
	        	Log.e(TAG,"Message not handled: "+aMsg.what);
	        	break;
    	}
		bypassBackStack = false;
    }

    @Override
    public void loadingStarted(Message aMsg) {
    	super.loadingStarted(aMsg);
        if(mThreadView != null){
//            mThreadWindow.onRefreshComplete();
        }
    	switch(aMsg.what){
		case AwfulSyncService.MSG_SYNC_THREAD:
	    	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO){
	    		if(getPage() == getLastPage()){
	    			mNextPage.setColorFilter(buttonSelectedColor);
	    			mPrevPage.setColorFilter(0);
	    			mRefreshBar.setColorFilter(0);
	    		}else if(getPage() <= 1){
	    			mPrevPage.setColorFilter(buttonSelectedColor);
	    			mNextPage.setColorFilter(0);
	    			mRefreshBar.setColorFilter(0);
	    		}else{
	    			mRefreshBar.setColorFilter(buttonSelectedColor);
	    			mPrevPage.setColorFilter(0);
	    			mNextPage.setColorFilter(0);
	    		}
	        }
	        break;
        default:
        	Log.e(TAG,"Message not handled: "+aMsg.what);
        	break;
    	}
    }

    @Override
	public void loadingUpdate(Message aMsg) {
		//super.loadingUpdate(aMsg);
    	setProgress(aMsg.arg2/2);
	}

	@Override
    public void loadingSucceeded(Message aMsg) {
    	super.loadingSucceeded(aMsg);
        refreshInfo();
    	switch (aMsg.what) {
    	case AwfulSyncService.MSG_TRANSLATE_REDIRECT:
    		if(aMsg.obj instanceof String){
    			AwfulURL result = AwfulURL.parse((String) aMsg.obj);
    			if(aMsg.obj.toString().contains(Constants.VALUE_LASTPOST)){
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
    			}else{
    				Log.e(TAG,"REDIRECT FAILED: "+aMsg.obj);
    				Toast.makeText(getActivity(), "Load Failed: Malformed URL", Toast.LENGTH_LONG).show();
    			}
    		}
			bypassBackStack = false;
    		break;
        case AwfulSyncService.MSG_SYNC_THREAD:
        	if(aMsg.arg2 == getPage()){
	        	setProgress(50);
	        	refreshPosts();
	            if(Constants.isFroyo()){
	    			mNextPage.setColorFilter(0);
	    			mPrevPage.setColorFilter(0);
	    			mRefreshBar.setColorFilter(0);
	            }
        	}
            break;
        case AwfulSyncService.MSG_SET_BOOKMARK:
        	refreshInfo();
            break;
        case AwfulSyncService.MSG_MARK_LASTREAD:
        	refreshInfo();
            refreshPosts();
            break;
        default:
        	Log.e(TAG,"Message not handled: "+aMsg.what);
        	break;
    	}
    }

    @SuppressWarnings("unused")
	private void populateThreadView(ArrayList<AwfulPost> aPosts) {
		updatePageBar();
		updateProbationBar();

        try {
            mThreadView.addJavascriptInterface(clickInterface, "listener");
            mThreadView.addJavascriptInterface(getSerializedPreferences(AwfulPreferences.getInstance(getActivity())), "preferences");
            boolean useTabletLayout = !mPrefs.threadLayout.equalsIgnoreCase("phone") && 
            		(mPrefs.threadLayout.equalsIgnoreCase("tablet") || Constants.isWidescreen(getActivity()));
            String html = AwfulThread.getHtml(aPosts, AwfulPreferences.getInstance(getActivity()), useTabletLayout, getPage(), mLastPage, mParentForumId, threadClosed);
            if(OUTPUT_HTML && Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)){
            	Toast.makeText(getActivity(), "OUTPUTTING DEBUG HTML", Toast.LENGTH_LONG).show();
            	FileOutputStream out = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "awful-thread-"+getThreadId()+"-"+getPage()+".html"));
            	out.write(html.replaceAll("file:///android_res/", "").replaceAll("file:///android_asset/", "").getBytes());
            	out.close();
            }
            mThreadView.loadDataWithBaseURL(Constants.BASE_URL + "/", html, "text/html", "utf-8", null);
        } catch (Exception e) {
        	e.printStackTrace();
            // If we've already left the activity the webview may still be working to populate,
            // just log it
        }
        Log.i(TAG,"Finished populateThreadView, posts:"+aPosts.size());
    }

    private String getSerializedPreferences(final AwfulPreferences aAppPrefs) {
        JSONObject result = new JSONObject();

        try {
            result.put("username", aAppPrefs.username);
            result.put("userQuote", "#a2cd5a");
            result.put("usernameHighlight", "#9933ff");
            result.put("youtubeHighlight", "#ff00ff");
            result.put("showSpoilers", aAppPrefs.showAllSpoilers);
            result.put("postFontSize", aAppPrefs.postFontSizePx);
            result.put("postcolor", ColorPickerPreference.convertToARGB(ColorProvider.getTextColor(aAppPrefs)));
            result.put("backgroundcolor", ColorPickerPreference.convertToARGB(ColorProvider.getBackgroundColor(aAppPrefs)));
            result.put("linkQuoteColor", ColorPickerPreference.convertToARGB(this.getResources().getColor(R.color.link_quote)));
            result.put("highlightUserQuote", Boolean.toString(aAppPrefs.highlightUserQuote));
            result.put("highlightUsername", Boolean.toString(aAppPrefs.highlightUsername));
            result.put("postjumpid", mPostJump);
            result.put("scrollPosition", savedScrollPosition);
            result.put("disableGifs", false);
        } catch (JSONException e) {
        }

        return result.toString();
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
        public static final int COPY_URL = 1;
        public static final int USER_POSTS = 2;
        public static final int IGNORE_USER = 3;
		
        final CharSequence[] mPostItems = {
            "Send Private Message",
            "Copy Post URL",
            "Read Posts by this User",
            "Ignore User"
        };
		
        final CharSequence[] mEditMenuItems = {
            "Edit",
            "Quote",
            "Mark Last Read",
            "Send Private Message",
            "Copy Post URL",
            "Read Posts by this User",
            "Ignore User"
        };
		
        final CharSequence[] mMenuItems = {
            "Quote",
            "Mark Last Read",
            "Send Private Message",
            "Copy Post URL",
            "Read Posts by this User",
            "Ignore User"
        };
        
        final CharSequence[] mProbatedItems = {
            "Mark Last Read",
            "Send Private Message",
            "Copy Post URL",
            "Read Posts by this User",
            "Ignore User"
        };

        public static final int MENU_EDIT = 0;
        public static final int MENU_QUOTE  = 1;
        public static final int MENU_LASTREAD = 2;
        public static final int MENU_SEND_PM  = 3;
        public static final int MENU_COPY_URL = 4;
        public static final int MENU_USER_POSTS = 5;
        public static final int MENU_USER_IGNORE = 6;
        
        public void onQuoteClick(final String aPostId) {
        	onQuoteClickInt(Integer.parseInt(aPostId));
        }
        
        //name it differently to avoid ambiguity on the JS interface
        public void onQuoteClickInt(final int aPostId){
            if(mReplyDraftSaved >0){
            	displayDraftAlert(mReplyDraftSaved, mDraftTimestamp, getThreadId(), aPostId,AwfulMessage.TYPE_QUOTE);
            }else{
                displayPostReplyDialog(getThreadId(), aPostId, AwfulMessage.TYPE_QUOTE);
            }
        }
        
        public void onLastReadClick(final String index) {
        	markLastRead(Integer.parseInt(index));
        }
        
        //name it differently to avoid ambiguity on the JS interface
        public void onLastReadClickInt(final int index) {
        	markLastRead(index);
        }

        public void onSendPMClick(final String aUsername) {
        	startActivity(new Intent(getActivity(), MessageDisplayActivity.class).putExtra(Constants.PARAM_USERNAME, aUsername));
        }

        // Post ID is the item tapped
        public void onEditClick(final String aPostId) {
        	onEditClickInt(Integer.parseInt(aPostId));
        }

        //name it differently to avoid ambiguity on the JS interface
        public void onEditClickInt(final int aPostId) {
            if(mReplyDraftSaved >0){
            	displayDraftAlert(mReplyDraftSaved, mDraftTimestamp, getThreadId(), aPostId, AwfulMessage.TYPE_EDIT);
            }else{
                displayPostReplyDialog(getThreadId(), aPostId, AwfulMessage.TYPE_EDIT);
            }
        }
        
        public void onMoreClick(final String aPostId, final String aUsername, final String aUserId) {
        	new AlertDialog.Builder(getActivity())
            .setTitle("Select an Action")
            .setItems(mPostItems, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface aDialog, int aItem) {
                    onPostActionItemSelected(aItem, aPostId, aUsername, aUserId);
                }
            })
            .show();
        }
        
        public void onMenuClick(final String aPostId, final String aUsername, final String aUserId, final String index, final String editable) {
        	final boolean edit = editable != null && editable.contains("true");
        	final boolean probated = mPrefs.isOnProbation();
        	new AlertDialog.Builder(getActivity())
            .setTitle("Select an Action")
            .setItems((probated?mProbatedItems:edit?mEditMenuItems:mMenuItems), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface aDialog, int aItem) {
                	//the non-edit menu is one item shorter, so we just shift that aItem up by one to compensate
                	onPostMenuItemSelected(aItem+(probated?2:edit?0:1), aPostId, aUsername, aUserId, index);
                }
            })
            .show();
        }
        
        public void onCopyUrlClick(final String aPostId) {
        	copyThreadURL(aPostId);
        }
        
        public void debugMessage(final String msg) {
        	Log.e(TAG,"Awful DEBUG: "+msg);
        }
        
        public void onUserPostsClick(final String aUserId) {
        	onUserPostsClickInt(Integer.parseInt(aUserId));
        }
        
        public void onUserPostsClickInt(final int aUserId) {
        	if(mUserId >0){
        		deselectUser("0");
        	}else{
        		selectUser(aUserId);
        	}
        }

		public void onIgnoreUserClick(final String aUserId) {
			// TODO Auto-generated method stub
			ignoreUser(aUserId);
		}
		
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

    }
    
	private void onPostMenuItemSelected(int aItem, String aPostId, String aUsername, String aUserId, String lastread) {
		switch(aItem){
			case ClickInterface.MENU_QUOTE:
				clickInterface.onQuoteClick(aPostId);
				break;
			case ClickInterface.MENU_EDIT:
				clickInterface.onEditClick(aPostId);
				break;
			case ClickInterface.MENU_LASTREAD:
				clickInterface.onLastReadClick(lastread);
				break;
			case ClickInterface.MENU_SEND_PM:
				clickInterface.onSendPMClick(aUsername);
				break;
			case ClickInterface.MENU_COPY_URL:
	        	clickInterface.onCopyUrlClick(aPostId);
				break;
			case ClickInterface.MENU_USER_POSTS:
				if(mUserId >0){
	        		deselectUser(aPostId);
	        	}else{
	        		selectUser(Integer.parseInt(aUserId));
	        	}
			break;
			case ClickInterface.MENU_USER_IGNORE:
	        	clickInterface.onIgnoreUserClick(aUserId);
			break;
		}
	}
    
	private void onPostActionItemSelected(int aItem,
			String aPostId, String aUsername, String aUserId) {
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
        		selectUser(Integer.parseInt(aUserId));
        	}
			break;
		case ClickInterface.IGNORE_USER:
        	ignoreUser(aUserId);
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
        .setItems((isImage?Constants.isGingerbread()?gBImageUrlMenuItems:imageUrlMenuItems:urlMenuItems), new DialogInterface.OnClickListener() {
        	       	
        	
            public void onClick(DialogInterface aDialog, int aItem) {
            	switch(aItem+(isImage?Constants.isGingerbread()?0:1:2)){
            	case 0:
        			Request request = new Request(link);
        			if (!Constants.isHoneycomb()) {
        				request.setShowRunningNotification(true);  
        			} else {
        				request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        			}

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
        			Toast.makeText(getActivity().getApplicationContext(), getString(R.string.copy_url_success), Toast.LENGTH_SHORT).show();
        			break;
            	case 4:
            		startActivity(createShareIntent());
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
			Toast.makeText(
					getActivity(),
					"No application found for protocol" + (split.length > 0 ? ": " + split[0] : "."),
					Toast.LENGTH_LONG)
						.show();
		}
	}
	
	@Override
	public void onPreferenceChange(AwfulPreferences mPrefs) {
		super.onPreferenceChange(mPrefs);
		getAwfulActivity().setPreferredFont(mPageCountText);
		aq.find(R.id.pagebar).backgroundColor(ColorProvider.getActionbarColor(mPrefs));
		if(mPageCountText != null){
			mPageCountText.setTextColor(ColorProvider.getActionbarFontColor(mPrefs));
		}
		if(mThreadView != null){
			mThreadView.setBackgroundColor(ColorProvider.getBackgroundColor(mPrefs));
            mThreadView.loadUrl("javascript:changeCSS('"+mPrefs.theme+"')");
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
			if(mThreadView != null){
				mThreadView.loadData(getBlankPage(), "text/html", "utf-8");
			}
	        syncThread();
            cancelOldSync();
		}
	}
	
	private String getBlankPage(){
		return "<html><head></head><body style='{background-color:#"+ColorPickerPreference.convertToARGB(ColorProvider.getBackgroundColor(mPrefs))+";'></body></html>";
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
	
	public void selectUser(int id){
		savedPage = getPage();
		mUserId = id;
		setPage(1);
		mLastPage = 1;
		mPostJump = "";
		if(mThreadView != null){
			mThreadView.loadData(getBlankPage(), "text/html", "utf-8");
		}
        syncThread();
	}
	
	public void deselectUser(String postId){
		if(mThreadView != null){
			mThreadView.loadData(getBlankPage(), "text/html", "utf-8");
		}
		if("0".equals(postId)){
			mUserId = 0;
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
        	setProgress(50);
        	if(aData.isClosed()){
        		return;
        	}
        	if(mThreadView != null){
        		populateThreadView(AwfulPost.fromCursor(getActivity(), aData));
        	}
            dataLoaded = true;
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
        		mReplyDraftSaved = aData.getInt(aData.getColumnIndex(AwfulMessage.TYPE));
        		if(mReplyDraftSaved > 0){
            		mDraftTimestamp = aData.getString(aData.getColumnIndex(AwfulProvider.UPDATED_TIMESTAMP));
            		//TODO add tablet notification
        			Log.i(TAG, "DRAFT SAVED: "+mReplyDraftSaved+" at "+mDraftTimestamp);
        		}
        		if(shareProvider != null){
        			shareProvider.setShareIntent(createShareIntent());
        		}
                getAwfulActivity().invalidateOptionsMenu();
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
		if(getActivity() != null){
			getLoaderManager().restartLoader(Constants.THREAD_INFO_LOADER_ID, null, mThreadLoaderCallback);
		}
	}
	
	public void refreshPosts(){
		if(getActivity() != null){
			getLoaderManager().restartLoader(Constants.POST_LOADER_ID, null, mPostLoaderCallback);
		}
	}
	
	public void setTitle(String title){
		mTitle = title;
		if(getActivity() != null && mTitle != null){
			getAwfulActivity().setActionbarTitle(mTitle, this);
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
        if(id == getThreadId() && page == getPage() && postJump == null){
            return;
        }
    	if(getActivity() != null){
	        getLoaderManager().destroyLoader(Constants.THREAD_INFO_LOADER_ID);
	        getLoaderManager().destroyLoader(Constants.POST_LOADER_ID);
    	}
    	setThreadId(id);//if the fragment isn't attached yet, just set the values and let the lifecycle handle it
		mUserId = 0;
    	setPage(page);
    	dataLoaded = false;
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
    			mThreadView.loadData(getBlankPage(), "text/html", "utf-8");
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
    	setPage(thread.page);
    	dataLoaded = false;
    	mLastPage = 1;
    	mPostJump = "";
    	savedScrollPosition = thread.scrollPos;
		updatePageBar();
		updateProbationBar();
    	if(getActivity() != null){
    		if(mThreadView != null){
    			mThreadView.loadData(getBlankPage(), "text/html", "utf-8");
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
		}else{
			return false;
		}
	}

	public void updateSidebarHint(boolean showIcon, boolean sidebarVisible) {
		mShowSidebarIcon = showIcon;
		if(mToggleSidebar != null){
			if(mShowSidebarIcon){
				mToggleSidebar.setVisibility(View.VISIBLE);
				mToggleSidebar.setImageResource(R.drawable.ic_menu_sidebar);
	    		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO){
					if(sidebarVisible){
						mToggleSidebar.setColorFilter(buttonSelectedColor);
					}else{
						mToggleSidebar.setColorFilter(0);
					}
	    		}
			}else{
				mToggleSidebar.setVisibility(View.VISIBLE);
				mToggleSidebar.setImageDrawable(null);
			}
		}
	}
	
	private void registerPreBlocks() {
		scrollCheckBounds = null;
		scrollCheckMinBound = -1;
		scrollCheckMaxBound = -1;
		if(mThreadView != null && dataLoaded){
			mHandler.postDelayed(new Runnable(){
				@Override
				public void run() {
					if(mThreadView != null){
						mThreadView.loadUrl("javascript:registerPreBlocks()");
					}
				}
			}, 2000);
		}
	}

	@Override
	public boolean canScrollX(int x, int y) {
		if(mPrefs.lockScrolling){
			return true;
		}
		if(mThreadView == null || scrollCheckBounds == null){
			return false;
		}
		y = y+mThreadView.getScrollY()+mThreadView.getTop();
		if(y > scrollCheckMaxBound || y < scrollCheckMinBound){
			return false;
		}
		for(int ix = 0; ix < scrollCheckBounds.length-1;ix+=2){
			if(y > scrollCheckBounds[ix] && y < scrollCheckBounds[ix+1]){
				return true;
			}
		}
		return false;
	}
	

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
		Toast.makeText(getAwfulActivity(), keepScreenOn? "Screen stays on" :"Screen turns itself off", Toast.LENGTH_SHORT).show();
	}

}
