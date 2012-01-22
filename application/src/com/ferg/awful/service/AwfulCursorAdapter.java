package com.ferg.awful.service;

import com.ferg.awful.AwfulActivity;
import com.ferg.awful.R;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.task.ImageCacheTask;
import com.ferg.awful.thread.AwfulEmote;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulMessage;
import com.ferg.awful.thread.AwfulPost;
import com.ferg.awful.thread.AwfulThread;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AwfulCursorAdapter extends CursorAdapter {
	private AwfulPreferences mPrefs;
	private AwfulActivity mParent;
	private LayoutInflater inf;
	private int mId;
	
	public AwfulCursorAdapter(AwfulActivity context, Cursor c) {
		this(context, c, 0);
	}
	public AwfulCursorAdapter(AwfulActivity context, Cursor c, int id) {
		super(context, c, 0);
		mPrefs = new AwfulPreferences(context);
		inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mParent = context;
		mId = id;
	}

	@Override
	public void bindView(View current, Context context, Cursor data) {
		if(data.getColumnIndex(AwfulThread.BOOKMARKED) >= 0){//unique to threads
			String tagUrl = AwfulThread.getView(current, mPrefs, data, context, mId == Constants.USERCP_ID);
			if(tagUrl != null){
				mParent.sendMessage(AwfulSyncService.MSG_GRAB_IMAGE, mId, tagUrl.hashCode(), tagUrl);
			}
		}else if(data.getColumnIndex(AwfulForum.PARENT_ID) >= 0){//unique to forums
			AwfulForum.getView(current, mPrefs, data);
		}else if(data.getColumnIndex(AwfulMessage.DATE) >= 0){
			AwfulMessage.getView(current, mPrefs, data);
		}else if(data.getColumnIndex(AwfulEmote.CACHEFILE) >= 0){
			AwfulEmote.getView(current, mPrefs, data);
		}
	}

	@Override
	public View newView(Context context, Cursor data, ViewGroup parent) {
		View row;
		if(data.getColumnIndex(AwfulThread.BOOKMARKED) >= 0){//unique to threads
			row = inf.inflate(R.layout.thread_item, parent, false);
			String tagUrl = AwfulThread.getView(row, mPrefs, data, context, mId == Constants.USERCP_ID);
			if(tagUrl != null){
				mParent.sendMessage(AwfulSyncService.MSG_GRAB_IMAGE, mId, tagUrl.hashCode(), tagUrl);
			}
		}else if(data.getColumnIndex(AwfulForum.PARENT_ID) >= 0){//unique to forums
			row = inf.inflate(R.layout.forum_item, parent, false);
			AwfulForum.getView(row, mPrefs, data);
		}else if(data.getColumnIndex(AwfulMessage.UNREAD) >= 0){
			row = inf.inflate(R.layout.forum_item, parent, false);
			AwfulMessage.getView(row, mPrefs, data);
		}else if(data.getColumnIndex(AwfulEmote.CACHEFILE) >= 0){
			row = inf.inflate(R.layout.forum_item, parent, false);//TODO add custom emote view
			AwfulEmote.getView(row, mPrefs, data);
		}else{
			row = inf.inflate(R.layout.loading, parent, false);
		}
		return row;
	}
	
	public int getInt(int position, String column){
    	Cursor tmpcursor = getCursor();
    	int col = tmpcursor.getColumnIndex(column);
    	if(tmpcursor.moveToPosition(position) && col >=0){
    		return tmpcursor.getInt(col);
    	}
		return 0;
	}

	public String getString(int position, String column){
    	Cursor tmpcursor = getCursor();
    	int col = tmpcursor.getColumnIndex(column);
    	if(tmpcursor.moveToPosition(position) && col >=0){
    		return tmpcursor.getString(col);
    	}
		return null;
	}
	public int getType(int position){
    	Cursor tmpcursor = getCursor();
    	if(tmpcursor.moveToPosition(position)){
    		if(tmpcursor.getColumnIndex(AwfulThread.BOOKMARKED) >= 0){//unique to threads
    			return R.layout.thread_item;
    		}else if(tmpcursor.getColumnIndex(AwfulForum.PARENT_ID) >= 0){//unique to forums
    			return R.layout.forum_item;
    		}else if(tmpcursor.getColumnIndex(AwfulPost.AVATAR) >= 0){//unique to posts
    			return R.layout.post_item;
    		}
    	}
    	return -1;
	}
}
