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

        // Hide the custom view.
        fullscreenContentDialog.dismiss();
        fullscreenContentDialog = null;

        // Remove the custom view from its container.
        customViewCallback.onCustomViewHidden();
    }
}
