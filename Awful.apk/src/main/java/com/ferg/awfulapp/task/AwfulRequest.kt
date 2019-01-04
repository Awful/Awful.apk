package com.ferg.awfulapp.task

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.support.annotation.UiThread
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
import com.ferg.awfulapp.task.AwfulRequest.Parameters.GetParams
import com.ferg.awfulapp.task.AwfulRequest.Parameters.PostParams
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

/**
 * Base class for requests to the Something Awful forums site, with HTML response and error handling.
 *
 * You can create a request by subclassing this, specifying the [baseUrl] you want to call
 * (and [isPostRequest] if necessary), and then adding data to the [parameters] object to define
 * the GET/POST parameters, any attachments etc. To send the request, call [build] with any required
 * callback listeners, and pass the resulting [Request] to a Volley queue (e.g. through
 * [NetworkUtils.queueRequest].
 *
 * The only method you need to implement is [handleResponse], where you process the response's HTML
 * [Document] and produce a return value for any result listeners. If you don't actually need to
 * do anything with the response (e.g. a fire-and-forget message to the site) you can just set  [T]
 * to a nullable type (Void? makes most sense if there's no meaningful result) and return null here.
 *
 * [handleError] and [customizeProgressListenerError] both have default implementations, but can be
 * overridden for special error handling and creating custom notification messages. You probably
 * won't want to change [handleError] in most cases, and if you do add any handler code (e.g. if
 * a network failure requires the app to update some state) you'll probably just want to call the
 * super method to get the standard error handling when you're done.
 */
abstract class AwfulRequest<T>(protected val context: Context, private val baseUrl: String, private val isPostRequest: Boolean = false) {
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var progressListener: ProgressListener? = null

    /**
     * Represents parameters to be added to the final request.
     * The concrete type depends on whether this is a GET or POST request.
     */
    protected val parameters: Parameters

    init {
        parameters = if (isPostRequest) PostParams() else GetParams()
    }


    protected sealed class Parameters {
        /** add a simple key/value parameter to this request */
        abstract fun add(key: String, value: String)

        /** add a file to this request by specifying its path */
        abstract fun attachFile(key: String, filePath: String)

        class PostParams : Parameters() {
            val params: MultipartEntityBuilder = MultipartEntityBuilder.create()
            val httpEntity: HttpEntity by lazy { params.build() }

            override fun add(key: String, value: String) {
                params.addPart(key, StringBody(value, ContentType.TEXT_PLAIN))
            }

            override fun attachFile(key: String, filePath: String) {
                params.addPart(key, FileBody(File(filePath)))
            }
        }

        class GetParams : Parameters() {
            val params = HashMap<String, String>()

            override fun add(key: String, value: String) {
                params[key] = value
            }

            override fun attachFile(key: String, filePath: String) {
                throw RuntimeException("Can't attach a file with a GET request - use a POST one instead")
            }
        }
    }


    open val requestTag: Any get() = REQUEST_TAG

    protected val preferences: AwfulPreferences get() = AwfulPreferences.getInstance(context)
    protected val contentResolver: ContentResolver get() = context.contentResolver


    /**
     * Build this request, for passing into [NetworkUtils.queueRequest].
     *
     * You can provide an optional [progressListener] for progress updates on the UI thread
     * (e.g. to update a loading bar). This will typically be an AwfulFragment.
     *
     * Passing in a [resultListener] will give you success and failure callbacks on the UI thread -
     * if you don't care about these (e.g. for fire-and-forget requests) this can be left null.
     *
     * Since both listeners receive 'finished' callbacks, with any resulting errors, you'll probably
     * want to handle any UI activity through [progressListener] and do any app logic through the
     * [resultListener] callback. By passing in an AwfulFragment as the progress listener, you'll
     * get progress bar updates and error message display for free!
     */
    @JvmOverloads
    fun build(progressListener: ProgressListener? = null, resultListener: AwfulResultCallback<T>? = null): Request<T> {
        this@AwfulRequest.progressListener = progressListener
        // if it's a GET request, we need to build the full parameterised URL here
        val requestUrl =
                if (parameters is GetParams) {
                    val builder = Uri.parse(baseUrl).buildUpon()
                    parameters.params.entries
                            .fold(builder) { uri, (k, v) -> uri.appendQueryParameter(k, v) }
                            .build().toString()
                } else baseUrl

        val successListener = resultListener?.let { Response.Listener(it::success) }

        val errorListener = Response.ErrorListener { error ->
            // TODO: 29/10/2017 this is a temporary warning/advice for people on older devices who can't connect - remove it once there's something better for recommending security updates
            if (error?.message?.contains("SSLProtocolException") == true) {
                Toast.makeText(context, R.string.ssl_connection_error_message, Toast.LENGTH_LONG).show()
            }
            resultListener?.failure(error)
        }

        return ActualRequest(requestUrl, successListener, errorListener).apply { tag = requestTag }
    }


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
     * By default this swallows non-critical errors, and returns false for everything else. If you need
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
    // TODO: check if any request classes should be using this, for better error feedback


    /**
     * Pass a progress [percent]age to any progress listener attached to this request.
     */
    private fun updateProgress(percent: Int) {
        //updateProgress() will be called from a secondary thread, so run these on the UI thread.
        progressListener?.let { handler.post { it.requestUpdate(this@AwfulRequest, percent) } }
    }


    /**
     * Parse a HTML [Document] from this request's [response].
     *
     * Don't override this, it's an internal function that's handled differently by [AwfulStrippedRequest]
     */
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


    /**
     * Final Volley Request class, created when the AwfulRequest is complete and ready to be queued.
     *
     * Since GET requests (apparently?) require their full parameterised URL to be passed into
     * the constructor here, we can't just make AwfulRequest a subclass of this, since its subclasses
     * add their GET parameters in the init blocks
     */
    private inner class ActualRequest internal constructor(
            url: String,
            private val success: Response.Listener<T>?,
            errorListener: Response.ErrorListener
    ) : Request<T>(
            if (isPostRequest) Request.Method.POST else Request.Method.GET,
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
                // TODO: find out what else this is meant to be catching, because it's swallowing every exception
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
            return mutableMapOf<String, String>().apply(NetworkUtils::setCookieHeaders)
                    .also { Timber.i("getHeaders: %s", this) }
        }


        @Throws(AuthFailureError::class)
        override fun getBody(): ByteArray {
            check(parameters is PostParams)
            return try {
                ByteArrayOutputStream().apply(parameters.httpEntity::writeTo).toByteArray()
            } catch (e: IOException) {
                Timber.w(e, "Failed to convert response body byte stream")
                super.getBody()
            }
        }

        override fun getBodyContentType(): String {
            check(parameters is PostParams)
            return parameters.httpEntity.contentType.value
        }
    }


    /**
     * Receives callbacks when a request succeeds or fails.
     */
    interface AwfulResultCallback<T> {
        /**
         * Called when the queued request successfully completes.
         *
         * If the request returns a [result] it will be passed here - most requests don't, in which
         * case this will be null.
         */
        @UiThread
        fun success(result: T)

        /**
         * Called when the network request fails, parsing was not successful, or if a forums issue was detected.
         *
         * Any generated [error] will be provided here, which may provide useful information!
         */
        @UiThread
        fun failure(error: VolleyError?)
    }


    /**
     * Receives callbacks on the request's lifecycle.
     */
    interface ProgressListener {
        /** Called when the request has been queued */
        @UiThread
        fun requestStarted(req: AwfulRequest<*>)

        /** Called when the request is announcing a progress [percent]age update */
        @UiThread
        fun requestUpdate(req: AwfulRequest<*>, percent: Int)

        /** Called when the request has finished, with a possible [error] if it failed */
        @UiThread
        fun requestEnded(req: AwfulRequest<*>, error: VolleyError?)
    }

    companion object {
        /** Used for identifying request types when cancelling, reassign this in subclasses */
        val REQUEST_TAG = Any()

        private val lenientRetryPolicy = DefaultRetryPolicy(20000, 1, 1f)
    }
}
