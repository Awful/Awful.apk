package com.ferg.awfulapp.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import timber.log.Timber;

import static android.content.SharedPreferences.Editor;
import static com.ferg.awfulapp.constants.Constants.COOKIE_DOMAIN_CAPTCHA;
import static com.ferg.awfulapp.constants.Constants.COOKIE_NAME_CAPTCHA;

/**
 * Handles all interactions with cookies
 */
public class CookieController {
    private static CookieManager cookieManager;
    private static String cookie = null;
    private static final String COOKIE_HEADER = "Cookie";
    private static final URI uri = URI.create(Constants.BASE_URL);

    private CookieController() {
    }

    static {
        cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
    }

    /**
     * Add the current session cookie's data to a header map.
     * <p>
     * The data is provided as a single header - see {@link #restoreLoginCookies(Context)} for the format.
     */
    public static void setCookieHeaders(@NonNull Map<String, String> headers) {
        if (cookie == null) {
            Timber.w("Cookie was empty for some reason, trying to restore cookie");
            restoreLoginCookies(AwfulPreferences.getInstance().getContext());
        }

        String finalCookies = getCaptchaCookie().orElse("")
                + Optional.ofNullable(cookie).orElse("");

        if (!finalCookies.isEmpty()) {
            headers.put(
                    COOKIE_HEADER,
                    finalCookies
            );
        }
    }

    /**
     * Attempts to initialize the HttpClient with cookie values
     * stored in the given Context's SharedPreferences through the
     * {@link #saveLoginCookies(Context)} method.
     *
     * @return Whether stored cookie values were found & initialized
     */
    public static synchronized boolean restoreLoginCookies(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(Constants.COOKIE_PREFERENCE, Context.MODE_PRIVATE);
        String useridCookieString = prefs.getString(Constants.COOKIE_PREF_USERID, null);
        String passwordCookieString = prefs.getString(Constants.COOKIE_PREF_PASSWORD, null);
        String sessionidCookieString = prefs.getString(Constants.COOKIE_PREF_SESSIONID, null);
        String sessionhashCookieString = prefs.getString(Constants.COOKIE_PREF_SESSIONHASH, null);
        if (useridCookieString == null || passwordCookieString == null || sessionidCookieString == null || sessionhashCookieString == null) {
            if (Constants.DEBUG) {
                Timber.e("Unable to retrieve cookies! Reasons:\n" +
                        (useridCookieString == null ? "USER_ID is NULL\n" : "") +
                        (passwordCookieString == null ? "pass is NULL\n" : "") +
                        (sessionidCookieString == null ? "sessid is NULL\n" : "") +
                        (sessionhashCookieString == null ? "sesshash is NULL\n" : ""));
            }

            cookie = "";
            return false;
        }

        if (!useridCookieString.startsWith(Constants.COOKIE_PREF_USERID) || !passwordCookieString.startsWith(Constants.COOKIE_PREF_PASSWORD)) {
            long expiry = prefs.getLong(Constants.COOKIE_PREF_EXPIRY_DATE, -1);
            long maxAge = expiry - System.currentTimeMillis();
            String expires = calculateExpires(maxAge);
            useridCookieString = String.format("%s=%s; Domain=%s; Path=%s;Max-Age=%s; Expires=%s;",Constants.COOKIE_PREF_USERID, useridCookieString, Constants.COOKIE_DOMAIN, Constants.COOKIE_PATH, maxAge, expires);
            passwordCookieString = String.format("%s=%s; Domain=%s; Path=%s;Max-Age=%s; Expires=%s;",Constants.COOKIE_PREF_PASSWORD, passwordCookieString, Constants.COOKIE_DOMAIN, Constants.COOKIE_PATH, maxAge, expires);
        }
        if (!sessionidCookieString.startsWith(Constants.COOKIE_PREF_SESSIONID) || !sessionhashCookieString.startsWith(Constants.COOKIE_PREF_SESSIONHASH)) {
            sessionidCookieString = String.format("%s=%s; Domain=%s; Path=%s;",Constants.COOKIE_PREF_SESSIONID, sessionidCookieString, Constants.COOKIE_DOMAIN, Constants.COOKIE_PATH);
            sessionhashCookieString = String.format("%s=%s; Domain=%s; Path=%s;",Constants.COOKIE_PREF_SESSIONHASH, sessionhashCookieString, Constants.COOKIE_DOMAIN, Constants.COOKIE_PATH);
        }

        HttpCookie useridCookie = HttpCookie.parse(useridCookieString).get(0);
        HttpCookie passwordCookie = HttpCookie.parse(passwordCookieString).get(0);
        HttpCookie sessionidCookie = HttpCookie.parse(sessionidCookieString).get(0);
        HttpCookie sessionhashCookie = HttpCookie.parse(sessionhashCookieString).get(0);
        // verify the cookie is valid - if not, we need to clear the cookie and return a failure
        if (useridCookie.getValue() == null || passwordCookie.getValue() == null || useridCookie.hasExpired() || passwordCookie.hasExpired()) {
            if (Constants.DEBUG) {
                Timber.w("Unable to restore cookies! Reasons:\n" +
                        (useridCookie.getValue() == null ? "USER_ID is NULL\n" : "") +
                        (passwordCookie.getValue() == null ? "PASSWORD is NULL\n" : "") +
                        (useridCookie.hasExpired() ? "userid cookie has expired, max age = " + useridCookie.getMaxAge() : "") +
                        (passwordCookie.hasExpired() ? "password cookie has expired, max age = " + passwordCookie.getMaxAge() : ""));
            }

            cookie = "";
            return false;
        }

        cookie = String.format("%s=%s;%s=%s;%s=%s;%s=%s;",
                Constants.COOKIE_NAME_USERID, useridCookie.getValue(),
                Constants.COOKIE_NAME_PASSWORD, passwordCookie.getValue(),
                Constants.COOKIE_NAME_SESSIONID, sessionidCookie.getValue(),
                Constants.COOKIE_NAME_SESSIONHASH, sessionhashCookie.getValue());


        HttpCookie[] allCookies = {
                useridCookie,
                passwordCookie,
                sessionidCookie,
                sessionhashCookie
        };


        for (HttpCookie tempCookie : allCookies) {
            if(!tempCookie.hasExpired()){
                cookieManager.getCookieStore().add(uri, tempCookie);
            }
        }

        if (Constants.DEBUG) {
            Timber.i("Cookies restored from prefs");
            Timber.i("Cookie dump: %s", TextUtils.join("\n", cookieManager.getCookieStore().getCookies()));
        }

        return true;
    }

    /**
     * Clears cookies from both the current client's store and
     * the persistent SharedPreferences. Effectively, logs out.
     */
    public static synchronized void clearLoginCookies(@NonNull Context context) {
        // First clear out the persistent preferences...
        context.getSharedPreferences(
                        Constants.COOKIE_PREFERENCE,
                        Context.MODE_PRIVATE)
                .edit().clear().apply();

        // Then the memory store
        cookieManager.getCookieStore().removeAll();
    }

    /**
     * Saves SomethingAwful login cookies that the client has received
     * during this session to the given Context's SharedPreferences. They
     * can be later restored with {@link #restoreLoginCookies(Context)}.
     *
     * @return Whether any login cookies were successfully saved
     */
    public static synchronized boolean saveLoginCookies(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(
                Constants.COOKIE_PREFERENCE,
                Context.MODE_PRIVATE);

        String useridValue = null;
        String passwordValue = null;
        String sessionId = null;
        String sessionHash = null;
        Date expires = null;
        Integer version = null;

        Timber.d("Saving cookies - here's what we got:");
        logCookies();

        for (HttpCookie cookie : cookieManager.getCookieStore().get(uri)) {
            switch (cookie.getName()) {
                case Constants.COOKIE_NAME_USERID:
                    useridValue = getCookieString(cookie, true);
                    break;
                case Constants.COOKIE_NAME_PASSWORD:
                    passwordValue = getCookieString(cookie, true);
                    break;
                case Constants.COOKIE_NAME_SESSIONID:
                    sessionId = getCookieString(cookie, true);
                    break;
                case Constants.COOKIE_NAME_SESSIONHASH:
                    sessionHash = getCookieString(cookie, true);
                    break;
                default:
                    // unrecognised cookie, ignore it! some cloudflare ones have a real short expiry
                    continue;
            }
        }

        if (useridValue == null || passwordValue == null) {
            return false;
        }

        Editor edit = prefs.edit();
        edit.putString(Constants.COOKIE_PREF_USERID, useridValue);
        edit.putString(Constants.COOKIE_PREF_PASSWORD, passwordValue);
        if (sessionId != null && sessionId.length() > 0) {
            edit.putString(Constants.COOKIE_PREF_SESSIONID, sessionId);
        }
        if (sessionHash != null && sessionHash.length() > 0) {
            edit.putString(Constants.COOKIE_PREF_SESSIONHASH, sessionHash);
        }

        edit.apply();
        return true;
    }

    public static String calculateExpires(long maxAge) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, ((int) maxAge));
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return ";expires=" + dateFormat.format(c.getTime());
    }

    public static synchronized String getCookieString(HttpCookie cookie) {
        return getCookieString(cookie, false);
    }

    public static synchronized String getCookieString(HttpCookie cookie, boolean calculateExpires) {
        String expires = "";
        if (calculateExpires) {
            expires = calculateExpires(cookie.getMaxAge());

        }
        return String.format("%s=%s%s;max-age=%s;domain=%s;path=%s", cookie.getName(), cookie.getValue(), expires, cookie.getMaxAge(), cookie.getDomain(), cookie.getPath());
    }

    public static synchronized String getCookieString(String type) {
        for (HttpCookie cookie : cookieManager.getCookieStore().get(uri)) {
            if (cookie.getName().contains(type))
                return getCookieString(cookie);
        }
        Timber.w("getCookieString couldn't find type: %s", type);
        return "";
    }

    /**
     * Set the Cloudflare captcha cookie after it was retrieved in a CaptchaActivity web view.
     */
    public static void setCaptchaCookie(String cookie) {
        final HttpCookie httpCookie = new HttpCookie(COOKIE_NAME_CAPTCHA, cookie);
        httpCookie.setDomain(COOKIE_DOMAIN_CAPTCHA); // applies to subdomains as well
        cookieManager.getCookieStore().add(uri, httpCookie);
    }

    /**
     * If the Cloudflare captcha cookie is set, return it so that it can be appended to the provided
     * cookies.
     */
    public static Optional<String> getCaptchaCookie() {
        // It seems like there is no direct accessor for a specific cookie, so this little dance has
        // to be done all the time to find the right one.
        for (HttpCookie c : cookieManager.getCookieStore().get(uri)) {
            if (c.getName().equals(COOKIE_NAME_CAPTCHA)) {
                return Optional.of(COOKIE_NAME_CAPTCHA + "=" + c.getValue() + ";");
            }
        }

        return Optional.empty();
    }

    public static void logCookies() {
        if (Constants.DEBUG) {
            Timber.i("---BEGIN COOKIE DUMP---");
            List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
            for (HttpCookie c : cookies) {
                Timber.d("Name: %s\nSecure only: %b, expired: %b, max age: %d\nContent: %s\n",
                        c.getName(), c.getSecure(), c.hasExpired(), c.getMaxAge(), c.toString());
            }
            Timber.i("---END COOKIE DUMP---");
        }
    }
}
