package com.ferg.awfulapp.webview

import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.annotation.CallSuper
import androidx.appcompat.app.AlertDialog
import com.ferg.awfulapp.constants.Constants.DEBUG

/**
 * Created by baka kaba on 22/01/2017.
 * 
 * 
 * Just a basic WebChromeClient with debug logging.
 * You can subclass this and override any methods to add specific functionality.
 */
open class LoggingWebChromeClient(private val webView: WebView) : WebChromeClient() {
    private var fullscreenContentDialog: AlertDialog? = null
    private var customViewCallback: CustomViewCallback? = null

    @CallSuper
    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
        if (DEBUG) Log.d(
            "Web Console",
            message.message() + " -- From line " + message.lineNumber() + " of " + message.sourceId()
        )
        return true
    }

    @CallSuper
    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)
        if (DEBUG) Log.d(TAG, "onCloseWindow")
    }

    @CallSuper
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        if (DEBUG) Log.d(
            TAG,
            "onCreateWindow" + (if (isDialog) " isDialog" else "") + (if (isUserGesture) " isUserGesture" else "")
        )
        return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
    }

    @CallSuper
    override fun onJsTimeout(): Boolean {
        if (DEBUG) Log.d(TAG, "onJsTimeout")
        return super.onJsTimeout()
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        // if a view already exists then immediately terminate the new one
        if (fullscreenContentDialog != null) {
            callback.onCustomViewHidden()
            return
        }

        // we lose the scroll position when viewing things fullscreen.
        //
        // it appears to work if we store the scroll position, then
        // immediately catch the scroll events generated when the scroll
        // position changes, then scroll right back.
        //
        // for embedded videos, doesn't work when video is scrolled far
        // enough down to hide <a class="video-link">. there's a hacky
        // correction below.
        // TODO: this doesn't perfectly restore when scrolled down to near the bottom of a page.
        webView.evaluateJavascript(
            "(function(){" +
                    "var scrollPos = window.scrollY;" +
                    "var restoreTimeout = undefined;" +
                    "window.addEventListener('scroll', debounceRestoreScroll);" +
                    "function debounceRestoreScroll() {" +
                    "clearTimeout(restoreTimeout);" +
                    "restoreTimeout = setTimeout(restore, 100);" +
                    "function restore() {" +
                    "window.scrollTo({top: scrollPos});" +
                    "window.removeEventListener('scroll', debounceRestoreScroll);" +
                    "}" +
                    "}" +
                    "})();",
            null
        )

        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        fullscreenContentDialog =
            AlertDialog.Builder(webView.context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(view).show()
        view.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        customViewCallback = callback
    }

    override fun onHideCustomView() {
        super.onHideCustomView() //To change body of overridden methods use File | Settings | File Templates.
        if (fullscreenContentDialog == null) return

        // see above: scroll position may be off by the video's height-ish
        // if embedded video is scrolled far enough down to not have
        // the <a class="video-link"> visible anymore.
        webView.evaluateJavascript(
            "(function(){" +
                    "var fullscreenElement = document.fullscreenElement;" +
                    "setTimeout(function(){" +  // assume we overshot the scroll position because of the above :cry:
                    "if (fullscreenElement.getBoundingClientRect().bottom < 0) {" +
                    "window.scrollBy({top: -fullscreenElement.clientHeight});" +
                    "}" +
                    "}, 250);" +
                    "})();",
            null
        )

        // Hide the custom view.
        fullscreenContentDialog?.dismiss()
        fullscreenContentDialog = null

        // Remove the custom view from its container.
        customViewCallback?.onCustomViewHidden()
    }

    companion object {
        private const val TAG = "WebChromeClient"
    }
}
