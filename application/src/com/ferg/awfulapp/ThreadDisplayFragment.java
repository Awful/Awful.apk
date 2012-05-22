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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.webkit.*;
import android.webkit.WebSettings.RenderPriority;
import android.widget.*;
import android.support.v4.app.*;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import greendroid.widget.QuickAction;
import greendroid.widget.QuickActionBar;
import greendroid.widget.QuickActionWidget;
import greendroid.widget.QuickActionWidget.OnQuickActionClickListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.json.*;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.ColorPickerPreference;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPagedItem;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;
import com.ferg.awfulapp.widget.NumberPicker;

/**
 * Uses intent extras:
 *  TYPE - STRING ID - DESCRIPTION
 *	int - Constants.THREAD_ID - id number for that thread
 *	int - Constants.THREAD_PAGE - page number to load
 *
 *  Can also handle an HTTP intent that refers to an SA showthread.php? url.
 */
public class ThreadDisplayFragment extends AwfulFragment implements AwfulUpdateCallback, OnQuickActionClickListener {
    private static final String TAG = "ThreadDisplayActivity";

    private PostLoaderManager mPostLoaderCallback;
    private ThreadDataCallback mThreadLoaderCallback;

    private ImageButton mToggleSidebar;
    
    private ImageButton mNextPage;
    private ImageButton mPrevPage;
    private ImageButton mRefreshBar;
    private View mPageBar;
    private TextView mPageCountText;
    private ViewGroup mThreadWindow;

    private WebView mThreadView;
    
    private boolean imagesLoadingState;
    private boolean threadLoadingState;

    private int mThreadId = 0;
    private int mUserId = 0;
    private int mPage = 1;
    private int mLastPage = 0;
    private int mParentForumId = 0;
    private int mReplyDraftSaved = 0;
    private String mDraftTimestamp = null;
    private boolean threadClosed = false;
    private boolean threadBookmarked = false;
    private boolean dataLoaded = false;
    
    //oh god i'm replicating core android functionality, this is a bad sign.
    //int[0] = threadid, int[1] = pagenum, int[2] = scroll position
    private LinkedList<AwfulStackEntry> backStack = new LinkedList<AwfulStackEntry>();
    
    private static final int buttonSelectedColor = 0x8033b5e5;//0xa0ff7f00;
    
    private String mTitle = null;
    
	private String mPostJump = "";
	private int savedPage = 0;//for reverting from "Find posts by"
	private int savedScrollPosition = 0;
	
	private ArrayList<ThreadQuickAction> actions = new ArrayList<ThreadQuickAction>();
	
	public static ThreadDisplayFragment newInstance(int id, int page) {
		ThreadDisplayFragment fragment = new ThreadDisplayFragment();
		Bundle args = new Bundle();
		args.putInt(Constants.THREAD_ID, id);
		args.putInt(Constants.THREAD_PAGE, page);
		fragment.setArguments(args);

        return fragment;
	}

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message aMsg) {
            handleStatusUpdate(aMsg.arg1);
            switch (aMsg.what) {
            	case AwfulSyncService.MSG_TRANSLATE_REDIRECT:
            		if(aMsg.obj instanceof String){
            			Uri resultLink = Uri.parse(aMsg.obj.toString());
            			String postJump = "";
            			if(resultLink.getFragment() != null){
            				postJump = resultLink.getFragment().replaceAll("\\D", "");
            			}
            			if(resultLink.getQueryParameter(Constants.PARAM_THREAD_ID) != null){
        					String threadId = resultLink.getQueryParameter(Constants.PARAM_THREAD_ID);
        					String pageNum = resultLink.getQueryParameter(Constants.PARAM_PAGE);
        					if(pageNum != null && pageNum.matches("\\d+")){
        						int pageNumber = Integer.parseInt(pageNum);
        						int perPage = Constants.ITEMS_PER_PAGE;
        						String paramPerPage = resultLink.getQueryParameter(Constants.PARAM_PER_PAGE);
        						if(paramPerPage != null && paramPerPage.matches("\\d+")){
        							perPage = Integer.parseInt(paramPerPage);
        						}
        						if(perPage != mPrefs.postPerPage){
        							pageNumber = (int) Math.ceil((double)(pageNumber*perPage) / mPrefs.postPerPage);
        						}
        						pushThread(Integer.parseInt(threadId), pageNumber, postJump);
        					}else{
        						pushThread(Integer.parseInt(threadId), 1, postJump);
        					}
        				}
            		}
            		break;
                case AwfulSyncService.MSG_SYNC_THREAD:
                    if(aMsg.arg1 != AwfulSyncService.Status.WORKING && getActivity() != null){
                    	refreshPosts();
                    }
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO){
                    	if(aMsg.arg1 == AwfulSyncService.Status.WORKING){
                    		if(getPage() == getLastPage()){
                    			mNextPage.setColorFilter(buttonSelectedColor);
                    		}else if(getPage() <= 1){
                    			mPrevPage.setColorFilter(buttonSelectedColor);
                    		}else{
                    			mRefreshBar.setColorFilter(buttonSelectedColor);
                    		}
	                    }else{
                    		if(getPage() == getLastPage()){
                    			mNextPage.setColorFilter(0);
                    		}else if(getPage() <= 1){
                    			mPrevPage.setColorFilter(0);
                    		}else{
                    			mRefreshBar.setColorFilter(0);
                    		}
	                    }
                    }
                    break;
                case AwfulSyncService.MSG_SET_BOOKMARK:
                	refreshInfo();
                    break;
                case AwfulSyncService.MSG_MARK_LASTREAD:
                	refreshInfo();
                    if(aMsg.arg1 == AwfulSyncService.Status.OKAY && getActivity() != null){
                    	refreshPosts();
                    }
                    break;
                default:
                    super.handleMessage(aMsg);
            }
        }
    };

    private Messenger mMessenger = new Messenger(mHandler);
    private ThreadContentObserver mThreadObserver = new ThreadContentObserver(mHandler);

    
	
	private WebViewClient callback = new WebViewClient(){
		@Override
		public void onPageFinished(WebView view, String url) {
			if (imagesLoadingState) {
				imagesLoadingState = false;
				imageLoadingFinished();
			}
			if(!isResumed()){
				Log.e(TAG,"PageFinished after pausing. Forcing Webview.pauseTimers");
				mHandler.postDelayed(new Runnable(){
					@Override
					public void run() {
						if(mThreadView != null){
							mThreadView.pauseTimers();
							mThreadView.onPause();
						}
					}
				}, 500);
			}
		}

		public void onLoadResource(WebView view, String url) {
			if (!threadLoadingState && !imagesLoadingState && url != null && url.startsWith("http")) {
				imagesLoadingState = true;
				imageLoadingStarted();
			}
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView aView, String aUrl) {
			Uri link = Uri.parse(aUrl);
			if(aUrl.contains(Constants.FUNCTION_THREAD)){
				//for the new quote-link stuff
				//http://forums.somethingawful.com/showthread.php?goto=post&postid=XXXX
				if(link.getQueryParameter(Constants.PARAM_GOTO) != null 
					&& link.getQueryParameter(Constants.PARAM_POST_ID) != null ){
					startPostRedirect(aUrl);
					return true;
				}
				//http://forums.somethingawful.com/showthread.php?action=showpost&postid=XXXX
				//but seriously, who uses that function? it doesn't even show up anymore.
				if(link.getQueryParameter(Constants.PARAM_ACTION) != null 
						&& link.getQueryParameter(Constants.PARAM_POST_ID) != null ){
					startPostRedirect(aUrl.replace("action=showpost", "goto=post"));
					return true;
				}
				if(link.getQueryParameter(Constants.PARAM_THREAD_ID) != null){
					String threadId = link.getQueryParameter(Constants.PARAM_THREAD_ID);
					String pageNum = link.getQueryParameter(Constants.PARAM_PAGE);
					if(pageNum != null && pageNum.matches("\\d+")){
						int pageNumber = Integer.parseInt(pageNum);
						int perPage = Constants.ITEMS_PER_PAGE;
						String paramPerPage = link.getQueryParameter(Constants.PARAM_PER_PAGE);
						if(paramPerPage != null && paramPerPage.matches("\\d+")){
							perPage = Integer.parseInt(paramPerPage);
						}
						if(perPage != mPrefs.postPerPage){
							pageNumber = (int) Math.ceil((double)(pageNumber*perPage) / mPrefs.postPerPage);
						}
						pushThread(Integer.parseInt(threadId), pageNumber, "");
					}else{
						pushThread(Integer.parseInt(threadId), 1, "");
					}
					return true;
				}
			}
			if(aUrl.contains(Constants.FUNCTION_FORUM)){
				if(link.getQueryParameter(Constants.PARAM_FORUM_ID) != null){
					String forumId = link.getQueryParameter(Constants.PARAM_FORUM_ID);
					String pageNum = link.getQueryParameter(Constants.PARAM_PAGE);
					if(pageNum != null && pageNum.matches("\\d+")){
						displayForum(Integer.parseInt(forumId), Integer.parseInt(pageNum));
					}else{
						displayForum(Integer.parseInt(forumId), 1);
					}
					return true;
				}
			}
			actions.clear();
			actions.add(new ThreadQuickAction(getActivity(), R.drawable.light_inline_link, "Copy URL", ThreadQuickAction.ACTION_COPY_URL, aUrl));
			actions.add(new ThreadQuickAction(getActivity(), R.drawable.light_inline_more, "Open External", ThreadQuickAction.ACTION_OPEN_LINK_EXTERNAL, aUrl));
			actions.add(new ThreadQuickAction(getActivity(), R.drawable.icon, "Open Internal", ThreadQuickAction.ACTION_OPEN_LINK_INTERNAL, aUrl));
			if(link.getLastPathSegment() != null 
					&& (link.getLastPathSegment().contains(".jpg") 
							|| link.getLastPathSegment().contains(".jpeg") 
							|| link.getLastPathSegment().contains(".png") 
							|| link.getLastPathSegment().contains(".gif")
						)
					){//TODO make this detection less retarded
				actions.add(new ThreadQuickAction(getActivity(), R.drawable.light_inline_allposts, "Inline Image", ThreadQuickAction.ACTION_EXPAND_IMAGE, aUrl));
			}
			QuickActionBar mBar = new QuickActionBar(getActivity());
			for(QuickAction qa : actions){
				mBar.addQuickAction(qa);
			}
			mBar.setOnQuickActionClickListener(ThreadDisplayFragment.this);
			mBar.show(mPageBar);
			return true;
		}
	};

    @Override
    public void onAttach(Activity aActivity) {
        super.onAttach(aActivity); Log.e(TAG, "onAttach");
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState); Log.e(TAG, "onCreate");
        setHasOptionsMenu(true);
        //setRetainInstance(true);
        
        Bundle args = getArguments();
        if(args != null){
	        mThreadId = args.getInt(Constants.THREAD_ID, 0);
	        mPage = args.getInt(Constants.THREAD_PAGE, 1);
        }
        
        String c2pThreadID = null;
        String c2pPostPerPage = null;
        String c2pPage = null;
        String c2pURLFragment = null;
        Intent data = getActivity().getIntent();
        // We may be getting thread info from a link or Chrome2Phone so handle that here
        if (data.getData() != null && data.getScheme().equals("http")) {
            c2pThreadID = data.getData().getQueryParameter("threadid");
            c2pPostPerPage = data.getData().getQueryParameter("perpage");
            c2pPage = data.getData().getQueryParameter("pagenumber");
            c2pURLFragment = data.getData().getEncodedFragment();
        }
        if(mThreadId < 1){
	        mThreadId = data.getIntExtra(Constants.THREAD_ID, mThreadId);
	        mPage = data.getIntExtra(Constants.THREAD_PAGE, mPage);
	        if (c2pThreadID != null) {
	        	mThreadId = Integer.parseInt(c2pThreadID);
	        }
	        if (c2pPage != null) {
	        	int page = Integer.parseInt(c2pPage);
	
	        	if (c2pPostPerPage != null && c2pPostPerPage.matches("\\d+")) {
	        		int ppp = Integer.parseInt(c2pPostPerPage);
	
	        		if (mPrefs.postPerPage != ppp) {
	        			page = (int) Math.ceil((double)(page*ppp) / mPrefs.postPerPage);
	        		}
	        	} else {
	        		if (mPrefs.postPerPage != Constants.ITEMS_PER_PAGE) {
	        			page = (int) Math.ceil((page*Constants.ITEMS_PER_PAGE)/(double)mPrefs.postPerPage);
	        		}
	        	}
	        	mPage = page;
	        	if (c2pURLFragment != null && c2pURLFragment.startsWith("post")) {
	        		setPostJump(c2pURLFragment.replaceAll("\\D", ""));
	        	}
	        }
        }
        if (savedInstanceState != null) {
        	Log.e(TAG, "onCreate savedState");
            mThreadId = savedInstanceState.getInt(Constants.THREAD_ID, mThreadId);
    		mPage = savedInstanceState.getInt(Constants.THREAD_PAGE, mPage);
    		savedScrollPosition = savedInstanceState.getInt("scroll_position", 0);
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
        super.onCreateView(aInflater, aContainer, aSavedState); Log.e(TAG, "onCreateView");
        View result = aInflater.inflate(R.layout.thread_display, aContainer, false);

		mPageCountText = (TextView) result.findViewById(R.id.page_count);
		getAwfulActivity().setPreferredFont(mPageCountText);
		
		mToggleSidebar = (ImageButton) result.findViewById(R.id.toggle_sidebar);
		mNextPage = (ImageButton) result.findViewById(R.id.next_page);
		mPrevPage = (ImageButton) result.findViewById(R.id.prev_page);
        mRefreshBar  = (ImageButton) result.findViewById(R.id.refresh);
		mPageBar = result.findViewById(R.id.page_indicator);
		mThreadView = (WebView) result.findViewById(R.id.thread);
		mThreadWindow = (FrameLayout) result.findViewById(R.id.thread_window);
		mThreadWindow.setBackgroundColor(mPrefs.postBackgroundColor);
		initThreadViewProperties();
		mNextPage.setOnClickListener(onButtonClick);
		mToggleSidebar.setOnClickListener(onButtonClick);
		mPrevPage.setOnClickListener(onButtonClick);
		mRefreshBar.setOnClickListener(onButtonClick);
		mPageCountText.setOnClickListener(onButtonClick);
		return result;
	}

	@Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState); Log.e(TAG, "onActivityCreated");
        if(dataLoaded || savedScrollPosition > 0){
        	refreshPosts();
        }
        updateSidebarHint(isDualPane(), isSidebarVisible());
		updatePageBar();
	}

	private void initThreadViewProperties() {
		mThreadView.resumeTimers();
		mThreadView.setWebViewClient(callback);
		mThreadView.setBackgroundColor(mPrefs.postBackgroundColor);
		mThreadView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		mThreadView.setDrawingCacheEnabled(false);//TODO maybe
		mThreadView.getSettings().setJavaScriptEnabled(true);
		mThreadView.getSettings().setRenderPriority(RenderPriority.HIGH);
        mThreadView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mThreadView.getSettings().setEnableSmoothTransition(true);
			mThreadView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
		}

		mThreadView.setWebChromeClient(new WebChromeClient() {
			public void onConsoleMessage(String message, int lineNumber, String sourceID) {
				Log.d("Web Console", message + " -- From line " + lineNumber + " of " + sourceID);
			}
		});
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
			mPrevPage.setImageResource(R.drawable.ic_menu_load);
			mPrevPage.setVisibility(View.VISIBLE);
			mRefreshBar.setVisibility(View.INVISIBLE);
		} else {
			mPrevPage.setImageResource(R.drawable.ic_menu_arrowleft);
		}

		if (getPage() == getLastPage()) {
			mNextPage.setImageResource(R.drawable.ic_menu_load);
			mRefreshBar.setVisibility(View.INVISIBLE);
		} else {
			mNextPage.setImageResource(R.drawable.ic_menu_arrowright);
		}
	}

    @Override
    public void onStart() {
        super.onStart(); Log.e(TAG, "onStart");
        
    }
    

    @Override
    public void onResume() {
        super.onResume(); Log.e(TAG, "Resume");
        
        if (mThreadView == null) {
            mThreadView = new WebView(getActivity());

            initThreadViewProperties();
            mThreadWindow.removeAllViews();
            mThreadWindow.addView(mThreadView, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        }else{
            mThreadView.onResume();
            mThreadView.resumeTimers();
        }
        getActivity().getContentResolver().registerContentObserver(AwfulThread.CONTENT_URI, true, mThreadObserver);
        refreshInfo();
    }
    
    
    @Override
    public void onPause() {
        super.onPause(); Log.e(TAG, "onPause");
        getActivity().getContentResolver().unregisterContentObserver(mThreadObserver);
        getLoaderManager().destroyLoader(Integer.MAX_VALUE-getThreadId());
        mThreadView.pauseTimers();
        mThreadView.stopLoading();
        mThreadView.onPause();
    }
        
    @Override
    public void onStop() {
        super.onStop(); Log.e(TAG, "onStop");
    }
    
    @Override
    public void onDestroyView(){
    	super.onDestroyView(); Log.e(TAG, "onDestroyView");
        try {
            mThreadWindow.removeView(mThreadView);
            mThreadView.destroy();
            mThreadView = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy(); Log.e(TAG, "onDestroy");
        getLoaderManager().destroyLoader(getThreadId());
    }

    @Override
    public void onDetach() {
        super.onDetach(); Log.e(TAG, "onDetach");
    }
    
    public boolean isDualPane(){
    	return (getActivity() != null && getActivity() instanceof ThreadDisplayActivity && ((ThreadDisplayActivity)getActivity()).isDualPane());
    }
    
    public boolean isSidebarVisible(){
    	return (getActivity() != null && getActivity() instanceof ThreadDisplayActivity && ((ThreadDisplayActivity)getActivity()).isSidebarVisible());
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) { 
    	Log.e(TAG, "onCreateOptionsMenu");
    	if(menu.size() == 0){
    		inflater.inflate(R.menu.post_menu, menu);
    	}
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    	Log.e(TAG, "onCreateOptionsMenu");
        if(menu == null){
            return;
        }
        MenuItem nextArrow = menu.findItem(R.id.next_page);
        if(nextArrow != null){
        	nextArrow.setVisible(mPrefs.upperNextArrow);
        }
        MenuItem bk = menu.findItem(R.id.bookmark);
        if(bk != null){
        	bk.setTitle((threadBookmarked? getString(R.string.unbookmark):getString(R.string.bookmark)));
        }
        MenuItem re = menu.findItem(R.id.reply);
        if(re != null){
        	re.setEnabled(!threadClosed);
        	if(threadClosed){
        		re.setTitle("Thread Locked");
        	}else{
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
    		//case R.id.find://TODO oops, broke this
    		//	this.mThreadView.showFindDialog(null, true);
    		//	break;
    		default:
    			return super.onOptionsItemSelected(item);
    		}

    		return true;
    	}

	private void copyThreadURL(String postId) {
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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ClipboardManager clipboard = (ClipboardManager) this.getActivity().getSystemService(
					Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText(this.getText(R.string.copy_url).toString() + this.mPage, url.toString());
			clipboard.setPrimaryClip(clip);

			Toast.makeText(this.getActivity().getApplicationContext(), getString(R.string.copy_url_success), Toast.LENGTH_SHORT).show();
		} else {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) this.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(url.toString());
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

    
    @Override
    public void onSaveInstanceState(Bundle outState){
    	super.onSaveInstanceState(outState);
    	Log.v(TAG,"onSaveInstanceState");
        outState.putInt(Constants.THREAD_PAGE, getPage());
    	outState.putInt(Constants.THREAD_ID, getThreadId());
    	if(mThreadView != null){
    		outState.putInt("scroll_position", mThreadView.getScrollY());
    	}
    }
    
    private void syncThread() {
        if(getActivity() != null){
        	dataLoaded = false;
        	getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_SYNC_THREAD,getThreadId(),getPage(), Integer.valueOf(mUserId));
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
        // If we're here because of a post result, refresh the thread
        switch (aRequestCode) {
            case PostReplyFragment.RESULT_POSTED:
            	if(getPage() < getLastPage()){
            		goToPage(getLastPage());
            	}else{
            		refresh();
            	}
                break;
        }
    }

    public void refresh() {
		mThreadView.loadData(getBlankPage(), "text/html", "utf-8");
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
                	if(imagesLoadingState && mThreadView != null){
                		mThreadView.stopLoading();
                		imagesLoadingState = false;
                		imageLoadingFinished();
                	}else{
                		refresh();
                	}
                    break;
                case R.id.page_count:
                	displayPagePicker();
                	break;
                case R.id.toggle_sidebar:
                	if(getActivity() != null && getActivity() instanceof ThreadDisplayActivity){
                		((ThreadDisplayActivity)getActivity()).toggleSidebar();
                	}
                	break;
            }
        }
    };

    public void displayPostReplyDialog() {
        Bundle args = new Bundle();
        args.putInt(Constants.THREAD_ID, mThreadId);
        args.putInt(Constants.EDITING, AwfulMessage.TYPE_NEW_REPLY);

        if(mReplyDraftSaved >0){
        	displayDraftAlert(mReplyDraftSaved, mDraftTimestamp, args);
        }else{
            displayPostReplyDialog(args);
        }
    }

    public void displayPostReplyDialog(Bundle aArgs) {
    	startActivityForResult(new Intent(getActivity(), PostReplyActivity.class).putExtras(aArgs), PostReplyFragment.RESULT_POSTED);
    }
    
    private void displayDraftAlert(int replyType, String timeStamp, final Bundle aArgs) {
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
                        displayPostReplyDialog(aArgs);
                    }
                })
            .setNegativeButton(R.string.draft_alert_discard, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface aDialog, int aWhich) {
                    ContentResolver cr = getActivity().getContentResolver();
                    cr.delete(AwfulMessage.CONTENT_URI_REPLY, AwfulMessage.ID+"=?", AwfulProvider.int2StrArray(mThreadId));
                    displayPostReplyDialog(aArgs);
                }
            }).setNeutralButton(R.string.draft_discard_only,  new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface aDialog, int aWhich) {
                    ContentResolver cr = getActivity().getContentResolver();
                    cr.delete(AwfulMessage.CONTENT_URI_REPLY, AwfulMessage.ID+"=?", AwfulProvider.int2StrArray(mThreadId));
                    mReplyDraftSaved = 0;
                }
            })
            .show();
        
    }

    private void handleStatusUpdate(int aStatus) {
        switch (aStatus) {
            case AwfulSyncService.Status.WORKING:
                loadingStarted();
                break;
            case AwfulSyncService.Status.OKAY:
                loadingSucceeded();
                break;
            case AwfulSyncService.Status.ERROR:
                loadingFailed();
                break;
        };
    }

    @Override
    public void loadingFailed() {
    	super.loadingFailed();
        if(getActivity() != null){
        	Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void loadingStarted() {
    	super.loadingStarted();
    	threadLoadingState = true;
    }

    @Override
    public void loadingSucceeded() {
    	super.loadingSucceeded();
    	threadLoadingState = false;
    }
    
    public void imageLoadingStarted() {
    	threadLoadingState = false;
    	if(getActivity() != null){
    		getAwfulActivity().setSupportProgressBarIndeterminateVisibility(true);
        }
    }
    
    public void imageLoadingFinished() {
    	if(getActivity() != null){
    		getAwfulActivity().setSupportProgressBarIndeterminateVisibility(false);
    	}
    }

    private void populateThreadView(ArrayList<AwfulPost> aPosts) {
		updatePageBar();

        try {
            mThreadView.addJavascriptInterface(new ClickInterface(), "listener");
            mThreadView.addJavascriptInterface(getSerializedPreferences(new AwfulPreferences(getActivity())), "preferences");

            mThreadView.loadDataWithBaseURL("http://forums.somethingawful.com", 
            		AwfulThread.getHtml(aPosts, new AwfulPreferences(getActivity()), Constants.isWidescreen(getActivity()), mPage == mLastPage, threadClosed), "text/html", "utf-8", null);//TODO fix
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
            result.put("postcolor", ColorPickerPreference.convertToARGB(aAppPrefs.postFontColor));
            result.put("backgroundcolor", ColorPickerPreference.convertToARGB(aAppPrefs.postBackgroundColor));
            result.put("linkQuoteColor", ColorPickerPreference.convertToARGB(aAppPrefs.postLinkQuoteColor));
            result.put("highlightUserQuote", Boolean.toString(aAppPrefs.highlightUserQuote));
            result.put("highlightUsername", Boolean.toString(aAppPrefs.highlightUsername));
            result.put("postjumpid", mPostJump);
            result.put("scrollPosition", savedScrollPosition);
        } catch (JSONException e) {
        }

        return result.toString();
    }

    private class ClickInterface {
        public static final int SEND_PM  = 0;
        public static final int COPY_URL = 1;
        public static final int USER_POSTS = 2;
		
        final CharSequence[] mPostItems = {
            "Send Private Message",
            "Copy Post URL",
            "Read Posts by this User"
        };
        
        public void onQuoteClick(final String aPostId) {
        	Bundle args = new Bundle();
            args.putInt(Constants.THREAD_ID, mThreadId);
            args.putInt(Constants.POST_ID, Integer.parseInt(aPostId));
            args.putInt(Constants.EDITING, AwfulMessage.TYPE_QUOTE);

            if(mReplyDraftSaved >0){
            	displayDraftAlert(mReplyDraftSaved, mDraftTimestamp, args);
            }else{
                displayPostReplyDialog(args);
            }
        }
        
        public void onLastReadClick(final String aLastReadUrl) {
        	markLastRead(Integer.parseInt(aLastReadUrl));
        }

        public void onSendPMClick(final String aUsername) {
        	startActivity(new Intent(getActivity(), MessageDisplayActivity.class).putExtra(Constants.PARAM_USERNAME, aUsername));
        }

        // Post ID is the item tapped
        public void onEditClick(final String aPostId) {
        	Bundle args = new Bundle();

            args.putInt(Constants.THREAD_ID, mThreadId);
            args.putInt(Constants.EDITING, AwfulMessage.TYPE_EDIT);
            args.putInt(Constants.POST_ID, Integer.parseInt(aPostId));


            if(mReplyDraftSaved >0){
            	displayDraftAlert(mReplyDraftSaved, mDraftTimestamp, args);
            }else{
                displayPostReplyDialog(args);
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
        
        public void onCopyUrlClick(final String aPostId) {
        	copyThreadURL(aPostId);
        }
        
        public void onUserPostsClick(final String aUserId) {
        	if(mUserId >0){
        		deselectUser();
        	}else{
        		selectUser(Integer.parseInt(aUserId));
        	}
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
        		deselectUser();
        	}else{
        		selectUser(Integer.parseInt(aUserId));
        	}
			break;
		}
	}
	
	@Override
	public void onQuickActionClicked(QuickActionWidget widget, int position) {
		ThreadQuickAction selected = actions.get(position);
		if(selected != null){
			switch(selected.action){
			case ThreadQuickAction.ACTION_EXPAND_IMAGE:
				if(mThreadView != null){
					mThreadView.loadUrl("javascript:showInlineImage('"+selected.actionData+"')");
				}
				break;
			case ThreadQuickAction.ACTION_OPEN_LINK_INTERNAL:
		        AwfulWebFragment.newInstance(selected.actionData).show(getFragmentManager().beginTransaction(), "awful_web_dialog");
				break;
			case ThreadQuickAction.ACTION_OPEN_LINK_EXTERNAL:
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(selected.actionData));
				PackageManager pacman = getActivity().getPackageManager();
				List<ResolveInfo> res = pacman.queryIntentActivities(browserIntent,
						PackageManager.MATCH_DEFAULT_ONLY);
				if (res.size() > 0) {
					browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getActivity().startActivity(browserIntent);
				} else {
					String[] split = selected.actionData.split(":");
					Toast.makeText(
							getActivity(),
							"No application found for protocol" + (split.length > 0 ? ": " + split[0] : "."),
							Toast.LENGTH_LONG)
								.show();
				}
				break;
			case ThreadQuickAction.ACTION_COPY_URL:
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("Copied URL", selected.actionData);
					clipboard.setPrimaryClip(clip);

					Toast.makeText(this.getActivity().getApplicationContext(), getString(R.string.copy_url_success), Toast.LENGTH_SHORT).show();
				} else {
					android.text.ClipboardManager clipboard = (android.text.ClipboardManager) this.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
					clipboard.setText(selected.actionData);
					Toast.makeText(this.getActivity().getApplicationContext(), getString(R.string.copy_url_success), Toast.LENGTH_SHORT).show();
				}
				break;
			}
		}
	}
	
	private class ThreadQuickAction extends QuickAction {
		public static final int ACTION_EXPAND_IMAGE = 1;
		public static final int ACTION_OPEN_LINK_INTERNAL = 2;
		public static final int ACTION_OPEN_LINK_EXTERNAL = 3;
		public static final int ACTION_COPY_URL = 4;
		
		public int action;
		public String actionData;
		public ThreadQuickAction(Context ctx, int drawableId, String title, int actionId, String data) {
			super(ctx, drawableId, title);
			action = actionId;
			actionData = data;
		}
		
	}
	
	@Override
	public void onPreferenceChange(AwfulPreferences mPrefs) {
		super.onPreferenceChange(mPrefs);
		getAwfulActivity().setPreferredFont(mPageCountText);
		if(mPageBar != null){
			mPageBar.setBackgroundColor(mPrefs.actionbarColor);
		}
		if(mPageCountText != null){
			mPageCountText.setTextColor(mPrefs.actionbarFontColor);
		}
		if(mThreadView != null){
			mThreadView.setBackgroundColor(mPrefs.postBackgroundColor);
		}
	}

	public void setPostJump(String postID) {
		mPostJump = postID;
	}
	
	public void goToPage(int aPage){
		if(aPage > 0 && aPage <= getLastPage()){
			setPage(aPage);
			updatePageBar();
			mPostJump = "";
            mThreadView.loadData(getBlankPage(), "text/html", "utf-8");
	        syncThread();
		}
	}
	
	private String getBlankPage(){
		return "<html><head></head><body style='{background-color:#"+ColorPickerPreference.convertToARGB(mPrefs.postBackgroundColor)+";'></body></html>";
	}
	
	public int getPage() {
        return mPage;
	}
	public void setPage(int aPage){
		mPage = aPage;
	}
	public void setThreadId(int aThreadId){
		mThreadId = aThreadId;
	}
	
	public void selectUser(int id){
		savedPage = mPage;
		mUserId = id;
		setPage(1);
		mLastPage = 1;
		mPostJump = "";
        mThreadView.loadData(getBlankPage(), "text/html", "utf-8");
        syncThread();
	}
	
	public void deselectUser(){
		mUserId = 0;
		setPage(savedPage);
		mLastPage = 0;
		mPostJump = "";
        mThreadView.loadData(getBlankPage(), "text/html", "utf-8");
        syncThread();
	}
	
	public int getLastPage() {
        return mLastPage;
	}

	public int getThreadId() {
        return mThreadId;
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
            populateThreadView(AwfulPost.fromCursor(getActivity(), aData));
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
        		mParentForumId = aData.getInt(aData.getColumnIndex(AwfulThread.FORUM_ID));
        		setTitle(aData.getString(aData.getColumnIndex(AwfulThread.TITLE)));
        		updatePageBar();
        		mReplyDraftSaved = aData.getInt(aData.getColumnIndex(AwfulMessage.TYPE));
        		if(mReplyDraftSaved > 0){
            		mDraftTimestamp = aData.getString(aData.getColumnIndex(AwfulProvider.UPDATED_TIMESTAMP));
            		//TODO add tablet notification
        			Log.i(TAG, "DRAFT SAVED: "+mReplyDraftSaved+" at "+mDraftTimestamp);
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
        	Log.e(TAG,"Thread Data update.");
        	refreshInfo();
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
	public void refreshInfo() {
		if(getActivity() != null){
			getLoaderManager().restartLoader(Integer.MAX_VALUE-getThreadId(), null, mThreadLoaderCallback);
		}
	}
	
	public void refreshPosts(){
		if(getActivity() != null){
			getLoaderManager().restartLoader(getThreadId(), null, mPostLoaderCallback);
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
	
	private void loadThread(int id, int page, String postJump) {
    	if(getActivity() != null){
	        getLoaderManager().destroyLoader(Integer.MAX_VALUE-getThreadId());
	        getLoaderManager().destroyLoader(getThreadId());
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
    	if(getActivity() != null){
            mThreadView.loadData(getBlankPage(), "text/html", "utf-8");
			refreshInfo();
			syncThread();
    	}
	}
	
	private void openThread(AwfulStackEntry thread) {
    	if(getActivity() != null){
	        getLoaderManager().destroyLoader(Integer.MAX_VALUE-getThreadId());
	        getLoaderManager().destroyLoader(getThreadId());
    	}
    	setThreadId(thread.id);//if the fragment isn't attached yet, just set the values and let the lifecycle handle it
		mUserId = 0;
    	setPage(thread.page);
    	dataLoaded = false;
    	mLastPage = 1;
    	mPostJump = "";
    	savedScrollPosition = thread.scrollPos;
		updatePageBar();
    	if(getActivity() != null){
            mThreadView.loadData(getBlankPage(), "text/html", "utf-8");
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
		openThread(backStack.removeFirst());
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
		if(mToggleSidebar != null){
			if(showIcon){
				mToggleSidebar.setVisibility(View.VISIBLE);
	    		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO){
					if(sidebarVisible){
						mToggleSidebar.setColorFilter(buttonSelectedColor);
					}else{
						mToggleSidebar.setColorFilter(0);
					}
	    		}
			}else{
				mToggleSidebar.setVisibility(View.INVISIBLE);
			}
		}
	}
}
