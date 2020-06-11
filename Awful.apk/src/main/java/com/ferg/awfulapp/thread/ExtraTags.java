package com.ferg.awfulapp.thread;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import android.util.Log;

import com.ferg.awfulapp.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Created by baka kaba on 26/05/2015.</p>
 *
 * <p>Utility class to parse rating URLs and return a reference to a rating type.
 * You can then use this reference to get drawables, check the rating category
 * (normal, FilmDump reviews) and so on.</p>
 */
public class ExtraTags {

    private static final String TAG = Class.class.getSimpleName();

    public static final int TYPE_NO_TAG     = 0;
    public static final int TYPE_ASK_TELL   = 1;
    public static final int TYPE_SA_MART    = 2;


    /** default value for missing ratings */
    private static final ElementCollection extraTags = new ElementCollection("ExtraTags");
    static {
        extraTags.add(TYPE_ASK_TELL, "tma", R.drawable.tma);
        extraTags.add(TYPE_ASK_TELL, "ama", R.drawable.ama);

        extraTags.add(TYPE_SA_MART, "icon-37-selling", R.drawable.icon_37_selling);
        extraTags.add(TYPE_SA_MART, "icon-38-buying", R.drawable.icon_38_buying);
        extraTags.add(TYPE_SA_MART, "icon-46-trading", R.drawable.icon_46_trading);
    }
    // convenient public constant for external checks
    public static final int NO_TAG = extraTags.NULL_ELEMENT_ID;

    private static final Pattern tagUrlPattern = Pattern.compile("somethingawful\\.com/?(.*)/(.+)\\.gif");


    /**
     * <p>Parse a tag icon URL and get an associated tag ID.</p>
     * Pass in the URL of the secondary tag icon for a thread, and this will try
     * to identify it. The resulting ID can be passed to the other methods in this class,
     * for specific information on the tag it represents.
     *
     * @param tagImageUrl    The full URL to a secondary SA tag icon
     * @return               a tag ID, {@link #NO_TAG} by default
     */
    public static int getId(String tagImageUrl) {
        if (tagImageUrl == null) {
            return extraTags.NULL_ELEMENT_ID;
        }
        Matcher matcher = tagUrlPattern.matcher(tagImageUrl);
        if (!matcher.find()) {
            Log.w(TAG, "Regex pattern doesn't match!");
            return extraTags.NULL_ELEMENT_ID;
        }
        String type     = matcher.group(1);
        String token    = matcher.group(2);

        // The URL format is a little awkward and inconsistent, this might break later
        int category = TYPE_NO_TAG;
        if ("forums/posticons".equals(type)) {
            category = TYPE_SA_MART;
        } else if ("".equals(type)) {
            category = TYPE_ASK_TELL;
        }
        return extraTags.findElement(category, token);
    }


    /**
     * Get a tag ID's type, i.e. the tag category it belongs to.
     * @param tagId   The ID to categorise
     * @return        A type constant
     */
    public static int getType(int tagId) {
        return extraTags.getType(tagId, TYPE_NO_TAG);
    }


    /**
     * Get the drawable associated with a given rating ID.
     * @param tagId         The tag ID to look up
     * @param resources
     * @return              Any associated drawable, otherwise null
     */
    @Nullable
    public static Drawable getDrawable(int tagId, Resources resources) {
        return extraTags.getDrawable(tagId, resources);
    }

}
