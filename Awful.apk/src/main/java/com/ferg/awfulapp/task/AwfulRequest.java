package com.ferg.awfulapp.task;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.crashlytics.android.Crashlytics;
import com.ferg.awfulapp.AwfulApplication;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.util.AwfulError;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by Matt Shepard on 8/7/13.
 */
public abstract class AwfulRequest<T> {

    /** Used for identifying request types when cancelling, reassign this in subclasses */
    public static final Object REQUEST_TAG = new Object();
    public static final String TAG = "AwfulRequest";

    private Context cont;
    private String baseUrl;
    private Handler handle;
    private Map<String, String> params = null;
    private MultipartEntityBuilder attachParams = MultipartEntityBuilder.create();
    private HttpEntity httpEntity = null;
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
        void success(T result);

        /**
         * Called whenever a network request fails, parsing was not successful, or if a forums issue is detected.
         * If AwfulRequest.build() is provided an AwfulFragment ProgressListener, it will automatically pass the error to the AwfulFragment's displayAlert function.
         * @param error
         */
        void failure(VolleyError error);
    }


    public Object getRequestTag() {
        return REQUEST_TAG;
    }


    protected void addPostParam(String key, String value){
        if(key == null || value == null){
            //intentionally triggering that NPE here, so we can log it now instead of when it hits the volley queue
            //noinspection ConstantConditions,RedundantStringToString
            Timber.e("PARAM NULL: %s - v: %s", key.toString(), value.toString());
        }
        if(attachParams != null){
            try {
                attachParams.addPart(key, new StringBody(value, ContentType.TEXT_PLAIN));
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
            attachParams = MultipartEntityBuilder.create();
            if(params != null){
                for(Map.Entry<String, String> item : params.entrySet()){
                    try {
                        attachParams.addPart(item.getKey(), new StringBody(item.getValue(), ContentType.TEXT_PLAIN));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        attachParams.addPart(key, new FileBody(new File(filename)));
    }

    protected void buildFinalRequest()
    {
        httpEntity = attachParams.build();
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
                // TODO: 29/10/2017 this is a temporary warning/advice for people on older devices who can't connect - remove it once there's something better for recommending security updates
                if (error != null && StringUtils.contains(error.getMessage(), "SSLProtocolException")) {
                    Toast.makeText(cont, R.string.ssl_connection_error_message, Toast.LENGTH_LONG).show();
                }
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
        final ActualRequest actualRequest = new ActualRequest(generateUrl(helper), successListener, errorListener);
        actualRequest.setTag(getRequestTag());
        return actualRequest;
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
     * Customize the error a request delivers in its {@link ProgressListener#requestEnded(AwfulRequest, VolleyError)} callback.
     *
     * You can use this to provide a more meaningful error, e.g. for the automatic user alerts that
     * fragments display - be aware that returning a different error (instead of just changing the
     * message) may affect error handling, e.g. code that looks for a {@link AwfulError#ERROR_LOGGED_OUT}
     *
     * @param error The actual error, typically network failure or whatever.
     * @return the error to pass to listeners, or null for no error (and no alert)
     */
    protected VolleyError customizeProgressListenerError(VolleyError error){
        return error;
    }

    /**
     * Whether or not to automatically check for common page errors during the request process.
     * Override it and return false to disable these checks.
     * See {@link #handleError} and {@link AwfulError#checkPageErrors} for more details.
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

    private static final RetryPolicy lenientRetryPolicy = new DefaultRetryPolicy(20000, 1, 1);

    private class ActualRequest extends Request<T>{
        private Response.Listener<T> success;
        public ActualRequest(String url, Response.Listener<T> successListener, Response.ErrorListener errorListener) {
            super(params != null? Method.POST : Method.GET, url, errorListener);
            Timber.i("Created request: %s", url);
            success = successListener;
            setRetryPolicy(lenientRetryPolicy);
        }

        @Override
        protected Response<T> parseNetworkResponse(NetworkResponse response) {
            try{
                long startTime = System.currentTimeMillis();
                Timber.i("Starting parse: %s", getUrl());
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
                    if(Constants.DEBUG) {
                        long parseTime = System.currentTimeMillis() - startTime;
                        Timber.i("Successful parse: %s\nTook %dms", getUrl(), parseTime);
                    }
                    return Response.success(result, HttpHeaderParser.parseCacheHeaders(response));
                }catch(AwfulError ae){
                    updateProgress(100);
                    return Response.error(ae);
                }
            } catch (OutOfMemoryError e) {
                if (AwfulApplication.crashlyticsEnabled()) {
                    Crashlytics.setString("Response URL", getUrl());
                    Crashlytics.setLong("Response data size", response.data.length);
                }
                throw e;
            }catch(Exception e){
                updateProgress(100);
                Timber.e(e, "Failed parse: %s", getUrl());
                return Response.error(new ParseError(e));
            }
        }


        @Override
        protected VolleyError parseNetworkError(VolleyError volleyError) {
            String errorMessage = "Network error: ";
            if (volleyError == null) {
                errorMessage += "(null VolleyError)";
            } else {
                Timber.e(volleyError);
                if (volleyError.getCause() != null) {
                    String causeMessage = volleyError.getCause().getMessage();
                    errorMessage += (causeMessage == null) ? "unknown" : causeMessage;
                }
                if (volleyError.networkResponse != null) {
                    errorMessage += "\nStatus code: " + volleyError.networkResponse.statusCode;
                }
            }
            Timber.e(errorMessage);
            return volleyError;// new AwfulError(errorMessage);
        }


        @Override
        public Request<?> setRequestQueue(RequestQueue requestQueue) {
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
            return this;
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
                progressListener.requestEnded(AwfulRequest.this, customizeProgressListenerError(error));
            }
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String, String> headers = super.getHeaders();
            if(headers == null || headers.size() < 1){
                headers = new HashMap<>();
            }
            NetworkUtils.setCookieHeaders(headers);
            Timber.i("getHeaders: %s", headers);
            return headers;
        }

        @Override
        protected Map<String, String> getParams() throws AuthFailureError {
            return params;
        }

        @Override
        public byte[] getBody() throws AuthFailureError {
            if(attachParams != null){
                if (httpEntity == null) {
                    buildFinalRequest();
                }
                try{
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    httpEntity.writeTo(bytes);
                    return bytes.toByteArray();
                }catch(IOException ioe){
                    Timber.e("Failed to convert body bytestream");
                }
            }
            return super.getBody();
        }

        @Override
        public String getBodyContentType() {
            if(attachParams != null){
                if (httpEntity == null) {
                    buildFinalRequest();
                }
                return httpEntity.getContentType().getValue();
            }
            return super.getBodyContentType();
        }
    }


    public interface ProgressListener<T>{
        void requestStarted(AwfulRequest<T> req);
        void requestUpdate(AwfulRequest<T> req, int percent);
        void requestEnded(AwfulRequest<T> req, VolleyError error);
    }
}
