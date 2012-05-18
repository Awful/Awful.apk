package com.ferg.awfulapp;

import java.util.List;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.ferg.awfulapp.preferences.AwfulPreferences;

public class AwfulWebFragment extends AwfulFragment {
	private static final String TAG = "AwfulWebFragment";

	private Handler mHandler = new Handler();
	private WebView mWebView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		onPreferenceChange(mPrefs);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View result = inflater.inflate(R.layout.web_display, container);
		mWebView = (WebView) result.findViewById(R.id.web_view);
		
		return result;
	}
	
	@Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState);
	}
	
	@Override
	public void onPreferenceChange(AwfulPreferences prefs) {
		super.onPreferenceChange(prefs);
		mWebView.setBackgroundColor(prefs.postBackgroundColor);
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
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
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(aUrl));
			PackageManager pacman = aView.getContext().getPackageManager();
			List<ResolveInfo> res = pacman.queryIntentActivities(browserIntent,
					PackageManager.MATCH_DEFAULT_ONLY);
			if (res.size() > 0) {
				browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				aView.getContext().startActivity(browserIntent);
			} else {
				String[] split = aUrl.split(":");
				Toast.makeText(
						aView.getContext(),
						"No application found for protocol"
								+ (split.length > 0 ? ": " + split[0] : "."), Toast.LENGTH_LONG)
						.show();
			}
			return true;
		}
	};
}
