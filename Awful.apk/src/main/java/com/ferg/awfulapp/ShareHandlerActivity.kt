package com.ferg.awfulapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.ferg.awfulapp.NavigationEvent.Url
import com.ferg.awfulapp.thread.AwfulURL

/**
 * Created by baka kaba on 02/07/2017.
 * 
 * 
 * Handles share intents from other apps.
 */
class ShareHandlerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get intent, action and MIME type
        val intent = getIntent()
        val action = intent.action
        val type = intent.type

        // currently just
        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                handleSendText(intent)
            }
        } else {
            // fallthrough - couldn't do anything with this share, let the user know
            Toast.makeText(this, R.string.share_handler_no_url_found, Toast.LENGTH_SHORT).show()
        }

        // the activity runs in its own task - drop it once we're done with the intent
        finish()
    }


    /**
     * Handles a plain text [Intent.ACTION_SEND] intent.
     */
    private fun handleSendText(intent: Intent) {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)

        // Right now we only handle URLs, and ignore any other plain text content

        // check if it's one of our URLs - AwfulURL parsing never fails, but falls back to EXTERNAL if it's not recognized
        val awfulURL = if (text != null) AwfulURL.parse(text) else null
        if (awfulURL != null && !awfulURL.isExternal) {
            val openAppIntent = Url(awfulURL).getIntent(this).setAction(Intent.ACTION_VIEW)
            startActivity(openAppIntent)
        } else {
            Toast.makeText(this, R.string.share_handler_no_url_found, Toast.LENGTH_LONG).show()
        }
    }
}
