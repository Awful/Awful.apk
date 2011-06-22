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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.ListFragment;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.reply.Reply;
import com.ferg.awful.service.AwfulServiceConnection.ThreadListAdapter;
import com.ferg.awful.thread.AwfulPost;
import com.ferg.awful.thread.AwfulThread;
import com.ferg.awful.widget.NumberPicker;

public class ThreadDisplayFragment extends ListFragment implements OnSharedPreferenceChangeListener, AwfulUpdateCallback, OnScrollListener {
    private static final String TAG = "ThreadDisplayActivity";

	private ThreadListAdapter adapt;
    private ParsePostQuoteTask mPostQuoteTask;
    private ParseEditPostTask mEditPostTask;

	private ImageButton mNext;
	private ImageButton mReply;
	private ImageButton mRefresh;
    private TextView mTitle;
	
	private ProgressDialog mDialog;
    private SharedPreferences mPrefs;
    
	private boolean queueDataUpdate;
	private Handler handler = new Handler();
	private class RunDataUpdate implements Runnable{
		boolean pageChange;
		public RunDataUpdate(boolean hasPageChanged){
			pageChange = hasPageChanged;
		}
		@Override
		public void run() {
			delayedDataUpdate(pageChange);
		}
	};
	private int savedPage = 0;
	private int savedPos = 0;



    // These just store values from shared preferences. This way, we only have to do redraws
    // and the like if the preferences defining drawing have actually changed since we last
    // saw them
    private int mDefaultPostBackgroundColor;
    private int mReadPostBackgroundColor;

    
    @Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
	}
    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        super.onCreateView(aInflater, aContainer, aSavedState);
		Log.e(TAG,"onCreateView()");
        View result = aInflater.inflate(R.layout.thread_display, aContainer, true);
        
        mTitle    = (TextView) result.findViewById(R.id.title);
        mNext     = (ImageButton) result.findViewById(R.id.next_page);
        mReply    = (ImageButton) result.findViewById(R.id.reply);
        mRefresh       = (ImageButton) result.findViewById(R.id.refresh);

        mTitle.setMovementMethod(new ScrollingMovementMethod());
        
        
        return result;
    }

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);
		Log.e(TAG,"onActivityCreated()");
        setRetainInstance(true);
        
        PreferenceManager.setDefaultValues(getActivity(), R.xml.settings, false);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        getListView().setBackgroundColor(mPrefs.getInt("default_post_background_color", getResources().getColor(R.color.background)));
        getListView().setCacheColorHint(mPrefs.getInt("default_post_background_color", getResources().getColor(R.color.background)));
        
        
        mDefaultPostBackgroundColor = mPrefs.getInt("default_post_background_color", getResources().getColor(R.color.background));

        registerForContextMenu(getListView());

		mNext.setOnClickListener(onButtonClick);
		mReply.setOnClickListener(onButtonClick);
		mRefresh.setOnClickListener(onButtonClick);
		getListView().setOnScrollListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
		Log.e(TAG,"onStart()");

        mTitle.setText("Loading...");
    }
    
    @Override
    public void setListAdapter(ListAdapter adapter){
    	super.setListAdapter(adapter);
    	adapt = (ThreadListAdapter) adapter;
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    	if("default_post_background_color".equals(key)) {
        	int newBackground = prefs.getInt(key, R.color.background);
        	if(newBackground != mDefaultPostBackgroundColor) {
        		mDefaultPostBackgroundColor = newBackground;
        		Log.d(TAG, "invalidating (color)");
        		getListView().invalidateViews(); 
        	}   			
        } else if("read_post_background_color".equals(key)) {
           	int newReadBG = prefs.getInt(key, R.color.background_read);
           	if(newReadBG != mReadPostBackgroundColor) {
           		mReadPostBackgroundColor = newReadBG;
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
		Log.e(TAG,"onPause()");

        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        cleanupTasks();
    }
        
    @Override
    public void onStop() {
        super.onStop();
		Log.e(TAG,"onStop()");
        cleanupTasks();
    }

    @Override
    public void onDetach() {
        super.onDetach();
		Log.e(TAG,"onDetach()");
	    savedPage = adapt.getPage();//saves page for orientation change.
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
		Log.e(TAG,"onDestroy()");
        cleanupTasks();
    }

    private void cleanupTasks() {
    	if (mDialog != null) {
            mDialog.dismiss();
        }
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
		Log.e(TAG,"onResume()");
    	
    	setScrollbarType();
    	
    	mPrefs.registerOnSharedPreferenceChangeListener(this);
    	if(queueDataUpdate){
    		dataUpdate(false);
    	}
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	if(menu.size() == 0){
    		inflater.inflate(R.menu.post_menu, menu);
    	}
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
                adapt.markLastRead((AwfulPost) adapt.getItem(info.position));
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
				case R.id.refresh:
					adapt.refresh();
					break;
			}
		}
	};



    

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
            	if (mDialog != null) {
                    mDialog.dismiss();
                }
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
            	if (mDialog != null) {
                    mDialog.dismiss();
                }
                Intent postReply = new Intent().setClass(getActivity(), PostReplyActivity.class);
                postReply.putExtra(Constants.THREAD, Integer.toString(adapt.getState().getID()));
                postReply.putExtra(Constants.QUOTE, aResult);

				startActivityForResult(postReply, 0);
            }
        }
    }

	@Override
	public void dataUpdate(boolean pageChange) {
		if(!this.isResumed()){
			queueDataUpdate = true;
			return;
		}else{
			queueDataUpdate = false;
			handler.post(new RunDataUpdate(pageChange));
		}
	}
	public void delayedDataUpdate(boolean pageChange) {
		mTitle.setText(Html.fromHtml(adapt.getTitle()));
		int last = adapt.getLastReadPost();
		if(savedPage == adapt.getPage() && savedPos >0 && savedPos < adapt.getCount()){
			getListView().setSelection(savedPos);
		}else{
			if(!pageChange && last >= 0 && last < adapt.getCount()){
				getListView().setSelection(last);
				savedPos = last;
			}
			if(pageChange && adapt.getCount() > 0){
				getListView().setSelection(0);
			}
		}
		if(adapt.getPage() == adapt.getLastPage()){
			mNext.setVisibility(View.GONE);
		}else{
			mNext.setVisibility(View.VISIBLE);
		}
	}
	public int getSavedPage() {
		return savedPage;
	}
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if(visibleItemCount>0 && firstVisibleItem >0){
			savedPos = firstVisibleItem+1;
		}
		
	}
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}
	
	@Override
	public void loadingFailed() {
		Log.e(TAG, "Loading failed.");
		mRefresh.setVisibility(View.VISIBLE);
		mRefresh.setAnimation(null);
		mRefresh.setImageResource(android.R.drawable.ic_dialog_alert);
		mRefresh.startAnimation(adapt.getBlinkingAnimation());
		Toast.makeText(getActivity(), "Loading Failed!", Toast.LENGTH_LONG).show();
	}

	@Override
	public void loadingStarted() {
		Log.e(TAG, "Loading started.");
		mRefresh.setVisibility(View.VISIBLE);
		mRefresh.setImageResource(R.drawable.ic_menu_refresh);
		mRefresh.startAnimation(adapt.getRotateAnimation());
	}

	@Override
	public void loadingSucceeded() {
		Log.e(TAG, "Loading succeeded.");
		mRefresh.setAnimation(null);
		mRefresh.setVisibility(View.GONE);
	}
}