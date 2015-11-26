package com.ferg.awfulapp.preferences;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ferg.awfulapp.AwfulActivity;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.fragments.RootSettings;
import com.ferg.awfulapp.preferences.fragments.SettingsFragment;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.util.AwfulUtils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * Created by baka kaba on 04/05/2015.
 *
 * Activity to host a new fragment-based settings system!
 * Holds a {@link RootSettings} which forms the root menu, and handles and
 * displays additional {@link SettingsFragment}s in place of PreferenceScreens (which like
 * to spawn new activities all over the screen). Please see the {@link SettingsFragment}
 * documentation for information on extending and adding to the Preference hierarchy.
 *
 * In portrait mode the root menu is displayed, and submenus open on top of this, as usual. The
 * back button walks back through the hierarchy, until the root menu is shown, at which point
 * the back button will exit the Settings activity.
 *
 * In dual-pane landscape mode, the fragment hierarchy is displayed on the right, and a copy of
 * the root menu is on the left. Since the root is always visible, the copy in the fragment
 * hierarchy is hidden, and the back stack will only walk back until the top level of a submenu is
 * visible.
 *
 * Switching between orientations maintains this state, while ensuring you get the expected behaviour
 * (e.g. pressing back in dual-pane mode with a top-level submenu displayed will exit, but rotating
 * to portrait first will display the submenu, and pressing back will move to the root menu)
 *
 */
public class SettingsActivity extends AwfulActivity implements AwfulPreferences.AwfulPreferenceUpdate,
        SettingsFragment.OnSubmenuSelectedListener {

    private static final String ROOT_FRAGMENT_TAG      = "rootfragtag";
    private static final String SUBMENU_FRAGMENT_TAG   = "subfragtag";
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

    /** Initialise all preference defaults from the XML hierarchy */
    public static void setDefaultsFromXml(Context context) {
        for (int id : PREFERENCE_XML_FILES) {
            PreferenceManager.setDefaultValues(context, id, true);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = AwfulPreferences.getInstance(this,this);
        currentThemeName = prefs.theme;
        setCurrentTheme();
        // theme needs to be set BEFORE the super call, or it'll be inconsistent
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        View leftPane = findViewById(R.id.root_fragment_container);
        if (leftPane != null && leftPane.getVisibility() == View.VISIBLE) {
            isDualPane = true;
        }

        Toolbar mToolbar = (Toolbar) findViewById(R.id.awful_toolbar);
        setSupportActionBar(mToolbar);
        setActionbarTitle(getString(R.string.settings_activity_title), null);

        FragmentManager fm = getFragmentManager();
        // if there's no previous fragment history being restored, initialise!
        if (savedInstanceState == null) {
            fm.beginTransaction()
                    .replace(R.id.main_fragment_container, new RootSettings(), ROOT_FRAGMENT_TAG)
                    .commit();
        }
        // don't display the root fragment in dual pane mode (there's one in the layout)
        if (isDualPane) {
            fm.executePendingTransactions();
            SettingsFragment fragment = (SettingsFragment) fm.findFragmentByTag(ROOT_FRAGMENT_TAG);
            if (fragment != null) {
                fm.beginTransaction().hide(fragment).commit();
            }
        }
    }

    /*
     * Overridden because the activity descends from the support library,
     * and looks at the SupportFragmentManager's backstack. We're using
     * PreferenceFragments which need to use the standard FragmentManager
     */
    @Override
    public void onBackPressed()
    {
        FragmentManager fm = getFragmentManager();
        int backStackEntryCount = fm.getBackStackEntryCount();
        // don't pop to the root fragment at the base of the fragment hierarchy in dual-pane mode
        if (isDualPane && backStackEntryCount > 1) {
            fm.popBackStack();
        } else if (!isDualPane && backStackEntryCount > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public void onSubmenuSelected(SettingsFragment container, String submenuFragment) {
        try {
            SettingsFragment fragment = (SettingsFragment)(Class.forName(submenuFragment).newInstance());
            boolean fromRootMenu = container != null && container instanceof RootSettings;
            displayFragment(fragment, fromRootMenu);
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            Log.e(TAG, "Unable to create fragment (" + submenuFragment + ")\n", e);
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
        if (addedFromRoot && fm.findFragmentByTag(SUBMENU_FRAGMENT_TAG) != null) {
            // when a root submenu is clicked, clear the side pane state first
            int fragsAddedToStack = fm.getBackStackEntryCount();
            for (int i = 0; i < fragsAddedToStack; i++) {
                fm.popBackStack();
            }
        }
        fm.beginTransaction().replace(R.id.main_fragment_container, fragment, SUBMENU_FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }


    @Override
    public void onPreferenceChange(AwfulPreferences preferences, String key) {
        // update the summaries on any loaded fragments
        for (String tag : new String[] {ROOT_FRAGMENT_TAG, SUBMENU_FRAGMENT_TAG}) {
            SettingsFragment fragment = (SettingsFragment) getFragmentManager().findFragmentByTag(tag);
            if (fragment != null) {
                fragment.setSummaries();
            }
        }

        if(!prefs.theme.equals(this.currentThemeName)) {
            this.currentThemeName = prefs.theme;
            setCurrentTheme();
            recreate();
        }
    }


    private void setCurrentTheme() {
        // TODO: this is yoinked out of AwfulActivity and could really do with being centralised
        if(prefs.theme.equals(ColorProvider.DEFAULT) || prefs.theme.equals(ColorProvider.CLASSIC)){
            setTheme(R.style.Theme_AwfulTheme);
        }else if(prefs.theme.equals(ColorProvider.FYAD)){
            setTheme(R.style.Theme_AwfulTheme_FYAD);
        }else if(prefs.theme.equals(ColorProvider.BYOB)){
            setTheme(R.style.Theme_AwfulTheme_BYOB);
        }else if(prefs.theme.equals(ColorProvider.YOSPOS)){
            setTheme(R.style.Theme_AwfulTheme_YOSPOS);
        }else if(prefs.theme.equals(ColorProvider.AMBERPOS)){
            setTheme(R.style.Theme_AwfulTheme_AMBERPOS);
        }else{
            setTheme(R.style.Theme_AwfulTheme_Dark);
        }
    }




    /*

        CODE FROM ORIGINAL SETTINGS ACTIVITY

     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SETTINGS_FILE) {
                if(AwfulUtils.isMarshmallow()){
                    int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        this.importData = data;
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.AWFUL_PERMISSION_READ_EXTERNAL_STORAGE);
                    }else{
                        importFile(data);
                    }
                }else {
                    importFile(data);
                }
            }
        }
    }
    protected void importFile(Intent data){
        Toast.makeText(this, "importing settings", Toast.LENGTH_SHORT).show();
        Uri selectedSetting = data.getData();
        String path = getFilePath(selectedSetting);
        if (path != null) {
            File settingsfile = new File(path);
            AwfulPreferences.getInstance(this).importSettings(settingsfile);
            this.finish();
        }
    }


    public String getFilePath(Uri uri) {
        Cursor cursor = null;
        try {
            String[] projection = { MediaStore.Images.Media.DATA };
            cursor = this.getContentResolver().query(uri, projection, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch(NullPointerException e) {
            Toast.makeText(this, "Your file explorer sent incompatible data, please try a different way", Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    @Override
    protected Dialog onCreateDialog(int dialogId) {
        switch(dialogId) {
            case DIALOG_ABOUT:
                CharSequence app_version = getText(R.string.app_name);
                try {
                    app_version = app_version + " " +
                            getPackageManager().getPackageInfo(getPackageName(), 0)
                                    .versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    // rather unlikely, just show app_name without version
                }
                // Build the text for the About dialog
                Resources res = getResources();
                String aboutText = getString(R.string.about_contributors_title) + "\n\n";
                aboutText += StringUtils.join(res.getStringArray(R.array.about_contributors_array), '\n');
                aboutText += "\n\n" + getString(R.string.about_libraries_title) + "\n\n";
                aboutText += StringUtils.join(res.getStringArray(R.array.about_libraries_array), '\n');

                return new AlertDialog.Builder(this)
                        .setTitle(app_version)
                        .setMessage(aboutText)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }})
                        .create();
            default:
                return super.onCreateDialog(dialogId);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
