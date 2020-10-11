package com.ferg.awfulapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageLoader
import com.ferg.awfulapp.ForumDisplayFragment.NULL_FORUM_ID
import com.ferg.awfulapp.ThreadDisplayFragment.NULL_THREAD_ID
import com.ferg.awfulapp.announcements.AnnouncementsManager
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.StringProvider
import com.ferg.awfulapp.provider.StringProvider.getString
import java.lang.ref.WeakReference

/**
 * Created by baka kaba on 11/01/2018.
 *
 * The navigation drawer code extracted from ForumsIndexActivity, with some refinement.
 *
 * The idea is to separate and encapsulate the behaviour, so this drawer is an independent component
 * that can listen for its own events (e.g. new private messages and announcements) and let the main
 * activity know when it needs to do something. The app code is still fairly tangled up, so some of
 * this (especially the 'set current forum' handling and display code) will need to be improved when
 * those issues are fixed. For now though, it should work like the old version.
 */

/**
 * A navigation drawer for the app's activities.
 *
 * Create this after inflating your activity's layout. You can call [open] and [close] on the drawer,
 * and [setCurrentForumAndThread] to update the hierarchy view. [drawerToggle] is exposed so the
 * activity can call lifecycle events like [ActionBarDrawerToggle.syncState], and set things like
 * the arrow behaviour if required.
 *
 * @param activity the Activity this navigation drawer is attached to
 * @param toolbar the Activity's action bar
 */
class NavigationDrawer(val activity: AwfulActivity, toolbar: Toolbar, val prefs: AwfulPreferences) {

    private val navigationMenu: NavigationView = activity.findViewById(R.id.navigation)
    private val drawerLayout: DrawerLayout = activity.findViewById(R.id.drawer_layout)
    private val username: TextView
    private val avatar: ImageView
    private val forumItem = menuItem(R.id.sidebar_forum)
    private val threadItem = menuItem(R.id.sidebar_thread)
    private val pmItem = menuItem(R.id.sidebar_pm)
    private val searchItem = menuItem(R.id.sidebar_search)
    private val announcementsItem = menuItem(R.id.sidebar_announcements)

    val drawerToggle: ActionBarDrawerToggle
    /** items that represent platinum-only features */
    private val platFeatures = listOf(pmItem, searchItem)

    private var currentForumId = NULL_FORUM_ID
    private var currentThreadId = NULL_THREAD_ID


    init {
        navigationMenu.setNavigationItemSelectedListener(::handleItemSelection)
        drawerToggle = ActionBarDrawerToggle(
            activity,
            drawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.setDrawerListener(drawerToggle)

        val nav = navigationMenu.getHeaderView(0)
        username = nav.findViewById(R.id.sidebar_username) as TextView
        avatar = nav.findViewById(R.id.sidebar_avatar) as ImageView

        prefs.registerCallback { _, _ -> refresh() }
        AnnouncementsManager.getInstance().registerListener { _, _, _, _ -> refresh() }
        refresh()
    }

    private fun menuItem(resId: Int) = navigationMenu.menu.findItem(resId)


    private fun loadAvatar(userTitle: String, avatar: ImageView) {
        NetworkUtils.getImageLoader().get(userTitle, object : ImageLoader.ImageListener {
            override fun onResponse(response: ImageLoader.ImageContainer, isImmediate: Boolean) {
                response.bitmap?.let(avatar::setImageBitmap)
                return
            }

            override fun onErrorResponse(error: VolleyError) =
                avatar.setImageResource(R.drawable.frog_icon)
        })
    }


    private fun handleItemSelection(menuItem: MenuItem): Boolean {
        with(activity) {
            when (menuItem.itemId) {
                R.id.sidebar_index -> navigate(NavigationEvent.ForumIndex)
                R.id.sidebar_forum -> navigate(NavigationEvent.Forum(id = currentForumId))
                R.id.sidebar_thread -> navigate(NavigationEvent.Thread(id = currentThreadId))
                R.id.sidebar_bookmarks -> navigate(NavigationEvent.Bookmarks)
                R.id.sidebar_settings -> navigate(NavigationEvent.Settings)
                R.id.sidebar_search -> navigate(NavigationEvent.SearchForums())
                R.id.sidebar_pm -> navigate(NavigationEvent.ShowPrivateMessages())
                R.id.sidebar_announcements -> navigate(NavigationEvent.Announcements)
                R.id.lepers_colony -> navigate(NavigationEvent.LepersColony())
                R.id.sidebar_logout -> showLogoutDialog()
                else -> return false //not handled, exit early without closing
            }
            close()
            return true
        }
    }


    private fun refresh() {
        username.text = prefs.username
        prefs.userAvatarUrl?.let { customTitle ->
            if (customTitle.isNotBlank()) loadAvatar(
                customTitle,
                avatar
            ) else avatar.setImageResource(R.drawable.frog_icon)
            avatar.clipToOutline = customTitle.isNotBlank()
        }

        // display the current forum title (or not)
        forumItem.isVisible = currentForumId != NULL_FORUM_ID && currentForumId !=
                Constants.USERCP_ID
        threadItem.isVisible = currentThreadId != NULL_THREAD_ID

        // update the forum and thread titles in the background, to avoid hitting the DB on the UI thread
        // to keep things simple, it also sets the text on anything we just hid, which will be placeholder text if the IDs are invalid
        HierarchyTextUpdater(
            currentForumId,
            currentThreadId,
            activity,
            forumItem,
            threadItem
        ).execute()
        platFeatures.forEach { it.setEnabled(prefs.hasPlatinum).isVisible = prefs.hasPlatinum }
        val unread = AnnouncementsManager.getInstance().unreadCount
        announcementsItem.title = getString(R.string.announcements) +
                if (unread == 0) "" else " ($unread)"
    }

    fun setCurrentForumAndThread(forumId: Int?, threadId: Int?) {
        currentForumId = forumId ?: NULL_FORUM_ID
        currentThreadId = threadId ?: NULL_THREAD_ID
        refresh()
    }

    fun open(): Boolean = drawerLayout.changeState(toOpen = true)
    fun close(): Boolean = drawerLayout.changeState(toOpen = false)
    /** Try to open or close the drawer, returning false if it was already in the requested state */
    private fun DrawerLayout.changeState(toOpen: Boolean): Boolean {
        return when (toOpen) {
            isDrawerOpen(GravityCompat.START) -> false
            true -> true.also { openDrawer(GravityCompat.START); }
            false -> true.also { closeDrawer(GravityCompat.START); }
        }
    }
}


/**
 * Updates the SA Forums -> Forum -> Thread hierarchy bit by loading titles from the DB on a background thread.
 *
 * This is not great (and updates get requested way too much right now) but this avoids the UI glitching
 * we were getting by doing it on the main thread.
 * Ideally in future we'll be passing around Forum and Thread objects that carry this data, so we can
 * avoid this completely.
 */
private class HierarchyTextUpdater(
    val forumId: Int,
    val threadId: Int,
    context: Context,
    forumItem: MenuItem,
    threadItem: MenuItem
) : AsyncTask<Void, Void, Void>() {

    private var forumName: String? = null
    private var threadName: String? = null
    // avoiding any strong references to the activity or drawer (which holds an activity reference too)
    @SuppressLint("StaticFieldLeak") // what do you want from me here
    private val appContext = context.applicationContext
    private val forumMenuItem = WeakReference(forumItem)
    private val threadMenuItem = WeakReference(threadItem)

    override fun doInBackground(vararg params: Void?): Void? {
        threadName = StringProvider.getThreadName(appContext, threadId)
        if (forumId != NULL_FORUM_ID) forumName = StringProvider.getForumName(appContext, forumId)
        return null
    }

    override fun onPostExecute(aVoid: Void?) {
        forumMenuItem.get()?.title = forumName ?: ""
        threadMenuItem.get()?.title = threadName ?: ""
    }

}
