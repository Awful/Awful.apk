package com.ferg.awfulapp.widget

import android.content.Context

/**
 * Created by baka kaba on 22/05/2016.
 * 
 * 
 * A NumberPicker with min/max buttons, configured as a page selector
 *
 * Get a PagePicker, which can be displayed with [.show]
 *
 * @param context        A context to associate with the AlertDialog
 * @param resultListener A callback for when the user selects a dialog option
 */
class PagePicker(context: Context, lastPage: Int, initialPage: Int, resultListener: ResultListener) :
    MinMaxNumberPicker(context, FIRST_PAGE, lastPage, initialPage, "Jump to Page", resultListener) {
    companion object {
        private const val FIRST_PAGE = 1
    }
}
