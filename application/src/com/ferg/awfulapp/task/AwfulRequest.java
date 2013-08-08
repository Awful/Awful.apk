package com.ferg.awfulapp.task;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.android.volley.*;
import com.android.volley.toolbox.HttpHeaderParser;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Matt Shepard on 8/7/13.
 */
public abstract class AwfulRequest<T> {
    private Context cont;
    private String baseUrl;
    private Handler handle;
    private ProgressListener progressListener;
    public AwfulRequest(Context context, String apiUrl) {
        cont = context;
        baseUrl = apiUrl;
        handle = new Handler(Looper.getMainLooper());
    }

    public interface AwfulResultCallback<T>{
        public void success(T result);
        public void failure(VolleyError error);
    }

    public Request<T> build(ProgressListener prog, final AwfulResultCallback<T> resultListener){
        return build(prog, new Response.Listener<T>() {
            @Override
            public void onResponse(T response) {
                resultListener.success(response);
            }
        },
        new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                resultListener.failure(error);
            }
        });
    }

    private Request<T> build(ProgressListener prog, Response.Listener<T> successListener, Response.ErrorListener errorListener){
        progressListener = prog;
        Uri.Builder helper = null;
        if(baseUrl != null){
            try{
                helper = Uri.parse(baseUrl).buildUpon();
            }catch(Exception e){
                helper = null;
            }
        }
        return new ActualRequest(generateUrl(helper), successListener, errorListener);
    }

    /**
     * Generate the URL to use in the request here. This includes any query arguments.
     * A Uri.Builder is provided with the base URL already processed if a base URL is provided in the constructor.
     * @param urlBuilder A Uri.Builder instance with the provided base URL. If no URL is provided in the constructor, this will be null.
     * @return String containing the full request URL.
     */
    protected abstract String generateUrl(Uri.Builder urlBuilder);

    /**
     * Handle the server response here, process any data and return any values if needed.
     * The return value is optional, you can specify Void for the type and return null.
     * @param doc The document containing the data from the request.
     * @return Any value returned will be provided to the Response.Listener callback.
     */
    protected abstract T handleResponse(Document doc) throws AwfulError;

    /**
     * Before a response is handled, it will be checked against AwfulError.checkPageErrors().
     * If that check fails, this function will be called and will include the error information.
     * You can choose to stop the request, or allow it to proceed to handleResponse().
     * If the request is stopped, the AwfulError class will be provided to the error listener. Otherwise the process will continue normally.
     * NOTE: Automatic error checking can be disabled by overriding shouldCheckErrors().
     * @param error An AwfulError object containing the cause for this error.
     * @param doc The full document file of the response, which will be provided to the handleResponse() if you allow the request to continue.
     * @return false to allow the request to continue to the handleResponse() stage, true will stop the request and return the AwfulError to the error handler callback.
     */
    protected abstract boolean handleError(AwfulError error, Document doc);

    protected AwfulPreferences getPreferences(){
        return AwfulPreferences.getInstance(cont);
    }
    protected Context getContext(){
        return cont;
    }
    protected ContentResolver getContentResolver(){
        return cont.getContentResolver();
    }

    /**
     * Whether or not to automatically check for common page errors during the request process.
     * Override it and return false to disable these checks.
     * @see handleError() and AwfulError.checkPageErrors() for more details.
     * @return true to automatically check, false to disable.
     */
    protected boolean shouldCheckErrors(){
        return true;
    }

    protected void updateProgress(final int percent){
        if(progressListener != null){
            //updateProgress() will be called from a secondary thread, so run these on the UI thread.
            handle.post(new Runnable() {
                @Override
                public void run() {
                    progressListener.progressUpdate(AwfulRequest.this, percent);
                }
            });
        }
    }

    private class ActualRequest extends Request<T>{
        private Response.Listener<T> success;
        public ActualRequest(String url, Response.Listener<T> successListener, Response.ErrorListener errorListener) {
            super(Method.GET, url, errorListener);
            if(Constants.DEBUG) Log.e("AwfulRequest", "Created request: " + url);
            success = successListener;
        }

        @Override
        protected Response<T> parseNetworkResponse(NetworkResponse response) {
            try{
                if(Constants.DEBUG) Log.i("AwfulRequest", "Starting parse: " + getUrl());
                updateProgress(25);
                Document doc = Jsoup.parse(new ByteArrayInputStream(response.data), HttpHeaderParser.parseCharset(response.headers), Constants.BASE_URL);
                updateProgress(50);
                if(shouldCheckErrors()){
                    AwfulError error = AwfulError.checkPageErrors(doc, getPreferences());
                    if(error != null && handleError(error, doc)){
                        updateProgress(100);
                        return Response.error(error);
                    }
                }
                try{
                    T result = handleResponse(doc);
                    updateProgress(100);
                    if(Constants.DEBUG) Log.i("AwfulRequest", "Successful parse: " + getUrl());
                    return Response.success(result, HttpHeaderParser.parseCacheHeaders(response));
                }catch(AwfulError ae){
                    updateProgress(100);
                    return Response.error(ae);
                }
            }catch(Exception e){
                updateProgress(100);
                if(Constants.DEBUG) Log.i("AwfulRequest", "Failed parse: " + getUrl());
                return Response.error(new ParseError(e));
            }
        }

        @Override
        protected void deliverResponse(T response) {
            success.onResponse(response);
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String, String> headers = super.getHeaders();
            if(headers == null || headers.size() < 1){
                headers = new HashMap<String, String>();
            }
            NetworkUtils.setCookieHeaders(headers);
            if(Constants.DEBUG) Log.i("AwfulRequest", "getHeaders: "+headers.toString());
            return headers;
        }
    }

    public static interface ProgressListener{
        public void progressUpdate(AwfulRequest req, int percent);
    }
}
