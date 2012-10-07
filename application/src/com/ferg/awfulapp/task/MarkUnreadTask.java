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

import android.content.ContentUris;
import android.content.ContentValues;
import android.os.Message;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;

public class MarkUnreadTask extends AwfulTask {

	public MarkUnreadTask(AwfulSyncService sync, Message aMsg) {
		super(sync, aMsg, null, AwfulSyncService.MSG_MARK_UNREAD);
	}

	@Override
	protected Boolean doInBackground(Void... p) {
		if (!isCancelled()) {
        	HashMap<String, String> params = new HashMap<String, String>();
            params.put(Constants.PARAM_THREAD_ID, Integer.toString(mId));
            params.put(Constants.PARAM_ACTION, "resetseen");

            try {
                NetworkUtils.postIgnoreBody(Constants.FUNCTION_THREAD, params);
                ContentValues last_read = new ContentValues();
                last_read.put(AwfulPost.PREVIOUSLY_READ, 0);
                mContext.getContentResolver().update(AwfulPost.CONTENT_URI, last_read, AwfulPost.THREAD_ID+"=?", new String[]{Integer.toString(mId)});
                ContentValues unread = new ContentValues();
                unread.put(AwfulThread.UNREADCOUNT, -1);
                mContext.getContentResolver().update(ContentUris.withAppendedId(AwfulThread.CONTENT_URI, mId), unread, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            	return false;
            }
        }
		return true;
	}

}
