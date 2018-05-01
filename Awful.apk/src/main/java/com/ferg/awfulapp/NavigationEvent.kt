package com.ferg.awfulapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ferg.awfulapp.announcements.AnnouncementsFragment
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.preferences.SettingsActivity
import com.ferg.awfulapp.thread.AwfulURL
import timber.log.Timber

/**
 * Represents the navigation events we handle within the app, and any associated data for each.
 *
 * This class is intended to keep all of the Intent-handling code in one place, so the rest of the
 * app can handle fully formed NavigationEvent objects instead.
 */

sealed class NavigationEvent(private val extraTypeId: String) {

    object ReAuthenticate : NavigationEvent(TYPE_RE_AUTHENTICATE)
    object MainActivity : NavigationEvent(TYPE_MAIN_ACTIVITY)
    object Bookmarks : NavigationEvent(TYPE_BOOKMARKS)
    object ForumIndex : NavigationEvent(TYPE_FORUM_INDEX)
    object Settings : NavigationEvent(TYPE_SETTINGS)
    object SearchForums : NavigationEvent(TYPE_SEARCH_FORUMS)
    object Announcements : NavigationEvent(TYPE_ANNOUNCEMENTS)

    data class Thread(val id: Int, val page: Int? = null, val postJump: String? = null) :
            NavigationEvent(TYPE_THREAD)

    data class Forum(val id: Int, val page: Int? = null) : NavigationEvent(TYPE_FORUM)
    data class Url(val url: AwfulURL) : NavigationEvent(TYPE_URL)

    // TODO: the activity just wants to parse an int anyway (a long is probably better), parse/validate here?
    /**
     * Show the user's private messages, with an optional URL for a message to open
     */
    data class PrivateMessages(val messageUri: Uri? = null) : NavigationEvent(TYPE_PRIVATE_MESSAGES)


    /**
     * Build an Intent for this NavigationEvent.
     *
     * This function produces the correctly structured Intents recognised by the app, including the
     * expected launch modes and flags. If you need to use Intents to e.g. navigate from one Activity
     * to another, you should use this instead of constructing one in another class.
     */
    fun getIntent(context: Context): Intent = activityIntent(context).putExtra(EVENT_EXTRA_KEY, extraTypeId).apply {
        // we've created the base intent and added its type data - any other data is added here
        when (this@NavigationEvent) {
            is Thread -> {
                putExtra(Constants.THREAD_ID, id)
                page?.let { putExtra(Constants.THREAD_PAGE, page) }
                postJump?.let { putExtra(Constants.THREAD_FRAGMENT, postJump) }
            }
            is Forum -> {
                putExtra(Constants.FORUM_ID, id)
                page?.let { putExtra(Constants.FORUM_PAGE, page) }
            }
            is PrivateMessages -> messageUri?.let(::setData)
        }
    }


    /**
     * Get the base intent for this navigation event, with the activity it opens and any special flags or configuration.
     *
     * Every event needs an entry here! This defines its destination, the activity that should open
     * and handle any other data it contains.
     */
    private fun activityIntent(context: Context): Intent = when (this) {
        MainActivity, Bookmarks, ForumIndex, is Thread, is Forum, is Url ->
            Intent().setClass(context, ForumsIndexActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        Settings ->
            Intent().setClass(context, SettingsActivity::class.java)
        SearchForums ->
            BasicActivity.intentFor(SearchFragment::class.java, context, context.getString(R.string.search_forums_activity_title))
        Announcements ->
            BasicActivity.intentFor(AnnouncementsFragment::class.java, context, context.getString(R.string.announcements))
        is PrivateMessages ->
            Intent().setClass(context, PrivateMessageActivity::class.java)
        else -> throw RuntimeException("No activity defined for event: $this")
    }


    companion object {

        // the identifiers for each navigation type, added to and read from the app's intents, and the key used to store them

        private const val EVENT_EXTRA_KEY = "navigation event"
        private const val TYPE_RE_AUTHENTICATE = "nav_re-auth"
        private const val TYPE_MAIN_ACTIVITY = "nav_main_activity"
        /** This one gets baked into the bookmarks widget when it's created, so if you change the value they'll have to be remade */
        private const val TYPE_BOOKMARKS = "nav_bookmarks"
        private const val TYPE_FORUM_INDEX = "nav_forum_index"
        private const val TYPE_THREAD = "nav_thread"
        private const val TYPE_FORUM = "nav_forum"
        private const val TYPE_URL = "nav_url"
        private const val TYPE_SETTINGS = "nav_settings"
        private const val TYPE_SEARCH_FORUMS = "nav_search_forums"
        private const val TYPE_PRIVATE_MESSAGES = "nav_private_messages"
        private const val TYPE_ANNOUNCEMENTS = "nav_announcements"


        /**
         * Parse an intent as one of the navigation events we handle. Defaults to [MainActivity]
         */
        fun Intent.parse(): NavigationEvent {
            parseUrl()?.let { return it }
            return when (getStringExtra(EVENT_EXTRA_KEY)) {
            //TODO: handle behaviour for missing data, e.g. can't navigate to a thread with no thread ID
                TYPE_RE_AUTHENTICATE -> ReAuthenticate
                TYPE_MAIN_ACTIVITY -> MainActivity
                TYPE_FORUM_INDEX -> ForumIndex
                TYPE_BOOKMARKS -> Bookmarks
                TYPE_FORUM -> Forum(
                        id = getIntExtra(Constants.FORUM_ID)!!,
                        page = getIntExtra(Constants.FORUM_PAGE)
                )
                TYPE_THREAD -> Thread(
                        id = getIntExtra(Constants.THREAD_ID)!!,
                        page = getIntExtra(Constants.THREAD_PAGE),
                        postJump = getStringExtra(Constants.THREAD_FRAGMENT)
                )
                else -> MainActivity
            }
        }


        /**
         * Attempt to parse an AwfulURL from an intent, converting to a NavigationEvent.
         *
         * Returns null if a valid URL couldn't be found.
         */
        private fun Intent.parseUrl(): NavigationEvent? {
            data?.scheme.apply {
                if (!equals("http") && !equals("https")) return null
            }
            with(AwfulURL.parse(dataString)) {
                // this mirrors the old behaviour in ForumsIndexActivity - basically we need to
                // hand the URL over to the ThreadDisplayFragment if it's a post or a redirecting thread.
                return when {
                // TODO: if it's a post, we're meant to pass the actual url through TDF.openThread(url) - let's not do that and just handle it here
                    isPost || (isThread && isRedirect) ->
                        Url(this)
                // TODO: can we spot null pages here? OR at least default ones?
                    isForum ->
                        Forum(id.toInt(), page.toInt())
                // TODO: can we get the fragment?
                    isThread ->
                        Thread(id.toInt(), page.toInt())
                    isForumIndex ->
                        ForumIndex
                    else -> null
                }
            }
        }

        /**
         * Nullable getter for int extras, which also logs when they're missing.
         */
        private fun Intent.getIntExtra(name: String) =
                if (hasExtra(name)) {
                    getIntExtra(name, -12345)
                } else {
                    Timber.i("No int extra: $name")
                    null
                }
    }
}