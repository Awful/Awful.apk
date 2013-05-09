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

import android.content.ContentResolver;
import android.os.Message;
import android.util.Log;

import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulEmote;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;

public class TrimDBTask extends AwfulTask {
	private static final int TABLE_THREAD = 1;
	private static final int TABLE_POST = 2;
	private static final int TABLE_UCP = 3;
	public TrimDBTask(AwfulSyncService sync, Message aMsg) {
		super(sync, aMsg, null, AwfulSyncService.MSG_TRIM_DB);
		if(mArg1 <1){
			mArg1 = 7;
		}
	}

	@Override
	protected String doInBackground(Void... params) {
    	ContentResolver dbInterface = mContext.getContentResolver();
    	int rowCount = 0;
		rowCount += dbInterface.delete(AwfulThread.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+mArg1+" days')", null);
		rowCount += dbInterface.delete(AwfulPost.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+mArg1+" days')", null);
		rowCount += dbInterface.delete(AwfulThread.CONTENT_URI_UCP, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+mArg1+" days')", null);
		//rowCount += dbInterface.delete(AwfulForum.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+mArg1+" days')", null);
		rowCount += dbInterface.delete(AwfulEmote.CONTENT_URI, AwfulProvider.UPDATED_TIMESTAMP+" < datetime('now','-"+mArg1+" days')", null);
    	Log.i(TAG,"Trimming DB older than "+mArg1+" days, culled: "+rowCount);
		return null;
	}

}
