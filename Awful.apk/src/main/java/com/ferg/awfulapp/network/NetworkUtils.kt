/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the software nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
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
 */
package com.ferg.awfulapp.network

import android.content.Context
import android.net.http.HttpResponseCache
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.Volley
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.util.LRUImageCache
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber.Forest.e
import timber.log.Timber.Forest.i
import timber.log.Timber.Forest.w
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.regex.Pattern

object NetworkUtils {
    private const val CHARSET = "windows-1252"

    private val unencodeCharactersPattern: Pattern = Pattern.compile("&#(\\d+);")
    private val encodeCharactersPattern: Pattern = Pattern.compile("([^\\x00-\\x7F])")

    private var mNetworkQueue: RequestQueue? = null
    private var mImageCache: LRUImageCache? = null
    private var mImageLoader: AwfulImageLoader? = null

    /**
     * Initialise request handling and caching - call this early!
     * 
     * @param context A context used to create a cache dir
     */
    fun init(context: Context) {
        // update the security provider first, to ensure we fix SSL errors before setting anything else up
        SecurityProvider.update(context)
        mNetworkQueue = Volley.newRequestQueue(context)
        // TODO: find out if this is even being used anywhere
        mImageCache = LRUImageCache()
        mImageLoader = AwfulImageLoader(mNetworkQueue, mImageCache)

        try {
            HttpResponseCache.install(File(context.cacheDir, "httpcache"), 5242880)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    val imageLoader: ImageLoader?
        get() = mImageLoader

    fun clearImageCache() {
        if (mImageCache != null) {
            mImageCache!!.clear()
        }
    }

    @JvmStatic
    fun queueRequest(request: Request<*>) {
        if (mNetworkQueue != null) {
            mNetworkQueue!!.add(request)
        } else {
            w("Can't queue request - NetworkQueue is null, has NetworkUtils been initialised?")
        }
    }

    fun cancelRequests(tag: Any?) {
        if (mNetworkQueue != null) {
            mNetworkQueue!!.cancelAll(tag)
        } else {
            w("Can't cancel requests - NetworkQueue is null, has NetworkUtils been initialised?")
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun get(aUrl: String?): Document? {
        return get(URI(aUrl))
    }

    @Throws(Exception::class)
    fun get(location: URI): Document? {
        i("Fetching %s", location)

        val urlConnection = location.toURL().openConnection() as HttpURLConnection?

        if (urlConnection == null) {
            e("Couldn't open connection")
            return null
        }

        var response: Document?

        try {
            val inputStream = urlConnection.getInputStream()
            response = Jsoup.parse(inputStream, CHARSET, Constants.BASE_URL)
        } finally {
            urlConnection.disconnect()
        }

        i("Fetched %s", location)
        return response
    }

    @Throws(Exception::class)
    @JvmStatic
    fun getRedirect(aUrl: String?, aParams: MutableMap<String?, String?>?): String {
        val location = URI(aUrl + getQueryStringParameters(aParams))

        var redirectLocation: String?
        val urlConnection = location.toURL().openConnection() as HttpURLConnection
        try {
            redirectLocation = urlConnection.getHeaderField("Location")
            if (redirectLocation == null) {
                // HttpURLConnection redirects internally, so get the end result instead
                redirectLocation = urlConnection.getURL().toString()
            }
        } finally {
            urlConnection.disconnect()
        }
        return redirectLocation
    }

    /**
     * Build a html query string from a map.
     * 
     * 
     * Returns an empty string if `parameters` is null
     * 
     * @param parameters Map of query string pairs
     * @return A valid query string
     */
    fun getQueryStringParameters(parameters: MutableMap<String?, String?>?): String {
        if (parameters == null) return ""

        val result = StringBuilder("?")

        try {
            var separator = ""

            for ((key, value) in parameters) {
                result.append(separator)
                    .append(key)
                    .append("=")
                    .append(URLEncoder.encode(value, "UTF-8"))

                separator = "&"
            }
        } catch (e: UnsupportedEncodingException) {
            i(e.toString())
        }

        return result.toString()
    }

    /**
     * Parses all html-escaped characters to a regular Java string. Does not handle html tags.
     * 
     * @param html
     * @return unencoded text.
     */
    @JvmStatic
    fun unencodeHtml(html: String?): String {
        if (html == null) {
            return ""
        }
        val processed = StringEscapeUtils.unescapeHtml4(html)

        val unencodedContent = StringBuffer(processed.length)
        val fixCharMatch = unencodeCharactersPattern.matcher(processed)
        while (fixCharMatch.find()) {
            val found = fixCharMatch.group(1) ?: continue
            fixCharMatch.appendReplacement(
                unencodedContent,
                found.toInt().toChar().toString()
            )
        }
        fixCharMatch.appendTail(unencodedContent)
        return unencodedContent.toString()
    }

    /**
     * Parses a Java string into html-escaped characters. Does not handle html tags.
     * 
     * @param str String to process
     * @return unencoded text.
     */
    fun encodeHtml(str: String): String {
        val unencodedContent = StringBuffer(str.length)
        val fixCharMatch = encodeCharactersPattern.matcher(str)
        while (fixCharMatch.find()) {
            fixCharMatch.appendReplacement(
                unencodedContent,
                "&#" + fixCharMatch.group(1)?.codePointAt(0) + ";"
            )
        }
        fixCharMatch.appendTail(unencodedContent)
        return unencodedContent.toString()
    }
}
