package com.ferg.awfulapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.reply.MessageComposer;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.PMReplyRequest;
import com.ferg.awfulapp.task.PMRequest;
import com.ferg.awfulapp.task.SendPrivateMessageRequest;
import com.ferg.awfulapp.thread.AwfulHtmlPage;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.webview.AwfulWebView;
import com.ferg.awfulapp.webview.WebViewJsInterface;
import com.ferg.awfulapp.widget.ThreadIconPicker;

import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MessageFragment extends AwfulFragment implements OnClickListener {

    private static final String TAG = "MessageFragment";
    
	private int pmId = -1;
	private String recipient;
	
	private AwfulWebView messageWebView;
	private MessageComposer messageComposer;
	private ImageButton mHideButton;
	private TextView mUsername;
	private TextView mPostdate;
	private TextView mTitle;
	private EditText mRecipient;
	private EditText mSubject;
	private View mBackground;
	private ThreadIconPicker threadIconPicker;

	private AwfulPreferences mPrefs;
	
	private ProgressDialog mDialog;

    private Messenger mMessenger = new Messenger(getHandler());
    private PMCallback mPMDataCallback = new PMCallback(getHandler());
    private ContentObserver pmReplyObserver = new ContentObserver(getHandler()){
    	@Override
        public void onChange (boolean selfChange){
        	Log.i(TAG,"PM Data update.");
            restartLoader(pmId, null, mPMDataCallback);
        }
    };

	interface PrivateMessageCallbacks {
		void onMessageClosed();
	}

    public static MessageFragment newInstance(String aUser, int aId) {
		return new MessageFragment(aUser, aId);
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
        mPrefs = AwfulPreferences.getInstance(getActivity());
        
        setRetainInstance(true);
        
        View result = aInflater.inflate(R.layout.private_message_fragment, aContainer, false);
        
        messageWebView = result.findViewById(R.id.messagebody);
		mHideButton = result.findViewById(R.id.hide_message);
		mHideButton.setOnClickListener(this);
		mRecipient = result.findViewById(R.id.message_user);
		mSubject = result.findViewById(R.id.message_subject);
		mUsername = result.findViewById(R.id.username);
		mPostdate = result.findViewById(R.id.post_date);
		mTitle = result.findViewById(R.id.message_title);

		messageComposer = (MessageComposer) getChildFragmentManager().findFragmentById(R.id.message_composer_fragment);
		threadIconPicker = (ThreadIconPicker) getChildFragmentManager().findFragmentById(R.id.thread_icon_picker);
		threadIconPicker.usePrivateMessageIcons();

		mBackground = result;
        updateColors(result, mPrefs);
		messageWebView.setJavascriptHandler(new WebViewJsInterface());
		messageWebView.setContent(AwfulHtmlPage.getContainerHtml(mPrefs, null, false));

		if(pmId <=0){
        	messageWebView.setVisibility(GONE);
        }else{
            syncPM();
        }
        return result;
    }

	private void updateColors(View v, AwfulPreferences prefs){
		int color = ColorProvider.PRIMARY_TEXT.getColor();
		messageComposer.setTextColor(color);
		mRecipient.setTextColor(color);
		mSubject.setTextColor(color);
		mUsername.setTextColor(color);
		mPostdate.setTextColor(color);
		mTitle.setTextColor(color);
	}
	
	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.private_message_writing, menu);
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
			case android.R.id.home:
				closeMessage();
				return true;
            case R.id.send_pm:
                showSubmitDialog();
                return true;
            case R.id.new_pm:
            	newMessage();
            	return true;
            case R.id.settings:
            	getAwfulActivity().navigate(NavigationEvent.Settings.INSTANCE);
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
	
	@Override
	public void onActivityCreated(Bundle savedState){
		super.onActivityCreated(savedState);
		restartLoader(pmId, null, mPMDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulMessage.CONTENT_URI, true, mPMDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulMessage.CONTENT_URI_REPLY, true, pmReplyObserver);
	}


	private void syncPM() {
		// TODO: rework this so we don't hold onto the activity - AwfulRequest wants to make a Toast so we can't just pass in the app context
		final Activity activity = getActivity();
		if (activity == null) {
			Timber.i("Activity unavailable - abandoning PM load");
			return;
		}
		queueRequest(new PMRequest(activity, pmId).build(this, new AwfulRequest.AwfulResultCallback<Void>() {
            @Override
            public void success(Void result) {
                restartLoader(pmId, null, mPMDataCallback);
                queueRequest(new PMReplyRequest(activity, pmId).build(MessageFragment.this, new AwfulRequest.AwfulResultCallback<Void>() {
                    @Override
                    public void success(Void result) {
                        restartLoader(pmId, null, mPMDataCallback);
                    }

                    @Override
                    public void failure(VolleyError error) {
                        //error is automatically displayed
                    }
                }));
            }

            @Override
            public void failure(VolleyError error) {
                //error is automatically displayed
            }
        }));
	}


    /**
     * Display a dialog allowing the user to send their message
     */
    private void showSubmitDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Send message?")
                .setPositiveButton(R.string.submit,
                        (dialog, button) -> {
                            if (mDialog == null && getActivity() != null) {
                                mDialog = ProgressDialog.show(getActivity(), "Sending", "Hopefully it didn't suck...", true, true);
                            }
                            saveReply();
                            sendPM();
                        })
                .setNegativeButton(R.string.cancel, (dialog, button) -> {
                })
                .show();
    }

	
	public void sendPM() {
        queueRequest(new SendPrivateMessageRequest(getActivity(), pmId).build(this, new AwfulRequest.AwfulResultCallback<Void>() {
            @Override
            public void success(Void result) {
                if(mDialog != null){
                    mDialog.dismiss();
                    mDialog = null;
                }
				getAlertView().setTitle("Message Sent!").setIcon(R.drawable.ic_check_circle).show();
				closeMessage();
            }

            @Override
            public void failure(VolleyError error) {
                if(mDialog != null){
                    mDialog.dismiss();
                    mDialog = null;
                }
				getAlertView().setTitle("Failed to send!").setSubtitle("Draft Saved").show();
            }
        }));
	}


	/**
	 * Close this message, letting the activity handle it
	 */
	private void closeMessage() {
		PrivateMessageCallbacks activity = (PrivateMessageCallbacks) getActivity();
		if (activity != null) {
			activity.onMessageClosed();
		}
	}


	public void saveReply(){
		ContentResolver content = getActivity().getContentResolver();
		ContentValues values = new ContentValues();
		values.put(AwfulMessage.ID, pmId);
		values.put(AwfulMessage.TITLE, mSubject.getText().toString());
		values.put(AwfulMessage.TYPE, AwfulMessage.TYPE_PM);
		values.put(AwfulMessage.RECIPIENT, mRecipient.getText().toString());
		values.put(AwfulMessage.REPLY_CONTENT, messageComposer.getText());
		values.put(AwfulMessage.REPLY_ICON, threadIconPicker.getIcon().iconId);
		if(content.update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY,pmId), values, null, null)<1){
			content.insert(AwfulMessage.CONTENT_URI_REPLY, values);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (messageWebView != null) {
			messageWebView.onResume();
		}
	}
	
	@Override
	public void onPause(){
		super.onPause();
		if(pmId>0){
			saveReply();
		}
		if (messageWebView != null) {
            messageWebView.onPause();
        }
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		getLoaderManager().destroyLoader(pmId);
		getActivity().getContentResolver().unregisterContentObserver(mPMDataCallback);
		getActivity().getContentResolver().unregisterContentObserver(pmReplyObserver);
	}

	@Override
	public void onDetach(){
		super.onDetach();
		if(mDialog!= null){
			mDialog.dismiss();
			mDialog = null;
		}
	}
	
	private void newMessage(){
		getLoaderManager().destroyLoader(pmId);
		pmId = -1;//TODO getNextId();
		recipient = null;
		messageComposer.setText(null, false);
		mUsername.setText("");
		mRecipient.setText("");
		mPostdate.setText("");
		messageWebView.setBodyHtml(null);
		mTitle.setText("New Message");
		mSubject.setText("");
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.hide_message:
				messageWebView.setVisibility(messageWebView.getVisibility() == VISIBLE ? GONE : VISIBLE);
				break;
		}
	}

	@Override
	public void onPreferenceChange(AwfulPreferences prefs, String key) {
		super.onPreferenceChange(mPrefs, key);
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
			// TODO: 05/05/2017 if pmId is negative (i.e. an invalid number) the load will fail - try and avoid doing it?
			Log.i(TAG,"Create PM Cursor:"+pmId);
            return new CursorLoader(getActivity(), 
            						ContentUris.withAppendedId(AwfulMessage.CONTENT_URI, pmId), 
            						AwfulProvider.PMReplyProjection, 
            						null,
            						null,
            						null);
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	//TODO retain info if entered into reply window
			// the Cursor will be null if pmId is negative
			if(aData != null && aData.moveToFirst()){
				Log.v(TAG,"PM load finished, populating: "+aData.getCount());
				if(messageWebView != null){
					messageWebView.setBodyHtml(null);
    			}
        		String title = aData.getString(aData.getColumnIndex(AwfulMessage.TITLE));
        		mTitle.setText(title);
				messageWebView.setBodyHtml(AwfulMessage.getMessageHtml(aData.getString(aData.getColumnIndex(AwfulMessage.CONTENT))));
				mPostdate.setText(aData.getString(aData.getColumnIndex(AwfulMessage.DATE)));
        		String replyTitle = aData.getString(aData.getColumnIndex(AwfulMessage.REPLY_TITLE));
        		String replyContent = aData.getString(aData.getColumnIndex(AwfulMessage.REPLY_CONTENT));
        		if(replyContent != null){
					if (!replyContent.equals(messageComposer.getText())) {
						messageComposer.setText(replyContent, false);
					}
        		}else{
        			messageComposer.setText(null, false);
        		}
        		if(replyTitle != null){
					if (!replyTitle.equals(mSubject.getText().toString())) {
						mSubject.setText(replyTitle);
					}
        		}else{
        			mSubject.setText(title);
        		}
        		String author = aData.getString(aData.getColumnIndex(AwfulMessage.AUTHOR));
				mUsername.setText("Sender: " + author);
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
		}
        
        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        	
        }
        
        @Override
        public void onChange (boolean selfChange){
        	Log.i(TAG,"PM Data update.");
        	restartLoader(pmId, null, this);
        }
    }


	@Override
	public String getTitle() {
		return mTitle.getText().toString();
	}


	@Override
	protected boolean doScroll(boolean down) {
		if (down) {
			messageWebView.pageDown(false);
		} else {
			messageWebView.pageUp(false);
		}
		return true;
	}


}
