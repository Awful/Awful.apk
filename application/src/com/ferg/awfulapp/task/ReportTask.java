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
import org.jsoup.nodes.Element;

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

public class ReportTask extends AwfulTask {
	
	int mId;
	String mComments;
	public ReportTask(AwfulSyncService sync, Message aMsg) {
		super(sync, aMsg, null, AwfulSyncService.MSG_SEND_PM);
		mId = aMsg.arg1;
		mComments = (String)aMsg.obj;
	}

	@Override
	protected String doInBackground(Void... params) {
		try {
				HashMap<String, String> para = new HashMap<String, String>();
	            para.put(Constants.PARAM_COMMENTS, mComments);
	            para.put(Constants.PARAM_POST_ID, String.valueOf(mId));
				Document result = NetworkUtils.post(Constants.FUNCTION_REPORT, para);
				if(result.getElementById("content") != null){
					Element standard = result.getElementsByClass("standard").first();
					if(standard != null && standard.hasText()){
						if(standard.text().contains("Thank you, but this thread has already been reported recently!")){
							return "Someone has already reported this thread recently";
						}else if(standard.text().contains("Thank you, but your princess is in another castle")){
							return "Thank you for your report";
						}
					}
				}	
				return null;
				} catch (Exception e) {
			Log.e(TAG,"PM Send Failure: "+Log.getStackTraceString(e));
			return "Failed to send report!";
		}
	}
}
