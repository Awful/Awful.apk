/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the software nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.ferg.awfulapp;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.ViewSwitcher;

import com.ferg.awfulapp.forums.Forum;
import com.ferg.awfulapp.forums.ForumListAdapter;
import com.ferg.awfulapp.forums.ForumRepository;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.ColorProvider;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.ferg.awfulapp.forums.ForumStructure.FLAT;
import static com.ferg.awfulapp.forums.ForumStructure.TWO_LEVEL;

public class ForumsIndexFragment extends AwfulFragment
        implements ForumRepository.ForumsUpdateListener, ForumListAdapter.EventListener {


    @BindView(R.id.forum_index_list)
    RecyclerView forumRecyclerView;
    @BindString(R.string.forums_title)
    String forumsTitle;
    @BindView(R.id.view_switcher)
    ViewSwitcher forumsListSwitcher;
    @BindView(R.id.forums_update_progress_bar)
    ProgressBar updatingIndicator;

    private ForumListAdapter forumListAdapter;
    private ForumRepository forumRepo;
    /**
     * repo timestamp for the currently displayed data, used to check if the repo has since updated
     */
    private long lastUpdateTime = -1;


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


    @Override
    public void onPreferenceChange(AwfulPreferences mPrefs, String key) {
        super.onPreferenceChange(mPrefs, key);
        if (forumRepo != null) {
            refreshForumList();
        }
        updateViewColours();
    }


    /**
     * Set any colours that need to change according to the current theme
     */
    private void updateViewColours() {
        if (forumRecyclerView != null) {
            forumRecyclerView.setBackgroundColor(ColorProvider.BACKGROUND.getColor());
        }
    }


    @Override
    public void onForumsUpdateStarted() {
        getActivity().runOnUiThread(() -> updatingIndicator.setVisibility(VISIBLE));
    }


    @Override
    public void onForumsUpdateCompleted(final boolean success) {
        getActivity().runOnUiThread(() -> {
            if (success) {
                Snackbar.make(forumRecyclerView, "Forums updated", Snackbar.LENGTH_SHORT).show();
                refreshForumList();
            }
            updatingIndicator.setVisibility(INVISIBLE);
        });
    }


    @Override
    public void onForumsUpdateCancelled() {
        getActivity().runOnUiThread(() -> updatingIndicator.setVisibility(INVISIBLE));
    }


    /**
     * Query the database for the current Forum data, and update the list
     */
    private void refreshForumList() {
        lastUpdateTime = forumRepo.getLastRefreshTime();
        // get a new data set (possibly empty if there's no data yet) and give it to the adapter
        List<Forum> forumList = forumRepo.getForumStructure()
                .getAsList()
                .includeSections(mPrefs.forumIndexShowSections)
                .formatAs(mPrefs.forumIndexHideSubforums ? TWO_LEVEL : FLAT)
                .build();
        forumListAdapter.updateForumList(forumList);
        refreshNoDataView();
    }


    /**
     * Show/hide the 'no data' view as appropriate, and show/hide the updating state
     */
    private void refreshNoDataView() {
        boolean noData = forumListAdapter.getParentItemList().isEmpty();
        // work out if we need to switch the empty view to the forum list, or vice versa
        if (noData && forumsListSwitcher.getCurrentView() == forumRecyclerView) {
            forumsListSwitcher.showNext();
        } else if (!noData && forumsListSwitcher.getNextView() == forumRecyclerView) {
            forumsListSwitcher.showNext();
        }
        // show the update spinner if an update is going on
        updatingIndicator.setVisibility(forumRepo.isUpdating() ? VISIBLE : INVISIBLE);
    }


    @Override
    public void onForumClicked(@NonNull Forum forum) {
        displayForum(forum.id, 1);
    }


    @Override
    public String getTitle() {
        return forumsTitle;
    }


    @Override
    protected boolean doScroll(boolean down) {
        int scrollAmount = forumRecyclerView.getHeight() / 2;
        forumRecyclerView.smoothScrollBy(0, down ? scrollAmount : -scrollAmount);
        return true;
    }
}
