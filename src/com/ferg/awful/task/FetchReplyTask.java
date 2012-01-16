package com.ferg.awful.task;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.util.Log;

import com.ferg.awful.reply.Reply;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulMessage;

public class FetchReplyTask extends AwfulTask {
	
	private int replyType;

	public FetchReplyTask(AwfulSyncService sync, int threadId, int postId) {
		super(sync, threadId, postId, null, AwfulSyncService.MSG_FETCH_POST_REPLY);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try{
			ContentResolver contentResolver = mContext.getContentResolver();
			ContentValues reply;
			switch(replyType){
				case AwfulMessage.TYPE_QUOTE:
					reply = Reply.fetchQuote(mId, mArg1);
					break;
				case AwfulMessage.TYPE_NEW_REPLY:
					reply = Reply.fetchPost(mId);
					break;
				case AwfulMessage.TYPE_EDIT:
					reply = Reply.fetchEdit(mId, mArg1);
					break;
				default:
					return false;
			}
			String content = reply.getAsString(AwfulMessage.REPLY_CONTENT);
			reply.remove(AwfulMessage.REPLY_CONTENT);
			if(contentResolver.update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mId), reply, null, null)<1){
				reply.put(AwfulMessage.REPLY_CONTENT, content);
				contentResolver.insert(AwfulMessage.CONTENT_URI_REPLY, reply);
			}
			Log.i(TAG, "Reply loaded and saved: "+mId);
		}catch(Exception e){
			Log.e(TAG, "Reply Load Failure: "+mId+" - "+e.getMessage());
			return false;
		}
		return true;
	}

}
