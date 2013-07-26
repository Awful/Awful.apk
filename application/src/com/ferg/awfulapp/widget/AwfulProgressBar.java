/********************************************************************************
 * Copyright (c) 2012, Matthew Shepard
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the software nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.ferg.awfulapp.widget;

import org.acra.ACRA;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.ferg.awfulapp.AwfulUpdateCallback;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.ColorProvider;

public class AwfulProgressBar extends View implements AwfulUpdateCallback {
	private int mProgress = 100;
	private Paint mProgressColor;
	private Paint mClearColor;
	private AwfulPreferences aPrefs;
	

	public AwfulProgressBar(Context context) {
		super(context);
		aPrefs = AwfulPreferences.getInstance(this.getContext(), this);
		setPaint(context);
	}

	public AwfulProgressBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		aPrefs = AwfulPreferences.getInstance(this.getContext(), this);
		setPaint(context);
	}

	public AwfulProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		aPrefs = AwfulPreferences.getInstance(this.getContext(), this);
		setPaint(context);
	}
	
	private void setPaint(Context context){
		mProgressColor = new Paint();
		mProgressColor.setColor(ColorProvider.getProgressbarColor(AwfulPreferences.getInstance(context)));
		mClearColor = new Paint();
		mClearColor.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawPaint(mClearColor);
		if(getHeight() > 3){
			canvas.drawRect(0, 0, (int)((mProgress/100.0)*getWidth()), getHeight()-2, mProgressColor);
		}else{
			canvas.drawRect(0, 0, (int)((mProgress/100.0)*getWidth()), getHeight(), mProgressColor);
		}
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
	

	@Override
	public void onPreferenceChange(AwfulPreferences prefs) {
			this.setPaint(getContext());
	}

	@Override
	public void loadingFailed(Message aMsg) {}

	@Override
	public void loadingStarted(Message aMsg) {}

	@Override
	public void loadingUpdate(Message aMsg) {}

	@Override
	public void loadingSucceeded(Message aMsg) {}

}
