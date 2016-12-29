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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v7.view.ActionMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.util.AwfulError;
import com.ferg.awfulapp.widget.AwfulProgressBar;

/**
 * AwfulFragment's red-headed step child.
 * Currently only exists for EmoteFragment, and usually falls behind on changes made to AwfulFragment.
 * Welp.
 */
public abstract class AwfulDialogFragment extends DialogFragment implements ActionMode.Callback, AwfulRequest.ProgressListener, AwfulPreferences.AwfulPreferenceUpdate {
	protected static String TAG = "AwfulFragment";
	protected AwfulPreferences mPrefs;
	protected int currentProgress = 100;
	private AwfulProgressBar mProgressBar;
	

    protected Handler mHandler = new Handler();
    
    @Override
    public void onAttach(Activity aActivity) {
    	super.onAttach(aActivity);
    	if(!(aActivity instanceof AwfulActivity)){
    		Log.e("AwfulFragment","PARENT ACTIVITY NOT EXTENDING AwfulActivity!");
    	}
        mPrefs = AwfulPreferences.getInstance(getAwfulActivity(), this);
    }
    
    protected View inflateView(int resId, ViewGroup container, LayoutInflater inflater){
    	View v = inflater.inflate(resId, container, false);
    	View progressBar = v.findViewById(R.id.progress_bar);
    	if(progressBar instanceof AwfulProgressBar){
    		mProgressBar = (AwfulProgressBar) progressBar;
    	}
    	return v;
    }
    
	@Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState);
		onPreferenceChange(mPrefs, null);
	}
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
        mPrefs.unregisterCallback(this);
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
    
    protected void displayThread(int aId, int aPage, int forumId, int forumPage) {
    	if(getActivity() != null){
    		getAwfulActivity().displayThread(aId, aPage, forumId, forumPage, false);
    	}
    }
	
	
	public AwfulActivity getAwfulActivity(){
		return (AwfulActivity) getActivity();
	}
	
	protected void displayForum(int forumId, int page){
		if(getAwfulActivity() != null){
			getAwfulActivity().displayForum(forumId, page);
		}
	}
	
    public void displayPostReplyDialog(final int threadId, final int postId, final int type) {
		if(getAwfulActivity() != null){
			getAwfulActivity().runOnUiThread(new Runnable(){
				@Override
				public void run() {
                    if(getActivity() != null){
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
		return isVisible();
	}
	
	protected void startActionMode(){
		if(getAwfulActivity() != null){
			getAwfulActivity().startSupportActionMode(this);
		}
	}

	@Override
	public void onPreferenceChange(AwfulPreferences prefs, String key) {
		
	}
	
	protected boolean isLoggedIn(){
		return getAwfulActivity().isLoggedIn();
	}
	
	public boolean onBackPressed() {
		return false;
	}

    public abstract String getTitle();

    public boolean volumeScroll(KeyEvent event) {
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

    public void queueRequest(Request request){
        queueRequest(request, false);
    }

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

    @Override
    public void requestStarted(AwfulRequest req) {
        AwfulActivity aa = getAwfulActivity();
        if(aa != null){
            aa.setSupportProgressBarVisibility(false);
            aa.setSupportProgressBarIndeterminateVisibility(true);
        }
    }

    @Override
    public void requestUpdate(AwfulRequest req, int percent) {
        setProgress(percent);
    }

    @Override
    public void requestEnded(AwfulRequest req, VolleyError error) {
        AwfulActivity aa = getAwfulActivity();
        if(aa != null){
            aa.setSupportProgressBarIndeterminateVisibility(false);
            aa.setSupportProgressBarVisibility(false);
        }
        if(error instanceof AwfulError){
            displayAlert((AwfulError) error);
        }else if(error != null){
            displayAlert(R.string.loading_failed);
        }
    }

    private static final int ALERT_DISPLAY_MILLIS = 3000;
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

    protected void displayAlertInternal(String title, String subtext, int iconRes, Animation animate){
        if(getActivity() == null){
            return;
        }
        View popup = LayoutInflater.from(getActivity()).inflate(R.layout.alert_popup, null);
        TextView popupTitle = (TextView)popup.findViewById(R.id.popup_title);
        popupTitle.setText(title);
        TextView popupSubTitle = (TextView) popup.findViewById(R.id.popup_subtitle);
        if(TextUtils.isEmpty(subtext)){
            popupSubTitle.setVisibility(View.GONE);
        }else{
            popupSubTitle.setVisibility(View.VISIBLE);
            popupSubTitle.setText(subtext);
        }
        if(iconRes != 0){
            ImageView popupIcon = (ImageView) popup.findViewById(R.id.popup_icon);
            if(animate != null){
                popupIcon.setImageResource(iconRes);
                popupIcon.startAnimation(animate);
            }else{
                popupIcon.setImageResource(iconRes);
            }
        }
        Toast toast = new Toast(AwfulPreferences.getInstance().getContext());
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
    
}
