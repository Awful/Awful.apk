package com.ferg.awfulapp.forums

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.ferg.awfulapp.R
import kotlin.math.min

/**
 * Created by baka kaba on 14/05/2016.
 * 
 * 
 * An image view with a default forum tag drawable, which can be overlaid with some text.
 */
class SquareForumTag : androidx.appcompat.widget.AppCompatImageView {
    // reusable objects, to reduce allocations while scrolling and recycling the views
    var hsv: FloatArray = FloatArray(3)


    // TODO: the colour-setting methods create new ColorFilters each time, might be able to work that in here
    // used to adjust values based on screen density
    private val scaler = resources.displayMetrics.density

    // density-adjusted padding between the text and the top of the view
    private val strokeWidth = 1.5f * scaler

    private var textPaint: Paint? = null
    private var strokePaint: Paint? = null
    private val textBounds = Rect()
    private var tagText = ""

    var tagBackground: Drawable? = null
    var tagFrog: Drawable? = null


    constructor(context: Context) : super(context) {
        init()
    }


    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }


    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }


    private fun init() {
        val counter = ContextCompat.getDrawable(context, R.drawable.forum_tag_frog) as LayerDrawable?
        tagBackground = counter?.findDrawableByLayerId(R.id.square_forum_tag_background)
        tagFrog = counter?.findDrawableByLayerId(R.id.square_forum_tag_frog)?.mutate()
        setImageDrawable(counter)
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint?.color = Color.WHITE

        // text shadow
//        textPaint.setShadowLayer(10f, 0f, 1f, Color.BLACK);
        // text outline
        strokePaint = Paint(textPaint)
        strokePaint?.let {
            it.style = Paint.Style.STROKE
            it.color = STROKE_COLOR
            it.strokeWidth = strokeWidth
            it.strokeJoin = Paint.Join.ROUND
        }
    }


    /**
     * Set the overlaid text for this tag.
     * 
     * 
     * Use an empty string if you want to display nothing
     */
    fun setTagText(text: String) {
        tagText = text
    }


    /**
     * Set the tag's main (background) colour
     * 
     * @param colour An ARGB colour, or null for no colour
     */
    fun setMainColour(@ColorInt colour: Int?) {
        if (colour == null) {
            tagBackground?.colorFilter = null
        } else {
            tagBackground?.setColorFilter(tweakColour(colour), PorterDuff.Mode.SRC)
        }
    }


    /**
     * Set the tag's secondary accent colour
     * 
     * @param colour An ARGB colour, or null for no colour
     */
    fun setAccentColour(@ColorInt colour: Int?) {
        if (colour == null) {
            tagFrog?.colorFilter = null
        } else {
            tagFrog?.setColorFilter(tweakColour(colour), PorterDuff.Mode.SRC_IN)
        }
    }


    /**
     * Adjust a colour, used to tweak provided tag colours
     * 
     * @param colour an ARGB colour
     * @return the adjusted colour
     */
    private fun tweakColour(@ColorInt colour: Int): Int {
        Color.colorToHSV(colour, hsv)
        hsv[1] = hsv[1] * SATURATION_MULTIPLIER
        return Color.HSVToColor(Color.alpha(colour), hsv)
    }


    /**
     * Calculate and set the dynamic text size on the paints.
     * 
     * 
     * This needs to be called while the view is visible, since it uses the view dimensions.
     */
    private fun setTextSize() {
        // get the current bounds of the stroked text (which will be bigger than the normal text)
        strokePaint?.getTextBounds(tagText, 0, tagText.length, textBounds)
        // work out the bounds size as a proportion of the actual view
        val currentWidth = textBounds.width() / width.toFloat()
        val currentHeight = textBounds.height() / height.toFloat()
        // get multipliers to scale the bounds to hit each required size
        val widthMaximiser: Float = DESIRED_TEXT_WIDTH / currentWidth
        val heightMaximiser: Float = MAX_TEXT_HEIGHT / currentHeight
        // the height maximizer is a hard limit, so don't exceed that
        val newTextSize = min(widthMaximiser, heightMaximiser) * strokePaint!!.textSize
        strokePaint?.textSize = newTextSize
        textPaint?.textSize = newTextSize
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (tagText == "") {
            return
        }
        // work out the size of the text box and where it should go
        setTextSize()
        textPaint?.getTextBounds(tagText, 0, tagText.length, textBounds)
        val x = (width - textBounds.width()) / 2
        val y = (height + textBounds.height()) / 2

        canvas.drawText(tagText, x.toFloat(), y.toFloat(), strokePaint!!)
        canvas.drawText(tagText, x.toFloat(), y.toFloat(), textPaint!!)
    }

    companion object {
        private val STROKE_COLOR = "#6E000000".toColorInt()

        // these are both percentages of the view's dimensions
        private const val DESIRED_TEXT_WIDTH = 0.8f
        private const val MAX_TEXT_HEIGHT = 0.3f

        // tint adjustment for the provided tag colors
        private const val SATURATION_MULTIPLIER = 0.8f
    }
}
