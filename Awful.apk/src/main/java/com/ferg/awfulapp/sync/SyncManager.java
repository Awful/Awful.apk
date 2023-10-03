package com.ferg.awfulapp.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.widget.Toast;

import com.ferg.awfulapp.Authentication;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.announcements.AnnouncementsManager;
import com.ferg.awfulapp.forums.CrawlerTask;
import com.ferg.awfulapp.forums.DropdownParserTask;
import com.ferg.awfulapp.forums.ForumRepository;
import com.ferg.awfulapp.messages.PmManager;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.Keys;
import com.ferg.awfulapp.task.FeatureRequest;
import com.ferg.awfulapp.task.RefreshUserProfileRequest;
import com.ferg.awfulapp.util.AwfulUtils;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Created by baka kaba on 29/04/2016.
 * <p/>
 * Class for handling general data sync tasks and housekeeping.
 */
public class SyncManager {

    private static final int FORUM_UPDATE_FREQUENCY = 1;
    private static final TimeUnit FORUM_UPDATE_FREQUENCY_UNITS = TimeUnit.DAYS;


    public static void sync(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        if (!Authentication.INSTANCE.isUserLoggedIn()) {
            Timber.w("Failed to sync - user is not logged in");
            return;
        }
        Timber.i("------ syncing profile and forum details");
        updateProfile(appContext);
        updateAccountFeatures(appContext);
        updatePms(appContext);
        updateAnnouncements(appContext);
        updateForums(appContext);
        trimDatabase(appContext);
        updateImgur(appContext);
    }


    private static void updateAccountFeatures(@NonNull Context context) {
        NetworkUtils.queueRequest(new FeatureRequest(context).build(null, null));
    }


    private static void updatePms(@NonNull Context context) {
        PmManager.updatePms(context);
    }


    private static void updateAnnouncements(@NonNull Context context) {
        // rubbish hack to avoid the Announcements and PM snackbars from appearing simultaneously and cancelling each other
        new Handler(Looper.getMainLooper())
                .postDelayed(() -> AnnouncementsManager.updateAnnouncements(context), 10_000L);
    }


    private static void updateProfile(@NonNull Context context) {
        NetworkUtils.queueRequest(new RefreshUserProfileRequest(context).build(null, null));
    }


    private static void trimDatabase(@NonNull Context context) {
        AwfulUtils.trimDbEntries(context.getContentResolver());
    }


    private static void updateForums(@NonNull final Context context) {
        final ForumRepository forumRepo = ForumRepository.getInstance(context);
        // cancel any update that's in progress
        forumRepo.cancelUpdate();

        boolean hasForumData = forumRepo.hasForumData();
        long lastSuccessfulUpdate = forumRepo.getLastRefreshTime();
        long timeSinceUpdate = FORUM_UPDATE_FREQUENCY_UNITS.convert(System.currentTimeMillis() - lastSuccessfulUpdate, TimeUnit.MILLISECONDS);
        String timeUnits = FORUM_UPDATE_FREQUENCY_UNITS.toString().toLowerCase();
        // TODO: add better data limiting, maybe as a separate settings category / general 'restrict data' option
        boolean limitDataUse = !AwfulPreferences.getInstance(context).canLoadImages();

        // work out if we're due an update
        boolean updateDue = timeSinceUpdate >= FORUM_UPDATE_FREQUENCY;

        // unless we really need some forum data (e.g. after a data clear), only update if scheduled and permitted
        if (hasForumData && (limitDataUse || !updateDue)) {
                Timber.d("Not updating forums - %s %s since last update", timeSinceUpdate, timeUnits);
            return;
        }
        CrawlerTask.Priority updatePriority = hasForumData ? CrawlerTask.Priority.LOW : CrawlerTask.Priority.HIGH;
            Timber.d("Updating forums (%s priority) - %s forum data, %d %s since last update",
                    updatePriority.name(),
                    hasForumData ? "we have old" : "no existing",
                    timeSinceUpdate,
                    timeUnits);

        // add a listener for the result - this is really to check for failure in a no-data situation
        forumRepo.registerListener(new UpdateResultHandler(forumRepo, context));

        // finally, now the handler is set up, start the update task
        forumRepo.updateForums(new CrawlerTask(context, updatePriority));
    }

    private static void updateImgur(@NonNull final Context context){
        AwfulPreferences mPrefs = AwfulPreferences.getInstance(context);
        if ( System.currentTimeMillis() > mPrefs.imgurTokenExpires) {
            mPrefs.setPreference(Keys.IMGUR_ACCOUNT_TOKEN, (String) null);
            mPrefs.setPreference(Keys.IMGUR_REFRESH_TOKEN, (String) null);
            mPrefs.setPreference(Keys.IMGUR_ACCOUNT, (String) null);
            mPrefs.setPreference(Keys.IMGUR_TOKEN_EXPIRES, 0L);
        }
    }


    private static class UpdateResultHandler implements ForumRepository.ForumsUpdateListener {

        private final ForumRepository forumRepo;
        private final Context context;
        // flag to check if the fallback DropdownParse task has been run yet
        volatile boolean parsedDropdown;


        UpdateResultHandler(ForumRepository forumRepo, Context context) {
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
            Timber.i(message, success ? "succeeded" : "failed", noForumData, parsedDropdown);

            if (noForumData && !parsedDropdown) {
                Timber.w("Forum update failed, still have no data - running dropdown parser to get something");
                parsedDropdown = true;
                // TODO: other callbacks are out of order, since other *Completed callbacks follow this, but this triggers some *Started ones first
                // basically the problem is, the index fragment gets a callback for this NEW update before it gets
                // the completed callback for the OLD one. So it cancels the progress bar and doesn't restart it
                forumRepo.updateForums(new DropdownParserTask(context));
            } else {
                if (noForumData) {
                    Handler handler = new Handler(context.getMainLooper());
                    handler.post(() -> Toast.makeText(context, R.string.forums_update_failure_message, Toast.LENGTH_LONG).show());
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
