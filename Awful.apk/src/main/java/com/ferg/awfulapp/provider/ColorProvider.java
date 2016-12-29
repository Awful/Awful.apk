package com.ferg.awfulapp.provider;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.ArrayRes;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.TypedValue;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.preferences.AwfulPreferences;


/**
 * Created by baka kaba on 02/01/2017.
 * <p>
 * Access to themed colour attributes used by the app.
 * <p>
 * This class handles resolving attributes to colours specified in a particular theme,
 * according to the current app theme, any special themes for a current forum, and
 * whether the user has chosen to force those forum themes. It also provides a few
 * helper functions, including the available bookmark group colours.
 */

public enum ColorProvider {

    PRIMARY_TEXT(R.attr.primaryPostFontColor),
    ALT_TEXT(R.attr.secondaryPostFontColor),
    BACKGROUND(R.attr.background),
    UNREAD_BACKGROUND(R.attr.unreadColor),
    UNREAD_BACKGROUND_DIM(R.attr.unreadColorDim),
    UNREAD_TEXT(R.attr.unreadFontColor),
    ACTION_BAR(R.attr.actionBarColor),
    ACTION_BAR_TEXT(R.attr.actionBarFontColor),
    PROGRESS_BAR(R.attr.progressBarColor);

    private static final int[] BOOKMARK_COLORS = getColorResIds(R.array.bookmarkColors);
    private static final int[] BOOKMARK_COLORS_DIM = getColorResIds(R.array.bookmarkDimColors);

    private final int colorAttr;

    ColorProvider(@AttrRes int colorAttr) {
        this.colorAttr = colorAttr;
    }


    /**
     * Convert an RGB packed int to its hex representation.
     * <p>
     * Does not pad with leading zeroes.
     */
    @NonNull
    public static String convertToRGB(@ColorInt int color) {
        return "#" + Integer.toHexString(color & 0x00FFFFFF);
    }


    /**
     * Get one of the standard bookmark colours.
     *
     * @param bookmarkGroup passed by the forum, used to colour the bookmarks
     * @param dimmed        if true the dimmed version will be returned
     * @return the bookmark group's colour, or the default for invalid group IDs
     */
    @SuppressWarnings("deprecation")
    @ColorInt
    public static int getBookmarkColor(int bookmarkGroup, boolean dimmed) {
        if (bookmarkGroup < 0 || bookmarkGroup >= BOOKMARK_COLORS.length) {
            bookmarkGroup = 0;
        }
        int colorId = dimmed ? BOOKMARK_COLORS_DIM[bookmarkGroup] : BOOKMARK_COLORS[bookmarkGroup];
        return AwfulPreferences.getInstance().getResources().getColor(colorId);
    }


    /**
     * Get the SRL background colour resource for a given forum.
     * <p>
     * This method returns a <b>resource ID</b>, not a resolved colour
     *
     * @return the ID for the appropriate colour resource
     */
    @ColorRes
    public static int getSRLBackgroundColor(@Nullable Integer forumId) {
        return getThemeColorResId(R.attr.srlBackgroundColor, forumId, AwfulPreferences.getInstance());
    }


    /**
     * Get the SRL progress colour resources according to the current theme, forum and user settings.
     * <p>
     * This method returns a set of <b>resource IDs</b>, not resolved colour ints
     *
     * @return the forum's themed colour resources (if any), otherwise the default set
     */
    @NonNull
    public static int[] getSRLProgressColors(@Nullable Integer forumId) {
        AwfulPreferences prefs = AwfulPreferences.getInstance();
        TypedValue colorsRef = new TypedValue();
        boolean foundThemedColors = AwfulTheme
                .forForum(forumId)
                .getTheme(prefs)
                .resolveAttribute(R.attr.srlProgressColors, colorsRef, true);

        @ArrayRes
        int colorArrayResId = foundThemedColors ? colorsRef.data : R.array.defaultSrlProgressColors;
        return getColorResIds(colorArrayResId);
    }


    /**
     * Helper function to get an int array from Resources
     */
    private static int[] getColorResIds(@ArrayRes int colorArrayResId) {
        Resources resources = AwfulPreferences.getInstance().getResources();
        TypedArray ta = resources.obtainTypedArray(colorArrayResId);
        int[] resIds = new int[ta.length()];
        for (int i = 0; i < ta.length(); i++) {
            resIds[i] = ta.getResourceId(i, -1);
        }
        ta.recycle();
        return resIds;
    }


    /**
     * Resolves a colour attr to a colour according to the current theme, forum and user settings.
     *
     * @param colourAttr One of the app's colour attrs
     * @param forumId    An optional forum to check for its theme
     * @param prefs      User preferences
     * @return The resolved colour
     * @see ColorProvider#getThemeColorResId(int, Integer, AwfulPreferences)
     */
    @SuppressWarnings("deprecation")
    @ColorInt
    private static int getThemeColour(@AttrRes int colourAttr, @Nullable Integer forumId, @NonNull AwfulPreferences prefs) {
        int resId = getThemeColorResId(colourAttr, forumId, prefs);
        return prefs.getResources().getColor(resId);
    }


    /**
     * Resolves a colour attr to a resource ID according to the current theme.
     * <p>
     * If a forum has its own theme, and the user has per-forum themes selected, this will retrieve
     * the colour from that forum's theme, otherwise the current app theme will be used.
     *
     * @param colourAttr One of the app's colour attrs
     * @param forumId    An optional forum to check for its theme
     * @param prefs      User preferences
     * @return The resolved resource ID
     */
    @ColorRes
    private static int getThemeColorResId(@AttrRes int colourAttr, @Nullable Integer forumId, @NonNull AwfulPreferences prefs) {
        TypedValue colourValue = new TypedValue();
        AwfulTheme
                .forForum(forumId)
                .getTheme(prefs)
                .resolveAttribute(colourAttr, colourValue, true);

        return colourValue.resourceId;
    }


    /**
     * Get the value for this colour from the current app theme.
     */
    @ColorInt
    public int getColor() {
        return getColor(null);
    }

    /**
     * Get the value for this colour, resolving for a specific forum.
     * <p>
     * This will check for a forum-specific theme and the user's per-forum preferences,
     * and return the appropriate colour.
     */
    @ColorInt
    public int getColor(@Nullable Integer forumId) {
        return getThemeColour(colorAttr, forumId, AwfulPreferences.getInstance());
    }
}
