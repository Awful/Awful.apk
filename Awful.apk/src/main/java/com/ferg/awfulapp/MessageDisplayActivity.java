package com.ferg.awfulapp;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;

import com.ferg.awfulapp.constants.Constants;

public class MessageDisplayActivity extends AwfulActivity implements MessageFragment.PrivateMessageCallbacks {

    Toolbar mToolbar;

	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.private_message_activity);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        setUpActionBar();
        setActionbarTitle("Message");
        setContentPane();
    }

    public void setContentPane() {
    	if (getSupportFragmentManager().findFragmentById(R.id.fragment_pane) == null) {
	        MessageFragment fragment = new MessageFragment(getIntent().getStringExtra(Constants.PARAM_USERNAME),getIntent().getIntExtra(Constants.PARAM_PRIVATE_MESSAGE_ID, 0));
	
	        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
	        transaction.replace(R.id.fragment_pane, fragment);
	        transaction.commit();
    	}
    }


    @Override
    public void onMessageClosed() {
        finish();
    }
}
