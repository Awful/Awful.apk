/*
 * Copyright (C) 2010 Daniel Nilsson
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

package com.ferg.awful.preferences;

import com.ferg.awful.R;

import android.app.Dialog;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

public class ColorPickerDialog 
	extends 
		Dialog 
	implements
		ColorPickerView.OnColorChangedListener,
		View.OnClickListener, android.view.View.OnKeyListener {

	private ColorPickerView mColorPicker;

	private ColorPickerPanelView mOldColor;
	private ColorPickerPanelView mNewColor;

	private OnColorChangedListener mListener;
	
	private EditText mColorEditCode;
	private EditText mOldColorEditCode;

	public interface OnColorChangedListener {
		public void onColorChanged(int color);
	}
	
	public ColorPickerDialog(Context context, int initialColor) {
		super(context);

		init(initialColor);
	}

	private void init(int color) {
		// To fight color branding.
		getWindow().setFormat(PixelFormat.RGBA_8888);

		setUp(color);

	}

	private void setUp(int color) {
		
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View layout = inflater.inflate(R.layout.dialog_color_picker, null);

		setContentView(layout);

		setTitle(R.string.dialog_color_picker);
		
		mColorPicker = (ColorPickerView) layout.findViewById(R.id.color_picker_view);
		mOldColor = (ColorPickerPanelView) layout.findViewById(R.id.old_color_panel);
		mNewColor = (ColorPickerPanelView) layout.findViewById(R.id.new_color_panel);
		mColorEditCode = (EditText) layout.findViewById(R.id.new_color_code);
		mOldColorEditCode = (EditText) layout.findViewById(R.id.old_color_code);
		
		((LinearLayout) mOldColor.getParent()).setPadding(
			Math.round(mColorPicker.getDrawingOffset()), 
			0, 
			Math.round(mColorPicker.getDrawingOffset()), 
			0
		);	

		mColorEditCode.setOnKeyListener(this);
		mColorEditCode.setText(Integer.toHexString(color).substring(2));
		mOldColorEditCode.setText(Integer.toHexString(color).substring(2));
		mOldColor.setOnClickListener(this);
		mNewColor.setOnClickListener(this);
		mColorPicker.setOnColorChangedListener(this);
		mOldColor.setColor(color);
		mColorPicker.setColor(color, true);

	}

	@Override
	public void onColorChanged(int color) {

		mNewColor.setColor(color);
		//using substring instead of bit arithmetic because toHexString will cut off leading digits otherwise.
		mColorEditCode.setText(Integer.toHexString(color).substring(2));

		/*
		if (mListener != null) {
			mListener.onColorChanged(color);
		}
		*/

	}

	public void setAlphaSliderVisible(boolean visible) {
		mColorPicker.setAlphaSliderVisible(visible);
	}
	
	/**
	 * Set a OnColorChangedListener to get notified when the color
	 * selected by the user has changed.
	 * @param listener
	 */
	public void setOnColorChangedListener(OnColorChangedListener listener){
		mListener = listener;
	}

	public int getColor() {
		return mColorPicker.getColor();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.old_color_panel:
				break;
			case R.id.new_color_panel:
				if (mListener != null) {
					mListener.onColorChanged(mNewColor.getColor());
				}
				break;
		}
		dismiss();
	}

	@Override
	public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
		if(arg0.getId() == mColorEditCode.getId()){
			String code = mColorEditCode.getText().toString();
			if(code.length() == 8 && !code.matches("[^0-9a-fA-F]")){
				mNewColor.setColor((int) (Long.parseLong(code, 16) & 0x00FFFFFF) | 0xFF000000);
			}
		}
		return false;
	}
	
}
