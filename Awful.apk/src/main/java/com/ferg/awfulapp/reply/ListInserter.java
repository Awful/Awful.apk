package com.ferg.awfulapp.reply;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.ferg.awfulapp.R;

/**
 * Created by baka kaba on 26/09/2016.
 * <p>
 * Handles inserting BBcode lists into an EditText.
 */

abstract class ListInserter extends Inserter {

    // fixed ordering for the extra list type options, so we can check selected options by position
    // (and the labels can be changed/translated)
    private final static int NUMERICAL = 1;
    private final static int ALPHABETICAL = 2;

    /**
     * Display a dialog to add a BBcode list block.
     * <p>
     * If the EditText contains selected text, this will be added to the list editing field
     * in the dialog.
     *
     * @param replyMessage The wrapped text will be added here
     * @param activity     The current Activity, used to display the dialog UI
     */
    static void smartInsert(@NonNull final EditText replyMessage, @NonNull final Activity activity) {
        View layout = getDialogLayout(R.layout.insert_list_dialog, activity);
        final EditText textField = (EditText) layout.findViewById(R.id.list_items_field);
        final Spinner listTypeSpinner = (Spinner) layout.findViewById(R.id.list_type_spinner);
        setToSelection(textField, replyMessage);

        DialogInterface.OnClickListener clickListener = (dialog, which) -> {
            int listTypeIndex = listTypeSpinner.getSelectedItemPosition();
            insertWithoutDialog(replyMessage, textField.getText().toString(), listTypeIndex);
        };
        getDialogBuilder(activity, layout, clickListener).setTitle("Insert list").show();
    }

    /**
     * Perform the insertion, using formatting parameters if required.
     * <p>
     * If a recognised type constant is supplied, the list will be formatted. These are basically index
     * positions in the options dropdown, with position 0 as the default (no formatting). If you change
     * the string array containing the options, the order needs to match these position constants.
     *
     * @param replyMessage  The reply message being edited
     * @param listItems     The items in the list, separated by newlines
     * @param listTypeIndex A type constant used to format
     */
    private static void insertWithoutDialog(@NonNull EditText replyMessage, @NonNull String listItems, int listTypeIndex) {
        // build the outer tags according to the selected list type
        String tagFormatString;
        switch (listTypeIndex) {
            case NUMERICAL:
                tagFormatString = "%n[list=1]%n%s[/list=1]%n";
                break;
            case ALPHABETICAL:
                tagFormatString = "%n[list=A]%n%s[/list=A]%n";
                break;
            default:
                tagFormatString = "%n[list]%n%s[/list]%n";
        }

        // tag each list item (they all end with a line break, so no need for one before the [/list] tag)
        StringBuilder items = new StringBuilder();
        for (String item : listItems.split("\n")) {
            items.append("[*] ").append(item).append("\n");
        }

        // build the list and insert it
        String bbCode = String.format(tagFormatString, items.toString());
        insertIntoReply(replyMessage, bbCode);
    }

}
