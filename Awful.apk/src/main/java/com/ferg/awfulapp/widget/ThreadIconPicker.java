package com.ferg.awfulapp.widget;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.view.menu.MenuBuilder;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.task.PostIconRequest;
import com.ferg.awfulapp.thread.AwfulPostIcon;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.ferg.awfulapp.constants.Constants.POST_ICON_REQUEST_TYPES.FORUM_POST;
import static com.ferg.awfulapp.constants.Constants.POST_ICON_REQUEST_TYPES.PM;
import static com.ferg.awfulapp.network.NetworkUtils.queueRequest;
import static com.ferg.awfulapp.thread.AwfulPostIcon.BLANK_ICON;

/**
 * Created by baka kaba on 19/11/2016.
 * <p>
 * A component that allows the user to select a thread/PM icon.
 * <p>
 * You need to call {@link #useForumIcons(int)} or {@link #usePrivateMessageIcons()} to set the
 * source of the icons the user can pick from, and then the icon view can be clicked to display
 * the icon sheet. Calling {@link #getIcon()} will get the currently selected icon, which defaults
 * to a blank 'no icon' version.
 *
 * @see AwfulPostIcon#BLANK_ICON
 */

public class ThreadIconPicker extends Fragment {

    private static final String TAG = ThreadIconPicker.class.getSimpleName();

    /**
     * fake forum ID so we can mix in the PM icons with the other forum icons
     */
    private static final int PM_FORUM_ID = -324546;

    private static final SparseArray<List<AwfulPostIcon>> iconsCache = new SparseArray<>();
    @BindView(R.id.selected_icon)
    ImageView selectedIconView;
    private AwfulPostIcon currentIcon = BLANK_ICON;
    @Nullable
    private Integer currentForumId = null;
    private ThemedBottomSheetDialog bottomSheet = null;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.icon_picker, container, true);
        ButterKnife.bind(this, view);
        return view;
    }


    /**
     * Set the picker to display the icons for a specific forum.
     * <p>
     * This clears any selected icon.
     * If you need to display the icons for private messages, call {@link #usePrivateMessageIcons()}
     * instead of passing an ID here.
     *
     * @param forumId the forum's ID on the site
     */
    public void useForumIcons(int forumId) {
        currentForumId = forumId;
        useIcon(BLANK_ICON);
        // check if we already loaded these
        if (iconsCache.get(forumId) != null) {
            Log.d(TAG, "useForumTags: Already cached for forum id: " + forumId);
            return;
        }
        // we need to fetch them icons
        loadTags(forumId == PM_FORUM_ID ? PM : FORUM_POST, forumId);
    }


    /**
     * Set the picker to display the icons for private messages.
     * <p>
     * This clears any selected icon.
     */
    public void usePrivateMessageIcons() {
        useForumIcons(PM_FORUM_ID);
    }


    /**
     * Show the bottom sheet containing the current icon set.
     * <p>
     * If an icon source hasn't been selected through {@link #useForumIcons(int)} or
     * {@link #usePrivateMessageIcons()}, this will do nothing.
     */
    @OnClick(R.id.selected_icon)
    public void showPicker() {
        if (currentForumId == null) {
            Log.w(TAG, "The user tried to select an icon before a source forum was set!\nYou should prevent this or initialise with one");
            return;
        }
        List<AwfulPostIcon> icons = iconsCache.get(currentForumId);
        if (icons == null) {
            Toast.makeText(getContext(), "Icons not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        showBottomSheet(icons);
    }


    /**
     * Display the bottom sheet populated with a list of icons the user can select from.
     */
    private void showBottomSheet(@NonNull List<AwfulPostIcon> icons) {
        if (bottomSheet != null) {
            bottomSheet.dismiss();
        }
        bottomSheet = new ThemedBottomSheetDialog(generatePostIconMenu(icons));
        // each icon's ID corresponds to its index in the list
        bottomSheet.setClickListeners(item -> useIcon(icons.get(item.getItemId())), null, null);
        bottomSheet.toggleVisible(getActivity());
    }


    /**
     * Set the currently selected icon.
     */
    private void useIcon(@NonNull AwfulPostIcon icon) {
        currentIcon = icon;
        selectedIconView.setImageResource(icon.drawableId);
    }


    /**
     * Get the currently selected icon.
     */
    @NonNull
    public AwfulPostIcon getIcon() {
        return currentIcon;
    }


    private void loadTags(@NonNull Constants.POST_ICON_REQUEST_TYPES iconType, int forumId) {
        // TODO: 19/11/2016 handle network requests
        queueRequest(new PostIconRequest(getActivity(), iconType, forumId)
                .build(null, new AwfulRequest.AwfulResultCallback<ArrayList<AwfulPostIcon>>() {

                    @Override
                    public void success(ArrayList<AwfulPostIcon> result) {
                        // add a blank 'no icon' icon too
                        result.add(0, BLANK_ICON);
                        // update the cache with these new icons
                        if (iconType == Constants.POST_ICON_REQUEST_TYPES.PM) {
                            iconsCache.put(PM_FORUM_ID, result);
                        } else if (iconType == FORUM_POST) {
                            iconsCache.put(forumId, result);
                        } else {
                            throw new RuntimeException("Unhandled post icon request type: " + iconType);
                        }
                    }

                    @Override
                    public void failure(VolleyError error) {
//                        new AwfulFragment.AlertBuilder().setTitle("Failed to retrieve posticons!").setSubtitle("Draft Saved").show();
                        Toast.makeText(getActivity(), "Failed to load icons\nForum ID " + forumId, Toast.LENGTH_SHORT).show();
                    }
                }));
    }


    /**
     * Create a menu from a list of icons, with their IDs and Orders set to their index positions.
     */
    @NonNull
    private Menu generatePostIconMenu(@NonNull List<AwfulPostIcon> postIcons) {
        Context context = getContext();
        Menu menu = new MenuBuilder(context);
        // add each icon, setting its ID to its index in the list
        AwfulPostIcon icon;
        for (int i = 0; i < postIcons.size(); i++) {
            icon = postIcons.get(i);
            menu.add(Menu.NONE, i, i, "")
                    .setIcon(icon.drawableId)
                    .setTitle(icon == BLANK_ICON ? "No icon" : "");
        }
        return menu;
    }

}
