package com.ferg.awful.task;

import java.util.HashMap;

import android.content.ContentUris;
import android.content.ContentValues;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulPost;
import com.ferg.awful.thread.AwfulThread;

public class MarkUnreadTask extends AwfulTask {

	public MarkUnreadTask(AwfulSyncService sync, int id, int arg1) {
		super(sync, id, arg1, null);
	}

	@Override
	protected Boolean doInBackground(Void... p) {
		if (!isCancelled()) {
        	HashMap<String, String> params = new HashMap<String, String>();
            params.put(Constants.PARAM_THREAD_ID, Integer.toString(mId));
            params.put(Constants.PARAM_ACTION, "resetseen");

            try {
                NetworkUtils.post(Constants.FUNCTION_THREAD, params);
                ContentValues last_read = new ContentValues();
                last_read.put(AwfulPost.PREVIOUSLY_READ, 0);
                mContext.getContentResolver().update(AwfulPost.CONTENT_URI, last_read, AwfulPost.THREAD_ID+"=?", new String[]{Integer.toString(mId)});
                ContentValues unread = new ContentValues();
                unread.put(AwfulThread.UNREADCOUNT, -1);
                mContext.getContentResolver().update(ContentUris.withAppendedId(AwfulThread.CONTENT_URI, mId), unread, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            	return false;
            }
        }
		return true;
	}

}
