package com.ferg.awfulapp.preferences;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

/**
 * Created by baka kaba on 13/06/2015.
 *
 * <p>Hey guess what - SwitchPreference didn't actually properly work properly until Lollipop!
 * Scrolling them out of view can actually make them flip randomly.
 * {@see <a href="https://code.google.com/p/android/issues/detail?id=26194">Fun!</a>}</p>
 *
 * Thanks google!
 */
public class CustomSwitchPreference extends SwitchPreference {

    public CustomSwitchPreference(Context context) {
        this(context, null);
    }

    public CustomSwitchPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.switchPreferenceStyle);
    }

    public CustomSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

}
