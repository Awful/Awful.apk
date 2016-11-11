package com.ferg.awfulapp.reply;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.ferg.awfulapp.R;

/**
 * Created by baka kaba on 25/09/2016.
 * <p>
 * General utility methods for Inserter subclasses.
 */

abstract class Inserter {

    /**
     * Functional interface for the various untagged inserters
     */
    interface Untagged {
        void insert(@NonNull final EditText replyMessage, @NonNull final Activity activity);
    }

    /**
     * Inflate a layout for use in a dialog.
     * <p>
     * The layout does not have its root parameters set, and is meant to be passed into
     * {@link #getDialogBuilder(Activity, View, DialogInterface.OnClickListener)}
     *
     * @return the inflated layout
     */
    static View getDialogLayout(@LayoutRes int layoutResId, @NonNull Activity activity) {
        LayoutInflater inflater = activity.getLayoutInflater();
        return inflater.inflate(layoutResId, null);
    }


    /**
     * Create and configure an AlertDialog Builder.
     * <p>
     * This sets the required parameters on the builder, i.e. the layout and the click listener for
     * the OK button. You'll probably want to chain a {@link AlertDialog.Builder#setTitle(CharSequence)}
     * call before calling {@link AlertDialog.Builder#show()}.
     *
     * @param activity              The activity displaying this dialog
     * @param dialogLayout          The custom layout view to display
     * @param positiveClickListener A listener to add to the OK button
     * @return the configured builder, ready to show()
     */
    static AlertDialog.Builder getDialogBuilder(@NonNull Activity activity,
                                                @NonNull View dialogLayout,
                                                @NonNull DialogInterface.OnClickListener positiveClickListener) {
        return new AlertDialog.Builder(activity)
                .setTitle("Insert image")
                .setView(dialogLayout)
                .setPositiveButton(R.string.alert_ok, positiveClickListener)
                .setNegativeButton(R.string.cancel, null);
    }


    /**
     * Get the currently selected text in an EditText.
     * <p>
     * If there is no selection (just a cursor) then the result will be an empty string.
     * If there is no cursor, this method will return null.
     *
     * @return the selected text, if any, or null if there is no active cursor
     */
    @Nullable
    static String getSelectedText(@NonNull EditText editText) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if (start == -1 || end == -1) {
            return null;
        }
        return String.valueOf(editText.getText().subSequence(start, end));
    }


    /**
     * Set a field's contents to the currently selected text in a reply.
     *
     * @param dialogField  the field to set
     * @param replyMessage the reply, possibly holding selected text
     */
    static void setToSelection(@NonNull EditText dialogField, @NonNull EditText replyMessage) {
        String selectedText = getSelectedText(replyMessage);
        setText(dialogField, selectedText);
    }


    /**
     * Set the contents of an EditText.
     * <p>
     * If the text string is null, the EditText will be set to empty.
     */
    static void setText(@NonNull EditText editText, @Nullable String text) {
        if (text != null && !text.isEmpty()) {
            editText.setText(text);
        }
    }

    /**
     * Replace the currently selected region, defaulting to appending to the EditText if there's no cursor.
     *
     * @param replyMessage The EditText containing the reply, where the text will be added
     * @param textToInsert The text to add at the cursor or replace the selection with
     */
    static void insertIntoReply(@NonNull EditText replyMessage, @NonNull String textToInsert) {
        int start = replyMessage.getSelectionStart();
        int end = replyMessage.getSelectionEnd();
        // no cursor? Put it at the end
        if (start == -1 || end == -1) {
            start = replyMessage.length();
            end = start;
        }
        replyMessage.getText().replace(start, end, textToInsert);
        // deselect and position the cursor after what we just added (newer APIs do this automatically)
        replyMessage.setSelection(start + textToInsert.length());
    }


    /**
     * Get the contents of the clipboard.
     *
     * @return The clipboard contents, or null if it can't be coerced to a String.
     */
    @Nullable
    static String getClipboardText(@NonNull Context context) {
        android.content.ClipboardManager cb = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        return cb.getText() == null ? null : cb.getText().toString();
    }


    /**
     * Check if a string appears to contain a URL.
     *
     * @return false if the string doesn't look like a URL (or is null)
     */
    static boolean isUrl(@Nullable String string) {
        // TODO: 28/09/2016 better handling of the clipboard, ClipData can hold mime types and everything
        if (string == null) {
            return false;
        } else if (string.startsWith("http://") || string.startsWith("https://")) {
            return true;
        }
        return false;
    }
}
