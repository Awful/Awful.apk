package com.ferg.awfulapp.provider

import android.util.TypedValue
import androidx.annotation.ArrayRes
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.ferg.awfulapp.R
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.AwfulPreferences.Companion.getInstance
import com.ferg.awfulapp.provider.AwfulTheme.Companion.forForum
import com.ferg.awfulapp.provider.ColorProvider.Companion.getThemeColorResId

/**
 * Created by baka kaba on 02/01/2017.
 * 
 * 
 * Access to themed color attributes used by the app.
 * 
 * 
 * This class handles resolving attributes to colors specified in a particular theme,
 * according to the current app theme, any special themes for a current forum, and
 * whether the user has chosen to force those forum themes. It also provides a few
 * helper functions, including the available bookmark group colors.
 */
enum class ColorProvider(@param:AttrRes private val colorAttr: Int) {
    PRIMARY_TEXT(R.attr.primaryPostFontColor),
    ALT_TEXT(R.attr.secondaryPostFontColor),
    BACKGROUND(androidx.appcompat.R.attr.background),
    UNREAD_BACKGROUND(R.attr.unreadColor),
    UNREAD_BACKGROUND_DIM(R.attr.unreadColorDim),
    UNREAD_TEXT(R.attr.unreadFontColor),
    ACTION_BAR(R.attr.actionBarColor),
    ACTION_BAR_TEXT(R.attr.actionBarFontColor),
    PROGRESS_BAR(R.attr.progressBarColor),
    SELF_THREAD_BACKGROUND(R.attr.selfThreadBackground);


    @get:ColorInt
    val color: Int
        /**
         * Get the value for this color from the current app theme.
         */
        get() = getColor(null)

    /**
     * Get the value for this color, resolving for a specific forum.
     * 
     * 
     * This will check for a forum-specific theme and the user's per-forum preferences,
     * and return the appropriate color.
     */
    @ColorInt
    fun getColor(forumId: Int?): Int {
        return getThemeColor(colorAttr, forumId, getInstance())
    }

    companion object {
        private val BOOKMARK_COLORS: IntArray = getColorResIds(R.array.bookmarkColors)
        private val BOOKMARK_COLORS_DIM: IntArray = getColorResIds(R.array.bookmarkDimColors)

        /**
         * Convert an RGB packed int to its hex representation.
         * 
         * 
         * Does not pad with leading zeroes.
         */
        fun convertToRGB(@ColorInt color: Int): String {
            return "#" + Integer.toHexString(color and 0x00FFFFFF)
        }

        @Composable
        @JvmStatic
        fun getResourceColor(resourceId: Int): Color {
            val context = LocalContext.current
            val resources = LocalResources.current
            val theme = context.theme

            val typedValue = TypedValue()
            theme.resolveAttribute(resourceId, typedValue, true)

            return when {
                typedValue.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT -> {
                    Color(typedValue.data)
                }

                typedValue.resourceId != 0 -> {
                    val colorStateList = resources.getColorStateList(
                        typedValue.resourceId,
                        theme
                    )

                    Color(colorStateList.defaultColor)
                }

                else -> Color.Unspecified
            }
        }


        /**
         * Get one of the standard bookmark colors.
         * 
         * @param bookmarkGroup passed by the forum, used to color the bookmarks
         * @param dimmed        if true the dimmed version will be returned
         * @return the bookmark group's color, or the default for invalid group IDs
         */
        @JvmStatic
        @ColorInt
        fun getBookmarkColor(bookmarkGroup: Int, dimmed: Boolean): Int {
            var bookmarkGroup = bookmarkGroup
            if (bookmarkGroup < 0 || bookmarkGroup >= BOOKMARK_COLORS.size) {
                bookmarkGroup = 0
            }
            val colorId: Int =
                if (dimmed) BOOKMARK_COLORS_DIM[bookmarkGroup] else BOOKMARK_COLORS[bookmarkGroup]
            return getInstance().resources.getColor(colorId)
        }


        /**
         * Get the SRL background color resource for a given forum.
         * 
         * 
         * This method returns a **resource ID**, not a resolved color
         * 
         * @return the ID for the appropriate color resource
         */
        @ColorRes
        fun getARLInertColor(forumId: Int? = null): Int {
            return getThemeColorResId(R.attr.arlInertColor, forumId, getInstance())
        }


        /**
         * Get the SRL progress color resources according to the current theme, forum and user settings.
         * 
         * 
         * This method returns a set of **resource IDs**, not resolved color ints
         * 
         * @return the forum's themed color resources (if any), otherwise the default set
         */
        fun getARLActiveColor(forumId: Int? = null): Int {
            return getThemeColorResId(R.attr.arlActiveColor, forumId, getInstance())
        }


        /**
         * Helper function to get an int array from Resources
         */
        private fun getColorResIds(@ArrayRes colorArrayResId: Int): IntArray {
            val resources = getInstance().resources
            val ta = resources.obtainTypedArray(colorArrayResId)
            val resIds = IntArray(ta.length())
            for (i in 0..<ta.length()) {
                resIds[i] = ta.getResourceId(i, -1)
            }
            ta.recycle()
            return resIds
        }


        /**
         * Resolves a color attr to a color according to the current theme, forum and user settings.
         * 
         * @param colorAttr One of the app's color attrs
         * @param forumId    An optional forum to check for its theme
         * @param prefs      User preferences
         * @return The resolved color
         * @see getThemeColorResId
         */
        @ColorInt
        private fun getThemeColor(
            @AttrRes colorAttr: Int,
            forumId: Int? = null,
            prefs: AwfulPreferences
        ): Int {
            val resId: Int = getThemeColorResId(colorAttr, forumId, prefs)
            return prefs.resources.getColor(resId)
        }


        /**
         * Resolves a color attr to a resource ID according to the current theme.
         * 
         * 
         * If a forum has its own theme, and the user has per-forum themes selected, this will retrieve
         * the color from that forum's theme, otherwise the current app theme will be used.
         * 
         * @param colorAttr One of the app's color attrs
         * @param forumId    An optional forum to check for its theme
         * @param prefs      User preferences
         * @return The resolved resource ID
         */
        @ColorRes
        private fun getThemeColorResId(
            @AttrRes colorAttr: Int,
            forumId: Int? = null,
            prefs: AwfulPreferences
        ): Int {
            val colorValue = TypedValue()
            forForum(forumId)
                .getTheme(prefs)
                .resolveAttribute(colorAttr, colorValue, true)

            return colorValue.resourceId
        }
    }
}
