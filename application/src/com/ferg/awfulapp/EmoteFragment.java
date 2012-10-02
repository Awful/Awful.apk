package com.ferg.awfulapp;

import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridView;

import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.service.AwfulCursorAdapter;

public class EmoteFragment extends AwfulDialogFragment implements OnClickListener {
	private EditText filterText;
	private GridView emoteGrid;
	private AwfulCursorAdapter adapter;

	@Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState);
	}

	@Override
	public void loadingSucceeded(Message aMsg) {
		// TODO Auto-generated method stub
		super.loadingSucceeded(aMsg);
	}

	@Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		// TODO Auto-generated method stub
		super.onPreferenceChange(prefs);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflateView(R.layout.emote_view, container, inflater);
		aq.find(R.id.delete_button).clicked(this);
		filterText = aq.find(R.id.filter_text).getEditText();
		emoteGrid = aq.find(R.id.emote_grid).adapter(adapter).getGridView();
		return v;
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
		//TODO
	}

}
