package com.ferg.awfulapp.task;

import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.service.AwfulSyncService;

import android.os.AsyncTask;
import android.os.Message;
import android.os.Messenger;

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
