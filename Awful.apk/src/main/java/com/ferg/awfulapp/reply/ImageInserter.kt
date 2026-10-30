package com.ferg.awfulapp.reply

import android.app.Activity
import android.content.DialogInterface
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import com.ferg.awfulapp.R

/**
 * Created by baka kaba on 25/09/2016.
 * 
 * 
 * Handles inserting BBcode image tags into EditTexts
 */
internal object ImageInserter : Inserter() {
    /**
     * Display a dialog to insert an image, with options.
     * 
     * 
     * If the supplied EditText has selected text which appears to be a URL, this will be
     * automatically added to the URL field. Otherwise the clipboard will be checked in the same way.
     * 
     * @param replyMessage The wrapped text will be added here
     * @param activity     The current Activity, used to display the dialog UI
     */
    @JvmStatic
    fun smartInsert(replyMessage: EditText, activity: Activity) {
        val layout = getDialogLayout(R.layout.insert_image_dialog, activity)
        val urlField = layout?.findViewById<View?>(R.id.url_field) as EditText
        val thumbnailCheckbox = layout.findViewById<View?>(R.id.use_thumbnail) as CheckBox

        // set the URL field to the selected text or the clipboard contents, if either looks like a URL
        val selectedText = getSelectedText(replyMessage)
        val clipboardText = getClipboardText(activity)
        if (isUrl(selectedText)) {
            setText(urlField, selectedText)
        } else if (isUrl(clipboardText)) {
            setText(urlField, clipboardText)
        }

        val clickListener =
            DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                insertWithoutDialog(
                    replyMessage,
                    urlField.text.toString(),
                    thumbnailCheckbox.isChecked
                )
            }
        getDialogBuilder(activity, layout, clickListener)!!.setTitle("Insert image")
            .show()
    }


    /**
     * Format a URL with BBcode image tags and insert into a reply.
     * 
     * @param replyMessage The reply message being edited
     * @param url          the image URL
     * @param useThumbnail true to use thumbnail tags
     */
    @JvmStatic
    fun insertWithoutDialog(replyMessage: EditText, url: String, useThumbnail: Boolean) {
        val template = if (useThumbnail) "[timg]%s[/timg]" else "[img]%s[/img]"
        val bbCode = String.format(template, url)
        insertIntoReply(replyMessage, bbCode)
    }
}
