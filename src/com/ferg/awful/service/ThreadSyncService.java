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

import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.thread.*;

public class ThreadSyncService extends Service {
    public static final String TAG = "ThreadSyncService";

    public static final int MSG_REGISTER_CLIENT   = 0;
    public static final int MSG_UNREGISTER_CLIENT = 1;
    public static final int MSG_SYNC_THREAD       = 2;
    public static final int MSG_PROGRESS_STATUS   = 3;

    private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    private MessageHandler mHandler       = new MessageHandler();
    private Messenger mMessenger          = new Messenger(mHandler);

    private AwfulPreferences mPrefs;

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent aIntent, int aFlags, int aStartId) {
        mPrefs = new AwfulPreferences(this);

        return Service.START_STICKY;
    }

    public class MessageHandler extends Handler { 
        @Override
        public void handleMessage(Message aMsg) {
            switch (aMsg.what) {
                case MSG_REGISTER_CLIENT:
                    registerClient(aMsg);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    unregisterClient(aMsg);
                    break;
                case MSG_SYNC_THREAD:
                    syncThread(aMsg.arg1, aMsg.arg2);
                    break;
            }
        }
    }

    private void registerClient(Message aMsg) {
        mClients.add(aMsg.replyTo);
    }

    private void unregisterClient(Message aMsg) {
        mClients.remove(aMsg.replyTo);
    }

    private void updateStatus(int aStatus) {
        for (Messenger client : mClients) {
            try {
                Message msg = Message.obtain(null, MSG_PROGRESS_STATUS, aStatus, 0);
                client.send(msg);
            } catch (RemoteException e) {
                mClients.remove(client);
            }
        }
    }

    private void syncThread(final int aThreadId, final int aPage) {
        updateStatus(Status.WORKING);

        Log.i(TAG, "Starting sync");
        new Thread() {
            public void run() {
                try {
                	AwfulThread.getThreadPosts(aThreadId, aPage, mPrefs.postPerPage, mPrefs);
                    Log.i(TAG, "Sync complete");
                    updateStatus(Status.OKAY);
                } catch (Exception e) {
                    Log.i(TAG, "Sync error");
                    updateStatus(Status.ERROR);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public static class Status {
        public static final int WORKING       = 0;
        public static final int OKAY          = 1;
        public static final int ERROR = 2;
    }
}
