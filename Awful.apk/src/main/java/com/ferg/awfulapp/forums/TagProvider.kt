package com.ferg.awfulapp.forums

import androidx.core.graphics.get
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageLoader.ImageContainer
import com.android.volley.toolbox.ImageLoader.ImageListener
import com.ferg.awfulapp.network.NetworkUtils
import org.apache.commons.lang3.StringUtils

/**
 * Created by baka kaba on 14/05/2016.
 * 
 * 
 * Provides forum tag icons.
 */
object TagProvider {
    /**
     * Set a SquareForumTag's appearance for a given forum.
     * 
     * 
     * This will apply the forum's associated colors and text overlay, if it has them.
     * 
     * @param target The SquareForumTag to remake
     * @param forum  The Forum whose details will be applied to the tag
     */
    fun setSquareForumTag(target: SquareForumTag, forum: Forum) {
        target.setTagText(forum.abbreviation)
        if (StringUtils.isEmpty(forum.tagUrl)) {
            return
        }
        NetworkUtils.imageLoader?.get(forum.tagUrl, object : ImageListener {
            override fun onResponse(response: ImageContainer, isImmediate: Boolean) {
                val threadTag = response.bitmap
                if (threadTag != null) {
                    // get square and background colors
                    target.setAccentColour(threadTag[4, 10])
                    target.setMainColour(threadTag[33, 13])
                } else {
                    target.setAccentColour(null)
                    target.setMainColour(null)
                }
            }


            override fun onErrorResponse(error: VolleyError?) {
            }
        })
    }
}
