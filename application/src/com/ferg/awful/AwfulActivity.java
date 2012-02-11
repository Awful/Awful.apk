package com.ferg.awful;

import java.util.LinkedList;

import com.ferg.awful.service.AwfulSyncService;

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.Html;
import android.view.*;

import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.example.google.tv.leftnavbar.LeftNavBar;
import com.example.google.tv.leftnavbar.LeftNavBarService;

/**
 * Convenience class to avoid having to call a configurator's lifecycle methods everywhere. This
 * class should avoid implementing things directly; the ActivityConfigurator does that job.
 * 
 * Most Activities in this awful app should extend this guy; that will provide things like locking
 * orientation according to user preference.
 * 
 * This class also provides a few helper methods for grabbing preferences and the like.
 */
public class AwfulActivity extends FragmentActivity implements ServiceConnection {
    private static final String TAG = "AwfulActivity";
	private ActivityConfigurator mConf;
    private Messenger mService = null;
    private LinkedList<Message> mMessageQueue = new LinkedList<Message>();

    private LeftNavBar mLeftNavBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConf = new ActivityConfigurator(this);
        mConf.onCreate();
        bindService(new Intent(this, AwfulSyncService.class), this, BIND_AUTO_CREATE);
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
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mConf.onPause();
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
    }

    public boolean isTablet() {
        Configuration config = getResources().getConfiguration();
        PackageManager manager = getPackageManager();

        if (manager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                // If it's a Honeycomb device, it has to be a tablet
                return true;
            } else if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) && config.smallestScreenWidthDp >= 600) {
                // If it's 3.2+ and the smallest screen width is at least a 7" device, it's a tablet
                return true;
            }
        }

        return false;
    }

    // Differs from isTablet() since sometimes we want to use large screen features for a TV
    public boolean isLargeScreen() {
        Configuration config = getResources().getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // If it's a Honeycomb device, it has to be a tablet
            return true;
        } else if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) && config.smallestScreenWidthDp >= 600) {
            // If it's 3.2+ and the smallest screen width is at least a 7" device, it's a tablet
            return true;
        }

        return false;
    }

    public boolean isTV() {
        return !getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
            && !useLegacyActionbar();
    }

    public static boolean useLegacyActionbar() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;
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
	public void registerSyncService(Messenger aMessenger, int aClientId){
		try {
            Message msg = Message.obtain(null, AwfulSyncService.MSG_REGISTER_CLIENT, aClientId,0);
            msg.replyTo = aMessenger;
            if(mService != null){
    			mService.send(msg);
    		}else{
    			mMessageQueue.add(msg);
    		}
        } catch (RemoteException e) {
            e.printStackTrace();
        }
	}

	public void unregisterSyncService(Messenger aMessenger, int aClientId){
		if(mService != null){
			try {
	            Message msg = Message.obtain(null, AwfulSyncService.MSG_UNREGISTER_CLIENT, aClientId,0);
	            msg.replyTo = aMessenger;
	            mService.send(msg);
	        } catch (RemoteException e) {
	            e.printStackTrace();
	        }
		}
	}
	public void sendMessage(int messageType, int id, int arg1){
		try {
            Message msg = Message.obtain(null, messageType, id, arg1);
    		if(mService != null){
    			mService.send(msg);
    		}else{
    			mMessageQueue.add(msg);
    		}
    			
        } catch (RemoteException e) {
            e.printStackTrace();
        }
	}
	public void sendMessage(int messageType, int id, int arg1, Object obj){
		try {
            Message msg = Message.obtain(null, messageType, id, arg1);
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
    
    public LeftNavBar getLeftNavBar() {
        if (mLeftNavBar == null) {
            mLeftNavBar = new LeftNavBar(this);
            mLeftNavBar.setOnClickHomeListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // This is called when the app icon is selected in the left navigation bar
                    // Doing nothing.
                }
            });
        }
        
        return mLeftNavBar;
    }

    public void setupLeftNavBar(int aActionItems) {
        setupLeftNavBar(aActionItems, false);
    }

    public void setupLeftNavBar(int aActionItems, boolean aShowTitleBar) {
        LeftNavBar bar = (LeftNavBarService.instance()).getLeftNavBar(this);
        bar.setBackgroundDrawable(getResources().getDrawable(com.example.google.tv.leftnavbar.R.drawable.bar_tv));

        bar = getLeftNavBar();

        bar.setCustomView(aActionItems);
        bar.setTitle(R.string.app_name);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setTitleBackground(getResources().getDrawable(R.drawable.bar));
        bar.setShowHideAnimationEnabled(true);

        if (aShowTitleBar) {
            bar.setDisplayOptions(
                LeftNavBar.DISPLAY_AUTO_EXPAND|
                ActionBar.DISPLAY_SHOW_HOME|
                LeftNavBar.DISPLAY_USE_LOGO_WHEN_EXPANDED|
                ActionBar.DISPLAY_SHOW_CUSTOM|
                ActionBar.DISPLAY_SHOW_TITLE
            );
        } else {
            bar.setDisplayOptions(
                LeftNavBar.DISPLAY_AUTO_EXPAND|
                ActionBar.DISPLAY_SHOW_HOME|
                LeftNavBar.DISPLAY_USE_LOGO_WHEN_EXPANDED|
                ActionBar.DISPLAY_SHOW_CUSTOM
            );
        }

        ViewGroup optionsContainer = (ViewGroup) bar.getCustomView();

        for (int i = 0; i < optionsContainer.getChildCount(); i++) {
            optionsContainer.getChildAt(i).setOnClickListener(onActionItemClick);
        }
    }

    private View.OnClickListener onActionItemClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.user_cp:
                    displayUserCP();
                    break;
                case R.id.pm:
                    startActivity(new Intent(AwfulActivity.this, PrivateMessageActivity.class));
                    break;
                case R.id.reply:
                    displayPostReplyDialog();
                    break;
            }
        }
    };

    public void displayUserCP() {
        UserCPFragment.newInstance(true).show(getSupportFragmentManager(), "user_control_panel_dialog");
    }

    public void displayPostReplyDialog() {
        // TODO: doing this because I don't even
    }

    @Override
    public ActionBar getActionBar() {
        if (isTV()) {
            return getLeftNavBar();
        }

        return super.getActionBar();
    }
    
    public void setActionbarTitle(String aTitle) {
        ActionBar action = getActionBar();

        action.setTitle(Html.fromHtml(aTitle).toString());
    }
}
