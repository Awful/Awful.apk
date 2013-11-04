package com.ferg.awfulapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import com.ferg.awfulapp.constants.Constants;

public class UserCPShortcutActivity extends Activity {

    private static final String EXTRA_KEY = "com.ferg.awfulapp.UserCPShortcutActivity";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Resolve the intent

        final Intent intent = getIntent();
        final String action = intent.getAction();

        // If the intent is a request to create a shortcut, we'll do that and exit

        if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
            setupShortcut();
        }

        finish();
    }

    private void setupShortcut() {

        Intent shortcutIntent = new Intent();
        shortcutIntent.setClass(this, ForumsIndexActivity.class);
        shortcutIntent.putExtra(Constants.FORUM_ID, Constants.USERCP_ID);

        // Then, set up the container intent (the response to the caller)
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.usercp));
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
                this,  R.drawable.icon);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        // Now, return the result to the launcher
        setResult(RESULT_OK, intent);
    }
}
