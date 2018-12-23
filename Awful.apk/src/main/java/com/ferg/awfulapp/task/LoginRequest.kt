package com.ferg.awfulapp.task

import android.content.Context
import com.android.volley.NetworkResponse
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.Keys
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

/**
 * AwfulRequest that sends a login request to the site, and stores login cookies and sets
 * the current username if successful.
 */
class LoginRequest(context: Context, private val username: String, password: String)
    : AwfulRequest<Boolean>(context, FUNCTION_LOGIN_SSL, isPostRequest = true) {

    init {
        with(parameters) {
            add(PARAM_ACTION, "login")
            add(PARAM_USERNAME, username)
            add(PARAM_PASSWORD, password)
        }
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Boolean = validateLoginState()

    override fun handleError(error: AwfulError, doc: Document): Boolean =
            error.networkResponse?.isRedirect == true || !error.isCritical


    private val NetworkResponse.isRedirect get() = this.statusCode == 302

    /**
     * Check if we've received login cookies, and if so store the username we used to log in
     */
    private fun validateLoginState(): Boolean {
        return NetworkUtils.saveLoginCookies(context).also { success ->
            if (success) preferences.setPreference(Keys.USERNAME, username)
        }
    }

}
