package com.ferg.awfulapp;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.ferg.awfulapp.preferences.AwfulPreferences;

public class AwfulWebFragment extends SherlockDialogFragment {
	private static final String TAG = "AwfulWebFragment";

	private Handler mHandler = new Handler();
	private WebView mWebView;
	
	private AwfulPreferences mPrefs;
	
	private String url;

	public static AwfulWebFragment newInstance(String url){
		AwfulWebFragment frag = new AwfulWebFragment();
		Bundle args = new Bundle();
		args.putString("url", url);
		frag.setArguments(args);
		return frag;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPrefs = new AwfulPreferences(getActivity());
		setStyle(STYLE_NO_FRAME, R.style.Theme_Sherlock_Light);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View result = inflater.inflate(R.layout.web_display, container, true);
		mWebView = (WebView) result.findViewById(R.id.web_view);
		mWebView.setWebViewClient(callback);
		
		return result;
	}
	
	@Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState);
		mWebView.setBackgroundColor(mPrefs.postBackgroundColor);
		url = getArguments().getString("url");
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
		if(mWebView != null){
			mWebView.onResume();
			mWebView.resumeTimers();
			mWebView.loadUrl(url);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if(mWebView != null){
			mWebView.stopLoading();
			mWebView.pauseTimers();
			mWebView.onPause();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle arg0) {
		super.onSaveInstanceState(arg0);
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mWebView.destroy();
		mWebView = null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private WebViewClient callback = new WebViewClient(){
		@Override
		public void onPageFinished(WebView view, String url) {
			if(!isResumed()){
				Log.e(TAG,"PageFinished after pausing. Forcing Webview.pauseTimers");
				mHandler.postDelayed(new Runnable(){
					@Override
					public void run() {
						if(mWebView != null){
							mWebView.pauseTimers();
							mWebView.onPause();
						}
					}
				}, 500);
			}
		}

		public void onLoadResource(WebView view, String url) {
			Log.i(TAG,"Load Resource: "+url);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView aView, String aUrl) {
			return false;
		}
	};
}
