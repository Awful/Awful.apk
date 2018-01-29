package com.ferg.awfulapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import com.ferg.awfulapp.Authentication.logOut
import com.ferg.awfulapp.Authentication.reAuthenticate
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences

/**
 * Things that relate to logging the user in and out, and their current status.
 */


/**
 * A dialog that allows the user to log out and [reAuthenticate]
 */
class LogOutDialog(context: Context) : AlertDialog(context) {

    init {
        setTitle(context.getString(R.string.logout))
        setMessage(context.getString(R.string.logout_message))
        setButton(BUTTON_POSITIVE, context.getString(R.string.logout)) { _, _ -> ownerActivity?.let { reAuthenticate(it) } ?: logOut() }
        setButton(BUTTON_NEGATIVE, context.getString(R.string.cancel), { _, _ -> Unit })
    }

}

object Authentication {

    fun isUserLoggedIn() = NetworkUtils.restoreLoginCookies(context)

    /**
     * Logs the user out silently - call [reAuthenticate] if you want to show the login screen instead
     */
    fun logOut() {
        NetworkUtils.clearLoginCookies(context)
    }


    /**
     * Log out and display the login screen.
     */
    fun reAuthenticate(resultHandler: Activity) {
        logOut()
        with(Intent(resultHandler, AwfulLoginActivity::class.java)) {
            // TODO: using CLEAR TOP to avoid several login activities piling up through multiple calls - ideally there'd be a timeout or something after the first
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            resultHandler.startActivityForResult(this, Constants.LOGIN_ACTIVITY_REQUEST)
        }
    }


    private val context by lazy { AwfulPreferences.getInstance().context }
}
