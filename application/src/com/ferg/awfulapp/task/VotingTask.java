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

import android.content.Context;
import android.os.Message;
import android.widget.Toast;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.service.AwfulSyncService;

public class VotingTask extends AwfulTask {
	protected Context ApplicationContext;
	/**
	 * @param id Thread id.
	 * @param arg1 Vote (1-5)
	 */
	public VotingTask(AwfulSyncService sync, Message aMsg, Context cxt) {
		super(sync, aMsg, null, AwfulSyncService.MSG_VOTE);
		this.ApplicationContext = cxt;
	}

	@Override
	protected Boolean doInBackground(Void... p) {
		if (!isCancelled()) {
        	
			HashMap<String, String> params = new HashMap<String, String>();
			params.put(Constants.PARAM_THREAD_ID, String.valueOf(mId));
			params.put(Constants.PARAM_VOTE, String.valueOf(mArg1+1));

            try {
            	NetworkUtils.postIgnoreBody(Constants.FUNCTION_RATE_THREAD, params);
            } catch (Exception e) {
                e.printStackTrace();
            	return false;
            }
        }
        return true;
	}

	@Override
	public void onPostExecute(Boolean success){
		if(!isCancelled()){
			if(success){
				mContext.updateStatus(replyTo, TYPE, AwfulSyncService.Status.OKAY, mId, mArg1);
				Toast successToast = Toast.makeText(ApplicationContext, String.format(this.mContext.getString(R.string.vote_succeeded), mArg1+1), Toast.LENGTH_LONG);
				successToast.show();
			}else{
				mContext.updateStatus(replyTo, TYPE, AwfulSyncService.Status.ERROR, mId, mArg1);
				Toast errorToast = Toast.makeText(ApplicationContext, R.string.vote_failed, Toast.LENGTH_LONG);
				errorToast.show();
			}
			mContext.taskFinished(this);
		}
	}
}
