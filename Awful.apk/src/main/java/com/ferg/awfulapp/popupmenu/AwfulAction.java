package com.ferg.awfulapp.popupmenu;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

/**
 * Created by baka kaba on 23/05/2017.
 *
 * Interface for items acting as menu actions in a {@link BasePopupMenu}
 */
interface AwfulAction {
    /**
     * Get an icon to display for this menu item.
     */
    @DrawableRes
    int getIconId();

    /**
     * Get the text to display for this menu item.
     */
    @NonNull
    String getMenuLabel();
}
