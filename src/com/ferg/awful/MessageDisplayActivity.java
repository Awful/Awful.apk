package com.ferg.awful;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.service.AwfulServiceConnection.GenericListAdapter;
import com.ferg.awful.thread.AwfulMessage;

import android.app.ActionBar;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MessageDisplayActivity extends AwfulActivity implements
		AwfulUpdateCallback, OnClickListener {
	
	private GenericListAdapter mServConn;
	private int pmId;
	private AwfulMessage message;
	private TextView mDisplayText;
	private EditText mEditReply;
	private Button mReplyButton;
	private TextView mUsername;
	private TextView mPostdate;
	private TextView mTitle;
	
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        if (isHoneycomb()) {
            requestWindowFeature(Window.FEATURE_ACTION_BAR);
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        
        setContentView(R.layout.message_view);
        
        mDisplayText = (TextView) findViewById(R.id.messagebody);
        mEditReply = (EditText) findViewById(R.id.edit_reply_text);
        mReplyButton = (Button) findViewById(R.id.message_reply_button);
        mReplyButton.setOnClickListener(this);
        mUsername = (TextView) findViewById(R.id.username);
        mPostdate = (TextView) findViewById(R.id.post_date);
        mTitle = (TextView) findViewById(R.id.message_title);
        
        pmId = getIntent().getIntExtra(Constants.PRIVATE_MESSAGE, 0);
        mServConn = getServiceConnection().createGenericAdapter(Constants.PRIVATE_MESSAGE, pmId, this);
    }
	
	private void setActionBar() {
        ActionBar action = getActionBar();
        action.setBackgroundDrawable(getResources().getDrawable(R.drawable.bar));
        action.setDisplayHomeAsUpEnabled(true);
    }
	
	@Override
    protected void onStart() {
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
	
	protected void onStop(){
		super.onStop();
		
	}

	@Override
	public void dataUpdate(boolean pageChange) {
		message = mServConn.getMessage(pmId);
		updateUI();
	}
	
	private void updateUI(){
		if(message != null){
			if(message.getTitle() != null){
				mTitle.setText(Html.fromHtml(message.getTitle()));
			}
			if(message.getContent() != null){
				mDisplayText.setText(Html.fromHtml(message.getContent()));
				mPostdate.setText(message.getDate());
			}else{
				mDisplayText.setText("Loading...");
			}
			mUsername.setText(message.getAuthor());
			if(message.getReplyText() != null){
				mEditReply.setText(message.getReplyText());
			}else{
				mEditReply.setText("Loading...");
			}
		}
	}

	@Override
	public void loadingFailed() {
		Toast.makeText(this, "Message Load Failed.", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void loadingStarted() {
		Toast.makeText(this, "Message Load Started.", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void loadingSucceeded() {
		Toast.makeText(this, "Message Load Succeeded.", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onClick(View v) {
		Toast.makeText(this, "TODO SEND SHIT", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onServiceConnected() {
		mServConn.fetchPrivateMessage(pmId);
	}

}
