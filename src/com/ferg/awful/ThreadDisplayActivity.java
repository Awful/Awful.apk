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
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.adapter.AdapterWrapper;
import com.ferg.awful.async.DrawableManager;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.htmlwidget.HtmlView;
import com.ferg.awful.reply.Reply;
import com.ferg.awful.thread.AwfulPost;
import com.ferg.awful.thread.AwfulThread;
import com.ferg.awful.thumbnail.ThumbnailAdapter;

public class ThreadDisplayActivity extends Activity {
    private static final String TAG = "ThreadDisplayActivity";

	private final DrawableManager mImageDownloader = new DrawableManager();

	private AwfulThread mThread;
    private FetchThreadTask mFetchTask;
    private ParsePostQuoteTask mPostQuoteTask;

	private ImageButton mNext;
	private ImageButton mReply;
    private ListView mPostList;
	private ProgressDialog mDialog;
    private SharedPreferences mPrefs;
    private TextView mTitle;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		
        mPrefs = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);

        mPostList = (ListView) findViewById(R.id.thread_posts);
        mTitle    = (TextView) findViewById(R.id.title);
        mNext     = (ImageButton) findViewById(R.id.next_page);
        mReply    = (ImageButton) findViewById(R.id.reply);

        registerForContextMenu(mPostList);

        final AwfulThread retainedThread = (AwfulThread) getLastNonConfigurationInstance();

        if (retainedThread == null || retainedThread.getPosts() == null) {
            // We may be getting thread info from ChromeToPhone so handle that here
            if (getIntent().getData() != null) {
                if (getIntent().getData().getScheme().equals("http")) {
                    Log.i(TAG, getIntent().getData().getQueryParameter("pagenumber"));

                    mThread = new AwfulThread(getIntent().getData().getQueryParameter("threadid"));

                    String page = getIntent().getData().getQueryParameter("pagenumber");

                    if (page != null) {
                        mFetchTask = new FetchThreadTask(Integer.parseInt(page));
                    } else {
                        mFetchTask = new FetchThreadTask();
                    }
                }
            } else {
                mThread = (AwfulThread) getIntent().getParcelableExtra(Constants.THREAD);
                mFetchTask = new FetchThreadTask();
            }
            mFetchTask.execute(mThread);
        } else {
            mThread = retainedThread;
            setListAdapter();
        }

        // If this is coming from ChromeToPhone we have to set the title later
        if (mThread.getTitle() != null) {
            mTitle.setText(Html.fromHtml(mThread.getTitle()));
        }

		mNext.setOnClickListener(onButtonClick);
		mReply.setOnClickListener(onButtonClick);
    }
    
    @Override
    public void onPause() {
        super.onPause();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mFetchTask != null) {
            mFetchTask.cancel(true);
        }
        
        if (mPostQuoteTask != null) {
            mPostQuoteTask.cancel(true);
        }
    }
        
    @Override
    public void onStop() {
        super.onStop();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mFetchTask != null) {
            mFetchTask.cancel(true);
        }
        
        if (mPostQuoteTask != null) {
            mPostQuoteTask.cancel(true);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mFetchTask != null) {
            mFetchTask.cancel(true);
        }
        
        if (mPostQuoteTask != null) {
            mPostQuoteTask.cancel(true);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.post_menu, menu);

		return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
			case R.id.go_back:
				if (mThread.getCurrentPage() != 1) {
					mFetchTask = new FetchThreadTask(mThread.getCurrentPage() - 1);
                    mFetchTask.execute(mThread);
				}
				break;
			case R.id.usercp:
                startActivity(new Intent().setClass(this, UserCPActivity.class));
				break;
			case R.id.go_to:
                final NumberPicker jumpToText = new NumberPicker(ThreadDisplayActivity.this);
                jumpToText.setRange(1, mThread.getLastPage());
                jumpToText.setCurrent(mThread.getCurrentPage());
                new AlertDialog.Builder(ThreadDisplayActivity.this)
                    .setTitle("Jump to Page")
                    .setView(jumpToText)
                    .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface aDialog, int aWhich) {
                                try {
                                    int pageInt = jumpToText.getCurrent();
                                    if (pageInt > 0 && pageInt <= mThread.getLastPage()) {
                                        mFetchTask = new FetchThreadTask(pageInt);
                                        mFetchTask.execute(mThread);
                                    }
                                } catch (NumberFormatException e) {
                                    Log.d(TAG, "Not a valid number: " + e.toString());
        	                        Toast.makeText(ThreadDisplayActivity.this,
                                        R.string.invalid_page, Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Log.d(TAG, e.toString());
                                }
                            }
                        })
                    .setNegativeButton("Cancel", null)
                    .show();
			case R.id.refresh:
				mFetchTask = new FetchThreadTask(true);
				mFetchTask.execute(mThread);
				break;
			default:
				return super.onOptionsItemSelected(item);
    	}

		return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu aMenu, View aView, ContextMenuInfo aMenuInfo) {
        super.onCreateContextMenu(aMenu, aView, aMenuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.post_longpress, aMenu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) aItem.getMenuInfo();

        switch (aItem.getItemId()) {
            case R.id.quote:
                mPostQuoteTask = new ParsePostQuoteTask();
                mPostQuoteTask.execute(info.id);
                return true;
        }

        return false;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        final AwfulThread currentThread = mThread;

        return currentThread;
    }
    
    @Override
    protected void onActivityResult(int aRequestCode, int aResultCode, Intent aData) {
		// If we're here because of a post result, refresh the thread
		switch (aResultCode) {
			case PostReplyActivity.RESULT_POSTED:
				mFetchTask = new FetchThreadTask(true);
				mFetchTask.execute(mThread);
				break;
		}
    }

    private void setListAdapter() {
        ArrayList<AwfulPost> posts = mThread.getPosts();

        mPostList.setAdapter(generateAdapter(posts));

        AwfulPost lastRead = null;

        // Maybe there's a better way to do this? It's 8am and I'm hung over
        for (AwfulPost post : posts) {
            if (post.isLastRead()) {
                lastRead = post;
                break;
            }
        }

        if (lastRead != null) {
            int index = posts.indexOf(lastRead);
            mPostList.setSelection(index);
        }
    }

	private View.OnClickListener onButtonClick = new View.OnClickListener() {
		public void onClick(View aView) {
			switch (aView.getId()) {
				case R.id.next_page:
					if (mThread.getCurrentPage() < mThread.getLastPage()) {
						mFetchTask = new FetchThreadTask(mThread.getCurrentPage() + 1);
                        mFetchTask.execute(mThread);
					}
					break;
				case R.id.reply:
					Intent postReply = new Intent().setClass(ThreadDisplayActivity.this,
							PostReplyActivity.class);
					postReply.putExtra(Constants.THREAD, mThread);
					startActivityForResult(postReply, 0);
					break;
			}
		}
	};

    private class ParsePostQuoteTask extends AsyncTask<Long, Void, String> {
        public void onPreExecute() {
            mDialog = ProgressDialog.show(ThreadDisplayActivity.this, "Loading", 
                "Hold on...", true);
        }

        public String doInBackground(Long... aParams) {
            String result = null;

            if (!isCancelled()) {
                try {
                    AwfulPostAdapter adapter = (AwfulPostAdapter) mPostList.getAdapter();
                    AwfulPost selected = (AwfulPost) adapter.getItem(aParams[0].intValue());

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

                Intent postReply = new Intent().setClass(ThreadDisplayActivity.this, PostReplyActivity.class);
                postReply.putExtra(Constants.THREAD, mThread);
                postReply.putExtra(Constants.QUOTE, aResult);

				startActivityForResult(postReply, 0);
            }
        }
    }

    private class FetchThreadTask extends AsyncTask<AwfulThread, Void, AwfulThread> {
		private boolean mForceLastPage = false;
		private int mPage;

		public FetchThreadTask() {}

		public FetchThreadTask(boolean aForceLastPage) {
			mForceLastPage = aForceLastPage;
		}

		public FetchThreadTask(int aPage) {
			mPage = aPage;
		}

        public void onPreExecute() {
            mDialog = ProgressDialog.show(ThreadDisplayActivity.this, "Loading", 
                "Hold on...", true);
        }

        public AwfulThread doInBackground(AwfulThread... aParams) {
            if (!isCancelled()) {
                try {
                    if (mPage == 0) {
                        // We set the unread count to -1 if the user has never
                        // visited that thread before
                        if (aParams[0].getUnreadCount() > -1 || mForceLastPage) {
                            aParams[0].getThreadPosts();
                        } else {
                            aParams[0].getThreadPosts(1);
                        }
                    } else {
                        aParams[0].getThreadPosts(mPage);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, e.toString());
                }
            }

            return aParams[0];
        }

        public void onPostExecute(AwfulThread aResult) {
            if (!isCancelled()) {
                mThread = aResult;
                setListAdapter();

                // If we're loading a thread from ChromeToPhone we have to set the 
                // title now
                if (mTitle.getText() == null || mTitle.getText().length() == 0) {
                    mTitle.setText(Html.fromHtml(mThread.getTitle()));
                }

                if (mDialog != null) {
                    mDialog.dismiss();
                }
            }
        }
    }

    /**
     * Factory method for a post adapter. Deals with a few decorator classes.
     */
    private ListAdapter generateAdapter(ArrayList<AwfulPost> posts) {
    	AwfulPostAdapterBase base = new AwfulPostAdapterBase(this, R.layout.post_item, posts);
    	return new AwfulPostAdapter(base);
    }
    /**
     * Decorates the base adapter that does the actual work with a
     * ThumbnailAdapter to render avatars, then adds SectionIndexer
     * capabilities for the fast scroll bar.
     *
     * Right now the SectionIndexer just does the post number relative
     * to the start of the page. In the future this might change to use
     * the page number in an endless list. 
     */
    public class AwfulPostAdapter extends AdapterWrapper implements SectionIndexer {
    	private AwfulPostAdapterBase mBaseAdapter;
    	
    	public AwfulPostAdapter(AwfulPostAdapterBase base) {
    		super(new ThumbnailAdapter(    		
    				ThreadDisplayActivity.this,
    				base,
    				((AwfulApplication)getApplication()).getImageCache(),
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

        private class ViewHolder {
        	public TextView username;
        	public TextView postDate;
        	public HtmlView postBody;
        	public ImageView avatar;
        	public ViewHolder(View v) {
        		username = (TextView) v.findViewById(R.id.username);
        		postDate = (TextView) v.findViewById(R.id.post_date);
        		postBody = (HtmlView) v.findViewById(R.id.postbody);
        		avatar = (ImageView) v.findViewById(R.id.avatar);
        	}
        }
        
        @Override
        public View getView(int aPosition, View aConvertView, ViewGroup aParent) {
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

            AwfulPost current = getItem(aPosition);

            viewHolder.username.setText(current.getUsername());
            viewHolder.postDate.setText("Posted on " + current.getDate());
            viewHolder.postBody.setHtml(current.getContent());

            // TODO: Why is this crashing when using the cache? Seems to be gif related.
            // Note: ImageDownloader changed since that todo was written; not sure if it's still an issue
            // mImageDownloader.fetchDrawableOnThread(current.getAvatar(), viewHolder.avatar);
            viewHolder.avatar.setTag(current.getAvatar());

            return inflatedView;
        }
    }
}
