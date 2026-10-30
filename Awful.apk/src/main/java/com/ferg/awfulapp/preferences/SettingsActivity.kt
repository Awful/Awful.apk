package com.ferg.awfulapp.preferences

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceManager
import com.ferg.awfulapp.AwfulActivity
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.preferences.AwfulPreferences.AwfulPreferenceUpdate
import com.ferg.awfulapp.preferences.AwfulPreferences.Companion.getInstance
import com.ferg.awfulapp.preferences.fragments.AccountSettings
import com.ferg.awfulapp.preferences.fragments.RootSettings
import timber.log.Timber.Forest.e
import androidx.core.view.isVisible
import com.ferg.awfulapp.preferences.fragments.SettingsFragment

/**
 * Created by baka kaba on 04/05/2015.
 * 
 * 
 * Activity to host a new fragment-based settings system!
 * Holds a [RootSettings] which forms the root menu, and handles and
 * displays additional [SettingsFragment]s in place of PreferenceScreens (which like
 * to spawn new activities all over the screen). Please see the [SettingsFragment]
 * documentation for information on extending and adding to the Preference hierarchy.
 * 
 * 
 * In portrait mode the root menu is displayed, and submenus open on top of this, as usual. The
 * back button walks back through the hierarchy, until the root menu is shown, at which point
 * the back button will exit the Settings activity.
 * 
 * 
 * In dual-pane landscape mode, the fragment hierarchy is displayed on the right, and a copy of
 * the root menu is on the left. Since the root is always visible, the copy in the fragment
 * hierarchy is hidden, and the back stack will only walk back until the top level of a submenu is
 * visible.
 * 
 * 
 * Switching between orientations maintains this state, while ensuring you get the expected behaviour
 * (e.g. pressing back in dual-pane mode with a top-level submenu displayed will exit, but rotating
 * to portrait first will display the submenu, and pressing back will move to the root menu)
 */
class SettingsActivity : AwfulActivity(), AwfulPreferenceUpdate, SettingsFragment.OnSubmenuSelectedListener {
    @JvmField
    var prefs: AwfulPreferences? = null
    private var currentThemeName: String? = null
    private var isDualPane = false

    private val importData: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getInstance(this, this)
        currentThemeName = prefs?.theme
        updateTheme()
        // theme needs to be set BEFORE the super call, or it'll be inconsistent
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        val leftPane = findViewById<View?>(R.id.root_fragment_container)
        val mainPane = findViewById<View?>(R.id.main_fragment_container)
        if (leftPane != null && leftPane.isVisible) {
            isDualPane = true
        }

        val page = intent.getStringExtra(Constants.SETTINGS_PAGE)
        val startFragment = if ("account" == page) {
            AccountSettings()
        } else {
            RootSettings()
        }

        val fm = supportFragmentManager
        // if there's no previous fragment history being restored, initialize!
        // we need to start with the root fragment, so it's always under the backstack
        if (savedInstanceState == null) {
            fm.beginTransaction()
                .replace(R.id.main_fragment_container, startFragment, ROOT_FRAGMENT_TAG)
                .commit()
            fm.executePendingTransactions()
        }

        // hide the root fragment in dual-pane mode (there's a copy visible in the layout),
        // but make sure it's shown in single-pane (we might have switched from dual-pane)
        val fragment = fm.findFragmentByTag(ROOT_FRAGMENT_TAG) as SettingsFragment?
        if (fragment != null) {
            if (isDualPane) {
                fm.beginTransaction().hide(fragment).commit()
            } else {
                fm.beginTransaction().show(fragment).commit()
            }
        }

        val toolbar = findViewById<View?>(R.id.awful_toolbar) as Toolbar?
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        updateTitleBar()
        setPreferredFont(leftPane)
        setPreferredFont(mainPane)
    }


    /*
     * Overridden because the activity descends from the support library,
     * and looks at the SupportFragmentManager's backstack. We're using
     * PreferenceFragments which need to use the standard FragmentManager
     */
    override fun onBackPressed() {
        val fm = supportFragmentManager
        val backStackCount = fm.backStackEntryCount
        // don't pop off the first entry in dual-pane mode, it will leave the second pane blank - just exit
        if (backStackCount == 0 || isDualPane && backStackCount == 1) {
            finish()
        } else {
            fm.popBackStackImmediate()
            updateTitleBar()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onSubmenuSelected(sourceFragment: SettingsFragment, submenuFragmentName: String) {
        try {
            val fragment = (Class.forName(submenuFragmentName).newInstance()) as SettingsFragment
            val fromRootMenu = sourceFragment is RootSettings
            displayFragment(fragment, fromRootMenu)
        } catch (e: IllegalAccessException) {
            e(e, "Unable to create fragment (%s)", submenuFragmentName)
        } catch (e: ClassNotFoundException) {
            e(e, "Unable to create fragment (%s)", submenuFragmentName)
        } catch (e: InstantiationException) {
            e(e, "Unable to create fragment (%s)", submenuFragmentName)
        }
    }


    /**
     * Display a preference fragment according to screen/layout settings.
     * This handles adding the given fragment to the layout, manipulating
     * the backstack as necessary, depending on the current layout and
     * if the fragment was added by the root screen (i.e. it's a
     * new section opened from a category header in the root menu).
     * 
     * @param fragment      The fragment to add and display
     * @param addedFromRoot True if the fragment was spawned from the root menu
     */
    private fun displayFragment(fragment: SettingsFragment, addedFromRoot: Boolean) {
        /*
        Dual-pane behavior requires:
        - The root menu is always present in the left pane
        - Submenus selected from the root open in the right pane, replacing its contents
        - Any subscreens within the submenu's hierarchy open in the right pane, and the
            back button walks back through them as normal
        - If the right pane is displaying a submenu's top level, the back button should
            exit the settings activity, not back through previously visible submenus
         */

        // if we're opening a submenu and there's already one open, wipe it from the back stack

        val fm = supportFragmentManager
        if (addedFromRoot) {
            // when a root submenu is clicked, we need a new submenu backstack
            clearBackStack(fm)
        }
        fm.beginTransaction()
            .replace(R.id.main_fragment_container, fragment, SUBMENU_FRAGMENT_TAG)
            .addToBackStack(null)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()
        updateTitleBar()
    }


    private fun clearBackStack(fm: FragmentManager) {
        val fragsAddedToStack = fm.backStackEntryCount
        for (i in 0..<fragsAddedToStack) {
            fm.popBackStackImmediate()
        }
    }


    /**
     * Update the action bar's title according to what's being displayed.
     * 
     * 
     * Call this whenever the layout or fragment stack changes.
     */
    private fun updateTitleBar() {
        val actionBar = supportActionBar ?: return
        val fm = supportFragmentManager
        // make sure fragment transactions are finished before we poke around in there
        fm.executePendingTransactions()
        // if there's a submenu fragment present, get the title from that
        // need to check #isAdded because popping the last submenu fragment off the backstack doesn't immediately remove it from the manager,
        // i.e. the find call won't return null (but it will later - this was fun to troubleshoot)
        var fragment = fm.findFragmentByTag(SUBMENU_FRAGMENT_TAG)
        if (fragment == null || !fragment.isAdded) {
            fragment = fm.findFragmentByTag(ROOT_FRAGMENT_TAG)
        }
        actionBar.title = (fragment as SettingsFragment).title
    }


    override fun onPreferenceChange(preferences: AwfulPreferences, key: String?) {
        // update the summaries on any loaded fragments
        for (tag in arrayOf(ROOT_FRAGMENT_TAG, SUBMENU_FRAGMENT_TAG)) {
            val fragment = supportFragmentManager.findFragmentByTag(tag) as SettingsFragment?
            fragment?.setSummaries()
        }

        if (mPrefs.theme != this.currentThemeName) {
            this.currentThemeName = mPrefs.theme
            updateTheme()
            recreate()
        }
    }


    /*

        CODE FROM ORIGINAL SETTINGS ACTIVITY

     */
    public override fun onActivityResult(request: Int, result: Int, intent: Intent?) {
        super.onActivityResult(request, result, intent)
        if (result == RESULT_OK) {
            if (request == SETTINGS_IMPORT) {
                importFile(intent!!)
            } else if (request == SETTINGS_EXPORT) {
                exportSettings(intent!!)
            }
        }
    }

    protected fun importFile(data: Intent) {
        val settingsUri = data.data
        val success = (settingsUri != null && getInstance(this).importSettings(settingsUri))
        Toast.makeText(
            this,
            (if (success) "Import success!" else "Unable to import settings file"),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun exportSettings(data: Intent) {
        val settingsUri = data.data
        val success = (settingsUri != null && getInstance(this).exportSettings(settingsUri))
        Toast.makeText(
            this,
            (if (success) "Settings exported!" else "Failed to export"),
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        private const val ROOT_FRAGMENT_TAG = "rootfragtag"
        private const val SUBMENU_FRAGMENT_TAG = "subfragtag"
        const val DIALOG_ABOUT: Int = 1
        const val SETTINGS_IMPORT: Int = 2
        const val SETTINGS_EXPORT: Int = 3

        /**
         * A list of all XML files involved in the preference hierarchy.
         * This is required for initializing defaults from the XML,
         * unfortunately. If you add a new fragment, put its XML file
         * in here so it can be checked when the app is first run.
         */
        private val PREFERENCE_XML_FILES = intArrayOf(
            R.xml.accountsettings,
            R.xml.imagesettings,
            R.xml.miscsettings,
            R.xml.postsettings,
            R.xml.post_highlighting_settings,
            R.xml.rootsettings,
            R.xml.themesettings,
            R.xml.threadinfosettings
        )

        /**
         * Initialize all preference defaults from the XML hierarchy
         */
        fun setDefaultsFromXml(context: Context) {
            for (id in PREFERENCE_XML_FILES) {
                PreferenceManager.setDefaultValues(context, id, true)
            }
        }
    }
}