package com.ferg.awful;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.dialog.LogOutDialog;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.service.AwfulServiceConnection.GenericListAdapter;
import com.ferg.awful.thread.AwfulMessage;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MessageFragment extends Fragment implements AwfulUpdateCallback, OnClickListener {

	private GenericListAdapter mServConn;
	
	private int pmId;
	private String recipient;
	private boolean replyLoaded = false;
	private AwfulMessage message;
	
	private TextView mDisplayText;
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

	
	public MessageFragment() {
	}
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
        
        
        mDisplayText = (TextView) result.findViewById(R.id.messagebody);
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
        
        mServConn = ((AwfulActivity) getActivity()).getServiceConnection().createGenericAdapter(Constants.PRIVATE_MESSAGE, pmId, this);

        return result;
    }
	
	private void updateColors(View v, AwfulPreferences prefs){
        mEditReply.setBackgroundColor(prefs.postBackgroundColor2);
        mRecipient.setBackgroundColor(prefs.postBackgroundColor2);
        mSubject.setBackgroundColor(prefs.postBackgroundColor2);
        mDisplayText.setBackgroundColor(prefs.postBackgroundColor);
        mDisplayText.setTextColor(prefs.postFontColor);
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
            		mServConn.fetchPrivateMessage(pmId);
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

        if (isHoneycomb()) {
            setActionBar();
        }
    }
	
	public void onResume(){
		super.onResume();
		message = mServConn.getMessage(pmId);
		if(pmId > 0){
			mServConn.fetchPrivateMessage(pmId);
		}
		updateUI();
	}
	
	public void onStop(){
		super.onStop();
		
	}
	
	public void onDetach(){
		super.onDetach();
		if(mEditReply != null && mSubject != null && mRecipient != null && ((message != null && message.isLoaded()) || pmId == 0)){
			saved_reply = mEditReply.getText();
			saved_title = mSubject.getText();
			saved_recipient = mRecipient.getText();
		}
		if(mPrefs != null){
			mPrefs.unRegisterListener();
		}
		if(mDialog!= null){
			mDialog.dismiss();
			mDialog = null;
		}
	}

	@Override
	public void dataUpdate(boolean pageChange, Bundle extras) {
		if(extras != null && extras.getBoolean(Constants.PARAM_MESSAGE)){
			if(mDialog!= null){
				mDialog.dismiss();
				mDialog = null;
			}
			if(getActivity() != null){
				Toast.makeText(getActivity(), "Message Sent!", Toast.LENGTH_LONG).show();
				if(getActivity() instanceof MessageDisplayActivity){
					getActivity().finish();
				}
			}
			
		}
		message = mServConn.getMessage(pmId);
		updateUI();
	}
	
	private void newMessage(){
		pmId = 0;
		recipient = null;
		saved_reply = null;
		saved_title = null;
		saved_recipient = null;
		mEditReply.setText("");
		mUsername.setText("");
		mRecipient.setText("");
		mPostdate.setText("");
		mEditReply.setText("");
		mDisplayText.setText("");
		mTitle.setText("New Message");
		mSubject.setText("");
	}
	
	private void updateUI(){
		if(message != null){//we are making a reply
			if(message.getTitle() != null){
				mTitle.setText(Html.fromHtml(message.getTitle()));
				mSubject.setText(message.getTitle());
			}
			if(message.getContent() != null){
				mDisplayText.setText(Html.fromHtml(message.getContent()));
				mPostdate.setText(" on " + message.getDate());
			}
			if(message.getReplyText() != null && !replyLoaded && saved_reply == null){
				mEditReply.setText(message.getReplyText());
				mSubject.setText(message.getReplyTitle());
				replyLoaded = true;
			}
			if(message.getAuthor() != null){
				mUsername.setText("Posted by " + message.getAuthor());
				mRecipient.setText(message.getAuthor());
			}
		}else{
			if(recipient != null){
				mRecipient.setText(recipient);
			}
			mTitle.setText("New Message");
		}
		if(saved_reply != null && saved_title != null && saved_recipient != null){
			mEditReply.setText(saved_reply);
			mSubject.setText(saved_title);
			mRecipient.setText(saved_recipient);
		}
	}

	@Override
	public void loadingFailed() {
		if(getActivity() != null){
			if (isHoneycomb()) {
				getActivity().setProgressBarIndeterminateVisibility(false);
			}
		    Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void loadingStarted() {
		if (isHoneycomb() && getActivity() != null) {
			getActivity().setProgressBarIndeterminateVisibility(true);
		}
	}

	@Override
	public void loadingSucceeded() {
		if (isHoneycomb() && getActivity() != null) {
			getActivity().setProgressBarIndeterminateVisibility(false);
		}
	}

	@Override
	public void onClick(View v) {
		sendPM();
	}
	
	public void sendPM(){
		mServConn.sendPM(mRecipient.getText().toString(), pmId, mSubject.getText().toString(), mEditReply.getText().toString());
		mDialog = ProgressDialog.show(getActivity(), "Sending", "Hopefully it didn't suck...", true);
	}

	@Override
	public void onServiceConnected() {
		if(pmId >0){
    		mServConn.fetchPrivateMessage(pmId);
    	}
	}

	private boolean isHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
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
}
