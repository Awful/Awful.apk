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

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.ferg.awfulapp.thread.AwfulHtmlPage;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.webview.AwfulWebView;
import com.ferg.awfulapp.webview.WebViewJsInterface;

import java.util.HashMap;

public class PreviewFragment extends DialogFragment {

    private AwfulWebView postPreView;
    private ProgressBar progressBar;

    HashMap<String, String> preferences;
    WebViewJsInterface jsInterface = new WebViewJsInterface();



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.post_preview, container);
        progressBar = dialogView.findViewById(R.id.preview_progress);
        postPreView = dialogView.findViewById(R.id.preview_webview);
        configureWebView();

        getDialog().setCanceledOnTouchOutside(true);
        postPreView.setContent(getBlankPage());

        return dialogView;
    }

    private String getBlankPage() {
        return AwfulHtmlPage.getContainerHtml(AwfulPreferences.getInstance(), 0, false);
    }


    protected void setContent(String content) {
        jsInterface.updatePreferences();
        // add the basic template HTML structure so this displays as a post, with the correct CSS styling etc
        String wrappedContent = "<style>iframe{height: auto !important;} </style><article class='post'><section class='postcontent'>"
                + content + "</section></article>";
        progressBar.setVisibility(View.GONE);
        postPreView.setVisibility(View.VISIBLE);
        postPreView.setBodyHtml(wrappedContent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // I don't know why I have to insist on that but alright then.
        postPreView.destroy();
    }

    public void configureWebView() {
        postPreView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest wrr) {
                return true;
            }
        });
        postPreView.setJavascriptHandler(jsInterface);
    }

}
