package com.ferg.awful;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.dialog.LogOutDialog;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.provider.AwfulProvider;
import com.ferg.awful.service.AwfulCursorAdapter;
import com.ferg.awful.service.AwfulSyncService;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulMessage;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MessageFragment extends DialogFragment implements AwfulUpdateCallback, OnClickListener {

    private static final String TAG = "MessageFragment";
    
	private int pmId = -1;
	private String recipient;
	
	private WebView mDisplayText;
	private EditText mEditReply;
	private Button mReplyButton;
	private TextView mUsername;
	private TextView mPostdate;
	private TextView mTitle;
	private EditText mRecipient;
	private EditText mSubject;
	private View mBackground;

	private AwfulPreferences mPrefs;

	private Editable saved_reply;
	private Editable saved_title;
	private Editable saved_recipient;

	private ProgressDialog mDialog;

	private boolean paused = false;

	private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message aMsg) {
        	Log.i(TAG, "Received Message:"+aMsg.what+" "+aMsg.arg1+" "+aMsg.arg2);
            switch (aMsg.arg1) {
                case AwfulSyncService.Status.OKAY:
                	loadingSucceeded();
                	if(aMsg.what == AwfulSyncService.MSG_FETCH_PM){
                		getActivity().getSupportLoaderManager().restartLoader(pmId, null, mPMDataCallback);
                	}
                    break;
                case AwfulSyncService.Status.WORKING:
                	loadingStarted();
                    break;
                case AwfulSyncService.Status.ERROR:
                	loadingFailed();
                    break;
                default:
                    super.handleMessage(aMsg);
            }
        }
    };
    private Messenger mMessenger = new Messenger(mHandler);
    private PMCallback mPMDataCallback = new PMCallback(mHandler);

    public static MessageFragment newInstance(String aUser, int aId) {
        MessageFragment fragment = new MessageFragment(aUser, aId);

        fragment.setShowsDialog(false);
        fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);

        return fragment;
    }
	
	public MessageFragment() {}

	/**
	 * Creates a new Message Display/Reply fragment.
	 * @param user User ID to send message to. Optional: will not be used if replying to message.
	 * @param id PM Id number to reply to. Will fetch message data from service automatically. Set to 0 for a blank message.
	 */
	public MessageFragment(String user, int id) {
		pmId = id;
		recipient = user;
	}

	@Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
	
	public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);
        mPrefs = new AwfulPreferences(getActivity());
        
        setRetainInstance(true);
        
        View result = aInflater.inflate(R.layout.message_view, aContainer, false);
        
        mDisplayText = (WebView) result.findViewById(R.id.messagebody);
        mEditReply = (EditText) result.findViewById(R.id.edit_reply_text);
        mReplyButton = (Button) result.findViewById(R.id.message_reply_button);
        mReplyButton.setOnClickListener(this);
        mRecipient = (EditText) result.findViewById(R.id.message_user);
        mSubject = (EditText) result.findViewById(R.id.message_subject);
        mUsername = (TextView) result.findViewById(R.id.username);
        mPostdate = (TextView) result.findViewById(R.id.post_date);
        mTitle = (TextView) result.findViewById(R.id.message_title);
        mBackground = result;
        updateColors(result, mPrefs);

        return result;
    }
	
	private void updateColors(View v, AwfulPreferences prefs){
        mEditReply.setBackgroundColor(prefs.postBackgroundColor2);
        mRecipient.setBackgroundColor(prefs.postBackgroundColor2);
        mSubject.setBackgroundColor(prefs.postBackgroundColor2);
        mDisplayText.setBackgroundColor(prefs.postBackgroundColor);
        mEditReply.setTextColor(prefs.postFontColor);
        mRecipient.setTextColor(prefs.postFontColor);
        mSubject.setTextColor(prefs.postFontColor);
		TextView miscSubject = (TextView) v.findViewById(R.id.misc_text_subject);
        TextView miscRecip = (TextView) v.findViewById(R.id.misc_text_recipient);
        TextView miscMess = (TextView) v.findViewById(R.id.misc_text_message);
        miscSubject.setBackgroundColor(prefs.postBackgroundColor);
        miscRecip.setBackgroundColor(prefs.postBackgroundColor);
        miscMess.setBackgroundColor(prefs.postBackgroundColor);
        miscSubject.setTextColor(prefs.postFontColor2);
        miscRecip.setTextColor(prefs.postFontColor2);
        miscMess.setTextColor(prefs.postFontColor2);
        v.setBackgroundColor(prefs.postBackgroundColor);
        
	}
	
	private void setActionBar() {
        ActionBar action = getActivity().getActionBar();
        action.setBackgroundDrawable(getResources().getDrawable(R.drawable.bar));
        action.setDisplayHomeAsUpEnabled(true);
    }
	
	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(menu.size() == 0){
            inflater.inflate(R.menu.private_message_menu, menu);
        }
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.send_pm:
                sendPM();
                return true;
            case R.id.new_pm:
            	newMessage();
            	return true;
            case R.id.refresh:
            	if(pmId >0){
            		syncPM();
            	}
            	return true;
            case R.id.settings:
            	startActivity(new Intent().setClass(getActivity(), SettingsActivity.class));
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
	
	@Override
    public void onStart() {
        super.onStart();
        ((AwfulActivity) getActivity()).registerSyncService(mMessenger, Constants.PRIVATE_MESSAGE_THREAD);
		getActivity().getSupportLoaderManager().restartLoader(pmId, null, mPMDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulMessage.CONTENT_URI, true, mPMDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulMessage.CONTENT_URI_REPLY, true, mPMDataCallback);
        syncPM();
    }
	
	private void syncPM() {
		((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_FETCH_PM, pmId, 0);
	}
	
	public void sendPM() {
		((AwfulActivity) getActivity()).sendMessage(AwfulSyncService.MSG_FETCH_PM_INDEX, pmId, AwfulMessage.TYPE_PM);
		mDialog = ProgressDialog.show(getActivity(), "Sending", "Hopefully it didn't suck...", true);
	}

	public void onResume(){
		super.onResume();
		try {
			if(paused){
				Class.forName("android.webkit.WebView").getMethod("onResume", (Class[]) null)
                .invoke(mDisplayText, (Object[]) null);
	            mDisplayText.resumeTimers();
				paused = false;
			}
        } catch (Exception e) {
        }
		if(pmId > 0){
			syncPM();
		}
	}
	public void onPause(){
		super.onPause();
		try {
            Class.forName("android.webkit.WebView").getMethod("onPause", (Class[]) null)
                .invoke(mDisplayText, (Object[]) null);
            paused = true;
            mDisplayText.pauseTimers();
        } catch (Exception e) {
        }
	}
	
	public void onStop(){
		super.onStop();
        ((AwfulActivity) getActivity()).unregisterSyncService(mMessenger, pmId);
		getActivity().getSupportLoaderManager().destroyLoader(pmId);
		getActivity().getContentResolver().unregisterContentObserver(mPMDataCallback);
	}
	
	public void onDetach(){
		super.onDetach();
		if(mPrefs != null){
			mPrefs.unRegisterListener();
		}
		if(mDialog!= null){
			mDialog.dismiss();
			mDialog = null;
		}
	}
	
	private void newMessage(){
		getActivity().getSupportLoaderManager().destroyLoader(pmId);
		pmId = -1;
		recipient = null;
		saved_reply = null;
		saved_title = null;
		saved_recipient = null;
		mEditReply.setText("");
		mUsername.setText("");
		mRecipient.setText("");
		mPostdate.setText("");
		mEditReply.setText("");
		mDisplayText.loadData("", "text/html", "utf-8");
		mTitle.setText("New Message");
		mSubject.setText("");
	}

	@Override
	public void loadingFailed() {
		if(getActivity() != null){
			if (!AwfulActivity.useLegacyActionbar()) {
				getActivity().setProgressBarIndeterminateVisibility(false);
			}
		    Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void loadingStarted() {
		if (!AwfulActivity.useLegacyActionbar() && getActivity() != null) {
			getActivity().setProgressBarIndeterminateVisibility(true);
		}
	}

	@Override
	public void loadingSucceeded() {
		if (!AwfulActivity.useLegacyActionbar() && getActivity() != null) {
			getActivity().setProgressBarIndeterminateVisibility(false);
		}
	}

	@Override
	public void onClick(View v) {
		sendPM();
	}
	

    private boolean isTablet() {
        return ((AwfulActivity) getActivity()).isTablet();
    }

	@Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		if(getView() != null){
			updateColors(getView(), prefs);
		}else{
			if(mBackground != null){
				updateColors(mBackground, prefs);
			}
		}
	}
	
	
	private class PMCallback extends ContentObserver implements LoaderManager.LoaderCallbacks<Cursor> {

		public PMCallback(Handler handler) {
			super(handler);
		}

		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
			Log.i(TAG,"Create PM Cursor:"+pmId);
            return new CursorLoader(getActivity(), 
            						ContentUris.withAppendedId(AwfulMessage.CONTENT_URI, pmId), 
            						AwfulProvider.PMReplyProjection, 
            						null,
            						null,
            						null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"PM load finished, populating: "+aData.getCount());
        	//TODO retain info if entered into reply window
        	if(aData.moveToFirst() && pmId >0){
        		String title = aData.getString(aData.getColumnIndex(AwfulMessage.TITLE));
    			mTitle.setText(Html.fromHtml(title));
        		mDisplayText.loadData(AwfulMessage.getMessageHtml(aData.getString(aData.getColumnIndex(AwfulMessage.CONTENT)),mPrefs),"text/html", "utf-8");
				mPostdate.setText(" on " + aData.getString(aData.getColumnIndex(AwfulMessage.TITLE)));
        		String replyTitle = aData.getString(aData.getColumnIndex(AwfulMessage.REPLY_TITLE));
        		String replyContent = aData.getString(aData.getColumnIndex(AwfulMessage.REPLY_CONTENT));
        		if(replyContent != null){
            		mEditReply.setText(replyContent);
        		}else{
        			mEditReply.setText("");
        		}
        		if(replyTitle != null){
        			mSubject.setText(Html.fromHtml(replyTitle));
        		}else{
        			mSubject.setText(Html.fromHtml(title));
        		}
        		String author = aData.getString(aData.getColumnIndex(AwfulMessage.AUTHOR));
				mUsername.setText("Posted by " + author);
        		String recip = aData.getString(aData.getColumnIndex(AwfulMessage.RECIPIENT));
        		if(recip != null){
        			mRecipient.setText(recip);
        		}else{
        			mRecipient.setText(author);
        		}
        	}else{
        		if(recipient != null){
        			mRecipient.setText(recipient);
        		}
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
        		getActivity().getSupportLoaderManager().restartLoader(pmId, null, this);
        	}
        }
    }
}
