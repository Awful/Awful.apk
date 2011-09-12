package com.ferg.awful.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.webkit.WebView;
import android.widget.ImageView;
import android.util.AttributeSet;

public class SnapshotWebView extends WebView {
    private Bitmap mSnapshot;
    private ImageView mSnapshotView;

    public SnapshotWebView(Context aContext) {
        super(aContext);
    }

    public SnapshotWebView(Context aContext, AttributeSet aAttributeSet) {
        super(aContext, aAttributeSet);
    }

    public void setSnapshotView(ImageView aSnapshotView) {
        mSnapshotView = aSnapshotView;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        if (!hasWindowFocus) {
            if (mSnapshot == null) {
                mSnapshot = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            }
            Canvas c = new Canvas(mSnapshot);
            c.translate(-getScrollX(), -getScrollY());
            draw(c);
            mSnapshotView.setImageBitmap(mSnapshot);
        } else if (mSnapshotView != null) {
            mSnapshotView.setImageDrawable(null);
        }
        setVisibility(hasWindowFocus ? VISIBLE : INVISIBLE);
        mSnapshotView.setVisibility(hasWindowFocus ? INVISIBLE : VISIBLE);
    }
    
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != VISIBLE && mSnapshot != null) {
            mSnapshot.recycle();
            mSnapshot = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mSnapshotView.setImageDrawable(null);

        if (mSnapshot != null) {
            mSnapshot.recycle();
            mSnapshot = null;
        }
    } 
}
