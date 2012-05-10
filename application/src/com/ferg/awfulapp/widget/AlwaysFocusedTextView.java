package com.ferg.awfulapp.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * This code was mostly cribbed from
 * http://stackoverflow.com/questions/1827751/is-there-a-way-to-make-ellipsize-marquee-always-scroll/2504840#2504840
 *
 */

public class AlwaysFocusedTextView extends TextView {

    public AlwaysFocusedTextView(Context context) {
        super(context);
    }

    public AlwaysFocusedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AlwaysFocusedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if(focused)
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    public void onWindowFocusChanged(boolean focused) {
        if(focused)
            super.onWindowFocusChanged(focused);
    }


    @Override
    public boolean isFocused() {
        return true;
    }

}
