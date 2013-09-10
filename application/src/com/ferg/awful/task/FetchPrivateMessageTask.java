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

import java.util.HashMap;

import org.jsoup.nodes.Document;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.os.Message;
import android.util.Log;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulMessage;

public class FetchPrivateMessageTask extends AwfulTask {

	public FetchPrivateMessageTask(AwfulSyncService sync, Message aMsg,	AwfulPreferences aPrefs) {
		super(sync, aMsg, aPrefs, AwfulSyncService.MSG_FETCH_PM);
	}

	@Override
	protected String doInBackground(Void... params) {
		try {
			HashMap<String, String> para = new HashMap<String, String>();
            para.put(Constants.PARAM_PRIVATE_MESSAGE_ID, Integer.toString(mId));
            para.put(Constants.PARAM_ACTION, "show");
			Document pmData = NetworkUtils.get(Constants.FUNCTION_PRIVATE_MESSAGE, para);
			ContentResolver cr = mContext.getContentResolver();
			ContentValues message = AwfulMessage.processMessage(pmData, mId);
			if(cr.update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI, mId), message, null, null)<1){
				cr.insert(AwfulMessage.CONTENT_URI, message);
			}
            para.put(Constants.PARAM_ACTION, "newmessage");
            Document pmReplyData = NetworkUtils.get(Constants.FUNCTION_PRIVATE_MESSAGE, para);
			ContentValues reply = AwfulMessage.processReplyMessage(pmReplyData, mId);
			reply.put(AwfulMessage.RECIPIENT,message.getAsString(AwfulMessage.AUTHOR));
			//we remove the reply content so as not to override the existing reply.
			String replyContent = reply.getAsString(AwfulMessage.REPLY_CONTENT);
			reply.remove(AwfulMessage.REPLY_CONTENT);
			String replyTitle = reply.getAsString(AwfulMessage.TITLE);
			reply.remove(AwfulMessage.TITLE);
			if(cr.update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mId), reply, null, null)<1){
				//but if the reply doesn't already exist, insert it.
				reply.put(AwfulMessage.REPLY_CONTENT, replyContent);
				reply.put(AwfulMessage.TITLE, replyTitle);
				cr.insert(AwfulMessage.CONTENT_URI_REPLY, reply);
			}
			Log.v(TAG,"Fetched msg: "+mId);
		} catch (Exception e) {
			Log.e(TAG,"PM Load Failure: "+Log.getStackTraceString(e));
			return "Failed to load PMs!";
		}
		return null;
	}

}
