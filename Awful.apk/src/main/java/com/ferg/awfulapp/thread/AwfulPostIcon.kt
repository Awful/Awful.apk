package com.ferg.awfulapp.thread

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.media.ThumbnailUtils
import android.os.Handler
import android.os.Looper
import androidx.annotation.DrawableRes
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageLoader.ImageContainer
import com.android.volley.toolbox.ImageLoader.ImageListener
import com.ferg.awfulapp.R
import com.ferg.awfulapp.network.NetworkUtils.imageLoader
import org.jsoup.select.Elements
import java.util.Locale
import androidx.core.graphics.drawable.toDrawable
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap

/**
 * Created by Christoph on 16.11.2016.
 */
class AwfulPostIcon {
    val iconId: String
    val iconUrl: String
    @JvmField
    val drawableId: Int
    @JvmField
    var drawable: Drawable? = null

    constructor(iconId: String, iconUrl: String, context: Context) {
        this.iconId = iconId
        this.iconUrl = iconUrl
        drawableId = getIconResId(iconUrl, context)
        if (drawableId == BLANK_ICON_DRAWABLE_ID) {
            Handler(Looper.getMainLooper()).post {
                imageLoader?.get(iconUrl, object : ImageListener {
                    override fun onResponse(response: ImageContainer, isImmediate: Boolean) {
                        drawable = getClassicIconDrawable(response.bitmap ?: return, context)
                    }

                    override fun onErrorResponse(error: VolleyError?) {}
                })
            }
        }
    }


    /**
     * A default empty icon, to represent 'no icon' options
     */
    private constructor() {
        iconId = BLANK_ICON_ID
        iconUrl = ""
        drawableId = BLANK_ICON_DRAWABLE_ID
        drawable = null
    }


    companion object {
        val BACKGROUND_FILTER: ColorFilter

        init {
            val matrix = ColorMatrix()
            matrix.setSaturation(0.7f)
            BACKGROUND_FILTER = ColorMatrixColorFilter(matrix)
        }

        @DrawableRes
        val BLANK_ICON_DRAWABLE_ID: Int = R.drawable.empty_thread_tag
        const val BLANK_ICON_ID: String = "0"
        @JvmField
        val BLANK_ICON: AwfulPostIcon = AwfulPostIcon()
        private const val TAG_SIZE = 90

        @DrawableRes
        private fun getIconResId(iconUrl: String, context: Context): Int {
            val localFileName = "@drawable/" + iconUrl.substring(
                iconUrl.lastIndexOf('/') + 1,
                iconUrl.lastIndexOf('.')
            ).replace('-', '_').lowercase(
                Locale.getDefault()
            )
            val imageID = context.resources.getIdentifier(localFileName, null, context.packageName)
            return if (imageID == 0) BLANK_ICON_DRAWABLE_ID else imageID
        }

        @JvmStatic
        fun getClassicIconDrawable(bitmap: Bitmap, context: Context): Drawable {
            // make a zoomed version of the tag bitmap that fills the view, and set it as the background
            val backgroundBitmap = ThumbnailUtils.extractThumbnail(bitmap, TAG_SIZE, TAG_SIZE)
            val backgroundDrawable = BitmapDrawable(context.resources, backgroundBitmap)
            backgroundDrawable.colorFilter = BACKGROUND_FILTER
            backgroundDrawable.alpha = 128

            val foregroundDrawable = bitmap.toDrawable(context.resources)
            val newHeight = ((TAG_SIZE.toFloat() / foregroundDrawable.intrinsicWidth.toFloat()) * foregroundDrawable.intrinsicHeight.toFloat()).roundToInt()
            val verticalInset: Int = (TAG_SIZE - newHeight) / 2

            val mashDrawable =
                LayerDrawable(arrayOf<Drawable>(backgroundDrawable, foregroundDrawable))
            mashDrawable.setLayerInset(0, 0, 0, 0, 0)
            mashDrawable.setLayerInset(1, 0, verticalInset, 0, verticalInset)

            val finalBitmap = createBitmap(TAG_SIZE, TAG_SIZE)
            mashDrawable.setBounds(0, 0, TAG_SIZE, TAG_SIZE)
            mashDrawable.draw(Canvas(finalBitmap))
            return finalBitmap.toDrawable(context.resources)
        }

        fun parsePostIcons(icons: Elements, context: Context): ArrayList<AwfulPostIcon> {
            val result = ArrayList<AwfulPostIcon>()

            for (icon in icons) {
                val iconUrl = icon.child(1).attr("src")
                val iconId = icon.child(0).`val`()
                val postIcon = AwfulPostIcon(iconId, iconUrl, context)
                result.add(postIcon)
            }

            return result
        }
    }
}
