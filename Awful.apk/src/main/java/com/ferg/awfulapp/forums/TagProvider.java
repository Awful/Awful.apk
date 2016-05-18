package com.ferg.awfulapp.forums;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

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
        target.setTagText(forum.getAbbreviation());
        if (StringUtils.isEmpty(forum.getTagUrl())) {
            return;
        }
        NetworkUtils.getImageLoader().get(forum.getTagUrl(), new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                Bitmap threadTag = response.getBitmap();
                if (threadTag != null) {
                    // get square and background colors
                    target.setAccentColour(threadTag.getPixel(4, 10));
                    target.setMainColour(threadTag.getPixel(33, 13));
                } else {
                    target.setAccentColour(null);
                    target.setMainColour(null);
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


}
