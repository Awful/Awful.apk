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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
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
import android.widget.Toast;
import android.support.v4.app.ListFragment;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.service.AwfulServiceConnection.AwfulListAdapter;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulThread;
import com.ferg.awful.thread.AwfulDisplayItem.DISPLAY_TYPE;
import com.ferg.awful.widget.NumberPicker;

public class ForumDisplayFragment extends ListFragment implements AwfulUpdateCallback {
    private static final String TAG = "ThreadsActivity";
    
    private AwfulListAdapter adapt;
    private ImageButton mUserCp;
	private ImageButton mNext;
    //private ProgressDialog mDialog;
    private SharedPreferences mPrefs;
    private TextView mTitle;

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        View result = aInflater.inflate(R.layout.forum_display, aContainer, false);

        mTitle      = (TextView) result.findViewById(R.id.title);
        mUserCp     = (ImageButton) result.findViewById(R.id.user_cp);

        return result;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

        setHasOptionsMenu(true);
        setRetainInstance(true);
		
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String c2pForumID = null;
        // we're receiving an intent from an outside link (say, ChromeToPhone). Let's check to see
    	// if we have a URL from such a link.
    	if (getActivity().getIntent().getData() != null && getActivity().getIntent().getData().getScheme().equals("http")) {
    		c2pForumID = getActivity().getIntent().getData().getQueryParameter("forumid");
    	}
    	int id = getActivity().getIntent().getIntExtra(Constants.FORUM, 0);
    	if(c2pForumID != null){
    		id = Integer.parseInt(c2pForumID);
    	}
        adapt = ((ForumDisplayActivity) getActivity()).getServiceConnection().createAdapter(DISPLAY_TYPE.FORUM, id, this);
        setListAdapter(adapt);
        //getListView().setOnScrollListener(new EndlessScrollListener());
        getListView().setOnItemClickListener(onThreadSelected);
        if(adapt.getTitle() != null) {
        	mTitle.setText(Html.fromHtml(adapt.getTitle()));
        }
        
        mUserCp.setOnClickListener(onButtonClick);
    }

    @Override
    public void onStart() {
        super.onStart();
        
        // final ArrayList<AwfulThread> retainedThreadList = (ArrayList<AwfulThread>) getLastNonConfigurationInstance();
        /*final ArrayList<AwfulThread> retainedThreadList = null;

        if (retainedThreadList == null || retainedThreadList.size() == 0) {
        	mFetchTask = new FetchThreadsTask();
        	mFetchTask.execute(mForum.getForumId());
        } else {
            ((ForumArrayAdapter) getListAdapter()).setThreads(retainedThreadList);
        }*/
    }
    
    @Override
    public void onPause() {
        super.onPause();

        //if (mDialog != null) {
        //    mDialog.dismiss();
        //}

        //if (mFetchTask != null) {
        //   mFetchTask.cancel(true);
        //}
    }
    @Override
    public void onResume() {
        super.onResume();
        adapt.refresh();
    }    
    @Override
    public void onStop() {
        super.onStop();

        //if (mDialog != null) {
        //    mDialog.dismiss();
        //}

        //if (mFetchTask != null) {
        //    mFetchTask.cancel(true);
        //}
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        //if (mDialog != null) {
        //    mDialog.dismiss();
        //}

        //if (mFetchTask != null) {
        //    mFetchTask.cancel(true);
        //}
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.forum_display_menu, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    		case R.id.settings:
    			startActivity(new Intent().setClass(getActivity(), SettingsActivity.class));
    			return true;
            case R.id.logout:
                NetworkUtils.clearLoginCookies(getActivity());
                startActivityForResult(new Intent().setClass(getActivity(), AwfulLoginActivity.class), 0);
                return true;
            case R.id.refresh:
                adapt.refresh();
                return true;
            case R.id.go_to:
                final NumberPicker jumpToText = new NumberPicker(getActivity());
                jumpToText.setRange(1, adapt.getLastPage());
                jumpToText.setCurrent(adapt.getPage());
                new AlertDialog.Builder(getActivity())
                    .setTitle("Jump to Page")
                    .setView(jumpToText)
                    .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface aDialog, int aWhich) {
                                try {
                                    int pageInt = jumpToText.getCurrent();
                                    if (pageInt > 0 && pageInt <= adapt.getLastPage()) {
                                    	adapt.goToPage(pageInt);
                                    }
                                } catch (NumberFormatException e) {
                                    Log.d(TAG, "Not a valid number: " + e.toString());
        	                        Toast.makeText(getActivity(),
                                        R.string.invalid_page, Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Log.d(TAG, e.toString());
                                }
                            }
                        })
                    .setNegativeButton("Cancel", null)
                    .show();
                return true;
			default:
				return super.onOptionsItemSelected(item);
    	}
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


	private AdapterView.OnItemClickListener onThreadSelected = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> aParent, View aView, int aPosition, long aId) {
            
            switch(adapt.getItemType(aPosition)) {
            case THREAD:
            	AwfulThread thread = (AwfulThread) adapt.getItem(aPosition);
                Intent viewThread = new Intent().setClass(getActivity(), ThreadDisplayActivity.class);
                viewThread.putExtra(Constants.THREAD, thread.getID());
                startActivity(viewThread);
                break;
                
            case FORUM:
            	AwfulForum forum = (AwfulForum) adapt.getItem(aPosition);
                Intent viewForum = new Intent().setClass(getActivity(), ForumDisplayActivity.class);
                viewForum.putExtra(Constants.FORUM, forum.getID());
                startActivity(viewForum);
                break;
            }
            
		}
	};
	/*
    private class EndlessScrollListener implements OnScrollListener {
    	private int visibleThreshold = 5;
    	private int currentPage = 0;
    	private int previousTotal = 0;
    	private boolean loading = true;
    	
    	public EndlessScrollListener() {
    	}
    	
    	public void onScroll(AbsListView view, int firstVisibleItem,
    			int visibleItemCount, int totalItemCount) {
    		if (loading) {
    			if (totalItemCount > previousTotal) {
    				loading = false;
    				previousTotal = totalItemCount;
    				currentPage++;
    			}
    		}
    		
    		if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
    			//load new items in background and add them
    			loading = true;
				if (adapt.getPage() != adapt.getLastPage()) {
					adapt.goToPage(adapt.getPage() + 1);
					//TODO new FetchThreadsTask(mForum.getCurrentPage()).execute(mForum.getForumId());
				}
    		}
    	}

    	public void onScrollStateChanged(AbsListView view, int scrollState) {
    		
    	}
    }*/

	@Override
	public void dataUpdate() {
        mTitle.setText(Html.fromHtml(adapt.getTitle()));
	}
}
