package com.ferg.awfulapp.task

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.crashlytics.android.Crashlytics
import com.ferg.awfulapp.AwfulApplication
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants.BASE_URL
import com.ferg.awfulapp.constants.Constants.SITE_HTML_ENCODING
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.util.AwfulError
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Created by Matt Shepard on 8/7/13.
 */
abstract class AwfulRequest<T>(protected val context: Context, private val baseUrl: String?) {
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var params: MutableMap<String, String>? = null
    private var attachParams: MultipartEntityBuilder? = MultipartEntityBuilder.create()
    private var httpEntity: HttpEntity? = null
    private var progressListener: ProgressListener? = null


    open val requestTag: Any
        get() = REQUEST_TAG

    protected val preferences: AwfulPreferences
        get() = AwfulPreferences.getInstance(context)
    protected val contentResolver: ContentResolver
        get() = context.contentResolver

    interface AwfulResultCallback<T> {
        /**
         * Called whenever a queued request successfully completes.
         * The return value is optional and will likely be null depending on request type.
         * @param result Response result or null if request does not provide direct result (most requests won't).
         */
        fun success(result: T)

        /**
         * Called whenever a network request fails, parsing was not successful, or if a forums issue is detected.
         * If AwfulRequest.build() is provided an AwfulFragment ProgressListener, it will automatically pass the error to the AwfulFragment's displayAlert function.
         * @param error
         */
        fun failure(error: VolleyError?)
    }


    protected fun addPostParam(key: String, value: String) {
        attachParams?.attachParam(key, value)
        params = (params ?: HashMap()).apply { this[key] = value }
    }

    protected fun attachFile(key: String, filename: String) {
        attachParams = (attachParams ?: MultipartEntityBuilder.create()).apply {
            params?.forEach { (k, v) -> attachParam(k, v) }
            addPart(key, FileBody(File(filename)))
        }
    }

    private fun MultipartEntityBuilder.attachParam(key: String, value: String) {
        addPart(key, StringBody(value, ContentType.TEXT_PLAIN))
    }

    protected fun buildFinalRequest() {
        httpEntity = attachParams!!.build()
    }

    protected fun setPostParams(post: MutableMap<String, String>) {
        params = post
    }

    /**
     * Build request with no status/success/failure callbacks. Useful for fire-and-forget calls.
     * @return The final request, to pass into queueRequest.
     */
    fun build(): Request<T> {
        return build(null, null, null)
    }

    /**
     * Build request, using the ProgressListener (AwfulFragment already implements this)
     * and the AwfulResultCallback (for success/failure messages).
     * @param prog A ProgressListener, typically the current AwfulFragment instance. A null value disables progress updates.
     * @param resultListener AwfulResultCallback interface for success/failure callbacks. These will always be called on the UI thread.
     * @return A result to pass into queueRequest. (AwfulApplication implements queueRequest, AwfulActivity provides a convenience shortcut to access it)
     */
    fun build(prog: ProgressListener?, resultListener: AwfulResultCallback<T>?): Request<T> {
        return build(prog, Response.Listener { response ->
            resultListener?.success(response)
        },
                Response.ErrorListener { error ->
                    // TODO: 29/10/2017 this is a temporary warning/advice for people on older devices who can't connect - remove it once there's something better for recommending security updates
                    error?.message?.contains("SSLProtocolException")?.let {
                        Toast.makeText(context, R.string.ssl_connection_error_message, Toast.LENGTH_LONG).show()
                    }
                    resultListener?.failure(error)
                })
    }

    /**
     * Build request, same as build(ProgressListener, AwfulResultCallback<T>) but provides direct access to volley callbacks.
     * There is no real reason to use this over the other version.
     * @param prog
     * @param successListener
     * @param errorListener
     * @return
    </T> */
    private fun build(prog: ProgressListener?, successListener: Response.Listener<T>?, errorListener: Response.ErrorListener?): Request<T> {
        progressListener = prog
        val helper = baseUrl?.run(Uri::parse)?.run(Uri::buildUpon)
        val actualRequest = ActualRequest(generateUrl(helper), successListener, errorListener)
        actualRequest.tag = requestTag
        return actualRequest
    }

    /**
     * Generate the URL to use in the request here. This includes any query arguments.
     * A Uri.Builder is provided with the base URL already processed if a base URL is provided in the constructor.
     * @param urlBuilder A Uri.Builder instance with the provided base URL. If no URL is provided in the constructor, this will be null.
     * @return String containing the full request URL.
     */
    protected abstract fun generateUrl(urlBuilder: Uri.Builder?): String

    /**
     * Handle the parsed response [doc]ument here, process any data and return any values if needed.
     *
     * The return value is optional, you can specify Void? for the type and return null. This result
     * will be passed to the response listener.
     */
    @Throws(AwfulError::class)
    protected abstract fun handleResponse(doc: Document): T


    /**
     * Handler for [error]s thrown by [AwfulError.checkPageErrors] when parsing a response.
     *
     * The main response-handler logic calls this when it encounters an [AwfulError], to check whether
     * the request implementation will handle it (and processing can proceed to [handleResponse]).
     * Returns true if the error was handled.
     *
     * By default this swallows non-critical errors, and allows everything else through. If you need
     * different behaviour for some reason, override this!
     */
    protected open fun handleError(error: AwfulError, doc: Document): Boolean = !error.isCritical

    /**
     * Customize the error a request delivers in its [ProgressListener.requestEnded] callback.
     *
     * You can use this to provide a more meaningful error, e.g. for the automatic user alerts that
     * fragments display - be aware that returning a different error (instead of just changing the
     * message) may affect error handling, e.g. code that looks for a [AwfulError.ERROR_LOGGED_OUT]
     *
     * @param error The actual error, typically network failure or whatever.
     * @return the error to pass to listeners, or null for no error (and no alert)
     */
    protected open fun customizeProgressListenerError(error: VolleyError): VolleyError = error


    protected fun updateProgress(percent: Int) {
        //updateProgress() will be called from a secondary thread, so run these on the UI thread.
        progressListener?.let { handler.post { it.requestUpdate(this@AwfulRequest, percent) } }
    }


    @Throws(IOException::class)
    protected open fun parseAsHtml(response: NetworkResponse): Document {
        val jsoupParseStart = System.currentTimeMillis()
        val doc = Jsoup.parse(ByteArrayInputStream(response.data), SITE_HTML_ENCODING, BASE_URL)
        Timber.d("Jsoup parsing finished (took ${System.currentTimeMillis() - jsoupParseStart}ms)")
        return doc
    }

    /**
     * Allows subclasses (i.e. AwfulStrippedRequest) to direct the document to the appropriate handler function.
     * Feels clunky to have this (don't override it in concrete classes!) as well as #handleResponse (do override that!)
     */
    @Throws(AwfulError::class)
    protected open fun handleResponseDocument(document: Document): T {
        return handleResponse(document)
    }

    private inner class ActualRequest internal constructor(
            url: String,
            private val success: Response.Listener<T>?,
            errorListener: Response.ErrorListener?
    ) : Request<T>(
            if (params != null) Request.Method.POST else Request.Method.GET,
            url,
            errorListener
    ) {

        init {
            Timber.i("Created request: $url")
            retryPolicy = lenientRetryPolicy
        }


        override fun parseNetworkResponse(response: NetworkResponse): Response<T> {
            val startTime = System.currentTimeMillis()
            Timber.i("Starting parse: $url")
            updateProgress(25)
            try {
                val doc = parseAsHtml(response)
                updateProgress(50)
                    val error = AwfulError.checkPageErrors(doc, preferences)
                    if (error != null && handleError(error, doc)) {
                        throw error
                    }

                val result = handleResponseDocument(doc)
                Timber.d("Successful parse: $url\nTook ${System.currentTimeMillis() - startTime}ms")
                return Response.success(result, HttpHeaderParser.parseCacheHeaders(response))
            } catch (ae: AwfulError) {
                return Response.error(ae)
            } catch (e: OutOfMemoryError) {
                if (AwfulApplication.crashlyticsEnabled()) {
                    Crashlytics.setString("Response URL", url)
                    Crashlytics.setLong("Response data size", response.data.size.toLong())
                }
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed parse: $url")
                return Response.error(ParseError(e))
            } finally {
                updateProgress(100)
            }
        }


        override fun parseNetworkError(volleyError: VolleyError?): VolleyError? {
            return volleyError.apply {
                with(StringBuilder()) {
                    append("Network error: ")
                    if (this@apply == null) {
                        append("(null VolleyError)")
                    } else {
                        Timber.e(volleyError)
                        append(cause?.message ?: "unknown cause")
                        networkResponse?.let { append("\nStatus code: ${networkResponse.statusCode}") }
                    }
                    Timber.e(toString())
                }
            }
        }


        override fun setRequestQueue(requestQueue: RequestQueue): Request<*> {
            super.setRequestQueue(requestQueue)
            progressListener?.let { handler.post { it.requestStarted(this@AwfulRequest) } }
            return this
        }

        override fun deliverResponse(response: T) {
            success?.onResponse(response)
            progressListener?.requestEnded(this@AwfulRequest, null)
        }

        override fun deliverError(error: VolleyError) {
            super.deliverError(error)
            progressListener?.requestEnded(this@AwfulRequest, customizeProgressListenerError(error))
        }

        @Throws(AuthFailureError::class)
        override fun getHeaders(): Map<String, String> {
            return (super.getHeaders()?.takeIf { it.isNotEmpty() } ?: HashMap())
                    .also(NetworkUtils::setCookieHeaders)
                    .also { Timber.i("getHeaders: %s", this) }
        }

        @Throws(AuthFailureError::class)
        override fun getParams(): Map<String, String>? = this@AwfulRequest.params

        @Throws(AuthFailureError::class)
        override fun getBody(): ByteArray {
            attachParams?.let {
                if (httpEntity == null) buildFinalRequest()
                try {
                    return ByteArrayOutputStream().apply(httpEntity!!::writeTo).toByteArray()
                } catch (ioe: IOException) {
                    Timber.e(ioe, "Failed to convert response body byte stream")
                }
            }
            return super.getBody()
        }

        override fun getBodyContentType(): String {
            attachParams?.let {
                if (httpEntity == null) buildFinalRequest()
                return httpEntity!!.contentType.value
            }
            return super.getBodyContentType()
        }
    }


    interface ProgressListener {
        fun requestStarted(req: AwfulRequest<*>)
        fun requestUpdate(req: AwfulRequest<*>, percent: Int)
        fun requestEnded(req: AwfulRequest<*>, error: VolleyError?)
    }

    companion object {

        /** Used for identifying request types when cancelling, reassign this in subclasses  */
        val REQUEST_TAG = Any()
        val TAG = "AwfulRequest"

        private val lenientRetryPolicy = DefaultRetryPolicy(20000, 1, 1f)
    }
}
