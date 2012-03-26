package com.ferg.awful;

import android.app.Activity;

import com.actionbarsherlock.app.SherlockFragment;
import com.ferg.awful.preferences.AwfulPreferences;

public abstract class AwfulFragment extends SherlockFragment implements AwfulUpdateCallback{

    protected AwfulPreferences mPrefs;
    
    @Override
    public void onAttach(Activity aActivity) {
    	super.onAttach(aActivity);
        mPrefs = new AwfulPreferences(getAwfulActivity(), this);
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
        mPrefs.unregisterCallback(this);
        mPrefs.unRegisterListener();
    }
	
	
	public AwfulActivity getAwfulActivity(){
		return (AwfulActivity) getActivity();
	}
}
