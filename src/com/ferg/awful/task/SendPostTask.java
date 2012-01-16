package com.ferg.awful.task;

import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.service.AwfulSyncService;

public class SendPostTask extends AwfulTask {

	public SendPostTask(AwfulSyncService sync, int id, int arg1) {
		super(sync, id, arg1, null, AwfulSyncService.MSG_SEND_POST);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		
		return false;
	}

}
