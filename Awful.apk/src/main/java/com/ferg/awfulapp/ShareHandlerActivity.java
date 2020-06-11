package com.ferg.awfulapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.widget.Toast;

import com.ferg.awfulapp.thread.AwfulURL;

/**
 * Created by baka kaba on 02/07/2017.
 * <p>
 * Handles share intents from other apps.
 */
public class ShareHandlerActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        // currently just
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent);
            }
        } else {
            // fallthrough - couldn't do anything with this share, let the user know
            Toast.makeText(this, R.string.share_handler_no_url_found, Toast.LENGTH_SHORT).show();
        }

        // the activity runs in its own task - drop it once we're done with the intent
        finish();
    }


    /**
     * Handles a plain text {@link Intent#ACTION_SEND} intent.
     */
    private void handleSendText(Intent intent) {
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);

        // Right now we only handle URLs, and ignore any other plain text content

        // check if it's one of our URLs - AwfulURL parsing never fails, but falls back to EXTERNAL if it's not recognised
        AwfulURL awfulURL = (text != null) ? AwfulURL.parse(text) : null;
        if (awfulURL != null && !awfulURL.isExternal()) {
            Intent openAppIntent = new NavigationEvent.Url(awfulURL).getIntent(this).setAction(Intent.ACTION_VIEW);
            startActivity(openAppIntent);
        } else {
            Toast.makeText(this, R.string.share_handler_no_url_found, Toast.LENGTH_LONG).show();
        }
    }
}
