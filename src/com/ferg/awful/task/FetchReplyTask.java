package com.ferg.awful.task;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.ferg.awful.provider.AwfulProvider;
import com.ferg.awful.reply.Reply;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulMessage;
import com.ferg.awful.thread.AwfulPost;

public class FetchReplyTask extends AwfulTask {
	
	private int type;

	public FetchReplyTask(AwfulSyncService sync, int threadId, int postId, int replyType) {
		super(sync, threadId, postId, null, AwfulSyncService.MSG_FETCH_POST_REPLY);
		type = replyType;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try{
			ContentResolver contentResolver = mContext.getContentResolver();
			ContentValues reply;
			/*Cursor replyData = contentResolver.query(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mId), AwfulProvider.DraftPostProjection, null, null, null);
			if(replyData.getCount()>0&&replyData.moveToFirst()){
				replyType = replyData.getInt(replyData.getColumnIndex(AwfulMessage.TYPE));
			}else{
				Log.e(TAG,"REPLY TYPE MISSING");
				return false;
			}*/
			switch(type){
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
			if(contentResolver.update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mId), reply, null, null)<1){
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
