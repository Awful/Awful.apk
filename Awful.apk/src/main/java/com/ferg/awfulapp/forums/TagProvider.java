package com.ferg.awfulapp.forums;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.ferg.awfulapp.network.NetworkUtils;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by baka kaba on 14/05/2016.
 * <p/>
 * Provides forum tag icons.
 */
@SuppressWarnings("SpellCheckingInspection")
public class TagProvider {

    /**
     * Set a SquareForumTag's appearance for a given forum.
     * <p/>
     * This will apply the forum's associated colours and text overlay, if it has them.
     *
     * @param target The SquareForumTag to remake
     * @param forum  The Forum whose details will be applied to the tag
     */
    static void setSquareForumTag(@NonNull final SquareForumTag target, @NonNull final Forum forum) {
        target.setTagText(getForumAbbreviation(forum));
        if (StringUtils.isEmpty(forum.getTagUrl())) {
            return;
        }
        NetworkUtils.getImageLoader().get(forum.getTagUrl(), new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                Bitmap threadTag = response.getBitmap();
                if (threadTag != null) {
                    // get square and background colors
                    target.setColorFilter(threadTag.getPixel(4, 10), PorterDuff.Mode.MULTIPLY);
                    target.setBackgroundColor(threadTag.getPixel(33, 13));
                } else {
                    target.setColorFilter(null);
                    target.setBackgroundColor(Color.TRANSPARENT);
                }
            }


            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
    }


    /**
     * Sets a NetworkImageView to the image specified by a forum's tagUrl.
     * <p/>
     * This is the old look.
     *
     * @param target The ImageView to set
     * @param forum  The forum whose tag will be used
     */
    @SuppressWarnings("unused")
    static void setWebsiteForumTag(@NonNull NetworkImageView target, @NonNull Forum forum) {
        if (StringUtils.isEmpty(forum.getTagUrl())) {
            return;
        }
        target.setImageUrl(forum.getTagUrl(), NetworkUtils.getImageLoader());
    }


    private static final SparseArray<String> forumAbbreviations = new SparseArray<>();

    static {
        forumAbbreviations.append(1, "GBS");
        forumAbbreviations.append(26, "FYAD");
        forumAbbreviations.append(268, "BYOB");
        forumAbbreviations.append(272, "RSF");

        forumAbbreviations.append(44, "GAMES");
        forumAbbreviations.append(46, "D&D");
        forumAbbreviations.append(167, "PYF");
        forumAbbreviations.append(158, "A/T");
        forumAbbreviations.append(22, "SH/SC");
        forumAbbreviations.append(192, "IYG");
        forumAbbreviations.append(122, "SAS");
        forumAbbreviations.append(179, "YLLS");
        forumAbbreviations.append(161, "GWS");
        forumAbbreviations.append(91, "AI");
        forumAbbreviations.append(124, "PI");
        forumAbbreviations.append(132, "TFR");
        forumAbbreviations.append(90, "TCC");
        forumAbbreviations.append(218, "GIP");

        forumAbbreviations.append(31, "CC");
        forumAbbreviations.append(151, "CD");
        forumAbbreviations.append(182, "TBB");
        forumAbbreviations.append(150, "NMD");
        forumAbbreviations.append(130, "TVIV");
        forumAbbreviations.append(144, "BSS");
        forumAbbreviations.append(27, "ADTRW");
        forumAbbreviations.append(215, "PHIZ");
        forumAbbreviations.append(255, "RGD");

        forumAbbreviations.append(61, "SAMART");
        forumAbbreviations.append(43, "GM");
        forumAbbreviations.append(241, "LAN");
        forumAbbreviations.append(188, "QCS");

        forumAbbreviations.append(21, "55555");
        forumAbbreviations.append(25, "11111");
    }

    /**
     * Get the abbreviation for a forum, as overlaid on its tag on the website.
     *
     * @param forum The forum to look up
     * @return its tag text, or an empty string
     */
    @NonNull
    private static String getForumAbbreviation(@NonNull Forum forum) {
        return forumAbbreviations.get(forum.id, "");
    }

}
