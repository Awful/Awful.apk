package com.ferg.awful;

import com.ferg.awful.constants.Constants;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.Window;

public class PrivateMessageActivity extends AwfulActivity {
	private View pane_two;
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
        setContentView(R.layout.fragment_pane);

        pane_two = findViewById(R.id.fragment_pane_two);
        setContentPane();
    }

    public void setContentPane() {
    	if (getSupportFragmentManager().findFragmentById(R.id.fragment_pane) == null) {
	        PrivateMessageListFragment fragment = new PrivateMessageListFragment();
	
	        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
	        transaction.replace(R.id.fragment_pane, fragment);
	        if(pane_two != null){
	        	transaction.replace(R.id.fragment_pane_two, new MessageFragment(null,0));
	        }
	        transaction.commit();
    	}
    }
    
    public void showMessage(String name, int id){
    	if(pane_two != null){
    		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
	        transaction.replace(R.id.fragment_pane_two, new MessageFragment(name,id));
    		transaction.commit();
    	}else{
    		if(name != null){
                startActivity(new Intent().setClass(this, MessageDisplayActivity.class).putExtra(Constants.PARAM_USERNAME, name));
    		}else{
                startActivity(new Intent().setClass(this, MessageDisplayActivity.class).putExtra(Constants.PARAM_PRIVATE_MESSAGE_ID, id));
    		}
    	}
    }

	public void sendMessage() {
		MessageFragment mf = (MessageFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_pane_two);
		if(mf != null){
			mf.sendPM();
		}
	}

}
