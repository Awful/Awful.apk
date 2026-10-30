package com.ferg.awfulapp.network

import android.graphics.Bitmap
import android.widget.ImageView.ScaleType
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.ImageRequest
import com.ferg.awfulapp.AwfulApplication.Companion.getAwfulUserAgent

/**
 * Image loader that can set Cloudflare captcha related headers on requests, which are also required
 * on `fi.somethingawful.com`. See `CaptchaActivity` and related classes for more details on the
 * captcha situation.
 */
class AwfulImageLoader
/**
 * Constructs a new ImageLoader.
 * 
 * @param queue      The RequestQueue to use for making image requests.
 * @param imageCache The cache to use as an L1 cache.
 */
    (queue: RequestQueue?, imageCache: ImageCache?) : ImageLoader(queue, imageCache) {
    override fun makeImageRequest(
        requestUrl: String?, maxWidth: Int, maxHeight: Int,
        scaleType: ScaleType?, cacheKey: String?
    ): Request<Bitmap?> {
        return object : ImageRequest(
            requestUrl,
            Response.Listener { response: Bitmap? -> onGetImageSuccess(cacheKey, response) },
            maxWidth,
            maxHeight,
            scaleType,
            Bitmap.Config.RGB_565,
            Response.ErrorListener { error: VolleyError? -> onGetImageError(cacheKey, error) }) {
            override fun getHeaders(): MutableMap<String?, String?> {
                val headers: MutableMap<String?, String?> = HashMap()
                headers["User-Agent"] = getAwfulUserAgent()

                val captchaCookie = CookieController.captchaCookie
                if (captchaCookie.isPresent) {
                    headers["Cookie"] = captchaCookie.get()
                }

                return headers
            }
        }
    }
}
