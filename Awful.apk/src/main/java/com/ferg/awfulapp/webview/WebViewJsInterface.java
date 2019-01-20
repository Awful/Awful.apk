package com.ferg.awfulapp.webview;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.JavascriptInterface;

import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.Keys;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import timber.log.Timber;

/**
 * Created by baka kaba on 23/01/2017.
 * <p>
 * Basic JavaScript handling for WebViews.
 * <p>
 * Subclass this to add handler methods specific to a particular context, e.g. when the WebView will
 * be used in a {@link com.ferg.awfulapp.ThreadDisplayFragment} and needs to react to other UI clicks.
 */
// TODO: 12/02/2017 JS interface methods are called on a separate thread apparently - none of our implementations are thread-safe at all
public class WebViewJsInterface {

    private final Map<String, String> preferences = new ConcurrentHashMap<>();

    @NonNull
    private volatile String bodyHtml = "";

    public WebViewJsInterface() {
        updatePreferences();
    }

    /**
     * Updates the JavaScript-accessible preference store from the current values in AwfulPreferences.
     */
    public final void updatePreferences() {
        AwfulPreferences aPrefs = AwfulPreferences.getInstance();

        preferences.clear();
        preferences.put("username", aPrefs.username);
        preferences.put("showSpoilers", Boolean.toString(aPrefs.showAllSpoilers));
        preferences.put("highlightUserQuote", Boolean.toString(aPrefs.highlightUserQuote));
        preferences.put("highlightUsername", Boolean.toString(aPrefs.highlightUsername));
        preferences.put("inlineTweets", Boolean.toString(aPrefs.inlineTweets));
        preferences.put("inlineInstagram", Boolean.toString(aPrefs.getPreference(Keys.INLINE_INSTAGRAM, false)));
        preferences.put("inlineSoundcloud", Boolean.toString(aPrefs.getPreference(Keys.INLINE_SOUNDCLOUD, true)));
        preferences.put("inlineTwitch", Boolean.toString(aPrefs.getPreference(Keys.INLINE_TWITCH, false)));
        preferences.put("inlineWebm", Boolean.toString(aPrefs.inlineWebm));
        preferences.put("autostartWebm", Boolean.toString(aPrefs.autostartWebm));
        preferences.put("inlineVines", Boolean.toString(aPrefs.inlineVines));
        preferences.put("disableGifs", Boolean.toString(aPrefs.disableGifs));
        preferences.put("hideSignatures", Boolean.toString(aPrefs.hideSignatures));
        preferences.put("disablePullNext", Boolean.toString(aPrefs.disablePullNext));

        setCustomPreferences(preferences);
    }

    /**
     * Add any additional JavaScript-accessible preference values to the store.
     * <p>
     * Override this to insert and update any additional preferences your webview's JS needs.
     *
     * @param preferences the preference store to add to
     */
    protected void setCustomPreferences(Map<String, String> preferences) {
    }

    @NonNull
    @JavascriptInterface
    public final String getBodyHtml() {
        return bodyHtml;
    }

    final void setBodyHtml(@Nullable String html) {
        bodyHtml = (html == null) ? "" : html;
    }

    @JavascriptInterface
    public String getPreference(String preference) {
        return preferences.get(preference);
    }


    @JavascriptInterface
    public void debugMessage(final String msg) {
        Timber.d("Awful DEBUG: %s", msg);
    }

    // TODO: 28/01/2017 work out if any other common interface methods can go in here
}
