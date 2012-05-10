package com.ferg.awfulapp.task;

import android.content.ContentResolver;
import android.util.Log;

import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;

public class TrimDBTask extends AwfulTask {
	private static final int TABLE_THREAD = 1;
	private static final int TABLE_POST = 2;
	private static final int TABLE_UCP = 3;
	private int table, trim_age;
	public TrimDBTask(AwfulSyncService sync, int table_code, int age) {
		super(sync, -1, 0, null, AwfulSyncService.MSG_TRIM_DB);
		if(age <1){
			trim_age = 7;
		}else{
			trim_age = age;
		}
		table = table_code;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
    	ContentResolver dbInterface = mContext.getContentResolver();
    	int rowCount = 0;
    	switch(table){
    	case TABLE_THREAD:
    		rowCount += dbInterface.delete(AwfulThread.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+trim_age+" days')", null);
    		break;
    	case TABLE_POST:
    		rowCount += dbInterface.delete(AwfulPost.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+trim_age+" days')", null);
    		break;
    	case TABLE_UCP:
    		rowCount += dbInterface.delete(AwfulThread.CONTENT_URI_UCP, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+trim_age+" days')", null);
    		break;
		default:
			rowCount += dbInterface.delete(AwfulThread.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+trim_age+" days')", null);
			rowCount += dbInterface.delete(AwfulPost.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+trim_age+" days')", null);
			rowCount += dbInterface.delete(AwfulThread.CONTENT_URI_UCP, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+trim_age+" days')", null);
    	}
    	Log.i(TAG,"Trimming DB older than "+trim_age+" days, culled: "+rowCount);
		return true;
	}

}
