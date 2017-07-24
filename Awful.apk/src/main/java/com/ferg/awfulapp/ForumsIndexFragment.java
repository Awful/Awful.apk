package com.ferg.awfulapp;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.ferg.awfulapp.forums.Forum;
import com.ferg.awfulapp.forums.ForumListAdapter;
import com.ferg.awfulapp.forums.ForumRepository;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.Keys;
import com.ferg.awfulapp.provider.ColorProvider;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
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

    @BindView(R.id.forum_index_list)
    RecyclerView forumRecyclerView;
    @BindView(R.id.view_switcher)
    ViewSwitcher forumsListSwitcher;
    @BindView(R.id.forums_update_progress_bar)
    ProgressBar updatingIndicator;
    @BindView(R.id.no_forums_label)
    TextView noForumsLabel;

    private ForumListAdapter forumListAdapter;
    private ForumRepository forumRepo;
    /**
     * repo timestamp for the currently displayed data, used to check if the repo has since updated
     */
    private long lastUpdateTime = -1;

    /**
     * Current view state - either showing the favourites list, or the full forums list
     */
    private boolean showFavourites = AwfulPreferences.getInstance().getPreference(Keys.FORUM_INDEX_PREFER_FAVOURITES, false);


    ///////////////////////////////////////////////////////////////////////////
    // Lifecycle
    ///////////////////////////////////////////////////////////////////////////


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(false);
    }


    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        View view = inflateView(R.layout.forum_index_fragment, aContainer, aInflater);
        ButterKnife.bind(this, view);
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

        forumListAdapter = ForumListAdapter.getInstance(context, new ArrayList<>(), this, mPrefs);
        forumRecyclerView.setAdapter(forumListAdapter);
        forumRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        // this fixes the issue where the activity is first created with this fragment visible, but
        // doesn't set the actual titlebar text (leaves the xml default) until the viewpager triggers it
        setTitle(getTitle());
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
                invalidateOptionsMenu();
                setTitle(getTitle());
                refreshForumList();
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
        // adjust the label in the 'no forums' view
        noForumsLabel.setText(showFavourites ? R.string.no_favourites : R.string.no_forums_data);

        // work out if we need to switch the empty view to the forum list, or vice versa
        boolean noData = forumListAdapter.getParentItemList().isEmpty();
        if (noData && forumsListSwitcher.getCurrentView() == forumRecyclerView) {
            forumsListSwitcher.showNext();
        } else if (!noData && forumsListSwitcher.getNextView() == forumRecyclerView) {
            forumsListSwitcher.showNext();
        }
        // show the update spinner if an update is going on
        updatingIndicator.setVisibility(forumRepo.isUpdating() ? VISIBLE : INVISIBLE);
    }


    // list formatting for the forums

    private List<Forum> getAllForums() {
        return forumRepo.getAllForums()
                .getAsList()
                .includeSections(mPrefs.forumIndexShowSections)
                .formatAs(mPrefs.forumIndexHideSubforums ? TWO_LEVEL : FLAT)
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
        displayForum(forum.id, 1);
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
        getActivity().runOnUiThread(() -> updatingIndicator.setVisibility(VISIBLE));
    }


    @Override
    public void onForumsUpdateCompleted(final boolean success) {
        getActivity().runOnUiThread(() -> {
            if (success) {
                Snackbar.make(forumRecyclerView, R.string.forums_updated_message, Snackbar.LENGTH_SHORT).show();
                refreshForumList();
            }
            updatingIndicator.setVisibility(INVISIBLE);
        });
    }


    @Override
    public void onForumsUpdateCancelled() {
        getActivity().runOnUiThread(() -> updatingIndicator.setVisibility(INVISIBLE));
    }


    @Override
    public void onPreferenceChange(AwfulPreferences mPrefs, @Nullable String key) {
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
        return getString(showFavourites ? R.string.favourite_forums_title : R.string.forums_title);
    }


    @Override
    protected boolean doScroll(boolean down) {
        int scrollAmount = forumRecyclerView.getHeight() / 2;
        forumRecyclerView.smoothScrollBy(0, down ? scrollAmount : -scrollAmount);
        return true;
    }
}
