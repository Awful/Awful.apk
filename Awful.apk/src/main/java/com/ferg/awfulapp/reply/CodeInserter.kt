package com.ferg.awfulapp.reply

import android.app.Activity
import android.content.DialogInterface
import android.view.View
import android.widget.EditText
import android.widget.Spinner
import com.ferg.awfulapp.R

/**
 * Created by baka kaba on 26/09/2016.
 * 
 * 
 * Handles inserting BBcode code blocks into EditTexts.
 */
internal object CodeInserter : Inserter() {
    /**
     * Show a dialog to add a code block in a reply's EditText.
     * 
     * 
     * This will display a dialog allowing the user to create and edit a code block, and choose
     * a language for syntax highlighting. If the EditText currently has text selected, it will be
     * added to the code editing field. If the user adds the block, it will replace any selection,
     * otherwise it will be inserted at the cursor (or the end of the EditText if there's no cursor).     *
     * 
     * @param replyMessage The wrapped text will be added here
     * @param activity     The current Activity, used to display the dialog UI
     */
    @JvmStatic
    fun smartInsert(replyMessage: EditText, activity: Activity) {
        val layout = getDialogLayout(R.layout.insert_code_dialog, activity)
        val textField = layout?.findViewById<View?>(R.id.text_field) as EditText
        val languageSpinner = layout.findViewById<View?>(R.id.language_spinner) as Spinner
        setToSelection(textField, replyMessage)

        val clickListener = DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
            // the first option in the dropdown should always be the 'no highlighting' option
            val language = if (languageSpinner.selectedItemPosition == 0) null else languageSpinner.selectedItem.toString()
            insertWithoutDialog(replyMessage, textField.text.toString(), language)
        }

        getDialogBuilder(activity, layout, clickListener)?.setTitle("Insert code block")?.show()
    }


    /**
     * Perform the insertion.
     * 
     * 
     * If a language string is supplied, the code tag will apply it as a parameter for syntax highlighting.
     * 
     * @param replyMessage The reply message being edited
     * @param codeText     the code block's text
     * @param language     an optional language name to apply as a code tag parameter
     */
    fun insertWithoutDialog(replyMessage: EditText, codeText: String, language: String?) {
        // it's a block element so it's better to have line breaks around it
        val languageParam = if (language == null) "" else "=$language"
        val bbCode = String.format("%n[code%s]%n%s%n[/code]%n", languageParam, codeText)
        insertIntoReply(replyMessage, bbCode)
    }
}
