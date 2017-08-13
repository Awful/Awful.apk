package com.ferg.awfulapp.reply;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.ferg.awfulapp.R;

/**
 * Created by baka kaba on 25/09/2016.
 * <p>
 * Handles inserting BBcode image tags into EditTexts
 */

abstract class ImageInserter extends Inserter {

    /**
     * Display a dialog to insert an image, with options.
     * <p>
     * If the supplied EditText has selected text which appears to be a URL, this will be
     * automatically added to the URL field. Otherwise the clipboard will be checked in the same way.
     *
     * @param replyMessage The wrapped text will be added here
     * @param activity     The current Activity, used to display the dialog UI
     */
    static void smartInsert(@NonNull final EditText replyMessage, @NonNull final Activity activity) {
        View layout = getDialogLayout(R.layout.insert_image_dialog, activity);
        final EditText urlField = (EditText) layout.findViewById(R.id.url_field);
        final CheckBox thumbnailCheckbox = (CheckBox) layout.findViewById(R.id.use_thumbnail);

        // set the URL field to the selected text or the clipboard contents, if either looks like a URL
        String selectedText = getSelectedText(replyMessage);
        String clipboardText = getClipboardText(activity);
        if (isUrl(selectedText)) {
            setText(urlField, selectedText);
        } else if (isUrl(clipboardText)) {
            setText(urlField, clipboardText);
        }

        DialogInterface.OnClickListener clickListener = (dialog, which) ->
                insertWithoutDialog(replyMessage, urlField.getText().toString(), thumbnailCheckbox.isChecked());
        getDialogBuilder(activity, layout, clickListener).setTitle("Insert image").show();
    }


    /**
     * Format a URL with BBcode image tags and insert into a reply.
     *
     * @param replyMessage The reply message being edited
     * @param url          the image URL
     * @param useThumbnail true to use thumbnail tags
     */
    static void insertWithoutDialog(@NonNull EditText replyMessage, @NonNull String url, boolean useThumbnail) {
        //noinspection SpellCheckingInspection
        String template = useThumbnail ? "[timg]%s[/timg]" : "[img]%s[/img]";
        String bbCode = String.format(template, url);
        insertIntoReply(replyMessage, bbCode);
    }

}
