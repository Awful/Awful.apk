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

import android.os.AsyncTask;
import android.os.Message;
import android.os.Messenger;

import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.service.AwfulSyncService;

/** A specialized AsyncTask for use with AwfulSyncService.
 * AwfulSyncService may avoid creating duplicate tasks if the new task's id and arg1 match any other pending task.
 * Can be extended as either separate class file or anonymous inner class.
 */
public abstract class AwfulTask extends AsyncTask<Void, Void, Boolean> {
	public static final String TAG = "AwfulTask";
	protected int mId = 0;
	protected int mArg1 = 0;
	protected int TYPE;
	protected AwfulSyncService mContext;
	protected AwfulPreferences mPrefs;
	private Object replyObject = null;
	protected Messenger replyTo;
	/**
	 * Constructs AwfulTask that can be queued immediately.
	 * @param sync AwfulSyncService where callbacks will be sent.
	 * @param id Semi-Unique id for this instance, typically used to identify the content to be loaded.
	 * @param arg1 An optional second argument to be used for page numbers, ect. Will be used for purposes of duplicate task avoidance if this value is anything other than 0.
	 * @param aPrefs Some tasks require access to preferences. Reusing an existing AwfulPreference object reduces processing time.
	 */
	public AwfulTask(AwfulSyncService sync, Message msg, AwfulPreferences aPrefs, int returnMessage){
		mPrefs = aPrefs;
		mContext = sync;
		mId = msg.arg1;
		mArg1 = msg.arg2;
		TYPE = returnMessage;
		replyTo = msg.replyTo;
	}
	public int getId(){
		return mId;
	}
	public int getArg1(){
		return mArg1;
	}
	public void setReplyObject(Object obj){
		replyObject = obj;
	}
	@Override
	protected void onPreExecute(){
		if(!isCancelled()){
			mContext.updateStatus(replyTo, TYPE, AwfulSyncService.Status.WORKING, mId, mArg1);
		}
	}
	@Override
	public void onPostExecute(Boolean success){
		if(!isCancelled()){
			if(success){
				mContext.updateStatus(replyTo, TYPE, AwfulSyncService.Status.OKAY, mId, mArg1, replyObject);
			}else{
				mContext.updateStatus(replyTo, TYPE, AwfulSyncService.Status.ERROR, mId, mArg1, replyObject);
			}
			mContext.taskFinished(this);
		}
	}

}
