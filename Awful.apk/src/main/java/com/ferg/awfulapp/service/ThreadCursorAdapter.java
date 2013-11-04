/********************************************************************************
 * Copyright (c) 2012, Matthew Shepard
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

package com.ferg.awfulapp.service;

import android.content.Context;
import android.database.Cursor;
import android.os.Messenger;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.androidquery.AQuery;
import com.ferg.awfulapp.AwfulActivity;
import com.ferg.awfulapp.AwfulFragment;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.thread.*;

public class ThreadCursorAdapter extends CursorAdapter {
	private static final String TAG = "ThreadCursorAdapter";
	private AwfulPreferences mPrefs;
	private AwfulActivity mParent;
    private AwfulFragment mFragment;
	private LayoutInflater inf;
	private AQuery aq;

	public ThreadCursorAdapter(AwfulActivity context, Cursor c, AwfulFragment fragment) {
		super(context, c, 0);
		mPrefs = AwfulPreferences.getInstance(context);
		inf = LayoutInflater.from(context);
		mParent = context;
		aq = new AQuery(context);
        mFragment = fragment;
	}

	@Override
	public void bindView(View current, Context context, Cursor data) {
        AwfulThread.getView(current, mPrefs, data, aq, mFragment);
		mParent.setPreferredFont(current);
	}

	@Override
	public View newView(Context context, Cursor data, ViewGroup parent) {
		View row = inf.inflate(R.layout.nu_thread_item, parent, false);
        AwfulThread.getView(row, mPrefs, data, aq, mFragment);
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
