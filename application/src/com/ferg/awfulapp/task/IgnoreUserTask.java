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

import android.os.Message;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.service.AwfulSyncService;

public class IgnoreUserTask extends AwfulTask {

	int userId = 0;
	AwfulPreferences mPrefs;

	public IgnoreUserTask(AwfulSyncService sync, Message aMsg, AwfulPreferences aPrefs) {
		super(sync, aMsg, null, AwfulSyncService.MSG_SEND_POST);
		userId = aMsg.arg1;
		mPrefs = aPrefs;
	}

	@Override
	protected String doInBackground(Void... params) {
		if (!isCancelled()) {
			HashMap<String, String> parameters = new HashMap<String, String>();
			parameters.put(Constants.PARAM_ACTION, Constants.ACTION_ADDLIST);
			parameters.put(Constants.PARAM_USERLIST, Constants.USERLIST_IGNORE);
			parameters.put(Constants.FORMKEY, mPrefs.ignoreFormkey);
			parameters.put(Constants.PARAM_USER_ID, String.valueOf(userId));
			try {
				NetworkUtils.postIgnoreBody(Constants.FUNCTION_MEMBER2,
						parameters);
			} catch (Exception e) {
				e.printStackTrace();
				return "Failed to ignore user";
			}
		}
		return null;
	}

}