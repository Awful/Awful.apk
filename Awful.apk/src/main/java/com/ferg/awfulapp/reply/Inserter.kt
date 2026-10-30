package com.ferg.awfulapp.reply

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.EditText
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import com.ferg.awfulapp.R

/**
 * Created by baka kaba on 25/09/2016.
 * 
 * 
 * General utility methods for Inserter subclasses.
 */
internal abstract class Inserter {
    /**
     * Functional interface for the various untagged inserters
     */
    internal fun interface Untagged {
        fun smartInsert(replyMessage: EditText, activity: Activity)
    }

    companion object {
        /**
         * Inflate a layout for use in a dialog.
         * 
         * 
         * The layout does not have its root parameters set, and is meant to be passed into
         * [.getDialogBuilder]
         * 
         * @return the inflated layout
         */
        @JvmStatic
        fun getDialogLayout(@LayoutRes layoutResId: Int, activity: Activity): View? {
            val inflater = activity.layoutInflater
            return inflater.inflate(layoutResId, null)
        }


        /**
         * Create and configure an AlertDialog Builder.
         * 
         * 
         * This sets the required parameters on the builder, i.e. the layout and the click listener for
         * the OK button. You'll probably want to chain a [AlertDialog.Builder.setTitle]
         * call before calling [AlertDialog.Builder.show].
         * 
         * @param activity              The activity displaying this dialog
         * @param dialogLayout          The custom layout view to display
         * @param positiveClickListener A listener to add to the OK button
         * @return the configured builder, ready to show()
         */
        @JvmStatic
        fun getDialogBuilder(
            activity: Activity,
            dialogLayout: View,
            positiveClickListener: DialogInterface.OnClickListener
        ): AlertDialog.Builder? {
            return AlertDialog.Builder(activity)
                .setTitle("Insert image")
                .setView(dialogLayout)
                .setPositiveButton(R.string.alert_ok, positiveClickListener)
                .setNegativeButton(R.string.cancel, null)
        }


        /**
         * Get the currently selected text in an EditText.
         * 
         * 
         * If there is no selection (just a cursor) then the result will be an empty string.
         * If there is no cursor, this method will return null.
         * 
         * @return the selected text, if any, or null if there is no active cursor
         */
        @JvmStatic
        fun getSelectedText(editText: EditText): String? {
            val start = editText.selectionStart
            val end = editText.selectionEnd
            if (start == -1 || end == -1) {
                return null
            }
            return editText.text.subSequence(start, end).toString()
        }


        /**
         * Set a field's contents to the currently selected text in a reply.
         * 
         * @param dialogField  the field to set
         * @param replyMessage the reply, possibly holding selected text
         */
        @JvmStatic
        fun setToSelection(dialogField: EditText, replyMessage: EditText) {
            val selectedText: String? = getSelectedText(replyMessage)
            setText(dialogField, selectedText)
        }


        /**
         * Set the contents of an EditText.
         * 
         * 
         * If the text string is null, the EditText will be set to empty.
         */
        @JvmStatic
        fun setText(editText: EditText, text: String?) {
            if (!text.isNullOrEmpty()) {
                editText.setText(text)
            }
        }

        /**
         * Replace the currently selected region, defaulting to appending to the EditText if there's no cursor.
         * 
         * @param replyMessage The EditText containing the reply, where the text will be added
         * @param textToInsert The text to add at the cursor or replace the selection with
         */
        @JvmStatic
        fun insertIntoReply(replyMessage: EditText, textToInsert: String) {
            var start = replyMessage.selectionStart
            var end = replyMessage.selectionEnd
            // no cursor? Put it at the end
            if (start == -1 || end == -1) {
                start = replyMessage.length()
                end = start
            }
            replyMessage.text.replace(start, end, textToInsert)
            // deselect and position the cursor after what we just added (newer APIs do this automatically)
            replyMessage.setSelection(start + textToInsert.length)
        }


        /**
         * Get the contents of the clipboard.
         * 
         * @return The clipboard contents, or null if it can't be coerced to a String.
         */
        @JvmStatic
        fun getClipboardText(context: Context): String? {
            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            return if (cb.text == null) null else cb.text.toString()
        }


        /**
         * Check if a string appears to contain a URL.
         * 
         * @return false if the string doesn't look like a URL (or is null)
         */
        @JvmStatic
        fun isUrl(string: String?): Boolean {
            // TODO: 28/09/2016 better handling of the clipboard, ClipData can hold mime types and everything
            if (string == null) {
                return false
            } else if (string.startsWith("http://") || string.startsWith("https://")) {
                return true
            }
            return false
        }
    }
}
