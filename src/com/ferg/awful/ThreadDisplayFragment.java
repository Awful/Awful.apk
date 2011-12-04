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
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.*;
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
import com.ferg.awful.reply.Reply;
import com.ferg.awful.service.ThreadSyncService;
import com.ferg.awful.thread.AwfulPost;
import com.ferg.awful.thread.AwfulThread;
import com.ferg.awful.widget.NumberPicker;
import com.ferg.awful.widget.SnapshotWebView;

public class ThreadDisplayFragment extends Fragment implements AwfulUpdateCallback {
    private static final String TAG = "ThreadDisplayActivity";

    private static final int LOADER_POSTS = 0;

    private ParsePostQuoteTask mPostQuoteTask;
    private ParseEditPostTask mEditPostTask;
    private SharedPreferences mPrefs;
    
    private boolean mBound = false;
    private Messenger mService = null;

    private PostLoaderManager mLoaderManager;

    private ImageButton mNext;
    private ImageButton mNextPage;
    private ImageButton mPrevPage;
    private ImageButton mReply;
    private ImageButton mRefresh;
    private ImageView mSnapshotView;
    private TextView mPageCountText;
    private TextView mTitle;
    private ProgressDialog mDialog;
    private ViewGroup mThreadWindow;

    private SnapshotWebView mThreadView;

    private boolean queueDataUpdate;
    private Bundle queueDataExtras;
    private boolean imagesLoadingState;

    private int savedPage = 0;
    
	private String mPostJump = "";

    public static ThreadDisplayFragment newInstance(int aThreadId) {
        ThreadDisplayFragment fragment = new ThreadDisplayFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt(Constants.THREAD_ID, aThreadId);

        fragment.setArguments(args);

        return fragment;
    }

    public static ThreadDisplayFragment newInstance(int aThreadId, int aPage) {
        ThreadDisplayFragment fragment = new ThreadDisplayFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt(Constants.THREAD_ID, aThreadId);
        args.putInt(Constants.PAGE, aPage);

        fragment.setArguments(args);

        return fragment;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message aMsg) {
            switch (aMsg.what) {
                case ThreadSyncService.MSG_PROGRESS_STATUS:
                    handleStatusUpdate(aMsg.arg1);
                    break;
                default:
                    getActivity().getSupportLoaderManager().initLoader(LOADER_POSTS, null, mLoaderManager);
                    super.handleMessage(aMsg);
            }
        }
    };

    private Messenger mMessenger = new Messenger(mHandler);
    private PostContentObserver mObserver = new PostContentObserver(mHandler);

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName aName, IBinder aBinder) {
            mService = new Messenger(aBinder);

            try {
                Message msg = Message.obtain(null, ThreadSyncService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                syncThread();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName aName) {
            mService = null;
        }
    };
	
	private WebViewClient callback = new WebViewClient(){
		@Override
		public void onPageFinished(WebView view, String url){
			if(imagesLoadingState){
				imagesLoadingState = false;
				imageLoadingFinished();
			}
			if(!isResumed()){
				Log.d(TAG,view.toString()+" pageFinished: "+url);
				mHandler.postDelayed(new Runnable(){
					@Override
					public void run() {
						pauseWebView();
					}
				}, 500);//this seems to be a race condition. if we call the pause code too soon, it might ignore the message.
			}
		}
		
		public void onLoadResource (WebView view, String url){
			if(!imagesLoadingState && url != null && url.startsWith("http")){
				imagesLoadingState = true;
				imageLoadingStarted();
			}
		}

        @Override
        public boolean shouldOverrideUrlLoading(WebView aView, String aUrl) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(aUrl));
            PackageManager pacman = aView.getContext().getPackageManager();
            List<ResolveInfo> res = pacman.queryIntentActivities(browserIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if(res.size() >0){
            	aView.getContext().startActivity(browserIntent);
            }else{
            	String[] split = aUrl.split(":");
            	Toast.makeText(aView.getContext(), "No application found for protocol"+(split.length > 0 ? ": "+split[0] : ".") ,Toast.LENGTH_LONG).show();
            }
            return true;
        }
	};

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        mLoaderManager = new PostLoaderManager();
    }

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);
        View result = aInflater.inflate(R.layout.thread_display, aContainer, false);
        
        if (AwfulActivity.useLegacyActionbar()) {
            View actionbar = ((ViewStub) result.findViewById(R.id.actionbar)).inflate();

            mTitle    = (TextView) actionbar.findViewById(R.id.title);
            mNext     = (ImageButton) actionbar.findViewById(R.id.next_page);
            mReply    = (ImageButton) actionbar.findViewById(R.id.reply);
            mRefresh  = (ImageButton) actionbar.findViewById(R.id.refresh);

            mTitle.setMovementMethod(new ScrollingMovementMethod());
        }

		mPageCountText = (TextView) result.findViewById(R.id.page_count);
		mNextPage      = (ImageButton) result.findViewById(R.id.next);
		mPrevPage      = (ImageButton) result.findViewById(R.id.prev_page);
        mThreadView    = (SnapshotWebView) result.findViewById(R.id.thread);
        mSnapshotView  = (ImageView) result.findViewById(R.id.snapshot);
        mThreadWindow  = (FrameLayout) result.findViewById(R.id.thread_window);

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

        initThreadViewProperties();
    }

    private void initThreadViewProperties() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    	
        mThreadView.resumeTimers();
        mThreadView.setWebViewClient(callback);
        mThreadView.setSnapshotView(mSnapshotView);
        mThreadView.getSettings().setJavaScriptEnabled(true);
        mThreadView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        mThreadView.setBackgroundColor(mPrefs.getInt("default_post_background_color", getResources().getColor(R.color.background)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mThreadView.getSettings().setEnableSmoothTransition(true);
        }
        
        mThreadView.setWebChromeClient(new WebChromeClient() {
            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                Log.d("Web Console", message + " -- From line " + lineNumber + " of " + sourceID);
            }
        });
    }

    private void initPageCountCallbacks() {
		mPrevPage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                mThreadView.loadData("", "text/html", "utf-8");
				// TODO: goToPage(mAdapter.getPage() - 1);
			}
		});

        /* TODO: 
		if (mAdapter.getPage() <= 1) {
			mPrevPage.setVisibility(View.INVISIBLE);
		} else {
			mPrevPage.setVisibility(View.VISIBLE);
		}

		if (mAdapter.getPage() == mAdapter.getLastPage()) {
			mNextPage.setImageResource(R.drawable.stat_notify_sync);
			mNextPage.setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
                    mThreadView.loadData("", "text/html", "utf-8");
					mAdapter.refresh();
				}
			});
		} else {
        */
			mNextPage.setImageResource(R.drawable.r_arrow);
            mNextPage.setOnClickListener(onButtonClick);
		// }

        mNextPage.setVisibility(View.VISIBLE);
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

        // TODO: setActionbarTitle(mAdapter.getTitle());

        getLoaderManager().initLoader(LOADER_POSTS, null, mLoaderManager);
        doBind();

        getActivity().getContentResolver().registerContentObserver(
                AwfulPost.CONTENT_URI, true, mObserver);
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
        if (getActivity() != null && getActivity().getIntent() != null){
        	// TODO: getActivity().getIntent().putExtra(Constants.PAGE, mAdapter.getPage());
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

        doUnbind();

        getActivity().getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // TODO: savedPage = mAdapter.getPage(); // saves page for orientation change.
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupTasks();
    }

    private void cleanupTasks() {
        if (mDialog != null) {
        }
        if (mEditPostTask != null) {
            mEditPostTask.cancel(true);
        }
        
        if (mPostQuoteTask != null) {
            mPostQuoteTask.cancel(true);
        }
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

        if (queueDataUpdate){
            delayedDataUpdate(queueDataExtras);
            queueDataUpdate = false;
            queueDataExtras = null;
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

        /* TODO: 
        if(bk != null){
            AwfulThread th = (AwfulThread) mAdapter.getState();
            if(th != null){
                bk.setTitle((th.isBookmarked()? getString(R.string.unbookmark):getString(R.string.bookmark)));
            }
        }
        */
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.next_page:
                showNextPage();
                break;
            case R.id.reply:
                displayPostReplyDialog();
                break;
            case R.id.go_back:
                // TODO: goToPage(mAdapter.getPage()-1);
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
                // TODO: mAdapter.toggleBookmark();
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

    private void doBind() {
        getActivity().bindService(new Intent(getActivity(), ThreadSyncService.class),
                mServiceConnection, getActivity().BIND_AUTO_CREATE);
        mBound = true;
    }

    private void doUnbind() {
        if (mBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, ThreadSyncService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            getActivity().unbindService(mServiceConnection);
            mBound = false;
        }
    }

    private void syncThread(int aPage) {

    }

    private void syncThread() {
        try {
            Message msg = Message.obtain(null, ThreadSyncService.MSG_SYNC_THREAD);
            msg.arg1 = getArguments().getInt(Constants.THREAD_ID, -1);
            msg.arg2 = 1;

            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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

        /* TODO: 
        jumpToText.setRange(1, mAdapter.getLastPage());
        jumpToText.setCurrent(mAdapter.getPage());
        new AlertDialog.Builder(getActivity())
            .setTitle("Jump to Page")
            .setView(jumpToText)
            .setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface aDialog, int aWhich) {
                        try {
                            int pageInt = jumpToText.getCurrent();
                            if (pageInt > 0 && pageInt <= mAdapter.getLastPage()) {
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
        */
    }

    private boolean onPostActionItemSelected(int aItem, String aPostId, String aLastReadUrl, String aUsername) {
        switch (aItem) {
            case ClickInterface.EDIT:
            	if (aUsername != null){
                    if (!isTablet()) {
                        startActivity(new Intent(getActivity(), MessageDisplayActivity.class).putExtra(Constants.PARAM_USERNAME, aUsername));
                    } else {
                        MessageFragment.newInstance(aUsername, 0).show(getFragmentManager(), "new_private_message_dialog");
                    }
            	}else{
                    mEditPostTask = new ParseEditPostTask();
                    mEditPostTask.execute(aPostId);
            	}
                return true;
            case ClickInterface.QUOTE:
                mPostQuoteTask = new ParsePostQuoteTask();
                mPostQuoteTask.execute(aPostId);
                return true;
            case ClickInterface.LAST_READ:
                // TODO: mAdapter.markLastRead(aLastReadUrl);
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
        // TODO: mAdapter.refresh();
    }

    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.next_page:
                case R.id.next:
                    showNextPage();
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
            }
        }
    };

    private void showNextPage() {
        /* TODO: 
        if (mAdapter.getPage() < mAdapter.getLastPage()) {
            mThreadView.loadData("", "text/html", "utf-8");
            goToPage(mAdapter.getPage()+1);
        }
        */
    }

    private void displayPostReplyDialog() {
        Bundle args = new Bundle();
        // TODO: args.putString(Constants.THREAD, mAdapter.getState().getID() + "");

        displayPostReplyDialog(args);
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

    private class ParseEditPostTask extends AsyncTask<String, Void, String> {
        private String mPostId = null;

        public void onPreExecute() {
            mDialog = ProgressDialog.show(getActivity(), "Loading", 
                "Hold on...", true);
        }

        public String doInBackground(String... aParams) {
            String result = null;

            if (!isCancelled()) {
                try {
                    mPostId = aParams[0];

                    result = Reply.getPost(mPostId);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }
            return result;
        }

        public void onPostExecute(String aResult) {
            if (!isCancelled()) {
                if (mDialog != null) {
                    mDialog.dismiss();
                }

                Bundle args = new Bundle();

                // TODO: args.putString(Constants.THREAD, mAdapter.getState().getID() + "");
                args.putString(Constants.QUOTE, aResult);
                args.putBoolean(Constants.EDITING, true);
                args.putString(Constants.POST_ID, mPostId);

                displayPostReplyDialog(args);
            }
        }
    }

    private class ParsePostQuoteTask extends AsyncTask<String, Void, String> {
        public void onPreExecute() {
            mDialog = ProgressDialog.show(getActivity(), "Loading", "Hold on...", true);
        }

        public String doInBackground(String... aParams) {
            String result = null;

            if (!isCancelled()) {
                try {
                    result = Reply.getQuote(aParams[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }
            return result;
        }

        public void onPostExecute(String aResult) {
            if (!isCancelled()) {
                if (mDialog != null) {
                    mDialog.dismiss();
                }

                Bundle args = new Bundle();
                // TODO: args.putString(Constants.THREAD, Integer.toString(mAdapter.getState().getID()));
                args.putString(Constants.QUOTE, aResult);

                displayPostReplyDialog(args);
            }
        }
    }

    @Override
    public void dataUpdate(boolean pageChange, Bundle extras) {
        if (!this.isResumed()) {
            queueDataUpdate = true;
            queueDataExtras = extras;
        } else {
            queueDataUpdate = false;
            queueDataExtras = null;
            delayedDataUpdate(extras);
        }
    }

    public void delayedDataUpdate(Bundle extras) {
        // TODO: setActionbarTitle(mAdapter.getTitle());

        /* TODO: 
        if (AwfulActivity.useLegacyActionbar()) {
            if (mAdapter.getPage() == mAdapter.getLastPage()) {
                mNext.setVisibility(View.GONE);
            } else {
                mNext.setVisibility(View.VISIBLE);
            }

            if(mAdapter.getThreadClosed()){
                mReply.setVisibility(View.GONE);
            } else {
                mReply.setVisibility(View.VISIBLE);
            }
        }
        */
    }

    public int getSavedPage() {
        return savedPage;
    }

    private void handleStatusUpdate(int aStatus) {
        switch (aStatus) {
            case ThreadSyncService.Status.WORKING:
                loadingStarted();
                break;
            case ThreadSyncService.Status.OKAY:
                loadingSucceeded();
                break;
            case ThreadSyncService.Status.ERROR:
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
            // TODO: mRefresh.startAnimation(mAdapter.getBlinkingAnimation());
        } else {
            getActivity().setProgressBarIndeterminateVisibility(false);
        }

        Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void loadingStarted() {
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
        if (AwfulActivity.useLegacyActionbar()) {
            mRefresh.setAnimation(null);
            mRefresh.setVisibility(View.GONE);
        } else {
            getActivity().setProgressBarIndeterminateVisibility(false);
        }
    }
    
    public void imageLoadingStarted() {
        if (AwfulActivity.useLegacyActionbar()) {
        	if(mRefresh != null){
	            mRefresh.setVisibility(View.VISIBLE);
	            mRefresh.setImageResource(android.R.drawable.ic_menu_mapmode);
	            // TODO: mRefresh.startAnimation(mAdapter.getBlinkingAnimation());
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
		initPageCountCallbacks();
        
        /* TODO: 
        mPageCountText.setText("Page " + mAdapter.getPage() + "/" + mAdapter.getLastPage());
        */

        try {
            mThreadView.addJavascriptInterface(new ClickInterface(), "listener");
            mThreadView.addJavascriptInterface(getSerializedPreferences(new AwfulPreferences(getActivity())), "preferences");

            mThreadView.loadDataWithBaseURL("http://forums.somethingawful.com", 
                    AwfulThread.getHtml(aPosts, new AwfulPreferences(getActivity()), isTablet()), "text/html", "utf-8", null);
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
                        onPostActionItemSelected(aItem, aPostId, aLastReadUrl, aUsername);
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
                        onPostActionItemSelected(aItem, aPostId, aLastReadUrl, null);
                    }
                })
                .show();
        }

        public void onPreviousPageClick() {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    // TODO: goToPage(mAdapter.getPage() - 1);
                }
            });
        }

        public void onNextPageClick() {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    // TODO: goToPage(mAdapter.getPage() + 1);
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
	public void onServiceConnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPreferenceChange(AwfulPreferences mPrefs) {
		// don't need this, threadview is automatically refreshed on resume.
		
	}

	public void setPostJump(String postID) {
		mPostJump = postID;
	}
	
	public void goToPage(int aPage){
		// TODO: mAdapter.goToPage(aPage);
		// TODO: mPageCountText.setText("Page " + mAdapter.getPage() + "/" + mAdapter.getLastPage());
		mPostJump = "";
	}

	public int getPage() {
        return 1;
		// TODO: return mAdapter.getPage();
	}

	public int getThreadId() {
        return 12345678;
		// TODO: return mAdapter.getCurrentId();
	}

    private class PostLoaderManager implements LoaderManager.LoaderCallbacks<Cursor> {
        private boolean mLoading;
        private Cursor mData;

        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            String sortOrder = AwfulPost.DATE + " ASC" + " LIMIT 40";

            String selection = AwfulPost.THREAD_ID + "='" 
                + Integer.toString(ThreadDisplayFragment.this.getArguments().getInt(Constants.THREAD_ID, -1)) + "'";

            mLoading = true;

            return new CursorLoader(getActivity(), AwfulPost.CONTENT_URI, 
                    null, selection, null, null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
            populateThreadView(AwfulPost.fromCursor(getActivity(), aData));
            mLoading = false;
        }

        public boolean isLoading() {
            return mLoading;
        }

        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
            // TODO: mAdapter.swapCursor(null);
        }
    }

    private class PostContentObserver extends ContentObserver {
        public PostContentObserver(Handler aHandler) {
            super(aHandler);
        }
    }
}
