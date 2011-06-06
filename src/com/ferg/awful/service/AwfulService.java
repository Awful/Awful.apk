package com.ferg.awful.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;

import org.htmlcleaner.TagNode;

import com.ferg.awful.AwfulLoginActivity;
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
import android.util.Log;

public class AwfulService extends Service {
	private static final String TAG = "AwfulService";
	private HashMap<String, AwfulPagedItem> db = new HashMap<String, AwfulPagedItem>();
	private boolean loggedIn;
	private WeakHashMap<AsyncTask, String> threadPool = new WeakHashMap<AsyncTask, String>();
	
	public void onCreate(){
		loggedIn = NetworkUtils.restoreLoginCookies(this);
		Log.e(TAG, "Service started.");
	}

	public void onDestroy(){
		Log.e(TAG, "Service onDestroy.");
		for(AsyncTask at : threadPool.keySet()){
			at.cancel(true);
		}
		threadPool.clear();
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
	
	private class FetchThreadTask extends AsyncTask<AwfulThread, Void, AwfulThread> {

		private int threadID;
		private int pageNum;
		private AwfulThread thread;
		public FetchThreadTask(int id, int page) {
			threadID = id;
			pageNum = page;
			thread = (AwfulThread) db.get("threadid="+threadID);
			if(thread == null){
				//Log.e(TAG,"thread not in DB. id: "+threadID);
				thread = new AwfulThread(threadID);
				db.put("threadid="+threadID, thread);
			}
		}

        public void onPreExecute() {
        }

        public AwfulThread doInBackground(AwfulThread... aParams) {
            if (!isCancelled() && thread != null) {
                try {
                    // We set the unread count to -1 if the user has never
                    // visited that thread before
                	thread.getThreadPosts(pageNum);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }

            return thread;
        }

        public void onPostExecute(AwfulThread aResult) {
            if (!isCancelled() && thread != null) {
            	//db.put("threadid="+threadID, aResult);//we should already have this in the db

                // If we're loading a thread from ChromeToPhone we have to set the 
                // title now
                //if (mTitle.getText() == null || mTitle.getText().length() == 0) {
                //    mTitle.setText(Html.fromHtml(mThread.getTitle()));
               // }
            	sendBroadcast(new Intent(Constants.DATA_UPDATE_BROADCAST).putExtra(Constants.DATA_UPDATE_URL, thread.getID()));
            }
            threadPool.remove(this);
        }
    }

    private class FetchForumThreadsTask extends AsyncTask<Integer, Void, ArrayList<AwfulThread>> {
		private int mPage;
		private AwfulForum mForum;
		private int mForumID;
		private ArrayList<AwfulForum> newSubforums;//it's 2:21am, time to hack shit together
		
		public FetchForumThreadsTask(int forumID, int aPage) {
			mPage = aPage;
			mForumID = forumID;
			mForum = (AwfulForum) db.get("forumid="+forumID);
		}

        public void onPreExecute() {
        	
        }

        public ArrayList<AwfulThread> doInBackground(Integer... aParams) {
            ArrayList<AwfulThread> result = new ArrayList<AwfulThread>();
            if(mForum == null){
            	mForum = new AwfulForum(mForumID);
            	db.put("forumid="+mForumID, mForum);
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
            	sendBroadcast(new Intent(Constants.DATA_UPDATE_BROADCAST).putExtra(Constants.DATA_UPDATE_URL, mForumID));
            }
            threadPool.remove(this);
        }
    }
    
	public void fetchThread(int id, int page) {
		if(threadPool.containsValue(id+" "+page)){
			Log.e(TAG, "dupe fetchThread "+id);
			return;
		}
		Log.e(TAG, "fetchThread "+id);
		threadPool.put(new FetchThreadTask(id, page).execute(), id+" "+page);
	}
	public void fetchForum(int id, int page) {
		if(threadPool.containsValue(id+" "+page)){
			Log.e(TAG, "dupe fetchForum "+id);
			return;
		}
		Log.e(TAG, "fetchForum "+id);
		if(id == 0){
			threadPool.put(new LoadForumsTask().execute(),id+" "+page);
		}else{
			threadPool.put(new FetchForumThreadsTask(id, page).execute(), id+" "+page);
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
	
	public void toggleBookmark(int threadId, boolean remove){
		threadPool.put(new BookmarkToggleTask(remove).execute(threadId), threadId+"");
	}
	
	private class BookmarkToggleTask extends AsyncTask<Integer, Void, Void> {
		boolean removeBookmark;
        public BookmarkToggleTask(boolean remove) {
        	removeBookmark = remove;
		}

		public Void doInBackground(Integer... aParams) {
            if (!isCancelled()) {
            	HashMap<String, String> params = new HashMap<String, String>();
                params.put(Constants.PARAM_THREAD_ID, aParams[0].toString());
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
            sendBroadcast(new Intent(Constants.DATA_UPDATE_BROADCAST).putExtra(Constants.DATA_UPDATE_URL, 0));
            threadPool.remove(this);
        }
    }
	
	private class LoadForumsTask extends AsyncTask<Void, Void, ArrayList<AwfulForum>> {
        public void onPreExecute() {
        }

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
            threadPool.remove(this);
        }
    }
}
