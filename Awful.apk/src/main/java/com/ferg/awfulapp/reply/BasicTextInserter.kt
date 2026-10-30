package com.ferg.awfulapp.reply

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Typeface
import android.view.View
import android.widget.EditText
import com.ferg.awfulapp.R

/**
 * Created by baka kaba on 26/09/2016.
 * 
 * 
 * Handles inserting basic, unparameterised BBcode tags into EditTexts.
 */
internal object BasicTextInserter : Inserter() {
    /**
     * Wrap selected text in a BBcode tag, or show a dialog to insert some.
     *
     * 
     * This will take an EditText object and check its current selection and cursor position.
     * If text is selected, it will be wrapped in the BBcode that relates to the supplied tag.
     * Otherwise, a dialog will pop up allowing the user to enter text that will be inserted at
     * the cursor position (or at the end of the EditText, if there's no cursor).
     * 
     * 
     * These are simple parameterless tags, and all are applied inline except for [BbCodeTag.PRE]
     * which displays as a block element.
     * 
     * @param replyMessage The wrapped text will be added here
     * @param tag          The tag type to add
     * @param activity     The current Activity, used to display the dialog UI
     */
    @JvmStatic
    fun smartInsert(
        replyMessage: EditText,
        tag: BbCodeTag,
        activity: Activity
    ) {
        // if there's text selected, just wrap it - don't show a dialog
        val selectedText = getSelectedText(replyMessage)
        if (!selectedText.isNullOrEmpty()) {
            insertWithoutDialog(replyMessage, selectedText, tag)
            return
        }

        // inflate the dialog layout and set up the UI
        val layout = getDialogLayout(R.layout.insert_text_dialog, activity)
        val textField = layout!!.findViewById<View?>(R.id.text_field) as EditText
        if (tag == BbCodeTag.FIXED) {
            textField.typeface = Typeface.MONOSPACE
        }
        setToSelection(textField, replyMessage)

        val clickListener = DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                insertWithoutDialog(
                    replyMessage,
                    textField.text.toString(),
                    tag
                )
            }
        getDialogBuilder(activity, layout, clickListener)!!.setTitle(tag.dialogTitle)
            .show()
    }

    /**
     * Perform the insertion.
     * 
     * @param replyMessage The reply message being edited
     * @param text         The text being wrapped
     * @param tag          The tag to wrap with
     */
    fun insertWithoutDialog(replyMessage: EditText, text: String, tag: BbCodeTag) {
        val bbCode = String.format(tag.tagFormatString, text)
        insertIntoReply(replyMessage, bbCode)
    }


    /**
     * Represents simple (parameterless) BBcode tags.
     */
    internal enum class BbCodeTag
    /**
     * @param dialogTitle     The title shown in the dialog when adding text with this tag
     * @param tagFormatString The format string used to wrap text with this tag
     */(val dialogTitle: String, val tagFormatString: String) {
        BOLD("Insert bold text", "[b]%s[/b]"),
        ITALICS("Insert italic text", "[i]%s[/i]"),
        UNDERLINE("Insert underlined text", "[u]%s[/u]"),
        STRIKEOUT("Insert strike text", "[s]%s[/s]"),
        SPOILER("Insert spoiler text", "[spoiler]%s[/spoiler]"),
        FIXED("Insert fixed-width text", "[fixed]%s[/fixed]"),
        SUPERSCRIPT("Insert superscript text", "[super]%s[/super]"),
        SUBSCRIPT("Insert subscript text", "[sub]%s[/sub]"),

        // this one is a block element, so it has line breaks for neatness in the reply view
        PRE("Insert preserved whitespace block", "%n[pre]%n%s%n[/pre]%n")
    }
}
