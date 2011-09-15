package com.ferg.awful.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

import org.htmlcleaner.TagNode;

import com.commonsware.cwac.bus.AbstractBus.Receiver;
import com.commonsware.cwac.cache.SimpleWebImageCache;
import com.ferg.awful.R;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulMessage;
import com.ferg.awful.thread.AwfulPagedItem;
import com.ferg.awful.thread.AwfulPost;
import com.ferg.awful.thread.AwfulPrivateMessages;
import com.ferg.awful.thread.AwfulThread;
import com.ferg.awful.thumbnail.ThumbnailBus;
import com.ferg.awful.thumbnail.ThumbnailMessage;

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
	private ThumbnailBus avatarBus=new ThumbnailBus();
	private SimpleWebImageCache<ThumbnailBus, ThumbnailMessage> avatarCache=new SimpleWebImageCache<ThumbnailBus, ThumbnailMessage>(null, null, 101, avatarBus);
	private LinkedList<Receiver<ThumbnailMessage>> registeredAvatarClients = new LinkedList<Receiver<ThumbnailMessage>>();
	private AwfulPreferences mPrefs;
	
	public void onCreate(){
		loggedIn = NetworkUtils.restoreLoginCookies(this);
        mPrefs = new AwfulPreferences(this);
		Log.e(TAG, "Service started.");
	}

	public void onDestroy(){
		Log.e(TAG, "Service onDestroy.");
		if(currentTask != null){
			currentTask.cancel(true);
		}
		threadPool.clear();
		while(registeredAvatarClients.peek() != null){
			avatarCache.getBus().unregister(registeredAvatarClients.poll());
		}
		mPrefs.unRegisterListener();
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
	

	public void registerForAvatarCache(String filter, Receiver<ThumbnailMessage> receiver) {
		avatarCache.getBus().register(filter, receiver);
		registeredAvatarClients.add(receiver);
	}

	public void unregisterForAvatarCache(Receiver<ThumbnailMessage> receiver) {
		avatarCache.getBus().unregister(receiver);
		registeredAvatarClients.remove(receiver);
	}
	
	public SimpleWebImageCache<ThumbnailBus, ThumbnailMessage> getAvatarCache() {
		return avatarCache;
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
	
	
    /**
     * Starts an asynchronous process that loads a thread's data for the specified page.
     * A broadcast will be sent once the data is processed. It will use Constants.DATA_UPDATE_BROADCAST and an integer extra DATA_UPDATE_ID_EXTRA.
     * @param id Thread ID number
     * @param page Page number
     */
	public void fetchThread(int id, int page) {
		if(isThreadQueued(id,page)){
			Log.e(TAG, "dupe fetchThread "+id);
			return;
		}
		Log.e(TAG, "fetchThread "+id);
		queueThread(new FetchThreadTask(id, page));
	}
	/**
     * Starts an asynchronous process that loads a forum's data (threads/subforums) for the specified page.
     * A broadcast will be sent once the data is processed. It will use Constants.DATA_UPDATE_BROADCAST and an integer extra DATA_UPDATE_ID_EXTRA.
     * @param id Forum ID number
     * @param page Page number
     */
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
	/**
	 * Queues a background task to mark a specific post as the last read. 
	 * Changes won't update locally until the next time the thread is parsed.
	 * Do not refresh immediately after calling this, or the changes will be lost.
	 * @param post The selected post.
	 */
	public void MarkLastRead(String aLastReadUrl){
		queueThread(new MarkLastReadTask(aLastReadUrl));
	}
	
	public AwfulPagedItem getItem(String string) {
		Log.e(TAG, "getItem "+string);
		return (AwfulForum) db.get(string);
	}
	
	/**
	 * Pulls an AwfulForum instance for the ID specified, or null if none exist yet.
	 * The forum's threads may not have been populated yet.
	 * @param currentId
	 * @return Forum or null if none exist.
	 */
	public AwfulForum getForum(int currentId) {
		Log.e(TAG, "getForum "+currentId);
		return (AwfulForum) db.get("forumid="+currentId);
	}
	/**
	 * Pulls an AwfulThread instance for the ID specified, or null if none exist yet.
	 * @param currentId
	 * @return Thread or null if none exist.
	 */
	public AwfulThread getThread(int currentId) {
		Log.e(TAG, "getThread "+currentId);
		return (AwfulThread) db.get("threadid="+currentId);
	}
	/**
	 * Toggles the bookmark status for the selected thread.
	 * @param threadId
	 */
	public void toggleBookmark(int threadId){
		queueThread(new BookmarkToggleTask(threadId));
	}
	
	/**
	 * Removed the unread-post-count for the specified thread (same as clicking the X).
	 * @param id Thread Id
	 */
	public void markThreadUnread(int id) {
		queueThread(new MarkThreadUnreadTask(id));
	}
	
	public void fetchPrivateMessages() {
		queueThread(new FetchPrivateMessageList());
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
		protected void sendUpdate(boolean success){
        	sendBroadcast(new Intent(Constants.DATA_UPDATE_BROADCAST).putExtra(
        			Constants.DATA_UPDATE_ID_EXTRA, mId).putExtra(
        					Constants.DATA_UPDATE_PAGE_EXTRA, mPage).putExtra(
        							Constants.DATA_UPDATE_STATUS_EXTRA, success));
		}
	}
	
	private class FetchThreadTask extends AwfulTask<Boolean> {
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

        public Boolean doInBackground(Void... vParams) {
        	boolean status = false;
            if (!isCancelled() && thread != null) {
                try {
                	thread.getThreadPosts(mPage, mPrefs.postPerPage);
                	status = true;
                } catch (Exception e) {
                	status = false;
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }

            return status;
        }

        public void onPostExecute(Boolean aResult) {
        	sendUpdate(aResult);
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
            if(mForum == null){
            	mForum = new AwfulForum(mId);
            	db.put("forumid="+mId, mForum);
            }
		}

        public ArrayList<AwfulThread> doInBackground(Void... vParams) {
            ArrayList<AwfulThread> result = null;
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
                    result = AwfulThread.parseForumThreads(threads, mPrefs.postPerPage);
                    mForum.parsePageNumbers(threads);
                    //Log.i(TAG, "Last Page: " +mForum.getLastPage());
                } catch (Exception e) {
                	result = null;
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }
            return result;
        }

        public void onPostExecute(ArrayList<AwfulThread> aResult) {
            if (!isCancelled() && aResult != null) {
            	for(AwfulThread at: aResult){
            		AwfulThread old = (AwfulThread) db.get("threadid="+at.getThreadId());
            		if(old==null){
            			//Log.e(TAG,"Added thread: "+at.getThreadId());
            			db.put("threadid="+at.getThreadId(), at);
            		}else{//use this section to copy any data you want to update during a forum refresh. ie: post count, bookmark status, ect
            			old.setUnreadCount(at.getUnreadCount());
            			old.setTotalCount(at.getTotalCount(), mPrefs.postPerPage);
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
            	sendUpdate(true);
            }else{
            	sendUpdate(false);
            }
            threadFinished(this);
        }
    }
	
	private class BookmarkToggleTask extends AwfulTask<Boolean> {
		private boolean removeBookmark;
        public BookmarkToggleTask(int threadId) {
        	mId = threadId;
        	AwfulThread th = (AwfulThread) db.get("threadid="+mId);
        	removeBookmark = (th != null ? th.isBookmarked() : true);
		}
		public Boolean doInBackground(Void... aParams) {
			boolean status = false;
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
                    status = true;
                } catch (Exception e) {
                	status = false;
                    Log.i(TAG, e.toString());
                }
            }
            return status;
        }

        public void onPostExecute(Boolean aResult) {
            sendUpdate(aResult);
            threadFinished(this);
        }
    }
	
	private class MarkThreadUnreadTask extends AwfulTask<Boolean> {
        public MarkThreadUnreadTask(int threadId) {
        	mId = threadId;
        	AwfulThread th = (AwfulThread) db.get("threadid="+mId);
        	th.setUnreadCount(-1);
		}
		public Boolean doInBackground(Void... aParams) {
			boolean status = false;
            if (!isCancelled()) {
            	HashMap<String, String> params = new HashMap<String, String>();
                params.put(Constants.PARAM_THREAD_ID, Integer.toString(mId));
                params.put(Constants.PARAM_ACTION, "resetseen");

                try {
                    NetworkUtils.post(Constants.FUNCTION_THREAD, params);
                    status = true;
                } catch (Exception e) {
                	status = false;
                    Log.i(TAG, e.toString());
                }
            }
            return status;
        }

        public void onPostExecute(Boolean aResult) {
            sendUpdate(aResult);
            threadFinished(this);
        }
    }
	
	private class LoadForumsTask extends AwfulTask<ArrayList<AwfulForum>> {
        public ArrayList<AwfulForum> doInBackground(Void... aParams) {
            ArrayList<AwfulForum> result = null;
            if (!isCancelled()) {
                try {
                    result = AwfulForum.getForumsFromRemote();
                } catch (Exception e) {
                	result = null;
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }
            return result;
        }

        public void onPostExecute(ArrayList<AwfulForum> aResult) {
            if (!isCancelled() && aResult != null) {
            	for(AwfulForum af : aResult){
            		if(db.get("forumid="+af.getForumId()) == null){
            			db.put("forumid="+af.getForumId(), af);
            		}
            	}
            	sendUpdate(true);
            }else{
            	sendUpdate(false);
            }
            threadFinished(this);
        }
    }
	private class MarkLastReadTask extends AwfulTask<Void> {
		private String lrUrl;
        public MarkLastReadTask(String lastReadUrl){
        	lrUrl = lastReadUrl;
        	mId = -99;
        }

        public Void doInBackground(Void... aParams) {
            if (!isCancelled()) {
                try {
                    NetworkUtils.get(Constants.BASE_URL+ lrUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }
            return null;
        }

        public void onPostExecute(Void aResult) {
            threadFinished(this);
        }
    }
	
	private class FetchPrivateMessageList extends AwfulTask<AwfulPrivateMessages>{
		private AwfulPrivateMessages pml;
		public FetchPrivateMessageList(){
			pml = (AwfulPrivateMessages) db.get("pm 0");
			if(pml == null){
				pml = new AwfulPrivateMessages();
			}
			db.put("pm 1", pml);
		}

		@Override
		protected AwfulPrivateMessages doInBackground(Void... params) {
			try {
				TagNode pmData = NetworkUtils.get(Constants.FUNCTION_PRIVATE_MESSAGE);
				ArrayList<AwfulMessage> mList = AwfulMessage.processMessageList(pmData);
				pml.setMessageList(mList);
			} catch (Exception e) {
				Log.e(TAG,"PM Load Failure: "+e.getLocalizedMessage());
			}
			return null;
		}
		
		public void onPostExecute(AwfulPrivateMessages results){
			sendUpdate(true);
		}
		
	}
}
