package com.ferg.awful;

import android.content.Intent;
import android.os.Bundle;

import com.ferg.awful.constants.Constants;

public class UserCPActivity extends AwfulActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startActivity(new Intent(this, ForumsIndexActivity.class).putExtra(Constants.FORUM_ID, Constants.USERCP_ID));
		finish();
	}

}
