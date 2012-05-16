package com.ferg.awfulapp.task;

import java.util.HashMap;

import android.content.ContentUris;
import android.content.ContentValues;
import android.os.Message;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;

public class MarkUnreadTask extends AwfulTask {

	public MarkUnreadTask(AwfulSyncService sync, Message aMsg) {
		super(sync, aMsg, null, AwfulSyncService.MSG_MARK_UNREAD);
	}

	@Override
	protected Boolean doInBackground(Void... p) {
		if (!isCancelled()) {
        	HashMap<String, String> params = new HashMap<String, String>();
            params.put(Constants.PARAM_THREAD_ID, Integer.toString(mId));
            params.put(Constants.PARAM_ACTION, "resetseen");

            try {
                NetworkUtils.postIgnoreBody(Constants.FUNCTION_THREAD, params);
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
