package com.ferg.awful.service;

import com.ferg.awful.R;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.thread.AwfulForum;
import com.ferg.awful.thread.AwfulThread;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AwfulCursorAdapter extends CursorAdapter {
	private AwfulPreferences mPrefs;
	public AwfulCursorAdapter(Context context, Cursor c) {
		super(context, c, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		mPrefs = new AwfulPreferences(context);
	}

	@Override
	public void bindView(View current, Context context, Cursor data) {
		if(data.getColumnIndex(AwfulThread.BOOKMARKED) >= 0){//unique to threads
			AwfulThread.getView(current, mPrefs, data);
		}else if(data.getColumnIndex(AwfulForum.PARENT_ID) >= 0){//unique to forums
			AwfulForum.getView(current, mPrefs, data);
		}
	}

	@Override
	public View newView(Context context, Cursor data, ViewGroup parent) {
		LayoutInflater inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View row;
		if(data.getColumnIndex(AwfulThread.BOOKMARKED) >= 0){//unique to threads
			row = inf.inflate(R.layout.thread_item, parent, false);
			AwfulThread.getView(row, mPrefs, data);
		}else if(data.getColumnIndex(AwfulForum.PARENT_ID) >= 0){//unique to forums
			row = inf.inflate(R.layout.forum_item, parent, false);
			AwfulForum.getView(row, mPrefs, data);
		}else{
			row = inf.inflate(R.layout.loading, parent, false);
		}
		return row;
	}
}
