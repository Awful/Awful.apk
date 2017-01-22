package com.ferg.awfulapp.webview;

import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.util.AwfulUtils;

import static com.ferg.awfulapp.constants.Constants.DEBUG;

/**
 * Created by baka kaba on 21/01/2017.
 * <p>
 * Centralised place for the different webview configurations the app uses.
 * <p>
 * This code was all moved from their respective fragments and put here instead.
 * Call one of these methods to set up a WebView in the same way as before.
 */

public abstract class WebViewConfig {

    // TODO: 28/01/2017 Everything now uses the 'thread' config, seems to work fine - if there are no issues, delete the old config methods, maybe move all this into AwfulWebView

    /**
     * Configure a WebView with the original settings from {@link com.ferg.awfulapp.ThreadDisplayFragment}
     */
    public static void configureForThread(@NonNull WebView webView) {
        AwfulPreferences prefs = AwfulPreferences.getInstance();
        WebSettings webSettings = webView.getSettings();
        doBasicConfiguration(webSettings, prefs);

        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webSettings.setJavaScriptEnabled(true);

        // TODO: fix deprecated warnings
        // TODO: see if we can get the linter to recognise the AwfulUtils version checks as API guards

        if (prefs.inlineYoutube || prefs.inlineWebm || prefs.inlineVines) {//YOUTUBE SUPPORT BLOWS
            webSettings.setPluginState(WebSettings.PluginState.ON_DEMAND);
        }
        if (AwfulUtils.isAtLeast(Build.VERSION_CODES.JELLY_BEAN_MR1) && (prefs.inlineWebm || prefs.inlineVines)) {
            webSettings.setMediaPlaybackRequiresUserGesture(false);
        }
        if (prefs.inlineTweets && AwfulUtils.isJellybean()) {
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            webSettings.setAllowFileAccessFromFileURLs(true);
            webSettings.setAllowFileAccess(true);
            webSettings.setAllowContentAccess(true);
        }
    }

    /**
     * Perform config common to all the config types.
     *
     * @param settings the WebSettings from the WebView being configured
     */
    private static void doBasicConfiguration(WebSettings settings, AwfulPreferences prefs) {
        settings.setRenderPriority(WebSettings.RenderPriority.LOW);
        settings.setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
        settings.setDefaultFontSize(prefs.postFontSizeDip);
        settings.setDefaultFixedFontSize(prefs.postFixedFontSizeDip);
        if (AwfulUtils.isLollipop()) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        if (DEBUG && AwfulUtils.isKitKat()) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }


    /**
     * Configure a WebView with the original settings from {@link com.ferg.awfulapp.MessageFragment}
     */
    @Deprecated
    public static void configureForPm(@NonNull WebView webView) {
        // TODO: 21/01/2017 this is missing a few things from the thread configs, try using those and see if it works ok
        AwfulPreferences prefs = AwfulPreferences.getInstance();
        WebSettings settings = webView.getSettings();
        doBasicConfiguration(settings, prefs);
    }

    /**
     * Configure a WebView with the original settings from {@link com.ferg.awfulapp.PreviewFragment}
     */
    @Deprecated
    public static void configureForPostPreview(@NonNull WebView webView) {
        AwfulPreferences prefs = AwfulPreferences.getInstance();
        WebSettings settings = webView.getSettings();
        doBasicConfiguration(settings, prefs);

        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        settings.setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());

        if (prefs.inlineYoutube || prefs.inlineWebm || prefs.inlineVines) {//YOUTUBE SUPPORT BLOWS
            settings.setPluginState(WebSettings.PluginState.ON_DEMAND);
        }
        // TODO: 21/01/2017 inlineTweets check to match thread config?
        if (AwfulUtils.isJellybean()) {
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
        }
    }

}
