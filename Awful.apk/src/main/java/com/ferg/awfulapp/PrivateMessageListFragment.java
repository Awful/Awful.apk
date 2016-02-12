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


import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.SettingsActivity;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.service.AwfulCursorAdapter;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.PMListRequest;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.util.AwfulUtils;
import android.support.design.widget.FloatingActionButton;

public class PrivateMessageListFragment extends AwfulFragment implements SwipeRefreshLayout.OnRefreshListener {
	

    private static final String TAG = "PrivateMessageList";

    private ListView mPMList;

	private AwfulCursorAdapter mCursorAdapter;
    private PMIndexCallback mPMDataCallback = new PMIndexCallback(mHandler);

    private SwipeRefreshLayout mSRL;

    private Toolbar mToolbar;

    private FloatingActionButton mFAB;
    
    private int currentFolder = FOLDER_INBOX;

    public final static int FOLDER_INBOX	= 0;
    public final static int FOLDER_SENT		= -1;
    
    
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(Activity aActivity) {
    	super.onAttach(aActivity);
    }
    
    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        mPrefs = AwfulPreferences.getInstance(this.getActivity());
        
        View result = aInflater.inflate(R.layout.private_message_fragment, aContainer, false);



        mToolbar = (Toolbar) result.findViewById(R.id.awful_toolbar_pm);
        this.getAwfulActivity().setSupportActionBar(mToolbar);
        this.getAwfulActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mPMList = (ListView) result.findViewById(R.id.message_listview);


        mFAB  = (FloatingActionButton) result.findViewById(R.id.just_pm);
        mFAB.setOnClickListener(onButtonClick);
        mFAB.setVisibility((mPrefs.noFAB ? View.GONE : View.VISIBLE));

        return result;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSRL = (SwipeRefreshLayout) view.findViewById(R.id.pm_swipe);
        mSRL.setOnRefreshListener(this);
        mSRL.setColorSchemeResources(
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light,
                android.R.color.holo_blue_bright);
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);


        mPMList.setOnItemClickListener(onPMSelected);
        
        mCursorAdapter = new AwfulCursorAdapter((AwfulActivity) getActivity(), null, this);
        mPMList.setAdapter(mCursorAdapter);
    }
    
    @Override
    public void onStart(){
    	super.onStart();
		restartLoader(Constants.PRIVATE_MESSAGE_THREAD, null, mPMDataCallback);
        getActivity().getContentResolver().registerContentObserver(AwfulForum.CONTENT_URI, true, mPMDataCallback);
        syncPMs();
        setTitle(getTitle());
    }
    
    private void syncPMs() {
    	if(getActivity() != null){
            queueRequest(new PMListRequest(getActivity(), currentFolder).build(this, new AwfulRequest.AwfulResultCallback<Void>() {
                @Override
                public void success(Void result) {
                    restartLoader(Constants.PRIVATE_MESSAGE_THREAD, null, mPMDataCallback);
                    mSRL.setRefreshing(false);
                    mPMList.setSelectionAfterHeaderView();
                }

                @Override
                public void failure(VolleyError error) {
                    if(null != error.getMessage() && error.getMessage().startsWith("java.net.ProtocolException: Too many redirects")){
                        Log.e(TAG, "Error: "+error.getMessage());
                        Log.e(TAG, "!!!Failed to sync PMs - You are now LOGGED OUT");
                        NetworkUtils.clearLoginCookies(getAwfulActivity());
                        getAwfulActivity().startActivity(new Intent().setClass(getAwfulActivity(), AwfulLoginActivity.class));
                    }
                    mSRL.setRefreshing(false);
                    //The error is already passed to displayAlert by the request framework.
                }
            }));
    	}
	}

	@Override
    public void onResume() {
        super.onResume();
    }
	
	@Override
	public void onStop(){
		super.onStop();
		getActivity().getSupportLoaderManager().destroyLoader(Constants.PRIVATE_MESSAGE_THREAD);
		getActivity().getContentResolver().unregisterContentObserver(mPMDataCallback);
	}
    
    @Override
    public void onDetach() {
        super.onDetach();
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(menu.size() == 0){
            inflater.inflate(R.menu.private_message_menu, menu);
        }

        MenuItem newPM = menu.findItem(R.id.new_pm);
        if(null != newPM){
            newPM.setVisible(mPrefs.noFAB);
        }
        MenuItem sendPM = menu.findItem(R.id.send_pm);
        if(null != sendPM){
            sendPM.setVisible(AwfulUtils.isTablet(getActivity()));
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.new_pm:
        	if(getActivity() instanceof PrivateMessageActivity){
                ((PrivateMessageActivity) getActivity()).showMessage(null, 0);
        	}
        	break;
        case R.id.send_pm:
        	if(getActivity() instanceof PrivateMessageActivity){
                ((PrivateMessageActivity) getActivity()).sendMessage();
        	}
        	break;
        case R.id.refresh:
        	syncPMs();
        	break;
        case R.id.toggle_folder:
        	currentFolder = (currentFolder==FOLDER_INBOX) ? FOLDER_SENT : FOLDER_INBOX;
            setTitle(getTitle());
            changeIcon(item);
        	syncPMs();
        	break;
        case R.id.settings:
        	startActivity(new Intent().setClass(getActivity(), SettingsActivity.class));
        	break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void changeIcon(MenuItem item) {
        int[] attrs;
        if(currentFolder == FOLDER_SENT){
            attrs = new int[]{ R.attr.iconMenuInboxSmall };
        }else{
            attrs = new int[]{ R.attr.iconMenuOutbox };
        }
        TypedArray ta = getView().getContext().getTheme().obtainStyledAttributes(attrs);
        item.setIcon(ta.getDrawable(0));
    }

    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.just_pm:
                    if(getActivity() instanceof PrivateMessageActivity){
                        ((PrivateMessageActivity) getActivity()).showMessage(null, 0);
                    }
                    break;
                case R.id.new_pm:
                    startActivity(new Intent().setClass(getActivity(), MessageDisplayActivity.class));
                    break;
                case R.id.refresh:
                	syncPMs();
                    break;
            }
        }
    };
    
    private AdapterView.OnItemClickListener onPMSelected = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            if(getActivity() instanceof PrivateMessageActivity){
            	((PrivateMessageActivity) getActivity()).showMessage(null, (int)aId);
            }else{
            	startActivity(new Intent(getActivity(), MessageDisplayActivity.class).putExtra(Constants.PARAM_PRIVATE_MESSAGE_ID, (int) aId));
            }
        }
    };

	@Override
	public void onPreferenceChange(AwfulPreferences mPrefs, String key) {
        super.onPreferenceChange(mPrefs, key);
        if("no_fab".equals(key)){
            mFAB.setVisibility((mPrefs.noFAB ? View.GONE : View.VISIBLE));
            invalidateOptionsMenu();
        }
	}
	private class PMIndexCallback extends ContentObserver implements LoaderManager.LoaderCallbacks<Cursor> {
        public PMIndexCallback(Handler handler) {
			super(handler);
		}

		public Loader<Cursor> onCreateLoader(int aId, Bundle aArgs) {
			Log.i(TAG,"Load PM Cursor.");
			return new CursorLoader(getActivity(), 
					AwfulMessage.CONTENT_URI, 
					AwfulProvider.PMProjection, 
					AwfulMessage.FOLDER+"=?", 
					AwfulProvider.int2StrArray(currentFolder),
					AwfulMessage.ID+" DESC");
        }

        public void onLoadFinished(Loader<Cursor> aLoader, Cursor aData) {
        	Log.v(TAG,"PM load finished, populating: "+aData.getCount());
        	mCursorAdapter.swapCursor(aData);
        }
        
        @Override
        public void onLoaderReset(Loader<Cursor> aLoader) {
        	mCursorAdapter.swapCursor(null);
        }
        
        @Override
        public void onChange (boolean selfChange){
        	Log.i(TAG,"PM Data update.");
        	restartLoader(Constants.PRIVATE_MESSAGE_THREAD, null, this);
        }
    }
	@Override
	public void onPageVisible() {
	}

	@Override
	public void onPageHidden() {
	}

	@Override
	public String getTitle() {
        switch (currentFolder){
            case FOLDER_INBOX:
                return "PM - Inbox";
            case FOLDER_SENT:
                return "PM - Sent";
        }
		return "Private Messages";
	}
	
	@Override
	public String getInternalId() {
		return TAG;
	}

	@Override
	public boolean volumeScroll(KeyEvent event) {
		// I have no idea where this fragment is coming from, not the FIA anyway
		return false;
	}

	@Override
	public void onRefresh() {
    	syncPMs();
	}
}
