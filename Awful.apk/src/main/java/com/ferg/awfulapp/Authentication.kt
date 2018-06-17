package com.ferg.awfulapp

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
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
class LogOutDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity!!)
                    .setTitle(R.string.logout)
                    .setMessage(R.string.logout_message)
                    .setPositiveButton(R.string.logout, { _, _ ->
                        activity?.let(::reAuthenticate) ?: logOut()
                    })
                    .setNegativeButton(R.string.cancel, { _, _ -> Unit })
                    .create()

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
