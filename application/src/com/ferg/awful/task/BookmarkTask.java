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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Message;
import android.util.Log;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.provider.AwfulProvider;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulThread;

public class BookmarkTask extends AwfulTask {
	/**
	 * 
	 * @param sync Service.
	 * @param id Thread ID to bookmark/unbookmark.
	 * @param arg1 Set to 1 to bookmark, 0 to remove bookmark.
	 * @param aPrefs
	 */
	public BookmarkTask(AwfulSyncService sync, Message aMsg) {
		super(sync, aMsg, null, AwfulSyncService.MSG_SET_BOOKMARK);
	}

	@Override
	protected String doInBackground(Void... p) {
		if (!isCancelled()) {
        	HashMap<String, String> params = new HashMap<String, String>();
            params.put(Constants.PARAM_THREAD_ID, Integer.toString(mId));
            ContentResolver cr = mContext.getContentResolver();
            if(mArg1 < 1){
            	params.put(Constants.PARAM_ACTION, "remove");
            	cr.delete(AwfulThread.CONTENT_URI_UCP, AwfulThread.ID+"=?", AwfulProvider.int2StrArray(mId));
            }else{
            	params.put(Constants.PARAM_ACTION, "add");
            }

            try {
                NetworkUtils.postIgnoreBody(Constants.FUNCTION_BOOKMARK, params);
                ContentValues cv = new ContentValues();
                cv.put(AwfulThread.BOOKMARKED, mArg1);
                cr.update(AwfulThread.CONTENT_URI, cv, AwfulThread.ID+"=?", AwfulProvider.int2StrArray(mId));
            } catch (Exception e) {
                Log.i(TAG, e.toString());
                return "Failed to set bookmark status!";
            }
        }
        return null;
	}

}
