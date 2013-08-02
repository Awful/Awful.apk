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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.DialogFragment;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.androidquery.AQuery;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.widget.AwfulProgressBar;

public abstract class AwfulDialogFragment extends DialogFragment implements AwfulUpdateCallback, ActionMode.Callback{
	protected static String TAG = "AwfulFragment";
	protected AwfulPreferences mPrefs;
	protected AQuery aq;
	protected int currentProgress = 100;
	private AwfulProgressBar mProgressBar;
	

    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message aMsg) {
    		AwfulActivity aa = getAwfulActivity();
        	if(aa != null){
	        	AwfulSyncService.debugLogReceivedMessage(TAG, aMsg);
	        	if(aMsg.what == AwfulSyncService.MSG_ERROR){
                    loadingFailed(aMsg);
	        	}else if(aMsg.what == AwfulSyncService.MSG_ERR_NOT_LOGGED_IN){
                    loadingFailed(aMsg);
	        		aa.reauthenticate();
	        	}else if(aMsg.what == AwfulSyncService.MSG_PROGRESS_PERCENT){
	        		loadingUpdate(aMsg);
	        	}else{
		            switch (aMsg.arg1) {
		                case AwfulSyncService.Status.WORKING:
		                    loadingStarted(aMsg);
		                    break;
		                case AwfulSyncService.Status.OKAY:
		                    loadingSucceeded(aMsg);
		                    break;
		                case AwfulSyncService.Status.ERROR:
		                    loadingFailed(aMsg);
		                    break;
		            };
	        	}
        	}
        }
    };

    protected Messenger mMessenger = new Messenger(mHandler);
    
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
    	aq = new AQuery(v);
    	return v;
    }
    
	@Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState);
		onPreferenceChange(mPrefs);
		if(mProgressBar != null){
			mProgressBar.setBackgroundColor(ColorProvider.getBackgroundColor());
		}
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
    		getAwfulActivity().displayThread(aId, aPage, forumId, forumPage);
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
			    	getAwfulActivity().displayReplyWindow(threadId, postId, type);
				}
			});
		}
    }
	
	protected void setProgress(int percent){
		currentProgress = percent;
		AwfulActivity aa = getAwfulActivity();
		if(mProgressBar != null){
			mProgressBar.setProgress(percent);
			if(aa != null && isFragmentVisible()){
				aa.hideProgressBar();
			}
		}else{
			if(aa != null && isFragmentVisible()){
				aa.setLoadProgress(percent);
			}
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
    public void loadingFailed(Message aMsg) {
		AwfulActivity aa = getAwfulActivity();
        if(aa != null){
        	setProgress(100);
        	aa.setSupportProgressBarIndeterminateVisibility(false);
			aa.setSupportProgressBarVisibility(false);
        }
    }

    @Override
    public void loadingStarted(Message aMsg) {
		AwfulActivity aa = getAwfulActivity();
    	if(aa != null){
    		aa.setSupportProgressBarIndeterminateVisibility(true);
    	}
    }

    @Override
    public void loadingSucceeded(Message aMsg) {
		AwfulActivity aa = getAwfulActivity();
    	if(aa != null){
    		aa.setSupportProgressBarIndeterminateVisibility(false);
			aa.setSupportProgressBarVisibility(false);
    	}
    }
    
    @Override
    public void loadingUpdate(Message aMsg) {
    	setProgress(aMsg.arg2);
    }
    
	@Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		
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
    
}
