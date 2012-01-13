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

package com.ferg.awful.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.htmlcleaner.TagNode;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.task.AwfulTask;
import com.ferg.awful.task.BookmarkTask;
import com.ferg.awful.task.IndexTask;
import com.ferg.awful.task.ThreadTask;
import com.ferg.awful.thread.*;

public class AwfulSyncService extends Service {
    public static final String TAG = "ThreadSyncService";

    public static final int MSG_REGISTER_CLIENT   = 0;
    public static final int MSG_UNREGISTER_CLIENT = 1;
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
    public static final int MSG_SEND_PM       = 11;
    public static final int MSG_VOTE       = 12;

    private HashMap<Integer,Messenger> mClients = new HashMap<Integer,Messenger>();
    private MessageHandler mHandler       = new MessageHandler();
    private Messenger mMessenger          = new Messenger(mHandler);

    private AwfulPreferences mPrefs = new AwfulPreferences(this);
    
	private AwfulTask currentTask;
	private Stack<AwfulTask> threadStack = new Stack<AwfulTask>();

    @Override
    public IBinder onBind(Intent intent) {

        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent aIntent, int aFlags, int aStartId) {
        return Service.START_STICKY;
    }

    public class MessageHandler extends Handler { 
        @Override
        public void handleMessage(Message aMsg) {
            switch (aMsg.what) {
                case MSG_REGISTER_CLIENT:
                    registerClient(aMsg, aMsg.arg1);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    unregisterClient(aMsg.arg1);
                    break;
                case MSG_SYNC_THREAD:
                    queueUniqueThread(new ThreadTask(AwfulSyncService.this, aMsg.arg1, aMsg.arg2, mPrefs));
                    break;
                case MSG_SYNC_FORUM:
                	syncForum(aMsg.arg1, aMsg.arg2);
                    break;
                case MSG_SYNC_INDEX:
                    queueUniqueThread(new IndexTask(AwfulSyncService.this, aMsg.arg1, aMsg.arg2, mPrefs));
                    break;
                case MSG_SET_BOOKMARK:
                	queueUniqueThread(new BookmarkTask(AwfulSyncService.this, aMsg.arg1, aMsg.arg2));
                    break;
            }
        }
    }

    private void registerClient(Message aMsg, int clientId) {
        mClients.put(clientId, aMsg.replyTo);
    }

    private void unregisterClient(int clientId) {
        mClients.remove(clientId);
    }

    public void updateStatus(int aMessageType, int aStatus, int clientId, int arg2) {
        Messenger client = mClients.get(clientId);
        try {
            Message msg = Message.obtain(null, aMessageType, aStatus, arg2);
            client.send(msg);
        } catch (RemoteException e) {
            mClients.remove(client);
        }
    }
    
    private void syncForum(final int aForumId, final int aPage) {
        Log.i(TAG, "Starting Forum sync:"+aForumId);
        //or tasks can be anon inner classes
        queueUniqueThread(new AwfulTask(this, aForumId, aPage, mPrefs, MSG_SYNC_FORUM){

			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					TagNode threads = null;
                    if(mId == Constants.USERCP_ID){
                    	threads = AwfulThread.getUserCPThreads(mArg1);
                        AwfulForum.parseUCPThreads(threads, mArg1, mContext.getContentResolver());
                    }else{
                    	threads = AwfulThread.getForumThreads(mId, mArg1);
                        AwfulForum.parseThreads(threads, mId, mArg1, mContext.getContentResolver());
                    }
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

}
