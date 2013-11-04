package com.ferg.awfulapp.htmlwidget;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

public class SpoilerSpan extends ClickableSpan {

	private boolean clicked = false;
	private int defaultColor;
	private int clickedColor;

	public SpoilerSpan(int defaultColor, int clickedColor) {
		this.defaultColor = defaultColor;
		this.clickedColor = clickedColor;
	}

	@Override
	public void updateDrawState(TextPaint ds) {
		if(clicked){
			ds.setColor(clickedColor);
		}else{
			ds.setColor(defaultColor);
		}
	}

	@Override
	public void onClick(View view) {
		clicked = !clicked;
		view.invalidate();
	}

}