package com.ferg.awfulapp.reply;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;

import com.ferg.awfulapp.R;

/**
 * Created by baka kaba on 25/09/2016.
 * <p>
 * Handles inserting BBcode URL tags into an EditText.
 */

public abstract class UrlInserter extends Inserter {

    /**
     * Display a dialog to insert a URL.
     * <p>
     * If the EditText contains selected text, it will be added to the URL field if it appears to be
     * a URL, otherwise it will be added to the link text field.
     * If the URL field is not filled, the clipboard will be checked in the same way.
     * <p>
     * If the user leaves the link text blank, the URL will be used for the display text.
     * <p>
     * The inserted block will replace any selection, otherwise it will be added at the cursor (or
     * if there's no cursor, at the end of the EditText).
     *
     * @param replyMessage The wrapped text will be added here
     * @param activity     The current Activity, used to display the dialog UI
     */
    public static void insert(@NonNull final EditText replyMessage, @NonNull final Activity activity) {
        View layout = getDialogLayout(R.layout.insert_url_dialog, activity);
        final EditText urlField = (EditText) layout.findViewById(R.id.url_field);
        final EditText textField = (EditText) layout.findViewById(R.id.text_field);

        // Apply the selected text to the URL field if it looks like a URL, otherwise set it as
        // the link text and try to use the clipboard contents as the URL
        if (isUrl(getSelectedText(replyMessage))) {
            setToSelection(urlField, replyMessage);
        } else {
            setToSelection(textField, replyMessage);
            String clipboardText = getClipboardText(activity);
            if (isUrl(clipboardText)) {
                setText(urlField, clipboardText);
            }
        }

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // if the link text is blank, use the URL
                String linkText = textField.getText().toString();
                String url = urlField.getText().toString();
                doInsert(replyMessage, url, linkText.isEmpty() ? url : linkText);
            }
        };
        getDialogBuilder(activity, layout, clickListener).setTitle("Insert URL").show();
    }


    /**
     * Perform the insertion.
     *
     * @param replyMessage The reply message being edited
     * @param url          The URL of the link
     * @param linkText     The text to display
     */
    private static void doInsert(@NonNull EditText replyMessage, @NonNull String url, @NonNull String linkText) {
        String bbCode = String.format("[url=\"%s\"]%s[/url]", url, linkText);
        insertIntoReply(replyMessage, bbCode);
    }


}
