/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
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

package com.ferg.awful;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.support.v4.app.ListFragment;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.dialog.LogOutDialog;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.service.AwfulServiceConnection.ForumListAdapter;
import com.ferg.awful.thread.AwfulThread;

public class UserCPFragment extends ListFragment implements AwfulUpdateCallback {
    private static final String TAG = "UserCPActivity";

    private ImageButton mHome;
    private TextView mTitle;
    private ForumListAdapter adapt;
	private SharedPreferences mPrefs;

    @Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
	}
		
    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);
        
        View result = aInflater.inflate(R.layout.user_cp, aContainer, false);

        mTitle      = (TextView) result.findViewById(R.id.title);
        mHome       = (ImageButton) result.findViewById(R.id.home);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.settings, false);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        
        return result;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

        setRetainInstance(true);

        mTitle.setText(getString(R.string.user_cp));
		mHome.setOnClickListener(onButtonClick);

		getListView().setOnItemClickListener(onThreadSelected);
		getListView().setBackgroundColor(mPrefs.getInt("default_post_background_color", getResources().getColor(R.color.background)));
		getListView().setCacheColorHint(mPrefs.getInt("default_post_background_color", getResources().getColor(R.color.background)));

		adapt = ((UserCPActivity) getActivity()).getServiceConnection().createForumAdapter(-1, this);
        setListAdapter(adapt);
    }

    @Override
    public void onStart() {
        super.onStart();
		
        // When coming from the desktop shortcut we won't have login cookies
		boolean loggedIn = NetworkUtils.restoreLoginCookies(getActivity());

		if (!loggedIn) {
			startActivityForResult(new Intent().setClass(getActivity(), AwfulLoginActivity.class), 0);
		}
    }
    
    @Override
    public void onResume() {
        super.onResume();
        adapt.refresh();
    }
        
    @Override
    public void onStop() {
        super.onStop();
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        setListAdapter(null);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	if(menu.size() == 0){
    		inflater.inflate(R.menu.forum_index_options, menu);
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    		case R.id.settings:
    			startActivity(new Intent().setClass(getActivity(), SettingsActivity.class));
    			return true;
            case R.id.logout:
                new LogOutDialog(getActivity()).show();
                break;
            case R.id.refresh:
            	adapt.refresh();
                break;
			default:
				return super.onOptionsItemSelected(item);
    	}

        return true;
    }

    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.home:
                    startActivity(new Intent().setClass(getActivity(), ForumsIndexActivity.class));
                    break;
            }
        }
    };

	private AdapterView.OnItemClickListener onThreadSelected = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            AwfulThread thread = (AwfulThread) getListAdapter().getItem(aPosition);

            Intent viewThread = new Intent().setClass(getActivity(), ThreadDisplayActivity.class);
            viewThread.putExtra(Constants.THREAD, thread.getID());

            startActivity(viewThread);
		}
	};

	@Override
	public void dataUpdate(boolean pageChange) {
		if(pageChange && this.isAdded() && adapt.getCount() >0){
        	getListView().setSelection(0);
        }
	}
}
