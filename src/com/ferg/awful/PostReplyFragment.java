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

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.provider.AwfulProvider;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulMessage;
import com.ferg.awful.thread.AwfulPost;

public class PostReplyFragment extends DialogFragment {
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
            		if(mDialog != null){
            			mDialog.dismiss();
            			mDialog = null;
            		}
                	if(aMsg.what == AwfulSyncService.MSG_FETCH_POST_REPLY){
                		getActivity().getSupportLoaderManager().restartLoader(mThreadId, null, mReplyDataCallback);
                	}
                	if(aMsg.what == AwfulSyncService.MSG_SEND_POST){
                		sendSuccessful = true;
                		if(getActivity() != null){
                			Toast.makeText(getActivity(), "Message Sent!", Toast.LENGTH_LONG).show();
                			if(getActivity() instanceof MessageDisplayActivity){
                				getActivity().finish();
                			}else{
                				((ThreadDisplayActivity)getActivity()).refreshInfo();
                				dismiss();
                			}
                		}
                	}
                    break;
                case AwfulSyncService.Status.WORKING:
                    break;
                case AwfulSyncService.Status.ERROR:
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
                	if(aMsg.what == AwfulSyncService.MSG_FETCH_POST_REPLY){
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

        fragment.setShowsDialog(false);
        fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);

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
    }
    
    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        View result = aInflater.inflate(R.layout.post_reply, aContainer, false);

        mMessage = (EditText) result.findViewById(R.id.post_message);
        mTitle   = (TextView) result.findViewById(R.id.title);
        mSubmit  = (ImageButton) result.findViewById(R.id.submit_button);

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
		getActivity().getSupportLoaderManager().restartLoader(mThreadId, null, mReplyDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulMessage.CONTENT_URI_REPLY, true, mReplyDataCallback);
        
        mTitle.setText(getString(R.string.post_reply));
        mSubmit.setOnClickListener(onSubmitClick);
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
        ((AwfulActivity) getActivity()).unregisterSyncService(mMessenger, mThreadId);
		getActivity().getSupportLoaderManager().destroyLoader(mThreadId);
		getActivity().getContentResolver().unregisterContentObserver(mReplyDataCallback);
        if(!sendSuccessful){
        	if(mMessage.getText().toString().trim().equalsIgnoreCase(originalReplyData.trim())){
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
    public void onDestroy() {
        super.onDestroy();
        cleanupTasks();
        if(getActivity() != null){
			if(getActivity() instanceof MessageDisplayActivity){
				getActivity().setResult(RESULT_POSTED);
			}else{
				((ThreadDisplayActivity)getActivity()).refreshInfo();
				((ThreadDisplayActivity)getActivity()).refreshThread();
				dismiss();
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
        	mDialog = ProgressDialog.show(getActivity(), "Posting", "Hopefully it didn't suck...", true);
        	saveReply();
    		((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_SEND_POST, mThreadId, mPostId, new Integer(mReplyType));
        }
    };
    
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
        	Log.v(TAG,"Reply load finished, populating: "+aData.getCount());
        	if(aData.getCount() >0 && aData.moveToFirst()){
        		mReplyType = aData.getInt(aData.getColumnIndex(AwfulMessage.TYPE));
        		mPostId = aData.getInt(aData.getColumnIndex(AwfulPost.EDIT_POST_ID));
        		String replyData = aData.getString(aData.getColumnIndex(AwfulMessage.REPLY_CONTENT));
        		if (replyData != null) {
    				String quoteData = NetworkUtils.unencodeHtml(replyData);
    				mMessage.setText(quoteData);
    				mMessage.setSelection(quoteData.length());
    				originalReplyData = aData.getString(aData.getColumnIndex(AwfulPost.REPLY_ORIGINAL_CONTENT));
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
			        mSubmit.setEnabled(true);
        		}else{
			        mSubmit.setEnabled(false);
        		}
        	}else{
		        //We'll enable it once we have a formkey and cookie
		        mSubmit.setEnabled(false);
		        if(mReplyType != AwfulMessage.TYPE_NEW_REPLY){
		        	mDialog = ProgressDialog.show(getActivity(), "Loading", "Fetching Message...", true);
		        }
	    		((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_FETCH_POST_REPLY, mThreadId, mPostId, new Integer(mReplyType));
        	}
        	aData.close();
        }
        
        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        	
        }
        
        @Override
        public void onChange (boolean selfChange){
        	Log.i(TAG,"PM Data update.");
        	if(getActivity() != null){
        		getActivity().getSupportLoaderManager().restartLoader(mThreadId, null, this);
        	}
        }
    }
}
