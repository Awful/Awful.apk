package com.ferg.awfulapp.webview;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * Created by baka kaba on 21/01/2017.
 */

public class AwfulWebView extends WebView {

    public static final String TAG = "AwfulWebView";

    public AwfulWebView(Context context) {
        super(context);
    }

    public AwfulWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AwfulWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AwfulWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    @Override
    public void onPause() {
        pauseTimers();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeTimers();
    }

    /**
     * Connects a handler to the JavaScript functions added to HTML for threads, posts, messages etc.
     * <p>
     * The base {@link WebViewJsInterface} handles some basic functions and preference injection.
     * If you need to handle other specific stuff (e.g. thread option menus), subclass it and add
     * the extra JavascriptInterface handler methods to that.
     *
     * @param handler an object containing JavaScriptInterface methods to handle JS function calls
     */
    public void setJavascriptHandler(@NonNull WebViewJsInterface handler) {
        addJavascriptInterface(handler, "listener");
    }

    /**
     * Calls the javascript function that updates the page HTML from its source.
     *
     * @param force if false the page will only update if it's currently blank.
     */
    public void refreshPageContents(boolean force) {
        loadUrl(String.format("javascript:loadPageHtml(%s)", force ? "" : "true"));
    }

    // TODO: 28/01/2017 Add other common functions, e.g. initialising different 'blank' templates, so fragments don't have to explicitly call loadDataWithBaseUrl(blaaaaa) or whatever
}
