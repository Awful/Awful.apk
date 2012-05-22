package com.ferg.awfulapp.task;

import android.os.Message;

import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.service.AwfulSyncService;

public class RedirectTask extends AwfulTask {
	private String mUrl; 
	public RedirectTask(AwfulSyncService sync, Message msg) {
		super(sync, msg, null, AwfulSyncService.MSG_TRANSLATE_REDIRECT);
		mUrl = (String) msg.obj;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			setReplyObject(NetworkUtils.getRedirect(mUrl, null));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
