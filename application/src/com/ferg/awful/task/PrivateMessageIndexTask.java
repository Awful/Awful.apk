package com.ferg.awful.task;

import org.htmlcleaner.TagNode;

import android.util.Log;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulMessage;

public class PrivateMessageIndexTask extends AwfulTask {

	public PrivateMessageIndexTask(AwfulSyncService sync, int id, int page) {
		super(sync, id, page, null, AwfulSyncService.MSG_FETCH_PM_INDEX);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			TagNode pmData = NetworkUtils.get(Constants.FUNCTION_PRIVATE_MESSAGE);
			AwfulMessage.processMessageList(mContext.getContentResolver(), pmData);
		} catch (Exception e) {
			Log.e(TAG,"PM Load Failure: "+Log.getStackTraceString(e));
			return false;
		}
		return true;
	}

}
