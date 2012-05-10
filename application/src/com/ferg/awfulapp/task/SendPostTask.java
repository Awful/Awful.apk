package com.ferg.awfulapp.task;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.util.Log;

import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.reply.Reply;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPost;

public class SendPostTask extends AwfulTask {
	public SendPostTask(AwfulSyncService sync, int id, int arg1, int replyType) {
		super(sync, id, arg1, null, AwfulSyncService.MSG_SEND_POST);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try{
			ContentResolver cr = mContext.getContentResolver();
			Cursor postInfo = cr.query(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mId), AwfulProvider.DraftPostProjection, null, null, null);
			if(postInfo.getCount() >0 && postInfo.moveToFirst()){
				String message = NetworkUtils.encodeHtml(postInfo.getString(postInfo.getColumnIndex(AwfulMessage.REPLY_CONTENT)));
				String formCookie = postInfo.getString(postInfo.getColumnIndex(AwfulPost.FORM_COOKIE));
				String formKey = postInfo.getString(postInfo.getColumnIndex(AwfulPost.FORM_KEY));
				int replyType = postInfo.getInt(postInfo.getColumnIndex(AwfulMessage.TYPE));
				int postId = postInfo.getInt(postInfo.getColumnIndex(AwfulPost.EDIT_POST_ID));
				if(replyType != AwfulMessage.TYPE_EDIT && (formKey == null || message == null || formCookie == null || message.length()<1 || formCookie.length()<1 || formKey.length()<1)){
					Log.e(TAG,"SEND POST FAILED: "+mId+" MISSING VARIABLES");
					return false;
				}
				switch(replyType){
					case AwfulMessage.TYPE_QUOTE:
					case AwfulMessage.TYPE_NEW_REPLY:
						Reply.post(message, formKey, formCookie, Integer.toString(mId));
						break;
					case AwfulMessage.TYPE_EDIT:
						Reply.edit(message, formKey, formCookie, Integer.toString(mId), Integer.toString(postId));
						break;
					default:
						return false;
				}
				cr.delete(AwfulMessage.CONTENT_URI_REPLY, AwfulMessage.ID+"=?", AwfulProvider.int2StrArray(mId));
			}else{
				return false;
			}
		}catch(Exception e){
			Log.e(TAG,"SEND POST FAILED: "+mId+" "+e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
