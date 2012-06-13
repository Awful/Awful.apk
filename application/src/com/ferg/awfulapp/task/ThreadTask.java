package com.ferg.awfulapp.task;

import android.os.Message;
import android.util.Log;

import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulThread;

public class ThreadTask extends AwfulTask {
	public static final String TAG = "ThreadTask";

	private int mUserId = 0;
	
	public ThreadTask(AwfulSyncService sync, Message aMsg, AwfulPreferences aPrefs) {
		super(sync, aMsg, aPrefs, AwfulSyncService.MSG_SYNC_THREAD);
		mUserId = (Integer) aMsg.obj;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
            AwfulThread.getThreadPosts(mContext, mId, mArg1, mPrefs.postPerPage, mPrefs, mUserId, replyTo);
            Log.i(TAG, "Sync complete");
        } catch (Exception e) {
            Log.i(TAG, "Sync error");
            e.printStackTrace();
            return false;
        }
		mContext.queueDelayedMessage(AwfulSyncService.MSG_TRIM_DB, 1000, 0, 7);
		return true;
	}

}
