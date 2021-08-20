package com.ferg.awfulapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ferg.awfulapp.announcements.AnnouncementsFragment
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.preferences.SettingsActivity
import com.ferg.awfulapp.search.SearchFilter
import com.ferg.awfulapp.search.SearchFragment
import com.ferg.awfulapp.thread.AwfulURL
import com.ferg.awfulapp.users.LepersColonyFragment
import com.ferg.awfulapp.util.AwfulUtils
import com.ferg.awfulapp.util.tryGetIntExtra
import timber.log.Timber

/**
 * Represents the navigation events we handle within the app, and any associated data for each.
 *
 * This class is intended to keep all of the Intent-handling code in one place, so the rest of the
 * app can handle fully formed NavigationEvent objects instead.
 */

sealed class NavigationEvent(private val extraTypeId: String) {

    /**
     * Get the base intent for this navigation event, with the activity it opens and any special flags or configuration.
     *
     * Defaults to opening the main activity, override this for events handled by other activities.
     */
    protected open fun activityIntent(context: Context): Intent =
            context.intentFor(ForumsIndexActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

    /**
     * Code to run when converting this event to an Intent.
     *
     * Override this when you need to include data, e.g. a thread ID, so it can be retrieved when the Intent is parsed.
     */
    protected open val addDataToIntent: Intent.() -> Unit = {}


    object ReAuthenticate : NavigationEvent(TYPE_RE_AUTHENTICATE)

    object MainActivity : NavigationEvent(TYPE_MAIN_ACTIVITY)

    object Bookmarks : NavigationEvent(TYPE_BOOKMARKS)

    object ForumIndex : NavigationEvent(TYPE_FORUM_INDEX)

    object Settings : NavigationEvent(TYPE_SETTINGS) {

        override fun activityIntent(context: Context) = context.intentFor(SettingsActivity::class.java)
    }

    class SearchForums(vararg val filters: SearchFilter) : NavigationEvent(TYPE_SEARCH_FORUMS) {

        override fun activityIntent(context: Context) =
                BasicActivity.intentFor(SearchFragment::class.java, context, context.getString(R.string.search_forums_activity_title))

        override val addDataToIntent: Intent.() -> Unit = {
            putParcelableArrayListExtra(KEY_SEARCH_FILTERS, filters.toCollection(ArrayList()))
        }
    }

    object Announcements : NavigationEvent(TYPE_ANNOUNCEMENTS) {

        override fun activityIntent(context: Context) =
                BasicActivity.intentFor(AnnouncementsFragment::class.java, context, context.getString(R.string.announcements))
    }

    data class Thread(val id: Int, val page: Int? = null, val postJump: String? = null) :
            NavigationEvent(TYPE_THREAD) {

        override val addDataToIntent: Intent.() -> Unit = {
            putExtra(Constants.THREAD_ID, id)
            page?.let { putExtra(Constants.THREAD_PAGE, page) }
            postJump?.let { putExtra(Constants.THREAD_FRAGMENT, postJump) }
        }
    }

    data class Forum(val id: Int, val page: Int? = null) : NavigationEvent(TYPE_FORUM) {

        override val addDataToIntent: Intent.() -> Unit = {
            putExtra(Constants.FORUM_ID, id)
            page?.let { putExtra(Constants.FORUM_PAGE, page) }
        }
    }

    data class Url(val url: AwfulURL) : NavigationEvent(TYPE_URL) {

        override val addDataToIntent: Intent.() -> Unit = {
            data = Uri.parse(url.url)
        }
    }

    /**
     * Show the user's private messages, with an optional URL for a message to open
     */
    data class ShowPrivateMessages(val messageUri: Uri? = null) : NavigationEvent(TYPE_SHOW_PRIVATE_MESSAGES) {
        // TODO: the activity just wants to parse an int anyway (a long is probably better), parse/validate here?

        override fun activityIntent(context: Context) = context.intentFor(PrivateMessageActivity::class.java)

        override val addDataToIntent: Intent.() -> Unit = {
            messageUri?.let(::setData)
        }
    }

    data class ComposePrivateMessage(val recipient: String? = null) : NavigationEvent(TYPE_COMPOSE_PRIVATE_MESSAGE) {

        override fun activityIntent(context: Context) = context.intentFor(MessageDisplayActivity::class.java)

        override val addDataToIntent: Intent.() -> Unit = {
            recipient?.let { putExtra(Constants.PARAM_USERNAME, recipient) }
        }
    }

    /**
     * Show the Leper's Colony, or a user's Rap Sheet if a [userId] is provided.
     *
     * You can specify an optional [page] number.
     */
    // TODO: remove the overloads bit when this isn't being accessed thru Java, i.e. the context menu that navigates to the LC
    data class LepersColony @JvmOverloads constructor(val userId: Int? = null, val page: Int = LepersColonyFragment.FIRST_PAGE) : NavigationEvent(TYPE_LEPERS_COLONY) {
        override fun activityIntent(context: Context) = context.intentFor(com.ferg.awfulapp.users.LepersColonyActivity::class.java)

        override val addDataToIntent: Intent.() -> Unit = {
            userId?.let { putExtra(Constants.PARAM_USER_ID, userId) }
            putExtra(Constants.PARAM_PAGE, page)
        }
    }


    /**
     * Build an Intent for this NavigationEvent.
     *
     * This function produces the correctly structured Intents recognised by the app, including the
     * expected launch modes and flags. If you need to use Intents to e.g. navigate from one Activity
     * to another, you should use this instead of constructing one in another class.
     */
    fun getIntent(context: Context): Intent = activityIntent(context).putExtra(EVENT_EXTRA_KEY, extraTypeId).apply(addDataToIntent)


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
        private const val TYPE_SHOW_PRIVATE_MESSAGES = "nav_show_private_messages"
        private const val TYPE_COMPOSE_PRIVATE_MESSAGE = "nav_compose_private_message"
        private const val TYPE_ANNOUNCEMENTS = "nav_announcements"
        private const val TYPE_LEPERS_COLONY = "nav_rap_sheet"

        private const val KEY_SEARCH_FILTERS = "key_search_filters"

        private fun Context.intentFor(clazz: Class<out Activity>): Intent = Intent().setClass(this, clazz)

        /**
         * Parse an intent as one of the navigation events we handle. Defaults to [MainActivity]
         */
        fun Intent.parse(): NavigationEvent {
            parseUrl()?.let { return it }
            return when (getStringExtra(EVENT_EXTRA_KEY)) {
            //TODO: handle behaviour for missing data, e.g. can't navigate to a thread with no thread ID
            // TODO: might be better to default to null? And let the caller decide what to do when parsing fails - can use the elvis ?: to supply a default event
                TYPE_RE_AUTHENTICATE -> NavigationEvent.ReAuthenticate
                TYPE_SETTINGS -> Settings
                TYPE_SEARCH_FORUMS -> SearchForums(
                        *getParcelableArrayListExtra<SearchFilter>(KEY_SEARCH_FILTERS)!!.toTypedArray()
                )
                TYPE_SHOW_PRIVATE_MESSAGES -> ShowPrivateMessages(
                        messageUri = data
                )
                TYPE_COMPOSE_PRIVATE_MESSAGE -> ComposePrivateMessage(
                        recipient = getStringExtra(Constants.PARAM_USERNAME)
                )
                TYPE_ANNOUNCEMENTS -> Announcements
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
                TYPE_LEPERS_COLONY -> LepersColony(
                        userId = getIntExtra(Constants.PARAM_USER_ID),
                        page = getIntExtra(Constants.PARAM_PAGE) ?: LepersColonyFragment.FIRST_PAGE
                )
                else -> {
                    Timber.w("Couldn't parse Intent as NavigationEvent - event key: ${getStringExtra(EVENT_EXTRA_KEY)}")
                    MainActivity
                }
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
                    isThread ->
                        Thread(id.toInt(), page.toInt(), fragment)
                    isForumIndex ->
                        ForumIndex
                    else -> null
                }
            }
        }

        /**
         * Nullable getter for int extras, which also logs when they're missing.
         */
        private fun Intent.getIntExtra(name: String) = tryGetIntExtra(name) ?: null.also { Timber.i("No int extra: $name") }
    }
}


/**
 * Interface for all components that handle [NavigationEvent]s, specifying what to do with
 * specific events and where to route the rest.
 */
interface NavigationEventHandler {

    /**
     * This is the general handler for any NavigationEvents that are not specifically consumed by [handleNavigation].
     *
     * This code should route the [event] to the next handler up the hierarchy, usually an [Activity].
     * Activities should call [NavigationEvent.getIntent] with [Activity.startActivity] to make the
     * app navigate to the appropriate activity, which can recreate the event with
     * [NavigationEvent.parse] and direct it to the appropriate component in [handleNavigation]
     */
    fun defaultRoute(event: NavigationEvent) {
        AwfulUtils.failSilently(Exception("Default navigation route expected but none specified! This event will be dropped\nEvent: $event"))
    }

    /**
     * Handle all the specific [NavigationEvent]s this handler knows about.
     *
     * This function is meant to define a set of events the handler either handles itself, or
     * routes to another specific component (e.g. an activity could pass a certain event to one of
     * its fragments). When an event is consumed like this, the function needs to return true, or it
     * will be passed to [defaultRoute]
     */
    fun handleNavigation(event: NavigationEvent): Boolean {
        Timber.i("No navigation event handler function - passing $event to default handler")
        return false
    }

    /**
     * Handle a [NavigationEvent], either in this class or by passing it to another [NavigationEventHandler]
     */
    fun navigate(event: NavigationEvent) {
        if (!handleNavigation(event)) defaultRoute(event)
    }
}