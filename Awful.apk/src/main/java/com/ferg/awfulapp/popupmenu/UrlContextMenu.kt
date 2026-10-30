package com.ferg.awfulapp.popupmenu

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import com.ferg.awfulapp.R
import com.ferg.awfulapp.ThreadDisplayFragment
import com.ferg.awfulapp.popupmenu.UrlContextMenu.UrlMenuAction
import com.ferg.awfulapp.provider.ColorProvider

/**
 * Created by baka kaba on 22/05/2017.
 * 
 * 
 * A popup context menu for when a URL is clicked.
 */
class UrlContextMenu : BasePopupMenu<UrlMenuAction>() {
    init {
        layoutResId = R.layout.select_url_action_dialog
    }

    private lateinit var url: String
    private var isImage = false
    private var isGif = false

    var titleText: TextView? = null

    var subheading: TextView? = null
    private var subheadingText: String? = null

    override fun init(args: Bundle) {
        url = args.getString(ARG_URL) ?: ""
        isImage = args.getBoolean(ARG_IS_IMAGE)
        isGif = args.getBoolean(ARG_IS_GIF)
        subheadingText = args.getString(ARG_SUBHEADING_TEXT)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        titleText = view?.findViewById(R.id.actionTitle)
        subheading = view?.findViewById(R.id.title_subheading)
        // tiny title text for long URLs - need to reapply the colour set in the xml
        titleText?.setTextAppearance(
            context,
            androidx.appcompat.R.style.TextAppearance_AppCompat_Small
        )
        titleText?.setTextColor(ColorProvider.ACTION_BAR_TEXT.color)

        setSubheading(subheadingText)
        return view
    }


    override fun generateMenuItems(): MutableList<UrlMenuAction?> {
        val awfulActions: MutableList<UrlMenuAction?> = ArrayList<UrlMenuAction?>()

        if (isImage) {
            awfulActions.add(UrlMenuAction.DISPLAY_IMAGE)
            awfulActions.add(if (isGif) UrlMenuAction.PLAY_GIF else UrlMenuAction.SHOW_INLINE)
            awfulActions.add(UrlMenuAction.DOWNLOAD_IMAGE)
        }
        awfulActions.add(UrlMenuAction.OPEN_URL)
        awfulActions.add(UrlMenuAction.COPY_LINK_URL)
        awfulActions.add(UrlMenuAction.SHARE_URL)

        return awfulActions
    }


    override fun onActionClicked(action: UrlMenuAction) {
        val parent = targetFragment as ThreadDisplayFragment?
        if (parent == null) {
            Log.w(TAG, "onActionClicked: can't get target ThreadDisplayFragment")
            return
        }
        when (action) {
            UrlMenuAction.DOWNLOAD_IMAGE -> parent.enqueueDownload(url.toUri())
            UrlMenuAction.PLAY_GIF, UrlMenuAction.SHOW_INLINE -> parent.showImageInline(url)
            UrlMenuAction.COPY_LINK_URL -> parent.copyToClipboard(url)
            UrlMenuAction.OPEN_URL -> parent.startUrlIntent(url)
            UrlMenuAction.SHARE_URL -> startActivity(parent.createShareIntent(url))
            UrlMenuAction.DISPLAY_IMAGE -> parent.displayImage(url)
        }
    }


    override fun getTitle(): String {
        return url
    }


    /**
     * Set the contents of the subheading, showing or hiding the view as necessary.
     * 
     * 
     * A null value for **text** hides the view entirely, but an empty string will show it.
     */
    fun setSubheading(text: String?) {
        subheadingText = text
        val subhead = subheading ?: return // layout isn't inflated yet, just store the text for when it is

        // null text means we're hiding the subheading
        if (subheadingText == null) {
            subhead.text = ""
            subhead.visibility = View.GONE
            return
        }

        // we have some text (empty string counts) so show the view
        if (subhead.visibility != View.VISIBLE) {
            // basic set-and-show for hidden subheader and old Androids that can't animate properly
            subhead.text = subheadingText
            subhead.visibility = View.VISIBLE
        } else {
            subhead.animate()?.alpha(0f)?.withEndAction {
                subhead.text = subheadingText
                subhead.animate()?.alpha(1f)
            }
        }
    }


    enum class UrlMenuAction(@param:DrawableRes override val iconId: Int, override val menuLabel: String) :
        AwfulAction {
        DISPLAY_IMAGE(R.drawable.ic_photo_dark, "Display Image"),
        PLAY_GIF(R.drawable.ic_movie_dark, "Play .gif"),
        SHOW_INLINE(R.drawable.ic_area_close_dark, "Show Image inline"),
        DOWNLOAD_IMAGE(R.drawable.ic_file_download_dark, "Download Image"),
        OPEN_URL(R.drawable.ic_open_in_app_dark, "Open URL"),
        COPY_LINK_URL(R.drawable.ic_insert_link_dark, "Copy URL"),
        SHARE_URL(R.drawable.ic_share_dark, "Share URL")
    }

    companion object {
        private val TAG: String = UrlContextMenu::class.java.simpleName

        const val ARG_URL: String = "url"
        const val ARG_IS_IMAGE: String = "isImage"
        const val ARG_IS_GIF: String = "isGif"
        const val ARG_SUBHEADING_TEXT: String = "subheadingText"

        /**
         * Get a context menu for a link.
         * 
         * @param url        the link's URL
         * @param isImage    true if this link represents an image
         * @param isGif      true if this link represents a GIF
         * @param subheading an optional subheading - passing null hides the view, see [.setSubheading]
         * @return the configured menu, ready to show
         */
        fun newInstance(
            url: String,
            isImage: Boolean,
            isGif: Boolean,
            subheading: String?
        ): UrlContextMenu {
            val args = Bundle()
            val fragment = UrlContextMenu()

            args.putString(ARG_URL, url)
            args.putBoolean(ARG_IS_IMAGE, isImage)
            args.putBoolean(ARG_IS_GIF, isGif)
            args.putString(ARG_SUBHEADING_TEXT, subheading)
            fragment.setArguments(args)
            return fragment
        }
    }
}
