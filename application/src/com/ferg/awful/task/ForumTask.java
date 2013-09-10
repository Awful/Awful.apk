package com.ferg.awful.task;

import org.apache.http.conn.ConnectTimeoutException;
import org.jsoup.nodes.Document;

import android.os.Message;
import android.util.Log;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.provider.AwfulProvider;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulPagedItem;
import com.ferg.awful.thread.AwfulThread;

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
                String error = AwfulPagedItem.checkPageErrors(threads, replyTo, mPrefs);
                if(error != null){
                	return error;
                }
                AwfulForum.parseUCPThreads(threads, mArg1, mContext.getContentResolver());
            }else{
            	threads = AwfulThread.getForumThreads(mId, mArg1, replyTo);
                replyTo.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, mId, 75));
                String error = AwfulPagedItem.checkPageErrors(threads, replyTo, mPrefs);
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
        } catch (ConnectTimeoutException e) {
            Log.e(TAG, "Network timeout");
            return "Network timeout! Check your internet connection.";
        } catch (Exception e) {
            Log.e(TAG, "Sync error");
            e.printStackTrace();
            return "Failed to load forum!";
        }
		return null;
	}
}
