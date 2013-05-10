/********************************************************************************
 * Copyright (c) 2012, Matthew Shepard
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the software nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.ferg.awfulapp;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;

public class PrivateMessageActivity extends AwfulActivity {
	private View pane_two;
    private String pmIntentID;
    private TextView mTitleView;
    private AwfulPreferences mPrefs;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.fragment_pane);
        mPrefs = new AwfulPreferences(this, this);
        
        ActionBar action = getSupportActionBar();
        if(action != null){
            action.setCustomView(R.layout.actionbar_title);
            mTitleView = (TextView) action.getCustomView();
            mTitleView.setMovementMethod(new ScrollingMovementMethod());
	        action.setBackgroundDrawable(new ColorDrawable(mPrefs.actionbarColor));
	        mTitleView.setTextColor(mPrefs.actionbarFontColor);
	        mTitleView.setText("Awful - Private Messages");//TODO move to r.string
	        action.setDisplayHomeAsUpEnabled(true);
	        action.setDisplayShowCustomEnabled(true);
        }

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
	        if (pane_two == null) {
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
