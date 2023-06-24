package com.ferg.awfulapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.ferg.awfulapp.network.CookieController;

import static com.ferg.awfulapp.constants.Constants.BASE_URL;
import static com.ferg.awfulapp.constants.Constants.COOKIE_NAME_CAPTCHA;
import static com.ferg.awfulapp.constants.Constants.FUNCTION_INDEX;


/**
 * Handles interactions with Cloudflare captchas. This is essentially a web view which displays the
 * catpcha to the user, and upon receiving a successful response persists the relevant session
 * cookie.
 *
 * Cloudflare responds with a `cf_clearance` cookie which authenticates the current combination of
 * User-Agent and IP when a captcha is solved. This is then persisted in the CookieController.
 */
public class CaptchaActivity extends AwfulActivity /* truly */ {
    /**
     * Informs other part of the application that captcha handling is in progress, which can be
     * used to suppress things like showing the login activity while the captcha handler is active.
     */
    private static boolean isActive = false;

    public static boolean isCaptchaBeingHandled() {
        return CaptchaActivity.isActive;
    }

    private WebView captchaView;

    private static final String TAG = "CaptchaActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "loading captcha web view");
        super.onCreate(savedInstanceState);
        CaptchaActivity.isActive = true;
        setContentView(R.layout.captcha_activity);

        captchaView = (WebView) findViewById(R.id.captchaView);
        captchaView.getSettings().setJavaScriptEnabled(true);

        final CaptchaActivity activity = this;
        captchaView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.equals(FUNCTION_INDEX)) {
                    // The Android web view provides absolutely no reasonable way to check
                    // whether we received a successful response. A workaround is checking
                    // whether markup that should be present on the index is there.
                    view.evaluateJavascript(
                            "(function() { return (document.body && document.body.id == 'something_awful'); })();",
                            s -> {
                                if (s.equals("true")) {
                                    Log.d(TAG, "captcha finished successfully");
                                    final String allCookies = CookieManager.getInstance().getCookie(BASE_URL);
                                    final String captchaCookie = parseCaptchaCookie(allCookies);

                                    if (captchaCookie != null) {
                                        CookieController.setCaptchaCookie(captchaCookie);
                                    } else {
                                        Log.w(TAG, "captcha finished, but captcha cookie not set");
                                    }

                                    activity.finish();
                                }
                            });
                }
            }
        });

        captchaView.loadUrl(FUNCTION_INDEX);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        captchaView.loadUrl(FUNCTION_INDEX);
    }

    private static String parseCaptchaCookie(String allCookies) {
        for (String cookie : allCookies.split("; ")) {
            if (cookie.startsWith(COOKIE_NAME_CAPTCHA)) {
                return cookie.substring(COOKIE_NAME_CAPTCHA.length() + 1 /* for the '=' */);
            }
        }

        return null;
    }

    /**
     * Helper method to be called by activities that receive an error response. If the error was due
     * to a captcha, the CaptchActivity will be invoked. Otherwise this does nothing.
     */
    public static void handleCaptchaChallenge(Activity sourceActivity, VolleyError error) {
        NetworkResponse response = error.networkResponse;
        if (response != null
                && response.statusCode == 403
                && response.headers != null
                && response.headers.containsKey("cf-mitigated")) {
            Log.i(TAG, "found captcha challenge, launching captcha activity");

            final Intent captchaIntent = new Intent(sourceActivity, CaptchaActivity.class);
            captchaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sourceActivity.startActivity(captchaIntent);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        CaptchaActivity.isActive = false;
    }
}
