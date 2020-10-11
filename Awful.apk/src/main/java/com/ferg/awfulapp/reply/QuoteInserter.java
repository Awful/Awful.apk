package com.ferg.awfulapp.reply;

import android.app.Activity;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.EditText;

import com.ferg.awfulapp.R;

/**
 * Created by baka kaba on 26/09/2016.
 * <p>
 * Handles inserting BBcode quote blocks into an EditText.
 */

abstract class QuoteInserter extends Inserter {

    /**
     * Display a dialog to insert a BBcode quote block.
     * <p>
     * If the EditText contains selected text, it will automatically be added into the "quote" field.
     * The inserted block will replace any selection, otherwise it will be added at the cursor (or
     * if there's no cursor, at the end of the EditText)
     *
     * @param replyMessage The wrapped text will be added here
     * @param activity     The current Activity, used to display the dialog UI
     */
    static void smartInsert(@NonNull final EditText replyMessage, @NonNull final Activity activity) {
        View layout = getDialogLayout(R.layout.insert_quote_dialog, activity);
        final EditText sourceField = (EditText) layout.findViewById(R.id.source_field);
        final EditText textField = (EditText) layout.findViewById(R.id.text_field);
        setToSelection(textField, replyMessage);

        DialogInterface.OnClickListener clickListener = (dialog, which) -> {
            String quoteSource = sourceField.getText().toString();
            insertWithoutDialog(replyMessage, textField.getText().toString(), quoteSource.isEmpty() ? null : quoteSource);
        };
        getDialogBuilder(activity, layout, clickListener).setTitle("Insert quote").show();
    }

    /**
     * Perform the insertion.
     * <p>
     * If a quote source is provided, the parameterised "X posted:" tag will be used.
     *
     * @param replyMessage The reply message being edited
     * @param quoteText    The text of the quote
     * @param quoteSource  An optional quote source
     */
    private static void insertWithoutDialog(@NonNull EditText replyMessage, @NonNull String quoteText, @Nullable String quoteSource) {
        // add the quote's source as a parameter if we have one
        String sourceParam = quoteSource == null ? "" : "=\"" + quoteSource + "\"";
        String bbCode = String.format("%n[quote%s]%n%s%n[/quote]%n", sourceParam, quoteText);
        insertIntoReply(replyMessage, bbCode);
    }

}
