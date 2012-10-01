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

import java.util.Stack;

import org.htmlcleaner.TagNode;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.task.AwfulTask;
import com.ferg.awfulapp.task.BookmarkTask;
import com.ferg.awfulapp.task.FetchPrivateMessageTask;
import com.ferg.awfulapp.task.FetchReplyTask;
import com.ferg.awfulapp.task.ImageCacheTask;
import com.ferg.awfulapp.task.IndexTask;
import com.ferg.awfulapp.task.MarkLastReadTask;
import com.ferg.awfulapp.task.MarkUnreadTask;
import com.ferg.awfulapp.task.PrivateMessageIndexTask;
import com.ferg.awfulapp.task.RedirectTask;
import com.ferg.awfulapp.task.SendPostTask;
import com.ferg.awfulapp.task.SendPrivateMessageTask;
import com.ferg.awfulapp.task.ThreadTask;
import com.ferg.awfulapp.task.TrimDBTask;
import com.ferg.awfulapp.task.VotingTask;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.thread.AwfulThread;

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
	
    private MessageHandler mHandler       = new MessageHandler();
    private Messenger mMessenger          = new Messenger(mHandler);

    private AwfulPreferences mPrefs;
    
	private AwfulTask currentTask;
	private Stack<AwfulTask> threadStack = new Stack<AwfulTask>();

    @Override
    public IBinder onBind(Intent intent) {
    	mPrefs = new AwfulPreferences(this);
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
                	syncForum(aMsg);
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
            }
        }
    }
    public void updateStatus(Messenger client, int aMessageType, int aStatus, int clientId, int arg2){
    	updateStatus(client, aMessageType, aStatus, clientId, arg2, null);
    }
    public void updateStatus(Messenger client, int aMessageType, int aStatus, int clientId, int arg2, Object obj) {
        Log.i(TAG, "Send Message - id: "+clientId+" type: "+aMessageType+" status: "+aStatus+" arg2: "+arg2);
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
    	mHandler.sendMessageDelayed(mHandler.obtainMessage(msgId, arg1, arg2), delayMillis);
    }
    
    private void syncForum(Message aMsg) {
        Log.i(TAG, "Starting Forum sync:"+aMsg.arg1);
        //tasks can be anon inner classes
        //but why would i do this
        queueUniqueThread(new AwfulTask(this, aMsg, mPrefs, MSG_SYNC_FORUM){

			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					TagNode threads = null;
                    replyTo.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, mId, 25));
                    if(mId == Constants.USERCP_ID){
                    	threads = AwfulThread.getUserCPThreads(mArg1, replyTo);
                        replyTo.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, mId, 75));
                		if(threads.findElementByAttValue("id", "notregistered", true, false) != null){
                        	NetworkUtils.clearLoginCookies(AwfulSyncService.this);
                        	replyTo.send(Message.obtain(null, AwfulSyncService.MSG_ERR_NOT_LOGGED_IN, 0, 0));
                        	return false;
                		}
                        AwfulForum.parseUCPThreads(threads, mArg1, mContext.getContentResolver());
                    }else{
                    	threads = AwfulThread.getForumThreads(mId, mArg1, replyTo);
                        replyTo.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, mId, 75));
                		if(threads.findElementByAttValue("id", "notregistered", true, false) != null){
                        	NetworkUtils.clearLoginCookies(AwfulSyncService.this);
                        	replyTo.send(Message.obtain(null, AwfulSyncService.MSG_ERR_NOT_LOGGED_IN, 0, 0));
                        	return false;
                        }
                        AwfulForum.parseThreads(threads, mId, mArg1, mContext.getContentResolver());
                    }
                    replyTo.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, mId, 100));
                } catch (Exception e) {
                    Log.i(TAG, "Sync error");
                    e.printStackTrace();
                    return false;
                }
				return true;
			}
        });
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
		if(!isThreadQueued(threadTask.getId(), threadTask.getArg1())){
			queueThread(threadTask);
		}
	}
	/**
	 * Queues a thread to the back of the queue, if there is no duplicate already in the queue.
	 * @param threadTask
	 */
	private void backQueueUniqueThread(AwfulTask threadTask) {
		if(!isThreadQueued(threadTask.getId(), threadTask.getArg1())){
			backQueueThread(threadTask);
		}
	}
	private void startNextThread() {
		if(currentTask == null && !threadStack.isEmpty()){
			currentTask = threadStack.pop();
			currentTask.execute();
		}
	}

	public void taskFinished(AwfulTask task) {
		if(currentTask.getId() == task.getId()){
			currentTask = null;
			startNextThread();
		}
	}
	private boolean isThreadQueued(int targetId, int arg1) {
		for(AwfulTask at : threadStack){
			if(at.getId()== targetId && (arg1 == 0 || at.getArg1() == arg1)){//if arg1 is 0, we arn't using it
				return true;
			}
		}
		if(currentTask != null && currentTask.getId()== targetId && (arg1 == 0 || currentTask.getArg1() == arg1)){
			return true;
		}
		return false;
	}

	public static void debugLogReceivedMessage(String tag, Message aMsg) {
		String msg = aMsg.what+" - ";
		switch(aMsg.what){
		case MSG_SYNC_THREAD:
			msg += "MSG_SYNC_THREAD";
			break;
		case MSG_SYNC_FORUM:
			msg += "MSG_SYNC_FORUM";
			break;
		case MSG_SYNC_INDEX:
			msg += "MSG_SYNC_INDEX";
			break;
		case MSG_FETCH_PM_INDEX:
			msg += "MSG_FETCH_PM_INDEX";
			break;
		case MSG_ERR_NOT_LOGGED_IN:
			msg += "MSG_ERR_NOT_LOGGED_IN";
			break;
		}
		Log.v(tag, tag+" Received: "+msg+" arg1: "+aMsg.arg1+" arg2: "+aMsg.arg2);
	}

}
