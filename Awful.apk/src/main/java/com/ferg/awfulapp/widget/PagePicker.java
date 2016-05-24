package com.ferg.awfulapp.widget;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by baka kaba on 22/05/2016.
 * <p/>
 * A NumberPicker with min/max buttons, configured as a page selector
 */
public class PagePicker extends MinMaxNumberPicker {

    private static final int FIRST_PAGE = 1;

    /**
     * Get a PagePicker, which can be displayed with {@link #show()}
     *
     * @param context        A context to associate with the AlertDialog
     * @param resultListener A callback for when the user selects a dialog option
     */
    public PagePicker(Context context, int lastPage, int initialPage, @NonNull ResultListener resultListener) {
        super(context, FIRST_PAGE, lastPage, initialPage, "Jump to Page", resultListener);
    }

}
