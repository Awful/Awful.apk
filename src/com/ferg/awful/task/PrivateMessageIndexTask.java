package com.ferg.awful.task;

import org.htmlcleaner.TagNode;

import android.util.Log;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulMessage;

public class PrivateMessageIndexTask extends AwfulTask {

	public PrivateMessageIndexTask(AwfulSyncService sync, int id, int page) {
		super(sync, id, page, null);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			//TODO this doesn't actually save data into the provider yet.
			TagNode pmData = NetworkUtils.get(Constants.FUNCTION_PRIVATE_MESSAGE);
			AwfulMessage.processMessageList(pmData);
		} catch (Exception e) {
			Log.e(TAG,"PM Load Failure: "+Log.getStackTraceString(e));
			return false;
		}
		return true;
	}

}
