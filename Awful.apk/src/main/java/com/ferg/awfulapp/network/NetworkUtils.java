/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the software nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.ferg.awfulapp.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.http.HttpResponseCache;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.util.LRUImageCache;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class NetworkUtils {
    private static final String CHARSET = "windows-1252";

    private static final Pattern unencodeCharactersPattern = Pattern.compile("&#(\\d+);");
    private static final Pattern encodeCharactersPattern = Pattern.compile("([^\\x00-\\x7F])");

    private static RequestQueue mNetworkQueue;
    private static LRUImageCache mImageCache;
    private static ImageLoader mImageLoader;

    private static CookieManager ckmngr;

    private static String cookie = null;
    private static final String COOKIE_HEADER = "Cookie";

    static {
        ckmngr = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(ckmngr);
    }

    /**
     * Initialise request handling and caching - call this early!
     *
     * @param context A context used to create a cache dir
     */
    public static void init(Context context) {
        // update the security provider first, to ensure we fix SSL errors before setting anything else up
        SecurityProvider.update(context);
        mNetworkQueue = Volley.newRequestQueue(context);
        // TODO: find out if this is even being used anywhere
        mImageCache = new LRUImageCache();
        mImageLoader = new ImageLoader(mNetworkQueue, mImageCache);

        try {
            HttpResponseCache.install(new File(context.getCacheDir(), "httpcache"), 5242880);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ImageLoader getImageLoader() {
        return mImageLoader;
    }

    public static void clearImageCache() {
        if (mImageCache != null) {
            mImageCache.clear();
        }
    }

    public static void queueRequest(Request request) {
        if (mNetworkQueue != null) {
            mNetworkQueue.add(request);
        } else {
            Timber.w("Can't queue request - NetworkQueue is null, has NetworkUtils been initialised?");
        }
    }

    public static void cancelRequests(Object tag) {
        if (mNetworkQueue != null) {
            mNetworkQueue.cancelAll(tag);
        } else {
            Timber.w("Can't cancel requests - NetworkQueue is null, has NetworkUtils been initialised?");
        }
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
        ckmngr.getCookieStore().add(URI.create(Constants.COOKIE_DOMAIN), useridCookie);
        ckmngr.getCookieStore().add(URI.create(Constants.COOKIE_DOMAIN), passwordCookie);
        ckmngr.getCookieStore().add(URI.create(Constants.COOKIE_DOMAIN), sessionhashCookie);

        if (Constants.DEBUG) {
            Timber.w("Cookies restored from prefs");
            Timber.w("Cookie dump: %s", TextUtils.join("\n", ckmngr.getCookieStore().getCookies()));
        }

        return true;
    }

    /**
     * Clears cookies from both the current client's store and
     * the persistent SharedPreferences. Effectively, logs out.
     */
    public static synchronized void clearLoginCookies(Context ctx) {
        // First clear out the persistent preferences...
        if (null == ctx) {
            ctx = AwfulPreferences.getInstance().getContext();
        }
        SharedPreferences prefs = ctx.getSharedPreferences(
                Constants.COOKIE_PREFERENCE,
                Context.MODE_PRIVATE);

        prefs.edit().clear().apply();

        // Then the memory store
        ckmngr.getCookieStore().removeAll();
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

        for (HttpCookie cookie : ckmngr.getCookieStore().getCookies()) {
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
        List<HttpCookie> cookies = ckmngr.getCookieStore().getCookies();
        for (HttpCookie cookie : cookies) {
            if (cookie.getDomain().contains(Constants.COOKIE_DOMAIN)) {
                if (cookie.getName().contains(type)) {
                    return String.format("%s=%s; domain=%s", type, cookie.getValue(), cookie.getDomain());
                }
            }
        }
        Timber.w("getCookieString couldn't find type: %s", type);
        return "";
    }

    public static Document get(String aUrl) throws Exception {
        return get(new URI(aUrl));
    }

    public static Document get(URI location) throws Exception {
        Timber.i("Fetching %s", location);

        HttpURLConnection urlConnection = (HttpURLConnection) location.toURL().openConnection();

        if (urlConnection == null) {
            Timber.e("Couldn't open connection");
            return null;
        }

        Document response;

        try {
            InputStream inputStream = urlConnection.getInputStream();
            response = Jsoup.parse(inputStream, CHARSET, Constants.BASE_URL);
        } finally {
            urlConnection.disconnect();
        }

        Timber.i("Fetched %s", location);
        return response;
    }

    public static String getRedirect(String aUrl, HashMap<String, String> aParams) throws Exception {
        URI location = new URI(aUrl + getQueryStringParameters(aParams));

        String redirectLocation;
        HttpURLConnection urlConnection = (HttpURLConnection) location.toURL().openConnection();
        try {
            redirectLocation = urlConnection.getHeaderField("Location");
            if (redirectLocation == null) {
                // HttpURLConnection redirects internally, so get the end result instead
                redirectLocation = urlConnection.getURL().toString();
            }
        } finally {
            urlConnection.disconnect();
        }
        if (redirectLocation != null) {
            return redirectLocation;
        }
        return null;
    }

    public static String getQueryStringParameters(HashMap<String, String> aParams) {
        if (aParams == null)
            return "";

        StringBuilder result = new StringBuilder("?");

        try {
            String separator = "";

            for (Map.Entry<String, String> entry : aParams.entrySet()) {
                result.append(separator)
                        .append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8"));

                separator = "&";
            }
        } catch (UnsupportedEncodingException e) {
            Timber.i(e.toString());
        }

        return result.toString();
    }

    public static void logCookies() {
        if (Constants.DEBUG) {
            Timber.i("---BEGIN COOKIE DUMP---");
            List<HttpCookie> cookies = ckmngr.getCookieStore().getCookies();
            for (HttpCookie c : cookies) {
                Timber.i(c.toString());
            }
            Timber.i("---END COOKIE DUMP---");
        }
    }

    /**
     * Parses all html-escaped characters to a regular Java string. Does not handle html tags.
     *
     * @param html
     * @return unencoded text.
     */
    public static String unencodeHtml(String html) {
        if (html == null) {
            return "";
        }
        String processed = StringEscapeUtils.unescapeHtml4(html);
        StringBuffer unencodedContent = new StringBuffer(processed.length());
        Matcher fixCharMatch = unencodeCharactersPattern.matcher(processed);
        while (fixCharMatch.find()) {
            fixCharMatch.appendReplacement(unencodedContent, Character.toString((char) Integer.parseInt(fixCharMatch.group(1))));
        }
        fixCharMatch.appendTail(unencodedContent);
        return unencodedContent.toString();
    }

    /**
     * Parses a Java string into html-escaped characters. Does not handle html tags.
     *
     * @return unencoded text.
     */
    public static String encodeHtml(String str) {
        StringBuffer unencodedContent = new StringBuffer(str.length());
        Matcher fixCharMatch = encodeCharactersPattern.matcher(str);
        while (fixCharMatch.find()) {
            fixCharMatch.appendReplacement(unencodedContent, "&#" + fixCharMatch.group(1).codePointAt(0) + ";");
        }
        fixCharMatch.appendTail(unencodedContent);
        return unencodedContent.toString();
    }
}
