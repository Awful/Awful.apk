package com.ferg.awful.task;

import java.util.HashMap;

import org.htmlcleaner.TagNode;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.util.Log;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.provider.AwfulProvider;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulMessage;

public class SendPrivateMessageTask extends AwfulTask {
	public SendPrivateMessageTask(AwfulSyncService sync, int id, int type) {
		super(sync, id, 0, null, AwfulSyncService.MSG_SEND_PM);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			ContentResolver cr = mContext.getContentResolver();
			Cursor pmInfo = cr.query(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mId), AwfulProvider.DraftProjection, null, null, null);
			if(pmInfo.getCount() >0 && pmInfo.moveToFirst()){
				HashMap<String, String> para = new HashMap<String, String>();
	            para.put(Constants.PARAM_PRIVATE_MESSAGE_ID, Integer.toString(mId));
	            para.put(Constants.PARAM_ACTION, Constants.ACTION_DOSEND);
	            para.put(Constants.DESTINATION_TOUSER, pmInfo.getString(pmInfo.getColumnIndex(AwfulMessage.RECIPIENT)));
	            para.put(Constants.PARAM_TITLE, pmInfo.getString(pmInfo.getColumnIndex(AwfulMessage.TITLE)));
	            //TODO move to constants
	            if(mId>0){
	            	para.put("prevmessageid", Integer.toString(mId));
	            }
	            para.put(Constants.PARAM_PARSEURL, Constants.YES);
	            para.put("savecopy", "yes");
	            para.put("iconid", "0");
	            para.put(Constants.PARAM_MESSAGE, pmInfo.getString(pmInfo.getColumnIndex(AwfulMessage.REPLY_CONTENT)));
				TagNode result = NetworkUtils.post(Constants.FUNCTION_PRIVATE_MESSAGE, para);
			}else{
				Log.e(TAG,"PM Send Failure: PM missing from DB "+mId);
				return false;
			}
			
		} catch (Exception e) {
			Log.e(TAG,"PM Send Failure: "+Log.getStackTraceString(e));
			return false;
		}
		return true;
	}

}
