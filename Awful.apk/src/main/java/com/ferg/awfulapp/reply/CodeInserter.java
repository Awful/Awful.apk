package com.ferg.awfulapp.reply;

import android.app.Activity;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.ferg.awfulapp.R;

/**
 * Created by baka kaba on 26/09/2016.
 * <p>
 * Handles inserting BBcode code blocks into EditTexts.
 */

abstract class CodeInserter extends Inserter {

    /**
     * Show a dialog to add a code block in a reply's EditText.
     * <p>
     * This will display a dialog allowing the user to create and edit a code block, and choose
     * a language for syntax highlighting. If the EditText currently has text selected, it will be
     * added to the code editing field. If the user adds the block, it will replace any selection,
     * otherwise it will be inserted at the cursor (or the end of the EditText if there's no cursor).     *
     *
     * @param replyMessage The wrapped text will be added here
     * @param activity     The current Activity, used to display the dialog UI
     */
    static void smartInsert(@NonNull final EditText replyMessage, @NonNull final Activity activity) {
        View layout = getDialogLayout(R.layout.insert_code_dialog, activity);
        final EditText textField = (EditText) layout.findViewById(R.id.text_field);
        final Spinner languageSpinner = (Spinner) layout.findViewById(R.id.language_spinner);
        setToSelection(textField, replyMessage);

        DialogInterface.OnClickListener clickListener = (dialog, which) -> {
            // the first option in the dropdown should always be the 'no highlighting' option
            String language = languageSpinner.getSelectedItemPosition() == 0 ? null : (String) languageSpinner.getSelectedItem();
            insertWithoutDialog(replyMessage, textField.getText().toString(), language);
        };

        getDialogBuilder(activity, layout, clickListener).setTitle("Insert code block").show();
    }


    /**
     * Perform the insertion.
     * <p>
     * If a language string is supplied, the code tag will apply it as a parameter for syntax highlighting.
     *
     * @param replyMessage The reply message being edited
     * @param codeText     the code block's text
     * @param language     an optional language name to apply as a code tag parameter
     */
    static void insertWithoutDialog(@NonNull EditText replyMessage, @NonNull String codeText, @Nullable String language) {
        // it's a block element so it's better to have line breaks around it
        String languageParam = language == null ? "" : "=" + language;
        String bbCode = String.format("%n[code%s]%n%s%n[/code]%n", languageParam, codeText);
        insertIntoReply(replyMessage, bbCode);
    }

}
