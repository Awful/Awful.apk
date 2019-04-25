package com.ferg.awfulapp.preferences;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.ferg.awfulapp.AwfulActivity;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.fragments.RootSettings;
import com.ferg.awfulapp.preferences.fragments.SettingsFragment;
import com.ferg.awfulapp.util.AwfulUtils;

import timber.log.Timber;

/**
 * Created by baka kaba on 04/05/2015.
 * <p>
 * Activity to host a new fragment-based settings system!
 * Holds a {@link RootSettings} which forms the root menu, and handles and
 * displays additional {@link SettingsFragment}s in place of PreferenceScreens (which like
 * to spawn new activities all over the screen). Please see the {@link SettingsFragment}
 * documentation for information on extending and adding to the Preference hierarchy.
 * <p>
 * In portrait mode the root menu is displayed, and submenus open on top of this, as usual. The
 * back button walks back through the hierarchy, until the root menu is shown, at which point
 * the back button will exit the Settings activity.
 * <p>
 * In dual-pane landscape mode, the fragment hierarchy is displayed on the right, and a copy of
 * the root menu is on the left. Since the root is always visible, the copy in the fragment
 * hierarchy is hidden, and the back stack will only walk back until the top level of a submenu is
 * visible.
 * <p>
 * Switching between orientations maintains this state, while ensuring you get the expected behaviour
 * (e.g. pressing back in dual-pane mode with a top-level submenu displayed will exit, but rotating
 * to portrait first will display the submenu, and pressing back will move to the root menu)
 */
public class SettingsActivity extends AwfulActivity implements AwfulPreferences.AwfulPreferenceUpdate,
        SettingsFragment.OnSubmenuSelectedListener {

    private static final String ROOT_FRAGMENT_TAG = "rootfragtag";
    private static final String SUBMENU_FRAGMENT_TAG = "subfragtag";
    public static final int DIALOG_ABOUT = 1;
    public static final int SETTINGS_FILE = 2;

    public AwfulPreferences prefs;
    private String currentThemeName;
    private boolean isDualPane;

    /**
     * A list of all XML files involved in the preference hierarchy.
     * This is required for initialising defaults from the XML,
     * unfortunately. If you add a new fragment, put its XML file
     * in here so it can be checked when the app is first run.
     */
    private static final int[] PREFERENCE_XML_FILES = new int[]{
            R.xml.accountsettings,
            R.xml.imagesettings,
            R.xml.miscsettings,
            R.xml.postsettings,
            R.xml.post_highlighting_settings,
            R.xml.rootsettings,
            R.xml.themesettings,
            R.xml.threadinfosettings
    };
    private Intent importData;

    /**
     * Initialise all preference defaults from the XML hierarchy
     */
    public static void setDefaultsFromXml(Context context) {
        for (int id : PREFERENCE_XML_FILES) {
            PreferenceManager.setDefaultValues(context, id, true);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = AwfulPreferences.getInstance(this, this);
        currentThemeName = prefs.theme;
        updateTheme();
        // theme needs to be set BEFORE the super call, or it'll be inconsistent
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        View leftPane = findViewById(R.id.root_fragment_container);
        if (leftPane != null && leftPane.getVisibility() == View.VISIBLE) {
            isDualPane = true;
        }

        FragmentManager fm = getFragmentManager();
        // if there's no previous fragment history being restored, initialise!
        // we need to start with the root fragment, so it's always under the backstack
        if (savedInstanceState == null) {
            fm.beginTransaction()
                    .replace(R.id.main_fragment_container, new RootSettings(), ROOT_FRAGMENT_TAG)
                    .commit();
            fm.executePendingTransactions();
        }

        // hide the root fragment in dual-pane mode (there's a copy visible in the layout),
        // but make sure it's shown in single-pane (we might have switched from dual-pane)
        SettingsFragment fragment = (SettingsFragment) fm.findFragmentByTag(ROOT_FRAGMENT_TAG);
        if (fragment != null) {
            if (isDualPane) {
                fm.beginTransaction().hide(fragment).commit();
            } else {
                fm.beginTransaction().show(fragment).commit();
            }
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.awful_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        updateTitleBar();
    }


    /*
     * Overridden because the activity descends from the support library,
     * and looks at the SupportFragmentManager's backstack. We're using
     * PreferenceFragments which need to use the standard FragmentManager
     */
    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        int backStackCount = fm.getBackStackEntryCount();
        // don't pop off the first entry in dual-pane mode, it will leave the second pane blank - just exit
        if (backStackCount == 0 || isDualPane && backStackCount == 1) {
            finish();
        } else {
            fm.popBackStackImmediate();
            updateTitleBar();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onSubmenuSelected(@NonNull SettingsFragment sourceFragment, @NonNull String submenuFragmentName) {
        try {
            SettingsFragment fragment = (SettingsFragment) (Class.forName(submenuFragmentName).newInstance());
            boolean fromRootMenu = sourceFragment instanceof RootSettings;
            displayFragment(fragment, fromRootMenu);
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            Timber.e(e, "Unable to create fragment (%s)", submenuFragmentName);
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
    private void displayFragment(SettingsFragment fragment, boolean addedFromRoot) {
        /*
        Dual-pane behaviour requires:
        - The root menu is always present in the left pane
        - Submenus selected from the root open in the right pane, replacing its contents
        - Any subscreens within the submenu's hierarchy open in the right pane, and the
            back button walks back through them as normal
        - If the right pane is displaying a submenu's top level, the back button should
            exit the settings activity, not back through previously visible submenus
         */

        // if we're opening a submenu and there's already one open, wipe it from the back stack
        FragmentManager fm = getFragmentManager();
        if (addedFromRoot) {
            // when a root submenu is clicked, we need a new submenu backstack
            clearBackStack(fm);
        }
        fm.beginTransaction()
                .replace(R.id.main_fragment_container, fragment, SUBMENU_FRAGMENT_TAG)
                .addToBackStack(null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
        updateTitleBar();
    }


    private void clearBackStack(FragmentManager fm) {
        int fragsAddedToStack = fm.getBackStackEntryCount();
        for (int i = 0; i < fragsAddedToStack; i++) {
            fm.popBackStackImmediate();
        }
    }


    /**
     * Update the action bar's title according to what's being displayed.
     * <p>
     * Call this whenever the layout or fragment stack changes.
     */
    private void updateTitleBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        FragmentManager fm = getFragmentManager();
        // make sure fragment transactions are finished before we poke around in there
        fm.executePendingTransactions();
        // if there's a submenu fragment present, get the title from that
        // need to check #isAdded because popping the last submenu fragment off the backstack doesn't immediately remove it from the manager,
        // i.e. the find call won't return null (but it will later - this was fun to troubleshoot)
        Fragment fragment = fm.findFragmentByTag(SUBMENU_FRAGMENT_TAG);
        if (fragment == null || !fragment.isAdded()) {
            fragment = fm.findFragmentByTag(ROOT_FRAGMENT_TAG);
        }
        actionBar.setTitle(((SettingsFragment) fragment).getTitle());
    }


    @Override
    public void onPreferenceChange(AwfulPreferences preferences, String key) {
        // update the summaries on any loaded fragments
        for (String tag : new String[]{ROOT_FRAGMENT_TAG, SUBMENU_FRAGMENT_TAG}) {
            SettingsFragment fragment = (SettingsFragment) getFragmentManager().findFragmentByTag(tag);
            if (fragment != null) {
                fragment.setSummaries();
            }
        }

        if (!getMPrefs().theme.equals(this.currentThemeName)) {
            this.currentThemeName = getMPrefs().theme;
            updateTheme();
            recreate();
        }
    }


    /*

        CODE FROM ORIGINAL SETTINGS ACTIVITY

     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SETTINGS_FILE) {
                if (AwfulUtils.isMarshmallow()) {
                    int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        this.importData = data;
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE);
                    } else {
                        importFile(data);
                    }
                } else {
                    importFile(data);
                }
            }
        }
    }

    protected void importFile(Intent data) {
        Uri settingsUri = data.getData();
        if (settingsUri != null && AwfulPreferences.getInstance(this).importSettings(settingsUri)) {
            Toast.makeText(this, "Import success!", Toast.LENGTH_SHORT).show();
            this.finish();
        } else {
            Toast.makeText(this, "Unable to import settings file", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected Dialog onCreateDialog(int dialogId) {
        if (dialogId == DIALOG_ABOUT)
            return getAboutDialog();
        else
            return super.onCreateDialog(dialogId);
    }

    private Dialog getAboutDialog() {
        return new AlertDialog.Builder(this)
                .setTitle(getAboutDialogTitle())
                .setMessage(getAboutDialogText())
                .setNeutralButton(android.R.string.ok, (dialog, which) -> {
                })
                .create();
    }

    private String getAboutDialogTitle() {
        String result = getText(R.string.app_name).toString();

        try {
            result += " " +
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // rather unlikely, just return app_name without version
        }

        return result;
    }

    private String getAboutDialogText() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(getString(R.string.about_contributors_title)).append("\n");

        for (String s : getStringArray(R.array.about_contributors_array)) {
            stringBuilder.append("- ").append(s).append('\n');
        }

        stringBuilder.append("\n").append(getString(R.string.about_libraries_title)).append("\n");

        for (String s : getStringArray(R.array.about_libraries_array)) {
            stringBuilder.append("- ").append(s).append('\n');
        }

        stringBuilder.append('\n').append(getWebViewUserAgentString());

        return stringBuilder.toString();
    }

    private String[] getStringArray(int arrayId) {
        return getResources().getStringArray(arrayId);
    }

    private String getWebViewUserAgentString() {
        return new WebView(this).getSettings().getUserAgentString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    importFile(importData);
                } else {
                    Toast.makeText(this, R.string.no_file_permission_settings_import, Toast.LENGTH_LONG).show();
                }
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
