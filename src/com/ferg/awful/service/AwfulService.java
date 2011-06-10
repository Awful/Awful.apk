package com.ferg.awful.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.htmlcleaner.TagNode;

import com.ferg.awful.R;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulPagedItem;
import com.ferg.awful.thread.AwfulThread;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class AwfulService extends Service {
	private static final String TAG = "AwfulService";
	private HashMap<String, AwfulPagedItem> db = new HashMap<String, AwfulPagedItem>();
	private boolean loggedIn;
	private AwfulTask<?> currentTask;
	private Stack<AwfulTask<?>> threadPool = new Stack<AwfulTask<?>>();
	
	public void onCreate(){
		loggedIn = NetworkUtils.restoreLoginCookies(this);
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		Log.e(TAG, "Service started.");
	}

	public void onDestroy(){
		Log.e(TAG, "Service onDestroy.");
		if(currentTask != null){
			currentTask.cancel(true);
		}
		threadPool.clear();
	}
	private void queueThread(AwfulTask<?> threadTask) {
		threadPool.push(threadTask);
		startNextThread();
	}
	private void startNextThread() {
		if(currentTask == null && !threadPool.isEmpty()){
			currentTask = threadPool.pop();
			currentTask.execute();
		}
	}

	private void threadFinished(AwfulTask<?> threadTask) {
		currentTask = null;
		startNextThread();
	}
	private boolean isThreadQueued(int targetId, int targetPage) {
		for(AwfulTask<?> at : threadPool){
			if(at.getId()== targetId && at.getPage() == targetPage){
				return true;
			}
		}
		if(currentTask != null && currentTask.getId()== targetId && currentTask.getPage() == targetPage){
			return true;
		}
		return false;
	}
	
	public boolean isLoggedIn(){
		return loggedIn;
	}
	
	//basic local-binding stuff.
	private final IBinder bindServ = new AwfulBinder();
	public class AwfulBinder extends Binder{
		public AwfulService getService() {
            return AwfulService.this;
        }
	};
	@Override
	public IBinder onBind(Intent arg0) {
		return bindServ;
	}
	
	private abstract class AwfulTask<T> extends AsyncTask<Void, Void, T>{
		protected int mId = 0;
		protected int mPage = 1;
		public int getId(){
			return mId;
		}
		public int getPage(){
			return mPage;
		}
	}
	
	private class FetchThreadTask extends AwfulTask<AwfulThread> {
		private AwfulThread thread;
		public FetchThreadTask(int id, int page) {
			mId = id;
			mPage = page;
			thread = (AwfulThread) db.get("threadid="+mId);
			if(thread == null){
				thread = new AwfulThread(mId);
				db.put("threadid="+mId, thread);
			}
		}

        public void onPreExecute() {
        }

        public AwfulThread doInBackground(Void... vParams) {
            if (!isCancelled() && thread != null) {
                try {
                	thread.getThreadPosts(mPage);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }

            return thread;
        }

        public void onPostExecute(AwfulThread aResult) {
            if (!isCancelled() && thread != null) {
            	sendBroadcast(new Intent(Constants.DATA_UPDATE_BROADCAST).putExtra(Constants.DATA_UPDATE_URL, thread.getID()));
            }
            threadFinished(this);
        }
    }

    private class FetchForumThreadsTask extends AwfulTask<ArrayList<AwfulThread>> {
		private AwfulForum mForum;
		private ArrayList<AwfulForum> newSubforums;
		@Override
		public String toString(){
			Log.e(TAG, "forumtask ToString?");
			return mId+" "+mPage;
		}
		
		public FetchForumThreadsTask(int forumID, int aPage) {
			mPage = aPage;
			mId = forumID;
			mForum = (AwfulForum) db.get("forumid="+forumID);
		}

        public void onPreExecute() {
        	
        }

        public ArrayList<AwfulThread> doInBackground(Void... vParams) {
            ArrayList<AwfulThread> result = new ArrayList<AwfulThread>();
            if(mForum == null){
            	mForum = new AwfulForum(mId);
            	db.put("forumid="+mId, mForum);
            	//Log.e(TAG, "Forum Not Found ID: "+mForumID);
            }
            if (!isCancelled() && !(mForum == null || mForum.getForumId() == null || mForum.getForumId().equals(""))) {
                try {
                    TagNode threads = null;
                    if(mForum.getID()<0){
                    	threads = AwfulThread.getUserCPThreads(mPage);
                    }else{
                    	threads = AwfulThread.getForumThreads(mForum.getForumId(), mPage);
                        if(mForum.getTitle() == null){
                        	mForum.setTitle(AwfulForum.parseTitle(threads));
                        }
                        newSubforums = AwfulThread.parseSubforums(threads);
                    }
                    result = AwfulThread.parseForumThreads(threads);
                    mForum.parsePageNumbers(threads);
                    //Log.i(TAG, "Last Page: " +mForum.getLastPage());
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }
            return result;
        }

        public void onPostExecute(ArrayList<AwfulThread> aResult) {
            if (!isCancelled()) {
            	for(AwfulThread at: aResult){
            		AwfulThread old = (AwfulThread) db.get("threadid="+at.getThreadId());
            		if(old==null){
            			//Log.e(TAG,"Added thread: "+at.getThreadId());
            			db.put("threadid="+at.getThreadId(), at);
            		}else{//use this section to copy any data you want to update during a forum refresh. ie: post count, bookmark status, ect
            			old.setUnreadCount(at.getUnreadCount());
            			old.setTotalCount(at.getTotalCount());
            			old.setBookmarked(at.isBookmarked());
            		}
            	}
            	mForum.setThreadPage(mPage, aResult);
            	if(newSubforums != null){//k, we have parsed forums
        			for(AwfulForum af : newSubforums){
        				AwfulForum other = (AwfulForum) db.get("forumid="+af.getID());
        				if(other != null){//was the same forum already in the DB?
        					boolean found = false;
        					for(AwfulForum gg : mForum.getSubforums()){
        						if(other == gg){//ok, it was, let's see if it matches one already on the local forum list
        							found = true;
        							gg.setSubtext(af.getSubtext());//since forum index doesn't have subforum subtexts
        						}
        					}
        					if(!found){//its not? welp, add it.
        						mForum.addSubforum(af);
        					}
        				}else{//not in db? add it
        					db.put("forumid="+af.getID(), af);
        					mForum.addSubforum(af);
        				}
        			}
        			
            	}
            	sendBroadcast(new Intent(Constants.DATA_UPDATE_BROADCAST).putExtra(Constants.DATA_UPDATE_URL, mId));
            }
            threadFinished(this);
        }
    }
    
	public void fetchThread(int id, int page) {
		if(isThreadQueued(id,page)){
			Log.e(TAG, "dupe fetchThread "+id);
			return;
		}
		Log.e(TAG, "fetchThread "+id);
		queueThread(new FetchThreadTask(id, page));
	}
	public void fetchForum(int id, int page) {
		if(isThreadQueued(id,page)){
			Log.e(TAG, "dupe fetchForum "+id);
			return;
		}
		Log.e(TAG, "fetchForum "+id);
		if(id == 0){
			queueThread(new LoadForumsTask());
		}else{
			queueThread(new FetchForumThreadsTask(id, page));
		}
	}
	

	public AwfulForum getForum(int currentId) {
		Log.e(TAG, "getForum "+currentId);
		return (AwfulForum) db.get("forumid="+currentId);
	}
	public AwfulThread getThread(int currentId) {
		Log.e(TAG, "getThread "+currentId);
		return (AwfulThread) db.get("threadid="+currentId);
	}
	
	public void toggleBookmark(int threadId){
		queueThread(new BookmarkToggleTask(threadId));
	}
	
	private class BookmarkToggleTask extends AwfulTask<Void> {
		private boolean removeBookmark;
        public BookmarkToggleTask(int threadId) {
        	mId = threadId;
        	AwfulThread th = (AwfulThread) db.get("threadid="+mId);
        	removeBookmark = (th != null ? th.isBookmarked() : true);
		}
		public Void doInBackground(Void... aParams) {
            if (!isCancelled()) {
            	HashMap<String, String> params = new HashMap<String, String>();
                params.put(Constants.PARAM_THREAD_ID, Integer.toString(mId));
                if(removeBookmark){
                	params.put(Constants.PARAM_ACTION, "remove");
                }else{
                	params.put(Constants.PARAM_ACTION, "add");
                }

                try {
                    NetworkUtils.post(Constants.FUNCTION_BOOKMARK, params);
                } catch (Exception e) {
                    Log.i(TAG, e.toString());
                }
            }
            return null;
        }

        public void onPostExecute(Void aResult) {
            sendBroadcast(new Intent(Constants.DATA_UPDATE_BROADCAST).putExtra(Constants.DATA_UPDATE_URL, mId));
            threadFinished(this);
        }
    }
	
	private class LoadForumsTask extends AwfulTask<ArrayList<AwfulForum>> {
        public ArrayList<AwfulForum> doInBackground(Void... aParams) {
            ArrayList<AwfulForum> result = new ArrayList<AwfulForum>();
            if (!isCancelled()) {
                try {
                    result = AwfulForum.getForumsFromRemote();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }
            return result;
        }

        public void onPostExecute(ArrayList<AwfulForum> aResult) {
            if (!isCancelled()) {
            	for(AwfulForum af : aResult){
            		if(db.get("forumid="+af.getForumId()) == null){
            			db.put("forumid="+af.getForumId(), af);
            		}
            	}
            	sendBroadcast(new Intent(Constants.DATA_UPDATE_BROADCAST).putExtra(Constants.DATA_UPDATE_URL, 0));
            }
            threadFinished(this);
        }
    }
}
