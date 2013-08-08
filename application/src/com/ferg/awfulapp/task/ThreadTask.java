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

import android.os.Message;
import android.util.Log;

import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulThread;

public class ThreadTask extends AwfulTask {
	public static final String TAG = "ThreadTask";

	private int mUserId = 0;
	
	public ThreadTask(AwfulSyncService sync, Message aMsg, AwfulPreferences aPrefs) {
		super(sync, aMsg, aPrefs, AwfulSyncService.MSG_SYNC_THREAD);
		mUserId = (Integer) aMsg.obj;
	}

	@Override
	protected String doInBackground(Void... params) {
        if(isCancelled()){
            return null;
        }
		String error = null;
		try {
			//error = AwfulThread.getThreadPosts(mContext, mId, mArg1, mPrefs.postPerPage, mPrefs, mUserId);
            Log.i(TAG, "Sync complete");
        } catch (Exception e) {
            Log.e(TAG, "Sync error");
            e.printStackTrace();
            return "Failed to load thread!";
        }
		mContext.queueDelayedMessage(AwfulSyncService.MSG_TRIM_DB, 1000, 0, 7);
		return error;
	}

}
