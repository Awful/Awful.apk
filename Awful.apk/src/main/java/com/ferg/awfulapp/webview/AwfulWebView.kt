package com.ferg.awfulapp.webview

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.webkit.WebSettings
import android.webkit.WebView
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.constants.Constants.DEBUG
import com.ferg.awfulapp.preferences.AwfulPreferences.Companion.getInstance
import timber.log.Timber.Forest.d
import timber.log.Timber.Forest.w

/**
 * Created by baka kaba on 21/01/2017.
 * 
 * 
 * A WebView pre-configured to display Awful content.
 * 
 * 
 * Because the app tends to use WebViews in various places, but with the same configuration
 * and method calls, this class is meant to collect everything in one place, so the WebView
 * can be dropped in with minimal configuration and tweaking, and classes can call simple
 * functions instead of concerning themselves with too many of the technical details.
 * 
 * 
 * To use it, add it to a layout. Call [.setContent] to display some HTML,
 * and use [.setJavascriptHandler] if you want to handle some
 * JavaScript on the page. By default this uses a [LoggingWebChromeClient] to add
 * some debug logging, and the default [android.webkit.WebViewClient]. You should
 * call [.onPause] and [.onResume] to handle those lifecycle events.
 * 
 * 
 * Most of the time you'll want to use [.setContent] to add the template from
 * [AwfulHtmlPage.getContainerHtml], which
 * loads the HTML, CSS and JS for displaying thread content, and then use [.setBodyHtml]
 * to add and display that content. [.setJavascriptHandler] needs to be
 * called, since the thread JS relies on it.
 * 
 * 
 * You can also run arbitrary JavaScript code with the [.runJavascript] method.
 */
class AwfulWebView : WebView {
    private var jsInterface: WebViewJsInterface? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }


    /**
     * Do the basic configuration for the app's WebViews.
     */
    private fun init() {
        val prefs = getInstance()
        val webSettings = settings
        webChromeClient = LoggingWebChromeClient(this)
        keepScreenOn = false // explicitly setting this since some people are complaining the screen stays on until they toggle it on and off

        setBackgroundColor(Color.TRANSPARENT)
        scrollBarStyle = SCROLLBARS_OUTSIDE_OVERLAY
        webSettings.javaScriptEnabled = true
        webSettings.defaultFontSize = prefs.postFontSizeSp
        webSettings.defaultFixedFontSize = prefs.postFixedFontSizeSp
        webSettings.domStorageEnabled = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        if (DEBUG) {
            setWebContentsDebuggingEnabled(true)
        }

        if (prefs.inlineWebm || prefs.inlineVines) {
            webSettings.mediaPlaybackRequiresUserGesture = false
        }

        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
    }


    override fun onPause() {
        pauseTimers()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        resumeTimers()
    }

    /**
     * Connects a handler to the JavaScript functions added to HTML for threads, posts, messages etc.
     * 
     * 
     * The base [WebViewJsInterface] handles some basic functions and preference injection.
     * If you need to handle other specific stuff (e.g. thread option menus), subclass it and add
     * the extra JavascriptInterface handler methods to that.
     * 
     * @param handler an object containing JavaScriptInterface methods to handle JS function calls
     */
    fun setJavascriptHandler(handler: WebViewJsInterface) {
        jsInterface = handler
        addJavascriptInterface(handler, HANDLER_NAME_IN_JAVASCRIPT)
    }


    /**
     * Set the content of this webview.
     * 
     * 
     * This loads the content string into the webview, treating it as UTF-8 HTML, and resolving any
     * relative URLs using the Something Awful base URL. If content is *null*, the webview
     * will be empty.
     * 
     * 
     * Use this method to insert basic content - if you need to do anything more complex, use
     * [loadData] and [loadDataWithBaseURL]
     * 
     * @param content the HTML content to display, or null to clear the webview
     */
    fun setContent(content: String?) {
        var content = content
        content = content ?: ""
        loadDataWithBaseURL(Constants.BASE_URL + "/", content, "text/html", "UTF-8", null)
    }


    /**
     * Helper function to execute some jabbascript in the webview.
     * 
     * @param javascript the code to run
     */
    fun runJavascript(javascript: String) {
        loadUrl("javascript:$javascript")
    }


    /**
     * Calls the javascript function that displays the current body HTML
     * 
     * 
     * This calls the #loadPageHtml function in *thread.js*, which displays the HTML passed to
     * [.setBodyHtml]. Calling this with unchanged HTML acts as a refresh, resetting
     * the displayed state of that page.
     * 
     */
    fun refreshPageContents() {
        runJavascript("loadPageHtml()")
    }


    /**
     * Set and display the current HTML for the container body.
     * 
     * 
     * Call this to update the WebView with new HTML content, calling [.refreshPageContents]
     * to display it. Does nothing if the passed HTML is unchanged from the currently added HTML,
     * or if [.setJavascriptHandler] hasn't been called yet.
     */
    fun setBodyHtml(html: String?) {
        if (jsInterface == null) {
            w("Attempted to set html with no JS interface handler added")
            return
        }
        if (html != null && html.hashCode() == jsInterface?.bodyHtml.hashCode()) {
            d("New HTML appears to match the current HTML, not updating")
            return
        }
        jsInterface?.bodyHtml = html ?: ""
        refreshPageContents()
    }

    companion object {
        const val TAG: String = "AwfulWebView"

        /**
         * thread.js uses this identifier to communicate with any handler we add
         */
        private const val HANDLER_NAME_IN_JAVASCRIPT = "listener"
    }
}
