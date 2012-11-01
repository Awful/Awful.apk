package com.ferg.awfulapp.widget;

import com.ferg.awfulapp.widget.AwfulFragmentPagerAdapter.AwfulPagerFragment;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class AwfulDualPaneView extends ViewGroup implements AwfulPagerFragment {
	private static final String TAG = "AwfulDualPaneView";
	
	private Fragment primary;
	private Fragment secondary;
	private AwfulPagerFragment primaryAPF;
	private AwfulPagerFragment secondaryAPF;
	
	public AwfulDualPaneView(Context context) {
		super(context);
	}
	
	public AwfulDualPaneView(Context context, AwfulPagerFragment first, AwfulPagerFragment second) {
		super(context);
		primary = (Fragment) first;
		secondary = (Fragment) second;
		primaryAPF = first;
		secondaryAPF = second;
	}
	
	public AwfulDualPaneView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AwfulDualPaneView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int width = View.MeasureSpec.getSize(widthMeasureSpec);
		int height = View.MeasureSpec.getSize(heightMeasureSpec);
		if(getChildCount() == 2){
			getChildAt(0).measure(MeasureSpec.makeMeasureSpec(2*width/5, MeasureSpec.EXACTLY), heightMeasureSpec);
			getChildAt(1).measure(MeasureSpec.makeMeasureSpec(3*width/5, MeasureSpec.EXACTLY), heightMeasureSpec);
		}
		
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		Log.e(TAG, "onLayout - l: "+l+" t: "+t+" r: "+r+" b: "+b+" changed: "+(changed?"true":"false"));
		if(getChildCount() > 1){
			getChildAt(0).layout(0, 0, 2*r/5, b);
			getChildAt(1).layout(2*r/5, 0, r, b);
		}
	}
	
	public void clearFragments(){
		primary = null;
		secondary = null;
		primaryAPF = null;
		secondaryAPF = null;
	}
	
	private boolean hasView(){
		return primary != null && secondary != null;
	}

	@Override
	public boolean onBackPressed() {
		return false;
	}

	@Override
	public void onPageVisible() {
		if(hasView()){
			primaryAPF.onPageVisible();
			secondaryAPF.onPageVisible();
		}
	}

	@Override
	public void onPageHidden() {
		if(hasView()){
			primaryAPF.onPageHidden();
			secondaryAPF.onPageHidden();
		}
	}

	@Override
	public String getTitle() {
		if(hasView()){
			String title = secondaryAPF.getTitle();
			if(title != null && title.length() > 0){
				return title;
			}else{
				return primaryAPF.getTitle();
			}
		}
		return null;
	}

	@Override
	public boolean canScrollX(int dx, int y) {
		return false;
	}

	@Override
	public int getProgressPercent() {
		if(hasView()){
			return Math.min(primaryAPF.getProgressPercent(), secondaryAPF.getProgressPercent());
		}
		return 100;
	}

	@Override
	public void fragmentMessage(String type, String contents) {
		if(hasView()){
			primaryAPF.fragmentMessage(type, contents);
			secondaryAPF.fragmentMessage(type, contents);
		}
	}

	@Override
	public boolean canSplitscreen() {
		if(hasView()){
			return primaryAPF.canSplitscreen() && secondaryAPF.canSplitscreen();
		}
		return false;
	}
	
	public AwfulPagerFragment getFirst(){
		return primaryAPF;
	}
	
	public AwfulPagerFragment getSecond(){
		return secondaryAPF;
	}

	public void refreshChildren() {
		if(primary != null && secondary != null){
			View priView = primary.getView();
			View secView = secondary.getView();
			removeAllViews();
			addView(priView);
			addView(secView);
		}
	}

}
