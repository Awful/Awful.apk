package com.ferg.awfulapp.widget;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.ferg.awfulapp.ForumsIndexActivity;
import com.ferg.awfulapp.ThreadDisplayFragment;

public class ToggleViewPager extends ViewPager{
    private boolean swipeEnabled = true;
    public ToggleViewPager(Context context) {
        super(context);
    }

    public ToggleViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return swipeEnabled && super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return swipeEnabled && super.onTouchEvent(ev);
    }

    public void setSwipeEnabled(boolean swipe){
        swipeEnabled = swipe;
    }
}
