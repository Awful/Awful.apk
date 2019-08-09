package com.ferg.awfulapp.widget;

import android.app.Activity;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.view.Menu;

import com.ferg.awfulapp.R;
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder;
import com.github.rubensousa.bottomsheetbuilder.BottomSheetMenuDialog;
import com.github.rubensousa.bottomsheetbuilder.adapter.BottomSheetItemClickListener;

/**
 * Created by baka kaba on 17/11/2016.
 * <p>
 * A component that displays a menu in a themed bottom sheet dialog.
 * <p>
 * Initialise it with a menu, then set any listeners you need. Calling {@link #toggleVisible(Activity)}
 * will display the dialog using the bottom sheet styles in the activity's current theme.
 * The sheet can also be explicitly dismissed with a call to {@link #dismiss()}.
 */

public class ThemedBottomSheetDialog {

    @NonNull
    private final Menu sheetMenu;
    private BottomSheetMenuDialog bottomSheetMenuDialog = null;
    @Nullable
    private BottomSheetItemClickListener itemClickListener;
    @Nullable
    private Runnable onCancelListener;
    @Nullable
    private Runnable onDismissListener;


    /**
     * Create a new BottomSheetDialog to display the contents of a menu.
     */
    public ThemedBottomSheetDialog(@NonNull Menu contents) {
        sheetMenu = contents;
    }


    /**
     * Set the listeners for this bottom sheet.
     */
    public void setClickListeners(@Nullable BottomSheetItemClickListener itemClickListener,
                                  @Nullable Runnable onCancelListener,
                                  @Nullable Runnable onDismissListener) {

        this.itemClickListener = itemClickListener;
        this.onCancelListener = onCancelListener;
        this.onDismissListener = onDismissListener;
    }


    /**
     * Display or hide the bottom sheet, as appropriate.
     */
    public void toggleVisible(@NonNull Activity activity) {
        // if there's a current sheet dialog, just dismiss it
        if (bottomSheetMenuDialog != null) {
            bottomSheetMenuDialog.dismissWithAnimation();
            bottomSheetMenuDialog = null;
            return;
        }

        // need to apply themed background and text colours programmatically it seems
        TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]{
                R.attr.bottomSheetBackgroundColor,
                R.attr.bottomSheetItemTextColor});
        int backgroundColour = a.getResourceId(0, 0);
        int itemTextColour = a.getResourceId(1, 0);
        a.recycle();

        // build and display the sheet dialog
        bottomSheetMenuDialog = new BottomSheetBuilder(activity)
                .setBackgroundColor(backgroundColour)
                .setItemTextColor(itemTextColour)
                .setMode(BottomSheetBuilder.MODE_GRID)
                .setTitleTextColor(itemTextColour)
                .setMenu(sheetMenu)
                .setItemClickListener(itemClickListener)
                .createDialog();
        bottomSheetMenuDialog.setOnCancelListener(dialog -> handleCancelDismiss(onCancelListener));
        bottomSheetMenuDialog.setOnDismissListener(dialog -> handleCancelDismiss(onDismissListener));
        bottomSheetMenuDialog.show();

        // force the dialog to expand since peek/collapsed has some measurement issue in landscape
        bottomSheetMenuDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }


    /**
     * Dismiss the current dialog, if visible
     */
    public void dismiss() {
        if (bottomSheetMenuDialog != null) {
            bottomSheetMenuDialog.dismiss();
            bottomSheetMenuDialog = null;
        }
    }


    /**
     * Convenience method to clear the dialog and run a listener if appropriate.
     */
    private void handleCancelDismiss(@Nullable Runnable listener) {
        bottomSheetMenuDialog = null;
        if (listener != null) {
            listener.run();
        }
    }

}
