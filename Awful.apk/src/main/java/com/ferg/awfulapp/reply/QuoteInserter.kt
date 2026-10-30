package com.ferg.awfulapp.reply

import android.app.Activity
import android.content.DialogInterface
import android.view.View
import android.widget.EditText
import com.ferg.awfulapp.R

/**
 * Created by baka kaba on 26/09/2016.
 * 
 * 
 * Handles inserting BBcode quote blocks into an EditText.
 */
internal object QuoteInserter : Inserter() {
    /**
     * Display a dialog to insert a BBcode quote block.
     * 
     * 
     * If the EditText contains selected text, it will automatically be added into the "quote" field.
     * The inserted block will replace any selection, otherwise it will be added at the cursor (or
     * if there's no cursor, at the end of the EditText)
     * 
     * @param replyMessage The wrapped text will be added here
     * @param activity     The current Activity, used to display the dialog UI
     */
    fun smartInsert(replyMessage: EditText, activity: Activity) {
        val layout = getDialogLayout(R.layout.insert_quote_dialog, activity)
        val sourceField = layout!!.findViewById<View?>(R.id.source_field) as EditText
        val textField = layout.findViewById<View?>(R.id.text_field) as EditText
        setToSelection(textField, replyMessage)

        val clickListener =
            DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                val quoteSource = sourceField.text.toString()
                insertWithoutDialog(
                    replyMessage,
                    textField.text.toString(),
                    quoteSource.ifEmpty { null }
                )
            }
        getDialogBuilder(activity, layout, clickListener)
            ?.setTitle("Insert quote")
            ?.show()
    }

    /**
     * Perform the insertion.
     * 
     * 
     * If a quote source is provided, the parameterized "X posted:" tag will be used.
     * 
     * @param replyMessage The reply message being edited
     * @param quoteText    The text of the quote
     * @param quoteSource  An optional quote source
     */
    private fun insertWithoutDialog(
        replyMessage: EditText,
        quoteText: String,
        quoteSource: String?
    ) {
        // add the quote's source as a parameter if we have one
        val sourceParam = if (quoteSource == null) "" else "=\"$quoteSource\""
        val bbCode = String.format("%n[quote%s]%n%s%n[/quote]%n", sourceParam, quoteText)
        insertIntoReply(replyMessage, bbCode)
    }
}
