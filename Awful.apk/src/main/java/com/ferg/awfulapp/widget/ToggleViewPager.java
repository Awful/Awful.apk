package com.ferg.awfulapp.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

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
        if(swipeEnabled){
            return super.onInterceptTouchEvent(ev);
        }else{
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(swipeEnabled){
            return super.onTouchEvent(ev);
        }else{
            return false;
        }
    }

    public void setSwipeEnabled(boolean swipe){
        swipeEnabled = swipe;
    }
}
