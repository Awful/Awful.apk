package com.ferg.awfulapp.thread

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import com.ferg.awfulapp.R
import java.util.regex.Pattern

/**
 * 
 * Created by baka kaba on 26/05/2015.
 * 
 * 
 * Utility class to parse rating URLs and return a reference to a rating type.
 * You can then use this reference to get drawables, check the rating category
 * (normal, FilmDump reviews) and so on.
 */
object ExtraTags {
    private val TAG: String = Class::class.java.simpleName

    const val TYPE_NO_TAG: Int = 0
    const val TYPE_ASK_TELL: Int = 1
    const val TYPE_SA_MART: Int = 2


    /** default value for missing ratings  */
    private val extraTags = ElementCollection("ExtraTags")

    init {
        extraTags.add(TYPE_ASK_TELL, "tma", R.drawable.tma)
        extraTags.add(TYPE_ASK_TELL, "ama", R.drawable.ama)

        extraTags.add(TYPE_SA_MART, "icon-37-selling", R.drawable.icon_37_selling)
        extraTags.add(TYPE_SA_MART, "icon-38-buying", R.drawable.icon_38_buying)
        extraTags.add(TYPE_SA_MART, "icon-46-trading", R.drawable.icon_46_trading)
    }

    // convenient public constant for external checks
    val NO_TAG: Int = extraTags.NULL_ELEMENT_ID

    private val tagUrlPattern: Pattern = Pattern.compile("somethingawful\\.com/?(.*)/(.+)\\.gif")


    /**
     * 
     * Parse a tag icon URL and get an associated tag ID.
     * Pass in the URL of the secondary tag icon for a thread, and this will try
     * to identify it. The resulting ID can be passed to the other methods in this class,
     * for specific information on the tag it represents.
     * 
     * @param tagImageUrl    The full URL to a secondary SA tag icon
     * @return               a tag ID, [.NO_TAG] by default
     */
    fun getId(tagImageUrl: String?): Int {
        if (tagImageUrl == null) {
            return extraTags.NULL_ELEMENT_ID
        }
        val matcher = tagUrlPattern.matcher(tagImageUrl)
        if (!matcher.find()) {
            Log.w(TAG, "Regex pattern doesn't match!")
            return extraTags.NULL_ELEMENT_ID
        }
        val type = matcher.group(1)
        val token = matcher.group(2)

        // The URL format is a little awkward and inconsistent, this might break later
        var category = TYPE_NO_TAG
        if ("forums/posticons" == type) {
            category = TYPE_SA_MART
        } else if ("" == type) {
            category = TYPE_ASK_TELL
        }
        return extraTags.findElement(category, token!!)
    }


    /**
     * Get a tag ID's type, i.e. the tag category it belongs to.
     * @param tagId   The ID to categorize
     * @return        A type constant
     */
    fun getType(tagId: Int): Int {
        return extraTags.getType(tagId, TYPE_NO_TAG)
    }


    /**
     * Get the drawable associated with a given rating ID.
     * @param tagId         The tag ID to look up
     * @param resources
     * @return              Any associated drawable, otherwise null
     */
    fun getDrawable(tagId: Int, resources: Resources?): Drawable? {
        return extraTags.getDrawable(tagId, resources)
    }
}
