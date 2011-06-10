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
import android.os.Bundle;
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
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.service.AwfulServiceConnection.ForumListAdapter;

public class ForumsIndexFragment extends Fragment implements AwfulUpdateCallback {
    private static final String TAG = "ForumsIndex";

    private ImageButton mUserCp;
    private ListView mForumList;
    private TextView mTitle;

	private ForumListAdapter adapt;

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        View result = aInflater.inflate(R.layout.forum_index, aContainer, false);

        mForumList = (ListView) result.findViewById(R.id.forum_list);
        mTitle     = (TextView) result.findViewById(R.id.title);
        mUserCp    = (ImageButton) result.findViewById(R.id.user_cp);
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

    

	/*private ExpandableListView.OnChildClickListener onForumSelected = new ExpandableListView.OnChildClickListener() {
		public boolean onChildClick(ExpandableListView aParent, View aView, int aGroupPosition, 
                int aChildPosition, long aId) 
        {
            AwfulForumAdapter adapter = (AwfulForumAdapter) mForumList.getExpandableListAdapter();
            AwfulForum forum = (AwfulForum) adapter.getChild(aGroupPosition, aChildPosition);

            Intent viewForum = new Intent().setClass(getActivity(), ForumDisplayActivity.class);
            viewForum.putExtra(Constants.FORUM, forum.getID());

            startActivity(viewForum);

            return true;
		}
	};*/

    /*public class AwfulForumAdapter extends BaseExpandableListAdapter {
        private ArrayList<AwfulForum> mForums;
        private LayoutInflater mInflater;

        public AwfulForumAdapter(Context aContext, ArrayList<AwfulForum> aForums) {
            mInflater     = LayoutInflater.from(aContext);
            mForums       = aForums;
        }

        @Override
        public View getChildView(int aGroupPosition, int aChildPosition, boolean isLastChild, 
                View aConvertView, ViewGroup aParent) 
        {
            View inflatedView = aConvertView;

            if (inflatedView == null) {
                inflatedView = mInflater.inflate(R.layout.subforum_item, null);
            }

			AwfulForum current = (AwfulForum) getChild(aGroupPosition, aChildPosition);

			TextView title   = (TextView) inflatedView.findViewById(R.id.title);
			title.setText(Html.fromHtml(current.getTitle()));

            return inflatedView;
        }

        @Override
        public View getGroupView(int aGroupPosition, boolean isExpanded, View aConvertView, ViewGroup aParent) {
            View inflatedView = aConvertView;

            if (inflatedView == null) {
                inflatedView = mInflater.inflate(R.layout.forum_item, null);
            }

			final AwfulForum current = (AwfulForum) getGroup(aGroupPosition);

			TextView title            = (TextView) inflatedView.findViewById(R.id.title);
			TextView subtext          = (TextView) inflatedView.findViewById(R.id.subtext);

			title.setText(Html.fromHtml(current.getTitle()));
			subtext.setText(current.getSubtext());

            inflatedView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View aView) {
                    Intent viewForum = new Intent().setClass(getActivity(), 
                        ForumDisplayActivity.class);
                    viewForum.putExtra(Constants.FORUM, current.getID());

                    startActivity(viewForum);
                }
            });

            return inflatedView;
        }

        public Object getChild(int aGroupPosition, int aChildPosition) {
            return mForums.get(aGroupPosition).getSubforums().get(aChildPosition);
        }

        public long getChildId(int aGroupPosition, int aChildPosition) {
            return aChildPosition;
        }

        public int getChildrenCount(int aGroupPosition) {
            return mForums.get(aGroupPosition).getSubforums().size();
        }

        public Object getGroup(int aGroupPosition) {
            return mForums.get(aGroupPosition);
        }

        public long getGroupId(int aGroupPosition) {
            return aGroupPosition;
        }

        public int getGroupCount() {
            return mForums.size();
        }

        public boolean isChildSelectable(int aGroupPosition, int aChildPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return true;
        }
    }*/
    
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
    		NetworkUtils.clearLoginCookies(getActivity());
    		startActivityForResult(new Intent().setClass(getActivity(), AwfulLoginActivity.class), 0);
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
}
