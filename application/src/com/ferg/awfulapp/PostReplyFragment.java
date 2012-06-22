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

package com.ferg.awfulapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;

public class PostReplyFragment extends AwfulFragment implements OnClickListener {
    private static final String TAG = "PostReplyActivity";

    public static final int RESULT_POSTED = 1;

    private Bundle mExtras;
    private EditText mMessage;
    private ProgressDialog mDialog;

    private int mThreadId;
    private int mPostId;
    private int mReplyType;
    private String mThreadTitle;
    private boolean sendSuccessful = false;
    private boolean threadClosed = false;
    private String originalReplyData = "";
    
    private ReplyCallback mReplyDataCallback = new ReplyCallback(mHandler);
    private ThreadDataCallback mThreadLoaderCallback;
    private ThreadContentObserver mThreadObserver = new ThreadContentObserver(mHandler);

    public static PostReplyFragment newInstance(Bundle aArguments) {
        PostReplyFragment fragment = new PostReplyFragment();

        fragment.setArguments(aArguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        new Thread(new Runnable() {
            public void run() {
                GoogleAnalyticsTracker.getInstance().trackPageView("/PostReplyFragment");
                GoogleAnalyticsTracker.getInstance().dispatch();
            }
        }).start();
        setHasOptionsMenu(true);
        mThreadLoaderCallback = new ThreadDataCallback();
    }
    
    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        View result = aInflater.inflate(R.layout.post_reply, aContainer, false);

        mMessage = (EditText) result.findViewById(R.id.post_message);
        //result.findViewById(R.id.bbcode).setOnClickListener(this);
        //result.findViewById(R.id.emotes).setOnClickListener(this);

        return result;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

        setRetainInstance(true);

        mMessage.setBackgroundColor(mPrefs.postBackgroundColor);
        mMessage.setTextColor(mPrefs.postFontColor);
        
        mExtras = getExtras();
        mReplyType = mExtras.getInt(Constants.EDITING, getArguments().getInt(Constants.EDITING, -1));
        mPostId = mExtras.getInt(Constants.POST_ID, getArguments().getInt(Constants.POST_ID, -1));
        mThreadId = mExtras.getInt(Constants.THREAD_ID, getArguments().getInt(Constants.THREAD_ID, -1));
        if(mReplyType <0 || mThreadId <0 || (mReplyType != AwfulMessage.TYPE_NEW_REPLY && mPostId < 0)){
        	Log.e(TAG,"MISSING ARGUMENTS!");
        	getActivity().finish();
        }
        
		getActivity().getSupportLoaderManager().restartLoader(Constants.REPLY_LOADER_ID, null, mReplyDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulMessage.CONTENT_URI_REPLY, true, mReplyDataCallback);
        refreshThreadInfo();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
    
    private void leave(){
    	if(getAwfulActivity() != null){
    		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getApplicationWindowToken(), 0);
    		getAwfulActivity().fragmentClosing(this);
    	}
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        cleanupTasks();
    }
        
    @Override
    public void onStop() {
        super.onStop();
        autosave();
        cleanupTasks();
    }
    
    private void autosave(){
        if(!sendSuccessful){
        	if(mMessage.getText().toString().replaceAll("\\s", "").equalsIgnoreCase(originalReplyData.replaceAll("\\s", ""))){
        		Log.i(TAG, "Message unchanged, discarding.");
        		deleteReply();//if the reply is unchanged, throw it out.
        	}else{
        		Log.i(TAG, "Message Unsent, saving.");
        		saveReply();
        	}
        }
    }

	@Override
	public void loadingStarted(Message aMsg) {
		super.loadingStarted(aMsg);
	}
    
    @Override
	public void loadingFailed(Message aMsg) {
		super.loadingFailed(aMsg);
		if(mDialog != null){
			mDialog.dismiss();
			mDialog = null;
		}
    	if(aMsg.what == AwfulSyncService.MSG_SEND_POST){
			saveReply();
    		Toast.makeText(getActivity(), "Post Failed to Send! Message Saved...", Toast.LENGTH_LONG).show();
    	}
    	if(aMsg.what == AwfulSyncService.MSG_FETCH_POST_REPLY){
			Toast.makeText(getActivity(), "Reply Load Failed!", Toast.LENGTH_LONG).show();
    	}
	}

	@Override
	public void loadingSucceeded(Message aMsg) {
		super.loadingSucceeded(aMsg);
		if(mDialog != null){
			mDialog.dismiss();
			mDialog = null;
		}
    	if(aMsg.what == AwfulSyncService.MSG_FETCH_POST_REPLY){
    		refreshLoader();
    	}
    	if(aMsg.what == AwfulSyncService.MSG_SEND_POST){
    		sendSuccessful = true;
			Toast.makeText(getActivity(), getActivity().getString(R.string.post_sent), Toast.LENGTH_LONG).show();
			getActivity().setResult(RESULT_POSTED);
			leave();
    	}
    }

	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Log.d(TAG,"onCreateOptionsMenu");
            inflater.inflate(R.menu.post_reply, menu);
    }
    
    private class BBCodeFragment extends DialogFragment implements OnItemClickListener{
    	public String[] items = new String[]{
    			"Bold", "Italics", "Underline", "Strikeout", "URL", "Image", "Quote", "Spoiler", "Code"
    	};
		ListView mListView;
		@Override
		public View onCreateView(LayoutInflater inflater,
				ViewGroup container, Bundle savedInstanceState) {
			super.onCreateView(inflater, container, savedInstanceState);
			mListView = new ListView(getActivity());
			mListView.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.spinner_item, items));
			mListView.setOnItemClickListener(this);
			return mListView;
		}
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			switch((int)arg3) {
	        case 0:
	        	insertBBCode(BBCODE.BOLD);
	            break;
	        case 1:
	        	insertBBCode(BBCODE.ITALICS);
	            break;
	        case 2:
	        	insertBBCode(BBCODE.UNDERLINE);
	            break;
	        case 3:
	        	insertBBCode(BBCODE.STRIKEOUT);
	            break;
	        case 4:
	        	insertBBCode(BBCODE.URL);
	            break;
	        case 5:
	        	insertBBCode(BBCODE.IMAGE);
	            break;
	        case 6:
	        	insertBBCode(BBCODE.QUOTE);
	            break;
	        case 7:
	        	insertBBCode(BBCODE.SPOILER);
	            break;
	        case 8:
	        	insertBBCode(BBCODE.CODE);
	            break;
			}
			dismiss();
		}
		
	};
	
	private int selectionStart = -1;
	private int selectionEnd = -1;
	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.bbcode){
			selectionStart = mMessage.getSelectionStart();//work around the ICS text selection actionbar, bane of my existence
			selectionEnd = mMessage.getSelectionEnd();
			BBCodeFragment fragment = new BBCodeFragment();
	        fragment.show(getActivity().getSupportFragmentManager(), "select_bbcode_dialog");
		}
		if(v.getId() == R.id.emotes){
			Toast.makeText(v.getContext(), "EMOTIONALLY UNAVAILABLE", Toast.LENGTH_LONG).show();
		}
	}
    
    private enum BBCODE {BOLD, ITALICS, UNDERLINE, STRIKEOUT, URL, IMAGE, QUOTE, SPOILER, CODE};

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
	        case R.id.bbcode_bold:
	        	insertBBCode(BBCODE.BOLD);
	            break;
	        case R.id.bbcode_italics:
	        	insertBBCode(BBCODE.ITALICS);
	            break;
	        case R.id.bbcode_underline:
	        	insertBBCode(BBCODE.UNDERLINE);
	            break;
	        case R.id.bbcode_strikeout:
	        	insertBBCode(BBCODE.STRIKEOUT);
	            break;
	        case R.id.bbcode_url:
	        	insertBBCode(BBCODE.URL);
	            break;
	        case R.id.bbcode_image:
	        	insertBBCode(BBCODE.IMAGE);
	            break;
	        case R.id.bbcode_quote:
	        	insertBBCode(BBCODE.QUOTE);
	            break;
	        case R.id.bbcode_spoiler:
	        	insertBBCode(BBCODE.SPOILER);
	            break;
	        case R.id.bbcode_code:
	        	insertBBCode(BBCODE.CODE);
	            break;
            case R.id.submit_button:
                postReply();
                break;
            case R.id.discard:
            	deleteReply();
            	leave();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }
    
    public void insertBBCode(BBCODE code){
    	if(selectionStart < 0){//we might be getting this from an earlier point
	    	selectionStart = mMessage.getSelectionStart();
	    	selectionEnd = mMessage.getSelectionEnd();
    	}
    	boolean highlighted = selectionStart != selectionEnd;
    	String startTag = null;
    	String endTag = null;
    	switch(code){
    	case BOLD:
    		startTag = "[b]";
    		endTag = "[/b]";
    		break;
    	case ITALICS:
    		startTag = "[i]";
    		endTag = "[/i]";
    		break;
    	case UNDERLINE:
    		startTag = "[u]";
    		endTag = "[/u]";
    		break;
    	case STRIKEOUT:
    		startTag = "[s]";
    		endTag = "[/s]";
    		break;
    	case URL:
    		/* TODO clipboard code, probably need to implement an alertdialog for this
    		String link = null;
    		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
    			ClipboardManager cb = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
    			String copy = String.valueOf(cb.getText());
    			if(copy.startsWith("http://") || copy.startsWith("https://")){
    				link = copy;
    			}
    		}else{
    			android.content.ClipboardManager cb = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
    			String copy = String.valueOf(cb.getText());
    			if(copy.startsWith("http://") || copy.startsWith("https://")){
    				link = copy;
    			}
    		}
    		if(link != null){
    			startTag = "[url="+link+"]";
    		}else{
    			startTag = "[url]";
    		}
    		*/
			startTag = "[url]";
    		endTag = "[/url]";
    		break;
    	case QUOTE:
    		startTag = "[quote]";
    		endTag = "[/quote]";
    		break;
    	case IMAGE:
    		startTag = "[img]";
    		endTag = "[/img]";
    		break;
    	case SPOILER:
    		startTag = "[spoiler]";
    		endTag = "[/spoiler]";
    		break;
    	case CODE:
    		startTag = "[code]";
    		endTag = "[/code]";
    		break;
    	}
    	if(startTag != null && endTag != null){
    		if(highlighted){
    			mMessage.getEditableText().insert(selectionStart, startTag);
    			mMessage.getEditableText().insert(selectionEnd+startTag.length(), endTag);
    			mMessage.setSelection(selectionStart+startTag.length());
    		}else{
    			mMessage.getEditableText().insert(selectionStart, startTag+endTag);
    			mMessage.setSelection(selectionStart+startTag.length());
    		}
    	}
    	selectionStart = -1;//reset them for next time
    	selectionEnd = -1;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
		getActivity().getSupportLoaderManager().destroyLoader(Constants.REPLY_LOADER_ID);
		getActivity().getSupportLoaderManager().destroyLoader(Constants.MISC_LOADER_ID);
		getActivity().getContentResolver().unregisterContentObserver(mReplyDataCallback);
		getActivity().getContentResolver().unregisterContentObserver(mThreadObserver);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupTasks();
    }

	@Override
	public void onDetach() {
		super.onDetach();
	}

    private void cleanupTasks() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    private Bundle getExtras() {
        if (getArguments() != null) {
            return getArguments();
        }

        return getActivity().getIntent().getExtras();
    }
    
    private void postReply() {
    	new AlertDialog.Builder(getActivity())
        .setTitle("Confirm Post?")
        .setPositiveButton(R.string.post_reply,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface aDialog, int aWhich) {
                	if(mDialog == null && getActivity() != null){
                		mDialog = ProgressDialog.show(getActivity(), "Posting", "Hopefully it didn't suck...", true, true);
                	}
                    saveReply();
                    ((AwfulActivity) getActivity()).sendMessage(mMessenger, AwfulSyncService.MSG_SEND_POST, mThreadId, mPostId, new Integer(mReplyType));
                }
            })
        .setNegativeButton(R.string.draft_alert_discard, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface aDialog, int aWhich) {
                ContentResolver cr = getActivity().getContentResolver();
                cr.delete(AwfulMessage.CONTENT_URI_REPLY, AwfulMessage.ID+"=?", AwfulProvider.int2StrArray(mThreadId));
                leave();
            }
        }).setNeutralButton(R.string.reply_alert_save_draft,  new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface aDialog, int aWhich) {
                saveReply();
                leave();
            }
        })
        .show();
    }
    
    private void deleteReply(){
		ContentResolver cr = getActivity().getContentResolver();
		cr.delete(AwfulMessage.CONTENT_URI_REPLY, AwfulMessage.ID+"=?", AwfulProvider.int2StrArray(mThreadId));
    }
    
    private void saveReply(){
    	if(getActivity() != null && mThreadId >0){
    		ContentResolver cr = getActivity().getContentResolver();
	    	ContentValues post = new ContentValues();
	    	post.put(AwfulMessage.ID, mThreadId);
	    	post.put(AwfulMessage.TYPE, mReplyType);
	    	String content = mMessage.getText().toString();
	    	if(content.length() >0){
	    		post.put(AwfulMessage.REPLY_CONTENT, content);
    		}
	    	if(cr.update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mThreadId), post, null, null)<1){
	    		cr.insert(AwfulMessage.CONTENT_URI_REPLY, post);
	    	}
    	}
    }

	private class ReplyCallback extends ContentObserver implements LoaderManager.LoaderCallbacks<Cursor> {

		public ReplyCallback(Handler handler) {
			super(handler);
		}

		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
			Log.i(TAG,"Create Reply Cursor: "+mThreadId);
            return new CursorLoader(getActivity(), 
            						ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, mThreadId), 
            						AwfulProvider.DraftPostProjection, 
            						null,
            						null,
            						null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.e(TAG,"Reply load finished, populating: "+aData.getCount());
        	if(!aData.isClosed() && aData.getCount() >0 && aData.moveToFirst()){
        		mReplyType = aData.getInt(aData.getColumnIndex(AwfulMessage.TYPE));
        		mPostId = aData.getInt(aData.getColumnIndex(AwfulPost.EDIT_POST_ID));
        		String replyData = aData.getString(aData.getColumnIndex(AwfulMessage.REPLY_CONTENT));
        		if (replyData != null) {
    				String quoteData = NetworkUtils.unencodeHtml(replyData);
    				if(quoteData.endsWith("[/quote]")){
    					quoteData = quoteData+"\n\n";
    				}
    				mMessage.setText(quoteData);
    				mMessage.setSelection(quoteData.length());
    				originalReplyData = NetworkUtils.unencodeHtml(aData.getString(aData.getColumnIndex(AwfulPost.REPLY_ORIGINAL_CONTENT)));
    				if(originalReplyData == null){
    					originalReplyData = "";
    				}
    				//TODO this part might be causing that odd swype bug, but I can't replicate it
    		        //if(mSelection>0 && mMessage.length() >= mSelection){
    		        //    mMessage.setSelection(mSelection);
    		        //}
    			}
        		String formKey = aData.getString(aData.getColumnIndex(AwfulPost.FORM_KEY));
        		String formCookie = aData.getString(aData.getColumnIndex(AwfulPost.FORM_COOKIE));
        		if((formKey != null && formCookie != null && formKey.length()>0 && formCookie.length()>0) || mReplyType == AwfulMessage.TYPE_EDIT){
        		}else{
			        if(getActivity() != null){
			        	((AwfulActivity) getActivity()).sendMessage(mMessenger, AwfulSyncService.MSG_FETCH_POST_REPLY, mThreadId, mPostId, new Integer(AwfulMessage.TYPE_NEW_REPLY));
			        }
        		}
        	}else{
		        if(mDialog == null && getActivity() != null){
		        	Log.e(TAG, "DISPLAYING DIALOG");
		        	//mDialog = ProgressDialog.show(getActivity(), "Loading", "Fetching Message...", true, true);
		        	((AwfulActivity) getActivity()).sendMessage(mMessenger, AwfulSyncService.MSG_FETCH_POST_REPLY, mThreadId, mPostId, new Integer(mReplyType));
		        }
        	}
        	Log.e(TAG, "CLOSING CURSOR");
        	aData.close();
        }
        
        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        	
        }
        
        @Override
        public void onChange (boolean selfChange){
        	Log.i(TAG,"Post Data update.");
        	if(getActivity() != null){
        		refreshLoader();
        	}
        }
    }
	
	private class ThreadDataCallback implements LoaderManager.LoaderCallbacks<Cursor> {

        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            return new CursorLoader(getActivity(), ContentUris.withAppendedId(AwfulThread.CONTENT_URI, mThreadId), 
            		AwfulProvider.ThreadProjection, null, null, null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"Thread title finished, populating.");
        	if(aData.getCount() >0 && aData.moveToFirst()){
        		threadClosed = aData.getInt(aData.getColumnIndex(AwfulThread.LOCKED))>0;
        		mThreadTitle = aData.getString(aData.getColumnIndex(AwfulThread.TITLE));
        		setTitle(getTitle());
        	}
        }
        
        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        }
    }
    private class ThreadContentObserver extends ContentObserver {
        public ThreadContentObserver(Handler aHandler) {
            super(aHandler);
        }
        @Override
        public void onChange (boolean selfChange){
        	Log.e(TAG,"Thread Data update.");
        	refreshThreadInfo();
        }
    }
	
	private void refreshLoader(){
		if(getActivity() != null){
			getLoaderManager().restartLoader(Constants.REPLY_LOADER_ID, null, mReplyDataCallback);
		}
	}
	
	private void refreshThreadInfo() {
		if(getActivity() != null){
			getLoaderManager().restartLoader(Constants.MISC_LOADER_ID, null, mThreadLoaderCallback);
		}
	}

	@Override
	public void onPageVisible() {
		
	}

	@Override
	public void onPageHidden() {
		if(getActivity() != null){
			InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mMessage.getApplicationWindowToken(), 0);
		}
	}
	
	public int getThreadId(){
		return mThreadId;
	}

	@Override
	public String getTitle() {
		String title = "";
		if(mThreadTitle != null && mThreadTitle.length()>0){
			title = " - "+mThreadTitle;
		}
		switch(mReplyType){
		case AwfulMessage.TYPE_EDIT:
			return "Editing"+title;
		case AwfulMessage.TYPE_NEW_REPLY:
			return "Reply"+title;
		case AwfulMessage.TYPE_QUOTE:
			return "Quote"+title;
		}
		return "Loading";//TODO replace with thread name
	}

	public void newReply(int threadId, int postId, int type) {
		deleteReply();
		mThreadId = threadId;
		mPostId = postId;
		mReplyType = type;
		mThreadTitle = null;
		refreshLoader();
		refreshThreadInfo();
		setTitle(getTitle());
	}
	
	
}
