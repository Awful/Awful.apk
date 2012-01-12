package com.ferg.awful.thread;

import com.ferg.awful.preferences.AwfulPreferences;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public interface AwfulDisplayItem {
	public View getView(LayoutInflater inf, View current, ViewGroup parent, AwfulPreferences aPref, Cursor data);
	public static enum DISPLAY_TYPE {POST , THREAD, FORUM, SUBFORUM};
}
