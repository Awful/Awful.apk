package com.ferg.awfulapp.task;

import org.jsoup.nodes.Document;

import android.os.Message;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.thread.AwfulPagedItem;
import com.ferg.awfulapp.thread.AwfulThread;

public class ForumTask extends AwfulTask {
	private final static String TAG = "ForumTask";
	

	public ForumTask(AwfulSyncService sync, Message msg, AwfulPreferences aPrefs) {
		super(sync, msg, aPrefs, AwfulSyncService.MSG_SYNC_FORUM);
	}

	@Override
	protected String doInBackground(Void... params) {
		try {
			Document threads = null;
            replyTo.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, mId, 25));
            if(mId == Constants.USERCP_ID){
            	threads = AwfulThread.getUserCPThreads(mArg1, replyTo);
                replyTo.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, mId, 75));
                String error = AwfulPagedItem.checkPageErrors(threads, replyTo);
                if(error != null){
                	return error;
                }
                AwfulForum.parseUCPThreads(threads, mArg1, mContext.getContentResolver());
            }else{
            	threads = AwfulThread.getForumThreads(mId, mArg1, replyTo);
                replyTo.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, mId, 75));
                String error = AwfulPagedItem.checkPageErrors(threads, replyTo);
                if(error != null){
                	Log.e(TAG,"Parsing Failed: "+error);
                	return error;
                }
        		String innerText = threads.getElementsByClass("inner").text();
        		if(innerText.contains("Specified forum was not found in the live forums.") || innerText.contains("You do not have permission to access this page.")){
                	Log.e(TAG,"Parsing Failed: Forum "+mId+" not found, deleting entry.");
        			mContext.getContentResolver().delete(AwfulForum.CONTENT_URI, AwfulForum.ID+"=?", AwfulProvider.int2StrArray(mId));
        			return "Error - Forum does not exist or you do not have permission to view it.";
        		}
                AwfulForum.parseThreads(threads, mId, mArg1, mContext.getContentResolver());
            }
            replyTo.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, mId, 100));
        } catch (Exception e) {
            Log.e(TAG, "Sync error");
            e.printStackTrace();
            return "Failed to load Forum!";
        }
		return null;
	}
}
