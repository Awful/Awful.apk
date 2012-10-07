package com.ferg.awfulapp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.androidquery.AQuery;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.widget.AwfulProgressBar;
import com.ferg.awfulapp.widget.AwfulFragmentPagerAdapter.AwfulPagerFragment;

public abstract class AwfulFragment extends SherlockFragment implements AwfulUpdateCallback, AwfulPagerFragment, ActionMode.Callback{
	protected static String TAG = "AwfulFragment";
	protected AwfulPreferences mPrefs;
	protected AQuery aq;
	protected int currentProgress = 100;
	private AwfulProgressBar mProgressBar;
	

    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message aMsg) {
        	if(getActivity()!= null){
	        	AwfulSyncService.debugLogReceivedMessage(TAG, aMsg);
	        	if(aMsg.what == AwfulSyncService.MSG_ERR_NOT_LOGGED_IN){
	        		AwfulActivity aa = getAwfulActivity();
	            	aa.setSupportProgressBarIndeterminateVisibility(false);
	        		aa.setSupportProgressBarVisibility(false);
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
        mPrefs = new AwfulPreferences(getAwfulActivity(), this);
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
	}
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
        mPrefs.unregisterCallback(this);
        mPrefs.unRegisterListener();
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
		if(mProgressBar != null){
			mProgressBar.setProgress(percent);
		}else{
			AwfulActivity aa = getAwfulActivity();
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
		if(getActivity()!=null){
			return getAwfulActivity().isFragmentVisible(this);
		}
		return false;
	}
	
	protected void startActionMode(){
		if(getAwfulActivity() != null){
			getAwfulActivity().startActionMode(this);
		}
	}
	
	@Override
    public void loadingFailed(Message aMsg) {
		AwfulActivity aa = getAwfulActivity();
        if(aa != null){
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
	
	public void sendFragmentMessage(String type, String contents){
		AwfulActivity aa = getAwfulActivity();
    	if(aa != null){
    		aa.fragmentMessage(type, contents);
    	}
	}
	
	/**
	 * Default implementation ignores messages from other fragments. Override this function to receive messages.
	 */
	public void fragmentMessage(String type, String contents){	}
	
	protected boolean isLoggedIn(){
		return getAwfulActivity().isLoggedIn();
	}
	
	public boolean onBackPressed() {
		return false;
	}

	@Override
	public boolean canScrollX(int x, int y) {
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
