package com.ferg.awfulapp.util

import android.text.TextUtils
import android.view.animation.Animation
import com.android.volley.VolleyError
import com.ferg.awfulapp.R
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.IntPreference
import com.ferg.awfulapp.preferences.LongPreference
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Document
import timber.log.Timber.Forest.w
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * AwfulError
 * This is an error class that encompasses all the predictable error states we will encounter from SA server responses.
 * This currently covers Logged out, some site down messages, and probation status.
 * 
 * 
 * Page responses from the site should be passed through [.checkPageErrors], which
 * performs some standard checks on the HTML and works out if a bad thing has happened. That way handlers for the various
 * requests we make can focus on looking for specific problems.
 */
class AwfulError @JvmOverloads constructor(
    val errorCode: Int = ERROR_GENERIC_FAILURE,
    private val errorMessage: String? = null
) : VolleyError() {
    constructor(message: String?) : this(ERROR_GENERIC_FAILURE, message)

    override val message: String?
        /**
         * If a custom message is registered with a code, it will be returned here.
         * If no custom message is provided, a generic message for that error type is provided.
         * 
         * @return A user-friendly error message.
         */
        get() {
            if (!TextUtils.isEmpty(errorMessage)) {
                return errorMessage
            }
            when (errorCode) {
                ERROR_LOGGED_OUT -> return "Error - Not Logged In"
                ERROR_FORUM_CLOSED -> return "Error - Forums Closed (Site Down)"
                ERROR_PROBATION -> return "You are under probation."
                ERROR_GENERIC_FAILURE -> return "Failed to load!"
                ERROR_ACCESS_DENIED -> return "Access denied."
            }
            return null
        }


    val subMessage: String?
        get() {
            when (errorCode) {
                ERROR_GENERIC_FAILURE -> return "Check your network connection and try again."
            }
            return null
        }

    val iconResource: Int
        get() = R.drawable.ic_error

    val iconAnimation: Animation?
        get() = null

    val isCritical: Boolean
        /**
         * Quick check to see if this type of error is typically unrecoverable.
         * Short-cut for handleError() callback in AwfulRequest.
         * 
         * @return true if this error type is normally unrecoverable and we should skip processing the response.
         */
        get() = errorCode != ERROR_PROBATION


    companion object {
        const val ERROR_LOGGED_OUT: Int = 0x00000001
        const val ERROR_FORUM_CLOSED: Int = 0x00000002
        const val ERROR_PROBATION: Int = 0x00000004
        const val ERROR_GENERIC_FAILURE: Int = 0x00000008
        const val ERROR_ACCESS_DENIED: Int = 0x00000010

        private val PROBATION_MESSAGE_REGEX: Pattern =
            Pattern.compile("(.*)until\\s(([\\s\\w:,])+).\\sYou(.*)")

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
        @Suppress("SpellCheckingInspection")
        fun checkPageErrors(page: Document, prefs: AwfulPreferences): AwfulError? {

            // Forums are _down_ down
            if (page.getElementsByTag("title").text() == "Something Awful Forum Status") {
                val reason = page.getElementsByClass("info").text()
                val message = if (TextUtils.isEmpty(reason)) null else "Forums Closed - $reason"
                return AwfulError(ERROR_FORUM_CLOSED, message)
            }

            // not logged in
            if (null != page.getElementById("notregistered")) {
                w("!!!Page says not registered - You are now LOGGED OUT")
                return AwfulError(ERROR_LOGGED_OUT)
            }

            // closed forums
            if (null != page.getElementById("closemsg")) {
                val reason = page.getElementsByClass("reason").text()
                val message = if (TextUtils.isEmpty(reason)) null else "Forums Closed - $reason"
                return AwfulError(ERROR_FORUM_CLOSED, message)
            }

            // Some generic error - shows up for (at least) post rate limiting and whatever #PostRequest was seeing in responses
            if (page.selectFirst("body")!!.hasClass("standarderror")) {
                val standard = page.selectFirst(".standard")
                if (standard != null && standard.hasText()) {
                    return AwfulError(
                        ERROR_ACCESS_DENIED,
                        standard.text().replace("Special Message From Senor Lowtax", "")
                    )
                }
            }

            // handle probation status by looking for the probation message (or lack of it)
            val probation = page.getElementById("probation_warn")
            if (probation == null) {
                // clear any probation
                prefs.setPreference(LongPreference.PROBATION_TIME, 0L)
            } else {
                // try to get the user ID (for the link to the Leper's Colony)
                val userLink = probation.getElementsByTag("a").first()
                if (userLink != null) {
                    val userId = StringUtils.substringAfterLast(userLink.attr("href"), "=")
                    prefs.setPreference(IntPreference.USER_ID, userId.toInt())
                }

                // try to parse the probation date - default to 1 day in case we can't parse it (not too scary)
                var probTimestamp = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
                val m: Matcher = PROBATION_MESSAGE_REGEX.matcher(probation.text())
                if (m.find()) {
                    val date = m.group(2)!!

                    // Jan 11, 2013 10:35 AM  vs  Jan 11, 2013 22:35
                    val pattern = if (StringUtils.endsWithIgnoreCase(
                            date,
                            "m"
                        )
                    ) "MMM d, yyyy hh:mm a" else "MMM d, yyyy HH:mm"
                    val probationFormat = SimpleDateFormat(pattern, Locale.US)
                    try {
                        probTimestamp = probationFormat.parse(date)?.time!!
                    } catch (e: ParseException) {
                        w(e, "checkPageErrors: couldn't parse probation date text: %s", date)
                    }
                } else {
                    w(
                        "checkPageErrors: couldn't find expected probation date text!\nFull text: %s",
                        probation.text()
                    )
                }

                prefs.setPreference(LongPreference.PROBATION_TIME, probTimestamp)
                return AwfulError(ERROR_PROBATION)
            }
            return null
        }
    }
}
