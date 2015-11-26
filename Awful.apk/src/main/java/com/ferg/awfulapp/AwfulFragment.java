/********************************************************************************
 * Copyright (c) 2012, Matthew Shepard
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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v7.view.ActionMode;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.androidquery.AQuery;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.util.AwfulError;
import com.ferg.awfulapp.widget.AwfulProgressBar;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

public abstract class AwfulFragment extends Fragment implements ActionMode.Callback, AwfulRequest.ProgressListener, AwfulPreferences.AwfulPreferenceUpdate {
	protected String TAG = "AwfulFragment";
    protected static final boolean DEBUG = Constants.DEBUG;

	protected AwfulPreferences mPrefs;
	protected AQuery aq;
	protected int currentProgress = 100;
	private AwfulProgressBar mProgressBar;
	protected SwipyRefreshLayout mSRL;
	

    protected Handler mHandler = new Handler();

    @Override
    public void onAttach(Activity aActivity) {
    	super.onAttach(aActivity); if(DEBUG) Log.e(TAG, "onAttach");
    	if(!(aActivity instanceof AwfulActivity)){
    		Log.e("AwfulFragment","PARENT ACTIVITY NOT EXTENDING AwfulActivity!");
    	}
    	if(mPrefs == null){
    		mPrefs = AwfulPreferences.getInstance(getAwfulActivity(), this);
    	}
    }
    
    protected View inflateView(int resId, ViewGroup container, LayoutInflater inflater){
    	View v = inflater.inflate(resId, container, false);
    	View progressBar = v.findViewById(R.id.progress_bar);
    	if(progressBar instanceof AwfulProgressBar){
    		mProgressBar = (AwfulProgressBar) progressBar;
    	}
    	aq = new AQuery(v);
    	return v;
    }
    
	@Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState); if(DEBUG) Log.e(TAG, "onActivityCreated");
		onPreferenceChange(mPrefs, null);
	}

    @Override
    public void onStart() {
        super.onStart(); if(DEBUG) Log.e(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume(); if(DEBUG) Log.e(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause(); if(DEBUG) Log.e(TAG, "onPause");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState); if(DEBUG) Log.e(TAG,"onSaveInstanceState");
    }

    @Override
    public void onStop() {
        super.onStop(); if(DEBUG) Log.e(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
    	super.onDestroy(); if(DEBUG) Log.e(TAG, "onDestroy");
        this.cancelNetworkRequests();
        mHandler.removeCallbacksAndMessages(null);
        mPrefs.unregisterCallback(this);
    }

    @Override
    public void onDetach() {
        super.onDetach(); if(DEBUG) Log.e(TAG, "onDetach");
        this.cancelNetworkRequests();
        mHandler.removeCallbacksAndMessages(null);
    }
    
    protected void displayForumIndex(){
    	if(getActivity() != null){
    		getAwfulActivity().displayForumIndex();
    	}
    }
    
    protected void displayForumContents(int aId) {
    	if(getActivity() != null){
    		getAwfulActivity().displayForum(aId, 1);
    	}
    }
    
    protected void displayThread(int aId, int aPage, int forumId, int forumPage, boolean forceReload) {
    	if(getActivity() != null){
    		getAwfulActivity().displayThread(aId, aPage, forumId, forumPage, forceReload);
    	}
    }
	
	
	public AwfulActivity getAwfulActivity(){
		return (AwfulActivity) getActivity();
	}
	
	protected void displayForum(long forumId, long page){
		if(getAwfulActivity() != null){
			getAwfulActivity().displayForum((int) forumId, (int) page);
		}
	}
	
    public void displayPostReplyDialog(final int threadId, final int postId, final int type) {
		if(getAwfulActivity() != null){
			getAwfulActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (getActivity() != null) {
                        startActivityForResult(
                                new Intent(getActivity(), PostReplyActivity.class)
                                        .putExtra(Constants.REPLY_THREAD_ID, threadId)
                                        .putExtra(Constants.EDITING, type)
                                        .putExtra(Constants.REPLY_POST_ID, postId),
                                PostReplyFragment.REQUEST_POST);
                    }
                }
            });
		}
    }
	
	protected void setProgress(int percent){
		currentProgress = percent;
		if(currentProgress > 0 && mSRL != null){
            mSRL.setRefreshing(false);
		}
		if(mProgressBar != null){
			mProgressBar.setProgress(percent, getActivity());
		}
	}
	
	public int getProgressPercent(){
		return currentProgress;
	}
	
	protected void setTitle(String title){
		if(getActivity()!=null){
			getAwfulActivity().setActionbarTitle(title, this);
		}
	}
	
	protected boolean isFragmentVisible(){
		if(getActivity()!=null){
			return getAwfulActivity().isFragmentVisible(this);
		}
		return false;
	}
	
	protected void startActionMode(){
		if(getAwfulActivity() != null){
			getAwfulActivity().startSupportActionMode(this);
		}
	}

    @Override
    public void requestStarted(AwfulRequest req) {
        if(mSRL != null){
            // P2R Library is ... awful - part 1
            mSRL.setDirection(SwipyRefreshLayoutDirection.TOP);
            mSRL.setRefreshing(true);
        }
    }

    @Override
    public void requestUpdate(AwfulRequest req, int percent) {
        setProgress(percent);
    }

    @Override
    public void requestEnded(AwfulRequest req, VolleyError error) {
        if(mSRL != null){
            mSRL.setRefreshing(false);
            // P2R Library is ... awful - part 2
            if(this instanceof ThreadDisplayFragment){
                mSRL.setDirection(SwipyRefreshLayoutDirection.BOTH);
            }else{
                mSRL.setDirection(SwipyRefreshLayoutDirection.TOP);
            }
        }
        if(error instanceof AwfulError){
            displayAlert((AwfulError) error);
        }else if(error != null){
            displayAlert(R.string.loading_failed);
        }
    }

    @Override
	public void onPreferenceChange(AwfulPreferences prefs, String key) {
		if(mSRL != null){
            DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
            float dpHeight = displayMetrics.heightPixels / displayMetrics.density;

            mSRL.setDistanceToTriggerSync(Math.round(prefs.p2rDistance*dpHeight));
		}
	}

	protected boolean isLoggedIn(){
		return getAwfulActivity().isLoggedIn();
	}
	
	public boolean onBackPressed() {
		return false;
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		return false;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {	}

    protected AwfulApplication getAwfulApplication(){
        AwfulActivity act = getAwfulActivity();
        if(act != null){
            return (AwfulApplication) act.getApplication();
        }
        return null;
    }
    public void queueRequest(Request request){
        queueRequest(request, false);
    }

    /**
     * Queue a network {@link Request}.
     * Set true to tag the request with the fragment, so it will be cancelled
     * when the fragment is destroyed. Set false if you want to retain the request's
     * default tag, e.g. so pending {@link com.ferg.awfulapp.task.PostRequest}s can
     * be cancelled when starting a new one.
     * @param request           A Volley request
     * @param cancelOnDestroy   Whether to tag with the fragment and automatically cancel
     */
    public void queueRequest(Request request, boolean cancelOnDestroy){
        if(request != null){
            if(cancelOnDestroy){
                request.setTag(this);
            }
            NetworkUtils.queueRequest(request);
        }
    }

    protected void cancelNetworkRequests(){
        NetworkUtils.cancelRequests(this);
    }



    protected void invalidateOptionsMenu() {
        AwfulActivity act = getAwfulActivity();
        if(act != null){
            act.supportInvalidateOptionsMenu();
        }
    }

    public abstract String getTitle();
    public abstract void onPageVisible();
    public abstract void onPageHidden();
    public abstract String getInternalId();
    public abstract boolean volumeScroll(KeyEvent event);


    protected void displayAlert(int titleRes){
        if(getActivity() != null){
            displayAlert(getString(titleRes), null, 0, null);
        }
    }

    protected void displayAlert(int titleRes, int subtitleRes, int iconRes){
        if(getActivity() != null){
            if(subtitleRes != 0){
                displayAlert(getString(titleRes), getString(subtitleRes), iconRes, null);
            }else{
                displayAlert(getString(titleRes), null, iconRes, null);
            }
        }
    }

    protected void displayAlert(AwfulError error){
        displayAlert(error.getMessage(), error.getSubMessage(), error.getIconResource(), error.getIconAnimation());
    }

    protected void displayAlert(String title){
        displayAlert(title, null, 0, null);
    }

    protected void displayAlert(String title, int iconRes){
        displayAlert(title, null, iconRes, null);
    }

    protected void displayAlert(String title, String subtext){
        displayAlert(title, subtext, 0, null);
    }

    private void displayAlert(final String title, final String subtext, final int iconRes, final Animation animate){
        if(Looper.getMainLooper().equals(Looper.myLooper())){
            displayAlertInternal(title, subtext, iconRes, animate);
        }else{
            //post on main thread, if this is called from a secondary thread.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    displayAlertInternal(title, subtext, iconRes, animate);
                }
            });
        }
    }

    private void displayAlertInternal(String title, String subtext, int iconRes, Animation animate){
        if(getActivity() == null){
            return;
        }
        View popup = LayoutInflater.from(getActivity()).inflate(R.layout.alert_popup, null);
        AQuery aq = new AQuery(popup);
        aq.find(R.id.popup_title).text(title);
        if(TextUtils.isEmpty(subtext)){
            aq.find(R.id.popup_subtitle).gone();
        }else{
            aq.find(R.id.popup_subtitle).visible().text(subtext);
        }
        if(iconRes != 0){

            int [] attrs = { iconRes};
            TypedArray ta = getView().getContext().getTheme().obtainStyledAttributes(attrs);
            if(animate != null){
                aq.find(R.id.popup_icon).image(ta.getDrawable(0)).animate(animate);
            }else{
                aq.find(R.id.popup_icon).image(ta.getDrawable(0));
            }
        }
        Toast toast = new Toast(getAwfulApplication());
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(popup);
        toast.show();
    }

    protected void restartLoader(int id, Bundle data, LoaderManager.LoaderCallbacks<? extends Object> callback) {
        if(getActivity() != null){
            getLoaderManager().restartLoader(id, data, callback);
        }
    }


    /**
     * Utility method to safely handle clipboard copying.
     * A <a href="https://code.google.com/p/android/issues/detail?id=58043">bug in 4.3</a>
     * means that clipboard writes can throw runtime exceptions if another app has registered
     * as a listener. This method catches them and displays an error message for the user.
     *
     * @param label             The {@link ClipData}'s label
     * @param clipText          The {@link ClipData}'s text
     * @param successMessageId  If supplied, a success message popup will be displayed
     * @return                  false if the copy failed
     */
    protected boolean safeCopyToClipboard(String label,
                                          String clipText,
                                          @Nullable @StringRes Integer successMessageId) {
        ClipboardManager clipboard = (ClipboardManager) this.getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, clipText);
        try {
            clipboard.setPrimaryClip(clip);
            if (successMessageId != null) {
                displayAlert(successMessageId, 0, R.attr.iconMenuLink);
            }
            return true;
        } catch (IllegalArgumentException | SecurityException e) {
            displayAlert("Unable to copy to clipboard!",
                    "Another app has locked access, you may need to reboot",
                    R.attr.iconMenuLoadFailed, null);
            e.printStackTrace();
            return false;
        }
    }
}
