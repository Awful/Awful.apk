package com.ferg.awfulapp.preferences.fragments

import androidx.annotation.UiThread
import androidx.preference.Preference
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.forums.CrawlerTask
import com.ferg.awfulapp.forums.ForumRepository
import com.ferg.awfulapp.forums.ForumRepository.Companion.getInstance
import com.ferg.awfulapp.forums.ForumRepository.ForumsUpdateListener
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.concurrent.Volatile

/**
 * Created by baka kaba on 19/04/2016.
 * 
 * 
 * Settings relating to the forum index.
 */
class ForumIndexSettings : SettingsFragment(), ForumsUpdateListener {
    private var forumRepo: ForumRepository = getInstance(null)

    init {
        SETTINGS_XML_RES_ID = R.xml.forum_index_settings

        prefClickListeners[UpdateForumsListener()] = intArrayOf(
            R.string.pref_key_update_forums_menu_item
        )

        prefClickListeners[Preference.OnPreferenceClickListener {
            forumRepo.clearForumData()
            true
        }] = intArrayOf(R.string.pref_key_clear_forums_data_menu_item)
    }

    @Volatile
    private var updateRunning = false


    override val title: String
        get() = getString(R.string.forum_index_settings)


    override fun initialiseSettings() {
        super.initialiseSettings()
        if (!Constants.DEBUG) {
            val clearPref = findPrefById(R.string.pref_key_clear_forums_data_menu_item)
            if (clearPref != null) {
                preferenceScreen.removePreference(clearPref)
            }
        }
    }


    override fun onResume() {
        super.onResume()
        // assume we're not updating, if we are then the on-register callback will fix it
        updateRunning = false
        setUpdateForumsSummary()
        forumRepo.registerListener(this)
    }


    override fun onPause() {
        super.onPause()
        forumRepo.unregisterListener(this)
    }


    override fun onForumsUpdateStarted() {
        handleForumUpdateCallback(true)
    }


    override fun onForumsUpdateCompleted(success: Boolean) {
        handleForumUpdateCallback(false)
    }


    override fun onForumsUpdateCancelled() {
        handleForumUpdateCallback(false)
    }


    private fun handleForumUpdateCallback(running: Boolean) {
        updateRunning = running
        requireActivity().runOnUiThread { this.setUpdateForumsSummary() }
    }


    @UiThread
    private fun setUpdateForumsSummary() {
        val updatePref = findPrefById(R.string.pref_key_update_forums_menu_item)
        if (updatePref != null) {
            if (updateRunning) {
                updatePref.setSummary(R.string.forum_index_update_forums_summary_updating)
            } else {
                val lastUpdateMessage = requireActivity().resources
                    .getString(R.string.forum_index_update_forums_summary_not_updating)
                val timeUnit = TimeUnit.HOURS
                val lastUpdate = System.currentTimeMillis() - forumRepo.lastRefreshTime
                val `when` = timeUnit.convert(lastUpdate, TimeUnit.MILLISECONDS)
                updatePref.setSummary(
                    String.format(
                        lastUpdateMessage, `when`, timeUnit.toString().lowercase(
                            Locale.getDefault()
                        )
                    )
                )
            }
        }
    }


    private inner class UpdateForumsListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            // TODO: maybe move this into a full sync button somewhere, that does forum features etc
            forumRepo.updateForums(CrawlerTask(requireActivity(), CrawlerTask.Priority.HIGH))
            return true
        }
    }
}
