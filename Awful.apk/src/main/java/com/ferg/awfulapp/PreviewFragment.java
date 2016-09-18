/********************************************************************************
 * Copyright (c) 2012, Matthew Shepard
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the software nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
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

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;
import com.ferg.awfulapp.util.AwfulUtils;

import java.util.HashMap;

public class PreviewFragment extends AwfulDialogFragment {
    private final static String TAG = "PreviewFragment";

    private WebView postPreView;
    private View dialogView;
    private ProgressBar progressBar;
    protected String nicefiedContent = "";

    HashMap<String, String> preferences;

    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        dialogView = inflateView(R.layout.post_preview, container, inflater);
        progressBar = (ProgressBar) dialogView.findViewById(R.id.preview_progress);
        configureWebView();

        getDialog().setCanceledOnTouchOutside(true);
        postPreView.loadDataWithBaseURL(Constants.BASE_URL + "/",getBlankPage(), "text/html", "utf-8",null);

        return dialogView;
    }

    private String getBlankPage(){
        return AwfulThread.getContainerHtml(mPrefs, 0);
    }


    protected void setContent(String content) {
        preparePreferences();

        nicefiedContent =  "<style>iframe{height: auto !important;} </style><article><section class='postcontent'>"+ content+"</section></article>";
        progressBar.setVisibility(View.GONE);
        postPreView.setVisibility(View.VISIBLE);

        postPreView.loadUrl("javascript:loadPageHtml()");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // I don't know why I have to insist on that but alright then.
        postPreView.destroy();
    }

    public void configureWebView() {
        postPreView = (WebView) dialogView.findViewById(R.id.post_pre_view);
        postPreView.setBackgroundColor(Color.TRANSPARENT);
        postPreView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        postPreView.getSettings().setJavaScriptEnabled(true);
        postPreView.getSettings().setRenderPriority(WebSettings.RenderPriority.LOW);
        postPreView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
        postPreView.getSettings().setDefaultFontSize(mPrefs.postFontSizeDip);
        postPreView.getSettings().setDefaultFixedFontSize(mPrefs.postFixedFontSizeDip);
        postPreView.setWebChromeClient(new WebChromeClient());
        if (Constants.DEBUG && AwfulUtils.isKitKat()) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        if (AwfulUtils.isLollipop()) {
            postPreView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        if (mPrefs.inlineYoutube || mPrefs.inlineWebm || mPrefs.inlineVines) {//YOUTUBE SUPPORT BLOWS
            postPreView.getSettings().setPluginState(WebSettings.PluginState.ON_DEMAND);
        }
        if (AwfulUtils.isJellybean()) {
            postPreView.getSettings().setAllowUniversalAccessFromFileURLs(true);
            postPreView.getSettings().setAllowFileAccessFromFileURLs(true);
            postPreView.getSettings().setAllowFileAccess(true);
            postPreView.getSettings().setAllowContentAccess(true);
        }

        postPreView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest wrr ) {
                return true;
            }
        });
        postPreView.addJavascriptInterface(this, "listener");
    }

    @JavascriptInterface
    public String getPreference(String preference) {
        return preferences.get(preference);
    }

    @JavascriptInterface
    public void haltSwipe() {

    }
    @JavascriptInterface
    public void resumeSwipe() {
    }

    @JavascriptInterface
    public String getBodyHtml(){
        return nicefiedContent;
    }

    private void preparePreferences() {
        AwfulPreferences aPrefs = AwfulPreferences.getInstance();

        preferences = new HashMap<String, String>();
        preferences.clear();
        preferences.put("username", aPrefs.username);
        preferences.put("showSpoilers", Boolean.toString(aPrefs.showAllSpoilers));
        preferences.put("highlightUserQuote", Boolean.toString(aPrefs.highlightUserQuote));
        preferences.put("highlightUsername", Boolean.toString(aPrefs.highlightUsername));
        preferences.put("inlineTweets", Boolean.toString(aPrefs.inlineTweets));
        preferences.put("inlineWebm", Boolean.toString(aPrefs.inlineWebm));
        preferences.put("autostartWebm", Boolean.toString(aPrefs.autostartWebm));
        preferences.put("inlineVines", Boolean.toString(aPrefs.inlineVines));
        preferences.put("disableGifs", Boolean.toString(aPrefs.disableGifs));
        preferences.put("hideSignatures", Boolean.toString(aPrefs.hideSignatures));
        preferences.put("disablePullNext", Boolean.toString(aPrefs.disablePullNext));
    }

    @Override
    public String getTitle() {
        return "Post Preview";
    }

    @Override
    public boolean volumeScroll(KeyEvent event) {
        return false;
    }
}
