package com.ferg.awful;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.ferg.awful.preferences.AwfulPreferences;

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
		if(getAwfulActivity() != null){
			getAwfulActivity().updateActionbarTheme();
		}
	}
    
}
