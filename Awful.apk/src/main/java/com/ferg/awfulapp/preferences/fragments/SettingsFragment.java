package com.ferg.awfulapp.preferences.fragments;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ListView;

import com.ferg.awfulapp.NavigationEvent;
import com.ferg.awfulapp.NavigationEventHandler;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.SettingsActivity;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created by baka kaba on 04/05/2015.
 * <p>
 * <p>Base fragment that adds preferences from a given XML resource,
 * sets defaults and enables options based on the user's device,
 * and sets preference summaries.</p>
 * <p>
 * <p>For some fragments the XML resource ID is all that's required.
 * Others may need to specify preferences etc., or override the
 * initialisation methods to perform more advanced shenanigans.</p>
 * <p>
 * <p>When adding a new fragment, please add its XML resId to
 * {@link SettingsActivity#PREFERENCE_XML_FILES} so it can be
 * automatically checked for defaults!</p>
 */
public abstract class SettingsFragment extends PreferenceFragment implements NavigationEventHandler {

    public static final String TAG = "SettingsFragment";
    private volatile boolean isInflated;
    protected AwfulPreferences mPrefs;
    protected OnSubmenuSelectedListener submenuSelectedListener;
    
    /*
        CONFIGURATION

        The following fields allow you to apply settings and basic
        behaviour to the fragment and its Preference elements, and
        should be initialised during construction.

        These generally involve arrays of String resource IDs, holding the keys
        of the preference elements which will have each behaviour applied.
        So for example, to set a preference to display its value as a
        summary, just add its key to the VALUE_SUMMARY_PREF_KEYS array.

        Custom behaviour can be added by defining click listeners and adding
        them to prefClickListeners, mapping them to the keys of the preferences
        you want to apply the listener to.
     */

    /**
     * <p>This must be set to the resource ID of a layout file containing the fragment's preferences</p>
     * <p>
     * <p>Layout files should describe <b>a single level</b> in the preference hierarchy -
     * don't use the standard {@link android.preference.PreferenceScreen} behaviour to define
     * additional levels, as they will launch a separate activity.</p>
     * <p>
     * <p>Instead, create a separate fragment to hold that content, and define a preference in
     * this layout which will open that fragment when clicked. Set this preference's
     * <i>android:fragment</i> value to this target fragment, and add the preference's key
     * to the SUBMENU_OPENING_KEYS array to enable its click behaviour.
     * See the {@link RootSettings} class for an example</p>
     * <p>
     * <p>(This isn't ideal, it would be better if the click listener was added automatically wherever
     * a fragment value is set on a preference in the XML, so if anyone can handle that cleanly be my guest)</p>
     */
    protected int SETTINGS_XML_RES_ID;

    /**
     * Preferences which should display a submenu fragment when clicked.
     * Set this to an array of preference key ResIDs, and those preferences
     * will display the fragment defined in their <i>android:fragment</i>
     * tag when clicked.
     */
    protected int[] SUBMENU_OPENING_KEYS;

    /**
     * Preferences whose summaries should be set to show their value.
     * Set this to an array of preference key ResIDs, and they will
     * automatically update and display their current value as a summary.
     */
    protected int[] VALUE_SUMMARY_PREF_KEYS;

    /**
     * Preferences whose summaries should reflect their unavailability on the user's version of Android,
     * if applicable. You need to actually disable the preference to mark it as unavailable.
     */
    protected int[] VERSION_DEPENDENT_SUMMARY_PREF_KEYS;

    /**
     * Add any custom onClick listeners here, mapping them to an array of
     * preference keys that the listener should be applied to.
     */
    protected Map<Preference.OnPreferenceClickListener, int[]> prefClickListeners = new ArrayMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = ((SettingsActivity) getActivity()).prefs;

        try {
            addPreferencesFromResource(SETTINGS_XML_RES_ID);
            // only set this flag AFTER the layout is inflated
            isInflated = true;
            initialiseSettings();
            setSummaries();
            registerListeners();
        } catch (Resources.NotFoundException e) {
            Log.w(TAG, "Resource not found while creating fragment: " + e.getMessage());
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // for some reason, if you theme android:listDivider it won't show up in the preference list
        // so doing this directly seems to be the only way to theme it? Can't just get() it either
        ListView listview = (ListView) getView().findViewById(android.R.id.list);
        Drawable divider = getResources().getDrawable(R.drawable.list_divider);
        TypedValue colour = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.R.attr.listDivider, colour, true);
        divider.setColorFilter(colour.data, PorterDuff.Mode.SRC_IN);
        listview.setDivider(divider);
    }


    //
    // Navigation
    //
    // TODO: convert to Kotlin, we only need defaultRoute defined (the interface has default implementations for the others)
    @Override
    public boolean handleNavigation(@NotNull NavigationEvent event) {
        return false;
    }

    @Override
    public void defaultRoute(@NotNull NavigationEvent event) {
        NavigationEventHandler activity = (NavigationEventHandler) getActivity();
        if (activity != null) {
            activity.navigate(event);
        }
    }

    @Override
    public void navigate(@NotNull NavigationEvent event) {
        if (!handleNavigation(event)) defaultRoute(event);
    }



    /**
     * Set required defaults and selectively enable preferences.
     * Override this to perform any custom initialisation in the fragment
     */
    protected void initialiseSettings() {
    }


    /**
     * Get a title for this fragment - this should usually be the same as the label of the preference
     * that opened it, e.g. clicking 'Images' should open a fragment whose title is 'Images'.
     * See <a href="https://material.io/guidelines/patterns/settings.html#settings-grouping-settings">the Material Design specs</a>
     */
    @NonNull
    public abstract String getTitle();


    /**
     * Update and display all summaries, where required.
     * This is called during fragment creation, and should also be called
     * on preference updates.
     */
    public final synchronized void setSummaries() {
        // viewing preferences for the first time initialises them with their
        // defaults, causing a lot of onPreferenceChanged callbacks that
        // trigger this method call. So check the preferences are ready
        if (!isInflated || getActivity() == null) {
            return;
        }
        String keyName;

        // handle standard summary setting
        if (VALUE_SUMMARY_PREF_KEYS != null) {
            for (int keyResId : VALUE_SUMMARY_PREF_KEYS) {
                keyName = getString(keyResId);
                ListPreference pl = (ListPreference) findPreference(keyName);
                pl.setSummary(pl.getEntry());
            }
        }

        // display a 'not on your version' summary if required and the pref has been disabled
        if (VERSION_DEPENDENT_SUMMARY_PREF_KEYS != null) {
            for (int keyResId : VERSION_DEPENDENT_SUMMARY_PREF_KEYS) {
                keyName = getString(keyResId);
                Preference p = findPreference(keyName);
                if (!p.isEnabled()) {
                    p.setSummary(getString(R.string.not_available_on_your_version));
                }
            }
        }
        // run any custom handling in the subclass
        onSetSummaries();
    }

    /**
     * Override this if you want to perform any special handling
     * when a summary update call comes in.
     */
    protected void onSetSummaries() {
    }


    /**
     * Register all the fragment's required listeners to their associated preferences.
     */
    private void registerListeners() {
        // add submenu handling if required
        if (SUBMENU_OPENING_KEYS != null && SUBMENU_OPENING_KEYS.length > 0) {
            prefClickListeners.put(new SubmenuListener(this), SUBMENU_OPENING_KEYS);
        }

        // attach each listener to its associated preferences
        Preference tempPref;
        String keyName;

        for (Map.Entry<Preference.OnPreferenceClickListener, int[]> entry : prefClickListeners.entrySet()) {
            int[] prefKeyIds = entry.getValue();
            if (prefKeyIds != null) {
                Preference.OnPreferenceClickListener listener = entry.getKey();
                for (int keyResId : prefKeyIds) {
                    keyName = getString(keyResId);
                    if ((tempPref = findPreference(keyName)) != null) {
                        tempPref.setOnPreferenceClickListener(listener);
                    } else {
                        Log.w(TAG, "Unable to set click listener on missing preference: " + keyName);
                    }
                }
            }
        }

    }

    @Nullable
    Preference findPrefById(@StringRes int prefKeyResId) {
        return findPreference(getString(prefKeyResId));
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Activities loading this fragment need to handle submenus
        if (activity instanceof OnSubmenuSelectedListener) {
            submenuSelectedListener = (OnSubmenuSelectedListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implement SettingsFragment.OnItemSelectedListener");
        }
    }


    public interface OnSubmenuSelectedListener {
        /**
         * Respond to a click on a preference that opens a submenu
         *
         * @param sourceFragment      The fragment containing the clicked preference
         * @param submenuFragmentName The name of the submenu fragment's class
         */
        void onSubmenuSelected(@NonNull SettingsFragment sourceFragment, @NonNull String submenuFragmentName);
    }


    /**
     * Listener for clicks on options that open submenus
     */
    private class SubmenuListener implements Preference.OnPreferenceClickListener {

        private final SettingsFragment mThis;

        SubmenuListener(SettingsFragment fragment) {
            mThis = fragment;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            submenuSelectedListener.onSubmenuSelected(mThis, preference.getFragment());
            return true;
        }
    }

}
