package com.ferg.awfulapp.webview

import android.webkit.JavascriptInterface
import com.ferg.awfulapp.preferences.AwfulPreferences.Companion.getInstance
import com.ferg.awfulapp.preferences.BooleanPreference
import timber.log.Timber.Forest.d
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.Volatile

/**
 * Created by baka kaba on 23/01/2017.
 * 
 * 
 * Basic JavaScript handling for WebViews.
 * 
 * 
 * Subclass this to add handler methods specific to a particular context, e.g. when the WebView will
 * be used in a [com.ferg.awfulapp.ThreadDisplayFragment] and needs to react to other UI clicks.
 */
// TODO: 12/02/2017 JS interface methods are called on a separate thread apparently - none of our implementations are thread-safe at all
open class WebViewJsInterface {
    private val preferences: MutableMap<String, String> = ConcurrentHashMap<String, String>()

    @get:JavascriptInterface
    @Volatile
    var bodyHtml: String = ""

    init {
        updatePreferences()
    }

    /**
     * Updates the JavaScript-accessible preference store from the current values in AwfulPreferences.
     */
    fun updatePreferences() {
        val aPrefs = getInstance()

        with(preferences) {
            clear()
            this["username"] = aPrefs.username.orEmpty() // Safer alternative to !!
            this["showSpoilers"] = aPrefs.showAllSpoilers.toString()
            this["highlightUserQuote"] = aPrefs.highlightUserQuote.toString()
            this["highlightUsername"] = aPrefs.highlightUsername.toString()
            this["inlineTweets"] = aPrefs.inlineTweets.toString()
            this["inlineBluesky"] = aPrefs.inlineBluesky.toString()
            this["inlineInstagram"] = aPrefs.getPreference(BooleanPreference.INLINE_INSTAGRAM, false).toString()
            this["inlineSoundcloud"] = aPrefs.getPreference(BooleanPreference.INLINE_SOUNDCLOUD, true).toString()
            this["inlineTwitch"] = aPrefs.getPreference(BooleanPreference.INLINE_TWITCH, false).toString()
            this["inlineWebm"] = aPrefs.inlineWebm.toString()
            this["autostartWebm"] = aPrefs.autostartWebm.toString()
            this["inlineVines"] = aPrefs.inlineVines.toString()
            this["disableGifs"] = aPrefs.disableGifs.toString()
            this["hideSignatures"] = aPrefs.hideSignatures.toString()
            this["disablePullNext"] = aPrefs.disablePullNext.toString()
        }

        setCustomPreferences(preferences)
    }

    /**
     * Add any additional JavaScript-accessible preference values to the store.
     * 
     * 
     * Override this to insert and update any additional preferences your webview's JS needs.
     * 
     * @param preferences the preference store to add to
     */
    protected open fun setCustomPreferences(preferences: MutableMap<String, String>) {
    }

    @JavascriptInterface
    fun getPreference(preference: String?): String? {
        return preferences[preference]
    }


    @JavascriptInterface
    fun debugMessage(msg: String?) {
        d("Awful DEBUG: %s", msg)
    } // TODO: 28/01/2017 work out if any other common interface methods can go in here
}
