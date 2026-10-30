package com.ferg.awfulapp.popupmenu

import android.os.Bundle
import android.util.Log
import androidx.annotation.DrawableRes
import com.ferg.awfulapp.AwfulActivity
import com.ferg.awfulapp.NavigationEvent.ComposePrivateMessage
import com.ferg.awfulapp.NavigationEvent.LepersColony
import com.ferg.awfulapp.R
import com.ferg.awfulapp.ThreadDisplayFragment
import com.ferg.awfulapp.popupmenu.ModeratePostMenu.Companion.newInstance
import com.ferg.awfulapp.popupmenu.PostContextMenu.PostMenuAction
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.thread.AwfulMessage

/**
 * Created by baka kaba on 23/05/2017.
 * 
 * 
 * A popup menu for a post in a thread.
 */
class PostContextMenu : BasePopupMenu<PostMenuAction>() {
    private var posterUserId = 0
    private var editable = false
    private var hasModControls = false
    private var posterHasPlat = false
    private var posterHasRole = false
    private var posterIsUnreportable = false
    private var threadId = 0
    private var postId = 0
    private var lastReadCode = 0
    private var postFilterUserId = 0
    private var posterUsername: String? = null
    private var posterAvatarUrl: String? = null


    override fun init(args: Bundle) {
        posterUserId = args.getInt(ARG_POSTER_USER_ID)
        editable = args.getBoolean(ARG_EDITABLE)
        hasModControls = args.getBoolean(ARG_HAS_MOD_CONTROLS);
        posterHasPlat = args.getBoolean(ARG_POSTER_HAS_PLAT)
        val posterRole = args.getString(ARG_POSTER_ROLE, "")
        posterHasRole = posterRole.isNotEmpty()
        posterIsUnreportable = "admin" == posterRole || "coder" == posterRole
        threadId = args.getInt(ARG_THREAD_ID)
        postId = args.getInt(ARG_POST_ID)
        lastReadCode = args.getInt(ARG_LAST_READ_CODE)
        posterUsername = args.getString(ARG_POSTER_USERNAME)
        postFilterUserId = args.getInt(ARG_POST_FILTER_USER_ID)
        posterAvatarUrl = args.getString(ARG_POSTER_AVATAR_URL)
    }


    override fun generateMenuItems(): MutableList<PostMenuAction?> {
        val prefs = AwfulPreferences.getInstance()
        val youHavePlat = prefs.hasPlatinum
        val ownPost = prefs.username == posterUsername

        val awfulActions: MutableList<PostMenuAction?> = ArrayList<PostMenuAction?>()

        awfulActions.add(PostMenuAction.QUOTE)
        if (editable && ownPost) {
            awfulActions.add(PostMenuAction.EDIT)
        }
        awfulActions.add(PostMenuAction.MARK_LAST_SEEN)
        if (!ownPost && youHavePlat && (posterHasPlat || posterHasRole)) {
            awfulActions.add(PostMenuAction.SEND_PM)
        }
        awfulActions.add(if (ownPost) PostMenuAction.YOUR_POSTS else PostMenuAction.USER_POSTS)
        if (!ownPost) {
            awfulActions.add(if (prefs.markedUsers?.contains(posterUsername) == true) PostMenuAction.UNMARK_USER else PostMenuAction.MARK_USER)
        }
        if (!ownPost && !posterIsUnreportable) {
            awfulActions.add(PostMenuAction.REPORT_POST)
        }
        awfulActions.add(PostMenuAction.COPY_URL)
        awfulActions.add(PostMenuAction.RAP_SHEET)
        if (!ownPost && !posterHasRole) {
            awfulActions.add(PostMenuAction.IGNORE_USER)
        }
        if (prefs.avatarsEnabled && posterAvatarUrl != null) {
            awfulActions.add(if (prefs.blockedAvatarUrls?.contains(posterAvatarUrl) == true) PostMenuAction.SHOW_AVATAR else PostMenuAction.HIDE_AVATAR)
        }
        if (!ownPost && (editable || hasModControls)) {
            awfulActions.add(PostMenuAction.MODERATE);
        }
        return awfulActions
    }


    override fun onActionClicked(action: PostMenuAction) {
        val parent = targetFragment as ThreadDisplayFragment?
        if (parent == null) {
            Log.w(TAG, "onActionClicked: can't get target ThreadDisplayFragment")
            return
        }
        val activity = activity as AwfulActivity?
        when (action) {
            PostMenuAction.SEND_PM -> activity?.navigate(ComposePrivateMessage(posterUsername))

            PostMenuAction.QUOTE -> parent.displayPostReplyDialog(
                threadId,
                postId,
                AwfulMessage.TYPE_QUOTE
            )

            PostMenuAction.EDIT -> parent.displayPostReplyDialog(
                threadId,
                postId,
                AwfulMessage.TYPE_EDIT
            )

            PostMenuAction.MARK_LAST_SEEN -> parent.markLastRead(lastReadCode)
            PostMenuAction.COPY_URL -> parent.copyThreadURL(postId, postFilterUserId)
            PostMenuAction.YOUR_POSTS, PostMenuAction.USER_POSTS -> parent.toggleUserPosts(
                postId,
                posterUserId,
                posterUsername
            )

            PostMenuAction.RAP_SHEET -> activity?.navigate(LepersColony(posterUserId))

            PostMenuAction.UNMARK_USER, PostMenuAction.MARK_USER -> parent.toggleMarkUser(
                posterUsername
            )

            PostMenuAction.IGNORE_USER -> parent.ignoreUser(posterUserId)
            PostMenuAction.REPORT_POST -> parent.reportUser(postId)
            PostMenuAction.SHOW_AVATAR, PostMenuAction.HIDE_AVATAR -> parent.toggleAvatar(
                posterAvatarUrl
            )

            PostMenuAction.MODERATE -> {
                val moderateMenu = newInstance(
                    threadId, postId,
                    posterUserId, editable, hasModControls
                )
                moderateMenu.setTargetFragment(parent, -1)
                moderateMenu.show(parent.getParentFragmentManager(), ModeratePostMenu.TAG)
            }
        }
    }


    override fun getMenuLabel(action: PostMenuAction): String {
        if (action == PostMenuAction.MODERATE) {
            return getString(R.string.action_moderate_post);
        }
        // need to add the post's username to some of these
        return String.format(action.menuLabel, posterUsername)
    }

    override fun getTitle(): String {
        return "Select an action"
    }

    enum class PostMenuAction(@param:DrawableRes override val iconId: Int, override val menuLabel: String) :
        AwfulAction {
        // the parameters here are for the post owner's username
        QUOTE(R.drawable.ic_format_quote_dark, "Quote Post"),
        EDIT(R.drawable.ic_create_dark, "Edit Post"),
        MARK_LAST_SEEN(R.drawable.ic_visibility_dark, "Mark post last read"),
        SEND_PM(R.drawable.ic_mail_dark, "PM %s"),
        USER_POSTS(R.drawable.ic_user_posts_dark, "Show only posts by %s"),
        UNMARK_USER(R.drawable.ic_account_minus_dark, "Unmark %s"),
        MARK_USER(R.drawable.ic_account_plus_dark, "Mark %s"),
        YOUR_POSTS(R.drawable.ic_user_posts_dark, "Show only your posts"),
        REPORT_POST(R.drawable.ic_announcement_dark_24dp, "Report Post"),
        COPY_URL(R.drawable.ic_share_dark, "Copy URL"),
        RAP_SHEET(R.drawable.ic_gavel_dark_24dp, "%s's rap sheet"),
        IGNORE_USER(R.drawable.ic_ignore_dark, "Ignore %s"),
        SHOW_AVATAR(R.drawable.ic_visibility_dark, "Show avatar of %s"),
        HIDE_AVATAR(R.drawable.ic_ignore_dark, "Hide avatar of %s"),
        // label comes from R.string.action_moderate_post, see #getMenuLabel
        MODERATE(R.drawable.ic_info_dark, "")
    }

    companion object {
        val TAG: String = PostContextMenu::class.java.simpleName

        private const val ARG_POSTER_USER_ID = "posterUserId"
        private const val ARG_EDITABLE = "editable"
        private const val ARG_HAS_MOD_CONTROLS = "hasModControls"
        private const val ARG_POSTER_HAS_PLAT = "posterHasPlat"
        private const val ARG_POSTER_ROLE = "posterRole"
        private const val ARG_THREAD_ID = "threadId"
        private const val ARG_POST_ID = "postId"
        private const val ARG_LAST_READ_CODE = "lastReadCode"
        private const val ARG_POSTER_USERNAME = "posterUsername"
        private const val ARG_POST_FILTER_USER_ID = "postFilterUserId"
        private const val ARG_POSTER_AVATAR_URL = "posterAvatarUrl"

        /**
         * Get a menu for a given post.
         * 
         * @param threadId           the ID of the thread the post belongs to
         * @param postId             the ID of the post the menu is for
         * @param lastReadCode       the ID code used when marking a post as the last-read (see [ThreadDisplayFragment.markLastRead])
         * @param editable           true if the post can be edited by you
         * @param posterUsername     the username of the post creator
         * @param posterUserId       the user ID of the post creator
         * @param posterHasPlat      true if the post creator has a platinum account
         * @param posterRole         the role of the post creator (e.g. "admin", "mod"), or null if none
         * @param posterAvatarUrl    the URL of the post creator's avatar
         * @return the configured menu, ready to show
         */
        fun newInstance(
            threadId: Int,
            postId: Int,
            lastReadCode: Int,
            editable: Boolean,
            hasModControls: Boolean,
            posterUsername: String,
            posterUserId: Int,
            posterHasPlat: Boolean,
            posterRole: String?,
            postFilterUserId: Int?,
            posterAvatarUrl: String?
        ): PostContextMenu {
            val fragment = PostContextMenu()
            val args = Bundle().apply {
                putInt(ARG_POSTER_USER_ID, posterUserId)
                putBoolean(ARG_EDITABLE, editable)
                putBoolean(ARG_HAS_MOD_CONTROLS, hasModControls)
                putBoolean(ARG_POSTER_HAS_PLAT, posterHasPlat)
                putString(ARG_POSTER_ROLE, posterRole)
                putInt(ARG_THREAD_ID, threadId)
                putInt(ARG_POST_ID, postId)
                putInt(ARG_LAST_READ_CODE, lastReadCode)
                putString(ARG_POSTER_USERNAME, posterUsername)
                putString(ARG_POSTER_AVATAR_URL, posterAvatarUrl)
                if (postFilterUserId != null) {
                    putInt(ARG_POST_FILTER_USER_ID, postFilterUserId)
                }
            }
            fragment.setArguments(args)
            return fragment
        }
    }
}
