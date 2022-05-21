package com.ferg.awfulapp.task;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import android.util.Log;
import android.util.Pair;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.ferg.awfulapp.AwfulApplication;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by baka kaba on 31/05/2017.
 * <p>
 * Handles requests to upload images to the Imgur API, and tracks quotas.
 * <p>
 * We handle two kinds of uploads - URLs (basically a link to an image hosted elsewhere), and an
 * actual file upload from the user's device. File uploads take an InputStream, which lets you use
 * the OS's content picker functionality to upload from sources like cloud providers instead of just
 * local files. The response JSON is returned, see the API docs for what this contains.
 * <p>
 * Imgur operates a credit system, so we need to keep track of the user and app limits and prevent
 * uploads when they're hit. Unfortunately we can't request this information (without spending credits)
 * so we need to store the latest data from the last response, and expire that when it becomes stale.
 * This isn't perfect (e.g. the credits for the entire app will probably have changed no matter what)
 * but it's a compromise we need to make.
 * <p>
 * Imgur doesn't allow many credits on the free tier - currently 12,500, enough for 1,250 upload attempts.
 * There's rate limiting for each user, but it's 500 credits per hour, so the daily app limit is only 25x
 * that. The user limit is implemented (in case it changes and starts to affect things), but we also have
 * our own daily user quota which will cut people off way earlier. This is to make sure a handful of users
 * can't spend all the app credits - the actual limit will probably need tweaking.
 * <p>
 * Also hitting the app credit limit too many times in a month will get us kicked off for the rest
 * of the month, so there's a buffer setting which effectively lowers the credit limit to block uploads
 * early. Unfortunately this only works when we know the current app credits, and if we don't have that
 * data (because we haven't uploaded in the current period) then we have to blindly attempt the upload
 * and hope we have credits. If the buffer isn't big enough and we end up with too many blind requests
 * pushing us over the limit, then this will need to be changed - maybe by making a simple GET to get
 * the current value when we don't have data. Slightly complicates things, but it might need to be done.
 * <p>
 * The {@link #getCurrentUploadLimit()} method attempts to provide the currently applicable limit
 * (i.e. the lowest one) and the time when that limit will reset. Because there are multiple limits
 * (some not directly affected by the user) this could be a bit confusing, with allowed uploads
 * decreasing between uses (if the app limit is close to being hit), 'resets' giving inconsistent
 * numbers (if another limit is now lower and taking precedence), etc. It might actually be better
 * to not give the user any indication of how many uploads they can perform if it's too wild, but
 * I think it's good to at least have an idea of what's going on, especially if you want to write a
 * post and have a few things to include.
 *
 * @see <a href="https://apidocs.imgur.com">https://apidocs.imgur.com</a>
 */
public class ImgurUploadRequest extends Request<JSONObject> {

    public static final String TAG = ImgurUploadRequest.class.getSimpleName();

    // keys for persisted data on last-known limits and when they're guaranteed to expire
    private static final String KEY_USER_CREDITS = TAG + ".user_credits";
    private static final String KEY_USER_CREDITS_EXPIRY = TAG + ".user_credits_expiry";

    private static final String KEY_APP_CREDITS = TAG + ".app_credits";
    private static final String KEY_APP_CREDITS_EXPIRY = TAG + ".app_credits_expiry";

    private static final String KEY_USER_QUOTA_REMAINING = TAG + ".user_quota_remaining";
    private static final String KEY_USER_QUOTA_EXPIRY = TAG + ".user_quota_expiry";

    /**
     * The cost of an upload according to the Imgur API
     */
    private static final int CREDITS_PER_UPLOAD = 10;
    /**
     * The (approximate) frequency of app credit resets - used to invalidate old data
     */
    private static final long APP_CREDIT_LIMIT_PERIOD = TimeUnit.DAYS.toMillis(1);
    /**
     * Used to lower the app credit limit, to avoid hitting the full limit (and getting the app banned)
     */
    private static final int APP_CREDIT_BUFFER = Constants.DEBUG ? 500 : 1000; // dev privilege

    /**
     * Our own quota period, we reset daily
     */
    private static final long USER_CREDIT_QUOTA_PERIOD = TimeUnit.DAYS.toMillis(1);
    /**
     * Our own per-user daily upload quota, to stop a handful of users spending all the app's credits
     */
    private static final int USER_CREDIT_QUOTA_MAX = Constants.DEBUG ? Integer.MAX_VALUE : 500; // dev privilege

    private static final String IMGUR_ENDPOINT_URL = "https://api.imgur.com/3/image";

    private final MultipartEntityBuilder attachParams = MultipartEntityBuilder.create();
    private final Response.Listener<JSONObject> jsonResponseListener;
    private HttpEntity httpEntity;


    ///////////////////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Create an upload request with its basic parameters.
     *
     * @param isFile true if we're uploading file data, false if we're providing a URL
     */
    private ImgurUploadRequest(boolean isFile,
                               @NonNull Response.Listener<JSONObject> jsonResponseListener,
                               @Nullable Response.ErrorListener errorListener) {
        super(Method.POST, IMGUR_ENDPOINT_URL, errorListener);
        this.jsonResponseListener = jsonResponseListener;
        setRetryPolicy(new DefaultRetryPolicy(20000, 1, 1));
        attachParams.addPart("type", new StringBody(isFile ? "file" : "URL", ContentType.TEXT_PLAIN));
    }


    /**
     * Upload an image to Imgur as data via an InputStream.
     *
     * @param imageStream          the image data
     * @param jsonResponseListener receives the response data from Imgur
     */
    public ImgurUploadRequest(@NonNull InputStream imageStream,
                              @NonNull Response.Listener<JSONObject> jsonResponseListener,
                              @Nullable Response.ErrorListener errorListener) {
        this(true, jsonResponseListener, errorListener);
        attachParams.addBinaryBody("image", imageStream);
        httpEntity = attachParams.build();
    }


    /**
     * Host an existing online image on Imgur by passing its URL.
     *
     * @param sourceUrl            the URL of the online image
     * @param jsonResponseListener receives the response data from Imgur
     */
    public ImgurUploadRequest(@NonNull String sourceUrl,
                              @NonNull Response.Listener<JSONObject> jsonResponseListener,
                              @Nullable Response.ErrorListener errorListener) {
        this(false, jsonResponseListener, errorListener);
        attachParams.addPart("image", new StringBody(sourceUrl, ContentType.TEXT_PLAIN));
        httpEntity = attachParams.build();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Request data
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        String clientId = AwfulPreferences.getInstance().getResources().getString(R.string.imgur_api_client_id);
        Map<String, String> headers = new ArrayMap<>(1);
        headers.put("Authorization", "Client-ID " + clientId);
        return headers;
    }


    @Override
    public String getBodyContentType() {
        return httpEntity.getContentType().getValue();
    }


    @Override
    public byte[] getBody() throws AuthFailureError {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            httpEntity.writeTo(bytes);
            return bytes.toByteArray();
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to convert body ByteStream");
        }
        return super.getBody();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Response handling
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        updateCredits(response);
        try {
            String json = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            return Response.success(new JSONObject(json), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    protected VolleyError parseNetworkError(VolleyError volleyError) {
        updateCredits(volleyError.networkResponse);
        return super.parseNetworkError(volleyError);
    }


    @Override
    protected void deliverResponse(JSONObject response) {
        jsonResponseListener.onResponse(response);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Upload credits
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Get the best estimate of the remaining number of uploads the user can perform, and when that limit resets.
     * <p>
     * Imgur sets limits on the number of basic API requests and actual uploads each app and user
     * can do. In addition, this app also limits user uploads to ration the overall app limit, and
     * prevent individual users from overdoing it at the expense of others. This method provides
     * the best guess at how many full uploads the user <b>can</b> perform, and a timestamp of when
     * that limit will be reset.
     * <p>
     * Because API requests cost credits, we don't actually ask the server how many credits
     * are remaining for the app and the user - upload responses provide this info, and we store that
     * when the user attempts an upload. If we don't have current data for these limits (e.g. the
     * most recent data is stale) then we can't draw any meaningful conclusions about the current
     * situation - in this case the method returns <b>null</b> for the count and timestamp.
     * <p>
     * If we have this data, then we return the lowest limit in place, along with the timestamp for
     * when we expect this limit to change (which may be null if we can't estimate that, e.g. the app
     * credit limit resets at an unknown time, but apparently within a day). This is complicated by
     * the fact that another limit might drop below this one - say if other users drain all the app
     * credits - and cause a new reset time to apply, or the reset might happen (e.g. your personal
     * quota is restored) but now another limit is lower (the total app limit), so the reset doesn't
     * seem to have applied properly.
     * <p>
     * Basically this is complicated with multiple restrictions in place, some happening at a distance
     * and affected by other users, and we're having to walk around in the dark trying not to touch
     * the API too much. So use this in an advisory capacity only!
     *
     * @return an upload count / reset timestamp pair, both potentially null if we don't have that data
     */
    @NonNull
    public static Pair<Integer, Long> getCurrentUploadLimit() {
        SharedPreferences appStatePrefs = AwfulApplication.getAppStatePrefs();
        long now = System.currentTimeMillis();

        long appCreditsExpiry = appStatePrefs.getLong(KEY_APP_CREDITS_EXPIRY, -1);
        Integer appCredits = appCreditsExpiry < now ? null : appStatePrefs.getInt(KEY_APP_CREDITS, 0);

        long userCreditsExpiry = appStatePrefs.getLong(KEY_USER_CREDITS_EXPIRY, -1);
        Integer userCredits = userCreditsExpiry < now ? null : appStatePrefs.getInt(KEY_USER_CREDITS, 0);

        // if either of these credits values are null (i.e. no current data) then we can't give any meaningful estimates
        if (appCredits == null || userCredits == null) {
            return new Pair<>(null, null);
        }

        // the quota is the in-app limit on a user's uploads, since we (currently) only get 1250 pics' worth TOTAL per day
        // we manage and reset this ourselves, so it's the only count we can be absolutely sure about
        long userQuotaExpiry = appStatePrefs.getLong(KEY_USER_QUOTA_EXPIRY, -1);
        int userQuotaRemaining = userQuotaExpiry < now ? USER_CREDIT_QUOTA_MAX : appStatePrefs.getInt(KEY_USER_QUOTA_REMAINING, USER_CREDIT_QUOTA_MAX);

        // return the minimum upload limit, and the time it expires (if appropriate)
        if (appCredits < userCredits && appCredits < userQuotaRemaining) {
            // we don't know exactly when the app's credits will be reset, so don't pass a timestamp
            return new Pair<>(appCredits / CREDITS_PER_UPLOAD, null);
        } else if (userCredits < userQuotaRemaining) {
            return new Pair<>(userCredits / CREDITS_PER_UPLOAD, userCreditsExpiry);
        } else {
            return new Pair<>(userQuotaRemaining / CREDITS_PER_UPLOAD, userQuotaExpiry);
        }
    }


    /**
     * Parse the request response and extract the current API credits data.
     *
     * @param response the response returned by the Imgur API
     */
    private static void updateCredits(@Nullable NetworkResponse response) {
        // every attempt (successful or not) costs credits!
        subtractUploadFromQuota();

        if (response == null) {
            return;
        }
        try {
            int userUploadCredits = Integer.parseInt(response.headers.get("X-RateLimit-UserRemaining"));
            int appUploadCredits = Integer.parseInt(response.headers.get("X-RateLimit-ClientRemaining"));
            long userCreditResetTimestamp = Long.parseLong(response.headers.get("X-RateLimit-UserReset")) * 1000L; // API timestamp is in seconds
            // only update the prefs when we've successfully parsed everything
            AwfulApplication.getAppStatePrefs().edit()
                    .putInt(KEY_USER_CREDITS, userUploadCredits)
                    .putLong(KEY_USER_CREDITS_EXPIRY, userCreditResetTimestamp)
                    .putInt(KEY_APP_CREDITS, appUploadCredits - APP_CREDIT_BUFFER) // record a lower number of total credits to provide some safety
                    .putLong(KEY_APP_CREDITS_EXPIRY, System.currentTimeMillis() + APP_CREDIT_LIMIT_PERIOD)
                    .apply();
        } catch (NumberFormatException | NullPointerException e) {
            // TODO: 05/06/2017 failed to update something - block uploads/checks for a while?
            Log.w(TAG, "updateCredits: failed to parse response!", e);
        }
    }


    /**
     * Remove one upload's worth of credits from the current user quota.
     * <p>
     * This will reset the quota if it has expired (the reset window has passed) before subtracting,
     * restoring the quota to its maximum and updating the expiry timestamp relative to now.
     */
    private static void subtractUploadFromQuota() {
        SharedPreferences appStatePrefs = AwfulApplication.getAppStatePrefs();
        long now = System.currentTimeMillis();
        int currentQuota = appStatePrefs.getInt(KEY_USER_QUOTA_REMAINING, USER_CREDIT_QUOTA_MAX);
        long quotaExpiryTime = appStatePrefs.getLong(KEY_USER_QUOTA_EXPIRY, -1);

        SharedPreferences.Editor editor = appStatePrefs.edit();
        if (quotaExpiryTime < now) {
            // quota has expired, reset it and set the new expiry time
            editor.putInt(KEY_USER_QUOTA_REMAINING, USER_CREDIT_QUOTA_MAX - CREDITS_PER_UPLOAD);
            editor.putLong(KEY_USER_QUOTA_EXPIRY, now + USER_CREDIT_QUOTA_PERIOD);
        } else {
            editor.putInt(KEY_USER_QUOTA_REMAINING, currentQuota - CREDITS_PER_UPLOAD);
        }
        editor.apply();
    }


}
