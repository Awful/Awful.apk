package com.ferg.awful.task;

import android.util.Log;

import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulThread;

public class ThreadTask extends AwfulTask {
	public static final String TAG = "ThreadTask";

	public ThreadTask(AwfulSyncService sync, int id, int arg1, AwfulPreferences aPrefs) {
		super(sync, id, arg1, aPrefs, AwfulSyncService.MSG_SYNC_THREAD);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
            AwfulThread.getThreadPosts(mContext, mId, mArg1, mPrefs.postPerPage, mPrefs);
            Log.i(TAG, "Sync complete");
        } catch (Exception e) {
            Log.i(TAG, "Sync error");
            e.printStackTrace();
            return false;
        }
		return true;
	}

}
