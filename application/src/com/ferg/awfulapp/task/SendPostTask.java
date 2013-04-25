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

package com.ferg.awfulapp.task;

import java.io.IOException;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.os.Message;
import android.util.Log;

import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.reply.Reply;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPost;

public class SendPostTask extends AwfulTask {
	public SendPostTask(AwfulSyncService sync, Message aMsg) {
		super(sync, aMsg, null, AwfulSyncService.MSG_SEND_POST);
	}

	@Override
	protected String doInBackground(Void... params) {
		try{
			ContentResolver cr = mContext.getContentResolver();
			Cursor postInfo = cr.query(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mId), AwfulProvider.DraftPostProjection, null, null, null);
			if(postInfo.getCount() >0 && postInfo.moveToFirst()){
				String message = NetworkUtils.encodeHtml(postInfo.getString(postInfo.getColumnIndex(AwfulMessage.REPLY_CONTENT)));
				String formCookie = postInfo.getString(postInfo.getColumnIndex(AwfulPost.FORM_COOKIE));
				String formKey = postInfo.getString(postInfo.getColumnIndex(AwfulPost.FORM_KEY));
				String attachment = postInfo.getString(postInfo.getColumnIndex(AwfulMessage.REPLY_ATTACHMENT));
				String bookmark = postInfo.getString(postInfo.getColumnIndex(AwfulPost.FORM_BOOKMARK));
				int replyType = postInfo.getInt(postInfo.getColumnIndex(AwfulMessage.TYPE));
				int postId = postInfo.getInt(postInfo.getColumnIndex(AwfulPost.EDIT_POST_ID));
				if(replyType != AwfulMessage.TYPE_EDIT && (formKey == null || message == null || formCookie == null || message.length()<1 || formCookie.length()<1 || formKey.length()<1)){
					Log.e(TAG,"SEND POST FAILED: "+mId+" MISSING VARIABLES");
					return LOADING_FAILED;
				}
				switch(replyType){
					case AwfulMessage.TYPE_QUOTE:
					case AwfulMessage.TYPE_NEW_REPLY:
						Reply.post(message, formKey, formCookie, Integer.toString(mId), bookmark, attachment);
						break;
					case AwfulMessage.TYPE_EDIT:
						Reply.edit(message, formKey, formCookie, Integer.toString(mId), Integer.toString(postId), bookmark, attachment);
						break;
					default:
						return LOADING_FAILED;
				}
				cr.delete(AwfulMessage.CONTENT_URI_REPLY, AwfulMessage.ID+"=?", AwfulProvider.int2StrArray(mId));
			}else{
				return LOADING_FAILED;
			}
		}catch (IOException e) {
			e.printStackTrace();
			return "Network failure!";
		}catch(Exception e){
			Log.e(TAG,"SEND POST FAILED: "+mId+" "+e.getMessage());
			e.printStackTrace();
			return LOADING_FAILED;
		}
		return null;
	}

}
