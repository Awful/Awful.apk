package com.ferg.awful.task;

import java.util.HashMap;

import org.htmlcleaner.TagNode;

import android.util.Log;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulMessage;

public class FetchPrivateMessageTask extends AwfulTask {

	public FetchPrivateMessageTask(AwfulSyncService sync, int id, int arg1,
			AwfulPreferences aPrefs) {
		super(sync, id, arg1, aPrefs);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			//TODO none of this is updated yet
			HashMap<String, String> para = new HashMap<String, String>();
            para.put(Constants.PARAM_PRIVATE_MESSAGE_ID, Integer.toString(mId));
            para.put(Constants.PARAM_ACTION, "show");
			TagNode pmData = NetworkUtils.get(Constants.FUNCTION_PRIVATE_MESSAGE, para);
			//AwfulMessage.processMessage(pmData, pm);
			//finished loading display message, notify UI
			publishProgress((Void[]) null);
			//after notifying, we can preload reply window text
            para.put(Constants.PARAM_ACTION, "newmessage");
			TagNode pmReplyData = NetworkUtils.get(Constants.FUNCTION_PRIVATE_MESSAGE, para);
			//AwfulMessage.processReplyMessage(pmReplyData, pm);
			//pm.setLoaded(true);
			Log.v(TAG,"Fetched msg: "+mId);
		} catch (Exception e) {
			Log.e(TAG,"PM Load Failure: "+Log.getStackTraceString(e));
			return false;
		}
		return true;
	}

}
