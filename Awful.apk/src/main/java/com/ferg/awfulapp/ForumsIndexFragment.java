package com.ferg.awfulapp;


import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ferg.awfulapp.databinding.ForumIndexFragmentBinding;
import com.google.android.material.snackbar.Snackbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ViewSwitcher;

import com.ferg.awfulapp.forums.Forum;
import com.ferg.awfulapp.forums.ForumListAdapter;
import com.ferg.awfulapp.forums.ForumRepository;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.widget.StatusFrog;

import java.util.ArrayList;
import java.util.List;

import static com.ferg.awfulapp.forums.ForumStructure.FLAT;
import static com.ferg.awfulapp.forums.ForumStructure.TWO_LEVEL;

/**
 * Created by baka kaba on 16/05/2016.
 * <p>
 * A fragment to display the current list of forums, or the user's favourites.
 * <p>
 * The fragment uses the {@link ForumRepository} to acquire {@link com.ferg.awfulapp.forums.ForumStructure}s
 * and format them according to the user's settings (e.g. as a single flat list), displaying the
 * results in a RecyclerView, and handling its click events. There is also a 'no data' view for when
 * the forum list is empty, with a customisable label and optional loading spinner.
 * <p>
 * There are two list displays, the full forums list and the user's favourite forums, switched by a
 * menu icon. In favourites mode, the user has the option to manage their list of favourites.
 * <p>
 * This fragment registers as a {@link com.ferg.awfulapp.forums.ForumRepository.ForumsUpdateListener}
 * to receive data update events, so it can refresh the forum list or display the loading spinner as
 * required.
 */
public class ForumsIndexFragment extends AwfulFragment
        implements ForumRepository.ForumsUpdateListener, ForumListAdapter.EventListener {

    private static final String KEY_SHOW_FAVOURITES = "show_favourites";

    RecyclerView forumRecyclerView;
    ViewSwitcher forumsListSwitcher;
    StatusFrog statusFrog;

    private ForumListAdapter forumListAdapter;
    private ForumRepository forumRepo;
    /**
     * repo timestamp for the currently displayed data, used to check if the repo has since updated
     */
    private long lastUpdateTime = -1;

    /**
     * Current view state - either showing the favourites list, or the full forums list
     */
    private boolean showFavourites = AwfulApplication.getAppStatePrefs().getBoolean(KEY_SHOW_FAVOURITES, false);


    ///////////////////////////////////////////////////////////////////////////
    // Lifecycle
    ///////////////////////////////////////////////////////////////////////////


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        showFavourites = AwfulApplication.getAppStatePrefs().getBoolean(KEY_SHOW_FAVOURITES, false);
        setHasOptionsMenu(true);
        setRetainInstance(false);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        ForumIndexFragmentBinding binding = ForumIndexFragmentBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        forumRecyclerView = binding.forumIndexList;
        forumsListSwitcher = binding.viewSwitcher;
        statusFrog = binding.statusFrog;
        updateViewColours();
        refreshProbationBar();
        forumsListSwitcher.setInAnimation(AnimationUtils.makeInAnimation(getContext(), true));
        return view;
    }


    @Override
    public void onActivityCreated(Bundle aSavedState) {
        super.onActivityCreated(aSavedState);
        Context context = getActivity();
        forumRepo = ForumRepository.getInstance(context);

        forumListAdapter = ForumListAdapter.getInstance(context, new ArrayList<>(), this, getPrefs());
        forumRecyclerView.setAdapter(forumListAdapter);
        forumRecyclerView.setLayoutManager(new LinearLayoutManager(context));
    }


    @Override
    public void onResume() {
        super.onResume();
        forumRepo.registerListener(this);
        if (lastUpdateTime != forumRepo.getLastRefreshTime()) {
            refreshForumList();
        } else {
            refreshNoDataView();
        }
        refreshProbationBar();
    }


    @Override
    public void onPause() {
        forumRepo.unregisterListener(this);
        super.onPause();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Menus
    ///////////////////////////////////////////////////////////////////////////


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forum_index_fragment, menu);
        MenuItem toggleFavourites = menu.findItem(R.id.toggle_list_fav_forums);
        toggleFavourites.setIcon(showFavourites ? R.drawable.ic_star_24dp : R.drawable.ic_star_border_24dp);
        toggleFavourites.setTitle(showFavourites ? R.string.forums_list_show_all_forums : R.string.forums_list_show_favorites_view);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toggle_list_fav_forums:
                // flip the view mode and refresh everything that needs to update
                showFavourites = !showFavourites;
                AwfulApplication.getAppStatePrefs().edit().putBoolean(KEY_SHOW_FAVOURITES, showFavourites).apply();
                invalidateOptionsMenu();
                refreshForumList();
                ((ForumsIndexActivity) getActivity()).onPageContentChanged();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Forum list setup and display
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Query the database for the current Forum data, and update the list
     */
    private void refreshForumList() {
        lastUpdateTime = forumRepo.getLastRefreshTime();
        // get a new data set (possibly empty if there's no data yet) and give it to the adapter
        List<Forum> forumList = showFavourites ? getFavouriteForums() : getAllForums();
        forumListAdapter.updateForumList(forumList);
        refreshNoDataView();
    }


    /**
     * Show/hide the 'no data' view as appropriate, and show/hide the updating state
     */
    private void refreshNoDataView() {
        statusFrog.showSpinner(false);
        if (showFavourites) {
            statusFrog.setStatusText(R.string.no_favourites);
        } else if (forumRepo.isUpdating()) {
            // an update is happening and this is the main forums list, so show the spinner and an active message
            statusFrog.setStatusText(R.string.getting_forums);
            statusFrog.showSpinner(true);
        } else {
            statusFrog.setStatusText(R.string.no_forums_data);
        }

        // work out if we need to switch the empty view to the forum list, or vice versa
        boolean noData = forumListAdapter.getParentItemList().isEmpty();
        if (noData && forumsListSwitcher.getCurrentView() == forumRecyclerView) {
            forumsListSwitcher.showNext();
        } else if (!noData && forumsListSwitcher.getNextView() == forumRecyclerView) {
            forumsListSwitcher.showNext();
        }
    }


    // list formatting for the forums

    private List<Forum> getAllForums() {
        return forumRepo.getAllForums()
                .getAsList()
                .includeSections(getPrefs().forumIndexShowSections)
                .formatAs(getPrefs().forumIndexHideSubforums ? TWO_LEVEL : FLAT)
                .build();
    }


    private List<Forum> getFavouriteForums() {
        return forumRepo.getFavouriteForums()
                .getAsList()
                .formatAs(FLAT)
                .build();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Event callbacks
    ///////////////////////////////////////////////////////////////////////////


    @Override
    public void onForumClicked(@NonNull Forum forum) {
        navigate(new NavigationEvent.Forum(forum.id, null));
    }

    @Override
    public void onContextMenuCreated(@NonNull Forum forum, @NonNull Menu contextMenu) {
        // show an option to set/unset the forum as a favourite
        MenuItem menuItem = contextMenu.add(forum.isFavourite() ? getString(R.string.forums_list_unset_favorite) : getString(R.string.forums_list_set_favorite));
        menuItem.setOnMenuItemClickListener(item -> {
            forumRepo.toggleFavorite(forum);
            forumListAdapter.notifyDataSetChanged();
            return true;
        });
    }

    @Override
    public void onForumsUpdateStarted() {
        getActivity().runOnUiThread(() -> statusFrog.showSpinner(true));
    }


    @Override
    public void onForumsUpdateCompleted(final boolean success) {
        getActivity().runOnUiThread(() -> {
            if (success) {
                Snackbar.make(forumRecyclerView, R.string.forums_updated_message, Snackbar.LENGTH_SHORT).show();
                refreshForumList();
            }
            statusFrog.showSpinner(false);
        });
    }


    @Override
    public void onForumsUpdateCancelled() {
        getActivity().runOnUiThread(() -> statusFrog.showSpinner(false));
    }


    @Override
    public void onPreferenceChange(@NonNull AwfulPreferences mPrefs, @Nullable String key) {
        super.onPreferenceChange(mPrefs, key);
        if (getString(R.string.pref_key_theme).equals(key)) {
            updateViewColours();
        } else if (getString(R.string.pref_key_favourite_forums).equals(key)) {
            // only refresh the list if we're looking at the favourites
            if (showFavourites && forumRepo != null) {
                refreshForumList();
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Other stuff
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Set any colours that need to change according to the current theme
     */
    private void updateViewColours() {
        if (forumRecyclerView != null) {
            forumRecyclerView.setBackgroundColor(ColorProvider.BACKGROUND.getColor());
        }
    }


    @Override
    public String getTitle() {
        if (isAdded()) {
            return getString(showFavourites ? R.string.favourite_forums_title : R.string.forums_title);
        }
        return "";
    }


    @Override
    protected boolean doScroll(boolean down) {
        int scrollAmount = forumRecyclerView.getHeight() / 2;
        forumRecyclerView.smoothScrollBy(0, down ? scrollAmount : -scrollAmount);
        return true;
    }
}
