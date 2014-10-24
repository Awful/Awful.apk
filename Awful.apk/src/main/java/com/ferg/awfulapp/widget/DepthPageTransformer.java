package com.ferg.awfulapp.widget;

import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * Created by Christoph on 20.10.2014.
 */
public class DepthPageTransformer implements ViewPager.PageTransformer {
    private static final float MIN_SCALE = 0.75f;

    public void transformPage(View view, float position) {
        view.setTranslationX(position < 0 ? 0f : -view.getWidth() * position);
    }
}