package com.ferg.awfulapp.util;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.toolbox.ImageLoader;

public class LRUImageCache implements ImageLoader.ImageCache {
    private LruCache<String, Bitmap> bitmapCache;

    public LRUImageCache() {
        this.bitmapCache = new LruCache<String, Bitmap>(5242880){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    @Override
    public Bitmap getBitmap(String url) {
        return bitmapCache.get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap) {
        bitmapCache.put(url, bitmap);
    }

    public void clear() {
        bitmapCache.evictAll();
    }
}
