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

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.ListFragment;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.reply.Reply;
import com.ferg.awful.service.AwfulServiceConnection.AwfulListAdapter;
import com.ferg.awful.thread.AwfulPost;
import com.ferg.awful.thread.AwfulDisplayItem.DISPLAY_TYPE;
import com.ferg.awful.thread.AwfulThread;
import com.ferg.awful.widget.NumberPicker;

public class ThreadDisplayFragment extends ListFragment implements OnSharedPreferenceChangeListener, AwfulUpdateCallback {
    private static final String TAG = "ThreadDisplayActivity";

	private AwfulListAdapter adapt;
    private ParsePostQuoteTask mPostQuoteTask;
    private ParseEditPostTask mEditPostTask;
    private MarkLastReadTask mMarkLastReadTask;

	private ImageButton mNext;
	private ImageButton mReply;
	private ProgressDialog mDialog;
    private SharedPreferences mPrefs;
    private TextView mTitle;

    // These just store values from shared preferences. This way, we only have to do redraws
    // and the like if the preferences defining drawing have actually changed since we last
    // saw them
    private int mDefaultPostFontSize;
    private int mDefaultPostFontColor;

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);

        View result = aInflater.inflate(R.layout.thread_display, aContainer, false);
        
        mTitle    = (TextView) result.findViewById(R.id.title);
        mNext     = (ImageButton) result.findViewById(R.id.next_page);
        mReply    = (ImageButton) result.findViewById(R.id.reply);

        return result;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);

        setHasOptionsMenu(true);
        setRetainInstance(true);
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mDefaultPostFontSize = mPrefs.getInt("default_post_font_size", 15);
        mDefaultPostFontColor = mPrefs.getInt("default_post_font_color", getResources().getColor(R.color.default_post_font));

        registerForContextMenu(getListView());

		mNext.setOnClickListener(onButtonClick);
		mReply.setOnClickListener(onButtonClick);
    }

    @Override
    public void onStart() {
        super.onStart();
        String c2pThreadID = null;
        String c2pPage = null;
        // We may be getting thread info from ChromeToPhone so handle that here
        if (getActivity().getIntent().getData() != null && getActivity().getIntent().getData().getScheme().equals("http")) {
        	c2pThreadID = getActivity().getIntent().getData().getQueryParameter("threadid");
            c2pPage = getActivity().getIntent().getData().getQueryParameter("pagenumber");
        }
        int threadid = getActivity().getIntent().getIntExtra(Constants.THREAD, 0);
        if(c2pThreadID != null){
        	threadid = Integer.parseInt(c2pThreadID);
        }
        adapt = ((ThreadDisplayActivity) getActivity()).getServiceConnection().createAdapter(DISPLAY_TYPE.THREAD, threadid, this);
        //ThumbnailAdapter imgadapt = new ThumbnailAdapter(getActivity(), adapt, ((AwfulApplication) getActivity().getApplication()).getImageCache(), new int[] {R.id.avatar});//new int[] {R.id.avatar}
        if(c2pPage != null){
    		adapt.goToPage(Integer.parseInt(c2pPage));
    	}
        setListAdapter(adapt);
        mTitle.setText(Html.fromHtml(adapt.getTitle()));
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    	if("default_post_font_size".equals(key)) {
    		int newSize = prefs.getInt(key, 15);
    		if(newSize != mDefaultPostFontSize) {
    			mDefaultPostFontSize = newSize;
    			Log.d(TAG, "invalidating (size)");
    			getListView().invalidateViews();   			
    		}
    	} else if("default_post_font_color".equals(key)) {
    		int newColor = prefs.getInt(key, R.color.default_post_font);
    		if(newColor != mDefaultPostFontColor) {
    			mDefaultPostFontColor = newColor;
    			Log.d(TAG, "invalidating (color)");
    			getListView().invalidateViews();    			
    		}
    	} else if("use_large_scrollbar".equals(key)) {
    		setScrollbarType();
    	}
    }
    
    private void setScrollbarType() {
    	getListView().setFastScrollEnabled(mPrefs.getBoolean("use_large_scrollbar", true));
    }
    
    @Override
    public void onPause() {
        super.onPause();

        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        cleanupTasks();
    }
        
    @Override
    public void onStop() {
        super.onStop();

        cleanupTasks();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupTasks();
    }

    private void cleanupTasks() {
        /*if (mDialog != null) {
            mDialog.dismiss();
        }*/

        //if (mFetchTask != null) {
        //    mFetchTask.cancel(true);
        //}
        
        if (mEditPostTask != null) {
            mEditPostTask.cancel(true);
        }
        
        if (mPostQuoteTask != null) {
            mPostQuoteTask.cancel(true);
        }
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	setScrollbarType();
    	onSharedPreferenceChanged(mPrefs, "default_post_font_size");
    	onSharedPreferenceChanged(mPrefs, "default_post_font_color");
    	
    	mPrefs.registerOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.post_menu, menu);
    }
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    	MenuItem bk = menu.findItem(R.id.bookmark);
    	if(bk != null){
    		AwfulThread th = (AwfulThread) adapt.getState();
    		bk.setTitle((th.isBookmarked()? getString(R.string.unbookmark):getString(R.string.bookmark)));
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
			case R.id.go_back:
				adapt.goToPage(adapt.getPage()-1);
				break;
			case R.id.usercp:
                startActivity(new Intent().setClass(getActivity(), UserCPActivity.class));
				break;
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
                                        //mFetchTask = new FetchThreadTask(pageInt);
                                        //mFetchTask.execute(mThread);
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
                break;
			case R.id.refresh:
				adapt.refresh();
				break;
			case R.id.settings:
				startActivity(new Intent().setClass(getActivity(), SettingsActivity.class));
				break;
			case R.id.bookmark:
				adapt.toggleBookmark();
				break;
			default:
				return super.onOptionsItemSelected(item);
    	}

		return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu aMenu, View aView, ContextMenuInfo aMenuInfo) {
        super.onCreateContextMenu(aMenu, aView, aMenuInfo);

        MenuInflater inflater = getActivity().getMenuInflater();
        Log.e(TAG, "onCreateContextMenu");
        AwfulPost selected = (AwfulPost) adapt.getItem(((AdapterContextMenuInfo) aMenuInfo).position);
        
        if (selected.isEditable()) {
            inflater.inflate(R.menu.user_post_longpress, aMenu);
        } else {
            inflater.inflate(R.menu.post_longpress, aMenu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) aItem.getMenuInfo();

        switch (aItem.getItemId()) {
            case R.id.edit:
                mEditPostTask = new ParseEditPostTask();
                mEditPostTask.execute(info.position);
                return true;
            case R.id.quote:
                mPostQuoteTask = new ParsePostQuoteTask();
                mPostQuoteTask.execute(info.position);
                return true;
            case R.id.last_read:
                mMarkLastReadTask = new MarkLastReadTask();
                mMarkLastReadTask.execute(info.position);
                return true;
        }

        return false;
    }
    
    @Override
    public void onActivityResult(int aRequestCode, int aResultCode, Intent aData) {
		// If we're here because of a post result, refresh the thread
		switch (aResultCode) {
			case PostReplyActivity.RESULT_POSTED:
				adapt.refresh();
				break;
		}
    }

    /*private void setListAdapter(ArrayList<AwfulPost> aPosts) {
        setListAdapter(generateAdapter(aPosts));

        setLastRead(aPosts);
    }

    private void setListAdapter() {
        ArrayList<AwfulPost> posts = mThread.getPosts();

        setListAdapter(generateAdapter(posts));

        setLastRead(posts);
    }*/

    /*private void setLastRead(int pos) {
        AwfulPost lastRead = null;

        // Maybe there's a better way to do getActivity()? It's 8am and I'm hung over
        for (AwfulPost post : aPosts) {
            if (post.isLastRead()) {
                lastRead = post;
                break;
            }
        }

        if (lastRead != null) {
            int index = aPosts.indexOf(lastRead);
            getListView().setSelection(index);
        }
    }*/

	private View.OnClickListener onButtonClick = new View.OnClickListener() {
		public void onClick(View aView) {
			switch (aView.getId()) {
				case R.id.next_page:
					if (adapt.getPage() < adapt.getLastPage()) {
						adapt.goToPage(adapt.getPage()+1);
					}
					break;
				case R.id.reply:
					Intent postReply = new Intent().setClass(getActivity(),
							PostReplyActivity.class);
					postReply.putExtra(Constants.THREAD, adapt.getState().getID()+"");
					startActivityForResult(postReply, 0);
					break;
			}
		}
	};

    private class MarkLastReadTask extends AsyncTask<Integer, Void, ArrayList<AwfulPost>> {
        public void onPreExecute() {
            mDialog = ProgressDialog.show(getActivity(), "Loading", 
                "Hold on...", true);
        }

        public ArrayList<AwfulPost> doInBackground(Integer... aParams) {
            ArrayList<AwfulPost> result = new ArrayList<AwfulPost>();

            if (!isCancelled()) {
                try {
                    //AwfulPostAdapter adapter = (AwfulPostAdapter) getListAdapter();
                    AwfulPost selected = (AwfulPost) adapt.getItem(aParams[0].intValue());

                    result = selected.markLastRead();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }

            return result;
        }

        public void onPostExecute(ArrayList<AwfulPost> aResult) {
            if (!isCancelled()) {
                mDialog.dismiss();

                //setListAdapter(aResult);
            }
        }
    }

    private class ParseEditPostTask extends AsyncTask<Integer, Void, String> {
        private String mPostId;

        public void onPreExecute() {
            mDialog = ProgressDialog.show(getActivity(), "Loading", 
                "Hold on...", true);
        }

        public String doInBackground(Integer... aParams) {
            String result = null;

            if (!isCancelled()) {
                try {
                    //AwfulPostAdapter adapter = (AwfulPostAdapter) getListAdapter();
                    AwfulPost selected = (AwfulPost) adapt.getItem(aParams[0].intValue());

                    mPostId = selected.getId();

                    result = Reply.getPost(selected.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }
            return result;
        }

        public void onPostExecute(String aResult) {
            if (!isCancelled()) {
                mDialog.dismiss();

                Intent postReply = new Intent().setClass(getActivity(), PostReplyActivity.class);
                postReply.putExtra(Constants.THREAD, adapt.getState().getID()+"");
                postReply.putExtra(Constants.QUOTE, aResult);
                postReply.putExtra(Constants.EDITING, true);
                postReply.putExtra(Constants.POST_ID, mPostId);

				startActivityForResult(postReply, 0);
            }
        }
    }

    private class ParsePostQuoteTask extends AsyncTask<Integer, Void, String> {
        public void onPreExecute() {
            mDialog = ProgressDialog.show(getActivity(), "Loading", 
                "Hold on...", true);
        }

        public String doInBackground(Integer... aParams) {
            String result = null;

            if (!isCancelled()) {
                try {
                    //AwfulPostAdapter adapter = (AwfulPostAdapter) getListAdapter();
                    AwfulPost selected = (AwfulPost) adapt.getItem(aParams[0].intValue());

                    result = Reply.getQuote(selected.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }
            return result;
        }

        public void onPostExecute(String aResult) {
            if (!isCancelled()) {
                mDialog.dismiss();

                Intent postReply = new Intent().setClass(getActivity(), PostReplyActivity.class);
                postReply.putExtra(Constants.THREAD, adapt.getState().getID()+"");
                postReply.putExtra(Constants.QUOTE, aResult);

				startActivityForResult(postReply, 0);
            }
        }
    }

	@Override
	public void dataUpdate() {
		mTitle.setText(adapt.getTitle());
		int last = adapt.getLastReadPost();
		if(last >= adapt.getCount()){
			last = adapt.getCount()-1;
		}
		if(last >0){
			getListView().setSelection(last);
		}
	}

    

    /**
     * Factory method for a post adapter. Deals with a few decorator classes.
     */
    /*private ListAdapter generateAdapter(ArrayList<AwfulPost> posts) {
    	AwfulPostAdapterBase base = new AwfulPostAdapterBase(getActivity(), R.layout.post_item, posts);
    	return new AwfulServiceAdapter(base);
    }*/

    /**
     * Decorates the base adapter that does the actual work with a
     * ThumbnailAdapter to render avatars, then adds SectionIndexer
     * capabilities for the fast scroll bar.
     *
     * Right now the SectionIndexer just does the post number relative
     * to the start of the page. In the future this might change to use
     * the page number in an endless list. 
     */
    /*
    public class AwfulPostAdapter extends AdapterWrapper implements SectionIndexer {
    	private AwfulPostAdapterBase mBaseAdapter;
    	
    	public AwfulPostAdapter(AwfulPostAdapterBase base) {
    		super(new ThumbnailAdapter(    		
    				getActivity(),
    				base,
    				((AwfulApplication) getActivity().getApplication()).getImageCache(),
        			new int[] {R.id.avatar}));
    		mBaseAdapter = base;
    	}

		@Override
		public int getPositionForSection(int section) {
			return section;
		}

		@Override
		public int getSectionForPosition(int position) {
			return position;
		}

		@Override
		public Object[] getSections() {
			int count = mBaseAdapter.getCount();
			String[] sections = new String[count];
			for(int i=0;i<count;i++) {
				sections[i] = Integer.toString(i+1);
			}
			return sections;
		}
    }

    
    public class AwfulPostAdapterBase extends ArrayAdapter<AwfulPost> {
        private ArrayList<AwfulPost> mPosts;
        private int mViewResource;
        private LayoutInflater mInflater;

        public AwfulPostAdapterBase(Context aContext, int aViewResource, ArrayList<AwfulPost> aPosts) {
            super(aContext, aViewResource, aPosts);

            mInflater     = LayoutInflater.from(aContext);
            mPosts        = aPosts;
            mViewResource = aViewResource;
        }

        private void startActivityForLink(String baseUrl, HashMap<String, String> params) {
        	String paramString = NetworkUtils.getQueryStringParameters(params);
			Uri uri = Uri.parse(baseUrl + paramString);
			Intent linkIntent = new Intent("android.intent.action.VIEW", uri);
			startActivity(linkIntent);
        }
        /**
         * this is already dead code, but I imagine someone will reimplement it eventually
         */
	/*
        private void showAvatarQuickAction(View anchor, final AwfulPost post, final long listId) {
        	QuickAction result = new QuickAction(anchor);
        	final String userid = post.getUserId();
        	
        	if(post.hasProfileLink()) {
	        	ActionItem profileAction = new ActionItem();
	        	profileAction.setTitle("Profile"); // TODO externalize
	        	profileAction.setIcon(getResources().getDrawable(R.drawable.ic_menu_usercp));
	        	profileAction.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
                        Intent profile = new Intent().setClass(getActivity(), ProfileActivity.class);
                        profile.putExtra(Constants.PARAM_USER_ID, userid);

						startActivity(profile);
					}
	        	});
	        	result.addActionItem(profileAction);
        	}	        	
        	
        	if(post.hasMessageLink()) {
	        	ActionItem messageAction = new ActionItem();
	        	messageAction.setTitle("Message"); // TODO externalize
	        	messageAction.setIcon(getResources().getDrawable(R.drawable.ic_menu_send));
	        	messageAction.setOnClickListener(new OnClickListener() {
	        		@Override
	        		public void onClick(View v) {
	        			HashMap<String, String> params = new HashMap<String, String>();
						params.put(Constants.PARAM_ACTION, Constants.ACTION_NEW_MESSAGE);
						params.put(Constants.PARAM_USER_ID, userid);
						startActivityForLink(Constants.FUNCTION_PRIVATE_MESSAGE, params);
	        		}
	        	});
	        	result.addActionItem(messageAction);
        	}
        	
        	if(post.hasPostHistoryLink()) {
	        	ActionItem postHistoryAction = new ActionItem();
	        	postHistoryAction.setTitle("Post History"); // TODO externalize
	        	postHistoryAction.setIcon(getResources().getDrawable(R.drawable.ic_menu_archive));
	        	postHistoryAction.setOnClickListener(new OnClickListener() {
	        		@Override
	        		public void onClick(View v) {
	        			HashMap<String, String> params = new HashMap<String, String>();
						params.put(Constants.PARAM_ACTION, Constants.ACTION_SEARCH_POST_HISTORY);
						params.put(Constants.PARAM_USER_ID, userid);
						startActivityForLink(Constants.FUNCTION_SEARCH, params);
	        		}
	        	});
	        	result.addActionItem(postHistoryAction);
        	}
        	
        	if(post.hasRapSheetLink()) {
	        	ActionItem rapSheetAction = new ActionItem();
	        	rapSheetAction.setTitle("Rap Sheet"); // TODO externalize
	        	rapSheetAction.setIcon(getResources().getDrawable(R.drawable.ic_menu_clear_playlist));
	        	rapSheetAction.setOnClickListener(new OnClickListener() {
	        		@Override
	        		public void onClick(View v) {
	        			HashMap<String, String> params = new HashMap<String, String>();
						params.put(Constants.PARAM_USER_ID, userid);
						startActivityForLink(Constants.FUNCTION_BANLIST, params);
	        		}
	        	});
	        	result.addActionItem(rapSheetAction);
        	}
        	
        	result.setAnimStyle(QuickAction.ANIM_AUTO);
        	result.show();
        }
        
        private class ViewHolder {
        	public TextView username;
        	public TextView postDate;
        	public HtmlView postBody;
        	public ImageView avatar;
        	public View postHead;
            public TextView pageCount;
            public RelativeLayout pageIndicator; 
        	
        	public ViewHolder(View v) {
        		username = (TextView) v.findViewById(R.id.username);
        		postDate = (TextView) v.findViewById(R.id.post_date);
        		postBody = (HtmlView) v.findViewById(R.id.postbody);
        		avatar = (ImageView) v.findViewById(R.id.avatar);
        		postHead = v.findViewById(R.id.posthead);
                pageCount = (TextView) v.findViewById(R.id.page_count);
                pageIndicator = (RelativeLayout) v.findViewById(R.id.page_indicator);
        	}
        }
        
        @Override
        public View getView(final int aPosition, View aConvertView, ViewGroup aParent) {
            View inflatedView = aConvertView;
            ViewHolder viewHolder = null;
            
            if (inflatedView == null) {
                inflatedView = mInflater.inflate(mViewResource, null);
                viewHolder = new ViewHolder(inflatedView);
                inflatedView.setTag(viewHolder);
                viewHolder.postBody.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
            	viewHolder = (ViewHolder) inflatedView.getTag();
            }

            final AwfulPost current = getItem(aPosition);
            
            viewHolder.username.setText(current.getUsername());
            viewHolder.postDate.setText("Posted on " + current.getDate());
            viewHolder.postBody.setHtml(current.getContent());

            // These are done per render instead of at view construction because there's
            // apparently no good way to force view reconstruction after, say, the user
            // changes preferences for these things.
            viewHolder.postBody.setTextSize(mDefaultPostFontSize);
            viewHolder.postBody.setTextColor(mDefaultPostFontColor);
            
            // change background color of previously read posts

            if (current.isPreviouslyRead()) {
            	if (current.isEven()) {
            		viewHolder.postBody.setBackgroundColor(Constants.READ_BACKGROUND_EVEN);
            	} else {
            		viewHolder.postBody.setBackgroundColor(Constants.READ_BACKGROUND_UNEVEN);
            	}
            } else {
                viewHolder.postBody.setBackgroundColor(getResources().getColor(R.color.forums_gray));
            }
            
            // Set up header quickactions
            final ViewHolder vh = viewHolder;
            OnClickListener listener = new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(v == vh.postHead) {
						// showAvatarQuickAction(vh.avatar, current, aPosition);
					}
				}
            };

            if (aPosition == (getCount() - 1))  {
                viewHolder.pageIndicator.setVisibility(View.VISIBLE);
                viewHolder.pageCount.setVisibility(View.VISIBLE);
                viewHolder.pageCount.setText("Page " + Integer.toString(mThread.getCurrentPage()) +
                        "/" + Integer.toString(mThread.getLastPage()));
            } else if (viewHolder.pageCount.getVisibility() == View.VISIBLE) {
                viewHolder.pageIndicator.setVisibility(View.GONE);
                viewHolder.pageCount.setVisibility(View.GONE);
            }
            
            viewHolder.postHead.setOnClickListener(listener);
            viewHolder.postBody.setOnClickListener(listener);
            
            if( current.getAvatar() == null ) {
            	viewHolder.avatar.setVisibility(View.INVISIBLE);
            } else {
            	viewHolder.avatar.setVisibility(View.VISIBLE);
            }
            
            viewHolder.avatar.setTag(current.getAvatar());
            

            return inflatedView;
        }
    }*/
}
