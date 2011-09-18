package com.ferg.awful;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;

public class PrivateMessageActivity extends AwfulActivity {
	
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.user_cp_activity);

        setContentPane();
    }

    public void setContentPane() {
    	if (getSupportFragmentManager().findFragmentById(R.id.ucpcontent) == null) {
	        PrivateMessageListFragment fragment = new PrivateMessageListFragment();
	
	        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
	        transaction.replace(R.id.ucpcontent, fragment);
	        transaction.commit();
    	}
    }

}
