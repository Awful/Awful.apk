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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Message;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;

public class MarkLastReadTask extends AwfulTask {

	public MarkLastReadTask(AwfulSyncService sync, Message aMsg) {
		super(sync, aMsg, null, AwfulSyncService.MSG_MARK_LASTREAD);
	}

	@Override
	protected String doInBackground(Void... params) {
		if (!isCancelled()) {
            try {
            	HashMap<String, String> param = new HashMap<String, String>();
                param.put(Constants.PARAM_ACTION, "setseen");
                param.put(Constants.PARAM_THREAD_ID, Integer.toString(mId));
                param.put(Constants.PARAM_INDEX, Integer.toString(mArg1));
                NetworkUtils.get(Constants.FUNCTION_THREAD, param);
                
                //set unread posts (> unreadIndex)
                ContentValues last_read = new ContentValues();
                last_read.put(AwfulPost.PREVIOUSLY_READ, 0);
                ContentResolver resolv = mContext.getContentResolver();
                resolv.update(AwfulPost.CONTENT_URI, 
							                		last_read, 
							                		AwfulPost.THREAD_ID+"=? AND "+AwfulPost.POST_INDEX+">?", 
							                		AwfulProvider.int2StrArray(mId,mArg1));
                
                //set previously read posts (< unreadIndex)
                last_read.put(AwfulPost.PREVIOUSLY_READ, 1);
                resolv.update(AwfulPost.CONTENT_URI, 
							                		last_read, 
							                		AwfulPost.THREAD_ID+"=? AND "+AwfulPost.POST_INDEX+"<=?", 
							                		AwfulProvider.int2StrArray(mId,mArg1));
                
                //update unread count
                Cursor threadData = resolv.query(ContentUris.withAppendedId(AwfulThread.CONTENT_URI, mId), AwfulProvider.ThreadProjection, null, null, null);
                if(threadData.getCount()>0 && threadData.moveToFirst()){
	                ContentValues thread_update = new ContentValues();
	                thread_update.put(AwfulThread.UNREADCOUNT, threadData.getInt(threadData.getColumnIndex(AwfulThread.POSTCOUNT)) - mArg1);
	                mContext.getContentResolver().update(AwfulThread.CONTENT_URI, 
	                									thread_update, 
	                									AwfulThread.ID+"=?", 
								                		AwfulProvider.int2StrArray(mId));
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Failed to mark position!";
            }
        }
        return null;
	}

}
