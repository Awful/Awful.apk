package com.ferg.awfulapp.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

public class AwfulHorizontalScrollView extends HorizontalScrollView {
	//This just exists to automatically scroll to the right when the view is onLayout.
	public AwfulHorizontalScrollView(Context context) {
		super(context);
	}
	public AwfulHorizontalScrollView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}
	public AwfulHorizontalScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		scrollTo(getWidth(), 0);
	}
	
	

}
