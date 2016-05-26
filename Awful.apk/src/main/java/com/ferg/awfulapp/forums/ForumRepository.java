package com.ferg.awfulapp.forums;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.IndexIconRequest;
import com.ferg.awfulapp.thread.AwfulForum;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.ferg.awfulapp.forums.Forum.BOOKMARKS;
import static com.ferg.awfulapp.forums.Forum.SECTION;
import static com.ferg.awfulapp.forums.ForumStructure.FLAT;

/**
 * Created by baka kaba on 04/04/2016.
 * <p/>
 * Provides access to current forum state, forcing updates etc.
 */
public class ForumRepository implements UpdateTask.ResultListener {

    private static final String TAG = "ForumRepo";
    /**
     * The ID of the 'root' of the forums hierarchy - anything with this parent ID will be top-level
     */
    static final int TOP_LEVEL_PARENT_ID = 0;

    private static ForumRepository mThis = null;

    // using a COW array to make listener de/registration and iteration ~fairly~ thread-safe
    private final Set<ForumsUpdateListener> listeners = new CopyOnWriteArraySet<>();
    // cached value to make timestamp queries faster and avoid jank
    private volatile Long lastSuccessfulUpdate = null;
    private final Context context;

    /**
     * The current update task, if any
     */
    private static volatile UpdateTask currentUpdateTask = null;
    /**
     * Synchronization lock for accessing currentUpdateTask
     */
    private static final Object updateLock = new Object();


    /**
     * Get an instance of ForumsRepository.
     * The first call <b>must</b> provide a Context to initialise the singleton!
     * Subsequent calls can pass null.
     *
     * @param context A context used to initialise the repo
     * @return A reference to the application-wide ForumRepository
     */
    public static ForumRepository getInstance(@Nullable Context context) {
        if (mThis == null && context == null) {
            throw new IllegalStateException("ForumRepository has not been initialised - requires a context, but got null");
        }
        if (mThis == null) {
            mThis = new ForumRepository(context);
        }
        return mThis;
    }


    private ForumRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }


    public void registerListener(@NonNull ForumsUpdateListener listener) {
        listeners.add(listener);
        // let the new listener know if there's an update in progress
        if (isUpdating()) {
            listener.onForumsUpdateStarted();
        }
    }


    public void unregisterListener(@NonNull ForumsUpdateListener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Update task lifecycle
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Cancel any running forum updates.
     */
    public void cancelUpdate() {
        synchronized (updateLock) {
            if (currentUpdateTask == null) {
                Log.d(TAG, "cancelUpdate: no task running");
                return;
            }
            Log.w(TAG, "Cancelling an update in progress");
            UpdateTask cancelledTask = currentUpdateTask;
            currentUpdateTask = null;
            cancelledTask.cancel();
            NetworkUtils.cancelRequests(IndexIconRequest.REQUEST_TAG);
        }
        for (ForumsUpdateListener listener : listeners) {
            listener.onForumsUpdateCancelled();
        }
    }


    /**
     * Update the current forum list in the background.
     * Does nothing if an update is already in progress
     *
     * @param updateTask The type of update to perform
     */
    public void updateForums(@NonNull UpdateTask updateTask) {
        // synchronize to make sure only one update can start
        synchronized (updateLock) {
            if (currentUpdateTask != null) {
                Log.w(TAG, "Tried to refresh forums while the task was already running!");
                return;
            }
            currentUpdateTask = updateTask;
            currentUpdateTask.execute(this);
        }
        for (ForumsUpdateListener listener : listeners) {
            listener.onForumsUpdateStarted();
        }
    }


    @Override
    public void onRefreshCompleted(@NonNull UpdateTask updateTask,
                                   boolean success,
                                   @Nullable ForumStructure parsedStructure) {
        synchronized (updateLock) {
            // we're only interested in the current task, so ignore anything else that might pop up
            if (updateTask != currentUpdateTask) {
                Log.w(TAG, "onRefreshCompleted: not the current update task, ignoring");
                return;
            }
            if (success && parsedStructure != null) {
                storeForumData(parsedStructure);
                refreshTags(updateTask);
            } else {
                onUpdateComplete(updateTask, false);
            }
        }
    }


    /**
     * Refresh the forum tags.
     * This needs to be done after the forum hierarchy has been rebuilt, since it updates the new records
     *
     * @param updateTask The update task that triggered the tag refresh
     */
    private void refreshTags(@NonNull final UpdateTask updateTask) {
        NetworkUtils.queueRequest(new IndexIconRequest(context).build(null, new AwfulRequest.AwfulResultCallback<Void>() {
            public void success(Void result) {
                onUpdateComplete(updateTask, true);
            }


            public void failure(VolleyError error) {
                onUpdateComplete(updateTask, false);
            }
        }));
    }


    /**
     * Called when the task is complete (whether successful or not)
     *
     * @param updateTask The task that has finished
     */
    private void onUpdateComplete(@NonNull UpdateTask updateTask, boolean updateSuccessful) {
        synchronized (updateLock) {
            if (updateTask != currentUpdateTask) {
                Log.w(TAG, "onUpdateComplete: not the current task, ignoring");
                return;
            }
            currentUpdateTask = null;
        }
        for (ForumsUpdateListener listener : listeners) {
            listener.onForumsUpdateCompleted(updateSuccessful);
        }
    }


    /**
     * Check if a forums data update is in progress.
     */
    public boolean isUpdating() {
        return currentUpdateTask != null;
    }


    ///////////////////////////////////////////////////////////////////////////
    // Get forums data
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Check if the repo currently has any forum data.
     * This is a quick way of determining if an update needs to run (e.g. after data wipe)
     */
    public boolean hasForumData() {
        Cursor cursor = getForumsCursor();
        int numForums = (cursor == null) ? 0 : cursor.getCount();
        if (cursor != null) {
            cursor.close();
        }
        return numForums > 0;
    }


    /**
     * Get the timestamp of the last forum update.
     * Since data is only stored when an update is successful, this won't reflect any
     * unsuccessful attempts since then.
     *
     * @return the timestamp in milliseconds, or 0 if there is no forum data
     * @see System#currentTimeMillis()
     */
    public long getLastUpdateTime() {
        // check if we have a cached value, otherwise we need to query the DB
        if (lastSuccessfulUpdate != null) {
            return lastSuccessfulUpdate;
        }
        Cursor cursor = getForumsCursor();
        if (cursor == null) {
            return 0;
        }
        long lastUpdate = 0;
        // since all the data is replaced on update, everything has the same timestamp
        if (cursor.moveToFirst()) {
            String timestamp = cursor.getString(cursor.getColumnIndex(AwfulProvider.UPDATED_TIMESTAMP));
            if (timestamp != null) {
                lastUpdate = Timestamp.valueOf(timestamp).getTime();
            } else {
                Log.w(TAG, "getLastUpdateTime: NULL timestamp even though we have data!");
            }
        }
        cursor.close();
        lastSuccessfulUpdate = lastUpdate;
        return lastUpdate;
    }


    @NonNull
    public ForumStructure getForumStructure() {
        return ForumStructure.buildFromOrderedList(loadForumData(), TOP_LEVEL_PARENT_ID);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Database operations
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Remove all cached forum data from the DB.
     */
    public void clearForumData() {
        lastSuccessfulUpdate = null;
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.delete(AwfulForum.CONTENT_URI, null, null);
    }


    /**
     * Get a Cursor pointing to all Forum records, ordered by index.
     */
    @Nullable
    private Cursor getForumsCursor() {
        ContentResolver contentResolver = context.getContentResolver();
        // get all forums, ordered by index (the order they were added to the DB)
        return contentResolver.query(AwfulForum.CONTENT_URI,
                AwfulProvider.ForumProjection,
                null,
                null,
                AwfulForum.INDEX);
    }


    /**
     * Get all forums stored in the DB, as a list of Forums ordered by index.
     * See {@link #storeForumData(ForumStructure)} for details on index ordering.
     *
     * @return The list of Forums
     */
    @NonNull
    private List<Forum> loadForumData() {
        List<Forum> forumList = new ArrayList<>();
        Cursor cursor = getForumsCursor();
        if (cursor == null) {
            return forumList;
        }

        Forum forum;
        while (cursor.moveToNext()) {
            forum = new Forum(
                    cursor.getInt(cursor.getColumnIndex(AwfulForum.ID)),
                    cursor.getInt(cursor.getColumnIndex(AwfulForum.PARENT_ID)),
                    cursor.getString(cursor.getColumnIndex(AwfulForum.TITLE)),
                    cursor.getString(cursor.getColumnIndex(AwfulForum.SUBTEXT))
            );
            // the forum might have an image tag too
            String tagUrl = cursor.getString(cursor.getColumnIndex(AwfulForum.TAG_URL));
            forum.setTagUrl(tagUrl);

            // set the type e.g. for the index list to handle formatting
            if (forum.id == Constants.USERCP_ID) {
                forum.setType(BOOKMARKS);
            } else if (forum.parentId == TOP_LEVEL_PARENT_ID) {
                forum.setType(SECTION);
            }
            forumList.add(forum);
        }
        cursor.close();
        return forumList;
    }


    /**
     * Store a list of Forums in the DB.
     * The forums will be assigned an index in the order they're passed in. This index is used
     * to determine the order a group of forums should be displayed in, e.g. a flat list of all
     * forums, or within a list of subforums.
     *
     * @param parsedStructure The forum hierarchy
     */
    private void storeForumData(@NonNull ForumStructure parsedStructure) {
        ContentResolver contentResolver = context.getContentResolver();
        lastSuccessfulUpdate = System.currentTimeMillis();
        String updateTime = new Timestamp(lastSuccessfulUpdate).toString();
        List<Forum> allForums = new ArrayList<>();

        // we're replacing all the forums, so wipe them
        clearForumData();

        // add any special forums not on the main hierarchy
        Forum bookmarks = new Forum(Constants.USERCP_ID, TOP_LEVEL_PARENT_ID, "Bookmarks", "");
        allForums.add(bookmarks);

        // get all the parsed forums in an ordered list, so we can store them in this order using the INDEX field
        allForums.addAll(parsedStructure.getAsList().includeSections(true).formatAs(FLAT).build());

        contentResolver.bulkInsert(AwfulForum.CONTENT_URI, getAsContentValues(allForums, updateTime));
    }


    /**
     * Create ContentValues objects for a set of forum details, and return them in an array.
     * This will automatically set the INDEX field according to the object's position in the input list.
     *
     * @param forums     an ordered list of Forums
     * @param updateTime a timestamp for the database records
     * @return the generated set of ContentValues
     */
    private ContentValues[] getAsContentValues(@NonNull List<Forum> forums, @NonNull String updateTime) {
        List<ContentValues> allContentValues = new ArrayList<>(forums.size());
        ContentValues contentValues;

        for (Forum forum : forums) {
            contentValues = new ContentValues();
            // use the current list size (before we add this element) as the index counter
            contentValues.put(AwfulForum.INDEX, allContentValues.size());
            contentValues.put(AwfulForum.ID, forum.id);
            contentValues.put(AwfulForum.PARENT_ID, forum.parentId);
            contentValues.put(AwfulForum.TITLE, forum.title);
            contentValues.put(AwfulForum.SUBTEXT, forum.subtitle);
            contentValues.put(AwfulProvider.UPDATED_TIMESTAMP, updateTime);
            allContentValues.add(contentValues);
        }

        return allContentValues.toArray(new ContentValues[allContentValues.size()]);
    }


    public interface ForumsUpdateListener {
        /**
         * Called when an update has started
         */
        void onForumsUpdateStarted();

        /**
         * Called when an update has finished - the forums data may or may not have changed.
         *
         * @param success true if the update operation finished successfully
         */
        void onForumsUpdateCompleted(boolean success);

        /**
         * Called when an update has been cancelled
         */
        void onForumsUpdateCancelled();
    }

}
