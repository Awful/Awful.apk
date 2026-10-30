package com.ferg.awfulapp.reply

import android.app.Activity
import android.content.DialogInterface
import android.view.View
import android.widget.EditText
import com.ferg.awfulapp.R
import org.apache.commons.lang3.StringUtils

/**
 * Created by baka kaba on 25/09/2016.
 * 
 * 
 * Handles inserting BBcode URL tags into an EditText.
 */
internal object UrlInserter : Inserter() {
    /**
     * Display a dialog to insert a URL.
     * 
     * 
     * If the EditText contains selected text, it will be added to the URL field if it appears to be
     * a URL, otherwise it will be added to the link text field.
     * If the URL field is not filled, the clipboard will be checked in the same way.
     * 
     * 
     * If the user leaves the link text blank, the URL will be used for the display text.
     * 
     * 
     * The inserted block will replace any selection, otherwise it will be added at the cursor (or
     * if there's no cursor, at the end of the EditText).
     * 
     * @param replyMessage The wrapped text will be added here
     * @param activity     The current Activity, used to display the dialog UI
     */
    fun smartInsert(replyMessage: EditText, activity: Activity) {
        val layout = getDialogLayout(R.layout.insert_url_dialog, activity)
        val urlField = layout?.findViewById<View?>(R.id.url_field) as EditText
        val textField = layout.findViewById<View?>(R.id.text_field) as EditText

        // Apply the selected text to the URL field if it looks like a URL, otherwise set it as
        // the link text and try to use the clipboard contents as the URL
        if (isUrl(getSelectedText(replyMessage))) {
            setToSelection(urlField, replyMessage)
        } else {
            setToSelection(textField, replyMessage)
            val clipboardText = getClipboardText(activity)
            if (isUrl(clipboardText)) {
                setText(urlField, clipboardText)
            }
        }

        val clickListener =
            DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                insertWithoutDialog(
                    replyMessage,
                    urlField.text.toString(),
                    textField.text.toString()
                )
            }
        getDialogBuilder(activity, layout, clickListener)!!.setTitle("Insert URL")
            .show()
    }


    /**
     * Perform the insertion.
     * 
     * 
     * If (non-empty) link text is provided then the url is added in the opening tag, otherwise
     * the url is added between the tags.
     * 
     * @param replyMessage The reply message being edited
     * @param url          The URL of the link
     * @param linkText     Optional text to display
     */
    fun insertWithoutDialog(replyMessage: EditText, url: String, linkText: String?) {
        val formatString =
            if (StringUtils.isEmpty(linkText)) "[url]%s[/url]" else "[url=\"%s\"]%s[/url]"
        val bbCode = String.format(formatString, url, linkText)
        insertIntoReply(replyMessage, bbCode)
    }
}
