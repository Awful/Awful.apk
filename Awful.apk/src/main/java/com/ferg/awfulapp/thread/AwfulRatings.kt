package com.ferg.awfulapp.thread

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import com.ferg.awfulapp.R
import java.util.regex.Pattern

/**
 * 
 * Created by baka kaba on 24/05/2015.
 * 
 * 
 * Utility class to parse rating URLs and return a reference to a rating type.
 * You can then use this reference to get drawables, check the rating category
 * (normal, FilmDump reviews) and so on.
 */
object AwfulRatings {
    private val TAG: String = Class::class.java.simpleName

    /* types for identifying the rating's... type */
    const val TYPE_NO_RATING: Int = 0
    const val TYPE_NORMAL: Int = 1
    const val TYPE_FILM_DUMP: Int = 2

    private val ratings = ElementCollection("Ratings").apply {

        add(TYPE_NORMAL, "5", R.drawable.rating_5stars)
        add(TYPE_NORMAL, "4", R.drawable.rating_4stars)
        add(TYPE_NORMAL, "3", R.drawable.rating_3stars)
        add(TYPE_NORMAL, "2", R.drawable.rating_2stars)
        add(TYPE_NORMAL, "1", R.drawable.rating_1stars)

        // film dump ratings have a 3-char token in the format #.#
        add(TYPE_FILM_DUMP, "5.0", R.drawable.rating_5_0stars)
        add(TYPE_FILM_DUMP, "4.5", R.drawable.rating_4_5stars)
        add(TYPE_FILM_DUMP, "4.0", R.drawable.rating_4_0stars)
        add(TYPE_FILM_DUMP, "3.5", R.drawable.rating_3_5stars)
        add(TYPE_FILM_DUMP, "3.0", R.drawable.rating_3_0stars)
        add(TYPE_FILM_DUMP, "2.5", R.drawable.rating_2_5stars)
        add(TYPE_FILM_DUMP, "2.0", R.drawable.rating_2_0stars)
        add(TYPE_FILM_DUMP, "1.5", R.drawable.rating_1_5stars)
        add(TYPE_FILM_DUMP, "1.0", R.drawable.rating_1_0stars)
        add(TYPE_FILM_DUMP, "0.5", R.drawable.rating_0_5stars)
        add(TYPE_FILM_DUMP, "0.0", R.drawable.rating_0_0stars)
    }

    /** default value for missing ratings  */
    val NO_RATING: Int = ratings.NULL_ELEMENT_ID

    private val ratingUrlPattern: Pattern = Pattern.compile("/rate/(\\w+)/(.+)stars")


    /**
     * 
     * Parse a rating icon URL and get an associated rating ID.
     * Pass in the URL of the rating image for a thread, and this will try
     * to identify it. The resulting ID can be passed to the other methods in this class,
     * for specific information on the rating it represents.
     * 
     * @param ratingImageUrl    The full URL to an SA rating icon
     * @return                  a rating ID, [.NO_RATING] by default
     */
    fun getId(ratingImageUrl: String?): Int {
        if (ratingImageUrl == null) {
            return ratings.NULL_ELEMENT_ID
        }
        val matcher = ratingUrlPattern.matcher(ratingImageUrl)
        if (!matcher.find()) {
            Log.w(TAG, "Pattern doesn't match")
            return ratings.NULL_ELEMENT_ID
        }
        val type = matcher.group(1)
        val ratingToken = matcher.group(2)

        // work out what kind of rating the URL is even talking about
        // there's only two right now, but there could be more later...
        var category = TYPE_NO_RATING
        if ("default" == type) {
            category = TYPE_NORMAL
        } else if ("reviews" == type) {
            category = TYPE_FILM_DUMP
        }
        return ratings.findElement(category, ratingToken)
    }


    /**
     * Get a rating ID's type, i.e. the rating category it belongs to.
     * Returns [.TYPE_NORMAL] for standard 1-5 ratings, [.TYPE_FILM_DUMP]
     * for Film Barn star ratings, or [.TYPE_NO_RATING] by default
     * @param ratingId   The ID to categorize
     * @return           A type constant
     */
    @JvmStatic
    fun getType(ratingId: Int): Int {
        return ratings.getType(ratingId, TYPE_NO_RATING)
    }


    /**
     * Get the drawable associated with a given rating ID.
     * @param ratingId      The rating ID to look up
     * @param resources
     * @return              Any associated drawable, otherwise null
     */
    @JvmStatic
    fun getDrawable(ratingId: Int, resources: Resources?): Drawable? {
        return ratings.getDrawable(ratingId, resources)
    }
}
