package com.ferg.awfulapp.popupmenu

import androidx.annotation.DrawableRes

/**
 * Created by baka kaba on 23/05/2017.
 * 
 * Interface for items acting as menu actions in a [BasePopupMenu]
 */
interface AwfulAction {
    @get:DrawableRes
    val iconId: Int

    /**
     * Get the text to display for this menu item.
     */
    val menuLabel: String
}
