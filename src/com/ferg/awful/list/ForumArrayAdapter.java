package com.ferg.awful.list;

import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ferg.awful.R;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulThread;

/**
 * A ListAdapter which is designed to handle both sub-forums and threads of a forum.
 *
 */
public class ForumArrayAdapter extends BaseAdapter {
	public enum ItemType {
		SUB_FORUM,
		THREAD
	}
	private SharedPreferences mPrefs;
	private List<AwfulForum>  mSubForums;
    private List<AwfulThread> mThreads;
    
    private LayoutInflater mInflater;

    public ForumArrayAdapter(Context aContext) {
        super();
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(aContext);
        mInflater = LayoutInflater.from(aContext);

        mSubForums = null;
        mThreads = null;
    }
    
    public void setSubForums(List<AwfulForum> subForums) {
    	mSubForums = subForums;
    	notifyDataSetChanged();
    }
    
    /**
     * If you modify the returned list or its contents, this adapter will have undefined behavior.
     * 
     * Use the set methods instead.
     */
    public List<AwfulForum> getSubForums() {
    	return mSubForums;
    }
    
    public void setThreads(List<AwfulThread> threads) {
    	mThreads = threads;
    	notifyDataSetChanged();
    }
    
    /**
     * If you modify the returned list or its contents, this adapter will have undefined behavior.
     * 
     * Use the set methods instead.
     */
    public List<AwfulThread> getThreads() {
    	return mThreads;
    }
    
    public void addThreads(List<AwfulThread> threads) {
    	if(mThreads == null) {
    		setThreads(threads);
    	} else {
		    mThreads.addAll(threads);
		    notifyDataSetChanged();
    	}
    }
    
    public ItemType getItemType(int position) {
    	int subforums = getSubForumCount();
		int threads = getThreadCount();
		
    	if(position < 0) return null;
		else if(position < subforums) return ItemType.SUB_FORUM;
		else if(position < threads) return ItemType.THREAD;
		else return null;
    }
    
    public int getSubForumCount() {
    	return mSubForums == null ? 0 : mSubForums.size();
    }
    
    public int getThreadCount() {
    	return mThreads == null ? 0 : mThreads.size();
    }
    
	@Override
	public int getCount() {
		return getSubForumCount() + getThreadCount();
	}

	@Override
	public Object getItem(int position) {
		int subforums = getSubForumCount();
		int threads = getThreadCount();
		
		if(position < 0) return null;
		else if(position < subforums) return mSubForums.get(position);
		else if(position < threads) return mThreads.get(position - subforums);
		else return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}
	
	@Override
	public int getItemViewType(int position) {
		return getItemType(position).ordinal();
	}
	
    @Override
    public View getView(int aPosition, View aConvertView, ViewGroup aParent) {
    	int subforums = getSubForumCount();
		int threads = getThreadCount();
		
		if(aPosition < 0) return null;
		else if(aPosition < subforums) return getSubForumView(aPosition, aConvertView, aParent);
		else if(aPosition < threads) return getThreadView(aPosition - subforums, aConvertView, aParent);
		else return null;
    }
    
    private class ForumViewHolder {
    	public TextView title;
    	public TextView subtext;
    	
    	public ForumViewHolder(View view) {
    		title       = (TextView)  view.findViewById(R.id.title);
            subtext     = (TextView)  view.findViewById(R.id.subtext);
    	}
    }  
    
    private View getSubForumView(int aPosition, View aConvertView, ViewGroup aParent) {
    	View inflatedView = aConvertView;
        ForumViewHolder viewHolder;
        
        if (inflatedView == null) {
            inflatedView = mInflater.inflate(R.layout.forum_item, null);
            viewHolder = new ForumViewHolder(inflatedView);
            inflatedView.setTag(viewHolder);
        } else {
        	viewHolder = (ForumViewHolder) inflatedView.getTag();
        }

        AwfulForum current = mSubForums.get(aPosition);

        viewHolder.title.setText(Html.fromHtml(current.getTitle()));
        viewHolder.subtext.setText(Html.fromHtml(current.getSubtext()));

        return inflatedView;
    }
    
    private class ThreadViewHolder {
    	public TextView title;
    	public TextView author;
    	public TextView unreadCount;
    	public ImageView stickyIcon;
    	
    	public ThreadViewHolder(View view) {
    		title       = (TextView)  view.findViewById(R.id.title);
    		title.setTextColor(mPrefs.getInt("default_post_font_color", view.getResources().getColor(R.color.default_post_font)));
            author      = (TextView)  view.findViewById(R.id.author);
            author.setTextColor(mPrefs.getInt("default_post_background2_color", view.getResources().getColor(R.color.background2)));
            unreadCount = (TextView)  view.findViewById(R.id.unread_count);
            stickyIcon  = (ImageView) view.findViewById(R.id.sticky_icon);
    	}
    }
    
    private View getThreadView(int aPosition, View aConvertView, ViewGroup aParent) {
        View inflatedView = aConvertView;
        ThreadViewHolder viewHolder;
        
        if (inflatedView == null) {
            inflatedView = mInflater.inflate(R.layout.thread_item, null);
            viewHolder = new ThreadViewHolder(inflatedView);
            inflatedView.setTag(viewHolder);
        } else {
        	viewHolder = (ThreadViewHolder) inflatedView.getTag();
        }

        AwfulThread current = mThreads.get(aPosition);

        viewHolder.title.setText(Html.fromHtml(current.getTitle()));
        viewHolder.author.setText("Author: " + current.getAuthor());

		if (current.getUnreadCount() == -1) {
			viewHolder.unreadCount.setVisibility(View.GONE);
		} else {
			viewHolder.unreadCount.setVisibility(View.VISIBLE);
			viewHolder.unreadCount.setText(Integer.toString(current.getUnreadCount()));
		}

        if (current.isSticky()) {
            viewHolder.stickyIcon.setVisibility(View.VISIBLE);
        } else {
        	viewHolder.stickyIcon.setVisibility(View.GONE);
        }

        return inflatedView;
    }
}