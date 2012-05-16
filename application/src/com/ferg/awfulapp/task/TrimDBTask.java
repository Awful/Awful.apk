package com.ferg.awfulapp.task;

import android.content.ContentResolver;
import android.os.Message;
import android.util.Log;

import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;

public class TrimDBTask extends AwfulTask {
	private static final int TABLE_THREAD = 1;
	private static final int TABLE_POST = 2;
	private static final int TABLE_UCP = 3;
	public TrimDBTask(AwfulSyncService sync, Message aMsg) {
		super(sync, aMsg, null, AwfulSyncService.MSG_TRIM_DB);
		if(mArg1 <1){
			mArg1 = 7;
		}
	}

	@Override
	protected Boolean doInBackground(Void... params) {
    	ContentResolver dbInterface = mContext.getContentResolver();
    	int rowCount = 0;
    	switch(mId){
    	case TABLE_THREAD:
    		rowCount += dbInterface.delete(AwfulThread.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+mArg1+" days')", null);
    		break;
    	case TABLE_POST:
    		rowCount += dbInterface.delete(AwfulPost.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+mArg1+" days')", null);
    		break;
    	case TABLE_UCP:
    		rowCount += dbInterface.delete(AwfulThread.CONTENT_URI_UCP, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+mArg1+" days')", null);
    		break;
		default:
			rowCount += dbInterface.delete(AwfulThread.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+mArg1+" days')", null);
			rowCount += dbInterface.delete(AwfulPost.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+mArg1+" days')", null);
			rowCount += dbInterface.delete(AwfulThread.CONTENT_URI_UCP, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+mArg1+" days')", null);
    	}
    	Log.i(TAG,"Trimming DB older than "+mArg1+" days, culled: "+rowCount);
		return true;
	}

}
