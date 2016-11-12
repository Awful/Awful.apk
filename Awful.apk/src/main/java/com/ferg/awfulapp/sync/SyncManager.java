package com.ferg.awfulapp.sync;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.forums.CrawlerTask;
import com.ferg.awfulapp.forums.DropdownParserTask;
import com.ferg.awfulapp.forums.ForumRepository;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.task.FeatureRequest;
import com.ferg.awfulapp.task.ProfileRequest;
import com.ferg.awfulapp.util.AwfulUtils;

import java.util.concurrent.TimeUnit;

import static com.ferg.awfulapp.constants.Constants.DEBUG;

/**
 * Created by baka kaba on 29/04/2016.
 * <p/>
 * Class for handling general data sync tasks and housekeeping.
 */
public class SyncManager {

    private static final String TAG = "SyncManager";
    private static final int FORUM_UPDATE_FREQUENCY = 7;
    private static final TimeUnit FORUM_UPDATE_FREQUENCY_UNITS = TimeUnit.DAYS;


    public static void sync(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        Log.i(TAG, "------ syncing profile and forum details");
        updateProfile(appContext);
        updateAccountFeatures(appContext);
        updateForums(context);
        trimDatabase(context);
    }


    private static void updateAccountFeatures(@NonNull Context context) {
        NetworkUtils.queueRequest(new FeatureRequest(context).build(null, null));
    }


    private static void updateProfile(@NonNull Context context) {
        NetworkUtils.queueRequest(new ProfileRequest(context).build(null, null));
    }


    private static void trimDatabase(@NonNull Context context) {
        AwfulUtils.trimDbEntries(context.getContentResolver());
    }


    private static void updateForums(@NonNull final Context context) {
        final ForumRepository forumRepo = ForumRepository.getInstance(context);
        // cancel any update that's in progress
        forumRepo.cancelUpdate();

        boolean hasForumData = forumRepo.hasForumData();
        long lastSuccessfulUpdate = forumRepo.getLastUpdateTime();
        long timeSinceUpdate = FORUM_UPDATE_FREQUENCY_UNITS.convert(System.currentTimeMillis() - lastSuccessfulUpdate, TimeUnit.MILLISECONDS);
        String timeUnits = FORUM_UPDATE_FREQUENCY_UNITS.toString().toLowerCase();
        // TODO: add better data limiting, maybe as a separate settings category / general 'restrict data' option
        boolean limitDataUse = !AwfulPreferences.getInstance(context).canLoadImages();

        // work out if we're due an update
        boolean updateDue = timeSinceUpdate >= FORUM_UPDATE_FREQUENCY;

        // unless we really need some forum data (e.g. after a data clear), only update if scheduled and permitted
        if (hasForumData && (limitDataUse || !updateDue)) {
            if (DEBUG)
                Log.d(TAG, String.format("Not updating forums - %s %s since last update", timeSinceUpdate, timeUnits));
            return;
        }
        int updatePriority = hasForumData ? CrawlerTask.PRIORITY_LOW : CrawlerTask.PRIORITY_HIGH;
        if (DEBUG) {
            Log.d(TAG, String.format("Updating forums (%s priority) - %s forum data, %d %s since last update",
                    updatePriority == CrawlerTask.PRIORITY_HIGH ? "high" : "low",
                    hasForumData ? "we have old" : "no existing",
                    timeSinceUpdate,
                    timeUnits));
        }

        // add a listener for the result - this is really to check for failure in a no-data situation
        forumRepo.registerListener(new UpdateResultHandler(forumRepo, context));

        // finally, now the handler is set up, start the update task
        forumRepo.updateForums(new CrawlerTask(context, updatePriority));
    }


    private static class UpdateResultHandler implements ForumRepository.ForumsUpdateListener {

        private final ForumRepository forumRepo;
        private final Context context;
        // flag to check if the fallback DropdownParse task has been run yet
        volatile boolean parsedDropdown;


        public UpdateResultHandler(ForumRepository forumRepo, Context context) {
            this.forumRepo = forumRepo;
            this.context = context;
            parsedDropdown = false;
        }


        @Override
        public void onForumsUpdateStarted() {
        }


        @Override
        public void onForumsUpdateCompleted(boolean success) {
            // check for a serious failure where we have no data, and run the basic dropdown update once if necessary
            boolean noForumData = !forumRepo.hasForumData();
            String message = "onForumsUpdateCompleted: sync %s, no forum data: %b, dropdown parse run: %b";
            Log.i(TAG, String.format(message, success ? "succeeded" : "failed", noForumData, parsedDropdown));

            if (noForumData && !parsedDropdown) {
                Log.w(TAG, "Forum update failed, still have no data - running dropdown parser to get something");
                parsedDropdown = true;
                // TODO: other callbacks are out of order, since other *Completed callbacks follow this, but this triggers some *Started ones first
                // basically the problem is, the index fragment gets a callback for this NEW update before it gets
                // the completed callback for the OLD one. So it cancels the progress bar and doesn't restart it
                forumRepo.updateForums(new DropdownParserTask(context));
            } else {
                if (noForumData) {
                    Handler handler = new Handler(context.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, R.string.forums_update_failure_message, Toast.LENGTH_LONG).show();
                        }
                    });
                }
                forumRepo.unregisterListener(this);
            }
        }


        @Override
        public void onForumsUpdateCancelled() {
            // we need to ditch this listener if the update was cancelled,
            // since a new update will add another with fresh state
            forumRepo.unregisterListener(this);
        }
    }
}
