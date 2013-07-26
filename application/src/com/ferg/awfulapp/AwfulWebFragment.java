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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.widget.AwfulFragmentPagerAdapter.AwfulPagerFragment;

public class AwfulWebFragment extends AwfulDialogFragment implements AwfulPagerFragment {
	private static final String TAG = "AwfulWebFragment";

	private Handler mHandler = new Handler();
	private WebView mWebView;
	
	private AwfulPreferences mPrefs;
	
	private String mUrl;
	private int internalContentWidth;
	private boolean loadStarted;

	public static AwfulWebFragment newInstance(String url){
		Log.e(TAG, "newInstance");
		AwfulWebFragment frag = new AwfulWebFragment();
		Bundle args = new Bundle();
		args.putString("url", url);
		frag.setArguments(args);
		return frag;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); Log.e(TAG, "onCreate");
        setHasOptionsMenu(true);
		mPrefs = AwfulPreferences.getInstance(getActivity());
		setStyle(STYLE_NO_FRAME, R.style.Theme_Sherlock_Light);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) { Log.e(TAG, "onCreateView");
		View result = inflater.inflate(R.layout.web_display, container, false);
		mWebView = (WebView) result.findViewById(R.id.web_view);
		mWebView.setWebViewClient(callback);
		mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new AwfulJSInterface(), "AwfulJS");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mWebView.getSettings().setEnableSmoothTransition(true);
			mWebView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
		}

		mWebView.setWebChromeClient(new WebChromeClient() {
			public void onConsoleMessage(String message, int lineNumber, String sourceID) {
				Log.d("Web Console", message + " -- From line " + lineNumber + " of " + sourceID);
			}
		});
		return result;
	}
	
	@Override
	public void onActivityCreated(Bundle aSavedState) {
		super.onActivityCreated(aSavedState); Log.e(TAG, "onActivityCreated");
		mWebView.setBackgroundColor(ColorProvider.getBackgroundColor(mPrefs));
		mUrl = getArguments().getString("url");
	}
	
	@Override
	public void onStart() {
		super.onStart(); Log.e(TAG, "onStart");
	}

	@Override
	public void onResume() {
		super.onResume(); Log.e(TAG, "onResume");
        resumeWebView();
		if(mWebView != null && !loadStarted){
			mWebView.loadUrl(mUrl);
			loadStarted = true;
		}
	}

    public void resumeWebView(){
        if (mWebView != null) {
        	mWebView.onResume();
        	mWebView.resumeTimers();
        }
    }
    
	@Override
	public void onPageVisible() {
		resumeWebView();
	}

	@Override
	public void onPageHidden() {
		pauseWebView();
	}
    
    private void pauseWebView(){
        if (mWebView != null) {
	    	mWebView.pauseTimers();
	    	mWebView.onPause();
        }
    }
	@Override
	public void onPause() {
		super.onPause(); Log.e(TAG, "onPause");
        pauseWebView();
	}

	@Override
	public void onSaveInstanceState(Bundle arg0) {
		super.onSaveInstanceState(arg0); Log.e(TAG, "onSaveInstanceState");
	}

	@Override
	public void onStop() {
		super.onStop(); Log.e(TAG, "onStop");
        if (mWebView != null) {
        	mWebView.stopLoading();
        }
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView(); Log.e(TAG, "onDestroyView");
		//mWebView.destroy();
		mWebView = null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy(); Log.e(TAG, "onDestroy");
	}
	
	public void loadUrl(String url){
		mUrl = url;
		if(mWebView != null){
			mWebView.loadUrl(mUrl);
		}
	}

	private WebViewClient callback = new WebViewClient(){
		
		private Runnable mUpdateThread;

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Log.i(TAG,"Page Started: "+url);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			Log.i(TAG,"Page load finished: "+url);
			if(getActivity() != null && mWebView != null){
				getAwfulActivity().setActionbarTitle(mWebView.getTitle(), AwfulWebFragment.this);
				mWebView.loadUrl("javascript:window.AwfulJS.setContentWidth(document.getElementsByTagName('html')[0].scrollWidth);");
			}
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
			if(mUpdateThread == null){
				mUpdateThread = new Runnable(){
					@Override
					public void run() {
						getAwfulActivity().setActionbarTitle(mWebView.getTitle(), AwfulWebFragment.this);
						mWebView.loadUrl("javascript:window.AwfulJS.setContentWidth(document.getElementsByTagName('html')[0].scrollWidth);");
						getAwfulActivity().invalidateOptionsMenu();
						mUpdateThread = null;
					}
				};
				mHandler.postDelayed(mUpdateThread, 200);
			}
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView aView, String aUrl) {
			return false;
		}
	};

	@Override
	public boolean onBackPressed() {
		return false;
	}



	@Override
	public String getTitle() {
		if(mWebView != null){
			return mWebView.getTitle();
		}
		return "QuickBrowser";
	}

	@Override
	public boolean canScrollX(int x, int y) {
		int ow = mWebView.getWidth();
		int l = mWebView.getScrollX();
		int r = l+mWebView.getWidth();
		//Log.e(TAG,"canScroll x:"+x+" ow:"+ow+" l:"+l+" r:"+r+" in:"+internalContentWidth);
		if(ow >= internalContentWidth){
			return false;
		}
		if(l < x){
			return false;
		}
		if(l-x >= internalContentWidth-ow){
			return false;
		}
		return true;
	}
	public AwfulActivity getAwfulActivity(){
		return (AwfulActivity) getActivity();
	}
	
	private class AwfulJSInterface {
        public void setContentWidth(String width) {
            if (width != null) {
            	internalContentWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Integer.parseInt(width), getResources().getDisplayMetrics());
                Log.d(TAG, "ConentWidth: " + internalContentWidth+" winWidth: "+mWebView.getWidth());
            }
        }
    }
	
	
	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) { 
    	Log.e(TAG, "onCreateOptionsMenu");
    	if(menu.size() == 0){
    		inflater.inflate(R.menu.browser_menu, menu);
    	}
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    	Log.e(TAG, "onCreateOptionsMenu");
        if(menu == null){
            return;
        }
        MenuItem nextArrow = menu.findItem(R.id.next_page);
        if(nextArrow != null){
        	nextArrow.setVisible(mWebView != null && mWebView.canGoForward());
        	nextArrow.setEnabled(mWebView != null && mWebView.canGoForward());
        }
        MenuItem backArrow = menu.findItem(R.id.prev_page);
        if(backArrow != null){
        	backArrow.setVisible(mWebView != null && mWebView.canGoBack());
        	backArrow.setEnabled(mWebView != null && mWebView.canGoBack());
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.next_page:
            	mWebView.goForward();
                break;
            case R.id.prev_page:
            	mWebView.goBack();
                break;
            case R.id.refresh:
                mWebView.reload();
                break;
            case R.id.usercp:
            	getAwfulActivity().displayUserCP();
                break;
            case R.id.settings:
                startActivity(new Intent().setClass(getActivity(), SettingsActivity.class));
                break;
    		case R.id.copy_url:
    			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
    				ClipboardManager clipboard = (ClipboardManager) this.getActivity().getSystemService(
    						Context.CLIPBOARD_SERVICE);
    				ClipData clip = ClipData.newPlainText(this.getText(R.string.copy_url).toString(), mWebView.getUrl());
    				clipboard.setPrimaryClip(clip);

    				Toast.makeText(this.getActivity().getApplicationContext(), getString(R.string.copy_url_success), Toast.LENGTH_SHORT).show();
    			} else {
    				android.text.ClipboardManager clipboard = (android.text.ClipboardManager) this.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
    				clipboard.setText(mWebView.getUrl());
    				Toast.makeText(this.getActivity().getApplicationContext(), getString(R.string.copy_url_success), Toast.LENGTH_SHORT).show();
    			}
    			break;
    		default:
    			return super.onOptionsItemSelected(item);
    		}

    		return true;
    	}

	@Override
	public int getProgressPercent() {
		if(mWebView != null){
			return mWebView.getProgress();
		}
		return 100;
	}

	@Override
	public String getInternalId() {
		return TAG;
	}

	@Override
	public boolean volumeScroll(KeyEvent event) {
	    int action = event.getAction();
	    int keyCode = event.getKeyCode();    
	        switch (keyCode) {
	        case KeyEvent.KEYCODE_VOLUME_UP:
	            if (action == KeyEvent.ACTION_DOWN) {
	            	mWebView.pageUp(false);   
	            }
	            return true;
	        case KeyEvent.KEYCODE_VOLUME_DOWN:
	            if (action == KeyEvent.ACTION_DOWN) {
	            	mWebView.pageDown(false);
	            }
	            return true;
	        default:
	            return false;
	        }
	}
}
