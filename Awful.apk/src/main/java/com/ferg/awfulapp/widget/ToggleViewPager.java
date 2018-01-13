package com.ferg.awfulapp.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.ferg.awfulapp.ForumsIndexActivity;

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
        return antiCrashEventHandler(ev, true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return antiCrashEventHandler(ev, false);
    }


    /**
     * Fix to avoid apparent bug in the support library, with infrequent crashing from an IAE.
     * (See <a href="https://code.google.com/p/android/issues/detail?id=64553">this issue</a>.)
     * @param ev            Event being passed
     * @param intercepting  Set true when handling onInterceptTouchEvent
     * @return              False if swiping is disabled or the exception was thrown,
     *                      otherwise the result of the superclass call
     */
    private boolean antiCrashEventHandler(MotionEvent ev, boolean intercepting) {
        /*
            When/if this is fixed, remove the internal SwipyRefreshLayout class and
            refactor the XML layouts to use the external library version again, thanks!
         */
        boolean result = false;
        try {
            result = intercepting ? super.onInterceptTouchEvent(ev) : super.onTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return swipeEnabled && result;
    }

    public void setSwipeEnabled(boolean swipe){
        swipeEnabled = swipe;
    }

    @Override
    public ForumsIndexActivity.ForumPagerAdapter getAdapter() {
        return (ForumsIndexActivity.ForumPagerAdapter) super.getAdapter();
    }
}
