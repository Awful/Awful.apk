package com.ferg.awfulapp.announcements;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.AwfulApplication;
import com.ferg.awfulapp.BasicActivity;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.ThreadListRequest;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Created by baka kaba on 25/01/2017.
 * <p>
 * Central point for managing and viewing announcements.
 * <p>
 * This class parses thread pages (sent from {@link com.ferg.awfulapp.task.ThreadListRequest})
 * looking for announcements, compares this list to the last set parsed (to determine if any are
 * new), and stores these as the current announcements. Listeners can register for updates when
 * a new announcement is parsed.
 * <p>
 * {@link #showAnnouncements(Activity)} displays the current announcements and records the last time
 * the user viewed them. This is used with {@link #getUnreadCount()} to determine how many of the
 * current announcements were found since the last view.
 * <p>
 * I don't really like this system, it feels kind of brittle - if announcements can update (I don't
 * know if they can) without the title changing, the change won't be spotted. And if a title is
 * changed it will look like a new announcement. I don't know if either of these are issues, but
 * that's the drawback. I was going to use the timestamps (which also aren't ideal) but it turns out
 * they randomly jump around timezones when the page is loaded, which is <i>real nice</i>
 */

public class AnnouncementsManager {

    // SharedPreference constants for storing state
    private static final String PREF_KEY_CURRENT_ANNOUNCEMENTS = "CURRENT_ANNOUNCEMENTS";
    private static final String PREF_KEY_READ_ANNOUNCEMENTS = "READ_ANNOUNCEMENTS";

    private static AnnouncementsManager manager = null;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @NonNull
    private final Map<AnnouncementListener, Object> callbacks = Collections.synchronizedMap(new WeakHashMap<AnnouncementListener, Object>());

    /**
     * Used to synchronize access to current state, i.e. current and unread announcements, and the 'updated from site' flag
     */
    private final Object stateLock = new Object();
    /**
     * The current announcements marked as read - this is a subset of currentAnnouncements
     */
    @NonNull
    private final Set<String> readAnnouncements = new HashSet<>();
    private boolean hasUpdatedFromSite = false;
    /**
     * The last set of announcements parsed
     */
    @NonNull
    private Set<String> currentAnnouncements = new HashSet<>();


    private AnnouncementsManager() {
    }

    /**
     * Initialise the AnnouncementsManager singleton, restoring state etc.
     */
    public static void init() {
        if (manager == null) {
            manager = new AnnouncementsManager();
            manager.restoreState();
        }
    }

    /**
     * Get the AnnouncementsManager singleton instance. You must call {@link #init()} first!
     */
    public static AnnouncementsManager getInstance() {
        if (manager == null) {
            throw new RuntimeException("AnnouncementsManager needs to be initialised with init() before use!");
        }
        return manager;
    }


    /**
     * Parse a <b>forum page (thread list)</b> to look for announcements.
     * <p>
     * <b>IMPORTANT: </b> this treats forum pages as the authority on announcements,
     * i.e. if there are any announcements they <b>always</b> appear on forum pages, and if there
     * aren't any present then there are no announcements.
     * <p>
     * If you pass in a different kind of page (e.g. the bookmarked thread list) they won't be present,
     * so the current announcements will be cleared. If you then pass in a forum page, containing
     * announcements, they'll all be treated as newly posted and trigger a notification.
     * Forum pages only, thanks!
     */
    public void parseForumPage(@NonNull Document page) {
        Set<String> parsedAnnouncements = new HashSet<>();
        int newCount = 0;
        int oldUnreadCount = 0;
        int oldReadCount = 0;
        boolean isFirstUpdate;
        boolean announcementsHaveChanged = false;

        // pull out all the announcement threads - timestamps jump forward and back in time at random so we'll have to ID by title
        Elements announcementElements = page.select("#forum a.announcement");
        synchronized (stateLock) {
            for (Element announcement : announcementElements) {
                // build a list of announcement titles, checking if they're known or new, and counting each type
                String title = announcement.text();
                parsedAnnouncements.add(title);
                if (readAnnouncements.contains(title)) {
                    oldReadCount++;
                } else if (currentAnnouncements.contains(title)) {
                    oldUnreadCount++;
                } else {
                    newCount++;
                }
            }

            // store the announcements we parsed, forget any read announcements that no longer exist
            // only doing this if something's changed, to avoid unnecessary work
            if (currentAnnouncements != parsedAnnouncements) {
                announcementsHaveChanged = true;
                currentAnnouncements = parsedAnnouncements;
                readAnnouncements.retainAll(currentAnnouncements);
                saveState();
            }

            isFirstUpdate = !hasUpdatedFromSite;
            hasUpdatedFromSite = true;
        }

        // notify about any changes
        if (isFirstUpdate || announcementsHaveChanged) {
            publishState(newCount, oldUnreadCount, oldReadCount, isFirstUpdate);
        }
    }


    /**
     * Publish an announcement state update to all listeners.
     * <p>
     * This provides each listener with the latest counts for each announcement type.
     * See {@link AnnouncementListener#onAnnouncementsUpdated(int, int, int, boolean)}
     * for details on isFirstUpdate
     *
     * @param newCount       the number of new, previously unseen messages found
     * @param oldUnreadCount the number of old unread messages (i.e. excluding new)
     * @param oldReadCount   the number of previously read messages
     * @param isFirstUpdate  true if this is the first update from the site for this app session
     */
    private void publishState(int newCount, int oldUnreadCount, int oldReadCount, boolean isFirstUpdate) {
        for (AnnouncementListener listener : callbacks.keySet()) {
            handler.post(() -> listener.onAnnouncementsUpdated(newCount, oldUnreadCount, oldReadCount, isFirstUpdate));
        }
    }


    /**
     * Register a listener for new announcement updates.
     * <p/>
     * Only holds a weak reference, so keep your own reference if necessary.
     */
    public void registerListener(@NonNull AnnouncementListener listener) {
        callbacks.put(listener, new Object());
    }


    /**
     * The number of currently unread announcements.
     * <p>
     * This is the unread total, i.e. those found since the user last viewed the announcements page.
     */
    public int getUnreadCount() {
        synchronized (stateLock) {
            // readAnnouncements is always a subset of currentAnnouncements
            return currentAnnouncements.size() - readAnnouncements.size();
        }
    }


    /**
     * Launch the Announcements activity.
     *
     * @param activity used to launch the new activity
     */
    public void showAnnouncements(@NonNull Activity activity) {
        Intent intent = BasicActivity.Companion.intentFor(AnnouncementsFragment.class, activity, activity.getString(R.string.announcements));
        activity.startActivity(intent);
    }


    /**
     * Set all current announcements as read.
     * <p>
     * Call this when they've successfully been displayed. If a new announcement appears after this manager's
     * latest update, and the user views it, this method won't be aware of the announcement and it will
     * appear as a new, unread announcement on the next parse. Not ideal but it's the best we can really do
     * right now, and it shouldn't come up much.
     */
    void markAllRead() {
        int totalRead;
        synchronized (stateLock) {
            readAnnouncements.clear();
            readAnnouncements.addAll(currentAnnouncements);
            totalRead = readAnnouncements.size();
            saveState();
        }
        publishState(0, 0, totalRead, false);
    }


    /**
     * Persist the current announcement data, so it can be restored on app reload.
     */
    private void saveState() {
        SharedPreferences.Editor appState = AwfulApplication.getAppStatePrefs().edit();
        synchronized (stateLock) {
            appState.putStringSet(PREF_KEY_CURRENT_ANNOUNCEMENTS, currentAnnouncements)
                    .putStringSet(PREF_KEY_READ_ANNOUNCEMENTS, readAnnouncements);
            appState.apply();
        }
    }


    /**
     * Restore saved announcement data, if it hasn't already been done.
     * <p>
     * This loads the persisted state written by {@link #saveState()}, and should be called as early
     * as possible to retain the last-known state and update on top of that.
     */
    private void restoreState() {
        SharedPreferences appState = AwfulApplication.getAppStatePrefs();
        synchronized (stateLock) {
            // can't just use the returned sets apparently!
            currentAnnouncements.clear();
            readAnnouncements.clear();
            currentAnnouncements.addAll(appState.getStringSet(PREF_KEY_CURRENT_ANNOUNCEMENTS, Collections.emptySet()));
            readAnnouncements.addAll(appState.getStringSet(PREF_KEY_READ_ANNOUNCEMENTS, Collections.emptySet()));
        }
    }


    /**
     * Wipes all stored announcement data
     */
    public void clearState() {
        synchronized (stateLock) {
            currentAnnouncements.clear();
            readAnnouncements.clear();
            saveState();
        }
    }


    /**
     * Check the site to update the current Announcement status
     */
    public static void updateAnnouncements(@NonNull Context context) {
        // loading any forum will trigger an announcement parse - SH/SC is *probably* a stable ID, unlike say GBS
        NetworkUtils.queueRequest(new ThreadListRequest(context, Constants.FORUM_ID_SHSC, 1).build(null, new AwfulRequest.AwfulResultCallback<Void>() {
            @Override
            public void success(Void result) {

            }

            @Override
            public void failure(VolleyError error) {
                String message = "Couldn't update announcements:\n" + error.getMessage();
                if (Constants.DEBUG) {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                }
                Log.w(AnnouncementsManager.class.getSimpleName(), message);
            }
        }));
    }


    public interface AnnouncementListener {
        /**
         * Called when the known announcement state changes
         * <p>
         * 'New' means a previously unseen announcement link, or one that appears to have updated.
         * On the next parse it will be treated as an 'old' announcement, so newCount
         * should be treated as a notification that some new announcements have been found.
         * <p>
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
        void onAnnouncementsUpdated(int newCount, int oldUnread, int oldRead, boolean isFirstUpdate);
    }
}
