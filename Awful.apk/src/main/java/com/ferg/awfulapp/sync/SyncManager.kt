package com.ferg.awfulapp.sync

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.ferg.awfulapp.Authentication.isUserLoggedIn
import com.ferg.awfulapp.R
import com.ferg.awfulapp.announcements.AnnouncementsManager
import com.ferg.awfulapp.forums.CrawlerTask
import com.ferg.awfulapp.forums.DropdownParserTask
import com.ferg.awfulapp.forums.ForumRepository
import com.ferg.awfulapp.forums.ForumRepository.ForumsUpdateListener
import com.ferg.awfulapp.messages.PmManager
import com.ferg.awfulapp.network.NetworkUtils.queueRequest
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.LongPreference
import com.ferg.awfulapp.preferences.StringPreference
import com.ferg.awfulapp.task.FeatureRequest
import com.ferg.awfulapp.task.RefreshUserProfileRequest
import com.ferg.awfulapp.util.AwfulUtils
import timber.log.Timber.Forest.d
import timber.log.Timber.Forest.i
import timber.log.Timber.Forest.w
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.concurrent.Volatile

/**
 * Created by baka kaba on 29/04/2016.
 * 
 * 
 * Class for handling general data sync tasks and housekeeping.
 */
object SyncManager {
    private const val FORUM_UPDATE_FREQUENCY = 1
    private val FORUM_UPDATE_FREQUENCY_UNITS = TimeUnit.DAYS


    fun sync(context: Context) {
        val appContext = context.applicationContext
        if (!isUserLoggedIn()) {
            w("Failed to sync - user is not logged in")
            return
        }
        i("------ syncing profile and forum details")
        updateProfile(appContext)
        updateAccountFeatures(appContext)
        updatePms(appContext)
        updateAnnouncements(appContext)
        updateForums(appContext)
        trimDatabase(appContext)
        updateImgur(appContext)
    }


    private fun updateAccountFeatures(context: Context) {
        queueRequest(FeatureRequest(context).build(null, null))
    }


    private fun updatePms(context: Context) {
        PmManager.updatePms(context)
    }


    private fun updateAnnouncements(context: Context) {
        // rubbish hack to avoid the Announcements and PM snackbars from appearing simultaneously and cancelling each other
        Handler(Looper.getMainLooper())
            .postDelayed({ AnnouncementsManager.updateAnnouncements(context) }, 10000L)
    }


    private fun updateProfile(context: Context) {
        queueRequest(RefreshUserProfileRequest(context).build(null, null))
    }


    private fun trimDatabase(context: Context) {
        AwfulUtils.trimDbEntries(context.contentResolver)
    }


    private fun updateForums(context: Context) {
        val forumRepo = ForumRepository.getInstance(context)
        // cancel any update that's in progress
        forumRepo.cancelUpdate()

        val hasForumData = forumRepo.hasForumData()
        val lastSuccessfulUpdate = forumRepo.lastRefreshTime
        val timeSinceUpdate = FORUM_UPDATE_FREQUENCY_UNITS.convert(
            System.currentTimeMillis() - lastSuccessfulUpdate,
            TimeUnit.MILLISECONDS
        )
        val timeUnits = FORUM_UPDATE_FREQUENCY_UNITS.toString().lowercase(Locale.getDefault())
        // TODO: add better data limiting, maybe as a separate settings category / general 'restrict data' option
        val limitDataUse = !AwfulPreferences.getInstance(context).canLoadImages()

        // work out if we're due an update
        val updateDue = timeSinceUpdate >= FORUM_UPDATE_FREQUENCY

        // unless we really need some forum data (e.g. after a data clear), only update if scheduled and permitted
        if (hasForumData && (limitDataUse || !updateDue)) {
            d("Not updating forums - %s %s since last update", timeSinceUpdate, timeUnits)
            return
        }
        val updatePriority =
            if (hasForumData) CrawlerTask.Priority.LOW else CrawlerTask.Priority.HIGH
        d(
            "Updating forums (%s priority) - %s forum data, %d %s since last update",
            updatePriority.name,
            if (hasForumData) "we have old" else "no existing",
            timeSinceUpdate,
            timeUnits
        )

        // add a listener for the result - this is really to check for failure in a no-data situation
        forumRepo.registerListener(UpdateResultHandler(forumRepo, context))

        // finally, now the handler is set up, start the update task
        forumRepo.updateForums(CrawlerTask(context, updatePriority))
    }

    private fun updateImgur(context: Context) {
        val mPrefs = AwfulPreferences.getInstance(context)
        if (System.currentTimeMillis() > mPrefs.imgurTokenExpires) {
            mPrefs.setPreference(StringPreference.IMGUR_ACCOUNT_TOKEN, null as String?)
            mPrefs.setPreference(StringPreference.IMGUR_REFRESH_TOKEN, null as String?)
            mPrefs.setPreference(StringPreference.IMGUR_ACCOUNT, null as String?)
            mPrefs.setPreference(LongPreference.IMGUR_TOKEN_EXPIRES, 0L)
        }
    }


    private class UpdateResultHandler(
        private val forumRepo: ForumRepository,
        private val context: Context
    ) : ForumsUpdateListener {
        // flag to check if the fallback DropdownParse task has been run yet
        @Volatile
        var parsedDropdown: Boolean = false


        override fun onForumsUpdateStarted() {
        }


        override fun onForumsUpdateCompleted(success: Boolean) {
            // check for a serious failure where we have no data, and run the basic dropdown update once if necessary
            val noForumData = !forumRepo.hasForumData()
            val message =
                "onForumsUpdateCompleted: sync %s, no forum data: %b, dropdown parse run: %b"
            i(message, if (success) "succeeded" else "failed", noForumData, parsedDropdown)

            if (noForumData && !parsedDropdown) {
                w("Forum update failed, still have no data - running dropdown parser to get something")
                parsedDropdown = true
                // TODO: other callbacks are out of order, since other *Completed callbacks follow this, but this triggers some *Started ones first
                // basically the problem is, the index fragment gets a callback for this NEW update before it gets
                // the completed callback for the OLD one. So it cancels the progress bar and doesn't restart it
                forumRepo.updateForums(DropdownParserTask(context))
            } else {
                if (noForumData) {
                    val handler = Handler(context.mainLooper)
                    handler.post {
                        Toast.makeText(
                            context,
                            R.string.forums_update_failure_message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                forumRepo.unregisterListener(this)
            }
        }


        override fun onForumsUpdateCancelled() {
            // we need to ditch this listener if the update was cancelled,
            // since a new update will add another with fresh state
            forumRepo.unregisterListener(this)
        }
    }
}
