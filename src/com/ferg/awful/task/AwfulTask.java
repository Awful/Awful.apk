package com.ferg.awful.task;

import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.service.AwfulSyncService;

import android.os.AsyncTask;

/** A specialized AsyncTask for use with AwfulSyncService.
 * AwfulSyncService may avoid creating duplicate tasks if the new task's id and arg1 match any other pending task.
 * Can be extended as either separate class file or anonymous inner class.
 */
public abstract class AwfulTask extends AsyncTask<Void, Void, Boolean> {
	public static final String TAG = "AwfulTask";
	protected int mId = 0;
	protected int mArg1 = 0;
	protected AwfulSyncService mContext;
	protected AwfulPreferences mPrefs;
	/**
	 * Constructs AwfulTask that can be queued immediately.
	 * @param sync AwfulSyncService where callbacks will be sent.
	 * @param id Semi-Unique id for this instance, typically used to identify the content to be loaded.
	 * @param arg1 An optional second argument to be used for page numbers, ect. Will be used for purposes of duplicate task avoidance if this value is anything other than 0.
	 * @param aPrefs Some tasks require access to preferences. Reusing an existing AwfulPreference object reduces processing time.
	 */
	public AwfulTask(AwfulSyncService sync, int id, int arg1, AwfulPreferences aPrefs){
		mPrefs = aPrefs;
		mContext = sync;
		mId = id;
		mArg1 = arg1;
	}
	public int getId(){
		return mId;
	}
	public int getArg1(){
		return mArg1;
	}
	@Override
	protected void onPreExecute(){
		if(!isCancelled()){
			mContext.updateStatus(AwfulSyncService.Status.WORKING, mId, mArg1);
		}
	}
	@Override
	public void onPostExecute(Boolean success){
		if(!isCancelled()){
			if(success){
				mContext.updateStatus(AwfulSyncService.Status.OKAY, mId, mArg1);
			}else{
				mContext.updateStatus(AwfulSyncService.Status.ERROR, mId, mArg1);
			}
			mContext.taskFinished(this);
		}
	}

}
