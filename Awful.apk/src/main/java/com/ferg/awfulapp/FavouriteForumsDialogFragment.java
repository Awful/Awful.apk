package com.ferg.awfulapp;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.ferg.awfulapp.forums.Forum;
import com.ferg.awfulapp.forums.ForumRepository;
import com.ferg.awfulapp.forums.ForumStructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by baka kaba on 26/05/2017.
 * <p>
 * A basic checklist dialog that shows and sets the user's favourite forums.
 * <p>
 * This gets the current forum list and marks any that appear in the current favourites list. If the
 * user hits OK, the favourites list is replaced with the currently selected forums.
 * <p>
 * This means that any favourites not present in the forum list (e.g. a forum that's been deleted)
 * can't be selected, and will essentially be removed when the user hits OK. A side effect is that
 * when the app has no forum data, no favourites can be selected at all, and if the user hits OK
 * then their favourites will be wiped. So you should avoid showing this dialog in that situation.
 */
public class FavouriteForumsDialogFragment extends DialogFragment {

    private ForumRepository forumRepo;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        forumRepo = ForumRepository.getInstance(getContext());
        List<Forum> forums = getAllForums();
        boolean[] currentFavourites = getFavouriteFilter(forums);
        boolean[] newFavourites = currentFavourites.clone();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        return builder.setTitle(R.string.manage_favourites_title)
                .setMultiChoiceItems(getForumTitles(forums), currentFavourites, (dialogInterface, i, b) -> newFavourites[i] = b)
                .setPositiveButton(R.string.alert_ok, (dialogInterface, i) -> forumRepo.setFavourites(filter(forums, newFavourites)))
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    private List<Forum> getAllForums() {
        return formatForumStructure(forumRepo.getAllForums());
    }

    private List<Forum> getFavouriteForums() {
        return formatForumStructure(forumRepo.getFavouriteForums());
    }

    private List<Forum> formatForumStructure(@NonNull ForumStructure structure) {
        return structure.getAsList().formatAs(ForumStructure.FLAT).includeSections(false).build();
    }


    /**
     * Get the {@link Forum#title}s for a list of forums.
     */
    @NonNull
    private String[] getForumTitles(@NonNull List<Forum> forums) {
        List<String> forumTitles = new ArrayList<>(forums.size());
        for (Forum forum : forums) {
            forumTitles.add(forum.title);
        }
        return forumTitles.toArray(new String[forumTitles.size()]);
    }


    /**
     * Get the 'is a favourite' status of each forum in a sequence.
     *
     * @return an array representing the status of each position in the list
     */
    @NonNull
    private boolean[] getFavouriteFilter(@NonNull List<Forum> forums) {
        List<Forum> favourites = getFavouriteForums();
        boolean[] checked = new boolean[forums.size()];
        for (int i = 0; i < forums.size(); i++) {
            if (favourites.contains(forums.get(i))) {
                checked[i] = true;
            }
        }
        return checked;
    }


    /**
     * Get items from a list that have a 'true' value at the same position in a filter array.
     *
     * @param items  The list of items to filter
     * @param filter An array of the same length, where <b>true</b> will retain the corresponding item
     * @return a new list with only the required items
     */
    @NonNull
    private <T> List<T> filter(@NonNull List<T> items, @NonNull boolean[] filter) {
        if (items.size() != filter.length)
            throw new IllegalArgumentException(String.format(Locale.US, "Items (%d) and filter (%d) are different lengths", items.size(), filter.length));
        List<T> filtered = new ArrayList<>();
        for (int j = 0; j < filter.length; j++) {
            if (filter[j]) {
                filtered.add(items.get(j));
            }
        }
        return filtered;
    }

}
