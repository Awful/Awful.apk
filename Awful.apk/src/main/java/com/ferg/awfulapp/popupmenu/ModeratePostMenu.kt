package com.ferg.awfulapp.popupmenu

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ferg.awfulapp.AwfulActivity
import com.ferg.awfulapp.NavigationEvent
import com.ferg.awfulapp.R
import com.ferg.awfulapp.ThreadDisplayFragment
import com.ferg.awfulapp.popupmenu.ModeratePostMenu.ModerateMenuAction.EDIT_POST
import com.ferg.awfulapp.popupmenu.ModeratePostMenu.ModerateMenuAction.REQUEST_BAN
import com.ferg.awfulapp.popupmenu.ModeratePostMenu.ModerateMenuAction.REQUEST_PROBATION
import com.ferg.awfulapp.thread.AwfulMessage

/**
 * Context menu with moderation actions for a post.
 *
 * Shows an edit option when the post is editable by the current user, plus the modqueue
 * probation/ban request options when the post carries modqueue controls.
 */
class ModeratePostMenu : BasePopupMenu<ModeratePostMenu.ModerateMenuAction>() {

    private var threadId = 0
    private var postId = 0
    private var posterUserId = 0
    private var editable = false
    private var hasModControls = false

    companion object {
        @JvmField
        val TAG: String = ModeratePostMenu::class.java.simpleName

        private const val ARG_THREAD_ID = "threadId"
        private const val ARG_POST_ID = "postId"
        private const val ARG_POSTER_USER_ID = "posterUserId"
        private const val ARG_EDITABLE = "editable"
        private const val ARG_HAS_MOD_CONTROLS = "hasModControls"

        /**
         * Get a moderation menu for a post, where [editable] adds the edit option and
         * [hasModControls] adds the modqueue probation/ban request options.
         */
        @JvmStatic
        fun newInstance(threadId: Int, postId: Int, posterUserId: Int, editable: Boolean, hasModControls: Boolean) =
                ModeratePostMenu().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_THREAD_ID, threadId)
                        putInt(ARG_POST_ID, postId)
                        putInt(ARG_POSTER_USER_ID, posterUserId)
                        putBoolean(ARG_EDITABLE, editable)
                        putBoolean(ARG_HAS_MOD_CONTROLS, hasModControls)
                    }
                }
    }

    override fun init(args: Bundle) = with(args) {
        threadId = getInt(ARG_THREAD_ID)
        postId = getInt(ARG_POST_ID)
        posterUserId = getInt(ARG_POSTER_USER_ID)
        editable = getBoolean(ARG_EDITABLE)
        hasModControls = getBoolean(ARG_HAS_MOD_CONTROLS)
    }

    override fun generateMenuItems() =
            mutableListOf<ModerateMenuAction>()
                    .apply { if (editable) add(EDIT_POST) }
                    .apply { if (hasModControls) { add(REQUEST_PROBATION); add(REQUEST_BAN) } }

    override fun getMenuLabel(action: ModerateMenuAction): String = getString(action.menuLabelRes)

    override fun getTitle(): String = getString(R.string.action_moderate_post)

    override fun onActionClicked(action: ModerateMenuAction) {
        when (action) {
            EDIT_POST ->
                (targetFragment as? ThreadDisplayFragment)
                        ?.displayPostReplyDialog(threadId, postId, AwfulMessage.TYPE_EDIT)
            REQUEST_PROBATION ->
                navigateTo(NavigationEvent.ModQueueRequest(ban = false, userId = posterUserId, postId = postId, threadId = threadId))
            REQUEST_BAN ->
                navigateTo(NavigationEvent.ModQueueRequest(ban = true, userId = posterUserId, postId = postId, threadId = threadId))
        }
    }

    private fun navigateTo(event: NavigationEvent) {
        (activity as AwfulActivity?)?.navigate(event)
    }

    enum class ModerateMenuAction(
            @DrawableRes private val iconResId: Int,
            @StringRes val menuLabelRes: Int
    ) : AwfulAction {

        EDIT_POST(R.drawable.ic_create_dark, R.string.action_edit_post),
        REQUEST_PROBATION(R.drawable.ic_history_dark_24dp, R.string.action_request_probation),
        REQUEST_BAN(R.drawable.ic_gavel_dark_24dp, R.string.action_request_ban);

        override fun getIconId() = iconResId
        // labels are resolved from menuLabelRes in getMenuLabel above, where a Context is available
        override fun getMenuLabel() = ""
    }
}
