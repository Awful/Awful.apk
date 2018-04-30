package com.ferg.awfulapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Activity that handles the 'create a bookmarks widget' Intent.
 */
class UserCPShortcutActivity : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If the intent is a request to create a shortcut, we'll do that and exit
        if (intent.action == Intent.ACTION_CREATE_SHORTCUT) {
            setupShortcut()
        }
        finish()
    }


    /**
     * Builds the Intent to create the Bookmarks widget, containing the app's "show the bookmarks"
     * Intent (for when it's clicked) and an icon and title for the widget.
     */
    private fun setupShortcut() {
        val shortcutIntent = NavigationEvent.Bookmarks.getIntent(this)
        val iconResource = Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher)
        // Then, set up the container intent (the response to the caller)
        with(Intent()) {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.usercp))
            putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource)
            // Now, return the result to the launcher
            setResult(Activity.RESULT_OK, this)
        }
    }
}
