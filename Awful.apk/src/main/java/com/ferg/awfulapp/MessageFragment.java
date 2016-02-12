package com.ferg.awfulapp;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.SettingsActivity;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.PMReplyRequest;
import com.ferg.awfulapp.task.PMRequest;
import com.ferg.awfulapp.task.SendPrivateMessageRequest;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.util.AwfulUtils;

public class MessageFragment extends AwfulFragment implements OnClickListener {

    private static final String TAG = "MessageFragment";
    
	private int pmId = -1;
	private String recipient;
	
	private WebView mDisplayText;
	private EditText mEditReply;
	private Button mReplyButton;
	private ImageButton mHideButton;
	private TextView mUsername;
	private TextView mPostdate;
	private TextView mTitle;
	private EditText mRecipient;
	private EditText mSubject;
	private View mBackground;

	private AwfulPreferences mPrefs;
	
	private ProgressDialog mDialog;

    private Messenger mMessenger = new Messenger(mHandler);
    private PMCallback mPMDataCallback = new PMCallback(mHandler);
    private ContentObserver pmReplyObserver = new ContentObserver(mHandler){
    	@Override
        public void onChange (boolean selfChange){
        	Log.i(TAG,"PM Data update.");
            restartLoader(pmId, null, mPMDataCallback);
        }
    };

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
        
        View result = aInflater.inflate(R.layout.message_view, aContainer, false);
        
        mDisplayText = (WebView) result.findViewById(R.id.messagebody);
        mEditReply = (EditText) result.findViewById(R.id.edit_reply_text);
        mReplyButton = (Button) result.findViewById(R.id.message_reply_button);
        mHideButton = (ImageButton) result.findViewById(R.id.hide_message);
        mReplyButton.setOnClickListener(this);
        mHideButton.setOnClickListener(this);
        mRecipient = (EditText) result.findViewById(R.id.message_user);
        mSubject = (EditText) result.findViewById(R.id.message_subject);
        mUsername = (TextView) result.findViewById(R.id.username);
        mPostdate = (TextView) result.findViewById(R.id.post_date);
        mTitle = (TextView) result.findViewById(R.id.message_title);
        mBackground = result;
        updateColors(result, mPrefs);
		initThreadViewProperties();

        if(pmId <=0){
        	mDisplayText.setVisibility(View.GONE);
        }else{
            syncPM();
        }
        return result;
    }

	private void initThreadViewProperties(){
		mDisplayText.getSettings().setRenderPriority(WebSettings.RenderPriority.LOW);
		mDisplayText.getSettings().setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
		mDisplayText.getSettings().setDefaultFontSize(mPrefs.postFontSizeDip);
		mDisplayText.getSettings().setDefaultFixedFontSize(mPrefs.postFixedFontSizeDip);
		if(AwfulUtils.isLollipop()) {
			mDisplayText.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
		}

		if (!mPrefs.enableHardwareAcceleration) {
			mDisplayText.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
		}
	}
	
	private void updateColors(View v, AwfulPreferences prefs){
        mEditReply.setTextColor(ColorProvider.getTextColor());
        mRecipient.setTextColor(ColorProvider.getTextColor());
        mSubject.setTextColor(ColorProvider.getTextColor());
        mUsername.setTextColor(ColorProvider.getTextColor());
        mPostdate.setTextColor(ColorProvider.getTextColor());
        mTitle.setTextColor(ColorProvider.getTextColor());
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
	public void onActivityCreated(Bundle savedState){
		super.onActivityCreated(savedState);
		restartLoader(pmId, null, mPMDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulMessage.CONTENT_URI, true, mPMDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulMessage.CONTENT_URI_REPLY, true, pmReplyObserver);
	}
	
	@Override
    public void onStart() {
        super.onStart();
    }
	
	private void syncPM() {
        queueRequest(new PMRequest(getActivity(), pmId).build(this, new AwfulRequest.AwfulResultCallback<Void>() {
            @Override
            public void success(Void result) {
                restartLoader(pmId, null, mPMDataCallback);
                queueRequest(new PMReplyRequest(getActivity(), pmId).build(MessageFragment.this, new AwfulRequest.AwfulResultCallback<Void>() {
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
	
	public void sendPM() {
		mDialog = ProgressDialog.show(getActivity(), "Sending", "Hopefully it didn't suck...", true);
		saveReply();
        queueRequest(new SendPrivateMessageRequest(getActivity(), pmId).build(this, new AwfulRequest.AwfulResultCallback<Void>() {
            @Override
            public void success(Void result) {
                if(mDialog != null){
                    mDialog.dismiss();
                    mDialog = null;
                }
                if(getActivity() instanceof MessageDisplayActivity){
                    getActivity().finish();
                    displayAlert("Message Sent!", R.attr.iconMenuLoadSuccess);
                }
            }

            @Override
            public void failure(VolleyError error) {
                if(mDialog != null){
                    mDialog.dismiss();
                    mDialog = null;
                }
                displayAlert("Failed to send!", "Draft Saved");
            }
        }));
	}
	
	public void saveReply(){
		ContentResolver content = getActivity().getContentResolver();
		ContentValues values = new ContentValues();
		values.put(AwfulMessage.ID, pmId);
		values.put(AwfulMessage.TITLE, mSubject.getText().toString());
		values.put(AwfulMessage.TYPE, AwfulMessage.TYPE_PM);
		values.put(AwfulMessage.RECIPIENT, mRecipient.getText().toString());
		values.put(AwfulMessage.REPLY_CONTENT, mEditReply.getText().toString());
		if(content.update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY,pmId), values, null, null)<1){
			content.insert(AwfulMessage.CONTENT_URI_REPLY, values);
		}
	}

	@Override
	public void onResume(){
		super.onResume();
		resumeWebView();

	}
	
	@Override
	public void onPause(){
		super.onPause();
		if(pmId>0){
			saveReply();
		}
        pauseWebView();
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
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.hide_message:
			if(mDisplayText.getVisibility() == View.VISIBLE){
				mDisplayText.setVisibility(View.GONE);
			}else{
				mDisplayText.setVisibility(View.VISIBLE);
			}
			break;
		case R.id.message_reply_button:
			sendPM();
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
    			if(mDisplayText != null){
    				mDisplayText.loadData(getBlankPage(), "text/html", "utf-8");
    			}
        		String title = aData.getString(aData.getColumnIndex(AwfulMessage.TITLE));
        		mTitle.setText(title);
        		mDisplayText.loadDataWithBaseURL(Constants.BASE_URL + "/",AwfulMessage.getMessageHtml(aData.getString(aData.getColumnIndex(AwfulMessage.CONTENT)),mPrefs),"text/html", "utf-8", null);
				mPostdate.setText(aData.getString(aData.getColumnIndex(AwfulMessage.DATE)));
        		String replyTitle = aData.getString(aData.getColumnIndex(AwfulMessage.REPLY_TITLE));
        		String replyContent = aData.getString(aData.getColumnIndex(AwfulMessage.REPLY_CONTENT));
        		if(replyContent != null){
            		mEditReply.setText(replyContent);
        		}else{
        			mEditReply.setText("");
        		}
        		if(replyTitle != null){
        			mSubject.setText(replyTitle);
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
        	aData.close();
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
	public void onPageVisible() {}

	@Override
	public void onPageHidden() {}

	@Override
	public String getTitle() {
		return mTitle.getText().toString();
	}

	@Override
	public String getInternalId() {
		return null;
	}

	@Override
	public boolean volumeScroll(KeyEvent event) {
	    int action = event.getAction();
	    int keyCode = event.getKeyCode();    
        switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_UP:
            if (action == KeyEvent.ACTION_DOWN) {
            	mDisplayText.pageUp(false);   
            }
            return true;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            if (action == KeyEvent.ACTION_DOWN) {
            	mDisplayText.pageDown(false);
            }
            return true;
        default:
            return false;
        }
    }
	
	private String getBlankPage(){
		return "<html><head></head><body style='{background-color:#"+ColorProvider.convertToARGB(ColorProvider.getBackgroundColor())+";'></body></html>";
	}
	
    @SuppressLint("NewApi")
    private void pauseWebView(){
        if (mDisplayText != null) {
        	mDisplayText.pauseTimers();
        	mDisplayText.onPause();
        }
    }

    @SuppressLint("NewApi")
    public void resumeWebView(){
    	if(getActivity() != null){
	        if (mDisplayText == null) {
	            //recreateWebview();
	        }else{
	        	mDisplayText.onResume();
	        	mDisplayText.resumeTimers();
	        }
    	}
    }
}
