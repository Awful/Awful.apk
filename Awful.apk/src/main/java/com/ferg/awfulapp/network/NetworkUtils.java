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
import android.net.http.HttpResponseCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.util.LRUImageCache;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
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
        return redirectLocation;
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
