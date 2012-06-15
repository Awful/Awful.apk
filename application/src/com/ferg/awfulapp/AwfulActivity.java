package com.ferg.awfulapp;

import java.util.LinkedList;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulMessage;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;
import com.androidquery.AQuery;

/**
 * Convenience class to avoid having to call a configurator's lifecycle methods everywhere. This
 * class should avoid implementing things directly; the ActivityConfigurator does that job.
 * 
 * Most Activities in this awful app should extend this guy; that will provide things like locking
 * orientation according to user preference.
 * 
 * This class also provides a few helper methods for grabbing preferences and the like.
 */
public class AwfulActivity extends SherlockFragmentActivity implements ServiceConnection, AwfulUpdateCallback {
    private static final String TAG = "AwfulActivity";
	private ActivityConfigurator mConf;
    private Messenger mService = null;
    private LinkedList<Message> mMessageQueue = new LinkedList<Message>();
    
    private boolean loggedIn = false;
    
    protected AQuery aq;

    private TextView mTitleView;
    
    private AwfulPreferences mPrefs;
    
    private boolean isActive = false;
    private BroadcastReceiver br = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(Constants.UNREGISTERED_BROADCAST)){
				clearCookies();
				if(isActive){
					Toast.makeText(AwfulActivity.this, "You are logged out!", Toast.LENGTH_LONG).show();
					reauthenticate();
				}
			}
		}
    };
    
    private void clearCookies(){
        NetworkUtils.clearLoginCookies(this);
    }
    
    private void reauthenticate(){
        startActivity(new Intent(this, AwfulLoginActivity.class));
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        aq = new AQuery(this);
        mConf = new ActivityConfigurator(this);
        mConf.onCreate();
        mPrefs = new AwfulPreferences(this, this);
        bindService(new Intent(this, AwfulSyncService.class), this, BIND_AUTO_CREATE);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        registerReceiver(br, new IntentFilter(Constants.UNREGISTERED_BROADCAST));
        loggedIn = NetworkUtils.restoreLoginCookies(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mConf.onStart();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mConf.onResume();
        isActive = true;
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mConf.onPause();
        isActive = false;
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        mConf.onStop();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConf.onDestroy();
        unbindService(this);
        unregisterReceiver(br);
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
	        action.setBackgroundDrawable(new ColorDrawable(aPrefs.actionbarColor));
	        mTitleView.setTextColor(aPrefs.actionbarFontColor);
	        setPreferredFont(mTitleView, Typeface.NORMAL);
        }
    }

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i(TAG, "Service Connected!");
        mService = new Messenger(service);
        for(Message msg : mMessageQueue){
        	try {
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
        }
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
        Log.i(TAG, "Service Disconnected!");
		mService = null;
	}
	public void sendMessage(Messenger callback, int messageType, int id, int arg1){
		sendMessage(callback, messageType, id, arg1, null);
	}
	public void sendMessage(Messenger callback, int messageType, int id, int arg1, Object obj){
		try {
            Message msg = Message.obtain(null, messageType, id, arg1);
            msg.replyTo = callback;
            msg.obj = obj;
    		if(mService != null){
    			mService.send(msg);
    		}else{
    			mMessageQueue.add(msg);
    		}
        } catch (RemoteException e) {
            e.printStackTrace();
        }
	}

    public void displayUserCP() {
    	displayForum(Constants.USERCP_ID, 1);
    }
    
    public void displayThread(int id, int page, int forumId, int forumPage){
    	startActivity(new Intent().setClass(this, ThreadDisplayActivity.class)
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
    
    public void displayQuickBrowser(String url){
        AwfulWebFragment.newInstance(url).show(getSupportFragmentManager().beginTransaction(), "awful_web_dialog");
    }
    
	public void displayReplyWindow(int threadId, int postId, int type) {
    	Bundle args = new Bundle();
        args.putInt(Constants.THREAD_ID, threadId);
        args.putInt(Constants.EDITING, type);
        args.putInt(Constants.POST_ID, postId);
    	startActivityForResult(new Intent(this, PostReplyActivity.class).putExtras(args), PostReplyFragment.RESULT_POSTED);
	}
    
    public void setActionbarTitle(String aTitle, Object requestor) {
        ActionBar action = getSupportActionBar();
        if(action != null){
        	mTitleView = (TextView) action.getCustomView();
        }
    	if(aTitle != null && mTitleView != null && aTitle.length()>0){
    		mTitleView.setText(Html.fromHtml(aTitle));
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
	
	//UNUSED - I don't know why I put them in the same interface. Oh well.
	@Override
	public void loadingFailed() {}
	@Override
	public void loadingStarted() {}
	@Override
	public void loadingSucceeded() {}

}
