package com.ferg.awfulapp.reply;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;

import com.ferg.awfulapp.R;

import static com.ferg.awfulapp.reply.BasicTextInserter.BbCodeTag.FIXED;

/**
 * Created by baka kaba on 26/09/2016.
 * <p>
 * Handles inserting basic, unparameterised BBcode tags into EditTexts.
 */

abstract class BasicTextInserter extends Inserter {

    /**
     * Wrap selected text in a BBcode tag, or show a dialog to insert some.
     * <p>
     * This will take an EditText object and check its current selection and cursor position.
     * If text is selected, it will be wrapped in the BBcode that relates to the supplied tag.
     * Otherwise, a dialog will pop up allowing the user to enter text that will be inserted at
     * the cursor position (or at the end of the EditText, if there's no cursor).
     * <p>
     * These are simple parameterless tags, and all are applied inline except for {@link BbCodeTag#PRE}
     * which displays as a block element.
     *
     * @param replyMessage The wrapped text will be added here
     * @param tag          The tag type to add
     * @param activity     The current Activity, used to display the dialog UI
     */
    static void smartInsert(@NonNull final EditText replyMessage,
                            @NonNull final BbCodeTag tag,
                            @NonNull final Activity activity) {
        // if there's text selected, just wrap it - don't show a dialog
        String selectedText = getSelectedText(replyMessage);
        if (selectedText != null && !selectedText.isEmpty()) {
            insertWithoutDialog(replyMessage, selectedText, tag);
            return;
        }

        // inflate the dialog layout and set up the UI
        View layout = getDialogLayout(R.layout.insert_text_dialog, activity);
        final EditText textField = (EditText) layout.findViewById(R.id.text_field);
        if (tag == FIXED) {
            textField.setTypeface(Typeface.MONOSPACE);
        }
        setToSelection(textField, replyMessage);

        DialogInterface.OnClickListener clickListener = (dialog, which) ->
                insertWithoutDialog(replyMessage, textField.getText().toString(), tag);
        getDialogBuilder(activity, layout, clickListener).setTitle(tag.dialogTitle).show();
    }

    /**
     * Perform the insertion.
     *
     * @param replyMessage The reply message being edited
     * @param text         The text being wrapped
     * @param tag          The tag to wrap with
     */
    static void insertWithoutDialog(@NonNull EditText replyMessage, @NonNull String text, @NonNull BbCodeTag tag) {
        String bbCode = String.format(tag.tagFormatString, text);
        insertIntoReply(replyMessage, bbCode);
    }


    /**
     * Represents simple (parameterless) BBcode tags.
     */
    enum BbCodeTag {
        BOLD("Insert bold text", "[b]%s[/b]"),
        ITALICS("Insert italic text", "[i]%s[/i]"),
        UNDERLINE("Insert underlined text", "[u]%s[/u]"),
        STRIKEOUT("Insert strike text", "[s]%s[/s]"),
        SPOILER("Insert spoiler text", "[spoiler]%s[/spoiler]"),
        FIXED("Insert fixed-width text", "[fixed]%s[/fixed]"),
        SUPERSCRIPT("Insert superscript text", "[super]%s[/super]"),
        SUBSCRIPT("Insert subscript text", "[sub]%s[/sub]"),
        // this one is a block element, so it has line breaks for neatness in the reply view
        PRE("Insert preserved whitespace block", "%n[pre]%n%s%n[/pre]%n");

        @NonNull
        private final String dialogTitle;
        @NonNull
        private final String tagFormatString;

        /**
         * @param dialogTitle     The title shown in the dialog when adding text with this tag
         * @param tagFormatString The format string used to wrap text with this tag
         */
        BbCodeTag(@NonNull String dialogTitle, @NonNull String tagFormatString) {
            this.dialogTitle = dialogTitle;
            this.tagFormatString = tagFormatString;
        }
    }


}
