package com.ferg.awfulapp.task

import android.content.SharedPreferences
import android.util.Log
import androidx.collection.ArrayMap
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.ferg.awfulapp.AwfulApplication.Companion.appStatePrefs
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.preferences.AwfulPreferences.Companion.getInstance
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import kotlin.time.Duration.Companion.days
import androidx.core.content.edit

/**
 * Created by baka kaba on 31/05/2017.
 * 
 * 
 * Handles requests to upload images to the Imgur API, and tracks quotas.
 * 
 * 
 * We handle two kinds of uploads - URLs (basically a link to an image hosted elsewhere), and an
 * actual file upload from the user's device. File uploads take an InputStream, which lets you use
 * the OS's content picker functionality to upload from sources like cloud providers instead of just
 * local files. The response JSON is returned, see the API docs for what this contains.
 * 
 * 
 * Imgur operates a credit system, so we need to keep track of the user and app limits and prevent
 * uploads when they're hit. Unfortunately we can't request this information (without spending credits)
 * so we need to store the latest data from the last response, and expire that when it becomes stale.
 * This isn't perfect (e.g. the credits for the entire app will probably have changed no matter what)
 * but it's a compromise we need to make.
 * 
 * 
 * Imgur doesn't allow many credits on the free tier - currently 12,500, enough for 1,250 upload attempts.
 * There's rate limiting for each user, but it's 500 credits per hour, so the daily app limit is only 25x
 * that. The user limit is implemented (in case it changes and starts to affect things), but we also have
 * our own daily user quota which will cut people off way earlier. This is to make sure a handful of users
 * can't spend all the app credits - the actual limit will probably need tweaking.
 * 
 * 
 * Also hitting the app credit limit too many times in a month will get us kicked off for the rest
 * of the month, so there's a buffer setting which effectively lowers the credit limit to block uploads
 * early. Unfortunately this only works when we know the current app credits, and if we don't have that
 * data (because we haven't uploaded in the current period) then we have to blindly attempt the upload
 * and hope we have credits. If the buffer isn't big enough and we end up with too many blind requests
 * pushing us over the limit, then this will need to be changed - maybe by making a simple GET to get
 * the current value when we don't have data. Slightly complicates things, but it might need to be done.
 * 
 * 
 * The [.getCurrentUploadLimit] method attempts to provide the currently applicable limit
 * (i.e. the lowest one) and the time when that limit will reset. Because there are multiple limits
 * (some not directly affected by the user) this could be a bit confusing, with allowed uploads
 * decreasing between uses (if the app limit is close to being hit), 'resets' giving inconsistent
 * numbers (if another limit is now lower and taking precedence), etc. It might actually be better
 * to not give the user any indication of how many uploads they can perform if it's too wild, but
 * I think it's good to at least have an idea of what's going on, especially if you want to write a
 * post and have a few things to include.
 * 
 * @see [https://apidocs.imgur.com](https://apidocs.imgur.com)
 */
class ImgurUploadRequest private constructor(
    isFile: Boolean,
    private val jsonResponseListener: Response.Listener<JSONObject?>,
    errorListener: Response.ErrorListener?
) : Request<JSONObject?>(
    Method.POST, IMGUR_ENDPOINT_URL, errorListener
) {
    private val attachParams: MultipartEntityBuilder = MultipartEntityBuilder.create()
    private var httpEntity: HttpEntity? = null


    /////////////////////////////////////////////////////////////////////////
    // Constructors
    /////////////////////////////////////////////////////////////////////////
    init {
        retryPolicy = DefaultRetryPolicy(20000, 1, 1f)
        attachParams.addPart(
            "type",
            StringBody(if (isFile) "file" else "URL", ContentType.TEXT_PLAIN)
        )
    }


    /**
     * Upload an image to Imgur as data via an InputStream.
     * 
     * @param imageStream          the image data
     * @param jsonResponseListener receives the response data from Imgur
     */
    constructor(
        imageStream: InputStream,
        jsonResponseListener: Response.Listener<JSONObject?>,
        errorListener: Response.ErrorListener?
    ) : this(true, jsonResponseListener, errorListener) {
        attachParams.addBinaryBody("image", imageStream, ContentType.DEFAULT_BINARY, "Awful image")
        httpEntity = attachParams.build()
    }


    /**
     * Host an existing online image on Imgur by passing its URL.
     * 
     * @param sourceUrl            the URL of the online image
     * @param jsonResponseListener receives the response data from Imgur
     */
    constructor(
        sourceUrl: String,
        jsonResponseListener: Response.Listener<JSONObject?>,
        errorListener: Response.ErrorListener?
    ) : this(false, jsonResponseListener, errorListener) {
        attachParams.addPart("image", StringBody(sourceUrl, ContentType.TEXT_PLAIN))
        httpEntity = attachParams.build()
    }


    /**//////////////////////////////////////////////////////////////////////// */ // Request data
    /**//////////////////////////////////////////////////////////////////////// */
    @Throws(AuthFailureError::class)
    override fun getHeaders(): MutableMap<String?, String?> {
        val bearer = getInstance().imgurAccountToken
        val headers: MutableMap<String?, String?> = ArrayMap(1)
        headers["Authorization"] = "Bearer $bearer"
        return headers
    }


    override fun getBodyContentType(): String? {
        return httpEntity!!.contentType.value
    }


    @Throws(AuthFailureError::class)
    override fun getBody(): ByteArray? {
        try {
            val bytes = ByteArrayOutputStream()
            httpEntity!!.writeTo(bytes)
            return bytes.toByteArray()
        } catch (ioe: IOException) {
            Log.e(TAG, "Failed to convert body ByteStream")
        }
        return super.getBody()
    }


    /**//////////////////////////////////////////////////////////////////////// */ // Response handling
    /**//////////////////////////////////////////////////////////////////////// */
    override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject?>? {
        updateCredits(response)
        try {
            val json = String(
                response.data,
                charset(HttpHeaderParser.parseCharset(response.headers))
            )
            return Response.success(
                JSONObject(json),
                HttpHeaderParser.parseCacheHeaders(response)
            )
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }


    override fun parseNetworkError(volleyError: VolleyError): VolleyError? {
        updateCredits(volleyError.networkResponse)
        return super.parseNetworkError(volleyError)
    }


    override fun deliverResponse(response: JSONObject?) {
        jsonResponseListener.onResponse(response)
    }


    companion object {
        val TAG: String = ImgurUploadRequest::class.java.simpleName

        // keys for persisted data on last-known limits and when they're guaranteed to expire
        private val KEY_USER_CREDITS: String = "$TAG.user_credits"
        private val KEY_USER_CREDITS_EXPIRY: String = "$TAG.user_credits_expiry"

        private val KEY_APP_CREDITS: String = "$TAG.app_credits"
        private val KEY_APP_CREDITS_EXPIRY: String = "$TAG.app_credits_expiry"

        private val KEY_USER_QUOTA_REMAINING: String = "$TAG.user_quota_remaining"
        private val KEY_USER_QUOTA_EXPIRY: String = "$TAG.user_quota_expiry"

        /**
         * The cost of an upload according to the Imgur API
         */
        private const val CREDITS_PER_UPLOAD = 10

        /**
         * The (approximate) frequency of app credit resets - used to invalidate old data
         */
        private val APP_CREDIT_LIMIT_PERIOD = 1.days.inWholeMilliseconds

        /**
         * Used to lower the app credit limit, to avoid hitting the full limit (and getting the app banned)
         */
        private val APP_CREDIT_BUFFER = if (Constants.DEBUG) 500 else 1000 // dev privilege

        /**
         * Our own quota period, we reset daily
         */
        private val USER_CREDIT_QUOTA_PERIOD = 1.days.inWholeMilliseconds

        /**
         * Our own per-user daily upload quota, to stop a handful of users spending all the app's credits
         */
        private val USER_CREDIT_QUOTA_MAX =
            if (Constants.DEBUG) Int.MAX_VALUE else 500 // dev privilege

        private const val IMGUR_ENDPOINT_URL = "https://api.imgur.com/3/image"

        /////////////////////////////////////////////////////////////////////////
        // Upload credits
        /////////////////////////////////////////////////////////////////////////
        /**
        * Get the best estimate of the remaining number of uploads the user can perform, and when that limit resets.
        *
        *
        * Imgur sets limits on the number of basic API requests and actual uploads each app and user
        * can do. In addition, this app also limits user uploads to ration the overall app limit, and
        * prevent individual users from overdoing it at the expense of others. This method provides
        * the best guess at how many full uploads the user **can** perform, and a timestamp of when
        * that limit will be reset.
        *
        *
        * Because API requests cost credits, we don't actually ask the server how many credits
        * are remaining for the app and the user - upload responses provide this info, and we store that
        * when the user attempts an upload. If we don't have current data for these limits (e.g. the
        * most recent data is stale) then we can't draw any meaningful conclusions about the current
        * situation - in this case the method returns **null** for the count and timestamp.
        *
        *
        * If we have this data , then we return the lowest limit in place, along with the timestamp for
        * when we expect this limit to change (which may be null if we can't estimate that, e.g. the app
        * credit limit resets at an unknown time, but apparently within a day). This is complicated by
        * the fact that another limit might drop below this one - say if other users drain all the app
        * credits - and cause a new reset time to apply, or the reset might happen (e.g. your personal
        * quota is restored) but now another limit is lower (the total app limit), so the reset doesn't
        * seem to have applied properly.
        *
        *
        * Basically this is complicated with multiple restrictions in place, some happening at a distance
        * and affected by other users, and we're having to walk around in the dark trying not to touch
        * the API too much. So use this in an advisory capacity only!
        *
        * @ return an upload count / reset timestamp pair, both potentially null if we don't have that data
        */
        @JvmStatic
        val currentUploadLimit: Pair<Int?, Long?>
            get() {
                val appStatePrefs : SharedPreferences = appStatePrefs!!
                val now = System.currentTimeMillis()

                val appCreditsExpiry = appStatePrefs.getLong(KEY_APP_CREDITS_EXPIRY, -1)
                val appCredits = if (appCreditsExpiry < now) null else appStatePrefs.getInt(
                    KEY_APP_CREDITS,
                    0
                )

                val userCreditsExpiry =
                    appStatePrefs.getLong(KEY_USER_CREDITS_EXPIRY, -1)
                val userCredits = if (userCreditsExpiry < now) null else appStatePrefs.getInt(
                    KEY_USER_CREDITS,
                    0
                )

                // if either of these credits values are null (i.e. no current data) then we can't give any meaningful estimates
                val realAppCredits = appCredits ?: return Pair(null, null)
                val realUserCredits = userCredits ?: return Pair(null, null)

                // the quota is the in-app limit on a user's uploads, since we (currently) only get 1250 pics' worth TOTAL per day
                // we manage and reset this ourselves, so it's the only count we can be absolutely sure about
                val userQuotaExpiry =
                    appStatePrefs.getLong(KEY_USER_QUOTA_EXPIRY, -1)
                val userQuotaRemaining =
                    if (userQuotaExpiry < now) USER_CREDIT_QUOTA_MAX else appStatePrefs.getInt(
                        KEY_USER_QUOTA_REMAINING,
                        USER_CREDIT_QUOTA_MAX
                    )

                // return the minimum upload limit, and the time it expires (if appropriate)
                if (realAppCredits < realUserCredits && realAppCredits < userQuotaRemaining) {
                    // we don't know exactly when the app's credits will be reset, so don't pass a timestamp
                    return Pair(
                        realAppCredits / CREDITS_PER_UPLOAD,
                        null
                    )
                } else if (realUserCredits < userQuotaRemaining) {
                    return Pair(
                        realUserCredits / CREDITS_PER_UPLOAD,
                        userCreditsExpiry
                    )
                } else {
                    return Pair(
                        userQuotaRemaining / CREDITS_PER_UPLOAD,
                        userQuotaExpiry
                    )
                }
            }


        /**
         * Parse the request response and extract the current API credits data.
         * 
         * @param response the response returned by the Imgur API
         */
        private fun updateCredits(response: NetworkResponse) {
            // every attempt (successful or not) costs credits!
            subtractUploadFromQuota()

            try {
                val userUploadCredits = response.headers?.get("X-RateLimit-UserRemaining")?.toInt() ?: 0
                val appUploadCredits = response.headers?.get("X-RateLimit-ClientRemaining")?.toInt() ?: 0
                val userCreditResetTimestamp = response.headers?.get("X-RateLimit-UserReset")?.toLong()?.times(1000L) ?: 0 // API timestamp is in seconds
                // only update the prefs when we've successfully parsed everything
                appStatePrefs?.edit {
                    putInt(KEY_USER_CREDITS, userUploadCredits)
                        .putLong(KEY_USER_CREDITS_EXPIRY, userCreditResetTimestamp)
                        .putInt(
                            KEY_APP_CREDITS,
                            appUploadCredits - APP_CREDIT_BUFFER
                        ) // record a lower number of total credits to provide some safety
                        .putLong(
                            KEY_APP_CREDITS_EXPIRY,
                            System.currentTimeMillis() + APP_CREDIT_LIMIT_PERIOD
                        )
                }
            } catch (e: NumberFormatException) {
                // TODO: 05/06/2017 failed to update something - block uploads/checks for a while?
                Log.w(TAG, "updateCredits: failed to parse response!", e)
            } catch (e: NullPointerException) {
                Log.w(TAG, "updateCredits: failed to parse response!", e)
            }
        }


        /**
         * Remove one upload's worth of credits from the current user quota.
         * 
         * 
         * This will reset the quota if it has expired (the reset window has passed) before subtracting,
         * restoring the quota to its maximum and updating the expiry timestamp relative to now.
         */
        private fun subtractUploadFromQuota() {
            appStatePrefs?.let {
                val now = System.currentTimeMillis()
                val currentQuota = it.getInt(KEY_USER_QUOTA_REMAINING, USER_CREDIT_QUOTA_MAX)
                val quotaExpiryTime = it.getLong(KEY_USER_QUOTA_EXPIRY, -1)

                it.edit {
                    if (quotaExpiryTime < now) {
                        // quota has expired, reset it and set the new expiry time
                        putInt(KEY_USER_QUOTA_REMAINING, USER_CREDIT_QUOTA_MAX - CREDITS_PER_UPLOAD)
                        putLong(KEY_USER_QUOTA_EXPIRY, now + USER_CREDIT_QUOTA_PERIOD)
                    } else {
                        putInt(KEY_USER_QUOTA_REMAINING, currentQuota - CREDITS_PER_UPLOAD)
                    }
                }
            }
        }
    }
}
