package com.ferg.awfulapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ViewSwitcher
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ComposeView
import com.ferg.awfulapp.AwfulApplication.Companion.appStatePrefs
import com.ferg.awfulapp.databinding.ForumIndexFragmentBinding
import com.ferg.awfulapp.forums.Forum
import com.ferg.awfulapp.forums.ForumRepository
import com.ferg.awfulapp.forums.ForumRepository.ForumsUpdateListener
import com.ferg.awfulapp.forums.ForumStructure
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.provider.ColorProvider
import com.ferg.awfulapp.widget.StatusFrog
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.edit
import com.ferg.awfulapp.forums.ExpandableForumIndexList
import com.ferg.awfulapp.forums.ForumStructure.ListFormat


/**
 * Created by baka kaba on 16/05/2016.
 * 
 * 
 * A fragment to display the current list of forums, or the user's favourites.
 * 
 * 
 * The fragment uses the [ForumRepository] to acquire [ForumStructure]s
 * and format them according to the user's settings (e.g. as a single flat list), displaying the
 * results in a RecyclerView, and handling its click events. There is also a 'no data' view for when
 * the forum list is empty, with a customisable label and optional loading spinner.
 * 
 * 
 * There are two list displays, the full forums list and the user's favourite forums, switched by a
 * menu icon. In favourites mode, the user has the option to manage their list of favourites.
 * 
 * 
 * This fragment registers as a [ForumsUpdateListener]
 * to receive data update events, so it can refresh the forum list or display the loading spinner as
 * required.
 */
class ForumsIndexFragment : AwfulFragment(), ForumsUpdateListener {
    lateinit var forumIndexView: ComposeView
    private val forumListState = LazyListState()
    var forumsListSwitcher: ViewSwitcher? = null
    var statusFrog: StatusFrog? = null

    var forums = mutableStateListOf<Forum>()

    private lateinit var forumRepo: ForumRepository

    /**
     * repo timestamp for the currently displayed data, used to check if the repo has since updated
     */
    private var lastUpdateTime: Long = -1

    /**
     * Current view state - either showing the favourites list, or the full forums list
     */
    private var showFavourites = appStatePrefs!!.getBoolean(KEY_SHOW_FAVOURITES, false)


    ///////////////////////////////////////////////////////////////////////////
    // Lifecycle
    ///////////////////////////////////////////////////////////////////////////
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //        showFavourites = AwfulApplication.getAppStatePrefs().getBoolean(KEY_SHOW_FAVOURITES, false);
        setHasOptionsMenu(true)
        setRetainInstance(false)
    }


    override fun onCreateView(
        aInflater: LayoutInflater,
        aContainer: ViewGroup?,
        aSavedState: Bundle?
    ): View {
        val binding = ForumIndexFragmentBinding.inflate(getLayoutInflater())
        val view: View = binding.getRoot()
        forumIndexView = binding.forumIndexList
        forumIndexView.setContent {
            ExpandableForumIndexList(
                forums = forums,
                showSubtitles = prefs.forumIndexShowSubtitles,
                onForumClick = ::onForumClicked,
                onForumFavToggle = ::onForumFavToggle,
                listState = forumListState
            )
        }
        forumsListSwitcher = binding.viewSwitcher
        statusFrog = binding.statusFrog
        probationBar = binding.probationBar
        updateViewColours()
        refreshProbationBar()
        forumsListSwitcher?.inAnimation = AnimationUtils.makeInAnimation(context, true)
        awfulActivity?.setPreferredFont(view)
        return view
    }


    override fun onActivityCreated(aSavedState: Bundle?) {
        super.onActivityCreated(aSavedState)
        val context: Context = requireActivity()
        forumRepo = ForumRepository.getInstance(context)
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


    /**//////////////////////////////////////////////////////////////////////// */ // Menus
    /**//////////////////////////////////////////////////////////////////////// */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.forum_index_fragment, menu)
        val toggleFavourites = menu.findItem(R.id.toggle_list_fav_forums)
        toggleFavourites.setIcon(if (showFavourites) R.drawable.ic_star_24dp else R.drawable.ic_star_border_24dp)
        toggleFavourites.setTitle(if (showFavourites) R.string.forums_list_show_all_forums else R.string.forums_list_show_favorites_view)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toggle_list_fav_forums -> {
                // flip the view mode and refresh everything that needs to update
                showFavourites = !showFavourites
                appStatePrefs?.edit { putBoolean(KEY_SHOW_FAVOURITES, showFavourites) }
                invalidateOptionsMenu()
                refreshForumList()
                (activity as ForumsIndexActivity).onPageContentChanged()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    ///////////////////////////////////////////////////////////////////////////
    // Forum list setup and display
    ///////////////////////////////////////////////////////////////////////////
    /**
    * Query the database for the current Forum data , and update the list
    */
    private fun refreshForumList() {
        lastUpdateTime = forumRepo.lastRefreshTime
        // get a new data set (possibly empty if there's no data yet) and give it to the adapter
        val forumList = if (showFavourites) this.favouriteForums else this.allForums
        forums.clear()
        forums.addAll(forumList)
        refreshNoDataView()
    }


    /**
     * Show/hide the 'no data' view as appropriate, and show/hide the updating state
     */
    private fun refreshNoDataView() {
        statusFrog?.let {
            it.showSpinner(false)
            if (showFavourites) {
                it.setStatusText(R.string.no_favourites)
            } else if (forumRepo.isUpdating) {
                // an update is happening and this is the main forums list, so show the spinner and an active message
                it.setStatusText(R.string.getting_forums)
                it.showSpinner(true)
            } else {
                it.setStatusText(R.string.no_forums_data)
            }
        }

        // work out if we need to switch the empty view to the forum list, or vice versa
        val noData = forums.isEmpty()
        if (noData && forumsListSwitcher?.currentView === forumIndexView) {
            forumsListSwitcher?.showNext()
        } else if (!noData && forumsListSwitcher?.nextView === forumIndexView) {
            forumsListSwitcher?.showNext()
        }
    }


    private val allForums: MutableList<Forum>
        // list formatting for the forums
        get() = forumRepo.allForums
            .asList
            .includeSections(prefs.forumIndexShowSections)
            .formatAs(if (prefs.forumIndexHideSubforums) ListFormat.TWO_LEVEL else ListFormat.FLAT)
            .build()


    private val favouriteForums: MutableList<Forum>
        get() = forumRepo.favouriteForums
            .asList
            .formatAs(ListFormat.FLAT)
            .build()


    /////////////////////////////////////////////////////////////////////////
    // Event callbacks
    /////////////////////////////////////////////////////////////////////////
    fun onForumClicked(forum: Forum) {
        navigate(NavigationEvent.Forum(forum.id, null))
    }

    fun onForumFavToggle(forum: Forum) {
        forumRepo.toggleFavorite(forum)
        refreshForumList()
    }

    override fun onForumsUpdateStarted() {
        requireActivity().runOnUiThread { statusFrog?.showSpinner(true) }
    }


    override fun onForumsUpdateCompleted(success: Boolean) {
        requireActivity().runOnUiThread {
            if (success) {
                Snackbar.make(
                    forumIndexView,
                    R.string.forums_updated_message,
                    Snackbar.LENGTH_SHORT
                ).show()
                refreshForumList()
            }
            statusFrog?.showSpinner(false)
        }
    }


    override fun onForumsUpdateCancelled() {
        requireActivity().runOnUiThread { statusFrog?.showSpinner(false) }
    }


    override fun onPreferenceChange(preferences: AwfulPreferences, key: String?) {
        super.onPreferenceChange(preferences, key)
        if (getString(R.string.pref_key_theme) == key) {
            updateViewColours()
        } else if (getString(R.string.pref_key_favourite_forums) == key) {
            // only refresh the list if we're looking at the favourites
            if (showFavourites) {
                refreshForumList()
            }
        } else if(getString(R.string.pref_key_forum_index_show_section_headers) == key || getString(R.string.pref_key_forum_index_show_subtitles) == key) {
            refreshForumList()
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Other stuff
    ///////////////////////////////////////////////////////////////////////////
    /*
    * Set any colors that need to change according to the current theme
    */
    private fun updateViewColours() {
        forumIndexView.setBackgroundColor(ColorProvider.BACKGROUND.color)
    }


    override fun getTitle(): String {
        if (isAdded) {
            return getString(if (showFavourites) R.string.favourite_forums_title else R.string.forums_title)
        }
        return ""
    }


    override fun doScroll(down: Boolean): Boolean {
        val scrollAmount = forumIndexView.height / 2
        forumListState.dispatchRawDelta( if (down) scrollAmount.toFloat() else -scrollAmount.toFloat() )
        return true
    }

    companion object {
        private const val KEY_SHOW_FAVOURITES = "show_favourites"
    }
}
