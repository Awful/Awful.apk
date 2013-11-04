package com.ferg.awfulapp.task;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.android.volley.*;
import com.android.volley.toolbox.HttpHeaderParser;
import com.ferg.awfulapp.*;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.util.AwfulError;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Matt Shepard on 8/7/13.
 */
public abstract class AwfulRequest<T> {
    private Context cont;
    private String baseUrl;
    private Handler handle;
    private Map<String, String> params = null;
    private MultipartEntity attachParams = null;
    private ProgressListener progressListener;
    public AwfulRequest(Context context, String apiUrl) {
        cont = context;
        baseUrl = apiUrl;
        handle = new Handler(Looper.getMainLooper());
    }

    public interface AwfulResultCallback<T>{
        /**
         * Called whenever a queued request successfully completes.
         * The return value is optional and will likely be null depending on request type.
         * @param result Response result or null if request does not provide direct result (most requests won't).
         */
        public void success(T result);

        /**
         * Called whenever a network request fails, parsing was not successful, or if a forums issue is detected.
         * If AwfulRequest.build() is provided an AwfulFragment ProgressListener, it will automatically pass the error to the AwfulFragment's displayAlert function.
         * @param error
         */
        public void failure(VolleyError error);
    }

    protected void addPostParam(String key, String value){
        if(key == null || value == null){
            //intentionally triggering that NPE here, so we can log it now instead of when it hits the volley queue
            Log.e("AWFULREQUEST", "PARAM NULL: "+key.toString()+" -v: "+value.toString());
        }
        if(attachParams != null){
            try {
                attachParams.addPart(key, new StringBody(value));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(params == null){
            params = new HashMap<String, String>();
        }
        params.put(key, value);
    }

    protected void attachFile(String key, String filename){
        if(attachParams == null){
            attachParams = new MultipartEntity();
            if(params != null){
                for(Map.Entry<String, String> item : params.entrySet()){
                    try {
                        attachParams.addPart(item.getKey(), new StringBody(item.getValue()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        attachParams.addPart(key, new FileBody(new File(filename)));
    }

    protected void setPostParams(Map<String, String> post){
        params = post;
    }

    /**
     * Build request with no status/success/failure callbacks. Useful for fire-and-forget calls.
     * @return The final request, to pass into queueRequest.
     */
    public Request<T> build(){
        return build(null, null, null);
    }

    /**
     * Build request, using the ProgressListener (AwfulFragment already implements this)
     * and the AwfulResultCallback (for success/failure messages).
     * @param prog A ProgressListener, typically the current AwfulFragment instance. A null value disables progress updates.
     * @param resultListener AwfulResultCallback interface for success/failure callbacks. These will always be called on the UI thread.
     * @return A result to pass into queueRequest. (AwfulApplication implements queueRequest, AwfulActivity provides a convenience shortcut to access it)
     */
    public Request<T> build(ProgressListener prog, final AwfulResultCallback<T> resultListener){
        return build(prog, new Response.Listener<T>() {
            @Override
            public void onResponse(T response) {
                if(resultListener != null){
                    resultListener.success(response);
                }
            }
        },
        new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(resultListener != null){
                    resultListener.failure(error);
                }
            }
        });
    }

    /**
     * Build request, same as build(ProgressListener, AwfulResultCallback<T>) but provides direct access to volley callbacks.
     * There is no real reason to use this over the other version.
     * @param prog
     * @param successListener
     * @param errorListener
     * @return
     */
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
     * This pretty much only exists so you can replace generic Volley errors with more user-friendly messages (gimmicks)
     * It is only called after all the normal volley error handling occurs, so this only affects the automatic user alerts.
     * @param error The actual error, typically network failure or whatever.
     * @return Just return an AwfulError with whatever message you want. Or null to disable alerts.
     */
    protected VolleyError customizeAlert(VolleyError error){
        return error;
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
                    progressListener.requestUpdate(AwfulRequest.this, percent);
                }
            });
        }
    }

    private class ActualRequest extends Request<T>{
        private Response.Listener<T> success;
        public ActualRequest(String url, Response.Listener<T> successListener, Response.ErrorListener errorListener) {
            super(params != null? Method.POST : Method.GET, url, errorListener);
            if(Constants.DEBUG) Log.e("AwfulRequest", "Created request: " + url);
            success = successListener;
        }

        @Override
        protected Response<T> parseNetworkResponse(NetworkResponse response) {
            try{
                if(Constants.DEBUG) Log.i("AwfulRequest", "Starting parse: " + getUrl());
                updateProgress(25);
                Document doc = Jsoup.parse(new ByteArrayInputStream(response.data), "CP1252", Constants.BASE_URL);
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
        public void setRequestQueue(RequestQueue requestQueue) {
            super.setRequestQueue(requestQueue);
            if(progressListener != null){
                handle.post(new Runnable() {
                    @Override
                    public void run() {
                        if(progressListener != null){
                            progressListener.requestStarted(AwfulRequest.this);
                        }
                    }
                });
            }
        }

        @Override
        protected void deliverResponse(T response) {
            if(success != null){
                success.onResponse(response);
            }
            if(progressListener != null){
                progressListener.requestEnded(AwfulRequest.this, null);
            }
        }

        @Override
        public void deliverError(VolleyError error) {
            super.deliverError(error);
            if(progressListener != null){
                progressListener.requestEnded(AwfulRequest.this, customizeAlert(error));
            }
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

        @Override
        protected Map<String, String> getParams() throws AuthFailureError {
            return params;
        }

        @Override
        public byte[] getBody() throws AuthFailureError {
            if(attachParams != null){
                try{
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    attachParams.writeTo(bytes);
                    return bytes.toByteArray();
                }catch(IOException ioe){
                    Log.e("AwfulRequest", "Failed to convert body bytestream");
                }
            }
            return super.getBody();
        }

        @Override
        public String getBodyContentType() {
            if(attachParams != null){
                return attachParams.getContentType().getValue();
            }
            return super.getBodyContentType();
        }
    }

    /**
     * Utility callbacks for AwfulRequest status updates.
     * This is for updating the actionbar within AwfulFragment.
     * You shouldn't need to use these, look at the AwfulResultCallback interface for success/failure results.
     */
    public static interface ProgressListener{
        public void requestStarted(AwfulRequest req);
        public void requestUpdate(AwfulRequest req, int percent);
        public void requestEnded(AwfulRequest req, VolleyError error);
    }
}
