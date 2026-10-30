package com.ferg.awfulapp.widget

import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.VolleyError
import com.ferg.awfulapp.AwfulSheetActivity
import com.ferg.awfulapp.R
import com.ferg.awfulapp.constants.Constants.POST_ICON_REQUEST_TYPES
import com.ferg.awfulapp.forums.Forum
import com.ferg.awfulapp.forums.ForumRepository.Companion.getInstance
import com.ferg.awfulapp.forums.ForumStructure
import com.ferg.awfulapp.network.NetworkUtils.queueRequest
import com.ferg.awfulapp.task.AwfulRequest.AwfulResultCallback
import com.ferg.awfulapp.task.PostIconRequest
import com.ferg.awfulapp.thread.AwfulPostIcon


/**
 * Created by baka kaba on 19/11/2016.
 * 
 * 
 * A component that allows the user to select a thread/PM icon.
 * 
 * 
 * You need to call [.useForumIcons] or [.usePrivateMessageIcons] to set the
 * source of the icons the user can pick from, and then the icon view can be clicked to display
 * the icon sheet. Calling [.getIcon] will get the currently selected icon, which defaults
 * to a blank 'no icon' version.
 * 
 * @see AwfulPostIcon.BLANK_ICON
 */
class ThreadIconPicker : Fragment() {
    lateinit var selectedIconView: ImageView

    /**
     * Get the currently selected icon.
     */
    var icon: AwfulPostIcon = AwfulPostIcon.BLANK_ICON
        private set
    private var currentForumId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.icon_picker, container, true)
        selectedIconView = view.findViewById(R.id.selected_icon)
        selectedIconView.setOnClickListener { showPicker() }
        selectedIconView.setOnLongClickListener {
            secretForumCycler()
            true
        }
        return view
    }


    /**
     * Set the picker to display the icons for a specific forum.
     * 
     * 
     * This clears any selected icon.
     * If you need to display the icons for private messages, call [.usePrivateMessageIcons]
     * instead of passing an ID here.
     * 
     * @param forumId the forum's ID on the site
     */
    fun useForumIcons(forumId: Int) {
        currentForumId = forumId
        useIcon(AwfulPostIcon.BLANK_ICON)
        // check if we already loaded these
        if (iconsCache.get(forumId) != null) {
            Log.d(TAG, "useForumTags: Already cached for forum id: " + forumId)
            return
        }
        // we need to fetch them icons
        loadTags(
            if (forumId == PM_FORUM_ID) POST_ICON_REQUEST_TYPES.PM else POST_ICON_REQUEST_TYPES.FORUM_POST,
            forumId
        )
    }


    /**
     * Set the picker to display the icons for private messages.
     * 
     * 
     * This clears any selected icon.
     */
    fun usePrivateMessageIcons() {
        useForumIcons(PM_FORUM_ID)
    }


    /**
     * Show the bottom sheet containing the current icon set.
     * 
     * 
     * If an icon source hasn't been selected through [.useForumIcons] or
     * [.usePrivateMessageIcons], this will do nothing.
     */
    fun showPicker() {
        val forumId = currentForumId ?: run {
            Log.w(TAG,"The user tried to select an icon before a source forum was set!\nYou should prevent this or initialise with one")
            return
        }
        val icons: MutableList<AwfulPostIcon> = iconsCache.get(forumId) ?: return Toast.makeText(context, "Icons not loaded", Toast.LENGTH_SHORT).show()
        showBottomSheet(icons)
    }


    /**
     * Display the bottom sheet populated with a list of icons the user can select from.
     */
    private fun showBottomSheet(icons: MutableList<AwfulPostIcon>) {
        val bottomSheetActivity = activity as? AwfulSheetActivity
        bottomSheetActivity?.let {
            it.showBottomSheet(
                menu = generatePostIconMenu(icons),
                onItemClick = { item -> useIcon(icons[item.itemId])
                }
            )
        }
    }


    /**
     * Set the currently selected icon.
     */
    private fun useIcon(icon: AwfulPostIcon) {
        this.icon = icon
        if (icon.drawable != null) {
            selectedIconView.setImageDrawable(icon.drawable)
        } else {
            selectedIconView.setImageResource(icon.drawableId)
        }
    }

    fun useIcon(iconId: String, iconUrl: String) {
        val newIcon = AwfulPostIcon(iconId, iconUrl, requireContext())
        useIcon(newIcon)
    }


    private fun loadTags(iconType: POST_ICON_REQUEST_TYPES, forumId: Int) {
        // TODO: 19/11/2016 handle network requests
        queueRequest(
            PostIconRequest(requireActivity(), iconType, forumId)
                .build(null, object : AwfulResultCallback<ArrayList<AwfulPostIcon>> {
                    override fun success(result: ArrayList<AwfulPostIcon>) {
                        // add a blank 'no icon' icon too
                        if (result.isNotEmpty()) {
                            result.add(0, AwfulPostIcon.BLANK_ICON)
                        }
                        // update the cache with these new icons
                        when (iconType) {
                            POST_ICON_REQUEST_TYPES.PM -> {
                                iconsCache.put(PM_FORUM_ID, result)
                            }
                            POST_ICON_REQUEST_TYPES.FORUM_POST -> {
                                iconsCache.put(forumId, result)
                            }
                        }
                    }

                    override fun failure(error: VolleyError?) {
                        Toast.makeText(
                            activity,
                            "Failed to load icons\nForum ID $forumId",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        )
    }


    /**
     * Create a menu from a list of icons, with their IDs and Orders set to their index positions.
     */
    private fun generatePostIconMenu(postIcons: MutableList<AwfulPostIcon>): Menu {
        val menu: Menu = PopupMenu(context, view).menu
        // add each icon, setting its ID to its index in the list

        for (i in postIcons.indices) {
            val icon = postIcons[i]
            val item = menu.add(Menu.NONE, i, i, "")
                .setTitle("")
            if (icon.drawable != null) {
                item.icon = icon.drawable
            } else {
                item.setIcon(icon.drawableId)
            }
        }
        return menu
    }

    /**//////////////////////////////////////////////////////////////////////// */ // Secret test stuff
    /**//////////////////////////////////////////////////////////////////////// */
    var allTheForums: MutableIterator<Forum>? = null

    fun secretForumCycler(): Boolean {
        if (allTheForums == null || !allTheForums!!.hasNext()) {
            val repo = getInstance(context)
            allTheForums = repo.allForums.asList.formatAs(ForumStructure.ListFormat.FLAT)
                .includeSections(false).build().iterator()
        }
        if (allTheForums!!.hasNext()) {
            val forum = allTheForums!!.next()
            useForumIcons(forum.id)
            Toast.makeText(context, "Icons from\n" + forum.title, Toast.LENGTH_SHORT).show()
        }
        return true
    }

    companion object {
        private val TAG: String = ThreadIconPicker::class.java.simpleName

        /**
         * fake forum ID so we can mix in the PM icons with the other forum icons
         */
        private const val PM_FORUM_ID = -324546

        private val iconsCache = SparseArray<MutableList<AwfulPostIcon>?>()
    }
}
