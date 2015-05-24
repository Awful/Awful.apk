package com.ferg.awfulapp;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Created by baka kaba on 24/05/2015.</p>
 *
 * <p>Utility class to parse rating URLs and return a reference to a rating type.
 * You can then use this reference to get drawables, check the rating category
 * (normal, FilmDump reviews) and so on.</p>
 *
 * <p>It's arranged in two parts - first there's a list containing all of the possible
 * unique ratings the site might throw at us, and their associated details.
 * Then there's the parser in {@link #getRating(String)} which handles examining URLs,
 * working out which type of rating it involves, and then pulling out a rating token
 * and looking for it in the list. The types are identified by constants, which allow
 * for the same tokens being reused in the SA URLs (it might happen), and also let
 * you check exactly what kind of rating this is, in case something needs to be handled
 * in the layouts.</p>
 *
 * <p>So if you need to extend this for new categories, handle whatever's going on with
 * the URL in the parser, and then add all the new ratings to the list (including the
 * tokens the parser pulls out). Don't forget to add a new type constant if necessary! </p>
 */
public abstract class AwfulRatings {

    private static final String TAG = Class.class.getSimpleName();

    /* types for identifying the rating's... type */
    public static final int TYPE_NO_RATING  = 0;
    public static final int TYPE_NORMAL     = 1;
    public static final int TYPE_FILM_DUMP  = 2;

    /** default value for missing ratings */
    public static final int NO_RATING;
    private static final List<Rating> ratings;
    static {
        List<Rating> tempRatings = new ArrayList<>();
        // make damn sure this constant is set right by doing it programmatically
        Rating noRating = new Rating(TYPE_NO_RATING, null, null);
        tempRatings.add(noRating);
        NO_RATING = tempRatings.indexOf(noRating);

        tempRatings.add(new Rating(TYPE_NORMAL, "5", R.drawable.rating_5stars));
        tempRatings.add(new Rating(TYPE_NORMAL, "4", R.drawable.rating_4stars));
        tempRatings.add(new Rating(TYPE_NORMAL, "3", R.drawable.rating_3stars));
        tempRatings.add(new Rating(TYPE_NORMAL, "2", R.drawable.rating_2stars));
        tempRatings.add(new Rating(TYPE_NORMAL, "1", R.drawable.rating_1stars));

        tempRatings.add(new Rating(TYPE_FILM_DUMP, "5.0", R.drawable.rating_review_5_0_stars));
        tempRatings.add(new Rating(TYPE_FILM_DUMP, "4.5", R.drawable.rating_review_4_5_stars));
        tempRatings.add(new Rating(TYPE_FILM_DUMP, "4.0", R.drawable.rating_review_4_0_stars));
        tempRatings.add(new Rating(TYPE_FILM_DUMP, "3.5", R.drawable.rating_review_3_5_stars));
        tempRatings.add(new Rating(TYPE_FILM_DUMP, "3.0", R.drawable.rating_review_3_0_stars));
        tempRatings.add(new Rating(TYPE_FILM_DUMP, "2.5", R.drawable.rating_review_2_5_stars));
        tempRatings.add(new Rating(TYPE_FILM_DUMP, "2.0", R.drawable.rating_review_2_0_stars));
        tempRatings.add(new Rating(TYPE_FILM_DUMP, "1.5", R.drawable.rating_review_1_5_stars));
        tempRatings.add(new Rating(TYPE_FILM_DUMP, "1.0", R.drawable.rating_review_1_0_stars));
        tempRatings.add(new Rating(TYPE_FILM_DUMP, "0.5", R.drawable.rating_review_0_5_stars));
        tempRatings.add(new Rating(TYPE_FILM_DUMP, "0.0", R.drawable.rating_review_0_0_stars));

        // no touching!
        ratings = Collections.unmodifiableList(tempRatings);
    }


    private static final Pattern ratingUrlPattern = Pattern.compile("/rate/(\\w+)/(.+)stars");


    /**
     * Parse a rating icon URL and get the associated rating reference.
     * This identifies the general rating type by the form of its URL,
     * and uses this to handle parsing the specific rating it contains.
     * @param ratingImageUrl    The full URL to an SA rating icon
     * @return                  a rating reference, {@link #NO_RATING} by default
     */
    public static int getRating(String ratingImageUrl) {
        Matcher matcher = ratingUrlPattern.matcher(ratingImageUrl);
        if (!matcher.find()) {
            Log.w(TAG, "Pattern doesn't match");
            return NO_RATING;
        }
        String type         = matcher.group(1);
        String ratingToken  = matcher.group(2);

        // work out what kind of rating the URL is even talking about
        // there's only two right now, but there could be more later...
        if ("default".equals(type)) {
            // normal ratings have a single number as a token, 1-5
            return parseRating(TYPE_NORMAL, ratingToken);
        } else if ("reviews".equals(type)) {
            // film dump ratings have a 3-char token in the format #.#
            return parseRating(TYPE_FILM_DUMP, ratingToken);
        }
        return NO_RATING;
    }


    /**
     * Parse normal rating values
     * @param ratingToken   The rating value encoded in the URL
     * @return              A rating reference, or {@link #NO_RATING} by default
     */
    private static int parseRating(int type, String ratingToken) {
        for (Rating rating : ratings) {
            if (rating.type == type && ratingToken.equals(rating.urlToken)) {
                return ratings.indexOf(rating);
            }
        }
        if (Constants.DEBUG) {
            Log.w(TAG, String.format("Unrecognised Rating value (%s) for type (%d) " + ratingToken, type));
        }
        return NO_RATING;
    }


    /**
     * Get a rating reference's type, i.e. the rating category it belongs to.
     * Returns {@link #TYPE_NORMAL} for standard 1-5 ratings, {@link #TYPE_FILM_DUMP}
     * for Film Barn star ratings, or {@link #TYPE_NO_RATING} by default
     * @param ratingReference   The reference to categorise
     * @return                  A type constant
     */
    public static int getType(int ratingReference) {
        try {
            Rating rating = ratings.get(ratingReference);
            return rating.type;
        } catch (IndexOutOfBoundsException e) {
            if (Constants.DEBUG) Log.w(TAG, "Can't get type of a non-existent constant: " + ratingReference);
            return TYPE_NO_RATING;
        }
    }


    /**
     * Get the drawable associated with a given rating reference.
     * @param ratingReference   A rating reference value
     * @param resources
     * @return                  Any associated drawable, otherwise null
     */
    @Nullable
    public static Drawable getDrawable(int ratingReference, Resources resources) {
        if (resources == null) {
            Log.w(TAG, "Null Resources object passed when getting drawable!");
            return null;
        }
        try {
            Rating rating = ratings.get(ratingReference);
            if (rating.drawableId != null) {
                return resources.getDrawable(rating.drawableId);
            }
        } catch (Resources.NotFoundException e) {
            if (Constants.DEBUG) Log.w(TAG, String.format("No drawable for rating constant: %d!", ratingReference));
        } catch (IndexOutOfBoundsException e) {
            if (Constants.DEBUG) Log.w(TAG, "Can't get drawable for a non-existent constant: " + ratingReference);
        }
        return null;
    }


    /**
     * Rating objects describing all the info you could ever need!
     * If you ever need to hold and handle any extra rating types, URL parameters etc,
     * add them in here in the rating description.
     */
    private static class Rating {
        /**
         * A type constant, used for grouping rating categories
         * */
        public final int type;
        /**
         * Holds the token in the URL which uniquely identifies this rating within its category.
         * For example, Film Dump ratings have a format of #.#, and each rating will have
         * a unique value.
         */
        public final String urlToken;
        /**
         * A resource ID for this rating's drawable. (May be null if there isn't one)
         */
        public final Integer drawableId;

        private Rating(int type, @Nullable String urlToken, @Nullable Integer drawableId) {
            this.type = type;
            this.drawableId = drawableId;
            this.urlToken = urlToken;
        }
    }
}
