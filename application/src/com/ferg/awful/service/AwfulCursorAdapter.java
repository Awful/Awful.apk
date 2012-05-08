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
import android.graphics.Typeface;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AwfulCursorAdapter extends CursorAdapter {
	private AwfulPreferences mPrefs;
	private AwfulActivity mParent;
	private LayoutInflater inf;
	private int mId;
	private int selectedId = -1;
	private boolean mIsSidebar;
	
	public AwfulCursorAdapter(AwfulActivity context, Cursor c) {
		this(context, c, 0, false);
	}
	public AwfulCursorAdapter(AwfulActivity context, Cursor c, int id, boolean isSidebar) {
		super(context, c, 0);
		mPrefs = new AwfulPreferences(context);
		inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mParent = context;
		mId = id;
		mIsSidebar = isSidebar;
	}
	
	public void setSelected(int id){
		selectedId = id;
	}
	
	public void setSidebar(boolean isSidebar){
		mIsSidebar = isSidebar;
	}

	@Override
	public void bindView(View current, Context context, Cursor data) {
		if(data.getColumnIndex(AwfulThread.BOOKMARKED) >= 0){//unique to threads
			String tagUrl = AwfulThread.getView(current, mPrefs, data, context, mId == Constants.USERCP_ID, mIsSidebar, false);
			if(tagUrl != null){
				mParent.sendMessage(AwfulSyncService.MSG_GRAB_IMAGE, mId, tagUrl.hashCode(), tagUrl);
			}
		}else if(data.getColumnIndex(AwfulForum.PARENT_ID) >= 0){//unique to forums
			AwfulForum.getSubforumView(current, mPrefs, data, mIsSidebar, false);
		}else if(data.getColumnIndex(AwfulMessage.DATE) >= 0){
			AwfulMessage.getView(current, mPrefs, data, mIsSidebar, false);
		}else if(data.getColumnIndex(AwfulEmote.CACHEFILE) >= 0){
			AwfulEmote.getView(current, mPrefs, data);
		}
		mParent.setPreferredFont(current);
	}

	@Override
	public View newView(Context context, Cursor data, ViewGroup parent) {
		View row;
		if(data.getColumnIndex(AwfulThread.BOOKMARKED) >= 0){//unique to threads
			row = inf.inflate(R.layout.thread_item, parent, false);
			String tagUrl = AwfulThread.getView(row, mPrefs, data, context, mId == Constants.USERCP_ID, mIsSidebar, false);
			if(tagUrl != null){
				mParent.sendMessage(AwfulSyncService.MSG_GRAB_IMAGE, mId, tagUrl.hashCode(), tagUrl);
			}
		}else if(data.getColumnIndex(AwfulForum.PARENT_ID) >= 0){//unique to forums
			row = inf.inflate(R.layout.thread_item, parent, false);
			AwfulForum.getSubforumView(row, mPrefs, data, mIsSidebar, false);
		}else if(data.getColumnIndex(AwfulMessage.UNREAD) >= 0){
			row = inf.inflate(R.layout.forum_item, parent, false);
			AwfulMessage.getView(row, mPrefs, data, mIsSidebar, false);
		}else if(data.getColumnIndex(AwfulEmote.CACHEFILE) >= 0){
			row = inf.inflate(R.layout.forum_item, parent, false);//TODO add custom emote view
			AwfulEmote.getView(row, mPrefs, data);
		}else{
			row = inf.inflate(R.layout.loading, parent, false);
		}
		mParent.setPreferredFont(row);
		return row;
	}
	
	public int getInt(long id, String column){
    	Cursor tmpcursor = getRow(id);
    	if(tmpcursor != null){
        	int col = tmpcursor.getColumnIndex(column);
    		return tmpcursor.getInt(col);
    	}
		return 0;
	}

	public String getString(long id, String column){
    	Cursor tmpcursor = getRow(id);
    	if(tmpcursor != null){
        	int col = tmpcursor.getColumnIndex(column);
    		return tmpcursor.getString(col);
    	}
		return null;
	}
	/**
	 * Returns a cursor pointing to the row containing the specified ID or null if not found.
	 * DO NOT CLOSE THE CURSOR.
	 * @param id
	 * @return cursor with specified row or null. DO NOT CLOSE.
	 */
	public Cursor getRow(long id){
    	Cursor tmpcursor = getCursor();
    	if(tmpcursor != null && tmpcursor.moveToFirst()){
    		do{
    			if(tmpcursor.getLong(tmpcursor.getColumnIndex(AwfulThread.ID)) == id){//contentprovider id tables are required to be _id
		    		return tmpcursor;
    			}
    		}while(tmpcursor.moveToNext());
    	}
    	return null;
	}
}
