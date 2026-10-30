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
 * Handles inserting BBcode lists into an EditText.
 */
internal object ListInserter : Inserter() {
    // fixed ordering for the extra list type options, so we can check selected options by position
    // (and the labels can be changed/translated)
    private const val NUMERICAL = 1
    private const val ALPHABETICAL = 2

    /**
     * Display a dialog to add a BBcode list block.
     * 
     * 
     * If the EditText contains selected text, this will be added to the list editing field
     * in the dialog.
     * 
     * @param replyMessage The wrapped text will be added here
     * @param activity     The current Activity, used to display the dialog UI
     */
    @JvmStatic
    fun smartInsert(replyMessage: EditText, activity: Activity) {
        val layout = getDialogLayout(R.layout.insert_list_dialog, activity)
        val textField = layout?.findViewById<View?>(R.id.list_items_field) as EditText
        val listTypeSpinner = layout.findViewById<View?>(R.id.list_type_spinner) as Spinner
        setToSelection(textField, replyMessage)

        val clickListener = DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
            val listTypeIndex = listTypeSpinner.selectedItemPosition
            insertWithoutDialog(replyMessage, textField.text.toString(), listTypeIndex)
        }
        getDialogBuilder(activity, layout, clickListener)
            ?.setTitle("Insert list")
            ?.show()
    }

    /**
     * Perform the insertion, using formatting parameters if required.
     * 
     * 
     * If a recognized type constant is supplied, the list will be formatted. These are basically index
     * positions in the options dropdown, with position 0 as the default (no formatting). If you change
     * the string array containing the options, the order needs to match these position constants.
     * 
     * @param replyMessage  The reply message being edited
     * @param listItems     The items in the list, separated by newlines
     * @param listTypeIndex A type constant used to format
     */
    private fun insertWithoutDialog(replyMessage: EditText, listItems: String, listTypeIndex: Int) {
        // build the outer tags according to the selected list type
        val tagFormatString = when (listTypeIndex) {
            NUMERICAL -> "%n[list=1]%n%s[/list=1]%n"
            ALPHABETICAL -> "%n[list=A]%n%s[/list=A]%n"
            else -> "%n[list]%n%s[/list]%n"
        }

        // tag each list item (they all end with a line break, so no need for one before the [/list] tag)
        val items = StringBuilder()
        for (item in listItems.split(Regex("\n")).dropLastWhile { it.isEmpty() }
            .toTypedArray()) {
            items.append("[*] ").append(item).append("\n")
        }

        // build the list and insert it
        val bbCode = String.format(tagFormatString, items.toString())
        insertIntoReply(replyMessage, bbCode)
    }
}
