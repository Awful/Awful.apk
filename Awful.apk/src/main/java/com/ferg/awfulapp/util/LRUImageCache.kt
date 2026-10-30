package com.ferg.awfulapp.util

import android.graphics.Bitmap
import androidx.collection.LruCache
import com.android.volley.toolbox.ImageLoader

class LRUImageCache : ImageLoader.ImageCache {
    private val bitmapCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(5242880) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    override fun getBitmap(url: String): Bitmap? {
        return bitmapCache[url]
    }

    override fun putBitmap(url: String, bitmap: Bitmap) {
        bitmapCache.put(url, bitmap)
    }

    fun clear() {
        bitmapCache.evictAll()
    }
}
