package com.ferg.awful.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlcleaner.TagNode;

import com.ferg.awful.R;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulMessage;
import com.ferg.awful.thread.AwfulPagedItem;
import com.ferg.awful.thread.AwfulPrivateMessages;
import com.ferg.awful.thread.AwfulThread;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class AwfulService extends Service {
	private static final String TAG = "AwfulService";
	private HashMap<String, AwfulPagedItem> db = new HashMap<String, AwfulPagedItem>();
	private boolean loggedIn;
	private AwfulTask<?> currentTask;
	private Stack<AwfulTask<?>> threadPool = new Stack<AwfulTask<?>>();
	private AwfulPreferences mPrefs;
	
	public void onCreate(){
		loggedIn = NetworkUtils.restoreLoginCookies(this);
        mPrefs = new AwfulPreferences(this);
		Log.v(TAG, "Service started.");
	}

	public void onDestroy(){
		Log.v(TAG, "Service onDestroy.");
		if(currentTask != null){
			currentTask.cancel(true);
		}
		threadPool.clear();
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
			Log.w(TAG, "dupe fetchThread "+id);
			return;
		}
		Log.v(TAG, "fetchThread "+id);
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
			Log.w(TAG, "dupe fetchForum "+id);
			return;
		}
		Log.v(TAG, "fetchForum "+id);
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
	public void MarkLastRead(String aLastReadUrl, int id, int page){
		queueThread(new MarkLastReadTask(aLastReadUrl, id, page));
	}
	
	public AwfulPagedItem getItem(String string) {
		return db.get(string);
	}
	
	public AwfulMessage getMessage(int pmId) {
		return (AwfulMessage) db.get(Constants.PRIVATE_MESSAGE+pmId);
	}
	
	/**
	 * Pulls an AwfulForum instance for the ID specified, or null if none exist yet.
	 * The forum's threads may not have been populated yet.
	 * @param currentId
	 * @return Forum or null if none exist.
	 */
	public AwfulForum getForum(int currentId) {
		Log.v(TAG, "getForum "+currentId);
		return (AwfulForum) db.get("forumid="+currentId);
	}
	/**
	 * Pulls an AwfulThread instance for the ID specified, or null if none exist yet.
	 * @param currentId
	 * @return Thread or null if none exist.
	 */
	public AwfulThread getThread(int currentId) {
		Log.v(TAG, "getThread "+currentId);
		return (AwfulThread) db.get("threadid="+currentId);
	}
	/**
	 * Toggles the bookmark status for the selected thread.
	 * @param threadId
	 */
	public void toggleBookmark(int threadId){
		queueThread(new BookmarkToggleTask(threadId));
	}
	
	public void rateThread(int vote, int threadId){
		queueThread(new VotingTask(vote, threadId));
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
	
	public void fetchPrivateMessage(int id){
		queueThread(new FetchPrivateMessageTask(id));
	}
	
	public void sendPM(String recipient, int prevMsgId, String subject, String content){
		queueThread(new SendMessageTask(recipient, prevMsgId, subject, content));
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
		protected void sendUpdate(boolean success, Bundle extras){
			Intent bcast = new Intent(Constants.DATA_UPDATE_BROADCAST).putExtra(
        			Constants.DATA_UPDATE_ID_EXTRA, mId).putExtra(
        					Constants.DATA_UPDATE_PAGE_EXTRA, mPage).putExtra(
        							Constants.DATA_UPDATE_STATUS_EXTRA, success).putExtra(Constants.EXTRA_BUNDLE, extras);
        	sendBroadcast(bcast);
		}
		protected void sendUpdate(boolean success){
        	sendUpdate(success, null);
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
                	thread.getThreadPosts(mPage, mPrefs.postPerPage, mPrefs);
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
        						if(other.getID() == gg.getID()){//ok, it was, let's see if it matches one already on the local forum list
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
	
	private class VotingTask extends AwfulTask<Boolean> {
		private int mRating;
        public VotingTask(int rating, int threadId) {
        	mRating = rating;
        	mId = threadId;
		}
		public Boolean doInBackground(Void... aParams) {
			boolean status = false;
            if (!isCancelled()) {
            	
				HashMap<String, String> params = new HashMap<String, String>();
				params.put(Constants.PARAM_THREAD_ID, String.valueOf(mId));
				params.put(Constants.PARAM_VOTE, String.valueOf(mRating+1));

                try {
                	NetworkUtils.post(Constants.FUNCTION_RATE_THREAD, params);
                    status = true;
                } catch (Exception e) {
                	status = false;
                    Log.i(TAG, e.toString());
                }
            }
            return status;
        }

        public void onPostExecute(Boolean aResult) {
        	if(aResult){
				Toast successToast = Toast.makeText(getApplicationContext(), String.format(getString(R.string.vote_succeeded), mRating+1),  Toast.LENGTH_LONG);
				successToast.show();
        	}else{
				Toast errorToast = Toast.makeText(getApplicationContext(), R.string.vote_failed, Toast.LENGTH_LONG);
				errorToast.show();
        	}
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
                    NetworkUtils.post(Constants.FUNCTION_THREAD, params);//TODO parse resulting posts to update unread status
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
	/**
	 * Parses forum index and returns list of visible forums.
	 * Sends the following extra data bundle (if successfully parsed): username (string), unread_pm (int, unread PM count, -1 indicates failed parse)
	 * @author Matt
	 *
	 */
	private class LoadForumsTask extends AwfulTask<ArrayList<AwfulForum>> {
		private Bundle parsedExtras = null;
        public ArrayList<AwfulForum> doInBackground(Void... aParams) {
            ArrayList<AwfulForum> result = null;
            if (!isCancelled()) {
                try {
                    TagNode response = NetworkUtils.get(Constants.BASE_URL);
                    result = AwfulForum.getForumsFromRemote(response);
                    TagNode[] pmBlock = response.getElementsByAttValue("id", "pm", true, true);
                    try{
	                    if(pmBlock.length >0){
	                    	TagNode[] bolded = pmBlock[0].getElementsByName("b", true);
	                    	if(bolded.length > 1){
	                    		String name = bolded[0].getText().toString().split("'")[0];
	                    		String unread = bolded[1].getText().toString();
	                    		Pattern findUnread = Pattern.compile("(\\d+)\\s+unread");
	                    		Matcher matchUnread = findUnread.matcher(unread);
	                    		int unreadCount = -1;
	                    		if(matchUnread.find()){
	                    			unreadCount = Integer.parseInt(matchUnread.group(1));
	                    		}
	                        	Log.v(TAG,"text: "+name+" - "+unreadCount);
	                        	parsedExtras = new Bundle();
	                        	parsedExtras.putString("username", name);
	                        	parsedExtras.putInt("unread_pm", unreadCount);
	                        	if(name != null && name.length() > 0){
	                        		mPrefs.setUsername(name);
	                        	}
	                    	}
	                    }
                    }catch(Exception e){
                    	//this chunk is optional, no need to fail everything if it doens't work out.
                    }
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
            	sendUpdate(true, parsedExtras);
            }else{
            	sendUpdate(false);
            }
            threadFinished(this);
        }
    }
	private class MarkLastReadTask extends AwfulTask<Void> {
		private String lrUrl;
        public MarkLastReadTask(String lastReadUrl, int threadid, int page){
        	lrUrl = lastReadUrl;
        	mId = threadid;
        	mPage = page;
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
        	Bundle bund = new Bundle();
        	bund.putBoolean("marklastread", true);
			sendUpdate(true, bund);
            threadFinished(this);
        }
    }
	
	private class FetchPrivateMessageList extends AwfulTask<ArrayList<AwfulMessage>>{
		private AwfulPrivateMessages pml;
		public FetchPrivateMessageList(){
			mId = Constants.PRIVATE_MESSAGE_THREAD;
			pml = (AwfulPrivateMessages) db.get(Constants.PRIVATE_MESSAGE);
			if(pml == null){
				pml = new AwfulPrivateMessages();
			}
		}

		@Override
		protected ArrayList<AwfulMessage> doInBackground(Void... params) {
			ArrayList<AwfulMessage> pmList = null;
			try {
				TagNode pmData = NetworkUtils.get(Constants.FUNCTION_PRIVATE_MESSAGE);
				pmList = AwfulMessage.processMessageList(pmData);
			} catch (Exception e) {
				pmList = null;
				Log.e(TAG,"PM Load Failure: "+Log.getStackTraceString(e));
			}
			return pmList;
		}
		
		public void onPostExecute(ArrayList<AwfulMessage> results){
			if(results != null){
				for(AwfulMessage m : results){
					AwfulMessage old = (AwfulMessage) db.get(Constants.PRIVATE_MESSAGE+m.getID());
					if(old == null){
						db.put(Constants.PRIVATE_MESSAGE+m.getID(), m);
					}else{
						old.setTitle(m.getTitle());
					}
				}
				pml.setMessageList(results);
				db.put(Constants.PRIVATE_MESSAGE+Constants.PRIVATE_MESSAGE_THREAD, pml);
				
				sendUpdate(true);
			}else{
				sendUpdate(false);
			}
            threadFinished(this);
		}
		
	}
	
	private class FetchPrivateMessageTask extends AwfulTask<AwfulMessage>{
		private AwfulMessage pm;
		public FetchPrivateMessageTask(int id){
			mId = id;
			pm = (AwfulMessage) db.get(Constants.PRIVATE_MESSAGE+mId);
			if(pm == null){
				pm = new AwfulMessage(mId);
				db.put(Constants.PRIVATE_MESSAGE+pm.getID(), pm);
			}
		}

		@Override
		protected AwfulMessage doInBackground(Void... params) {
			try {
				HashMap<String, String> para = new HashMap<String, String>();
                para.put(Constants.PARAM_PRIVATE_MESSAGE_ID, Integer.toString(mId));
                para.put(Constants.PARAM_ACTION, "show");
				TagNode pmData = NetworkUtils.get(Constants.FUNCTION_PRIVATE_MESSAGE, para);
				AwfulMessage.processMessage(pmData, pm);
				//finished loading display message, notify UI
				publishProgress((Void[]) null);
				//after notifying, we can preload reply window text
                para.put(Constants.PARAM_ACTION, "newmessage");
				TagNode pmReplyData = NetworkUtils.get(Constants.FUNCTION_PRIVATE_MESSAGE, para);
				AwfulMessage.processReplyMessage(pmReplyData, pm);
				pm.setLoaded(true);
				Log.v(TAG,"Fetched msg: "+mId);
			} catch (Exception e) {
				pm = null;
				Log.e(TAG,"PM Load Failure: "+Log.getStackTraceString(e));
			}
			return pm;
		}
		
		protected void onProgressUpdate(Void... progress) {
			//message loaded, update UI and return to preload reply section.
			if(pm != null){
				sendUpdate(true);
			}else{
				sendUpdate(false);
			}
	     }
		
		public void onPostExecute(AwfulMessage results){
			if(results != null){
				sendUpdate(true);
			}else{
				sendUpdate(false);
			}
            threadFinished(this);
		}
		
	}
	
	private class SendMessageTask extends AwfulTask<Boolean>{
		private String mRecipient;
		private String mTitle;
		private String mContent;
		public SendMessageTask(String recipient, int prevMsgId, String title, String content){
			mId = prevMsgId;
			mContent = content;
			mRecipient = recipient;
			mTitle = title;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				HashMap<String, String> para = new HashMap<String, String>();
                para.put(Constants.PARAM_PRIVATE_MESSAGE_ID, Integer.toString(mId));
                para.put(Constants.PARAM_ACTION, Constants.ACTION_DOSEND);
                para.put(Constants.DESTINATION_TOUSER, mRecipient);
                para.put(Constants.PARAM_TITLE, mTitle);
                //TODO move to constants
                if(mId>0){
                	para.put("prevmessageid", Integer.toString(mId));
                }
                para.put(Constants.PARAM_PARSEURL, Constants.YES);
                para.put("savecopy", "yes");
                para.put("iconid", "0");
                para.put(Constants.PARAM_MESSAGE, mContent);
				TagNode result = NetworkUtils.post(Constants.FUNCTION_PRIVATE_MESSAGE, para);
			} catch (Exception e) {
				Log.e(TAG,"PM Send Failure: "+Log.getStackTraceString(e));
				return false;
			}
			return true;
		}
		
		public void onPostExecute(Boolean results){
			if(results.booleanValue()){
				Bundle b = new Bundle();
				b.putBoolean(Constants.PARAM_MESSAGE, true);
				sendUpdate(true, b);
			}else{
				sendUpdate(false);
			}
            threadFinished(this);
		}
		
	}
}
