package com.ferg.awfulapp.webview;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.util.AwfulUtils;

import static com.ferg.awfulapp.constants.Constants.DEBUG;

/**
 * Created by baka kaba on 21/01/2017.
 * <p>
 * A WebView pre-configured to display Awful content.
 * <p>
 * Because the app tends to use WebViews in various places, but with the same configuration
 * and method calls, this class is meant to collect everything in one place, so the WebView
 * can be dropped in with minimal configuration and tweaking, and classes can call simple
 * functions instead of concerning themselves with too many of the technical details.
 * <p>
 * To use it, add it to a layout. Call {@link #setContent(String)} to display some HTML,
 * and use {@link #setJavascriptHandler(WebViewJsInterface)} if you want to handle some
 * JavaScript on the page. By default this uses a {@link LoggingWebChromeClient} to add
 * some debug logging, and the default {@link android.webkit.WebViewClient}. You should
 * call {@link #onPause()} and {@link #onResume()} to handle those lifecycle events.
 *
 * You can run arbitrary JavaScript code with the {@link #runJavascript(String)} method, or invoke
 * the thread JavaScript's own loadPageHtml function with {@link #refreshPageContents(boolean)}.
 */

public class AwfulWebView extends WebView {

    public static final String TAG = "AwfulWebView";
    /** thread.js uses this identifier to communicate with any handler we add */
    private static final String HANDLER_NAME_IN_JAVASCRIPT = "listener";

    public AwfulWebView(Context context) {
        super(context);
        init();
    }

    public AwfulWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AwfulWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AwfulWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    /**
     * Do the basic configuration for the app's WebViews.
     */
    private void init() {
        AwfulPreferences prefs = AwfulPreferences.getInstance();
        WebSettings webSettings = getSettings();
        setWebChromeClient(new LoggingWebChromeClient());

        setBackgroundColor(Color.TRANSPARENT);
        setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultFontSize(prefs.postFontSizeSp);
        webSettings.setDefaultFixedFontSize(prefs.postFixedFontSizeSp);
        webSettings.setDomStorageEnabled(true);

        if (AwfulUtils.isLollipop()) {
            //noinspection AndroidLintNewApi, AndroidLintInlinedApi
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        if (DEBUG) {
            //noinspection AndroidLintNewApi
            WebView.setWebContentsDebuggingEnabled(true);
        }

        if (prefs.inlineWebm || prefs.inlineVines) {
            //noinspection AndroidLintNewApi
            webSettings.setMediaPlaybackRequiresUserGesture(false);
        }

        if (prefs.inlineTweets) {
            //noinspection AndroidLintNewApi
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            webSettings.setAllowFileAccess(true);
            webSettings.setAllowContentAccess(true);
        }
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
        addJavascriptInterface(handler, HANDLER_NAME_IN_JAVASCRIPT);
    }


    /**
     * Set the content of this webview.
     * <p>
     * This loads the content string into the webview, treating it as UTF-8 HTML, and resolving any
     * relative URLs using the Something Awful base URL. If content is <i>null</i>, the webview
     * will be empty.
     * <p>
     * Use this method to insert basic content - if you need to do anything more complex, use
     * {@link WebView#loadData(String, String, String)} and {@link WebView#loadDataWithBaseURL(String, String, String, String, String)}
     *
     * @param content the HTML content to display, or null to clear the webview
     */
    public void setContent(@Nullable String content) {
        content = (content == null) ? "" : content;
        loadDataWithBaseURL(Constants.BASE_URL + "/", content, "text/html", "UTF-8", null);
    }


    /**
     * Helper function to execute some jabbascript in the webview.
     * @param javascript the code to run
     */
    public void runJavascript(@NonNull String javascript) {
        loadUrl("javascript:" + javascript);
    }


    /**
     * Calls the javascript function that updates some page content from its source.
     *
     * This calls the #loadPageHtml function in <i>thread.js</i>, which in turn calls #getBodyHtml
     * on the handler passed to {@link #setJavascriptHandler(WebViewJsInterface)}, and inserts the
     * results into a container on the page.
     *
     * @param force if false the page will only update if it's currently blank.
     */
    public void refreshPageContents(boolean force) {
        runJavascript(String.format("loadPageHtml(%s)", force ? "" : "true"));
    }

}
