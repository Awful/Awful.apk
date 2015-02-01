/*-
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ferg.awfulapp.htmlwidget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

/**
 * Provides smooth horizontal scrolling for {@link TextView TextViews} that
 * extend past the edge of the screen.
 * <p>
 * TODO: Replace this class by implementing smooth scrolling in {@link HtmlView}?
 */
public class HorizontalScroller extends HorizontalScrollView {

    private static int getMaxLineWidth(TextView textView) {
        Layout layout = textView.getLayout();
        if (layout != null) {
            int lineCount = layout.getLineCount();
            float max = 0;
            for (int i = 0; i < lineCount; i++) {
                float lineWidth = layout.getLineWidth(i);
                max = Math.max(max, lineWidth);
            }
            return (int) max;
        } else {
            return 0;
        }
    }

    public HorizontalScroller(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public HorizontalScroller(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalScroller(Context context) {
        super(context);
    }

    @Override
    protected void measureChild(@NonNull View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();

        int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft()
                + getPaddingRight(), lp.width);
        int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec, getPaddingTop()
                + getPaddingBottom(), lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(@NonNull View child, int parentWidthMeasureSpec, int widthUsed,
            int parentHeightMeasureSpec, int heightUsed) {
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft()
                + getPaddingRight() + lp.leftMargin + lp.rightMargin + widthUsed, lp.width);
        int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec, getPaddingTop()
                + getPaddingBottom() + lp.topMargin + lp.bottomMargin + heightUsed, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int visibility = child.getVisibility();
            if (visibility != GONE && child instanceof TextView) {
                int childLeft = child.getLeft();
                int childTop = child.getTop();
                int childRight = child.getRight();
                int childBottom = child.getBottom();

                int padding = child.getPaddingLeft() + child.getPaddingRight();
                int maxLineWidth = getMaxLineWidth((TextView) child);
                int preferredRight = childLeft + maxLineWidth + padding;

                if (childRight < preferredRight) {
                    child.layout(childLeft, childTop, preferredRight, childBottom);
                }
            }
        }
    }
}
