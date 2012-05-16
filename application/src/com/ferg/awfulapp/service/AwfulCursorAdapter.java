package com.ferg.awfulapp.service;

import greendroid.widget.AsyncImageView;

import java.util.HashMap;
import java.util.TreeMap;

import com.ferg.awfulapp.AwfulActivity;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.task.ImageCacheTask;
import com.ferg.awfulapp.thread.AwfulEmote;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class AwfulCursorAdapter extends CursorAdapter {
	private static final String TAG = "AwfulCursorAdapter";
	private AwfulPreferences mPrefs;
	private AwfulActivity mParent;
	private LayoutInflater inf;
	private int mId;
	private int selectedId = -1;
	private boolean mIsSidebar;
	
	public AwfulCursorAdapter(AwfulActivity context, Cursor c) {
		this(context, c, 0, false, null);
	}
	public AwfulCursorAdapter(AwfulActivity context, Cursor c, int id, boolean isSidebar, Messenger tagCallback) {
		super(context, c, 0);
		mPrefs = new AwfulPreferences(context);
		inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mParent = context;
		mId = id;
		mIsSidebar = isSidebar;
		mReplyTo = tagCallback;
	}
	
	public void setSelected(int id){
		selectedId = id;
	}
	
	public void setSidebar(boolean isSidebar){
		mIsSidebar = isSidebar;
	}

	@Override
	public void bindView(View current, Context context, Cursor data) {
		ImageView tag = null;
		if(data.getColumnIndex(AwfulThread.BOOKMARKED) >= 0){//unique to threads
			tag = AwfulThread.getView(current, mPrefs, data, mParent, mId == Constants.USERCP_ID, mIsSidebar, false);
		}else if(data.getColumnIndex(AwfulForum.PARENT_ID) >= 0){//unique to forums
			AwfulForum.getSubforumView(current, mParent, mPrefs, data, mIsSidebar, false);
		}else if(data.getColumnIndex(AwfulMessage.DATE) >= 0){
			AwfulMessage.getView(current, mPrefs, data, mIsSidebar, false);
		}else if(data.getColumnIndex(AwfulEmote.CACHEFILE) >= 0){
			AwfulEmote.getView(current, mPrefs, data);
		}
		mParent.setPreferredFont(current);
		if(tag != null){
			Object tagStuff = tag.getTag();
			if(tagStuff instanceof String[]){
				String[] tagFile = (String[]) tagStuff;
				mParent.sendMessage(mReplyTo, AwfulSyncService.MSG_GRAB_IMAGE, mId, tagFile[1].hashCode(), tagFile[1]);
				tag.setTag(tagFile[0]);
				imageQueue.put(tagFile[1].hashCode(), tag);
			}else if(tagStuff instanceof String){
				AwfulThread.setBitmap(mParent, tag, imageCache);
			}
		}
	}

	@Override
	public View newView(Context context, Cursor data, ViewGroup parent) {
		View row;
		ImageView tag = null;
		if(data.getColumnIndex(AwfulThread.BOOKMARKED) >= 0){//unique to threads
			row = inf.inflate(R.layout.thread_item, parent, false);
			tag = AwfulThread.getView(row, mPrefs, data, mParent, mId == Constants.USERCP_ID, mIsSidebar, false);
		}else if(data.getColumnIndex(AwfulForum.PARENT_ID) >= 0){//unique to forums
			row = inf.inflate(R.layout.thread_item, parent, false);
			AwfulForum.getSubforumView(row, mParent, mPrefs, data, mIsSidebar, false);
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
		if(tag != null){
			Object tagStuff = tag.getTag();
			if(tagStuff instanceof String[]){
				String[] tagFile = (String[]) tagStuff;
				mParent.sendMessage(mReplyTo, AwfulSyncService.MSG_GRAB_IMAGE, mId, tagFile[1].hashCode(), tagFile[1]);
				tag.setTag(tagFile[0]);
				imageQueue.put(tagFile[1].hashCode(), tag);
			}else if(tagStuff instanceof String){
				AwfulThread.setBitmap(mParent, tag, imageCache);
			}
		}
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
	
	private HashMap<Integer,ImageView> imageQueue = new HashMap<Integer,ImageView>();
	private HashMap<String,Bitmap> imageCache = new HashMap<String,Bitmap>();
	private Handler mImageHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			if(msg.arg1 == AwfulSyncService.Status.OKAY && msg.what == AwfulSyncService.MSG_GRAB_IMAGE){
				ImageView imgTag = imageQueue.remove(msg.arg2);
				if(imgTag != null){
					AwfulThread.setBitmap(mParent, imgTag, imageCache);
				}
			}
		}
		
	};
	private Messenger mReplyTo = new Messenger(mImageHandler);
}
