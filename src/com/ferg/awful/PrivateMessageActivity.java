package com.ferg.awful;

import com.ferg.awful.constants.Constants;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.*;

public class PrivateMessageActivity extends AwfulActivity {
	private View pane_two;
    private String pmIntentID;
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
        if (getIntent().getData() != null && getIntent().getData().getScheme().equals("http")) {
        	pmIntentID = getIntent().getData().getQueryParameter(Constants.PARAM_PRIVATE_MESSAGE_ID);
        }
        pane_two = findViewById(R.id.fragment_pane_two);
        setContentPane();
    }

    public void setContentPane() {
    	if (getSupportFragmentManager().findFragmentById(R.id.fragment_pane) == null) {
	        PrivateMessageListFragment fragment = new PrivateMessageListFragment();
	
	        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
	        transaction.replace(R.id.fragment_pane, fragment);
	        int pmid = 0;
	        if(pmIntentID != null && pmIntentID.matches("\\d+")){
        		pmid = Integer.parseInt(pmIntentID);
        	}
	        if(pane_two != null){
		        transaction.replace(R.id.fragment_pane_two, new MessageFragment(null,pmid));
	        }else{
	        	if(pmid > 0){//this should only trigger if we intercept 'private.php?pmid=XXXXX' and we are not on a tablet.
	        		startActivity(new Intent().setClass(this, MessageDisplayActivity.class).putExtra(Constants.PARAM_PRIVATE_MESSAGE_ID, pmid));
	        	}
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                returnHome();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void returnHome() {
        finish();
        Intent i = new Intent(this, ForumsIndexActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }
}
