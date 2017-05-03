package com.ferg.awfulapp.task;

import android.support.annotation.NonNull;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

public class ImageSizeRequest extends Request<Integer> {
    private final Response.Listener<Integer> listener;

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
}
