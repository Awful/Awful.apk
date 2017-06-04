package com.ferg.awfulapp.task;

import android.support.annotation.NonNull;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

/**
 * Basic request to get the size of a resource at a given URL.
 * <p>
 * In case of an error, the listener will be called with a null result.
 */
public class ImageSizeRequest extends Request<Integer> {
    private final Response.Listener<Integer> listener;

    /**
     * A Volley Request to get the size of a resource at a given URL.
     *
     * @param url      the url of the resource
     * @param listener receives a response containing the resource size, or null if there was an error
     */
    public ImageSizeRequest(@NonNull String url, Response.Listener<Integer> listener) {
        super(Method.HEAD, url, null);
        this.listener = listener;
    }

    @Override
    protected Response<Integer> parseNetworkResponse(NetworkResponse response) {
        String length = response.headers.get("Content-Length");
        if (length == null) {
            return null;
        }
        try {
            return Response.success(Integer.parseInt(length), HttpHeaderParser.parseCacheHeaders(response));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected void deliverResponse(Integer response) {
        listener.onResponse(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        // just return a 'nope' size value, doesn't really matter why we failed
        listener.onResponse(null);
    }
}
