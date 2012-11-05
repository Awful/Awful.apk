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

import java.util.HashMap;

import org.jsoup.nodes.Document;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.os.Message;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulMessage;

public class SendPrivateMessageTask extends AwfulTask {
	public SendPrivateMessageTask(AwfulSyncService sync, Message aMsg) {
		super(sync, aMsg, null, AwfulSyncService.MSG_SEND_PM);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			ContentResolver cr = mContext.getContentResolver();
			Cursor pmInfo = cr.query(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mId), AwfulProvider.DraftProjection, null, null, null);
			if(pmInfo.getCount() >0 && pmInfo.moveToFirst()){
				HashMap<String, String> para = new HashMap<String, String>();
	            para.put(Constants.PARAM_PRIVATE_MESSAGE_ID, Integer.toString(mId));
	            para.put(Constants.PARAM_ACTION, Constants.ACTION_DOSEND);
	            para.put(Constants.DESTINATION_TOUSER, pmInfo.getString(pmInfo.getColumnIndex(AwfulMessage.RECIPIENT)));
	            para.put(Constants.PARAM_TITLE, pmInfo.getString(pmInfo.getColumnIndex(AwfulMessage.TITLE)));
	            //TODO move to constants
	            if(mId>0){
	            	para.put("prevmessageid", Integer.toString(mId));
	            }
	            para.put(Constants.PARAM_PARSEURL, Constants.YES);
	            para.put("savecopy", "yes");
	            para.put("iconid", "0");
	            para.put(Constants.PARAM_MESSAGE, pmInfo.getString(pmInfo.getColumnIndex(AwfulMessage.REPLY_CONTENT)));
				Document result = NetworkUtils.post(Constants.FUNCTION_PRIVATE_MESSAGE, para);
			}else{
				Log.e(TAG,"PM Send Failure: PM missing from DB "+mId);
				return false;
			}
			
		} catch (Exception e) {
			Log.e(TAG,"PM Send Failure: "+Log.getStackTraceString(e));
			return false;
		}
		return true;
	}

}
