package com.ferg.awfulapp;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
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
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.service.AwfulCursorAdapter;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.thread.AwfulEmote;
import com.ferg.awfulapp.thread.AwfulForum;

public class EmoteFragment extends AwfulDialogFragment implements OnClickListener, OnItemClickListener {
	private EditText filterText;
	private GridView emoteGrid;
	private AwfulCursorAdapter adapter;
	private EmoteDataCallback emoteLoader = new EmoteDataCallback();
	
	private boolean loadFailed = false;

	@Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState);
		getDialog().setTitle("Emotes");
	}

	@Override
	public void loadingSucceeded(Message aMsg) {
		super.loadingSucceeded(aMsg);
		if(getAwfulActivity() != null){
			restartLoader();
		}
		setProgress(100);
	}

	@Override
	public void loadingFailed(Message aMsg) {
		super.loadingFailed(aMsg);
		loadFailed = true;
	}

	@Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		super.onPreferenceChange(prefs);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflateView(R.layout.emote_view, container, inflater);
		adapter = new AwfulCursorAdapter(getAwfulActivity(), null);
		aq.find(R.id.delete_button).clicked(this);
		filterText = aq.find(R.id.filter_text).textColor(mPrefs.postFontColor).getEditText();
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
		emoteGrid = aq.find(R.id.emote_grid).adapter(adapter).itemClicked(this).backgroundColor(mPrefs.postBackgroundColor).getGridView();
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
	public void onPageVisible() {
	}

	@Override
	public void onPageHidden() {
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
		if(getAwfulActivity() != null){
			getLoaderManager().restartLoader(Constants.EMOTE_LOADER_ID, null, emoteLoader);
		}
	}

	public void syncEmotes() {
		if(getAwfulActivity() != null){
			getAwfulActivity().sendMessage(mMessenger, AwfulSyncService.MSG_FETCH_EMOTES, 0, 0);
		}
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

	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
		TextView tv = (TextView) v.findViewById(R.id.emote_text);
		Toast.makeText(getAwfulActivity(), tv.getText().toString().trim(), Toast.LENGTH_SHORT).show();
		sendFragmentMessage("emote-selected", tv.getText().toString().trim());
		dismiss();
	}
}
