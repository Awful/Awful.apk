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
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.service.AwfulSyncService;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;

public class FetchProfileTask extends AwfulTask {

	String mUsername;

	public FetchProfileTask(AwfulSyncService sync, Message msg,
			AwfulPreferences aPrefs) {
		super(sync, msg, aPrefs, AwfulSyncService.MSG_FETCH_FEATURES);
		mUsername = (String) msg.obj;
	}

	@Override
	protected String doInBackground(Void... params) {
		try {

			HashMap<String, String> para = new HashMap<String, String>();
			para.put(Constants.PARAM_USERNAME,
					(mUsername == null || mUsername.equals("0")) ? mPrefs.username : mUsername);
			para.put(Constants.PARAM_ACTION, Constants.ACTION_PROFILE);
			Document data = NetworkUtils.get(Constants.FUNCTION_MEMBER, para,
					replyTo, 50);
			//TODO: Do something other than get the ignore key.
			Element formkey = data.getElementsByAttributeValue("name", "formkey").first();
			if (formkey != null) {
				try {
					mPrefs.setStringPreference("ignore_formkey", formkey.val());
				} catch (Exception e) {
					e.printStackTrace();
				}

			} else {
				throw new Exception("Profile page did not load");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "Failed to load account features!";
		}
		return null;
	}
}
