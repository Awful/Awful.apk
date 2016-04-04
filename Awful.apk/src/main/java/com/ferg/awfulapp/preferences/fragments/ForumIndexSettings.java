package com.ferg.awfulapp.preferences.fragments;

import android.preference.Preference;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.forums.CrawlerTask;
import com.ferg.awfulapp.forums.ForumRepository;

import java.util.concurrent.TimeUnit;

/**
 * Created by baka kaba on 19/04/2016.
 * <p/>
 * Settings relating to the forum index.
 */
public class ForumIndexSettings extends SettingsFragment
        implements ForumRepository.ForumsUpdateListener {

    {
        SETTINGS_XML_RES_ID = R.xml.forum_index_settings;

        prefClickListeners.put(new UpdateForumsListener(), new int[]{
                R.string.pref_key_update_forums_menu_item
        });
    }

    private final ForumRepository forumRepo = ForumRepository.getInstance(null);
    private volatile boolean updateRunning = false;


    @Override
    public void onResume() {
        super.onResume();
        // assume we're not updating, if we are then the on-register callback will fix it
        updateRunning = false;
        setUpdateForumsSummary();
        forumRepo.registerListener(this);
    }


    @Override
    public void onPause() {
        super.onPause();
        forumRepo.unregisterListener(this);
    }


    @Override
    public void onForumsUpdateStarted() {
        updateRunning = true;
        setUpdateForumsSummary();
    }


    @Override
    public void onForumsUpdateCompleted(boolean success) {
        updateRunning = false;
        setUpdateForumsSummary();
    }


    @Override
    public void onForumsUpdateCancelled() {
        updateRunning = false;
        setUpdateForumsSummary();
    }


    private void setUpdateForumsSummary() {
        Preference updatePref = findPrefById(R.string.pref_key_update_forums_menu_item);
        if (updatePref != null) {
            if (updateRunning) {
                updatePref.setSummary(R.string.forum_index_update_forums_summary_updating);
            } else {
                String lastUpdateMessage = getActivity().getResources().getString(R.string.forum_index_update_forums_summary_not_updating);
                TimeUnit timeUnit = TimeUnit.HOURS;
                long lastUpdate = System.currentTimeMillis() - forumRepo.getLastUpdateTime();
                long when = timeUnit.convert(lastUpdate, TimeUnit.MILLISECONDS);
                updatePref.setSummary(String.format(lastUpdateMessage, when, timeUnit.toString().toLowerCase()));
            }
        }
    }


    private class UpdateForumsListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            // TODO: maybe move this into a full sync button somewhere, that does forum features etc
            forumRepo.updateForums(new CrawlerTask(getActivity(), CrawlerTask.PRIORITY_HIGH));
            return true;
        }
    }

}
