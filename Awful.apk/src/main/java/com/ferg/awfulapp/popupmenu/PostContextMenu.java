package com.ferg.awfulapp.popupmenu;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.util.Log;

import com.ferg.awfulapp.AwfulActivity;
import com.ferg.awfulapp.NavigationEvent;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.ThreadDisplayFragment;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.thread.AwfulMessage;

import java.util.ArrayList;
import java.util.List;

import static com.ferg.awfulapp.popupmenu.PostContextMenu.PostMenuAction.COPY_URL;
import static com.ferg.awfulapp.popupmenu.PostContextMenu.PostMenuAction.EDIT;
import static com.ferg.awfulapp.popupmenu.PostContextMenu.PostMenuAction.IGNORE_USER;
import static com.ferg.awfulapp.popupmenu.PostContextMenu.PostMenuAction.MARK_LAST_SEEN;
import static com.ferg.awfulapp.popupmenu.PostContextMenu.PostMenuAction.MARK_USER;
import static com.ferg.awfulapp.popupmenu.PostContextMenu.PostMenuAction.REPORT_POST;
import static com.ferg.awfulapp.popupmenu.PostContextMenu.PostMenuAction.SEND_PM;
import static com.ferg.awfulapp.popupmenu.PostContextMenu.PostMenuAction.UNMARK_USER;
import static com.ferg.awfulapp.popupmenu.PostContextMenu.PostMenuAction.USER_POSTS;
import static com.ferg.awfulapp.popupmenu.PostContextMenu.PostMenuAction.YOUR_POSTS;

/**
 * Created by baka kaba on 23/05/2017.
 * <p>
 * A popup menu for a post in a thread.
 */
public class PostContextMenu extends BasePopupMenu<PostContextMenu.PostMenuAction> {

    public static final String TAG = PostContextMenu.class.getSimpleName();

    private static final String ARG_POSTER_USER_ID = "posterUserId";
    private static final String ARG_EDITABLE = "editable";
    private static final String ARG_POSTER_HAS_PLAT = "posterHasPlat";
    private static final String ARG_POSTER_IS_ADMIN_OR_MOD = "posterIsAdminOrMod";
    private static final String ARG_THREAD_ID = "threadId";
    private static final String ARG_POST_ID = "postId";
    private static final String ARG_LAST_READ_CODE = "lastReadCode";
    private static final String ARG_POSTER_USERNAME = "posterUsername";

    private int posterUserId;
    private boolean editable;
    private boolean posterHasPlat;
    private boolean posterIsAdminOrMod;
    private int threadId;
    private int postId;
    private int lastReadCode;
    private String posterUsername = null;


    /**
     * Get a menu for a given post.
     *
     * @param threadId           the ID of the thread the post belongs to
     * @param postId             the ID of the post the menu is for
     * @param lastReadCode       the ID code used when marking a post as the last-read (see {@link ThreadDisplayFragment#markLastRead(int)})
     * @param editable           true if the post can be edited by you
     * @param posterUsername     the username of the post creator
     * @param posterUserId       the user ID of the post creator
     * @param posterHasPlat      true if the post creator has a platinum account
     * @param posterIsAdminOrMod true if the post creator has mod/admin status
     * @return the configured menu, ready to show
     */
    public static PostContextMenu newInstance(int threadId,
                                              int postId,
                                              int lastReadCode,
                                              boolean editable,
                                              @NonNull String posterUsername,
                                              int posterUserId,
                                              boolean posterHasPlat,
                                              boolean posterIsAdminOrMod) {
        Bundle args = new Bundle();
        PostContextMenu fragment = new PostContextMenu();

        args.putInt(ARG_POSTER_USER_ID, posterUserId);
        args.putBoolean(ARG_EDITABLE, editable);
        args.putBoolean(ARG_POSTER_HAS_PLAT, posterHasPlat);
        args.putBoolean(ARG_POSTER_IS_ADMIN_OR_MOD, posterIsAdminOrMod);
        args.putInt(ARG_THREAD_ID, threadId);
        args.putInt(ARG_POST_ID, postId);
        args.putInt(ARG_LAST_READ_CODE, lastReadCode);
        args.putString(ARG_POSTER_USERNAME, posterUsername);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    void init(@NonNull Bundle args) {
        posterUserId = args.getInt(ARG_POSTER_USER_ID);
        editable = args.getBoolean(ARG_EDITABLE);
        posterHasPlat = args.getBoolean(ARG_POSTER_HAS_PLAT);
        posterIsAdminOrMod = args.getBoolean(ARG_POSTER_IS_ADMIN_OR_MOD);
        threadId = args.getInt(ARG_THREAD_ID);
        postId = args.getInt(ARG_POST_ID);
        lastReadCode = args.getInt(ARG_LAST_READ_CODE);
        posterUsername = args.getString(ARG_POSTER_USERNAME);
    }


    @NonNull
    @Override
    List<PostMenuAction> generateMenuItems() {
        AwfulPreferences prefs = AwfulPreferences.getInstance();
        boolean youHavePlat = prefs.hasPlatinum;
        boolean ownPost = prefs.username.equals(posterUsername);

        List<PostMenuAction> awfulActions = new ArrayList<>();

        awfulActions.add(PostMenuAction.QUOTE);
        if (editable) {
            awfulActions.add(EDIT);
        }
        awfulActions.add(MARK_LAST_SEEN);
        if (!ownPost && youHavePlat && (posterHasPlat || posterIsAdminOrMod)) {
            awfulActions.add(SEND_PM);
        }
        awfulActions.add(ownPost ? YOUR_POSTS : USER_POSTS);
        if (!ownPost) {
            awfulActions.add(prefs.markedUsers.contains(posterUsername) ? UNMARK_USER : MARK_USER);
        }
        if (!ownPost && youHavePlat && !posterIsAdminOrMod) {
            awfulActions.add(REPORT_POST);
        }
        awfulActions.add(COPY_URL);
        if (!ownPost) {
            awfulActions.add(IGNORE_USER);
        }
        return awfulActions;
    }


    @Override
    void onActionClicked(@NonNull PostMenuAction action) {
        ThreadDisplayFragment parent = (ThreadDisplayFragment) getTargetFragment();
        if (parent == null) {
            Log.w(TAG, "onActionClicked: can't get target ThreadDisplayFragment");
            return;
        }
        switch (action) {
            case SEND_PM:
                AwfulActivity activity = (AwfulActivity) getActivity();
                if (activity != null) {
                    activity.navigate(new NavigationEvent.ComposePrivateMessage(posterUsername));
                }
                break;
            case QUOTE:
                parent.displayPostReplyDialog(threadId, postId, AwfulMessage.TYPE_QUOTE);
                break;
            case EDIT:
                parent.displayPostReplyDialog(threadId, postId, AwfulMessage.TYPE_EDIT);
                break;
            case MARK_LAST_SEEN:
                parent.markLastRead(lastReadCode);
                break;
            case COPY_URL:
                parent.copyThreadURL(postId);
                break;
            case YOUR_POSTS:
            case USER_POSTS:
                parent.toggleUserPosts(postId, posterUserId, posterUsername);
                break;
            case UNMARK_USER:
            case MARK_USER:
                parent.toggleMarkUser(posterUsername);
                break;
            case IGNORE_USER:
                parent.ignoreUser(posterUserId);
                break;
            case REPORT_POST:
                parent.reportUser(postId);
                break;
        }
    }


    @NonNull
    @Override
    String getMenuLabel(@NonNull PostMenuAction action) {
        // need to add the post's username to some of these
        return String.format(action.getMenuLabel(), posterUsername);
    }

    @NonNull
    @Override
    public String getTitle() {
        return "Select an action";
    }

    enum PostMenuAction implements AwfulAction {
        // the parameters here are for the post owner's username
        QUOTE(R.drawable.ic_format_quote_dark, "Quote Post"),
        EDIT(R.drawable.ic_create_dark, "Edit Post"),
        MARK_LAST_SEEN(R.drawable.ic_visibility_dark, "Mark post last read"),
        SEND_PM(R.drawable.ic_mail_dark, "PM %s"),
        USER_POSTS(R.drawable.ic_user_posts_dark, "Show only posts by %s"),
        UNMARK_USER(R.drawable.ic_account_minus_dark, "Unmark %s"),
        MARK_USER(R.drawable.ic_account_plus_dark, "Mark %s"),
        YOUR_POSTS(R.drawable.ic_user_posts_dark, "Show only your posts"),
        REPORT_POST(R.drawable.ic_error_dark, "Report Post"),
        COPY_URL(R.drawable.ic_share_dark, "Copy URL"),
        IGNORE_USER(R.drawable.ic_ignore_dark, "Ignore %s");

        final int iconId;
        @NonNull
        private final String menuText;

        PostMenuAction(@DrawableRes int iconId, @NonNull String menuText) {
            this.iconId = iconId;
            this.menuText = menuText;
        }

        @Override
        public int getIconId() {
            return iconId;
        }

        @Override
        @NonNull
        public String getMenuLabel() {
            return menuText;
        }
    }
}
