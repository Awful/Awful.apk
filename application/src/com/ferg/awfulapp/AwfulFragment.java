package com.ferg.awfulapp;

import android.app.Activity;
import android.os.Bundle;
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
import com.ferg.awfulapp.widget.AwfulFragmentPagerAdapter.AwfulPagerFragment;

public abstract class AwfulFragment extends SherlockFragment implements AwfulUpdateCallback, AwfulPagerFragment, ActionMode.Callback{

	protected AwfulPreferences mPrefs;
	protected AQuery aq;
    
    @Override
    public void onAttach(Activity aActivity) {
    	super.onAttach(aActivity);
    	if(!(aActivity instanceof AwfulActivity)){
    		Log.e("AwfulFragment","PARENT ACTIVITY NOT EXTENDING AwfulActivity!");
    	}
        mPrefs = new AwfulPreferences(getAwfulActivity(), this);
    }
    
    protected View inflateView(int resId, ViewGroup container, LayoutInflater inflater){
    	View v = inflater.inflate(R.layout.forum_display, container, false);
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
	
	protected void setProgress(int percent){
		AwfulActivity aa = getAwfulActivity();
		if(aa != null){
			aa.setSupportProgressBarVisibility(percent<100);
			aa.setSupportProgress(percent*100);
		}
	}
	
	protected void startActionMode(){
		if(getAwfulActivity() != null){
			getAwfulActivity().startActionMode(this);
		}
	}
	
	@Override
    public void loadingFailed() {
        if(getActivity() != null){
        	getAwfulActivity().setSupportProgressBarIndeterminateVisibility(false);
        }
    }

    @Override
    public void loadingStarted() {
    	if(getActivity() != null){
    		getAwfulActivity().setSupportProgressBarIndeterminateVisibility(true);
    	}
    }

    @Override
    public void loadingSucceeded() {
    	if(getActivity() != null){
    		getAwfulActivity().setSupportProgressBarIndeterminateVisibility(false);
    	}
    }

	@Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		
	}

	public boolean onBackPressed() {
		return false;
	}

	@Override
	public boolean canScrollX(int x) {
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
