package com.ferg.awfulapp.task

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser

/**
 * Basic request to get the size of a resource at a given URL.
 *
 * In case of an error, the listener will be called with a null result.
 */
class ImageSizeRequest
/**
 * A Volley Request to get the size of a resource at a given URL.
 *
 * @param url      the url of the resource
 * @param listener receives a response containing the resource size, or null if there was an error
 */
(url: String, private val listener: Response.Listener<Int>) : Request<Int>(Request.Method.HEAD, url, null) {

    override fun parseNetworkResponse(response: NetworkResponse): Response<Int>? {
        val length = response.headers["Content-Length"] ?: return null
        return try {
            Response.success(Integer.parseInt(length), HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: NumberFormatException) {
            null
        }
    }

    override fun deliverResponse(response: Int?) {
        listener.onResponse(response)
    }

    override fun deliverError(error: VolleyError) {
        // just return a 'nope' size value, doesn't really matter why we failed
        listener.onResponse(null)
    }
}
