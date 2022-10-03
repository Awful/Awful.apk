package com.ferg.awfulapp.webview;

import android.os.Message;
import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import static com.ferg.awfulapp.constants.Constants.DEBUG;

/**
 * Created by baka kaba on 22/01/2017.
 * <p>
 * Just a basic WebChromeClient with debug logging.
 * You can subclass this and override any methods to add specific functionality.
 */

public class LoggingWebChromeClient extends WebChromeClient {

    private static final String TAG = "WebChromeClient";
    @Nullable
    private AlertDialog fullscreenContentDialog = null;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private WebView webView;

    @CallSuper
    public boolean onConsoleMessage(ConsoleMessage message) {
        if (DEBUG)
            Log.d("Web Console", message.message() + " -- From line " + message.lineNumber() + " of " + message.sourceId());
        return true;
    }

    public LoggingWebChromeClient(WebView webView) {
        super();
        this.webView = webView;
    }

    @CallSuper
    @Override
    public void onCloseWindow(WebView window) {
        super.onCloseWindow(window);
        if (DEBUG) Log.d(TAG, "onCloseWindow");
    }

    @CallSuper
    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
        if (DEBUG)
            Log.d(TAG, "onCreateWindow" + (isDialog ? " isDialog" : "") + (isUserGesture ? " isUserGesture" : ""));
        return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
    }

    @CallSuper
    @Override
    public boolean onJsTimeout() {
        if (DEBUG) Log.d(TAG, "onJsTimeout");
        return super.onJsTimeout();
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        // if a view already exists then immediately terminate the new one
        if (fullscreenContentDialog != null) {
            callback.onCustomViewHidden();
            return;
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
                null);

        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        fullscreenContentDialog = new AlertDialog.Builder(webView.getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(view).show();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        customViewCallback = callback;
    }

    @Override
    public void onHideCustomView() {
        super.onHideCustomView();    //To change body of overridden methods use File | Settings | File Templates.
        if (fullscreenContentDialog == null)
            return;

        // see above: scroll position may be off by the video's height-ish
        // if embedded video is scrolled far enough down to not have
        // the <a class="video-link"> visible anymore.
        webView.evaluateJavascript(
        "(function(){" +
                "var fullscreenElement = document.fullscreenElement;" +
                "setTimeout(function(){" +
                    // assume we overshot the scroll position because of the above :cry:
                    "if (fullscreenElement.getBoundingClientRect().bottom < 0) {" +
                        "window.scrollBy({top: -fullscreenElement.clientHeight});" +
                    "}" +
                "}, 250);" +
            "})();",
        null);

        // Hide the custom view.
        fullscreenContentDialog.dismiss();
        fullscreenContentDialog = null;

        // Remove the custom view from its container.
        customViewCallback.onCustomViewHidden();
    }
}
