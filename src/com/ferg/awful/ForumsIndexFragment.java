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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.support.v4.app.Fragment;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.dialog.LogOutDialog;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.service.AwfulServiceConnection.ForumListAdapter;

public class ForumsIndexFragment extends Fragment implements AwfulUpdateCallback, OnSharedPreferenceChangeListener {
    private static final String TAG = "ForumsIndex";

    private ImageButton mUserCp;
    private ListView mForumList;
    private TextView mTitle;
    private int mDefaultPostFontColor;
    private int mDefaultPostBackgroundColor;

	private ForumListAdapter adapt;

	private SharedPreferences mPrefs;

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        View result = aInflater.inflate(R.layout.forum_index, aContainer, false);

        mForumList = (ListView) result.findViewById(R.id.forum_list);
        mTitle     = (TextView) result.findViewById(R.id.title);
        mUserCp    = (ImageButton) result.findViewById(R.id.user_cp);
        
        PreferenceManager.setDefaultValues(getActivity(), R.xml.settings, false);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        
        mDefaultPostFontColor = mPrefs.getInt("default_post_font_color", getResources().getColor(R.color.default_post_font));
        mDefaultPostBackgroundColor = mPrefs.getInt("default_post_background_color", getResources().getColor(R.color.background));
        mForumList.setBackgroundColor(mDefaultPostBackgroundColor);
        mForumList.setCacheColorHint(mDefaultPostBackgroundColor);
		if(((ForumsIndexActivity) getActivity()).getServiceConnection() != null){
			adapt = ((ForumsIndexActivity) getActivity()).getServiceConnection().createForumAdapter(0, this);
			mForumList.setAdapter(adapt);
		}
		mForumList.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				Intent viewForum = new Intent().setClass(getActivity(), ForumDisplayActivity.class);
	            viewForum.putExtra(Constants.FORUM, (int) arg3);
	            Log.e(TAG, "Starting ForumDisplay, ID: "+arg3);
	            startActivity(viewForum);
			}
        });
        return result;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

        setHasOptionsMenu(true);
        setRetainInstance(true);
		


        mTitle.setText(getString(R.string.forums_title));
        mUserCp.setOnClickListener(onButtonClick);
    }

    @Override
    public void onStart() {
        super.onStart();

		boolean loggedIn = NetworkUtils.restoreLoginCookies(getActivity());
		if (loggedIn) {
            Log.e(TAG, "Cookie Loaded!");
		} else {
			startActivityForResult(new Intent().setClass(getActivity(), AwfulLoginActivity.class), 0);
		}
    }
    
    @Override
    public void onPause() {
        super.onPause();
    	mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }
    @Override
    public void onResume() {
        super.onResume();
    	mPrefs.registerOnSharedPreferenceChangeListener(this);
    }
        
    @Override
    public void onStop() {
        super.onStop();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// The only activity we call for result is login
    	// Odds are we want to refresh whether or not it was successful
    	
    	//refresh
    	adapt.refresh();
    }

    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.user_cp:
                    startActivity(new Intent().setClass(getActivity(), UserCPActivity.class));
                    break;
            }
        }
    };

    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forum_index_options, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.settings:
    		startActivity(new Intent().setClass(getActivity(), SettingsActivity.class));
    		return true;
    	case R.id.logout:
            new LogOutDialog(getActivity()).show();
            return true;
    	case R.id.refresh:
    		adapt.refresh();
            return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }

	@Override
	public void dataUpdate(boolean pageChange) {
		
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs,
			String key) {
    	if("default_post_font_color".equals(key)) {
    		int newColor = prefs.getInt(key, R.color.default_post_font);
    		if(newColor != mDefaultPostFontColor) {
    			mDefaultPostFontColor = newColor;
    			Log.d(TAG, "invalidating (color)");
    			mForumList.invalidateViews();    			
    		}
    	} else if("default_post_background_color".equals(key)) {
        	int newBackground = prefs.getInt(key, R.color.background);
        	if(newBackground != mDefaultPostBackgroundColor) {
        		mDefaultPostBackgroundColor = newBackground;
                mForumList.setBackgroundColor(mDefaultPostBackgroundColor);
                mForumList.setCacheColorHint(mDefaultPostBackgroundColor);
        		Log.d(TAG, "invalidating (color)");
        		mForumList.invalidateViews(); 
        	}   			
        }
	}
}
