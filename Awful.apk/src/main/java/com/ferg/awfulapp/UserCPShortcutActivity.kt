package com.ferg.awfulapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.ferg.awfulapp.preferences.AwfulPreferences

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
        shortcutIntent.action = Intent.ACTION_DEFAULT
        val name = getString(R.string.usercp)
        val launcherIcon = IconCompat.createWithResource(this, getLauncherIcon())

        val shortcut = ShortcutInfoCompat.Builder(this, getString(R.string.awful_bookmarks_shortcut_id))
            .setIntent(shortcutIntent)
            .setShortLabel(name)
            // TODO: doesn't support themed icons in this configuration. Unable to find a workaround right now.
            .setIcon(launcherIcon)
            .build()

        setResult(RESULT_OK, ShortcutManagerCompat.createShortcutResultIntent(this, shortcut))
    }

    private fun getLauncherIcon(): Int {
        val launcherIconString = AwfulPreferences.getInstance().launcherIcon.replace('.','_')
        return when(launcherIconString) {
            "frog" -> R.mipmap.ic_launcher
            else -> resources.getIdentifier("ic_launcher_$launcherIconString","mipmap", packageName)
        }
    }
}
