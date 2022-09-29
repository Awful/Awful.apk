package com.ferg.awfulapp.util;

import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.animation.Animation;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.Keys;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * AwfulError
 * This is an error class that encompasses all the predictable error states we will encounter from SA server responses.
 * This currently covers Logged out, some site down messages, and probation status.
 * <p>
 * Page responses from the site should be passed through {@link #checkPageErrors(Document, AwfulPreferences)}, which
 * performs some standard checks on the HTML and works out if a bad thing has happened. That way handlers for the various
 * requests we make can focus on looking for specific problems.
 */
public class AwfulError extends VolleyError {

    public static final int ERROR_LOGGED_OUT = 0x00000001;
    public static final int ERROR_FORUM_CLOSED = 0x00000002;
    public static final int ERROR_PROBATION = 0x00000004;
    public static final int ERROR_GENERIC_FAILURE = 0x00000008;
    public static final int ERROR_ACCESS_DENIED = 0x00000010;

    private static final Pattern PROBATION_MESSAGE_REGEX = Pattern.compile("(.*)until\\s(([\\s\\w:,])+).\\sYou(.*)");

    private final int errorCode;
    @Nullable
    private final String errorMessage;


    public AwfulError() {
        this(ERROR_GENERIC_FAILURE, null);
    }

    public AwfulError(int code) {
        this(code, null);
    }

    public AwfulError(@Nullable String message) {
        this(AwfulError.ERROR_GENERIC_FAILURE, message);
    }

    public AwfulError(int code, @Nullable String message) {
        errorCode = code;
        errorMessage = message;
    }

    /**
     * If a custom message is registered with a code, it will be returned here.
     * If no custom message is provided, a generic message for that error type is provided.
     *
     * @return A user-friendly error message.
     */
    @Nullable
    @Override
    public String getMessage() {
        if (!TextUtils.isEmpty(errorMessage)) {
            return errorMessage;
        }
        switch (errorCode) {
            case ERROR_LOGGED_OUT:
                return "Error - Not Logged In";
            case ERROR_FORUM_CLOSED:
                return "Error - Forums Closed (Site Down)";
            case ERROR_PROBATION:
                return "You are under probation.";
            case ERROR_GENERIC_FAILURE:
                return "Failed to load!";
            case ERROR_ACCESS_DENIED:
                return "Access denied.";
        }
        return null;
    }


    @Nullable
    public String getSubMessage() {
        switch (errorCode) {
            case ERROR_GENERIC_FAILURE:
                return "Check your network connection and try again.";
        }
        return null;
    }

    public int getIconResource() {
        return R.drawable.ic_error;
    }

    @Nullable
    public Animation getIconAnimation() {
        return null;
    }

    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Quick check to see if this type of error is typically unrecoverable.
     * Short-cut for handleError() callback in AwfulRequest.
     *
     * @return true if this error type is normally unrecoverable and we should skip processing the response.
     */
    public boolean isCritical() {
        return errorCode != ERROR_PROBATION;
    }


    /**
     * Checks a page for forum errors.
     * Detects forum closures, logged-out state, and banned/probate status.
     * Automatically used in AwfulRequest handling process, see AwfulRequest.handleError for more.
     * (Method moved from AwfulPagedItem)
     *
     * @param page  Full HTML page to check.
     * @param prefs An AwfulPreference object to reference or update preferences.
     * @return AwfulError object if an error is detected, null otherwise.
     */
    @SuppressWarnings("SpellCheckingInspection")
    public static AwfulError checkPageErrors(Document page, AwfulPreferences prefs) {
        // not logged in
        if (null != page.getElementById("notregistered")) {
            Timber.w("!!!Page says not registered - You are now LOGGED OUT");
            return new AwfulError(ERROR_LOGGED_OUT);
        }

        // closed forums
        if (null != page.getElementById("closemsg")) {
            String reason = page.getElementsByClass("reason").text();
            String message = TextUtils.isEmpty(reason) ? null : "Forums Closed - " + reason;
            return new AwfulError(ERROR_FORUM_CLOSED, message);
        }

        // Some generic error - shows up for (at least) post rate limiting and whatever #PostRequest was seeing in responses
        if (page.selectFirst("body").hasClass("standarderror")) {
            Element standard = page.selectFirst(".standard");
            if (standard != null && standard.hasText()) {
                return new AwfulError(AwfulError.ERROR_ACCESS_DENIED, standard.text().replace("Special Message From Senor Lowtax", ""));
            }
        }

        // handle probation status by looking for the probation message (or lack of it)
        Element probation = page.getElementById("probation_warn");
        if (probation == null) {
            // clear any probation
            prefs.setPreference(Keys.PROBATION_TIME, 0L);
        } else {
            // try to get the user ID (for the link to the Leper's Colony)
            Element userLink = probation.getElementsByTag("a").first();
            if (userLink != null) {
                String userId = StringUtils.substringAfterLast(userLink.attr("href"), "=");
                prefs.setPreference(Keys.USER_ID, Integer.parseInt(userId));
            }

            // try to parse the probation date - default to 1 day in case we can't parse it (not too scary)
            long probTimestamp = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
            Matcher m = PROBATION_MESSAGE_REGEX.matcher(probation.text());
            if (m.find()) {
                String date = m.group(2);

                // Jan 11, 2013 10:35 AM  vs  Jan 11, 2013 22:35
                String pattern = StringUtils.endsWithIgnoreCase(date, "m") ? "MMM d, yyyy hh:mm a" : "MMM d, yyyy HH:mm";
                SimpleDateFormat probationFormat = new SimpleDateFormat(pattern, Locale.US);
                try {
                    probTimestamp = probationFormat.parse(date).getTime();
                } catch (ParseException e) {
                    Timber.w(e, "checkPageErrors: couldn't parse probation date text: %s", date);
                }
            } else {
                Timber.w("checkPageErrors: couldn't find expected probation date text!\nFull text: %s", probation.text());
            }

            prefs.setPreference(Keys.PROBATION_TIME, probTimestamp);
            return new AwfulError(ERROR_PROBATION);
        }
        return null;
    }

}
