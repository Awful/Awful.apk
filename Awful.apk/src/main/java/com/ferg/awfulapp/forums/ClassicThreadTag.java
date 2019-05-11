package com.ferg.awfulapp.forums;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Christoph on 12.03.2017.
 */

public class ClassicThreadTag extends ImageView {

    static final ColorFilter BACKGROUND_FILTER;

    static {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0.7f);
        BACKGROUND_FILTER = new ColorMatrixColorFilter(matrix);
    }

    @Nullable
    private Bitmap tagBitmap = null;

    public ClassicThreadTag(Context context) {
        super(context);
    }

    public ClassicThreadTag(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ClassicThreadTag(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ClassicThreadTag(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        tagBitmap = bm;
        updateBackground();
    }

    private void updateBackground() {
        int w = getWidth();
        int h = getHeight();
        // only do this if the view is laid out and we have a bitmap
        if (w < 1 || h < 1 || tagBitmap == null) {
            return;
        }
        // make a zoomed version of the tag bitmap that fills the view, and set it as the background
        Bitmap backgroundBitmap = ThumbnailUtils.extractThumbnail(tagBitmap, getWidth(), getHeight());
        BitmapDrawable drawable = new BitmapDrawable(getResources(), backgroundBitmap);
        drawable.setColorFilter(BACKGROUND_FILTER);
        drawable.setAlpha(128);
        setBackground(drawable);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateBackground();
    }
}
