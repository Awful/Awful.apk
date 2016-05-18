package com.ferg.awfulapp.forums;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.ferg.awfulapp.R;

/**
 * Created by baka kaba on 14/05/2016.
 * <p/>
 * An image view with a default forum tag drawable, which can be overlaid with some text.
 */
public class SquareForumTag extends ImageView {

    private static final int STROKE_COLOR = Color.parseColor("#6E000000");
    // the amount of space between the top of the view and the text, in dp (I think)
    private static final int TEXT_TOP_PADDING = 6;
    // these are both percentages of the view's dimensions
    private static final float DESIRED_TEXT_WIDTH = 0.8f;
    private static final float MAX_TEXT_HEIGHT = 0.3f;
    // tint adjustment for the provided tag colours
    private static final float SATURATION_MULTIPLIER = 0.8f;

    // used to adjust values based on screen density
    private final float scaler = getResources().getDisplayMetrics().density;
    // density-adjusted padding between the text and the top of the view
    private final int textTopOffset = (int) (TEXT_TOP_PADDING * scaler);
    private final float strokeWidth = 1.5f * scaler;

    private Paint textPaint;
    private Paint strokePaint;
    private final Rect textBounds = new Rect();
    private String tagText = "";


    public SquareForumTag(Context context) {
        super(context);
        init();
    }


    public SquareForumTag(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public SquareForumTag(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SquareForumTag(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    private void init() {
        Drawable counter = ContextCompat.getDrawable(getContext(), R.drawable.forumtagtest);
        setImageDrawable(counter);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);

        // text shadow
//        textPaint.setShadowLayer(10f, 0f, 1f, Color.BLACK);
        // text outline
        strokePaint = new Paint(textPaint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(STROKE_COLOR);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
    }


    /**
     * Set the overlaid text for this tag.
     * <p/>
     * Use an empty string if you want to display nothing
     */
    public void setTagText(@NonNull String text) {
        tagText = text;
    }


    /**
     * Set the tag's main (background) colour
     *
     * @param colour An ARGB colour, or null for no colour
     */
    public void setMainColour(@ColorInt Integer colour) {
        if (colour == null) {
            setBackgroundColor(Color.TRANSPARENT);
        } else {
            setBackgroundColor(tweakColour(colour));
        }
    }


    /**
     * Set the tag's secondary accent colour
     *
     * @param colour An ARGB colour, or null for no colour
     */
    public void setAccentColour(@ColorInt Integer colour) {
        if (colour == null) {
            setColorFilter(null);
        } else {
            setColorFilter(tweakColour(colour), PorterDuff.Mode.MULTIPLY);
        }
    }


    /**
     * Adjust a colour, used to tweak provided tag colours
     *
     * @param colour an ARGB colour
     * @return the adjusted colour
     */
    private int tweakColour(@ColorInt int colour) {
        float[] hsv = new float[3];
        Color.colorToHSV(colour, hsv);
        hsv[1] = hsv[1] * SATURATION_MULTIPLIER;
        return Color.HSVToColor(Color.alpha(colour), hsv);
    }


    /**
     * Calculate and set the dynamic text size on the paints.
     * <p/>
     * This needs to be called while the view is visible, since it uses the view dimensions.
     */
    private void setTextSize() {
        // get the current bounds of the stroked text (which will be bigger than the normal text)
        strokePaint.getTextBounds(tagText, 0, tagText.length(), textBounds);
        // work out the bounds size as a proportion of the actual view
        float currentWidth = textBounds.width() / (float) getWidth();
        float currentHeight = textBounds.height() / (float) getHeight();
        // get multipliers to scale the bounds to hit each required size
        float widthMaximiser = DESIRED_TEXT_WIDTH / currentWidth;
        float heightMaximiser = MAX_TEXT_HEIGHT / currentHeight;
        // the height maximiser is a hard limit, so don't exceed that
        float newTextSize = Math.min(widthMaximiser, heightMaximiser) * strokePaint.getTextSize();
        strokePaint.setTextSize(newTextSize);
        textPaint.setTextSize(newTextSize);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (tagText.equals("")) {
            return;
        }
        // work out the size of the text box and where it should go
        setTextSize();
        textPaint.getTextBounds(tagText, 0, tagText.length(), textBounds);
        int x = (getWidth() - textBounds.width()) / 2;
        int y = (textBounds.height() + textTopOffset);

        canvas.drawText(tagText, x, y, strokePaint);
        canvas.drawText(tagText, x, y, textPaint);
    }
}
