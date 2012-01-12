package com.ferg.awful.task;

import java.util.HashMap;

import android.util.Log;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.service.AwfulSyncService;

public class BookmarkTask extends AwfulTask {
	/**
	 * 
	 * @param sync Service.
	 * @param id Thread ID to bookmark/unbookmark.
	 * @param arg1 Set to 1 to bookmark, 0 to remove bookmark.
	 * @param aPrefs
	 */
	public BookmarkTask(AwfulSyncService sync, int id, int arg1) {
		super(sync, id, arg1, null);
	}

	@Override
	protected Boolean doInBackground(Void... p) {
		if (!isCancelled()) {
        	HashMap<String, String> params = new HashMap<String, String>();
            params.put(Constants.PARAM_THREAD_ID, Integer.toString(mId));
            if(mArg1 > 0){
            	params.put(Constants.PARAM_ACTION, "remove");
            }else{
            	params.put(Constants.PARAM_ACTION, "add");
            }

            try {
                NetworkUtils.post(Constants.FUNCTION_BOOKMARK, params);
            } catch (Exception e) {
                Log.i(TAG, e.toString());
                return false;
            }
        }
        return true;
	}

}
