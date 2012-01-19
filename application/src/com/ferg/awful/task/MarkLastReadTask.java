package com.ferg.awful.task;

import java.util.HashMap;

import android.content.ContentValues;
import android.util.Log;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulPost;

public class MarkLastReadTask extends AwfulTask {

	public MarkLastReadTask(AwfulSyncService sync, int id, int index) {
		super(sync, id, index, null, AwfulSyncService.MSG_MARK_LASTREAD);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (!isCancelled()) {
            try {
            	HashMap<String, String> param = new HashMap<String, String>();
                param.put(Constants.PARAM_ACTION, "setseen");
                param.put(Constants.PARAM_THREAD_ID, Integer.toString(mId));
                param.put(Constants.PARAM_INDEX, Integer.toString(mArg1));
                NetworkUtils.get(Constants.FUNCTION_THREAD, param);
                ContentValues last_read = new ContentValues();
                last_read.put(AwfulPost.PREVIOUSLY_READ, 0);
                mContext.getContentResolver().update(AwfulPost.CONTENT_URI, 
							                		last_read, 
							                		AwfulPost.THREAD_ID+"=? AND "+AwfulPost.POST_INDEX+">?", 
							                		new String[]{Integer.toString(mId),Integer.toString(mArg1)});//I love SQL
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
	}

}
