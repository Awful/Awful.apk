package com.ferg.awful.task;

import java.util.HashMap;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.provider.AwfulProvider;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulPost;
import com.ferg.awful.thread.AwfulThread;

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
                
                //set unread posts (> unreadIndex)
                ContentValues last_read = new ContentValues();
                last_read.put(AwfulPost.PREVIOUSLY_READ, 0);
                ContentResolver resolv = mContext.getContentResolver();
                resolv.update(AwfulPost.CONTENT_URI, 
							                		last_read, 
							                		AwfulPost.THREAD_ID+"=? AND "+AwfulPost.POST_INDEX+">?", 
							                		AwfulProvider.int2StrArray(mId,mArg1));
                
                //set previously read posts (< unreadIndex)
                last_read.put(AwfulPost.PREVIOUSLY_READ, 1);
                resolv.update(AwfulPost.CONTENT_URI, 
							                		last_read, 
							                		AwfulPost.THREAD_ID+"=? AND "+AwfulPost.POST_INDEX+"<=?", 
							                		AwfulProvider.int2StrArray(mId,mArg1));
                
                //update unread count
                Cursor threadData = resolv.query(ContentUris.withAppendedId(AwfulThread.CONTENT_URI, mId), AwfulProvider.ThreadProjection, null, null, null);
                if(threadData.getCount()>0){
	                ContentValues thread_update = new ContentValues();
	                thread_update.put(AwfulThread.UNREADCOUNT, threadData.getInt(threadData.getColumnIndex(AwfulThread.POSTCOUNT)) - mArg1);
	                mContext.getContentResolver().update(AwfulPost.CONTENT_URI, 
	                									thread_update, 
								                		AwfulPost.THREAD_ID+"=?", 
								                		AwfulProvider.int2StrArray(mId));
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
	}

}
