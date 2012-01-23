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

package com.ferg.awful;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.*;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.webkit.*;
import android.widget.*;
import android.support.v4.app.*;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import java.util.ArrayList;
import java.util.List;

import org.json.*;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.preferences.ColorPickerPreference;
import com.ferg.awful.provider.AwfulProvider;
import com.ferg.awful.reply.Reply;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulMessage;
import com.ferg.awful.thread.AwfulPagedItem;
import com.ferg.awful.thread.AwfulPost;
import com.ferg.awful.thread.AwfulThread;
import com.ferg.awful.widget.NumberPicker;
import com.ferg.awful.widget.SnapshotWebView;

public class ThreadDisplayFragment extends Fragment implements AwfulUpdateCallback {
    private static final String TAG = "ThreadDisplayActivity";
    private AwfulPreferences mPrefs;

    private PostLoaderManager mPostLoaderCallback;
    private ThreadDataCallback mThreadLoaderCallback;

    private ImageButton mNext;
    private ImageButton mNextPage;
    private ImageButton mPrevPage;
    private ImageButton mReply;
    private ImageButton mRefresh;
    private ImageButton mRefreshBar;
    private ImageView mSnapshotView;
    private TextView mPageCountText;
    private TextView mTitle;
    private ProgressDialog mDialog;
    private ViewGroup mThreadWindow;

    private SnapshotWebView mThreadView;
    
    private boolean imagesLoadingState;
    private boolean threadLoadingState;

    private int mPage = 1;
    private int mThreadId = 0;
    private int mLastPage = 0;
    private int mReplyDraftSaved = 0;
    private String mDraftTimestamp = null;
    private boolean threadClosed = false;
    private boolean threadBookmarked = false;
    
	private String mPostJump = "";

    public static ThreadDisplayFragment newInstance(int aThreadId) {
        ThreadDisplayFragment fragment = new ThreadDisplayFragment();
        fragment.setThreadId(aThreadId);
        fragment.setPage(1);
        return fragment;
    }

    public static ThreadDisplayFragment newInstance(int aThreadId, int aPage) {
        ThreadDisplayFragment fragment = new ThreadDisplayFragment();
        fragment.setThreadId(aThreadId);
        fragment.setPage(aPage);
        return fragment;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message aMsg) {
            switch (aMsg.what) {
                case AwfulSyncService.MSG_SYNC_THREAD:
                    handleStatusUpdate(aMsg.arg1);
                	getActivity().getSupportLoaderManager().restartLoader(getThreadId(), null, mPostLoaderCallback);
                    break;
                case AwfulSyncService.MSG_SET_BOOKMARK:
                    handleStatusUpdate(aMsg.arg1);
                	refreshInfo();
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
			if (!isResumed()) {
				Log.d(TAG, view.toString() + " pageFinished: " + url);
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						pauseWebView();
					}
				}, 500);// this seems to be a race condition. if we call the
						// pause code too soon, it might ignore the message.
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
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(aUrl));
			PackageManager pacman = aView.getContext().getPackageManager();
			List<ResolveInfo> res = pacman.queryIntentActivities(browserIntent,
					PackageManager.MATCH_DEFAULT_ONLY);
			if (res.size() > 0) {
				aView.getContext().startActivity(browserIntent);
			} else {
				String[] split = aUrl.split(":");
				Toast.makeText(
						aView.getContext(),
						"No application found for protocol"
								+ (split.length > 0 ? ": " + split[0] : "."), Toast.LENGTH_LONG)
						.show();
			}
			return true;
		}
	};

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        mPostLoaderCallback = new PostLoaderManager();
        mThreadLoaderCallback = new ThreadDataCallback();
		mPrefs = new AwfulPreferences(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);
        View result = aInflater.inflate(R.layout.thread_display, aContainer, false);
        
        if (AwfulActivity.useLegacyActionbar()) {
            View actionbar = ((ViewStub) result.findViewById(R.id.actionbar)).inflate();

            mTitle    = (TextView) actionbar.findViewById(R.id.title);
            mNext     = (ImageButton) actionbar.findViewById(R.id.next_page_top);
            mReply    = (ImageButton) actionbar.findViewById(R.id.reply);
            mRefresh  = (ImageButton) actionbar.findViewById(R.id.refresh_top);

            mTitle.setMovementMethod(new ScrollingMovementMethod());
        }

		mPageCountText = (TextView) result.findViewById(R.id.page_count);
		mNextPage = (ImageButton) result.findViewById(R.id.next_page);
		mPrevPage = (ImageButton) result.findViewById(R.id.prev_page);
        mRefreshBar  = (ImageButton) result.findViewById(R.id.refresh);
		mThreadView = (SnapshotWebView) result.findViewById(R.id.thread);
		mSnapshotView = (ImageView) result.findViewById(R.id.snapshot);
		mThreadWindow = (FrameLayout) result.findViewById(R.id.thread_window);
		
		mNextPage.setOnClickListener(onButtonClick);
		mPrevPage.setOnClickListener(onButtonClick);
		mRefreshBar.setOnClickListener(onButtonClick);
		updatePageBar();

		return result;
	}

	@Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState);

		if (AwfulActivity.useLegacyActionbar()) {
			mNext.setOnClickListener(onButtonClick);
			mReply.setOnClickListener(onButtonClick);
			mRefresh.setOnClickListener(onButtonClick);
		}

        ((AwfulActivity) getActivity()).registerSyncService(mMessenger, getThreadId());
        getActivity().getContentResolver().registerContentObserver(AwfulThread.CONTENT_URI, true, mThreadObserver);
		initThreadViewProperties();
	}

	private void initThreadViewProperties() {
		mThreadView.resumeTimers();
		mThreadView.setWebViewClient(callback);
		mThreadView.setSnapshotView(mSnapshotView);
		mThreadView.getSettings().setJavaScriptEnabled(true);
		mThreadView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		mThreadView.setBackgroundColor(mPrefs.postBackgroundColor);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mThreadView.getSettings().setEnableSmoothTransition(true);
		}

		mThreadView.setWebChromeClient(new WebChromeClient() {
			public void onConsoleMessage(String message, int lineNumber, String sourceID) {
				Log.d("Web Console", message + " -- From line " + lineNumber + " of " + sourceID);
			}
		});
	}
	
	public void updatePageBar(){
		mPageCountText.setText("Page " + getPage() + "/" + (getLastPage()>0?getLastPage():"?"));
        if (AwfulActivity.useLegacyActionbar()) {
            if (getPage() == getLastPage()) {
                mNext.setVisibility(View.GONE);
            } else {
                mNext.setVisibility(View.VISIBLE);
            }

            if(threadClosed){
                mReply.setVisibility(View.GONE);
            } else {
                mReply.setVisibility(View.VISIBLE);
            }
        }
		if (getPage() <= 1) {
			mPrevPage.setVisibility(View.INVISIBLE);
		} else {
			mPrevPage.setVisibility(View.VISIBLE);
		}

		if (getPage() == getLastPage()) {
            mNextPage.setVisibility(View.GONE);
            mRefreshBar.setVisibility(View.VISIBLE);
		} else {
            mNextPage.setVisibility(View.VISIBLE);
            mRefreshBar.setVisibility(View.GONE);
		}
	}

    private boolean isTablet() {
        return ((AwfulActivity) getActivity()).isTablet();
    }

    private void setActionbarTitle(String aTitle) {
        if (AwfulActivity.useLegacyActionbar()) {
            mTitle.setText(Html.fromHtml(aTitle));
        } else {
            ((ThreadDisplayActivity) getActivity()).setThreadTitle(aTitle);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getLoaderManager().initLoader(getThreadId(), null, mPostLoaderCallback);
        getLoaderManager().initLoader(Integer.MAX_VALUE-getThreadId(), null, mThreadLoaderCallback);

        
        syncThread();
    }
    

    @Override
    public void onResume() {
        super.onResume();
        
        if (mThreadWindow.getChildCount() < 2) {
            mThreadView = new SnapshotWebView(getActivity());

            initThreadViewProperties();

            mThreadWindow.addView(mThreadView, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        }else{
        	if(mThreadView != null){
	        	try {
	                Class.forName("android.webkit.WebView").getMethod("onResume", (Class[]) null)
	                    .invoke(mThreadView, (Object[]) null);
	                mThreadView.resumeTimers();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
        	}
        }
        refreshInfo();
    }
    
    
    @Override
    public void onPause() {
        super.onPause();
        try {
            mThreadView.pauseTimers();
            mThreadView.stopLoading();
            Class.forName("android.webkit.WebView").getMethod("onPause", (Class[]) null)
                .invoke(mThreadView, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        cleanupTasks();
    }
    
    public void pauseWebView(){
    	try {
            mThreadView.pauseTimers();
            Class.forName("android.webkit.WebView").getMethod("onPause", (Class[]) null)
                .invoke(mThreadView, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
        
    @Override
    public void onStop() {
        super.onStop();

        mThreadView.stopLoading();
        cleanupTasks();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
    
    @Override
    public void onDestroyView(){
    	super.onDestroyView();
        try {
            mThreadWindow.removeView(mThreadView);
            mThreadView.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        getActivity().getContentResolver().unregisterContentObserver(mThreadObserver);
        ((AwfulActivity) getActivity()).unregisterSyncService(mMessenger, getThreadId());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupTasks();
    }

    private void cleanupTasks() {
        if (mDialog != null) {
        }
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(menu.size() == 0){
            inflater.inflate(R.menu.post_menu, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if(menu == null || !AwfulActivity.useLegacyActionbar()){
            return;
        }

        MenuItem bk = menu.findItem(R.id.bookmark);
        bk.setTitle((threadBookmarked? getString(R.string.unbookmark):getString(R.string.bookmark)));
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
            case R.id.go_back:
                goToPage(getPage()-1);
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
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle aOutState) {
        super.onSaveInstanceState(aOutState);
    }
    
    private void syncThread() {
        ((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_SYNC_THREAD,getThreadId(),getPage());
    }
    
    private void markLastRead(int index) {
        ((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_MARK_LASTREAD,getThreadId(),index);
    }

    private void toggleThreadBookmark() {
        ((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_SET_BOOKMARK,getThreadId(),(threadBookmarked?0:1));
    }

    private void displayUserCP() {
        if (!isTablet()) {
            startActivity(new Intent().setClass(getActivity(), UserCPActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } else {
            UserCPFragment.newInstance(true).show(getFragmentManager(), "user_control_panel_dialog");
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

    private boolean onPostActionItemSelected(int aItem, String aPostId, int aLastReadIndex, String aUsername) {
        switch (aItem) {
            case ClickInterface.EDIT:
            	if (aUsername != null){
                    if (!isTablet()) {
                        startActivity(new Intent(getActivity(), MessageDisplayActivity.class).putExtra(Constants.PARAM_USERNAME, aUsername));
                    } else {
                        MessageFragment.newInstance(aUsername, 0).show(getFragmentManager(), "new_private_message_dialog");
                    }
            	}else{
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
                return true;
            case ClickInterface.QUOTE:
                Bundle args = new Bundle();
                args.putInt(Constants.THREAD_ID, mThreadId);
                args.putInt(Constants.POST_ID, Integer.parseInt(aPostId));
                args.putInt(Constants.EDITING, AwfulMessage.TYPE_QUOTE);

                if(mReplyDraftSaved >0){
                	displayDraftAlert(mReplyDraftSaved, mDraftTimestamp, args);
                }else{
                    displayPostReplyDialog(args);
                }
                return true;
            case ClickInterface.LAST_READ:
            	markLastRead(aLastReadIndex);
                return true;
        }

        return false;
    }
    
    @Override
    public void onActivityResult(int aRequestCode, int aResultCode, Intent aData) {
        // If we're here because of a post result, refresh the thread
        switch (aResultCode) {
            case PostReplyFragment.RESULT_POSTED:
                refresh();
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
                case R.id.next_page_top:
                	goToPage(getPage() + 1);
                    break;
                case R.id.prev_page:
                	goToPage(getPage() - 1);
                    break;
                case R.id.reply:
                    displayPostReplyDialog();
                    break;
                case R.id.refresh_top:
                case R.id.refresh:
                	if(imagesLoadingState && mThreadView != null){
                		mThreadView.stopLoading();
                		imagesLoadingState = false;
                		imageLoadingFinished();
                	}else{
                		refresh();
                	}
                    break;
            }
        }
    };

    private void displayPostReplyDialog() {
        Bundle args = new Bundle();
        args.putInt(Constants.THREAD_ID, mThreadId);
        args.putInt(Constants.EDITING, AwfulMessage.TYPE_NEW_REPLY);

        if(mReplyDraftSaved >0){
        	displayDraftAlert(mReplyDraftSaved, mDraftTimestamp, args);
        }else{
            displayPostReplyDialog(args);
        }
    }

    private void displayPostReplyDialog(Bundle aArgs) {
        if (isTablet()) {
            PostReplyFragment fragment = PostReplyFragment.newInstance(aArgs);
            fragment.setTargetFragment(this, 0);
            fragment.show(getActivity().getSupportFragmentManager(), "post_reply_dialog");
        } else {
            Intent postReply = new Intent().setClass(getActivity(),
                    PostReplyActivity.class);
            postReply.putExtras(aArgs);
            startActivityForResult(postReply, 0);
        }
    }
    
    private void displayDraftAlert(int replyType, String timeStamp, final Bundle aArgs) {
    	TextView draftAlertMsg = new TextView(getActivity());
    	switch(replyType){
    	case AwfulMessage.TYPE_EDIT:
        	draftAlertMsg.setText("Unsent Edit Found from "+timeStamp);
    		break;
    	case AwfulMessage.TYPE_QUOTE:
        	draftAlertMsg.setText("Unsent Quote Found from "+timeStamp);
    		break;
    	case AwfulMessage.TYPE_NEW_REPLY:
        	draftAlertMsg.setText("Unsent Reply Found from "+timeStamp);
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
        if (AwfulActivity.useLegacyActionbar()) {
            mRefresh.setVisibility(View.VISIBLE);
            mRefresh.setAnimation(null);
            mRefresh.setImageResource(android.R.drawable.ic_dialog_alert);
            mRefresh.startAnimation(mFlashingAnimation);
        } else {
            getActivity().setProgressBarIndeterminateVisibility(false);
        }

        Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void loadingStarted() {
    	threadLoadingState = true;
        if (AwfulActivity.useLegacyActionbar()) {
            mRefresh.setVisibility(View.VISIBLE);
            mRefresh.setImageResource(R.drawable.ic_menu_refresh);
            // TODO: mRefresh.startAnimation(mAdapter.getRotateAnimation());
        } else {
            getActivity().setProgressBarIndeterminateVisibility(true);
        }
    }

    @Override
    public void loadingSucceeded() {
    	threadLoadingState = false;
        if (AwfulActivity.useLegacyActionbar()) {
            mRefresh.setAnimation(null);
            mRefresh.setVisibility(View.GONE);
        } else {
            getActivity().setProgressBarIndeterminateVisibility(false);
        }
    }
    
    public void imageLoadingStarted() {
    	threadLoadingState = false;
        if (AwfulActivity.useLegacyActionbar()) {
        	if(mRefresh != null){
	            mRefresh.setVisibility(View.VISIBLE);
	            mRefresh.setImageResource(android.R.drawable.ic_menu_mapmode);
	            mRefresh.startAnimation(mFlashingAnimation);
        	}
        } else {
            getActivity().setProgressBarIndeterminateVisibility(true);
        }
    }
    
    public void imageLoadingFinished() {
        if (AwfulActivity.useLegacyActionbar()) {
        	if(mRefresh != null){
        		mRefresh.setAnimation(null);
        		mRefresh.setVisibility(View.GONE);
        	}
        } else {
            getActivity().setProgressBarIndeterminateVisibility(false);
        }
    }

    private void populateThreadView(ArrayList<AwfulPost> aPosts) {
		updatePageBar();

        try {
            mThreadView.addJavascriptInterface(new ClickInterface(), "listener");
            mThreadView.addJavascriptInterface(getSerializedPreferences(new AwfulPreferences(getActivity())), "preferences");

            mThreadView.loadDataWithBaseURL("http://forums.somethingawful.com", 
                    AwfulThread.getHtml(aPosts, new AwfulPreferences(getActivity()), ((AwfulActivity) getActivity()).isLargeScreen()), "text/html", "utf-8", null);
        } catch (NullPointerException e) {
            // If we've already left the activity the webview may still be working to populate,
            // just log it
        }
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
        } catch (JSONException e) {
        }

        return result.toString();
    }

    private class ClickInterface {
        public static final int QUOTE     = 0;
        public static final int LAST_READ = 1;
        public static final int EDIT      = 2;

        final CharSequence[] mEditablePostItems = {
            "Quote", 
            "Mark last read",
            "Edit Post"
        };
        final CharSequence[] mPostItems = {
            "Quote", 
            "Mark last read",
            "Send Private Message"
        };

        // Post ID is the item tapped
        public void onPostClick(final String aPostId, final String aLastReadUrl, final String aUsername) {
            new AlertDialog.Builder(getActivity())
                .setTitle("Select an Action")
                .setItems(mPostItems, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface aDialog, int aItem) {
                        onPostActionItemSelected(aItem, aPostId, Integer.parseInt(aLastReadUrl), aUsername);
                    }
                })
                .show();
        }

        // Post ID is the item tapped
        public void onEditablePostClick(final String aPostId, final String aLastReadUrl) {
            new AlertDialog.Builder(getActivity())
                .setTitle("Select an Action")
                .setItems(mEditablePostItems, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface aDialog, int aItem) {
                        onPostActionItemSelected(aItem, aPostId, Integer.parseInt(aLastReadUrl), null);
                    }
                })
                .show();
        }

        public void onPreviousPageClick() {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    goToPage(getPage() - 1);
                }
            });
        }

        public void onNextPageClick() {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    goToPage(getPage() + 1);
                }
            });
        }

        public void onRefreshPageClick() {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    refresh();
                }
            });
        }
    }

	@Override
	public void onPreferenceChange(AwfulPreferences mPrefs) {
		// don't need this, threadview is automatically refreshed on resume.
		//actually, it's not anymore, but it doesn't matter much
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
	public int getLastPage() {
        return mLastPage;
	}

	public int getThreadId() {
        return mThreadId;
	}

    private class PostLoaderManager implements LoaderManager.LoaderCallbacks<Cursor> {

        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            String sortOrder = AwfulPost.POST_INDEX + " ASC LIMIT " + mPrefs.postPerPage;

            String selection = AwfulPost.THREAD_ID + "=? AND " + AwfulPost.POST_INDEX + " >= ? AND " + AwfulPost.POST_INDEX + " < ?";
            int index = AwfulPagedItem.pageToIndex(getPage(), mPrefs.postPerPage, 0);
            String[] args = new String[]{Integer.toString(getThreadId()), Integer.toString(index), Integer.toString(index+mPrefs.postPerPage)};
            Log.v(TAG,"Displaying thread: "+getThreadId()+" index: "+index+" page: "+getPage()+" perpage: "+mPrefs.postPerPage);

            return new CursorLoader(getActivity(), AwfulPost.CONTENT_URI, 
                    null, selection, args, sortOrder);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.i(TAG,"Load finished, populating.");
            populateThreadView(AwfulPost.fromCursor(getActivity(), aData));
        }

        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        }
    }
    //I'll probably break this out into a separate object.
    private class ThreadDataCallback implements LoaderManager.LoaderCallbacks<Cursor> {

        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            return new CursorLoader(getActivity(), ContentUris.withAppendedId(AwfulThread.CONTENT_URI, getThreadId()), 
            		AwfulProvider.ThreadProjection, null, null, null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"Thread title finished, populating.");
        	if(aData.getCount() >0 && aData.moveToFirst()){
                setActionbarTitle(aData.getString(aData.getColumnIndex(AwfulThread.TITLE)));
        		mLastPage = AwfulPagedItem.indexToPage(aData.getInt(aData.getColumnIndex(AwfulThread.POSTCOUNT)),mPrefs.postPerPage);
        		threadClosed = aData.getInt(aData.getColumnIndex(AwfulThread.LOCKED))>0;
        		threadBookmarked = aData.getInt(aData.getColumnIndex(AwfulThread.BOOKMARKED))>0;
        		updatePageBar();
        		mReplyDraftSaved = aData.getInt(aData.getColumnIndex(AwfulMessage.TYPE));
        		if(mReplyDraftSaved > 0){
            		mDraftTimestamp = aData.getString(aData.getColumnIndex(AwfulProvider.UPDATED_TIMESTAMP));
            		if(mReply != null){
            			mReply.startAnimation(mFlashingAnimation);
            		}//TODO add tablet notification
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
	static {
		mFlashingAnimation.setInterpolator(new LinearInterpolator());
		mFlashingAnimation.setRepeatCount(Animation.INFINITE);
		mFlashingAnimation.setDuration(500);
	}
	public void refreshInfo() {
    	getLoaderManager().restartLoader(Integer.MAX_VALUE-getThreadId(), null, mThreadLoaderCallback);
	}
}
