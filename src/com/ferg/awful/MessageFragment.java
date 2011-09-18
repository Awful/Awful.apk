package com.ferg.awful;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.service.AwfulServiceConnection.GenericListAdapter;
import com.ferg.awful.thread.AwfulMessage;

import android.app.ActionBar;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Html;
import android.view.LayoutInflater;
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

	private AwfulPreferences mPrefs;

	private Editable saved_reply;

	private boolean sending;
	
	public MessageFragment() {
	}
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
        mPrefs = new AwfulPreferences(this.getActivity());
        
        setRetainInstance(true);
        
        View result = aInflater.inflate(R.layout.message_view, aContainer, false);
        mPrefs = new AwfulPreferences(getActivity());
        
        
        mDisplayText = (TextView) result.findViewById(R.id.messagebody);
        mEditReply = (EditText) result.findViewById(R.id.edit_reply_text);
        mReplyButton = (Button) result.findViewById(R.id.message_reply_button);
        mReplyButton.setOnClickListener(this);
        mRecipient = (EditText) result.findViewById(R.id.message_user);
        mSubject = (EditText) result.findViewById(R.id.message_subject);
        mUsername = (TextView) result.findViewById(R.id.username);
        mPostdate = (TextView) result.findViewById(R.id.post_date);
        mTitle = (TextView) result.findViewById(R.id.message_title);

        result.setBackgroundColor(mPrefs.postBackgroundColor);
        mDisplayText.setBackgroundColor(mPrefs.postBackgroundColor);
        mDisplayText.setTextColor(mPrefs.postFontColor);
        
        mServConn = ((AwfulActivity) getActivity()).getServiceConnection().createGenericAdapter(Constants.PRIVATE_MESSAGE, pmId, this);

        return result;
    }
	
	private void setActionBar() {
        ActionBar action = getActivity().getActionBar();
        action.setBackgroundDrawable(getResources().getDrawable(R.drawable.bar));
        action.setDisplayHomeAsUpEnabled(true);
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
		updateUI();
	}
	
	public void onStop(){
		super.onStop();
		
	}
	
	public void onDetach(){
		super.onDetach();
		saved_reply = mEditReply.getText();
		mPrefs.unRegisterListener();
	}

	@Override
	public void dataUpdate(boolean pageChange, Bundle extras) {
		if(extras != null && extras.getBoolean(Constants.PARAM_MESSAGE)){
			Toast.makeText(getActivity(), "Message Sent!", Toast.LENGTH_LONG).show();
			getActivity().finish();
		}
		message = mServConn.getMessage(pmId);
		updateUI();
	}
	
	private void updateUI(){
		if(message != null){//we are making a reply
			if(message.getTitle() != null){
				mTitle.setText(Html.fromHtml(message.getTitle()));
				mSubject.setText(message.getTitle());
			}
			if(message.getContent() != null){
				mDisplayText.setText(Html.fromHtml(message.getContent()));
				mPostdate.setText(message.getDate());
			}
			//TODO This isn't rotation-safe, but I'm going to refactor into a fragment next.
			if(message.getReplyText() != null && !replyLoaded && saved_reply == null){
				mEditReply.setText(message.getReplyText());
				replyLoaded = true;
			}
			if(saved_reply != null){
				mEditReply.setText(saved_reply);
			}
			if(message.getAuthor() != null){
				mUsername.setText(message.getAuthor());
				mRecipient.setText(message.getAuthor());
			}
		}else{
			if(mRecipient != null){
				mRecipient.setText(recipient);
			}
		}
	}

	@Override
	public void loadingFailed() {
	}

	@Override
	public void loadingStarted() {
	}

	@Override
	public void loadingSucceeded() {
	}

	@Override
	public void onClick(View v) {
		mServConn.sendPM(mRecipient.getText().toString(), pmId, mSubject.getText().toString(), mEditReply.getText().toString());
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
}
