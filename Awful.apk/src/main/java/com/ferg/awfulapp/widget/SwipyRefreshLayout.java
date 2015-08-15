package com.ferg.awfulapp.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by baka kaba on 15/08/2015.
 *
 * <p>This is a (hopefully) temporary extension of the SwipyRefreshLayout library,
 * to catch and swallow an exception that seems to be caused by an
 * <a href="https://code.google.com/p/android/issues/detail?id=64553">internal bug</a>.</p>
 *
 * <p>When/if this is fixed, remove the same code from {@link ToggleViewPager} too thanks!</p>
 */
public class SwipyRefreshLayout extends com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout{

    public SwipyRefreshLayout(Context context) {
        super(context);
    }

    public SwipyRefreshLayout(Context context, AttributeSet attrs) {
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
     * @param ev            Motion event being passed
     * @param intercepting  Set true when handling onInterceptTouchEvent
     * @return              False if the exception was thrown, otherwise the result of the superclass call
     */
    private boolean antiCrashEventHandler(MotionEvent ev, boolean intercepting) {
        boolean result = false;
        try {
            result = intercepting ? super.onInterceptTouchEvent(ev) : super.onTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return result;
    }
}
