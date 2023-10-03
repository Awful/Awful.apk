package com.ferg.awfulapp.network;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.ferg.awfulapp.AwfulApplication;
import com.ferg.awfulapp.CaptchaActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Image loader that can set Cloudflare captcha related headers on requests, which are also required
 * on `fi.somethingawful.com`. See `CaptchaActivity` and related classes for more details on the
 * captcha situation.
 */
public class AwfulImageLoader extends ImageLoader {
    /**
     * Constructs a new ImageLoader.
     *
     * @param queue      The RequestQueue to use for making image requests.
     * @param imageCache The cache to use as an L1 cache.
     */
    public AwfulImageLoader(RequestQueue queue, ImageCache imageCache) {
        super(queue, imageCache);
    }

    @Override
    protected Request<Bitmap> makeImageRequest(String requestUrl, int maxWidth, int maxHeight,
                                               ImageView.ScaleType scaleType, final String cacheKey) {
        return new ImageRequest(requestUrl, response ->
                onGetImageSuccess(cacheKey, response), maxWidth, maxHeight, scaleType, Bitmap.Config.RGB_565, error ->
                onGetImageError(cacheKey, error)) {
            @Override
            public Map<String, String> getHeaders() {
                final Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", AwfulApplication.getAwfulUserAgent());

                final Optional<String> captchaCookie = CookieController.getCaptchaCookie();
                if (captchaCookie.isPresent()) {
                    headers.put("Cookie", captchaCookie.get());
                }

                return headers;
            }
        };
    }
}
