package com.ferg.awfulapp.task;

import java.util.ArrayList;
import java.util.HashMap;

import org.jsoup.nodes.Document;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Message;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulEmote;

public class FetchEmotesTask extends AwfulTask {

	public FetchEmotesTask(AwfulSyncService sync, Message msg, AwfulPreferences aPrefs) {
		super(sync, msg, aPrefs, AwfulSyncService.MSG_FETCH_EMOTES);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			HashMap<String, String> para = new HashMap<String, String>();
			para.put(Constants.PARAM_ACTION, "showsmilies");
			Document data = NetworkUtils.getJSoup(Constants.FUNCTION_MISC, para, replyTo, 50);
			ArrayList<ContentValues> emotes = AwfulEmote.parseEmotes(data);
			int resultCount = mContext.getContentResolver().bulkInsert(AwfulEmote.CONTENT_URI, emotes.toArray(new ContentValues[emotes.size()]));
	        Log.i(TAG, "Inserted "+resultCount+" emotes into DB. "+emotes.size());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
