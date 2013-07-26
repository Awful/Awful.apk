/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
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

package com.ferg.awfulapp.service;

import java.util.Iterator;
import java.util.Stack;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.task.*;

public class AwfulSyncService extends Service {
    public static final String TAG = "ThreadSyncService";

    public static final int MSG_PROGRESS_PERCENT   = 1;
    public static final int MSG_SYNC_THREAD       = 2;
    public static final int MSG_PROGRESS_STATUS   = 3;
    public static final int MSG_SYNC_FORUM       = 4;
    public static final int MSG_SYNC_INDEX       = 5;
    /** arg1 = threadId. Set arg2 = 1 to add bookmark, 0 to remove bookmark. */
    public static final int MSG_SET_BOOKMARK       = 6;
    public static final int MSG_FETCH_PM       = 7;
    public static final int MSG_MARK_LASTREAD       = 8;
    public static final int MSG_MARK_UNREAD       = 9;
    public static final int MSG_FETCH_PM_INDEX       = 10;
    /** arg1 = pmId. */
    public static final int MSG_SEND_PM       = 11;
    /** arg1 = threadId, arg2 = vote (1-5) */
    public static final int MSG_VOTE       = 12;
    /** arg1 = threadId. arg2 = post index (for quote/edit) */
    public static final int MSG_FETCH_POST_REPLY       = 13;
    /** arg1 = threadId. */
    public static final int MSG_SEND_POST       = 14;
    /** arg1 = (optional) table to clear (from TrimDBTask.TABLE_*),
     *  arg2 = (optional) messages older than this number of days are trimmed, default: 7 **/
	public static final int MSG_TRIM_DB = 15;
	/** arg1 = category/emote id, arg2 = url hash for duplicate task prevention, obj = String url **/
	public static final int MSG_GRAB_IMAGE = 16;
	public static final int MSG_FETCH_EMOTES = 17;
	/** obj = initial string, returns string with redirected URL **/
	public static final int MSG_TRANSLATE_REDIRECT = 18;
    public static final int MSG_ERR_NOT_LOGGED_IN   = 19;
    /** generic error message, (optional) obj=String - error message to display **/
	public static final int MSG_ERROR = 20;
    /** forums closed error message, (optional) obj=String - error message to display **/
	public static final int MSG_ERROR_FORUMS_CLOSED = 21;
    public static final int MSG_CANCEL_SYNC_THREAD  = 22;
    public static final int MSG_FETCH_FEATURES = 23;
    public static final int MSG_FETCH_PROFILE = 24;
    public static final int MSG_IGNORE_USER = 25;
	
    private MessageHandler mHandler       = new MessageHandler();
    private Messenger mMessenger          = new Messenger(mHandler);

    private AwfulPreferences mPrefs;
    
	private AwfulTask currentTask;
	private Stack<AwfulTask> threadStack = new Stack<AwfulTask>();

    @Override
    public IBinder onBind(Intent intent) {
    	mPrefs = AwfulPreferences.getInstance(this);
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent aIntent, int aFlags, int aStartId) {
        return Service.START_STICKY;
    }

    public class MessageHandler extends Handler { 
        @Override
        public void handleMessage(Message aMsg) {
        	debugLogReceivedMessage(TAG,aMsg);
            switch (aMsg.what) {
                case MSG_SYNC_THREAD:
                    queueUniqueThread(new ThreadTask(AwfulSyncService.this, aMsg, mPrefs));
                    break;
                case MSG_SYNC_FORUM:
                    queueUniqueThread(new ForumTask(AwfulSyncService.this, aMsg, mPrefs));
                    break;
                case MSG_SYNC_INDEX:
                	backQueueUniqueThread(new IndexTask(AwfulSyncService.this, aMsg, mPrefs));
                    break;
                case MSG_SET_BOOKMARK:
                	queueUniqueThread(new BookmarkTask(AwfulSyncService.this, aMsg));
                    break;
                case MSG_FETCH_PM_INDEX:
                	queueUniqueThread(new PrivateMessageIndexTask(AwfulSyncService.this, aMsg));
                    break;
                case MSG_FETCH_PM:
                	queueUniqueThread(new FetchPrivateMessageTask(AwfulSyncService.this, aMsg, mPrefs));
                    break;
                case MSG_MARK_LASTREAD:
                	queueUniqueThread(new MarkLastReadTask(AwfulSyncService.this, aMsg));
                    break;
                case MSG_MARK_UNREAD:
                	queueUniqueThread(new MarkUnreadTask(AwfulSyncService.this, aMsg));
                    break;
                case MSG_VOTE:
                	queueUniqueThread(new VotingTask(AwfulSyncService.this, aMsg, getApplicationContext()));
                    break;
                case MSG_SEND_PM:
                	queueUniqueThread(new SendPrivateMessageTask(AwfulSyncService.this, aMsg));
                    break;
                case MSG_FETCH_POST_REPLY:
                	queueUniqueThread(new FetchReplyTask(AwfulSyncService.this, aMsg));
                    break;
                case MSG_SEND_POST:
                	queueUniqueThread(new SendPostTask(AwfulSyncService.this, aMsg));
                    break;
                case MSG_TRIM_DB:
                	backQueueUniqueThread(new TrimDBTask(AwfulSyncService.this, aMsg));
                    break;
                case MSG_GRAB_IMAGE:
                	backQueueUniqueThread(new ImageCacheTask(AwfulSyncService.this, aMsg));
                    break;
                case MSG_TRANSLATE_REDIRECT:
                	queueUniqueThread(new RedirectTask(AwfulSyncService.this, aMsg));
                    break;
                case MSG_FETCH_EMOTES:
                	queueUniqueThread(new FetchEmotesTask(AwfulSyncService.this, aMsg, mPrefs));
                    break;
                case MSG_CANCEL_SYNC_THREAD:
                    cancelTypeTasks(MSG_SYNC_THREAD, aMsg.arg2);
                    break;
                case MSG_FETCH_FEATURES:
                    queueUniqueThread(new FetchFeaturesTask(AwfulSyncService.this, aMsg, mPrefs));
                    break;
                case MSG_FETCH_PROFILE:
                    queueUniqueThread(new FetchProfileTask(AwfulSyncService.this, aMsg, mPrefs));
                    break;
                case MSG_IGNORE_USER:
                    queueUniqueThread(new IgnoreUserTask(AwfulSyncService.this, aMsg, mPrefs));
                    break;
            }
        }
    }
    public void updateStatus(Messenger client, int aMessageType, int aStatus, int clientId, int arg2){
    	updateStatus(client, aMessageType, aStatus, clientId, arg2, null);
    }
    public void updateStatus(Messenger client, int aMessageType, int aStatus, int clientId, int arg2, Object obj) {
        Log.i(TAG, "Send Message - id: "+clientId+" type: "+getMessageTypeFromId(aMessageType)+" status: "+getMessageStatusFromId(aStatus)+" arg2: "+arg2);
        if(client != null){
	        try {
	            Message msg = Message.obtain(null, aMessageType, aStatus, arg2, obj);
	            client.send(msg);
	        } catch (RemoteException e) {
	        }
        }
    }
    /**
     * Queues a message intended for the AwfulSyncService, which will be processed after the specified delay.
     * Useful for triggering background processes (such as TrimDBTask).
     * @param msgId Message ID from AwfulSyncService.MSG_*
     * @param delayMillis
     * @param arg1 Arguments to pass into the message.
     * @param arg2
     */
    public void queueDelayedMessage(int msgId, int delayMillis, int arg1, int arg2){
        Log.i(TAG, "Send Message - delay: "+delayMillis+" type: "+msgId+" arg1: "+arg1+" arg2: "+arg2);
    	mHandler.sendMessageDelayed(mHandler.obtainMessage(msgId, arg1, arg2), delayMillis);
    }

    public static class Status {
        public static final int WORKING       = 0;
        public static final int OKAY          = 1;
        public static final int ERROR = 2;
    }

    //////THREAD QUEUING STUFF//////
    
	private void queueThread(AwfulTask threadTask) {
		threadStack.push(threadTask);
		startNextThread();
	}
	private void backQueueThread(AwfulTask threadTask) {
		threadStack.add(threadTask);
		startNextThread();
	}
	/**
	 * Queues a thread only if there is no duplicate already in the queue.
	 * It compares both task's ID and arg1 (if not 0) for equality.
	 * @param threadTask
	 */
	private void queueUniqueThread(AwfulTask threadTask) {
		if(!isThreadQueued(threadTask.getArg1(), threadTask.getArg2())){
			queueThread(threadTask);
		}
	}
	/**
	 * Queues a thread to the back of the queue, if there is no duplicate already in the queue.
	 * @param threadTask
	 */
	private void backQueueUniqueThread(AwfulTask threadTask) {
		if(!isThreadQueued(threadTask.getArg1(), threadTask.getArg2())){
			backQueueThread(threadTask);
		}
	}

    /**
     * Cancels all tasks of 'type', except any with an arg2 that matches the given argument.
     * @param type
     * @param excludeArg2
     */
    private void cancelTypeTasks(int type, int excludeArg2){
        Iterator<AwfulTask> awfulTaskIterator = threadStack.iterator ();
        while(awfulTaskIterator.hasNext()){
            AwfulTask at = awfulTaskIterator.next();
            if(at.getType() == type && excludeArg2 != at.getArg2()){
                awfulTaskIterator.remove();
            }
        }
        if(currentTask != null && currentTask.getType() == type && currentTask.getArg2() != excludeArg2){
            currentTask.cancel(true);
            currentTask = null;
            startNextThread();
        }
    }

	private void startNextThread() {
		if(currentTask == null && !threadStack.isEmpty()){
			currentTask = threadStack.pop();
			currentTask.execute();
		}
	}

	public void taskFinished(AwfulTask task) {
		if(currentTask.getArg1() == task.getArg1()){
			currentTask = null;
			startNextThread();
		}
	}
	private boolean isThreadQueued(int targetId, int arg1) {
		for(AwfulTask at : threadStack){
			if(at.getArg1()== targetId && (arg1 == 0 || at.getArg2() == arg1)){//if arg1 is 0, we arn't using it
				return true;
			}
		}
		if(currentTask != null && currentTask.getArg1()== targetId && (arg1 == 0 || currentTask.getArg2() == arg1)){
			return true;
		}
		return false;
	}
	
	public static String getMessageTypeFromId(int what){
		switch(what){
		case MSG_SYNC_THREAD:
			return "MSG_SYNC_THREAD";
		case MSG_SYNC_FORUM:
			return "MSG_SYNC_FORUM";
		case MSG_SYNC_INDEX:
			return "MSG_SYNC_INDEX";
		case MSG_FETCH_PM_INDEX:
			return "MSG_FETCH_PM_INDEX";
		case MSG_ERR_NOT_LOGGED_IN:
			return "MSG_ERR_NOT_LOGGED_IN";
		case MSG_PROGRESS_PERCENT:
			return "MSG_PROGRESS_PERCENT";
		case MSG_PROGRESS_STATUS:
			return "MSG_PROGRESS_STATUS";
		case MSG_SET_BOOKMARK:
			return "MSG_SET_BOOKMARK";
		case MSG_FETCH_PM:
			return "MSG_FETCH_PM";
		case MSG_MARK_LASTREAD:
			return "MSG_MARK_LASTREAD";
		case MSG_MARK_UNREAD:
			return "MSG_MARK_UNREAD";
		case MSG_SEND_PM:
			return "MSG_SEND_PM";
		case MSG_VOTE:
			return "MSG_VOTE";
		case MSG_FETCH_POST_REPLY:
			return "MSG_FETCH_POST_REPLY";
		case MSG_SEND_POST:
			return "MSG_SEND_POST";
		case MSG_TRIM_DB:
			return "MSG_TRIM_DB";
		case MSG_GRAB_IMAGE:
			return "MSG_GRAB_IMAGE";
		case MSG_FETCH_EMOTES:
			return "MSG_FETCH_EMOTES";
		case MSG_TRANSLATE_REDIRECT:
			return "MSG_TRANSLATE_REDIRECT";
		case MSG_ERROR:
			return "MSG_ERROR";
		case MSG_ERROR_FORUMS_CLOSED:
			return "MSG_ERROR_FORUMS_CLOSED";
		default:
			return what+"";
		}
	}
	
	public static String getMessageStatusFromId(int status){
		switch(status){
		case Status.WORKING:
			return "WORKING";
		case Status.ERROR:
			return "ERROR";
		case Status.OKAY:
			return "OKAY";
		default:
			return status+"";
		}
	}

	public static void debugLogReceivedMessage(String tag, Message aMsg) {
		String msg = aMsg.what+" - "+getMessageTypeFromId(aMsg.what);
		Log.i(tag, tag+" Received: "+msg+" arg1: "+getMessageStatusFromId(aMsg.arg1)+" arg2: "+aMsg.arg2);
	}

}
