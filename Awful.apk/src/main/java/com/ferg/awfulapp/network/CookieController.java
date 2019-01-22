package com.ferg.awfulapp.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

import static android.content.SharedPreferences.Editor;

public class CookieController {
    private static CookieManager cookieManager;
    private static String cookie = null;
    private static final String COOKIE_HEADER = "Cookie";

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
        if (!cookie.isEmpty()) {
            headers.put(COOKIE_HEADER, cookie);
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
        SharedPreferences prefs = ctx.getSharedPreferences(
                Constants.COOKIE_PREFERENCE,
                Context.MODE_PRIVATE);
        String useridCookieValue = prefs.getString(Constants.COOKIE_PREF_USERID, null);
        String passwordCookieValue = prefs.getString(Constants.COOKIE_PREF_PASSWORD, null);
        String sessionidCookieValue = prefs.getString(Constants.COOKIE_PREF_SESSIONID, null);
        String sessionhashCookieValue = prefs.getString(Constants.COOKIE_PREF_SESSIONHASH, null);
        long expiry = prefs.getLong(Constants.COOKIE_PREF_EXPIRY_DATE, -1);
        int cookieVersion = prefs.getInt(Constants.COOKIE_PREF_VERSION, 0);

        if (useridCookieValue == null || passwordCookieValue == null || expiry == -1) {
            if (Constants.DEBUG) {
                Timber.w("Unable to restore cookies! Reasons:\n" +
                        (useridCookieValue == null ? "USER_ID is NULL\n" : "") +
                        (passwordCookieValue == null ? "PASSWORD is NULL\n" : "") +
                        (expiry == -1 ? "EXPIRY is -1" : ""));
            }

            cookie = "";
            return false;
        }

        cookie = String.format("%s=%s;%s=%s;%s=%s;%s=%s;",
                Constants.COOKIE_NAME_USERID, useridCookieValue,
                Constants.COOKIE_NAME_PASSWORD, passwordCookieValue,
                Constants.COOKIE_NAME_SESSIONID, sessionidCookieValue,
                Constants.COOKIE_NAME_SESSIONHASH, sessionhashCookieValue);

        HttpCookie useridCookie =
                new HttpCookie(Constants.COOKIE_NAME_USERID, useridCookieValue);
        HttpCookie passwordCookie =
                new HttpCookie(Constants.COOKIE_NAME_PASSWORD, passwordCookieValue);
        HttpCookie sessionidCookie =
                new HttpCookie(Constants.COOKIE_NAME_SESSIONID, sessionidCookieValue);
        HttpCookie sessionhashCookie =
                new HttpCookie(Constants.COOKIE_NAME_SESSIONHASH, sessionhashCookieValue);

        Date expiryDate = new Date(expiry);
        Date now = new Date();
        HttpCookie[] allCookies = {useridCookie, passwordCookie, sessionidCookie, sessionhashCookie};

        Timber.e("now.compareTo(expiryDate):%s", (expiryDate.getTime() - now.getTime()));
        for (HttpCookie tempCookie : allCookies) {
            tempCookie.setVersion(cookieVersion);
            tempCookie.setDomain(Constants.COOKIE_DOMAIN);
            tempCookie.setMaxAge(expiryDate.getTime() - now.getTime());
            tempCookie.setPath(Constants.COOKIE_PATH);
        }
        cookieManager.getCookieStore().add(URI.create(Constants.COOKIE_DOMAIN), useridCookie);
        cookieManager.getCookieStore().add(URI.create(Constants.COOKIE_DOMAIN), passwordCookie);
        cookieManager.getCookieStore().add(URI.create(Constants.COOKIE_DOMAIN), sessionhashCookie);

        if (Constants.DEBUG) {
            Timber.w("Cookies restored from prefs");
            Timber.w("Cookie dump: %s", TextUtils.join("\n", cookieManager.getCookieStore().getCookies()));
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

        for (HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
            if (!cookie.getDomain().contains(Constants.COOKIE_DOMAIN))
                continue;

            switch (cookie.getName()) {
                case Constants.COOKIE_NAME_USERID:
                    useridValue = cookie.getValue();
                    break;
                case Constants.COOKIE_NAME_PASSWORD:
                    passwordValue = cookie.getValue();
                    break;
                case Constants.COOKIE_NAME_SESSIONID:
                    sessionId = cookie.getValue();
                    break;
                case Constants.COOKIE_NAME_SESSIONHASH:
                    sessionHash = cookie.getValue();
                    break;
            }

            // keep the soonest valid expiry in case they don't match
            Calendar c = Calendar.getInstance();
            c.add(Calendar.SECOND, ((int) cookie.getMaxAge()));
            Date cookieExpiryDate = c.getTime();
            if (expires == null || (cookieExpiryDate != null && cookieExpiryDate.before(expires))) {
                expires = cookieExpiryDate;
            }
            // fall back to the lowest cookie spec version
            if (version == null || cookie.getVersion() < version) {
                version = cookie.getVersion();
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
        if (expires != null) {
            edit.putLong(Constants.COOKIE_PREF_EXPIRY_DATE, expires.getTime());
        }
        edit.putInt(Constants.COOKIE_PREF_VERSION, version);

        edit.apply();
        return true;
    }

    public static synchronized String getCookieString(String type) {
        for (HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
            if (!cookie.getDomain().contains(Constants.COOKIE_DOMAIN))
                continue;

            if (cookie.getName().contains(type))
                return String.format("%s=%s; domain=%s", type, cookie.getValue(), cookie.getDomain());
        }
        Timber.w("getCookieString couldn't find type: %s", type);
        return "";
    }

    public static void logCookies() {
        if (Constants.DEBUG) {
            Timber.i("---BEGIN COOKIE DUMP---");
            List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
            for (HttpCookie c : cookies) {
                Timber.i(c.toString());
            }
            Timber.i("---END COOKIE DUMP---");
        }
    }
}
