package com.ferg.awful.preferences;

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;

/**
 * An {@link EditTextPreference} which only accepts numeric input. By
 * default, this only accepts positive integers. This can be changed with
 * the {@link #setAcceptsDecimal(boolean)} and/or {@link #setAcceptsSigned(boolean)}
 * methods.
 * 
 * This method stores values in preferences as ints or floats, not Strings. Which
 * one depends on whether and how {@link #setAcceptsDecimal(boolean)} has been
 * called. By default, it uses ints.
 */
public class NumericEditTextPreference extends EditTextPreference {
	private boolean mAcceptsSigned;
	private boolean mAcceptsDecimal;
	
	public NumericEditTextPreference(Context context) {
		super(context);
		initialize();
	}

	public NumericEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	public NumericEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}
	
	private void initialize() {
		mAcceptsSigned = false;
		mAcceptsDecimal = false;
		
		resetTextListener();
	}
	
	private void resetTextListener() {
		getEditText().setKeyListener(DigitsKeyListener.getInstance(mAcceptsSigned, mAcceptsDecimal));
	}
	
	public void setAcceptsSigned(boolean aAcceptsSigned) {
		mAcceptsSigned = aAcceptsSigned;
	}
	
	public void setAcceptsDecimal(boolean aAcceptsDecimal) {
		mAcceptsDecimal = aAcceptsDecimal;
	}
	
	@Override
    protected String getPersistedString(String defaultReturnValue) {
		if(mAcceptsDecimal) {
			return String.valueOf(getPersistedFloat(0.0f));
		} else {
			return String.valueOf(getPersistedInt(0));			
		}
    }

    @Override
    protected boolean persistString(String value) {
    	if(mAcceptsDecimal) {
    		return persistFloat(Float.valueOf(value));
    	} else {
    		return persistInt(Integer.valueOf(value));
    	}
    }
}
