package com.ferg.awfulapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.Keys


/**
 * Activity that handles the 'authorize with imgur' Intent.
 */
class AwfulImgurAuthActivity : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If the intent is a view request,  we'll do that and exit
        if (intent.action == Intent.ACTION_VIEW) {
            parseImgurResponse()
        }
        finish()
    }

    private fun parseImgurResponse() {
        val tokenUri = Uri.parse(intent.data?.toString()?.replace("#","?"));
        val accessToken = tokenUri.getQueryParameter("access_token");
        val refreshToken = tokenUri.getQueryParameter("refresh_token");
        val account = tokenUri.getQueryParameter("account_username");
        val expiresIn = (tokenUri.getQueryParameter("expires_in")!!.toLong() * 1000L) + System.currentTimeMillis();

        AwfulPreferences.getInstance().setPreference(Keys.IMGUR_ACCOUNT_TOKEN, accessToken);
        AwfulPreferences.getInstance().setPreference(Keys.IMGUR_REFRESH_TOKEN, refreshToken);
        AwfulPreferences.getInstance().setPreference(Keys.IMGUR_ACCOUNT, account);
        AwfulPreferences.getInstance().setPreference(Keys.IMGUR_TOKEN_EXPIRES, expiresIn ?: 0L);
    }
}
