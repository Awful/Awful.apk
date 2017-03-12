package com.ferg.awfulapp.forums;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Christoph on 12.03.2017.
 */

public class ClassicThreadTag extends ImageView {

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

    public void applyBackground() {
        if (this.getDrawable() != null) {
            Drawable tagBackground = this.getDrawable().getConstantState().newDrawable().mutate();
            tagBackground.setAlpha(75);
            setBackgroundDrawable(tagBackground);
            setAlpha(255);
        }
    }
}
