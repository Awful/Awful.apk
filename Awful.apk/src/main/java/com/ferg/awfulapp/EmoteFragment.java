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

package com.ferg.awfulapp;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.reply.MessageComposer;
import com.ferg.awfulapp.service.AwfulCursorAdapter;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.EmoteRequest;
import com.ferg.awfulapp.thread.AwfulEmote;

public class EmoteFragment extends AwfulDialogFragment implements OnClickListener, OnItemClickListener {
	private final static String TAG = "EmoteFragment";
	private EditText filterText;
	private GridView emoteGrid;
	private AwfulCursorAdapter adapter;
	private EmoteDataCallback emoteLoader = new EmoteDataCallback();

	private MessageComposer parent;
	
	private boolean loadFailed = false;

	public EmoteFragment(MessageComposer parent) {
		super();
        this.parent = parent;
    }

    public EmoteFragment() {
        super();
    }

    @Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState);
		getDialog().setTitle("Emotes");
	}

	@Override
	public void onPreferenceChange(AwfulPreferences prefs, String key) {
		super.onPreferenceChange(prefs, key);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflateView(R.layout.emote_view, container, inflater);
		adapter = new AwfulCursorAdapter(getAwfulActivity(), null, null);

		Button deleteButton = (Button) v.findViewById(R.id.delete_button);
		deleteButton.setOnClickListener(this);
		filterText = (EditText) v.findViewById(R.id.filter_text);
		filterText.setTextColor(ColorProvider.getTextColor());
		filterText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {
				updateFilter();
			}
		});
		emoteGrid = (GridView) v.findViewById(R.id.emote_grid);
		emoteGrid.setAdapter(adapter);
		emoteGrid.setOnItemClickListener(this);
		emoteGrid.setBackgroundColor(ColorProvider.getBackgroundColor());

		return v;
	}

	@Override
	public void onStart() {
		super.onStart();
		restartLoader();
	}

	@Override
	public void onStop() {
		super.onStop();
		getLoaderManager().destroyLoader(Constants.EMOTE_LOADER_ID);
	}

	@Override
	public String getTitle() {
		return "Emotes";
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.delete_button:
			filterText.setText("");
			updateFilter();
			break;
		}
	}
	
	private void updateFilter(){
		emoteLoader.setFilterText(filterText.getText().toString().trim());
		restartLoader();
	}
	
	private void restartLoader(){
		restartLoader(Constants.EMOTE_LOADER_ID, null, emoteLoader);
	}

	public void syncEmotes() {
		if(getActivity() != null){
            queueRequest(new EmoteRequest(getActivity()).build(this, new AwfulRequest.AwfulResultCallback<Void>() {
                @Override
                public void success(Void result) {
                    if(getAwfulActivity() != null){
                        restartLoader();
                        loadFailed = false;
                    }
                }

                @Override
                public void failure(VolleyError error) {
                    loadFailed = true;
                }
            }));
		}
    }

	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
		TextView tv = (TextView) v.findViewById(R.id.emote_text);
		Toast.makeText(getAwfulActivity(), tv.getText().toString().trim(), Toast.LENGTH_SHORT).show();
		parent.insertEmote(tv.getText().toString().trim());
		dismiss();
	}

	@Override
	public boolean volumeScroll(KeyEvent event) {
		return false;
	}

	private class EmoteDataCallback implements LoaderManager.LoaderCallbacks<Cursor>{
		private String filter;
		@Override
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        	Log.v(TAG,"Creating emote cursor");
			return new CursorLoader(getActivity(),
					AwfulEmote.CONTENT_URI,
					AwfulProvider.EmoteProjection,
					(filter!=null && filter.length() > 0? AwfulEmote.TEXT+" LIKE '%' || ? || '%'" : null),
            						(filter!=null && filter.length() > 0? new String[]{filter} : null),
            						AwfulEmote.INDEX);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> l, Cursor c) {
			adapter.swapCursor(c);
			if(c.getCount() < 5 && (filter == null || filter.length()<1) && !loadFailed){
				syncEmotes();
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			adapter.swapCursor(null);
		}

		public void setFilterText(String text){
			filter = text;
		}
	}
}
