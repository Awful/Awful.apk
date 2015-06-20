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
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.thread.AwfulURL;
import com.ferg.awfulapp.util.LRUImageCache;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
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

    private static DefaultHttpClient sHttpClient;

    private static RequestQueue     mNetworkQueue;
    private static LRUImageCache    mImageCache;
    private static ImageLoader      mImageLoader;

    private static String cookie = null;
    private static final String COOKIE_HEADER = "Cookie";


    static {
        HttpParams httpPar = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpPar, 10000);//10 second timeout when connecting. does not apply to data transfer
        HttpConnectionParams.setSoTimeout(httpPar, 20000);//timeout to wait if no data transfer occurs //TODO bumped to 20, but we need to monitor cell status now
        HttpConnectionParams.setSocketBufferSize(httpPar, 65548);
        HttpClientParams.setRedirecting(httpPar, false);
        sHttpClient = new DefaultHttpClient(httpPar);
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

        if (useridCookieValue != null && passwordCookieValue != null && expiry != -1) {
            cookie = String.format("%s=%s;%s=%s;%s=%s;%s=%s;",
                    Constants.COOKIE_PREF_USERID, useridCookieValue,
                    Constants.COOKIE_PREF_PASSWORD, passwordCookieValue,
                    Constants.COOKIE_PREF_SESSIONID, sessionidCookieValue,
                    Constants.COOKIE_PREF_SESSIONHASH, sessionhashCookieValue);

            BasicClientCookie useridCookie =
                    new BasicClientCookie(Constants.COOKIE_NAME_USERID, useridCookieValue);
            BasicClientCookie passwordCookie =
                    new BasicClientCookie(Constants.COOKIE_NAME_PASSWORD, passwordCookieValue);
            BasicClientCookie sessionidCookie =
                    new BasicClientCookie(Constants.COOKIE_NAME_SESSIONID, sessionidCookieValue);
            BasicClientCookie sessionhashCookie =
                    new BasicClientCookie(Constants.COOKIE_NAME_SESSIONHASH, sessionhashCookieValue);

            Date expiryDate = new Date(expiry);
            BasicClientCookie[] allCookies = {useridCookie, passwordCookie, sessionidCookie, sessionhashCookie};
            for (BasicClientCookie tempCookie : allCookies) {
                tempCookie.setDomain(Constants.COOKIE_DOMAIN);
                tempCookie.setExpiryDate(expiryDate);
                tempCookie.setPath(Constants.COOKIE_PATH);
            }

            CookieStore jar = sHttpClient.getCookieStore();
            jar.addCookie(useridCookie);
            jar.addCookie(passwordCookie);
            sHttpClient.setCookieStore(jar);

            Log.w(TAG, "Cookies restored from prefs");
            Log.w(TAG, "Cookie dump: " + jar.toString());
            return true;
        } else {
            String logMsg = "Unable to restore cookies! Reasons:\n";
            logMsg += (useridCookieValue == null) ? "USER_ID is NULL\n" : "";
            logMsg += (passwordCookieValue == null) ? "PASSWORD is NULL\n" : "";
            logMsg += (expiry == -1) ? "EXPIRY is -1" : "";
            Log.w(TAG, logMsg);
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
        sHttpClient.getCookieStore().clear();
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

        List<Cookie> cookies = sHttpClient.getCookieStore().getCookies();
        for (Cookie cookie : cookies) {
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
                Date cookieExpiryDate = cookie.getExpiryDate();
                if (expires == null || (cookieExpiryDate != null && cookieExpiryDate.before(expires))) {
                    expires = cookieExpiryDate;
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

        edit.apply();
        return true;
    }

    public static synchronized String getCookieString(String type) {
        List<Cookie> cookies = sHttpClient.getCookieStore().getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getDomain().contains(Constants.COOKIE_DOMAIN)) {
                if (cookie.getName().contains(type)) {
                    return String.format("%s=%s; domain=%s", type, cookie.getValue(), cookie.getDomain());
                }
            }
        }
        Log.w(TAG, "getCookieString couldn't find type: " + type);
        return "";
    }

    public static Document get(AwfulURL aUrl, Messenger statusCallback, int midpointPercent) throws Exception {
        return get(new URI(aUrl.getURL()), statusCallback, midpointPercent);
    }

    public static Document get(String aUrl, HashMap<String, String> aParams) throws Exception {
        return get(new URI(aUrl + getQueryStringParameters(aParams)), null, 0);
    }

    public static Document get(String aUrl) throws Exception {
        return get(new URI(aUrl), null, 0);
    }

    public static Document get(String aUrl, HashMap<String, String> aParams, Messenger statusCallback, int midpointPercent) throws Exception {
        return get(new URI(aUrl + getQueryStringParameters(aParams)), statusCallback, midpointPercent);
    }

    public static Document get(URI location, Messenger statusCallback, int midpointPercent) throws Exception {
        Document response = null;

        Log.i(TAG, "Fetching " + location);

        HttpResponse httpResponse = sHttpClient.execute(new HttpGet(location));
        HttpEntity entity = httpResponse.getEntity();

        if (entity != null) {
            InputStream entityStream = entity.getContent();
            response = Jsoup.parse(entityStream, CHARSET, Constants.BASE_URL);
            entityStream.close();
        }

        Log.i(TAG, "Fetched " + location);
        return response;
    }


    public static void delete(String aUrl) throws Exception {
        Log.i(TAG, "DELETE: " + aUrl);
        HttpResponse hResponse = sHttpClient.execute(new HttpDelete(aUrl));
        if (hResponse.getStatusLine().getStatusCode() < 400) {
            throw new Exception("ERROR: " + hResponse.getStatusLine().getStatusCode());
        }
    }

    public static String getRedirect(String aUrl, HashMap<String, String> aParams) throws Exception {
        URI location;
        if (aParams != null) {
            location = new URI(aUrl + getQueryStringParameters(aParams));
        } else {
            location = new URI(aUrl);
        }

        HttpResponse httpResponse = sHttpClient.execute(new HttpGet(location));
        Header redirectLocation = httpResponse.getFirstHeader("Location");
        if (redirectLocation != null) {
            return redirectLocation.getValue();
        }
        return null;
    }

    public static InputStream getStream(String aUrl) throws Exception {
        URI location = new URI(aUrl);

        Log.i(TAG, "Fetching " + location);

        HttpResponse httpResponse;
        httpResponse = sHttpClient.execute(new HttpGet(location));
        return httpResponse.getEntity().getContent();
    }

    public static Document post(AwfulURL aUrl, Messenger statusCallback, int midpointPercent) throws Exception {
        return post(new URI(aUrl.getURL()), null, statusCallback, midpointPercent);
    }

    public static Document post(String aUrl, HashMap<String, String> aParams) throws Exception {
        return post(new URI(aUrl), aParams, null, 0);
    }


    public static Document post(String aUrl, HashMap<String, String> aParams, Messenger statusCallback, int midpointPercent) throws Exception {
        return post(new URI(aUrl), aParams, statusCallback, midpointPercent);
    }

    public static Document post(URI location, HashMap<String, String> aParams, Messenger statusCallback, int midpointPercent) throws Exception {
        Document response = null;

        Log.i(TAG, location.toString());

        HttpPost httpPost = new HttpPost(location);
        ArrayList<NameValuePair> paramdata = getPostParameters(aParams);
        if (location.equals(new URI(Constants.FUNCTION_LOGIN_SSL))) {
            UrlEncodedFormEntity post = new UrlEncodedFormEntity(paramdata, "CP1252");
            httpPost.setEntity(post);
        } else {
            MultipartEntityBuilder post = MultipartEntityBuilder.create();
            for (NameValuePair data : paramdata) {
                if ("attachment".equals(data.getName())) {
                    post.addPart(data.getName(), new FileBody(new File(data.getValue())));
                } else {
                    if (data.getValue() != null) {
                        post.addPart(data.getName(), new StringBody(data.getValue(), ContentType.TEXT_PLAIN));
                    }
                }
            }
            httpPost.setEntity(post.build());
        }

        HttpResponse httpResponse = sHttpClient.execute(httpPost);
        HttpEntity entity = httpResponse.getEntity();

        if (entity != null) {
            InputStream entityStream = entity.getContent();
            response = Jsoup.parse(entityStream, CHARSET, Constants.BASE_URL);
            entityStream.close();
        }

        return response;
    }

    /**
     * POSTs a message to the selected URL, but ignores any response body. Useful for quick-actions that we don't need the body content from.
     *
     * @param aUrl
     * @param aParams
     * @return HTTP Request status code (200 = success, ect)
     * @throws Exception
     */
    public static int postIgnoreBody(String aUrl, HashMap<String, String> aParams) throws Exception {

        Log.i(TAG, aUrl);
        HttpPost httpPost = new HttpPost(aUrl);
        httpPost.setEntity(
                new UrlEncodedFormEntity(getPostParameters(aParams)));

        HttpResponse httpResponse = sHttpClient.execute(httpPost);
        return httpResponse.getStatusLine().getStatusCode();
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
        Log.i(TAG, "---BEGIN COOKIE DUMP---");
        List<Cookie> cookies = sHttpClient.getCookieStore().getCookies();
        for (Cookie c : cookies) {
            Log.i(TAG, c.toString());
        }
        Log.i(TAG, "---END COOKIE DUMP---");
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
