package com.ferg.awfulapp;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.preferences.AwfulPreferences;

public abstract class AwfulFragment extends SherlockFragment implements AwfulUpdateCallback{

	protected AwfulPreferences mPrefs;
    
    @Override
    public void onAttach(Activity aActivity) {
    	super.onAttach(aActivity);
    	if(!(aActivity instanceof AwfulActivity)){
    		Log.e("AwfulFragment","PARENT ACTIVITY NOT EXTENDING AwfulActivity!");
    	}
        mPrefs = new AwfulPreferences(getAwfulActivity(), this);
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
    
}
