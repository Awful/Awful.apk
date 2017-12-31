package com.ferg.awfulapp


import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.ViewSwitcher
import butterknife.BindView
import butterknife.ButterKnife
import com.ferg.awfulapp.forums.Forum
import com.ferg.awfulapp.forums.ForumListAdapter
import com.ferg.awfulapp.forums.ForumRepository
import com.ferg.awfulapp.forums.ForumStructure.FLAT
import com.ferg.awfulapp.forums.ForumStructure.TWO_LEVEL
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.Keys
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.widget.StatusFrog
import java.util.*

/**
 * Created by baka kaba on 16/05/2016.
 *
 *
 * A fragment to display the current list of forums, or the user's favourites.
 *
 *
 * The fragment uses the [ForumRepository] to acquire [com.ferg.awfulapp.forums.ForumStructure]s
 * and format them according to the user's settings (e.g. as a single flat list), displaying the
 * results in a RecyclerView, and handling its click events. There is also a 'no data' view for when
 * the forum list is empty, with a customisable label and optional loading spinner.
 *
 *
 * There are two list displays, the full forums list and the user's favourite forums, switched by a
 * menu icon. In favourites mode, the user has the option to manage their list of favourites.
 *
 *
 * This fragment registers as a [com.ferg.awfulapp.forums.ForumRepository.ForumsUpdateListener]
 * to receive data update events, so it can refresh the forum list or display the loading spinner as
 * required.
 */
class ForumsIndexFragment : AwfulFragment(), ForumRepository.ForumsUpdateListener, ForumListAdapter.EventListener {

    @BindView(R.id.forum_index_list)    lateinit var forumRecyclerView: RecyclerView
    @BindView(R.id.view_switcher)       lateinit var forumsListSwitcher: ViewSwitcher
    @BindView(R.id.status_frog)         lateinit var statusFrog: StatusFrog

    lateinit private var forumListAdapter: ForumListAdapter
    lateinit private var forumRepo: ForumRepository

    // repo timestamp for the currently displayed data, used to check if the repo has since updated
    private var lastUpdateTime: Long = -1

    // Current view state - either showing the favourites list, or the full forums list
    private var showFavourites = AwfulPreferences.getInstance().getPreference(Keys.FORUM_INDEX_PREFER_FAVOURITES, false)

    // list formatting for the forums
    private val allForums: List<Forum>
        get() = forumRepo.allForums
                .asList
                .includeSections(mPrefs.forumIndexShowSections)
                .formatAs(if (mPrefs.forumIndexHideSubforums) TWO_LEVEL else FLAT)
                .build()

    private val favouriteForums: List<Forum>
        get() = forumRepo.favouriteForums
                .asList
                .formatAs(FLAT)
                .build()


    ///////////////////////////////////////////////////////////////////////////
    // Lifecycle
    ///////////////////////////////////////////////////////////////////////////


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = false
    }


    override fun onCreateView(aInflater: LayoutInflater, aContainer: ViewGroup?, aSavedState: Bundle?): View? {
        val view = inflateView(R.layout.forum_index_fragment, aContainer, aInflater)
        ButterKnife.bind(this, view)
        updateViewColours()
        refreshProbationBar()
        forumsListSwitcher.inAnimation = AnimationUtils.makeInAnimation(context, true)
        return view
    }


    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        val context = activity ?: return
        forumRepo = ForumRepository.getInstance(context)

        forumListAdapter = ForumListAdapter.getInstance(context, ArrayList(), this, mPrefs)
        forumRecyclerView.adapter = forumListAdapter
        forumRecyclerView.layoutManager = LinearLayoutManager(context)

        // this fixes the issue where the activity is first created with this fragment visible, but
        // doesn't set the actual titlebar text (leaves the xml default) until the viewpager triggers it
        setTitle(getTitle())
    }


    override fun onResume() {
        super.onResume()
        forumRepo.registerListener(this)
        if (lastUpdateTime != forumRepo.lastRefreshTime) {
            refreshForumList()
        } else {
            refreshNoDataView()
        }
        refreshProbationBar()
    }


    override fun onPause() {
        forumRepo.unregisterListener(this)
        super.onPause()
    }


    ///////////////////////////////////////////////////////////////////////////
    // Menus
    ///////////////////////////////////////////////////////////////////////////


    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.forum_index_fragment, menu)
        val toggleFavourites = menu?.findItem(R.id.toggle_list_fav_forums)
        toggleFavourites?.setIcon(if (showFavourites) R.drawable.ic_star_24dp else R.drawable.ic_star_border_24dp)
        toggleFavourites?.setTitle(if (showFavourites) R.string.forums_list_show_all_forums else R.string.forums_list_show_favorites_view)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toggle_list_fav_forums -> {
                // flip the view mode and refresh everything that needs to update
                showFavourites = !showFavourites
                invalidateOptionsMenu()
                setTitle(getTitle())
                refreshForumList()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    ///////////////////////////////////////////////////////////////////////////
    // Forum list setup and display
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Query the database for the current Forum data, and update the list
     */
    private fun refreshForumList() {
        lastUpdateTime = forumRepo.lastRefreshTime
        // get a new data set (possibly empty if there's no data yet) and give it to the adapter
        val forumList = if (showFavourites) favouriteForums else allForums
        forumListAdapter.updateForumList(forumList)
        refreshNoDataView()
    }


    /**
     * Show/hide the 'no data' view as appropriate, and show/hide the updating state
     */
    private fun refreshNoDataView() {
        // adjust the label in the 'no forums' view
        statusFrog.setStatusText(if (showFavourites) R.string.no_favourites else R.string.no_forums_data)

        // work out if we need to switch the empty view to the forum list, or vice versa
        val noData = forumListAdapter.parentItemList.isEmpty()
        if (noData && forumsListSwitcher.currentView === forumRecyclerView) {
            forumsListSwitcher.showNext()
        } else if (!noData && forumsListSwitcher.nextView === forumRecyclerView) {
            forumsListSwitcher.showNext()
        }
        // show the update spinner if an update is going on
        statusFrog.showSpinner(forumRepo.isUpdating)
    }


    ///////////////////////////////////////////////////////////////////////////
    // Event callbacks
    ///////////////////////////////////////////////////////////////////////////


    override fun onForumClicked(forum: Forum) {
        displayForum(forum.id.toLong(), 1)
    }


    override fun onContextMenuCreated(forum: Forum, contextMenu: Menu) {
        // show an option to set/unset the forum as a favourite
        val menuItem = contextMenu.add(if (forum.isFavourite) getString(R.string.forums_list_unset_favorite) else getString(R.string.forums_list_set_favorite))
        menuItem.setOnMenuItemClickListener {
            forumRepo.toggleFavorite(forum)
            forumListAdapter.notifyDataSetChanged()
            true
        }
    }

    override fun onForumsUpdateStarted() {
        activity?.runOnUiThread { statusFrog.showSpinner(true) }
    }


    override fun onForumsUpdateCompleted(success: Boolean) {
        activity?.runOnUiThread {
            if (success) {
                Snackbar.make(forumRecyclerView, R.string.forums_updated_message, Snackbar.LENGTH_SHORT).show()
                refreshForumList()
            }
            statusFrog.showSpinner(false)
        }
    }


    override fun onForumsUpdateCancelled() {
        activity?.runOnUiThread { statusFrog.showSpinner(false) }
    }


    override fun onPreferenceChange(prefs: AwfulPreferences, key: String?) {
        super.onPreferenceChange(prefs, key)
        if (getString(R.string.pref_key_theme) == key) {
            updateViewColours()
        } else if (getString(R.string.pref_key_favourite_forums) == key) {
            // only refresh the list if we're looking at the favourites
            if (showFavourites) {
                refreshForumList()
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Other stuff
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Set any colours that need to change according to the current theme
     */
    private fun updateViewColours() {
        forumRecyclerView.setBackgroundColor(ColorProvider.BACKGROUND.color)
    }

    override fun getTitle() = getString(if (showFavourites) R.string.favourite_forums_title else R.string.forums_title)

    override fun doScroll(down: Boolean): Boolean {
        val scrollAmount = forumRecyclerView.height / 2
        forumRecyclerView.smoothScrollBy(0, if (down) scrollAmount else -scrollAmount)
        return true
    }
}
