package com.ferg.awfulapp.popupmenu

import android.os.Bundle
import android.support.annotation.DrawableRes
import com.ferg.awfulapp.AwfulActivity
import com.ferg.awfulapp.NavigationEvent
import com.ferg.awfulapp.R
import com.ferg.awfulapp.popupmenu.PunishmentContextMenu.PunishmentMenuAction.*
import com.ferg.awfulapp.thread.AwfulURL
import com.ferg.awfulapp.users.User
import timber.log.Timber

/**
 * Created by baka kaba on 17/08/2018.
 *
 * Context menu for entries in the Leper's Colony / rap sheets.
 */

class PunishmentContextMenu : BasePopupMenu<PunishmentContextMenu.PunishmentMenuAction>() {

    private lateinit var punishedUser: User
    private lateinit var admin: User
    private var badPostUrl: String? = null
    private var isRapSheet: Boolean = false

    companion object {
        private const val ARG_USERNAME = "punished username"
        private const val ARG_USER_ID = "punished user ID"
        private const val ARG_ADMIN_NAME = "admin username"
        private const val ARG_ADMIN_ID = "admin ID"
        private const val ARG_BAD_POST_URL = "bad post URL"
        private const val ARG_IS_RAP_SHEET = "is rap sheet"

        /**
         * Initialise and create a menu fragment to show.
         *
         * The [punishedUser] and [admin] Users represent the person being banned etc, and the
         * admin who approved the punishment. [badPostUrl] is the link to the post they got in
         * trouble for, or null if it's missing (e.g. a deleted post).
         * [isRapSheet] should be set to true if the menu is being displayed on the
         * user's own rap sheet page, if set to false then an option will be added to navigate there.
         */
        fun newInstance(punishedUser: User, admin: User, badPostUrl: String?, isRapSheet: Boolean): PunishmentContextMenu {
            return PunishmentContextMenu().also { fragment ->
                Bundle().apply {
                    putString(ARG_USERNAME, punishedUser.username)
                    putInt(ARG_USER_ID, punishedUser.id)
                    putString(ARG_ADMIN_NAME, admin.username)
                    putInt(ARG_ADMIN_ID, admin.id)
                    putString(ARG_BAD_POST_URL, badPostUrl)
                    putBoolean(ARG_IS_RAP_SHEET, isRapSheet)
                }.run(fragment::setArguments)
            }
        }
    }

    override fun init(args: Bundle) = with(args) {
        punishedUser = User(id = getInt(ARG_USER_ID), username = getString(ARG_USERNAME))
        admin = User(id = getInt(ARG_ADMIN_ID), username = getString(ARG_ADMIN_NAME))
        badPostUrl = getString(ARG_BAD_POST_URL)
        isRapSheet = getBoolean(ARG_IS_RAP_SHEET)
    }

    override fun generateMenuItems() =
            mutableListOf<PunishmentMenuAction>()
                    .apply { badPostUrl?.let { add(GO_TO_BAD_POST) } }
                    .apply { if (!isRapSheet) add(USER_RAP_SHEET) }
//                    .apply { add(MORE_BY_ADMIN) }

    override fun getMenuLabel(action: PunishmentMenuAction) =
            String.format(action.menuLabel, punishedUser.username)

    // TODO: this doesn't really NEED a title, maybe make it optional (with the title area removed)?
    // this would probably be better with a disabled menu entry (a new MISSING_POST Action or something)
    // but I ain't rewriting the whole context menu system to make that happen right now
    override fun getTitle() = badPostUrl?.let { "Select an action" } ?: "(post is unavailable)"

    override fun onActionClicked(action: PunishmentMenuAction) {
        fun tryNavigate(e: NavigationEvent) {
            (activity as AwfulActivity?)?.navigate(e)
        }
        when (action) {
            GO_TO_BAD_POST ->
                badPostUrl.run(AwfulURL::parse).run(NavigationEvent::Url).run(::tryNavigate)
            USER_RAP_SHEET ->
                NavigationEvent.LepersColony(punishedUser.id).run(::tryNavigate)
            MORE_BY_ADMIN ->
                Timber.w("Admin filtering not implemented")
        }
    }


    enum class PunishmentMenuAction(
            @DrawableRes private val iconResId: Int,
            private val menuText: String
    ) : AwfulAction {

        GO_TO_BAD_POST(R.drawable.ic_insert_comment_dark_24dp, "View this bad post"),
        USER_RAP_SHEET(R.drawable.ic_gavel_dark_24dp, "Rap sheet for %s"),
        MORE_BY_ADMIN(R.drawable.ic_error_dark, "More approved by this admin");

        // TODO: if these menus are all reworked in Kotlin, make these overridable vals instead of functions so we don't have to add getters like this
        override fun getIconId() = iconResId
        override fun getMenuLabel() = menuText
    }

}