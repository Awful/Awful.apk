package com.ferg.awfulapp;

import java.io.File;
import java.util.LinkedList;

import com.ferg.awfulapp.util.AwfulUtils;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher.Options;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import com.androidquery.AQuery;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.widget.AwfulHeaderTransformer;

/**
 * Convenience class to avoid having to call a configurator's lifecycle methods everywhere. This
 * class should avoid implementing things directly; the ActivityConfigurator does that job.
 * 
 * Most Activities in this awful app should extend this guy; that will provide things like locking
 * orientation according to user preference.
 * 
 * This class also provides a few helper methods for grabbing preferences and the like.
 */
public class AwfulActivity extends ActionBarActivity implements AwfulPreferences.AwfulPreferenceUpdate {
    protected static String TAG = "AwfulActivity";
    protected static final boolean DEBUG = Constants.DEBUG;
	private ActivityConfigurator mConf;
    private LinkedList<Message> mMessageQueue = new LinkedList<Message>();
    
    private boolean loggedIn = false;
    
    protected AQuery aq;

    private TextView mTitleView;
    
    protected AwfulPreferences mPrefs;
    
	protected PullToRefreshAttacher mP2RAttacher;
    
    public void reauthenticate(){
    	NetworkUtils.clearLoginCookies(this);
        startActivityForResult(new Intent(this, AwfulLoginActivity.class), Constants.LOGIN_ACTIVITY_REQUEST);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); if(DEBUG) Log.e(TAG, "onCreate");
        aq = new AQuery(this);
        mConf = new ActivityConfigurator(this);
        mConf.onCreate();
        mPrefs = AwfulPreferences.getInstance(this, this);
        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        supportRequestWindowFeature(Window.FEATURE_PROGRESS);
        loggedIn = NetworkUtils.restoreLoginCookies(this);
    	Options p2roptions = new Options();
    	p2roptions.headerTransformer = new AwfulHeaderTransformer();
    	p2roptions.refreshOnUp = true;
    	mP2RAttacher = PullToRefreshAttacher.get(this, p2roptions);
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
        
        if (isLoggedIn()) {
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
        if(AwfulUtils.isICS()){
            HttpResponseCache cache = HttpResponseCache.getInstalled();
            if(cache != null){
                cache.flush();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy(); if(DEBUG) Log.e(TAG, "onDestroy");
        mConf.onDestroy();
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
        updateActionbarTheme(mPrefs);
        action.setDisplayShowCustomEnabled(true);
        action.setDisplayHomeAsUpEnabled(true);
    }
    
    protected void updateActionbarTheme(AwfulPreferences aPrefs){
        ActionBar action = getSupportActionBar();
        if(action != null && mTitleView != null){
	        action.setBackgroundDrawable(new ColorDrawable(ColorProvider.getActionbarColor()));
	        mTitleView.setTextColor(ColorProvider.getActionbarFontColor());
	        setPreferredFont(mTitleView, Typeface.NORMAL);
        }
    }

    public void displayUserCP() {
    	displayForum(Constants.USERCP_ID, 1);
    }
    
    public void displayThread(int id, int page, int forumId, int forumPage){
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
    
	public void displayReplyWindow(int threadId, int postId, int type) {
    	startActivityForResult(
                new Intent(this, PostReplyActivity.class)
                    .putExtra(Constants.REPLY_THREAD_ID, threadId)
                    .putExtra(Constants.EDITING, type)
                    .putExtra(Constants.REPLY_POST_ID, postId),
                PostReplyFragment.REQUEST_POST);
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
    
    public static boolean isHoneycomb(){
    	return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

	@Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		updateActionbarTheme(prefs);
	}
	
	protected boolean isLoggedIn(){
		if(!loggedIn){
			loggedIn = NetworkUtils.restoreLoginCookies(this);
		}
		return loggedIn;
	}

	public boolean isFragmentVisible(AwfulFragment awfulFragment) {
		return true;
	}

	public void setLoadProgress(int percent) {
		setSupportProgressBarVisibility(percent<100);
    	setSupportProgressBarIndeterminateVisibility(false);
		setSupportProgress(percent*100);
	}
	
	public void hideProgressBar(){
		setSupportProgressBarVisibility(false);
    	setSupportProgressBarIndeterminateVisibility(false);
	}

	


	@Override
	public File getCacheDir() {
		Log.e(TAG,"getCacheDir(): "+super.getCacheDir());
		return super.getCacheDir();
	}
	
	
	PullToRefreshAttacher getPullToRefreshAttacher() {
        return mP2RAttacher;
    }
}
