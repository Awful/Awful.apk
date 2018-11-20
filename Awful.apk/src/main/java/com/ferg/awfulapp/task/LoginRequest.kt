package com.ferg.awfulapp.task

import android.content.Context
import android.net.Uri
import com.android.volley.NetworkResponse
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.Keys
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

/**
 * AwfulRequest that sends a login request to the site, and stores login cookies and sets the current username if successful.
 */
class LoginRequest(context: Context, private val username: String, password: String) : AwfulRequest<Boolean>(context, null) {
    init {
        addPostParam(Constants.PARAM_ACTION, "login")
        addPostParam(Constants.PARAM_USERNAME, username)
        addPostParam(Constants.PARAM_PASSWORD, password)
    }

    override fun generateUrl(urlBuilder: Uri.Builder?): String = Constants.FUNCTION_LOGIN_SSL

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
