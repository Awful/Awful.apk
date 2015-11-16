package com.ferg.awfulapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.task.FeatureRequest;
import com.ferg.awfulapp.task.ProfileRequest;

import java.io.File;
import java.util.LinkedList;

/**
 * Convenience class to avoid having to call a configurator's lifecycle methods everywhere. This
 * class should avoid implementing things directly; the ActivityConfigurator does that job.
 * 
 * Most Activities in this awful app should extend this guy; that will provide things like locking
 * orientation according to user preference.
 * 
 * This class also provides a few helper methods for grabbing preferences and the like.
 */
public class AwfulActivity extends AppCompatActivity implements AwfulPreferences.AwfulPreferenceUpdate {
    protected static String TAG = "AwfulActivity";
    protected static final boolean DEBUG = Constants.DEBUG;
	private ActivityConfigurator mConf;
    
    private boolean loggedIn = false;
    
    protected AQuery aq;

    private TextView mTitleView;
    
    protected AwfulPreferences mPrefs;
    
    public void reauthenticate(){
    	NetworkUtils.clearLoginCookies(this);
        startActivityForResult(new Intent(this, AwfulLoginActivity.class), Constants.LOGIN_ACTIVITY_REQUEST);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPrefs = AwfulPreferences.getInstance(this,this);
        if(mPrefs.theme.equals(ColorProvider.DEFAULT) || mPrefs.theme.equals(ColorProvider.CLASSIC)){
            setTheme(R.style.Theme_AwfulTheme);
        }else if(mPrefs.theme.equals(ColorProvider.FYAD)){
            setTheme(R.style.Theme_AwfulTheme_FYAD);
        }else if(mPrefs.theme.equals(ColorProvider.BYOB)){
            setTheme(R.style.Theme_AwfulTheme_BYOB);
        }else if(mPrefs.theme.equals(ColorProvider.YOSPOS)){
            setTheme(R.style.Theme_AwfulTheme_YOSPOS);
        }else if(mPrefs.theme.equals(ColorProvider.AMBERPOS)){
            setTheme(R.style.Theme_AwfulTheme_AMBERPOS);
        }else{
            setTheme(R.style.Theme_AwfulTheme_Dark);
        }
        super.onCreate(savedInstanceState); if(DEBUG) Log.e(TAG, "onCreate");
        aq = new AQuery(this);
        mConf = new ActivityConfigurator(this);
        mConf.onCreate();
    }

    @Override
    protected void onStart() {
        super.onStart(); if(DEBUG) Log.e(TAG, "onStart");
        mConf.onStart();
    }
    
    @Override
    protected void onResume() {
        super.onResume(); if(DEBUG) Log.e(TAG, "onResume");
        mConf.onResume();
        // check login state when coming back into use, instead of in onCreate
        loggedIn = NetworkUtils.restoreLoginCookies(this.getAwfulApplication());
        if (isLoggedIn()) {
        	if(mPrefs.ignoreFormkey == null || mPrefs.userTitle == null){
        		 NetworkUtils.queueRequest(new ProfileRequest(this).build(null, null));
        	}
            if(mPrefs.ignoreFormkey == null){
                NetworkUtils.queueRequest(new FeatureRequest(this).build(null, null));
            }
            Log.v(TAG, "Cookie Loaded!");
        } else {
        	if(!(this instanceof AwfulLoginActivity)){
        		reauthenticate();
        	}
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause(); if(DEBUG) Log.e(TAG, "onPause");
        mConf.onPause();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onStop() {
        super.onStop(); if(DEBUG) Log.e(TAG, "onStop");
        mConf.onStop();
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if(cache != null){
            cache.flush();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy(); if(DEBUG) Log.e(TAG, "onDestroy");
        mConf.onDestroy();
        mPrefs.unregisterCallback(this);
    }


    @Override
	protected void onActivityResult(int request, int result, Intent intent) {
		super.onActivityResult(request, result, intent);
    	Log.w(TAG,"onActivityResult: " + request+" result: "+result);
		if(request == Constants.LOGIN_ACTIVITY_REQUEST && result == Activity.RESULT_CANCELED){
			finish();
		}
	}

	protected void setActionBar() {

        ActionBar action = getSupportActionBar();
        action.setDisplayShowTitleEnabled(false);
        action.setCustomView(R.layout.actionbar_title);
        mTitleView = (TextView) action.getCustomView();
        mTitleView.setMovementMethod(new ScrollingMovementMethod());
        updateActionbarTheme();
        action.setDisplayShowCustomEnabled(true);
        action.setDisplayHomeAsUpEnabled(true);
    }
    
    protected void updateActionbarTheme(){
        ActionBar action = getSupportActionBar();
        if(action != null && mTitleView != null){
	        //action.setBackgroundDrawable(new ColorDrawable(ColorProvider.getActionbarColor()));
	        mTitleView.setTextColor(ColorProvider.getActionbarFontColor());
	        setPreferredFont(mTitleView, Typeface.NORMAL);
        }
    }

    public void displayUserCP() {
    	displayForum(Constants.USERCP_ID, 1);
    }
    
    public void displayThread(int id, int page, int forumId, int forumPage, boolean forceReload){
    	startActivity(new Intent().setClass(this, ForumsIndexActivity.class)
    							  .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    							  .putExtra(Constants.THREAD_ID, id)
    							  .putExtra(Constants.THREAD_PAGE, page)
    							  .putExtra(Constants.FORUM_ID, forumId)
    							  .putExtra(Constants.FORUM_PAGE, forumPage));
    }
    
    public void displayForum(int id, int page){
    	startActivity(new Intent().setClass(this, ForumsIndexActivity.class)
    							  .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    							  .putExtra(Constants.FORUM_ID, id)
    							  .putExtra(Constants.FORUM_PAGE, page));
    }
    
    public void displayForumIndex(){
    	startActivity(new Intent().setClass(this, ForumsIndexActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void setActionbarTitle(String aTitle, Object requestor) {
        ActionBar action = getSupportActionBar();
        if(action != null){
        	mTitleView = (TextView) action.getCustomView();
        }
    	if(aTitle != null && mTitleView != null && aTitle.length()>0){
//    		mTitleView.setText(Html.fromHtml(aTitle));
    		mTitleView.setText(aTitle);
			mTitleView.scrollTo(0, 0);
    	}else{
    		Log.e(TAG, "FAILED setActionbarTitle - "+aTitle);
    	}
    }
    
    public AwfulApplication getAwfulApplication(){
    	return (AwfulApplication) getApplication();
    }
    
    public void setPreferredFont(View view, int flags){
    	if(getApplication() != null && view != null){
    		((AwfulApplication)getApplication()).setPreferredFont(view, flags);
    	}
    }
    
    public void setPreferredFont(View view){
    	setPreferredFont(view, -1);
    }

	@Override
	public void onPreferenceChange(AwfulPreferences prefs, String key) {
        Log.d(TAG, "Key changed: "+key);
        if("theme".equals(key) || "page_layout".equals(key)) {
            if (mPrefs.theme.equals(ColorProvider.DEFAULT) || mPrefs.theme.equals(ColorProvider.CLASSIC)) {
                setTheme(R.style.Theme_AwfulTheme);
            } else if (mPrefs.theme.equals(ColorProvider.FYAD)) {
                setTheme(R.style.Theme_AwfulTheme_FYAD);
            } else if (mPrefs.theme.equals(ColorProvider.BYOB)) {
                setTheme(R.style.Theme_AwfulTheme_BYOB);
            } else if (mPrefs.theme.equals(ColorProvider.YOSPOS)) {
                setTheme(R.style.Theme_AwfulTheme_YOSPOS);
            } else if (mPrefs.theme.equals(ColorProvider.AMBERPOS)) {
                setTheme(R.style.Theme_AwfulTheme_AMBERPOS);
            } else {
                setTheme(R.style.Theme_AwfulTheme_Dark);
            }
            afterThemeChange();
        }
		updateActionbarTheme();
	}
	
	protected boolean isLoggedIn(){
		if(!loggedIn){
			loggedIn = NetworkUtils.restoreLoginCookies(this.getAwfulApplication());
		}
		return loggedIn;
	}

	public boolean isFragmentVisible(AwfulFragment awfulFragment) {
		return true;
	}


	
    public void afterThemeChange() {
        recreate();
    }

	@Override
	public File getCacheDir() {
		Log.e(TAG,"getCacheDir(): "+super.getCacheDir());
		return super.getCacheDir();
	}
}
