package com.ferg.awful.thread;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ferg.awful.preferences.AwfulPreferences;

public class AwfulEmote {

	public static final String ID = "_id";
	public static final String TEXT = "text";
	public static final String SUBTEXT = "subtext";//hover text
	public static final String URL = "url";
	public static final String CACHEFILE = "cachefile";//location of cached file or null if not cached yet.
	public static void getView(View current, AwfulPreferences aPref, Cursor data) {
	}

}
