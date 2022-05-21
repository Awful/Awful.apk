package com.ferg.awfulapp.thread;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import android.util.Log;

import com.ferg.awfulapp.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Created by baka kaba on 24/05/2015.</p>
 *
 * <p>Utility class to parse rating URLs and return a reference to a rating type.
 * You can then use this reference to get drawables, check the rating category
 * (normal, FilmDump reviews) and so on.</p>
 */
public abstract class AwfulRatings {

    private static final String TAG = Class.class.getSimpleName();

    /* types for identifying the rating's... type */
    public static final int TYPE_NO_RATING  = 0;
    public static final int TYPE_NORMAL     = 1;
    public static final int TYPE_FILM_DUMP  = 2;

    private static final ElementCollection ratings = new ElementCollection("Ratings");
    static {
        // normal ratings have a single number as a token, 1-5
        ratings.add(TYPE_NORMAL, "5", R.drawable.rating_5stars);
        ratings.add(TYPE_NORMAL, "4", R.drawable.rating_4stars);
        ratings.add(TYPE_NORMAL, "3", R.drawable.rating_3stars);
        ratings.add(TYPE_NORMAL, "2", R.drawable.rating_2stars);
        ratings.add(TYPE_NORMAL, "1", R.drawable.rating_1stars);

        // film dump ratings have a 3-char token in the format #.#
        ratings.add(TYPE_FILM_DUMP, "5.0", R.drawable.rating_5_0stars);
        ratings.add(TYPE_FILM_DUMP, "4.5", R.drawable.rating_4_5stars);
        ratings.add(TYPE_FILM_DUMP, "4.0", R.drawable.rating_4_0stars);
        ratings.add(TYPE_FILM_DUMP, "3.5", R.drawable.rating_3_5stars);
        ratings.add(TYPE_FILM_DUMP, "3.0", R.drawable.rating_3_0stars);
        ratings.add(TYPE_FILM_DUMP, "2.5", R.drawable.rating_2_5stars);
        ratings.add(TYPE_FILM_DUMP, "2.0", R.drawable.rating_2_0stars);
        ratings.add(TYPE_FILM_DUMP, "1.5", R.drawable.rating_1_5stars);
        ratings.add(TYPE_FILM_DUMP, "1.0", R.drawable.rating_1_0stars);
        ratings.add(TYPE_FILM_DUMP, "0.5", R.drawable.rating_0_5stars);
        ratings.add(TYPE_FILM_DUMP, "0.0", R.drawable.rating_0_0stars);
    }
    /** default value for missing ratings */
    public  static final int NO_RATING = ratings.NULL_ELEMENT_ID;

    private static final Pattern ratingUrlPattern = Pattern.compile("/rate/(\\w+)/(.+)stars");


    /**
     * <p>Parse a rating icon URL and get an associated rating ID.</p>
     * Pass in the URL of the rating image for a thread, and this will try
     * to identify it. The resulting ID can be passed to the other methods in this class,
     * for specific information on the rating it represents.
     *
     * @param ratingImageUrl    The full URL to an SA rating icon
     * @return                  a rating ID, {@link #NO_RATING} by default
     */
    public static int getId(String ratingImageUrl) {
        if (ratingImageUrl == null) {
            return ratings.NULL_ELEMENT_ID;
        }
        Matcher matcher = ratingUrlPattern.matcher(ratingImageUrl);
        if (!matcher.find()) {
            Log.w(TAG, "Pattern doesn't match");
            return ratings.NULL_ELEMENT_ID;
        }
        String type         = matcher.group(1);
        String ratingToken  = matcher.group(2);

        // work out what kind of rating the URL is even talking about
        // there's only two right now, but there could be more later...
        int category = TYPE_NO_RATING;
        if ("default".equals(type)) {
            category = TYPE_NORMAL;
        } else if ("reviews".equals(type)) {
            category = TYPE_FILM_DUMP;
        }
        return ratings.findElement(category, ratingToken);
    }


    /**
     * Get a rating ID's type, i.e. the rating category it belongs to.
     * Returns {@link #TYPE_NORMAL} for standard 1-5 ratings, {@link #TYPE_FILM_DUMP}
     * for Film Barn star ratings, or {@link #TYPE_NO_RATING} by default
     * @param ratingId   The ID to categorise
     * @return           A type constant
     */
    public static int getType(int ratingId) {
        return ratings.getType(ratingId, TYPE_NO_RATING);
    }


    /**
     * Get the drawable associated with a given rating ID.
     * @param ratingId      The rating ID to look up
     * @param resources
     * @return              Any associated drawable, otherwise null
     */
    @Nullable
    public static Drawable getDrawable(int ratingId, Resources resources) {
        return ratings.getDrawable(ratingId, resources);
    }
}
