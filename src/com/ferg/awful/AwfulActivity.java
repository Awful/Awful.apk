package com.ferg.awful;

import com.ferg.awful.service.AwfulSyncService;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import android.support.v4.app.FragmentActivity;

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
    private ActivityConfigurator mConf;
    private Messenger mService = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConf = new ActivityConfigurator(this);
        mConf.onCreate();
        bindService(new Intent(this, AwfulSyncService.class), mServiceConnection, BIND_AUTO_CREATE);
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
        unbindService(mServiceConnection);
    }

    public boolean isTablet() {
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

    public static boolean useLegacyActionbar() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;
    }
    
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName aName, IBinder aBinder) {

            
        }

        public void onServiceDisconnected(ComponentName aName) {
            mService = null;
        }
    };

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
        mService = new Messenger(service);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mService = null;
	}
	public boolean registerSyncService(Messenger aMessenger, int aClientId){
		if(mService == null){
			return false;
		}
		try {
            Message msg = Message.obtain(null, AwfulSyncService.MSG_REGISTER_CLIENT, aClientId,0);
            msg.replyTo = aMessenger;
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
		return true;
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
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
	}
}
