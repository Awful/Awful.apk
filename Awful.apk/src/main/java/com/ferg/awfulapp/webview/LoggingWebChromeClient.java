package com.ferg.awfulapp.webview;

import android.os.Message;
import android.support.annotation.CallSuper;
import android.util.Log;
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

    @CallSuper
    public boolean onConsoleMessage(ConsoleMessage message) {
        if (DEBUG)
            Log.d("Web Console", message.message() + " -- From line " + message.lineNumber() + " of " + message.sourceId());
        return true;
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
}
