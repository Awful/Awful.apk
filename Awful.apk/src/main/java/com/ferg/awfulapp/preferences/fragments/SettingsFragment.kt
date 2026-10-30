package com.ferg.awfulapp.preferences.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Resources.NotFoundException
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.collection.ArrayMap
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.RecyclerView
import com.ferg.awfulapp.AwfulActivity
import com.ferg.awfulapp.NavigationEvent
import com.ferg.awfulapp.NavigationEventHandler
import com.ferg.awfulapp.R
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.SettingsActivity
import kotlin.concurrent.Volatile

/**
 * Created by baka kaba on 04/05/2015.
 * 
 * 
 * 
 * Base fragment that adds preferences from a given XML resource,
 * sets defaults and enables options based on the user's device,
 * and sets preference summaries.
 * 
 * 
 * 
 * For some fragments the XML resource ID is all that's required.
 * Others may need to specify preferences etc., or override the
 * initialisation methods to perform more advanced shenanigans.
 * 
 * 
 * 
 * When adding a new fragment, please add its XML resId to
 * [SettingsActivity.PREFERENCE_XML_FILES] so it can be
 * automatically checked for defaults!
 */
abstract class SettingsFragment : PreferenceFragmentCompat(), NavigationEventHandler {
    @Volatile
    private var isInflated = false
    @JvmField
    protected var mPrefs: AwfulPreferences? = null
    protected var submenuSelectedListener: OnSubmenuSelectedListener? = null

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
     * 
     * This must be set to the resource ID of a layout file containing the fragment's preferences
     * 
     * 
     * 
     * Layout files should describe **a single level** in the preference hierarchy -
     * don't use the standard [android.preference.PreferenceScreen] behaviour to define
     * additional levels, as they will launch a separate activity.
     * 
     * 
     * 
     * Instead, create a separate fragment to hold that content, and define a preference in
     * this layout which will open that fragment when clicked. Set this preference's
     * *android:fragment* value to this target fragment, and add the preference's key
     * to the SUBMENU_OPENING_KEYS array to enable its click behaviour.
     * See the [RootSettings] class for an example
     * 
     * 
     * 
     * (This isn't ideal, it would be better if the click listener was added automatically wherever
     * a fragment value is set on a preference in the XML, so if anyone can handle that cleanly be my guest)
     */
    @JvmField
    protected var SETTINGS_XML_RES_ID: Int = 0

    /**
     * Preferences which should display a submenu fragment when clicked.
     * Set this to an array of preference key ResIDs, and those preferences
     * will display the fragment defined in their *android:fragment*
     * tag when clicked.
     */
    @JvmField
    protected var SUBMENU_OPENING_KEYS: IntArray? = null

    /**
     * Preferences whose summaries should be set to show their value.
     * Set this to an array of preference key ResIDs, and they will
     * automatically update and display their current value as a summary.
     */
    @JvmField
    protected var VALUE_SUMMARY_PREF_KEYS: IntArray? = null

    /**
     * Preferences whose summaries should reflect their unavailability on the user's version of Android,
     * if applicable. You need to actually disable the preference to mark it as unavailable.
     */
    protected var VERSION_DEPENDENT_SUMMARY_PREF_KEYS: IntArray? = null

    /**
     * Add any custom onClick listeners here, mapping them to an array of
     * preference keys that the listener should be applied to.
     */
    @JvmField
    protected var prefClickListeners: MutableMap<Preference.OnPreferenceClickListener?, IntArray?> =
        ArrayMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mPrefs = (activity as SettingsActivity).prefs

        try {
            addPreferencesFromResource(SETTINGS_XML_RES_ID)
            // only set this flag AFTER the layout is inflated
            isInflated = true
            initialiseSettings()
            setSummaries()
            registerListeners()
        } catch (e: NotFoundException) {
            Log.w(TAG, "Resource not found while creating fragment: " + e.message)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // for some reason, if you theme android:listDivider it won't show up in the preference list
        // so doing this directly seems to be the only way to theme it? Can't just get() it either
        val divider = resources.getDrawable(R.drawable.list_divider)
        val colour = TypedValue()
        requireActivity().theme.resolveAttribute(android.R.attr.listDivider, colour, true)
        divider.setColorFilter(colour.data, PorterDuff.Mode.SRC_IN)
        setDivider(divider)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val result = super.onCreateView(inflater, container, savedInstanceState)

        (activity as AwfulActivity).setPreferredFont(result)

        return result
    }

    @SuppressLint("RestrictedApi")
    internal inner class AwfulPreferenceAdapter(preferenceGroup: PreferenceGroup) :
        PreferenceGroupAdapter(preferenceGroup) {
        var TAG: String = "MyAdapter"
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreferenceViewHolder {
            val holder = super.onCreateViewHolder(parent, viewType)
            (activity as AwfulActivity).setPreferredFont(holder.itemView)
            return holder
        }
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        return AwfulPreferenceAdapter(preferenceScreen)
    }

    //
    // Navigation
    //
    // TODO: convert to Kotlin, we only need defaultRoute defined (the interface has default implementations for the others)
    override fun handleNavigation(event: NavigationEvent): Boolean {
        return false
    }

    override fun defaultRoute(event: NavigationEvent) {
        val activity = activity as NavigationEventHandler?
        activity?.navigate(event)
    }

    override fun navigate(event: NavigationEvent) {
        if (!handleNavigation(event)) defaultRoute(event)
    }


    /**
     * Set required defaults and selectively enable preferences.
     * Override this to perform any custom initialization in the fragment
     */
    protected open fun initialiseSettings() {
    }


    /**
     * Get a title for this fragment - this should usually be the same as the label of the preference
     * that opened it, e.g. clicking 'Images' should open a fragment whose title is 'Images'.
     * See [the Material Design specs](https://material.io/guidelines/patterns/settings.html#settings-grouping-settings)
     */
    abstract val title: String


    /**
     * Update and display all summaries, where required.
     * This is called during fragment creation, and should also be called
     * on preference updates.
     */
    @Synchronized
    fun setSummaries() {
        // viewing preferences for the first time initializes them with their
        // defaults, causing a lot of onPreferenceChanged callbacks that
        // trigger this method call. So check the preferences are ready
        if (!isInflated || activity == null) {
            return
        }
        var keyName: String?

        // handle standard summary setting
        if (VALUE_SUMMARY_PREF_KEYS != null) {
            for (keyResId in VALUE_SUMMARY_PREF_KEYS) {
                keyName = getString(keyResId)
                val pl = findPreference<Preference?>(keyName) as ListPreference?
                pl?.setSummary(pl.getEntry())
            }
        }

        // display a 'not on your version' summary if required and the pref has been disabled
        if (VERSION_DEPENDENT_SUMMARY_PREF_KEYS != null) {
            for (keyResId in VERSION_DEPENDENT_SUMMARY_PREF_KEYS) {
                keyName = getString(keyResId)
                val p = findPreference<Preference?>(keyName)
                p?.isEnabled?.let {
                    if (!it) {
                        p.setSummary(getString(R.string.not_available_on_your_version))
                    }
                }
            }
        }
        // run any custom handling in the subclass
        onSetSummaries()
    }

    /**
     * Override this if you want to perform any special handling
     * when a summary update call comes in.
     */
    protected open fun onSetSummaries() {
    }


    /**
     * Register all the fragment's required listeners to their associated preferences.
     */
    private fun registerListeners() {
        // add submenu handling if required
        if (SUBMENU_OPENING_KEYS != null && SUBMENU_OPENING_KEYS!!.isNotEmpty()) {
            prefClickListeners[SubmenuListener(this)] = SUBMENU_OPENING_KEYS
        }

        // attach each listener to its associated preferences
        var tempPref: Preference?
        var keyName: String?

        for ((listener, prefKeyIds) in prefClickListeners) {
            if (prefKeyIds != null) {
                for (keyResId in prefKeyIds) {
                    keyName = getString(keyResId)
                    if ((findPreference<Preference?>(keyName).also { tempPref = it }) != null) {
                        tempPref?.onPreferenceClickListener = listener
                    } else {
                        Log.w(TAG, "Unable to set click listener on missing preference: " + keyName)
                    }
                }
            }
        }
    }

    fun findPrefById(@StringRes prefKeyResId: Int): Preference? {
        return findPreference(getString(prefKeyResId))
    }


    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        // Activities loading this fragment need to handle submenus
        if (activity is OnSubmenuSelectedListener) {
            submenuSelectedListener = activity as OnSubmenuSelectedListener
        } else {
            throw ClassCastException(
                activity.toString()
                        + " must implement SettingsFragment.OnItemSelectedListener"
            )
        }
    }


    interface OnSubmenuSelectedListener {
        /**
         * Respond to a click on a preference that opens a submenu
         * 
         * @param sourceFragment      The fragment containing the clicked preference
         * @param submenuFragmentName The name of the submenu fragment's class
         */
        fun onSubmenuSelected(sourceFragment: SettingsFragment, submenuFragmentName: String)
    }


    /**
     * Listener for clicks on options that open submenus
     */
    private inner class SubmenuListener(private val mThis: SettingsFragment) :
        Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            submenuSelectedListener?.onSubmenuSelected(mThis, preference.fragment!!)
            return true
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        return
    }

    companion object {
        const val TAG: String = "SettingsFragment"
    }
}
