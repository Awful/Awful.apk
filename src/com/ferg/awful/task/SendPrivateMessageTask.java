package com.ferg.awful.task;

import java.util.HashMap;

import org.htmlcleaner.TagNode;

import android.util.Log;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.service.AwfulSyncService;

public class SendPrivateMessageTask extends AwfulTask {
	public SendPrivateMessageTask(AwfulSyncService sync, int id, int type) {
		super(sync, id, 0, null, AwfulSyncService.MSG_SEND_PM);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			HashMap<String, String> para = new HashMap<String, String>();
            para.put(Constants.PARAM_PRIVATE_MESSAGE_ID, Integer.toString(mId));
            para.put(Constants.PARAM_ACTION, Constants.ACTION_DOSEND);
            //para.put(Constants.DESTINATION_TOUSER, mRecipient);
            //para.put(Constants.PARAM_TITLE, mTitle);
            //TODO move to constants
            if(mId>0){
            	para.put("prevmessageid", Integer.toString(mId));
            }
            para.put(Constants.PARAM_PARSEURL, Constants.YES);
            para.put("savecopy", "yes");
            para.put("iconid", "0");
            //para.put(Constants.PARAM_MESSAGE, mContent);
			TagNode result = NetworkUtils.post(Constants.FUNCTION_PRIVATE_MESSAGE, para);
		} catch (Exception e) {
			Log.e(TAG,"PM Send Failure: "+Log.getStackTraceString(e));
			return false;
		}
		return true;
	}

}
