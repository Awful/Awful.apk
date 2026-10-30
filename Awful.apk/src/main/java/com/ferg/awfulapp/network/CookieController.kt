package com.ferg.awfulapp.network

import android.content.Context
import android.text.TextUtils
import androidx.core.content.edit
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.constants.Constants.COOKIE_DOMAIN_CAPTCHA
import com.ferg.awfulapp.constants.Constants.COOKIE_NAME_CAPTCHA
import com.ferg.awfulapp.preferences.AwfulPreferences
import timber.log.Timber.Forest.d
import timber.log.Timber.Forest.e
import timber.log.Timber.Forest.i
import timber.log.Timber.Forest.w
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Optional
import java.util.TimeZone

/**
 * Handles all interactions with cookies
 */
object CookieController {
    private val cookieManager: CookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL)
    private var cookie: String? = null
    private const val COOKIE_HEADER = "Cookie"
    private val uri: URI? = URI.create(Constants.BASE_URL)

    init {
        CookieHandler.setDefault(cookieManager)
    }

    /**
     * Add the current session cookie's data to a header map.
     * 
     * 
     * The data is provided as a single header - see [.restoreLoginCookies] for the format.
     */
    fun setCookieHeaders(headers: MutableMap<String, String>) {
        if (cookie == null) {
            w("Cookie was empty for some reason, trying to restore cookie")
            restoreLoginCookies(AwfulPreferences.getInstance().context)
        }

        val finalCookies: String = (captchaCookie.orElse("")
                + Optional.ofNullable<String>(cookie).orElse(""))

        if (finalCookies.isNotEmpty()) {
            headers[COOKIE_HEADER] = finalCookies
        }
    }

    /**
     * Attempts to initialize the HttpClient with cookie values
     * stored in the given Context's SharedPreferences through the
     * [.saveLoginCookies] method.
     * 
     * @return Whether stored cookie values were found & initialized
     */
    @Synchronized
    fun restoreLoginCookies(ctx: Context): Boolean {
        val prefs = ctx.getSharedPreferences(Constants.COOKIE_PREFERENCE, Context.MODE_PRIVATE)
        var useridCookieString = prefs.getString(Constants.COOKIE_PREF_USERID, null)
        var passwordCookieString = prefs.getString(Constants.COOKIE_PREF_PASSWORD, null)
        var sessionidCookieString = prefs.getString(Constants.COOKIE_PREF_SESSIONID, null)
        var sessionhashCookieString = prefs.getString(Constants.COOKIE_PREF_SESSIONHASH, null)
        if (useridCookieString == null || passwordCookieString == null || sessionidCookieString == null || sessionhashCookieString == null) {
            if (Constants.DEBUG) {
                e( buildString {
                    append("Unable to retrieve cookies! Reasons:\n")
                    append(if (useridCookieString == null) "USER_ID is NULL\n" else "")
                    append(if (passwordCookieString == null) "pass is NULL\n" else "")
                    append(if (sessionidCookieString == null) "sessid is NULL\n" else "")
                    append(if (sessionhashCookieString == null) "sesshash is NULL\n" else "")
                })
            }

            cookie = ""
            return false
        }

        if (!useridCookieString.startsWith(Constants.COOKIE_PREF_USERID) || !passwordCookieString.startsWith(
                Constants.COOKIE_PREF_PASSWORD
            )
        ) {
            val expiry = prefs.getLong(Constants.COOKIE_PREF_EXPIRY_DATE, -1)
            val maxAge = expiry - System.currentTimeMillis()
            val expires = calculateExpires(maxAge)
            useridCookieString = String.format(
                "%s=%s; Domain=%s; Path=%s;Max-Age=%s; Expires=%s;",
                Constants.COOKIE_PREF_USERID,
                useridCookieString,
                Constants.COOKIE_DOMAIN,
                Constants.COOKIE_PATH,
                maxAge,
                expires
            )
            passwordCookieString = String.format(
                "%s=%s; Domain=%s; Path=%s;Max-Age=%s; Expires=%s;",
                Constants.COOKIE_PREF_PASSWORD,
                passwordCookieString,
                Constants.COOKIE_DOMAIN,
                Constants.COOKIE_PATH,
                maxAge,
                expires
            )
        }
        if (!sessionidCookieString.startsWith(Constants.COOKIE_PREF_SESSIONID) || !sessionhashCookieString.startsWith(
                Constants.COOKIE_PREF_SESSIONHASH
            )
        ) {
            sessionidCookieString = String.format(
                "%s=%s; Domain=%s; Path=%s;",
                Constants.COOKIE_PREF_SESSIONID,
                sessionidCookieString,
                Constants.COOKIE_DOMAIN,
                Constants.COOKIE_PATH
            )
            sessionhashCookieString = String.format(
                "%s=%s; Domain=%s; Path=%s;",
                Constants.COOKIE_PREF_SESSIONHASH,
                sessionhashCookieString,
                Constants.COOKIE_DOMAIN,
                Constants.COOKIE_PATH
            )
        }

        val useridCookie = HttpCookie.parse(useridCookieString)[0]
        val passwordCookie = HttpCookie.parse(passwordCookieString)[0]
        val sessionidCookie = HttpCookie.parse(sessionidCookieString)[0]
        val sessionhashCookie = HttpCookie.parse(sessionhashCookieString)[0]
        // verify the cookie is valid - if not, we need to clear the cookie and return a failure
        if (useridCookie.value == null || passwordCookie.value == null || useridCookie.hasExpired() || passwordCookie.hasExpired()) {
            if (Constants.DEBUG) {
                w(buildString {
                    append("Unable to restore cookies! Reasons:\n")
                    append(if (passwordCookie.value == null) "PASSWORD is NULL\n" else "")
                    append(if (passwordCookie.value == null) "PASSWORD is NULL\n" else "")
                    append(if (useridCookie.hasExpired()) "userid cookie has expired, max age = " + useridCookie.maxAge else "")
                    append(if (passwordCookie.hasExpired()) "password cookie has expired, max age = " + passwordCookie.maxAge else "")
                })
            }

            cookie = ""
            return false
        }

        cookie = String.format(
            "%s=%s;%s=%s;%s=%s;%s=%s;",
            Constants.COOKIE_NAME_USERID, useridCookie.value,
            Constants.COOKIE_NAME_PASSWORD, passwordCookie.value,
            Constants.COOKIE_NAME_SESSIONID, sessionidCookie.value,
            Constants.COOKIE_NAME_SESSIONHASH, sessionhashCookie.value
        )


        val allCookies = arrayOf<HttpCookie>(
            useridCookie,
            passwordCookie,
            sessionidCookie,
            sessionhashCookie
        )


        for (tempCookie in allCookies) {
            if (!tempCookie.hasExpired()) {
                cookieManager.cookieStore.add(uri, tempCookie)
            }
        }

        if (Constants.DEBUG) {
            i("Cookies restored from prefs")
            i("Cookie dump: %s", TextUtils.join("\n", cookieManager.cookieStore.cookies))
        }

        return true
    }

    /**
     * Clears cookies from both the current client's store and
     * the persistent SharedPreferences. Effectively, logs out.
     */
    @Synchronized
    fun clearLoginCookies(context: Context) {
        // First clear out the persistent preferences...
        context.getSharedPreferences(
            Constants.COOKIE_PREFERENCE,
            Context.MODE_PRIVATE
        ).edit { clear() }

        // Then the memory store
        cookieManager.cookieStore.removeAll()
    }

    /**
     * Saves SomethingAwful login cookies that the client has received
     * during this session to the given Context's SharedPreferences. They
     * can be later restored with [.restoreLoginCookies].
     * 
     * @return Whether any login cookies were successfully saved
     */
    @Synchronized
    fun saveLoginCookies(ctx: Context): Boolean {
        val prefs = ctx.getSharedPreferences(
            Constants.COOKIE_PREFERENCE,
            Context.MODE_PRIVATE
        )

        var useridValue: String? = null
        var passwordValue: String? = null
        var sessionId: String? = null
        var sessionHash: String? = null
        //val expires: Date? = null
        //val version: Int? = null

        d("Saving cookies - here's what we got:")
        logCookies()

        for (cookie in cookieManager.cookieStore.get(uri)) {
            when (cookie.name) {
                Constants.COOKIE_NAME_USERID -> useridValue = getCookieString(cookie, true)
                Constants.COOKIE_NAME_PASSWORD -> passwordValue = getCookieString(cookie, true)
                Constants.COOKIE_NAME_SESSIONID -> sessionId = getCookieString(cookie, true)
                Constants.COOKIE_NAME_SESSIONHASH -> sessionHash = getCookieString(cookie, true)
                else -> continue // unrecognised cookie, ignore it! some cloudflare ones have a real short expiry
            }
        }

        if (useridValue == null || passwordValue == null) {
            return false
        }

        prefs.edit {
            putString(Constants.COOKIE_PREF_USERID, useridValue)
            putString(Constants.COOKIE_PREF_PASSWORD, passwordValue)
            if (!sessionId.isNullOrEmpty()) {
                putString(Constants.COOKIE_PREF_SESSIONID, sessionId)
            }
            if (!sessionHash.isNullOrEmpty()) {
                putString(Constants.COOKIE_PREF_SESSIONHASH, sessionHash)
            }
        }
        return true
    }

    fun calculateExpires(maxAge: Long): String {
        val c = Calendar.getInstance()
        c.add(Calendar.SECOND, (maxAge.toInt()))
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
        return ";expires=" + dateFormat.format(c.getTime())
    }

    @Synchronized
    fun getCookieString(cookie: HttpCookie): String {
        return getCookieString(cookie, false)
    }

    @Synchronized
    fun getCookieString(cookie: HttpCookie, calculateExpires: Boolean): String {
        var expires = ""
        if (calculateExpires) {
            expires = calculateExpires(cookie.maxAge)
        }
        return String.format(
            "%s=%s%s;max-age=%s;domain=%s;path=%s",
            cookie.name,
            cookie.value,
            expires,
            cookie.maxAge,
            cookie.domain,
            cookie.path
        )
    }

    @Synchronized
    fun getCookieString(type: String): String {
        for (cookie in cookieManager.cookieStore.get(uri)) {
            if (cookie.name.contains(type)) return getCookieString(cookie)
        }
        w("getCookieString couldn't find type: %s", type)
        return ""
    }

    /**
     * Set the Cloudflare captcha cookie after it was retrieved in a CaptchaActivity web view.
     */
    fun setCaptchaCookie(cookie: String?) {
        val httpCookie = HttpCookie(COOKIE_NAME_CAPTCHA, cookie)
        httpCookie.domain = COOKIE_DOMAIN_CAPTCHA // applies to subdomains as well
        cookieManager.cookieStore.add(uri, httpCookie)
    }

    val captchaCookie: Optional<String>
        /**
         * If the Cloudflare captcha cookie is set, return it so that it can be appended to the provided
         * cookies.
         */
        get() {
            // It seems like there is no direct accessor for a specific cookie, so this little dance has
            // to be done all the time to find the right one.
            for (c in cookieManager.cookieStore.get(uri)) {
                if (c.name == COOKIE_NAME_CAPTCHA) {
                    return Optional.of<String>(COOKIE_NAME_CAPTCHA + "=" + c.value + ";")
                }
            }

            return Optional.empty<String>()
        }

    fun logCookies() {
        if (Constants.DEBUG) {
            i("---BEGIN COOKIE DUMP---")
            val cookies = cookieManager.cookieStore.cookies
            for (c in cookies) {
                d(
                    "Name: %s\nSecure only: %b, expired: %b, max age: %d\nContent: %s\n",
                    c.name, c.secure, c.hasExpired(), c.maxAge, c.toString()
                )
            }
            i("---END COOKIE DUMP---")
        }
    }
}
