/********************************************************************************
 * Copyright (c) 2012, Matthew Shepard
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the software nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.ferg.awful.task;

import java.sql.Timestamp;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.os.Message;
import android.util.Log;

import com.ferg.awful.provider.AwfulProvider;
import com.ferg.awful.reply.Reply;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulMessage;

public class FetchReplyTask extends AwfulTask {
	
	private int type;

	public FetchReplyTask(AwfulSyncService sync, Message aMsg) {
		super(sync, aMsg, null, AwfulSyncService.MSG_FETCH_POST_REPLY);
		type = (Integer) aMsg.obj;
	}

	@Override
	protected String doInBackground(Void... params) {
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
					return "Internal Error: Unknown reply type!";
			}
			reply.put(AwfulProvider.UPDATED_TIMESTAMP, new Timestamp(System.currentTimeMillis()).toString());
			if(contentResolver.update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mId), reply, null, null)<1){
				contentResolver.insert(AwfulMessage.CONTENT_URI_REPLY, reply);
			}
			Log.i(TAG, "Reply loaded and saved: "+mId);
		}catch(Exception e){
			Log.e(TAG, "Reply Load Failure: "+mId+" - "+e.getMessage());
			return "Failed to load reply!";
		}
		return null;
	}

}
