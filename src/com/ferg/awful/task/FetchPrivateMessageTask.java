package com.ferg.awful.task;

import java.util.HashMap;

import org.htmlcleaner.TagNode;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.util.Log;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulMessage;

public class FetchPrivateMessageTask extends AwfulTask {

	public FetchPrivateMessageTask(AwfulSyncService sync, int id, int arg1,
			AwfulPreferences aPrefs) {
		super(sync, id, arg1, aPrefs, AwfulSyncService.MSG_FETCH_PM);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			HashMap<String, String> para = new HashMap<String, String>();
            para.put(Constants.PARAM_PRIVATE_MESSAGE_ID, Integer.toString(mId));
            para.put(Constants.PARAM_ACTION, "show");
			TagNode pmData = NetworkUtils.get(Constants.FUNCTION_PRIVATE_MESSAGE, para);
			ContentResolver cr = mContext.getContentResolver();
			ContentValues message = AwfulMessage.processMessage(pmData, mId);
			if(cr.update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI, mId), message, null, null)<1){
				cr.insert(AwfulMessage.CONTENT_URI, message);
			}
            para.put(Constants.PARAM_ACTION, "newmessage");
			TagNode pmReplyData = NetworkUtils.get(Constants.FUNCTION_PRIVATE_MESSAGE, para);
			ContentValues reply = AwfulMessage.processReplyMessage(pmReplyData, mId, message.getAsString(AwfulMessage.AUTHOR));
			if(cr.update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mId), reply, null, null)<1){
				cr.insert(AwfulMessage.CONTENT_URI_REPLY, reply);
			}
			Log.v(TAG,"Fetched msg: "+mId);
		} catch (Exception e) {
			Log.e(TAG,"PM Load Failure: "+Log.getStackTraceString(e));
			return false;
		}
		return true;
	}

}
