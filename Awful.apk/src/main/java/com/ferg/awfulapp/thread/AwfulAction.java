package com.ferg.awfulapp.thread;

import android.util.Log;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.preferences.AwfulPreferences;

import java.util.ArrayList;

/**
 * Created by Christoph on 25.03.2016.
 */
public class AwfulAction {

    public static ArrayList<AwfulAction> getPostActions(String username, boolean editable, boolean isAdminOrMod, boolean isPlat) {
        AwfulPreferences prefs = AwfulPreferences.getInstance();
        ArrayList<AwfulAction> awfulActions = new ArrayList<>();

        awfulActions.add(new AwfulAction(ActionType.QUOTE, R.drawable.ic_format_quote_dark, "Quote Post"));
        if (editable) {
            awfulActions.add(new AwfulAction(ActionType.EDIT, R.drawable.ic_create_dark, "Edit Post"));
        }
        awfulActions.add(new AwfulAction(ActionType.MARK_LAST_SEEN, R.drawable.ic_visibility_dark, "Mark post last read"));
        if (prefs.hasPlatinum && (isPlat || isAdminOrMod) && !username.equals(prefs.username)) {
            awfulActions.add(new AwfulAction(ActionType.SEND_PM, R.drawable.ic_mail_dark, "PM " + username));
        }
        if(!username.equals(prefs.username)){
            awfulActions.add(new AwfulAction(ActionType.USER_POSTS, R.drawable.ic_user_posts_dark, "Show only posts by " + username));
            if (prefs.markedUsers.contains(username)) {
                awfulActions.add(new AwfulAction(ActionType.MARK_USER, R.drawable.ic_account_minus_dark, "Unmark " + username));
            } else {
                awfulActions.add(new AwfulAction(ActionType.MARK_USER, R.drawable.ic_account_plus_dark, "Mark " + username));
            }
        }else{
            awfulActions.add(new AwfulAction(ActionType.USER_POSTS, R.drawable.ic_user_posts_dark, "Show only your posts"));
        }
        if (prefs.hasPlatinum && !isAdminOrMod && !username.equals(prefs.username)) {
            awfulActions.add(new AwfulAction(ActionType.REPORT_POST, R.drawable.ic_error_dark, "Report Post"));
        }
        awfulActions.add(new AwfulAction(ActionType.COPY_URL, R.drawable.ic_share_dark, "Copy URL"));
        if(!username.equals(prefs.username)){
            awfulActions.add(new AwfulAction(ActionType.IGNORE_USER, R.drawable.ic_ignore_dark, "Ignore " + username));
        }
        return awfulActions;
    }

    public static ArrayList<AwfulAction> getURLActions(String url, boolean isImage, boolean isGif) {
        ArrayList<AwfulAction> awfulActions = new ArrayList<>();

        if(isImage){
            awfulActions.add(new AwfulAction(ActionType.DOWNLOAD_IMAGE, R.drawable.ic_file_download_dark, "Download Image"));
            if(isGif){
                awfulActions.add(new AwfulAction(ActionType.SHOW_INLINE, R.drawable.ic_movie_dark, "Play .gif" ));
            }else{
                awfulActions.add(new AwfulAction(ActionType.SHOW_INLINE, R.drawable.ic_photo_dark, "Show Image inline"));
            }
        }

        awfulActions.add(new AwfulAction(ActionType.OPEN_URL, R.drawable.ic_open_in_app_dark, "Open URL"));
        awfulActions.add(new AwfulAction(ActionType.COPY_LINK_URL, R.drawable.ic_insert_link_dark, "Copy URL"));
        awfulActions.add(new AwfulAction(ActionType.SHARE_URL, R.drawable.ic_share_dark, "Share URL"));

        return awfulActions;
    }

    public AwfulAction(ActionType type, int icon, String title){
        actionIcon = icon;
        actionTitle = title;
        actionType = type;
    }

    private ActionType actionType;
    private int actionIcon;
    private String actionTitle;

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public int getActionIcon() {
        return actionIcon;
    }

    public void setActionIcon(int actionIcon) {
        this.actionIcon = actionIcon;
    }

    public String getActionTitle() {
        return actionTitle;
    }

    public void setActionTitle(String actionTitle) {
        this.actionTitle = actionTitle;
    }

    public enum ActionType {
        QUOTE, EDIT, MARK_LAST_SEEN, SEND_PM, COPY_URL, USER_POSTS, IGNORE_USER, MARK_USER, REPORT_POST, DOWNLOAD_IMAGE, SHOW_INLINE, OPEN_URL, COPY_LINK_URL, SHARE_URL
    }
}
