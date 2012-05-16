package com.ferg.awfulapp.task;

import java.util.HashMap;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Message;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulThread;

public class BookmarkTask extends AwfulTask {
	/**
	 * 
	 * @param sync Service.
	 * @param id Thread ID to bookmark/unbookmark.
	 * @param arg1 Set to 1 to bookmark, 0 to remove bookmark.
	 * @param aPrefs
	 */
	public BookmarkTask(AwfulSyncService sync, Message aMsg) {
		super(sync, aMsg, null, AwfulSyncService.MSG_SET_BOOKMARK);
	}

	@Override
	protected Boolean doInBackground(Void... p) {
		if (!isCancelled()) {
        	HashMap<String, String> params = new HashMap<String, String>();
            params.put(Constants.PARAM_THREAD_ID, Integer.toString(mId));
            ContentResolver cr = mContext.getContentResolver();
            if(mArg1 < 1){
            	params.put(Constants.PARAM_ACTION, "remove");
            	cr.delete(AwfulThread.CONTENT_URI_UCP, AwfulThread.ID+"=?", AwfulProvider.int2StrArray(mId));
            }else{
            	params.put(Constants.PARAM_ACTION, "add");
            }

            try {
                NetworkUtils.postIgnoreBody(Constants.FUNCTION_BOOKMARK, params);
                ContentValues cv = new ContentValues();
                cv.put(AwfulThread.BOOKMARKED, mArg1);
                cr.update(AwfulThread.CONTENT_URI, cv, AwfulThread.ID+"=?", AwfulProvider.int2StrArray(mId));
            } catch (Exception e) {
                Log.i(TAG, e.toString());
                return false;
            }
        }
        return true;
	}

}
