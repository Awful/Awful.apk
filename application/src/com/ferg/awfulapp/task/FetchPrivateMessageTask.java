package com.ferg.awfulapp.task;

import java.util.HashMap;

import org.htmlcleaner.TagNode;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.os.Message;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulMessage;

public class FetchPrivateMessageTask extends AwfulTask {

	public FetchPrivateMessageTask(AwfulSyncService sync, Message aMsg,	AwfulPreferences aPrefs) {
		super(sync, aMsg, aPrefs, AwfulSyncService.MSG_FETCH_PM);
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
			ContentValues reply = AwfulMessage.processReplyMessage(pmReplyData, mId);
			reply.put(AwfulMessage.RECIPIENT,message.getAsString(AwfulMessage.AUTHOR));
			//we remove the reply content so as not to override the existing reply.
			String replyContent = reply.getAsString(AwfulMessage.REPLY_CONTENT);
			reply.remove(AwfulMessage.REPLY_CONTENT);
			String replyTitle = reply.getAsString(AwfulMessage.TITLE);
			reply.remove(AwfulMessage.TITLE);
			if(cr.update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mId), reply, null, null)<1){
				//but if the reply doesn't already exist, insert it.
				reply.put(AwfulMessage.REPLY_CONTENT, replyContent);
				reply.put(AwfulMessage.TITLE, replyTitle);
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
