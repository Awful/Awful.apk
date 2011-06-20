package com.ferg.awful.htmlwidget;

import android.graphics.Color;
import android.os.Parcel;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

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