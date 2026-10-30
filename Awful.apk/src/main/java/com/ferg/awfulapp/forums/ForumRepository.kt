package com.ferg.awfulapp.forums

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import androidx.core.content.edit
import com.android.volley.VolleyError
import com.ferg.awfulapp.AwfulApplication.Companion.appStatePrefs
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.StringPreference
import com.ferg.awfulapp.provider.AwfulProvider
import com.ferg.awfulapp.provider.DatabaseHelper
import com.ferg.awfulapp.task.AwfulRequest.AwfulResultCallback
import com.ferg.awfulapp.task.IndexIconRequest
import com.ferg.awfulapp.task.IndexIconRequest.Companion.REQUEST_TAG
import com.ferg.awfulapp.thread.AwfulForum
import org.apache.commons.lang3.StringUtils
import java.sql.Timestamp
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.concurrent.Volatile

/**
 * Created by baka kaba on 04/04/2016.
 * 
 * 
 * Provides access to current forum state, forcing updates etc.
 */
class ForumRepository private constructor(context: Context) : UpdateTask.ResultListener {
    /**
     * Synchronization lock for accessing currentUpdateTask
     */
    private val updateLock = Any()

    /**
     * The current update task, if any
     */
    @Volatile
    private var currentUpdateTask: UpdateTask? = null

    // using a COW array to make listener de/registration and iteration ~fairly~ thread-safe
    private val listeners: MutableSet<ForumsUpdateListener> =
        CopyOnWriteArraySet()
    private val context: Context = context.applicationContext

    fun registerListener(listener: ForumsUpdateListener) {
        listeners.add(listener)
        // let the new listener know if there's an update in progress
        if (this.isUpdating) {
            listener.onForumsUpdateStarted()
        }
    }


    fun unregisterListener(listener: ForumsUpdateListener) {
        listeners.remove(listener)
    }


    /////////////////////////////////////////////////////////////////////////
    // Update task lifecycle
    /////////////////////////////////////////////////////////////////////////
    /**
    * Cancel any running forum updates.
    */
    fun cancelUpdate() {
        synchronized(updateLock) {
            if (currentUpdateTask == null) {
                Log.d(TAG, "cancelUpdate: no task running")
                return
            }
            Log.w(TAG, "Cancelling an update in progress")
            val cancelledTask = currentUpdateTask
            currentUpdateTask = null
            cancelledTask?.cancel()
            NetworkUtils.cancelRequests(REQUEST_TAG)
        }
        for (listener in listeners) {
            listener.onForumsUpdateCancelled()
        }
    }


    /**
     * Update the current forum list in the background.
     * Does nothing if an update is already in progress
     * 
     * @param updateTask The type of update to perform
     */
    fun updateForums(updateTask: UpdateTask) {
        // synchronize to make sure only one update can start
        synchronized(updateLock) {
            if (currentUpdateTask != null) {
                Log.w(TAG, "Tried to refresh forums while the task was already running!")
                return
            }
            currentUpdateTask = updateTask
            currentUpdateTask?.execute(this)
        }
        for (listener in listeners) {
            listener.onForumsUpdateStarted()
        }
    }


    override fun onRefreshCompleted(
        task: UpdateTask,
        success: Boolean,
        forumStructure: ForumStructure?
    ) {
        synchronized(updateLock) {
            // we're only interested in the current task, so ignore anything else that might pop up
            if (task !== currentUpdateTask) {
                Log.w(TAG, "onRefreshCompleted: not the current update task, ignoring")
                return
            }
            if (success && forumStructure != null) {
                storeForumData(forumStructure)
                refreshTags(task)
            } else {
                onUpdateComplete(task, false)
            }
        }
    }


    /**
     * Refresh the forum tags.
     * This needs to be done after the forum hierarchy has been rebuilt, since it updates the new records
     * 
     * @param updateTask The update task that triggered the tag refresh
     */
    private fun refreshTags(updateTask: UpdateTask) {
        NetworkUtils.queueRequest(
            IndexIconRequest(context).build(
                null,
                object : AwfulResultCallback<Void?> {
                    override fun success(result: Void?) {
                        onUpdateComplete(updateTask, true)
                    }


                    override fun failure(error: VolleyError?) {
                        onUpdateComplete(updateTask, false)
                    }
                })
        )
    }


    /**
     * Called when the task is complete (whether successful or not)
     * 
     * @param updateTask The task that has finished
     */
    private fun onUpdateComplete(updateTask: UpdateTask, updateSuccessful: Boolean) {
        synchronized(updateLock) {
            if (updateTask !== currentUpdateTask) {
                Log.w(TAG, "onUpdateComplete: not the current task, ignoring")
                return
            }
            currentUpdateTask = null
        }
        for (listener in listeners) {
            listener.onForumsUpdateCompleted(updateSuccessful)
        }
    }


    val isUpdating: Boolean
        /**
         * Check if a forums data update is in progress.
         */
        get() = currentUpdateTask != null


    /////////////////////////////////////////////////////////////////////////
    // Get forums data
    /////////////////////////////////////////////////////////////////////////
    /**
    * Check if the repo currently has any forum data .
    * This is a quick way of determining if an update needs to run (e.g. after data wipe)
    */
    fun hasForumData(): Boolean {
        val cursor = getForumsCursor(null)
        val numForums = cursor?.count ?: 0
        cursor?.close()
        return numForums > 0
    }


    var lastRefreshTime: Long
        /**
         * Get the timestamp of the last successful full update.
         * 
         * @return the timestamp in milliseconds, or 0 if there is no forum data
         * @see System.currentTimeMillis
         */
        get() {
            // forum data may be updated (with timestamps) after a full refresh, so we need to keep a separate timestamp
            val prefs = appStatePrefs
            return prefs!!.getLong(PREF_KEY_FORUM_REFRESH_TIMESTAMP, 0)
        }
        /**
         * Store the last time the forums were fully refreshed
         * 
         * @param timestamp the time to set in millis
         */
        private set(timestamp) {
            var timestamp = timestamp
            timestamp = if (timestamp < 0) 0 else timestamp
            val prefs = appStatePrefs
            prefs?.edit {
                putLong(PREF_KEY_FORUM_REFRESH_TIMESTAMP, timestamp)
            }
        }


    val allForums: ForumStructure
        get() = ForumStructure.buildFromOrderedList(
            loadForumData(getForumsCursor(null)),
            TOP_LEVEL_PARENT_ID
        )


    val favouriteForums: ForumStructure
        /**
         * Get the user's current favourited forums as a ForumStructure.
         * 
         * 
         * These are ordered by stored index, i.e. in order of appearance in the full forum list.
         */
        get() {
            val favourites =
                loadForumData(getForumsCursor(favouriteForumIds))
            return ForumStructure.buildFromOrderedList(favourites, null)
        }


    /**
     * Toggle a forum's favourite status.
     * 
     * 
     * This relies on the internal favourites state and ignores the current [Forum.isFavourite] value.
     * The Forum is updated to reflect the new state.
     * 
     * @param forum The forum to add or remove
     */
    fun toggleFavorite(forum: Forum) {
        // generate a new set of favourite forum IDs by removing or adding the toggled one
        val favourites: MutableList<String> = mutableListOf(*favouriteForumIds)
        val forumId = forum.id.toString()
        if (favourites.remove(forumId)) {
            forum.isFavourite = false
        } else {
            // we don't handle custom ordering (see #getFavouriteForums) so we can just add anywhere
            favourites.add(forumId)
            forum.isFavourite = true
        }
        setFavouriteForumIds(favourites)
    }


    /////////////////////////////////////////////////////////////////////////
    // Database operations
    /////////////////////////////////////////////////////////////////////////
    /**
    * Remove all cached forum data from the DB.
    */
    fun clearForumData() {
        val contentResolver = context.contentResolver
        contentResolver.delete(AwfulForum.CONTENT_URI, null, null)
        this.lastRefreshTime = 0
    }


    /**
     * Get Forum records, ordered by [AwfulForum.INDEX] as stored.
     * 
     * @param forumIds the IDs of the forums you want, or null to return all forums
     * @see .storeForumData
     */
    private fun getForumsCursor(forumIds: Array<String>?): Cursor? {
        val contentResolver = context.contentResolver
        // if we have some IDs we need to build a WHERE query, otherwise leave both null to get everything
        var where: String? = null
        if (forumIds != null) {
            val placeholders = StringUtils.repeat("?", ",", forumIds.size)
            where = AwfulForum.ID + " IN (" + placeholders + ")"
        }

        // get the required forums, ordered by index (the order they were added to the DB)
        return contentResolver.query(
            AwfulForum.CONTENT_URI,
            AwfulProvider.ForumProjection,
            where,
            forumIds,
            AwfulForum.INDEX
        )
    }


    /**
     * Store the current page count for a forum
     */
    fun setPageCount(forumId: Int, pageCount: Int) {
        // TODO: 08/02/2017 need a more general way to update various bit of data, maybe passing a Forum object
        var pageCount = pageCount
        pageCount = if (pageCount < 1) 1 else pageCount
        val forumData = ContentValues(2)
        forumData.put(AwfulForum.PAGE_COUNT, pageCount)
        forumData.put(DatabaseHelper.UPDATED_TIMESTAMP, this.timestamp)

        val contentResolver = context.contentResolver
        val uri = ContentUris.withAppendedId(AwfulForum.CONTENT_URI, forumId.toLong())
        if (contentResolver.update(uri, forumData, null, null) < 1) {
            Log.w(TAG, "Unknown forum ID " + forumId + " while trying to update page count")
        }
    }


    /**
     * Build a list of Forum objects from a list of forum records, ordered by index.
     * See [.storeForumData] for details on index ordering.
     * 
     * @param cursor a cursor over the required forum records
     * @return The resulting list of Forums
     */
    private fun loadForumData(cursor: Cursor?): MutableList<Forum?> {
        val forumList: MutableList<Forum?> = ArrayList()
        if (cursor == null) {
            return forumList
        }

        var forum: Forum?
        val favouriteForumIds = listOf<String?>(*favouriteForumIds)
        while (cursor.moveToNext()) {
            forum = Forum(
                cursor.getInt(cursor.getColumnIndexOrThrow(AwfulForum.ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(AwfulForum.PARENT_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(AwfulForum.TITLE)),
                cursor.getString(cursor.getColumnIndexOrThrow(AwfulForum.SUBTEXT))
            )
            // the forum might have an image tag too
            val tagUrl = cursor.getString(cursor.getColumnIndexOrThrow(AwfulForum.TAG_URL))
            forum.tagUrl = tagUrl

            // set favourite status by checking the favourites list
            forum.isFavourite = favouriteForumIds.contains(forum.id.toString())

            // set the type e.g. for the index list to handle formatting
            if (forum.id == Constants.USERCP_ID) {
                forum.type = ForumType.BOOKMARKS
            } else if (forum.parentId == TOP_LEVEL_PARENT_ID) {
                forum.type = ForumType.SECTION
            }
            forumList.add(forum)
        }
        cursor.close()
        return forumList
    }


    /**
     * Store a list of Forums in the DB.
     * The forums will be assigned an index in the order they're passed in. This index is used
     * to determine the order a group of forums should be displayed in, e.g. a flat list of all
     * forums, or within a list of subforums.
     * 
     * @param parsedStructure The forum hierarchy
     */
    private fun storeForumData(parsedStructure: ForumStructure) {
        // we're replacing all the forums, so wipe them
        clearForumData()
        val timestamp = System.currentTimeMillis()
        this.lastRefreshTime = timestamp
        val updateTime = Timestamp(timestamp).toString()
        val allForums: MutableList<Forum> = ArrayList()

        // add any special forums not on the main hierarchy
        val bookmarks = Forum(Constants.USERCP_ID, TOP_LEVEL_PARENT_ID, "Bookmarks", "")
        allForums.add(bookmarks)

        // get all the parsed forums in an ordered list, so we can store them in this order using the INDEX field
        allForums.addAll(
            parsedStructure.asList.includeSections(true).formatAs(ForumStructure.ListFormat.FLAT).build()
        )

        val contentResolver = context.contentResolver
        contentResolver.bulkInsert(
            AwfulForum.CONTENT_URI,
            getAsContentValues(allForums, updateTime)
        )
    }


    // TODO: 06/02/2017 a way to push a forum in (for updates, esp page counts - aren't implemented in Forum yet)
    // indexes are a problem - they're used to order forums (keeping subforums with their parents, e.g. in a flat list)
    // but inserting a new forum means rewriting all the indices - basically rebuilding the forum
    // might be better to just ignore new forums and only catch them on refreshes
    /**
     * Create ContentValues objects for a set of forum details, and return them in an array.
     * This will automatically set the INDEX field according to the object's position in the input list.
     * 
     * @param forums     an ordered list of Forums
     * @param updateTime a timestamp for the database records
     * @return the generated set of ContentValues
     */
    private fun getAsContentValues(
        forums: MutableList<Forum>,
        updateTime: String
    ): Array<ContentValues?> {
        val allContentValues: MutableList<ContentValues?> = ArrayList(forums.size)
        var contentValues: ContentValues?

        for (forum in forums) {
            contentValues = ContentValues()
            // use the current list size (before we add this element) as the index counter
            contentValues.put(AwfulForum.INDEX, allContentValues.size)
            contentValues.put(AwfulForum.ID, forum.id)
            contentValues.put(AwfulForum.PARENT_ID, forum.parentId)
            contentValues.put(AwfulForum.TITLE, forum.title)
            contentValues.put(AwfulForum.SUBTEXT, forum.subtitle)
            contentValues.put(DatabaseHelper.UPDATED_TIMESTAMP, updateTime)
            allContentValues.add(contentValues)
        }

        return allContentValues.toTypedArray<ContentValues?>()
    }

    private val timestamp: String
        /**
         * The current time as an SQL timestamp
         */
        get() = Timestamp(System.currentTimeMillis()).toString()


    interface ForumsUpdateListener {
        /**
         * Called when an update has started
         */
        fun onForumsUpdateStarted()

        /**
         * Called when an update has finished - the forums data may or may not have changed.
         * 
         * @param success true if the update operation finished successfully
         */
        fun onForumsUpdateCompleted(success: Boolean)

        /**
         * Called when an update has been cancelled
         */
        fun onForumsUpdateCancelled()
    }

    companion object {
        /**
         * The ID of the 'root' of the forums hierarchy - anything with this parent ID will be top-level
         */
        const val TOP_LEVEL_PARENT_ID: Int = 0
        private const val TAG = "ForumRepo"
        private const val PREF_KEY_FORUM_REFRESH_TIMESTAMP = "LAST_FORUM_REFRESH_TIME"
        private const val FAV_ID_SEPARATOR = ' '
        private var mThis: ForumRepository? = null

        /**
         * Get an instance of ForumsRepository.
         * The first call **must** provide a Context to initialize the singleton!
         * Subsequent calls can pass null.
         * 
         * @param context A context used to initialize the repo
         * @return A reference to the application-wide ForumRepository
         */
        @JvmStatic
        fun getInstance(context: Context?): ForumRepository {
            check(!(mThis == null && context == null)) { "ForumRepository has not been initialised - requires a context, but got null" }
            if (mThis == null) {
                mThis = ForumRepository(context!!)
            }
            return mThis!!
        }

        /////////////////////////////////////////////////////////////////////////
        // Favourites
        /////////////////////////////////////////////////////////////////////////
        /**
        * Get the user's favourite forums, as a sequence of forum IDs.
        */
        private val favouriteForumIds: Array<String>
            get() {
                val favouriteList =
                    AwfulPreferences.getInstance().getPreference(StringPreference.FAVOURITE_FORUMS, "")
                return StringUtils.split(
                    favouriteList,
                    FAV_ID_SEPARATOR
                )
            }


        /**
         * Set the user's favourite forums, as a sequence of forum IDs.
         */
        private fun setFavouriteForumIds(forumIds: MutableList<String>) {
            // stored as a single string of IDs
            val joinedIds: String? = StringUtils.join(forumIds, FAV_ID_SEPARATOR)
            AwfulPreferences.getInstance().setPreference(StringPreference.FAVOURITE_FORUMS, joinedIds)
        }
    }
}
