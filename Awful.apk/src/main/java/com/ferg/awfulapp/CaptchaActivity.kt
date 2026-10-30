package com.ferg.awfulapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.android.volley.VolleyError
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.CookieController

/**
 * Handles interactions with Cloudflare captchas. This is essentially a web view which displays the
 * catpcha to the user, and upon receiving a successful response persists the relevant session
 * cookie.
 * 
 * Cloudflare responds with a `cf_clearance` cookie which authenticates the current combination of
 * User-Agent and IP when a captcha is solved. This is then persisted in the CookieController.
 */
class CaptchaActivity : AwfulActivity() /* truly */ {
    private var captchaView: WebView? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "loading captcha web view")
        super.onCreate(savedInstanceState)
        captchaIsBeingHandled = true
        setContentView(R.layout.captcha_activity)

        captchaView = findViewById<View?>(R.id.captchaView) as WebView
        captchaView?.let {
            it.settings.javaScriptEnabled = true
        }

        val activity = this
        captchaView?.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (url == Constants.FUNCTION_INDEX) {
                    // The Android web view provides absolutely no reasonable way to check
                    // whether we received a successful response. A workaround is checking
                    // whether markup that should be present on the index is there.
                    view.evaluateJavascript(
                        "(function() { return (document.body && document.body.id == 'something_awful'); })();"
                    ) { s: String? ->
                        if (s == "true") {
                            Log.d(TAG, "captcha finished successfully")
                            val allCookies =
                                CookieManager.getInstance().getCookie(Constants.BASE_URL)
                            val captchaCookie: String? = parseCaptchaCookie(allCookies)

                            if (captchaCookie != null) {
                                CookieController.setCaptchaCookie(captchaCookie)
                            } else {
                                Log.w(TAG, "captcha finished, but captcha cookie not set")
                            }

                            activity.finish()
                        }
                    }
                }
            }
        }

        captchaView?.loadUrl(Constants.FUNCTION_INDEX)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        captchaView?.loadUrl(Constants.FUNCTION_INDEX)
    }

    public override fun onStop() {
        super.onStop()
        captchaIsBeingHandled = false
    }

    companion object {
        /**
         * Informs other part of the application that captcha handling is in progress, which can be
         * used to suppress things like showing the login activity while the captcha handler is active.
         */
        var captchaIsBeingHandled: Boolean = false
            private set

        @JvmStatic
        fun isCaptchaBeingHandled(): Boolean {
            return captchaIsBeingHandled
        }

        private const val TAG = "CaptchaActivity"

        private fun parseCaptchaCookie(allCookies: String): String? {
            for (cookie in allCookies.split(Regex("; "))) {
                if (cookie.startsWith(Constants.COOKIE_NAME_CAPTCHA)) {
                    return cookie.substring(Constants.COOKIE_NAME_CAPTCHA.length + 1 /* for the '=' */)
                }
            }

            return null
        }

        /**
         * Helper method to be called by activities that receive an error response. If the error was due
         * to a captcha, the CaptchaActivity will be invoked. Otherwise, this does nothing.
         */
        @JvmStatic
        fun handleCaptchaChallenge(sourceActivity: Activity, error: VolleyError) {
            val response = error.networkResponse
            response?.let {
                if(it.statusCode == 403 && it.headers != null && it.headers!!.containsKey("cf-mitigated")) {
                    Log.i(TAG, "found captcha challenge, launching captcha activity")

                    val captchaIntent = Intent(sourceActivity, CaptchaActivity::class.java)
                    captchaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    sourceActivity.startActivity(captchaIntent)
                }
            }
        }
    }
}
