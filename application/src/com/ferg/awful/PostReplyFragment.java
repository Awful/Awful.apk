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

package com.ferg.awful;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.provider.AwfulProvider;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulMessage;
import com.ferg.awful.thread.AwfulPost;

public class PostReplyFragment extends AwfulFragment implements OnClickListener {
    private static final String TAG = "PostReplyActivity";

    public static final int RESULT_POSTED = 1;

    private Bundle mExtras;
    private ImageButton mSubmit;
    private EditText mMessage;
    private ProgressDialog mDialog;
    private AwfulPreferences mPrefs;
    private TextView mTitle;

    private int mThreadId;
    private int mPostId;
    private int mSelection;
    private int mReplyType;
    private boolean sendSuccessful = false;
    private String originalReplyData = "";
    
	private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message aMsg) {
        	Log.i(TAG, "Received Message:"+aMsg.what+" "+aMsg.arg1+" "+aMsg.arg2);
            switch (aMsg.arg1) {
                case AwfulSyncService.Status.OKAY:
            		loadingSucceeded();
            		if(mDialog != null){
            			mDialog.dismiss();
            			mDialog = null;
            		}
                	if(aMsg.what == AwfulSyncService.MSG_FETCH_POST_REPLY){
                		refreshLoader();
                	}
                	if(aMsg.what == AwfulSyncService.MSG_SEND_POST){
                		sendSuccessful = true;
                		if(getActivity() != null){ 
                			Toast.makeText(getActivity(), getActivity().getString(R.string.post_sent), Toast.LENGTH_LONG).show();
                			if(getActivity() instanceof PostReplyActivity){
                				getActivity().setResult(RESULT_POSTED);
                				getActivity().finish();
                			}
                		}
                	}
                    break;
                case AwfulSyncService.Status.WORKING:
                	loadingStarted();
                    break;
                case AwfulSyncService.Status.ERROR:
            		loadingFailed();
                	if(mDialog != null){
            			mDialog.dismiss();
            			mDialog = null;
            		}
                	if(aMsg.what == AwfulSyncService.MSG_SEND_POST){
            			saveReply();
	            		if(getActivity() != null){
	            			Toast.makeText(getActivity(), "Post Failed to Send! Message Saved...", Toast.LENGTH_LONG).show();
	            		}
                	}
                	if(aMsg.what == AwfulSyncService.MSG_FETCH_POST_REPLY && getActivity() != null){
            			Toast.makeText(getActivity(), "Reply Load Failed!", Toast.LENGTH_LONG).show();
                	}
                    break;
                default:
                    super.handleMessage(aMsg);
            }
        }
    };
    private Messenger mMessenger = new Messenger(mHandler);
    private ReplyCallback mReplyDataCallback = new ReplyCallback(mHandler);

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

        if (!((AwfulActivity) getActivity()).isTV()) {
            setHasOptionsMenu(true);
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        View result = aInflater.inflate(R.layout.post_reply, aContainer, false);

        mMessage = (EditText) result.findViewById(R.id.post_message);
        result.findViewById(R.id.bbcode).setOnClickListener(this);
        result.findViewById(R.id.emotes).setOnClickListener(this);
        mPrefs = new AwfulPreferences(getActivity());

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
        
        ((AwfulActivity) getActivity()).registerSyncService(mMessenger, mThreadId);
		getActivity().getSupportLoaderManager().restartLoader(Constants.REPLY_LOADER_ID, null, mReplyDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulMessage.CONTENT_URI_REPLY, true, mReplyDataCallback);
        
        if (((AwfulActivity) getActivity()).isTV()) {
            mTitle.setText(getString(R.string.post_reply));
            mSubmit.setOnClickListener(onSubmitClick);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        cleanupTasks();
    }
        
    @Override
    public void onStop() {
        super.onStop();
        if(!sendSuccessful){
        	if(mMessage.getText().toString().replaceAll("\\s", "").equalsIgnoreCase(originalReplyData.replaceAll("\\s", ""))){
        		Log.i(TAG, "Message unchanged, discarding.");
        		deleteReply();//if the reply is unchanged, throw it out.
        	}else{
        		Log.i(TAG, "Message Unsent, saving.");
        		saveReply();
        	}
        }
        cleanupTasks();
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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
    		/* clipboard code, probably need to implement an alertdialog for this
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
        //((AwfulActivity) getActivity()).unregisterSyncService(mMessenger, mThreadId);//this is causing the threadview to lose its sync
		getActivity().getSupportLoaderManager().destroyLoader(mThreadId);
		getActivity().getContentResolver().unregisterContentObserver(mReplyDataCallback);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupTasks();
        if(getActivity() != null){
			if(getActivity() instanceof PostReplyActivity){
			}else{
				((ThreadDisplayActivity)getActivity()).refreshInfo();
				((ThreadDisplayActivity)getActivity()).refreshThread();
			}
		}
    }

	@Override
	public void onDetach() {
		super.onDetach();
		mSelection = mMessage.getSelectionStart();
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

    private View.OnClickListener onSubmitClick = new View.OnClickListener() {
        public void onClick(View aView) {
            postReply();
        }
    };

    private void postReply() {
    	if(mDialog == null && getActivity() != null){
    		mDialog = ProgressDialog.show(getActivity(), "Posting", "Hopefully it didn't suck...", true, true);
    	}
        saveReply();
        ((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_SEND_POST, mThreadId, mPostId, new Integer(mReplyType));
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
        	if(aData.getCount() >0 && aData.moveToFirst()){
        		mReplyType = aData.getInt(aData.getColumnIndex(AwfulMessage.TYPE));
        		mPostId = aData.getInt(aData.getColumnIndex(AwfulPost.EDIT_POST_ID));
        		String replyData = aData.getString(aData.getColumnIndex(AwfulMessage.REPLY_CONTENT));
        		if (replyData != null) {
    				String quoteData = NetworkUtils.unencodeHtml(replyData)+"\n\n";
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
    		        if(mSubmit != null){
    		        	mSubmit.setEnabled(true);
    		        }
        		}else{
        			if(mSubmit != null){
        				mSubmit.setEnabled(false);
        			}
			        if(getActivity() != null){
			        	((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_FETCH_POST_REPLY, mThreadId, mPostId, new Integer(AwfulMessage.TYPE_NEW_REPLY));
			        }
        		}
        	}else{
		        //We'll enable it once we have a formkey and cookie
		        if(mSubmit != null){
		        	mSubmit.setEnabled(false);
		        }
		        if(mDialog == null && getActivity() != null && mReplyType != AwfulMessage.TYPE_NEW_REPLY){
		        	Log.e(TAG, "DISPLAYING DIALOG");
		        	mDialog = ProgressDialog.show(getActivity(), "Loading", "Fetching Message...", true, true);
		        }
		        if(getActivity() != null){
		        	((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_FETCH_POST_REPLY, mThreadId, mPostId, new Integer(mReplyType));
		        }
        	}
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
	
	private void refreshLoader(){
		if(getActivity() != null){
			getLoaderManager().restartLoader(Constants.REPLY_LOADER_ID, null, mReplyDataCallback);
		}
	}
}
