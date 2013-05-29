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

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;

public class PostReplyFragment extends AwfulFragment implements OnClickListener {
    private static final String TAG = "PostReplyFragment";

    public static final int REQUEST_POST = 5;
    public static final int RESULT_POSTED = 6;
    public static final int RESULT_CANCELLED = 7;
    public static final int RESULT_EDITED = 8;
    public static final int ADD_ATTACHMENT = 9;

    private EditText mMessage;
    private ProgressDialog mDialog;

    private int mThreadId;
    private int mPostId;
    private int mReplyType;
    private String mThreadTitle;
    private boolean sendSuccessful = false;
    private String originalReplyData = "";
    private String mFileAttachment;
    
    private ReplyCallback mReplyDataCallback = new ReplyCallback(mHandler);
    private ThreadDataCallback mThreadLoaderCallback;
    private ThreadContentObserver mThreadObserver = new ThreadContentObserver(mHandler);

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);Log.e(TAG,"onCreate");
        setHasOptionsMenu(true);
        setRetainInstance(false);
        mThreadLoaderCallback = new ThreadDataCallback();

        Intent intent = getActivity().getIntent();
        mReplyType = intent.getIntExtra(Constants.EDITING, AwfulMessage.TYPE_NEW_REPLY);
        mPostId = intent.getIntExtra(Constants.REPLY_POST_ID, 0);
        mThreadId = intent.getIntExtra(Constants.REPLY_THREAD_ID, 0);
        if(mPostId == 0 && mThreadId == 0){
            getActivity().finish();
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);Log.e(TAG,"onCreateView");

        View result = inflateView(R.layout.post_reply, aContainer, aInflater);

        mMessage = aq.find(R.id.post_message).getEditText();
        mMessage.setText("");
        aq.find(R.id.bbcode).clicked(this);
        aq.find(R.id.emotes).clicked(this);

        return result;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);Log.e(TAG,"onActivityCreated");

        mMessage.setBackgroundColor(mPrefs.postBackgroundColor);
        mMessage.setTextColor(mPrefs.postFontColor);
        getActivity().getContentResolver().registerContentObserver(AwfulMessage.CONTENT_URI_REPLY, true, mReplyDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulThread.CONTENT_URI, true, mThreadObserver);
        refreshLoader();
        refreshThreadInfo();
        
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ADD_ATTACHMENT) {
                Uri selectedImageUri = data.getData();
                File attachment = new File(getFilePath(selectedImageUri));
                Toast attachmentToast;
                if(attachment.isFile() && attachment.canRead()){
                	if(attachment.length()>1048576){
                		attachmentToast = Toast.makeText(this.getActivity(), String.format(this.getString(R.string.file_too_big), attachment.getName()), Toast.LENGTH_LONG);
                		mFileAttachment = null;
                	}else{
                		mFileAttachment = getFilePath(selectedImageUri);
                		attachmentToast = Toast.makeText(this.getActivity(), String.format(this.getString(R.string.file_attached), attachment.getName()), Toast.LENGTH_LONG);
                	}
                }else{
                	attachmentToast = Toast.makeText(this.getActivity(), String.format(this.getString(R.string.file_unreadable), attachment.getName()), Toast.LENGTH_LONG);
                	mFileAttachment = null;
                }

                attachmentToast.show();
                this.getAwfulActivity().invalidateOptionsMenu();
            }
        }

    }
    
    public String getFilePath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = this.getActivity().getContentResolver().query(uri, projection, null, null, null);
        if(cursor!=null)
        {
            //HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
            //THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
            int column_index = cursor
            .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        else return null;
    }


    @Override
    public void onResume() {
        super.onResume(); Log.e(TAG,"onResume");
    }
    
    private void leave(){
    	if(getAwfulActivity() != null){
    		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    		if(imm != null && getView() != null){
    			imm.hideSoftInputFromWindow(getView().getApplicationWindowToken(), 0);
    		}
    		getAwfulActivity().fragmentClosing(this);
    	}
    }
    
    @Override
    public void onPause() {
        super.onPause();Log.e(TAG,"onPause");
        cleanupTasks();
    }
        
    @Override
    public void onStop() {
        super.onStop();Log.e(TAG,"onStop");
        cleanupTasks();
    }
    
    
    
    private void autosave(){
        if(!sendSuccessful && mMessage != null){
        	if(mMessage.length() < 1 || mMessage.getText().toString().replaceAll("\\s", "").equalsIgnoreCase(originalReplyData.replaceAll("\\s", "")) || this.sendSuccessful == true){
        		Log.i(TAG, "Message unchanged, discarding.");
        		deleteReply();//if the reply is unchanged, throw it out.
        		mMessage.setText("");
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
			if(mReplyType == AwfulMessage.TYPE_EDIT){
				getActivity().setResult(mPostId);
			}else{
				getActivity().setResult(RESULT_POSTED);
			}
			leave();
    	}
    }

	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Log.d(TAG,"onCreateOptionsMenu");
        inflater.inflate(R.menu.post_reply, menu);
        MenuItem attach = menu.findItem(R.id.add_attachment);
        if(attach != null && mPrefs != null){
        	attach.setEnabled(mPrefs.hasPlatinum);
        	attach.setVisible(mPrefs.hasPlatinum);
        }
        MenuItem remove = menu.findItem(R.id.remove_attachment);
        if(remove != null && mPrefs != null){
        	remove.setEnabled((mPrefs.hasPlatinum && this.mFileAttachment != null));
        	remove.setVisible(mPrefs.hasPlatinum && this.mFileAttachment != null);
        }
    }
	
    @Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		super.onPreferenceChange(prefs);
		//refresh the menu to show/hide attach option (plat only)
		getAwfulActivity().invalidateOptionsMenu();
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
            	getActivity().setResult(RESULT_CANCELLED);
            	leave();
                break;
            case R.id.save_draft:
            	saveReply();
            	getActivity().setResult(RESULT_CANCELLED);
            	leave();
            	break;
            case R.id.emotes:
    	    	selectionStart = mMessage.getSelectionStart();
            	new EmoteFragment().show(getFragmentManager(), "emotes");
            	break;
            case R.id.add_attachment:
            	    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            	    intent.setType("image/*");
            	    startActivityForResult(Intent.createChooser(intent,
                            "Select Picture"), ADD_ATTACHMENT);

            	break;
            case R.id.remove_attachment:
        	    this.mFileAttachment = null;
        	    Toast removeToast = Toast.makeText(getAwfulActivity(), getAwfulActivity().getResources().getText(R.string.file_removed), Toast.LENGTH_SHORT);
        	    removeToast.show();
                this.getAwfulActivity().invalidateOptionsMenu();
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
    
    private void insertEmote(String emote){
    	selectionStart = mMessage.getSelectionStart();
		mMessage.getEditableText().insert(selectionStart, emote);
		mMessage.setSelection(selectionStart+emote.length());
    	selectionStart = -1;//reset them for next time
    	selectionEnd = -1;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();Log.e(TAG,"onDestroyView");
        autosave();
		getLoaderManager().destroyLoader(Constants.REPLY_LOADER_ID);
		getLoaderManager().destroyLoader(Constants.MISC_LOADER_ID);
		getActivity().getContentResolver().unregisterContentObserver(mReplyDataCallback);
		getActivity().getContentResolver().unregisterContentObserver(mThreadObserver);
        mMessage = null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();Log.e(TAG,"onDestroy");
        cleanupTasks();
    }

	@Override
	public void onDetach() {
		super.onDetach();Log.e(TAG,"onDetach");
	}

    private void cleanupTasks() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
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
                    sendPost();
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

    private void sendPost(){
        ((AwfulActivity) getActivity()).sendMessage(mMessenger, AwfulSyncService.MSG_SEND_POST, mThreadId, mPostId, new Integer(mReplyType));
    }
    
    private void deleteReply(){
		ContentResolver cr = getActivity().getContentResolver();
		cr.delete(AwfulMessage.CONTENT_URI_REPLY, AwfulMessage.ID+"=?", AwfulProvider.int2StrArray(mThreadId));
		cr.notifyChange(AwfulThread.CONTENT_URI, null);
		sendSuccessful = true;
    }
    
    private void saveReply(){
    	if(getActivity() != null && mThreadId >0 && mMessage != null){
    		ContentResolver cr = getActivity().getContentResolver();
	    	ContentValues post = new ContentValues();
	    	post.put(AwfulMessage.ID, mThreadId);
	    	post.put(AwfulMessage.TYPE, mReplyType);
	    	String content = mMessage.getText().toString();
	    	if(content.length() >0){
	    		post.put(AwfulMessage.REPLY_CONTENT, content);
    		}
	    	if(mFileAttachment != null){
		    	post.put(AwfulMessage.REPLY_ATTACHMENT, mFileAttachment);
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
		        	Log.d(TAG, "DISPLAYING DIALOG");
		        	mDialog = ProgressDialog.show(getActivity(), "Loading", "Fetching Message...", true, true);
		        	((AwfulActivity) getActivity()).sendMessage(mMessenger, AwfulSyncService.MSG_FETCH_POST_REPLY, mThreadId, mPostId, new Integer(mReplyType));
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
	
	private class ThreadDataCallback implements LoaderManager.LoaderCallbacks<Cursor> {

        public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
            return new CursorLoader(getActivity(), ContentUris.withAppendedId(AwfulThread.CONTENT_URI, mThreadId), 
            		AwfulProvider.ThreadProjection, null, null, null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"Thread title finished, populating.");
        	if(aData.getCount() >0 && aData.moveToFirst()){
        		//threadClosed = aData.getInt(aData.getColumnIndex(AwfulThread.LOCKED))>0;
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
		autosave();
		if(getActivity() != null && mMessage != null){
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
		return "Loading";
	}

	public void newReply(int threadId, int postId, int type) {
		if(threadId == mThreadId){
			deleteReply();
		}else{
			autosave();
		}
		mThreadId = threadId;
		mPostId = postId;
		mReplyType = type;
		mThreadTitle = null;
		sendSuccessful = false;
		originalReplyData = "";
		if(mMessage != null){
			mMessage.setText("");
		}
		refreshLoader();
		refreshThreadInfo();
		setTitle(getTitle());
	}

	@Override
	public void fragmentMessage(String type, String contents) {
		if(type.equalsIgnoreCase("emote-selected")){
			insertEmote(contents);
		}
	}
	

	@Override
	public String getInternalId() {
		return TAG;
	}

	@Override
	public boolean volumeScroll(KeyEvent event) {
		//I don't think that's necessary
		return false;
	}
}
