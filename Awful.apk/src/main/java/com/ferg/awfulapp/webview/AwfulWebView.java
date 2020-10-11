package com.ferg.awfulapp.webview;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.thread.AwfulHtmlPage;

import timber.log.Timber;

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
 * <p>
 * Most of the time you'll want to use {@link #setContent(String)} to add the template from
 * {@link AwfulHtmlPage#getContainerHtml(AwfulPreferences, Integer, boolean)}, which
 * loads the HTML, CSS and JS for displaying thread content, and then use {@link #setBodyHtml(String)}
 * to add and display that content. {@link #setJavascriptHandler(WebViewJsInterface)} needs to be
 * called, since the thread JS relies on it.
 * <p>
 * You can also run arbitrary JavaScript code with the {@link #runJavascript(String)} method.
 */

public class AwfulWebView extends WebView {

    public static final String TAG = "AwfulWebView";
    /**
     * thread.js uses this identifier to communicate with any handler we add
     */
    private static final String HANDLER_NAME_IN_JAVASCRIPT = "listener";

    @Nullable
    private WebViewJsInterface jsInterface = null;

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
        setWebChromeClient(new LoggingWebChromeClient(this));
        setKeepScreenOn(false); // explicitly setting this since some people are complaining the screen stays on until they toggle it on and off

        setBackgroundColor(Color.TRANSPARENT);
        setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultFontSize(prefs.postFontSizeSp);
        webSettings.setDefaultFixedFontSize(prefs.postFixedFontSizeSp);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        if (DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        if (prefs.inlineWebm || prefs.inlineVines) {
            webSettings.setMediaPlaybackRequiresUserGesture(false);
        }

        if (prefs.inlineTweets) {
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
        jsInterface = handler;
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
     *
     * @param javascript the code to run
     */
    public void runJavascript(@NonNull String javascript) {
        loadUrl("javascript:" + javascript);
    }


    /**
     * Calls the javascript function that displays the current body HTML
     * <p>
     * This calls the #loadPageHtml function in <i>thread.js</i>, which displays the HTML passed to
     * {@link #setBodyHtml(String)}. Calling this with unchanged HTML acts as a refresh, resetting
     * the displayed state of that page.
     *
     */
    public void refreshPageContents() {
        runJavascript("loadPageHtml()");
    }


    /**
     * Set and display the current HTML for the container body.
     * <p>
     * Call this to update the WebView with new HTML content, calling {@link #refreshPageContents()}
     * to display it. Does nothing if the passed HTML is unchanged from the currently added HTML,
     * or if {@link #setJavascriptHandler(WebViewJsInterface)} hasn't been called yet.
     */
    public void setBodyHtml(@Nullable String html) {
        if (jsInterface == null) {
            Timber.w("Attempted to set html with no JS interface handler added");
            return;
        }
        if (html != null && html.hashCode() == jsInterface.getBodyHtml().hashCode()) {
            Timber.d("New HTML appears to match the current HTML, not updating");
            return;
        }
        jsInterface.setBodyHtml(html);
        refreshPageContents();
    }

}
