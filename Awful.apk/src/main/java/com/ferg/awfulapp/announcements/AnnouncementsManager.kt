package com.ferg.awfulapp.announcements

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.UiThread
import com.android.volley.VolleyError
import com.ferg.awfulapp.AwfulApplication.Companion.appStatePrefs
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.task.AwfulRequest.AwfulResultCallback
import com.ferg.awfulapp.task.ThreadListRequest
import org.jsoup.nodes.Document
import java.util.Collections
import java.util.WeakHashMap

/**
 * Created by baka kaba on 25/01/2017.
 * 
 * 
 * Central point for managing and viewing announcements.
 * 
 * 
 * This class parses thread pages (sent from [ThreadListRequest])
 * looking for announcements, compares this list to the last set parsed (to determine if any are
 * new), and stores these as the current announcements. Listeners can register for updates when
 * a new announcement is parsed.
 * 
 * 
 * [com.ferg.awfulapp.NavigationEvent.Announcements] displays the current announcements and
 * records the last time * the user viewed them. This is used with [.getUnreadCount] to
 * determine how many of the current announcements were found since the last view.
 * 
 * 
 * I don't really like this system, it feels kind of brittle - if announcements can update (I don't
 * know if they can) without the title changing, the change won't be spotted. And if a title is
 * changed it will look like a new announcement. I don't know if either of these are issues, but
 * that's the drawback. I was going to use the timestamps (which also aren't ideal) but it turns out
 * they randomly jump around timezones when the page is loaded, which is *real nice*
 */
class AnnouncementsManager private constructor() {
    private val handler = Handler(Looper.getMainLooper())

    private val callbacks: MutableMap<AnnouncementListener, Any?> =
        Collections.synchronizedMap<AnnouncementListener, Any?>(
            WeakHashMap<AnnouncementListener?, Any?>()
        )

    /**
     * Used to synchronize access to current state, i.e. current and unread announcements, and the 'updated from site' flag
     */
    private val stateLock = Any()

    /**
     * The current announcements marked as read - this is a subset of currentAnnouncements
     */
    private val readAnnouncements: MutableSet<String?> = HashSet()
    private var hasUpdatedFromSite = false

    /**
     * The last set of announcements parsed
     */
    private var currentAnnouncements: MutableSet<String?> = HashSet()


    /**
     * Parse a **forum page (thread list)** to look for announcements.
     * 
     * 
     * **IMPORTANT: ** this treats forum pages as the authority on announcements,
     * i.e. if there are any announcements they **always** appear on forum pages, and if there
     * aren't any present then there are no announcements.
     * 
     * 
     * If you pass in a different kind of page (e.g. the bookmarked thread list) they won't be present,
     * so the current announcements will be cleared. If you then pass in a forum page, containing
     * announcements, they'll all be treated as newly posted and trigger a notification.
     * Forum pages only, thanks!
     */
    fun parseForumPage(page: Document) {
        val parsedAnnouncements: MutableSet<String?> = HashSet()
        var newCount = 0
        var oldUnreadCount = 0
        var oldReadCount = 0
        val isFirstUpdate: Boolean
        var announcementsHaveChanged = false

        // pull out all the announcement threads - timestamps jump forward and back in time at random so we'll have to ID by title
        val announcementElements = page.select("#forum a.announcement")
        synchronized(stateLock) {
            for (announcement in announcementElements) {
                // build a list of announcement titles, checking if they're known or new, and counting each type
                val title = announcement.text()
                parsedAnnouncements.add(title)
                if (readAnnouncements.contains(title)) {
                    oldReadCount++
                } else if (currentAnnouncements.contains(title)) {
                    oldUnreadCount++
                } else {
                    newCount++
                }
            }
            // store the announcements we parsed, forget any read announcements that no longer exist
            // only doing this if something's changed, to avoid unnecessary work
            if (currentAnnouncements !== parsedAnnouncements) {
                announcementsHaveChanged = true
                currentAnnouncements = parsedAnnouncements
                readAnnouncements.retainAll(currentAnnouncements)
                saveState()
            }

            isFirstUpdate = !hasUpdatedFromSite
            hasUpdatedFromSite = true
        }

        // notify about any changes
        if (isFirstUpdate || announcementsHaveChanged) {
            publishState(newCount, oldUnreadCount, oldReadCount, isFirstUpdate)
        }
    }


    /**
     * Publish an announcement state update to all listeners.
     * 
     * 
     * This provides each listener with the latest counts for each announcement type.
     * See [AnnouncementListener.onAnnouncementsUpdated]
     * for details on isFirstUpdate
     * 
     * @param newCount       the number of new, previously unseen messages found
     * @param oldUnreadCount the number of old unread messages (i.e. excluding new)
     * @param oldReadCount   the number of previously read messages
     * @param isFirstUpdate  true if this is the first update from the site for this app session
     */
    private fun publishState(
        newCount: Int,
        oldUnreadCount: Int,
        oldReadCount: Int,
        isFirstUpdate: Boolean
    ) {
        for (listener in callbacks.keys) {
            handler.post {
                listener.onAnnouncementsUpdated(
                    newCount,
                    oldUnreadCount,
                    oldReadCount,
                    isFirstUpdate
                )
            }
        }
    }


    /**
     * Register a listener for new announcement updates.
     * 
     * 
     * Only holds a weak reference, so keep your own reference if necessary.
     */
    fun registerListener(listener: AnnouncementListener) {
        callbacks[listener] = Any()
    }


    val unreadCount: Int
        /**
         * The number of currently unread announcements.
         * 
         * 
         * This is the unread total, i.e. those found since the user last viewed the announcements page.
         */
        get() {
            synchronized(stateLock) {
                // readAnnouncements is always a subset of currentAnnouncements
                return currentAnnouncements.size - readAnnouncements.size
            }
        }


    /**
     * Set all current announcements as read.
     * 
     * 
     * Call this when they've successfully been displayed. If a new announcement appears after this manager's
     * latest update, and the user views it, this method won't be aware of the announcement and it will
     * appear as a new, unread announcement on the next parse. Not ideal but it's the best we can really do
     * right now, and it shouldn't come up much.
     */
    fun markAllRead() {
        val totalRead: Int
        synchronized(stateLock) {
            readAnnouncements.clear()
            readAnnouncements.addAll(currentAnnouncements)
            totalRead = readAnnouncements.size
            saveState()
        }
        publishState(0, 0, totalRead, false)
    }


    /**
     * Persist the current announcement data, so it can be restored on app reload.
     */
    private fun saveState() {
        val appState = appStatePrefs!!.edit()
        synchronized(stateLock) {
            appState.putStringSet(PREF_KEY_CURRENT_ANNOUNCEMENTS, currentAnnouncements)
                .putStringSet(PREF_KEY_READ_ANNOUNCEMENTS, readAnnouncements)
            appState.apply()
        }
    }


    /**
     * Restore saved announcement data, if it hasn't already been done.
     * 
     * 
     * This loads the persisted state written by [.saveState], and should be called as early
     * as possible to retain the last-known state and update on top of that.
     */
    private fun restoreState() {
        val appState = appStatePrefs
        synchronized(stateLock) {
            // can't just use the returned sets apparently!
            currentAnnouncements.clear()
            readAnnouncements.clear()
            currentAnnouncements.addAll(
                appState!!.getStringSet(
                    PREF_KEY_CURRENT_ANNOUNCEMENTS,
                    kotlin.collections.mutableSetOf<String?>()
                )!!
            )
            readAnnouncements.addAll(
                appState.getStringSet(
                    PREF_KEY_READ_ANNOUNCEMENTS,
                    kotlin.collections.mutableSetOf<String?>()
                )!!
            )
        }
    }


    interface AnnouncementListener {
        /**
         * Called when the known announcement state changes
         * 
         * 
         * 'New' means a previously unseen announcement link, or one that appears to have updated.
         * On the next parse it will be treated as an 'old' announcement, so newCount
         * should be treated as a notification that some new announcements have been found.
         * 
         * 
         * The isFirstUpdate flag is set to true for the first update from the site following an
         * app restart. This lets listeners handle the initial state report differently from other
         * updates (e.g. displaying an information popup when the app is first opened)
         * 
         * @param newCount      the number of newly found announcements
         * @param oldUnread     the number of old, unread announcements
         * @param oldRead       the number of old, previously read announcements
         * @param isFirstUpdate true if this is the initial update from the site
         */
        @UiThread
        fun onAnnouncementsUpdated(
            newCount: Int,
            oldUnread: Int,
            oldRead: Int,
            isFirstUpdate: Boolean
        )
    }

    companion object {
        // SharedPreference constants for storing state
        private const val PREF_KEY_CURRENT_ANNOUNCEMENTS = "CURRENT_ANNOUNCEMENTS"
        private const val PREF_KEY_READ_ANNOUNCEMENTS = "READ_ANNOUNCEMENTS"

        private var manager: AnnouncementsManager? = null

        /**
         * Initialise the AnnouncementsManager singleton, restoring state etc.
         */
        fun init() {
            if (manager == null) {
                manager = AnnouncementsManager()
                manager!!.restoreState()
            }
        }
        /**
         * Get the AnnouncementsManager singleton instance. You must call [.init] first!
         */
        fun getInstance(): AnnouncementsManager {
            if (manager == null) {
                throw RuntimeException("AnnouncementsManager needs to be initialised with init() before use!")
            }
            return manager!!
        }


        /**
         * Check the site to update the current Announcement status
         */
        @JvmStatic
        fun updateAnnouncements(context: Context) {
            // loading any forum will trigger an announcement parse - SH/SC is *probably* a stable ID, unlike say GBS
            NetworkUtils.queueRequest(
                ThreadListRequest(context, Constants.FORUM_ID_SHSC, 1).build(
                    null,
                    object : AwfulResultCallback<Void?> {
                        override fun success(result: Void?) {
                        }

                        override fun failure(error: VolleyError?) {
                            val message = "Couldn't update announcements:\n" + error?.message
                            if (Constants.DEBUG) {
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                            Log.w(AnnouncementsManager::class.java.simpleName, message)
                        }
                    })
            )
        }
    }
}
