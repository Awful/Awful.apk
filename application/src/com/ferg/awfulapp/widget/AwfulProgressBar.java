package com.ferg.awfulapp.widget;

import android.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.view.View;

public class AwfulProgressBar extends View {
	private int mProgress = 100;
	private Paint mProgressColor;
	private Paint mClearColor;
	

	public AwfulProgressBar(Context context) {
		super(context);
		setPaint(context);
	}

	public AwfulProgressBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setPaint(context);
	}

	public AwfulProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		setPaint(context);
	}
	
	private void setPaint(Context context){
		mProgressColor = new Paint();
		mProgressColor.setColor(context.getResources().getColor(R.color.holo_blue_light));
		mClearColor = new Paint();
		mClearColor.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawPaint(mClearColor);
		canvas.drawRect(0, 0, (int)((mProgress/100.0)*getWidth()), getHeight(), mProgressColor);
	}
	
	public void setProgress(int progress){
		mProgress = progress;
		if(progress < 100 && progress > 0){
			setVisibility(View.VISIBLE);
			invalidate();
		}else{
			setVisibility(View.GONE);
		}
	}

}
