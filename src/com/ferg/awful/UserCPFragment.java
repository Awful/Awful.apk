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
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.support.v4.app.DialogFragment;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.dialog.LogOutDialog;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.service.AwfulServiceConnection.ForumListAdapter;
import com.ferg.awful.thread.AwfulDisplayItem;
import com.ferg.awful.thread.AwfulThread;
import com.ferg.awful.thread.AwfulDisplayItem.DISPLAY_TYPE;

public class UserCPFragment extends DialogFragment implements AwfulUpdateCallback {
    private static final String TAG = "UserCPActivity";

    private ImageButton mHome;
    private ImageButton mPrivateMessage;
    private ListView mBookmarkList;
    private TextView mTitle;
    private ForumListAdapter adapt;
    private SharedPreferences mPrefs;
    private ImageButton mRefresh;

    public static UserCPFragment newInstance(boolean aModal) {
        UserCPFragment fragment = new UserCPFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putBoolean(Constants.MODAL, aModal);

        fragment.setArguments(args);

        fragment.setShowsDialog(false);
        fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
        
    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.settings, false);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        
        View result = aInflater.inflate(R.layout.user_cp, aContainer, false);

        mBookmarkList = (ListView) result.findViewById(R.id.bookmark_list);

        if (isHoneycomb()) {
            View actionbar = ((ViewStub) result.findViewById(R.id.actionbar_blank)).inflate();
            mTitle         = (TextView) actionbar.findViewById(R.id.title);
        } else {
            View actionbar = ((ViewStub) result.findViewById(R.id.actionbar)).inflate();
            mHome          = (ImageButton) actionbar.findViewById(R.id.home);
            mPrivateMessage = (ImageButton) actionbar.findViewById(R.id.pm_button);
            mTitle         = (TextView) actionbar.findViewById(R.id.title);
            mRefresh       = (ImageButton) actionbar.findViewById(R.id.refresh);
        }
        
        return result;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

        setRetainInstance(true);

        mTitle.setText(getString(R.string.user_cp));

        if (!isHoneycomb()) {
            mHome.setOnClickListener(onButtonClick);
            mRefresh.setOnClickListener(onButtonClick);
            mPrivateMessage.setOnClickListener(onButtonClick);
        }

        mBookmarkList.setOnItemClickListener(onThreadSelected);
        mBookmarkList.setBackgroundColor(mPrefs.getInt("default_post_background_color", getResources().getColor(R.color.background)));
        mBookmarkList.setCacheColorHint(mPrefs.getInt("default_post_background_color", getResources().getColor(R.color.background)));

        adapt = ((AwfulActivity) getActivity()).getServiceConnection().createForumAdapter(-1, this);
        mBookmarkList.setAdapter(adapt);
        registerForContextMenu(mBookmarkList);
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
        if(mBookmarkList != null){
        	mBookmarkList.setAdapter(null);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu aMenu, View aView, ContextMenuInfo aMenuInfo) {
        super.onCreateContextMenu(aMenu, aView, aMenuInfo);

        MenuInflater inflater = getActivity().getMenuInflater();
        AwfulDisplayItem selected = (AwfulDisplayItem) adapt.getItem(((AdapterContextMenuInfo) aMenuInfo).position);
        
        switch(selected.getType()){
	        case THREAD:
	            inflater.inflate(R.menu.thread_longpress, aMenu);
	        	break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) aItem.getMenuInfo();
    	AwfulThread thread = (AwfulThread) adapt.getItem(info.position);
    	if(thread == null || thread.getType() != DISPLAY_TYPE.THREAD){
    		return false;
    	}
        switch (aItem.getItemId()) {
            case R.id.first_page:
                Intent viewThread = new Intent().setClass(getActivity(), ThreadDisplayActivity.class).putExtra(Constants.THREAD, thread.getID()).putExtra(Constants.PAGE, 1);
                startActivity(viewThread);
                return true;
            case R.id.last_page:
            	Intent viewThread2 = new Intent().setClass(getActivity(), ThreadDisplayActivity.class).putExtra(Constants.THREAD, thread.getID()).putExtra(Constants.PAGE, thread.getLastPage());
            	startActivity(viewThread2);
                return true;
            case R.id.thread_bookmark:
            	adapt.toggleBookmark(thread.getID());
            	adapt.refresh();
                return true;
            case R.id.mark_thread_unread:
            	adapt.markThreadUnread(thread.getID());
            	adapt.refresh();
                return true;
        }

        return false;
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

    private boolean isHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        public void onClick(View aView) {
            switch (aView.getId()) {
                case R.id.home:
                    startActivity(new Intent().setClass(getActivity(), ForumsIndexActivity.class));
                    break;
                case R.id.pm_button:
                    startActivity(new Intent().setClass(getActivity(), PrivateMessageActivity.class));
                    break;
                case R.id.refresh:
                    adapt.refresh();
                    break;
            }
        }
    };

    private AdapterView.OnItemClickListener onThreadSelected = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            AwfulThread thread = (AwfulThread) mBookmarkList.getAdapter().getItem(aPosition);

            Intent viewThread = new Intent().setClass(getActivity(), ThreadDisplayActivity.class);
            viewThread.putExtra(Constants.THREAD, thread.getID());

            startActivity(viewThread);
        }
    };

    @Override
    public void dataUpdate(boolean pageChange, Bundle extras) {
        if(pageChange && this.isAdded() && mBookmarkList!= null && mBookmarkList.getCount() >0){
            mBookmarkList.setSelection(0);
        }
    }

    @Override
    public void loadingFailed() {
        Log.e(TAG, "Loading failed.");
        if (!isHoneycomb()) {
            mRefresh.setVisibility(View.VISIBLE);
            mRefresh.setAnimation(null);
            mRefresh.setImageResource(android.R.drawable.ic_dialog_alert);
            mRefresh.startAnimation(adapt.getBlinkingAnimation());
        }
        if(getActivity() != null){
        	Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void loadingStarted() {
        Log.e(TAG, "Loading started.");
        if (!isHoneycomb()) {
            mRefresh.setVisibility(View.VISIBLE);
            mRefresh.setImageResource(R.drawable.ic_menu_refresh);
            mRefresh.startAnimation(adapt.getRotateAnimation());
        }
    }

    @Override
    public void loadingSucceeded() {
        Log.e(TAG, "Loading succeeded.");
        if (!isHoneycomb()) {
            mRefresh.setAnimation(null);
            mRefresh.setVisibility(View.GONE);
        }
    }

	@Override
	public void onServiceConnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		if(mBookmarkList != null){
			mBookmarkList.setBackgroundColor(prefs.postBackgroundColor);
	        mBookmarkList.setCacheColorHint(prefs.postBackgroundColor);
		}
	}
}
