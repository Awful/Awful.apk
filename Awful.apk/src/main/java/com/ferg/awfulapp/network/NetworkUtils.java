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
import android.os.Messenger;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.util.LRUImageCache;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkUtils {
    private static final String TAG = "NetworkUtils";
    private static final String CHARSET = "windows-1252";

    private static final Pattern unencodeCharactersPattern = Pattern.compile("&#(\\d+);");
    private static final Pattern encodeCharactersPattern = Pattern.compile("([^\\x00-\\x7F])");


    private static RequestQueue     mNetworkQueue;
    private static LRUImageCache    mImageCache;
    private static ImageLoader      mImageLoader;

    private static CookieManager ckmngr;

    private static String cookie = null;
    private static final String COOKIE_HEADER = "Cookie";


    static {
        ckmngr = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(ckmngr);
    }


    /**
     * Initialise request handling and caching - call this early!
     * @param context   A context used to create a cache dir
     */
    public static void init(Context context) {
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


    public static void clearImageCache() {
        if (mImageCache != null) {
            mImageCache.clear();
        }
    }


    public static void clearDiskCache() {
        Cache diskCache;
        if (mNetworkQueue != null && (diskCache = mNetworkQueue.getCache()) != null) {
            diskCache.clear();
        }
    }

    public static void queueRequest(Request request){
        if (mNetworkQueue != null) {
            mNetworkQueue.add(request);
        } else {
            Log.w(TAG, "Can't queue request - NetworkQueue is null, has NetworkUtils been initialised?");
        }
    }


    public static void cancelRequests(Object tag){
        if (mNetworkQueue != null) {
            mNetworkQueue.cancelAll(tag);
        } else {
            Log.w(TAG, "Can't cancel requests - NetworkQueue is null, has NetworkUtils been initialised?");
        }
    }

    public static void setCookieHeaders(Map<String, String> headers) {
        if(cookie == null){
            Log.e(TAG,"Cookie was empty for some reason, trying to restore cookie");
            restoreLoginCookies(AwfulPreferences.getInstance().getContext());
        }
        if (cookie.length() > 0) {
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

        if (useridCookieValue != null && passwordCookieValue != null && expiry != -1) {
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

            Log.e(TAG, "now.compareTo(expiryDate):" + (expiryDate.getTime() - now.getTime()));
            for (HttpCookie tempCookie : allCookies) {
                tempCookie.setVersion(cookieVersion);
                tempCookie.setDomain(Constants.COOKIE_DOMAIN);
                tempCookie.setMaxAge(expiryDate.getTime() - now.getTime());
                tempCookie.setPath(Constants.COOKIE_PATH);
            }
            ckmngr.getCookieStore().add(URI.create(Constants.COOKIE_DOMAIN), useridCookie);
            ckmngr.getCookieStore().add(URI.create(Constants.COOKIE_DOMAIN), passwordCookie);
            ckmngr.getCookieStore().add(URI.create(Constants.COOKIE_DOMAIN), sessionhashCookie);
            if(Constants.DEBUG) {
                Log.w(TAG, "Cookies restored from prefs");
                Log.w(TAG, "Cookie dump: " + TextUtils.join("\n", ckmngr.getCookieStore().getCookies()));
            }
            return true;
        } else {
            String logMsg = "Unable to restore cookies! Reasons:\n";
            logMsg += (useridCookieValue == null) ? "USER_ID is NULL\n" : "";
            logMsg += (passwordCookieValue == null) ? "PASSWORD is NULL\n" : "";
            logMsg += (expiry == -1) ? "EXPIRY is -1" : "";
            if(Constants.DEBUG) Log.w(TAG, logMsg);
            cookie = "";
        }

        return false;
    }

    /**
     * Clears cookies from both the current client's store and
     * the persistent SharedPreferences. Effectively, logs out.
     */
    public static synchronized void clearLoginCookies(Context ctx) {
        // First clear out the persistent preferences...
        if(null == ctx){
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

        List<HttpCookie> cookies = ckmngr.getCookieStore().getCookies();
        for (HttpCookie cookie : cookies) {
            if (cookie.getDomain().contains(Constants.COOKIE_DOMAIN)) {
                final String cookieName = cookie.getName();
                switch (cookieName) {
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
                Date cookieExpiryDate  = c.getTime();
                if (expires == null || (cookieExpiryDate != null && cookieExpiryDate.before(expires))) {
                    expires = cookieExpiryDate;
                }
                // fall back to the lowest cookie spec version
                if (version == null || cookie.getVersion() < version) {
                    version = cookie.getVersion();
                }
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
        Log.w(TAG, "getCookieString couldn't find type: " + type);
        return "";
    }

    public static Document get(String aUrl) throws Exception {
        return get(new URI(aUrl), null, 0);
    }

    public static Document get(URI location, Messenger statusCallback, int midpointPercent) throws Exception {
        Document response = null;
        String responseString = "";

        Log.i(TAG, "Fetching " + location);

        HttpURLConnection urlConnection = (HttpURLConnection) location.toURL().openConnection();
        try {
            if (urlConnection != null) {
                response = Jsoup.parse(urlConnection.getInputStream(), CHARSET, Constants.BASE_URL);
            }
        }finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        Log.i(TAG, "Fetched " + location);
        return response;
    }

//    public static void delete(String aUrl) throws Exception {
//        Log.i(TAG, "DELETE: " + aUrl);
//        HttpResponse hResponse = sHttpClient.execute(new HttpDelete(aUrl));
//        if (hResponse.getStatusLine().getStatusCode() < 400) {
//            throw new Exception("ERROR: " + hResponse.getStatusLine().getStatusCode());
//        }
//    }

    public static String getRedirect(String aUrl, HashMap<String, String> aParams) throws Exception {
        URI location;
        if (aParams != null) {
            location = new URI(aUrl + getQueryStringParameters(aParams));
        } else {
            location = new URI(aUrl);
        }
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


    private static ArrayList<NameValuePair> getPostParameters(HashMap<String, String> aParams) {
        // Append parameters
        ArrayList<NameValuePair> result = new ArrayList<NameValuePair>();

        if (aParams != null) {
            for (Map.Entry<String, String> param : aParams.entrySet()) {
                result.add(new BasicNameValuePair(param.getKey(), param.getValue()));
            }
        }

        return result;
    }

    public static String getQueryStringParameters(HashMap<String, String> aParams) {
        StringBuilder result = new StringBuilder("?");

        if (aParams != null) {
            try {
                // Loop over each parameter and add it to the query string
                Iterator<Map.Entry<String,String>> iter = aParams.entrySet().iterator();

                while (iter.hasNext()) {
                    Map.Entry<String, String> param = iter.next();

                    result.append(param.getKey()).append("=").append(URLEncoder.encode(param.getValue(), "UTF-8"));

                    if (iter.hasNext()) {
                        result.append("&");
                    }
                }
            } catch (UnsupportedEncodingException e) {
                Log.i(TAG, e.toString());
            }
        } else {
            return "";
        }

        return result.toString();
    }

    public static void logCookies() {
        if(Constants.DEBUG) {
            Log.i(TAG, "---BEGIN COOKIE DUMP---");
            List<HttpCookie> cookies = ckmngr.getCookieStore().getCookies();
            for (HttpCookie c : cookies) {
                Log.i(TAG, c.toString());
            }
            Log.i(TAG, "---END COOKIE DUMP---");
        }
    }

    public static String getAsString(Document pc) {
        return pc.text();
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



    /*
        Stupid garbage to stave off forced logouts
     */

    private static final int MAX_REPEATED_LOGOUT_DODGES = 4;
    private static AtomicInteger remaining_dodges = new AtomicInteger(MAX_REPEATED_LOGOUT_DODGES);

    /**
     * If a request hits the 'not registered' page, check if the user actually has
     * login cookies, and ignore a few of them. Call {@link #resetDodges()} when a
     * request succeeds normally, to reset the allowed failures counter
     * @return  True if the problem should be ignored, false if the user should be logged out
     */
    public synchronized static boolean dodgeLogoutBullet() {
        String username = NetworkUtils.getCookieString(Constants.COOKIE_NAME_USERID);
        String password = NetworkUtils.getCookieString(Constants.COOKIE_NAME_PASSWORD);
        if ("".equals(username) || "".equals(password)) {
            Log.w(TAG, "Your cookie is broken though, better log in");
            return false;
        }
        Log.w(TAG, "Looks like you're logged in to me though...");
        if (remaining_dodges.decrementAndGet() <= 0) {
            Log.w(TAG, "But it's happened " + MAX_REPEATED_LOGOUT_DODGES + " times in a row, logging out");
            return false;
        }
        Log.w(TAG, "Letting it slide, " + remaining_dodges.get() + " chances remaining");
        return true;
    }

    /**
     * Reset the failure counter, call this after a request passes the 'unregistered' check
     */
    public static void resetDodges() {
        remaining_dodges.set(MAX_REPEATED_LOGOUT_DODGES);
    }
}
