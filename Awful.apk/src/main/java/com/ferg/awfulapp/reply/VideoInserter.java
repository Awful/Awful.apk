package com.ferg.awfulapp.reply;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;

import com.ferg.awfulapp.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by baka kaba on 26/09/2016.
 * <p>
 * Handles inserting BBcode video tags into an EditText.
 */

public abstract class VideoInserter extends Inserter {

    /**
     * Display a dialog to insert a video.
     * <p>
     * If the EditText contains selected text, it will be added to the URL field if it
     * appears to be a URL. Otherwise the clipboard will be checked in the same way.
     * <p>
     * The inserted block will replace any selection, otherwise it will be added at the cursor (or
     * if there's no cursor, at the end of the EditText).
     *
     * @param replyMessage The wrapped text will be added here
     * @param activity     The current Activity, used to display the dialog UI
     */
    public static void insert(@NonNull final EditText replyMessage, @NonNull final Activity activity) {
        View layout = getDialogLayout(R.layout.insert_video_dialog, activity);
        final EditText urlField = (EditText) layout.findViewById(R.id.url_field);

        // set the URL field to the selected text or the clipboard contents, if either looks like a URL
        String selectedText = getSelectedText(replyMessage);
        String clipboardText = getClipboardText(activity);
        if (isUrl(selectedText)) {
            setText(urlField, selectedText);
        } else if (isUrl(clipboardText)) {
            setText(urlField, clipboardText);
        }

        DialogInterface.OnClickListener clickListener = new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                doInsert(replyMessage, urlField.getText().toString());
            }
        };

        getDialogBuilder(activity, layout, clickListener).setTitle("Insert video").show();
    }


    /**
     * Perform the insertion.
     * <p>
     * Video URLs will be sanitised, in case they need to be altered to work with BBcode.
     *
     * @param replyMessage The reply message being edited
     * @param videoUrl     the URL to add to the tag
     */
    private static void doInsert(@NonNull EditText replyMessage, @NonNull String videoUrl) {
        videoUrl = sanitiseUrl(videoUrl);
        final String bbCodeTemplate = "%n[video]%s[/video]%n";
        String bbCode = String.format(bbCodeTemplate, videoUrl);
        insertIntoReply(replyMessage, bbCode);
    }

    /**
     * Handle any annoying URLs the site can't manage, i.e. the mobile youtu.be/lol stuff
     *
     * @param videoUrl the url to check
     * @return a sanitised version if appropriate, otherwise the original is returned
     */
    private static String sanitiseUrl(@NonNull String videoUrl) {
        /*
            I *think* all these mobile URLs are in the format
                youtu.be/{video ID, not a URL param}?{any actual params}
            and normal ones are all
                youtube.com/watch?{normal params including the video ID}
         */
        // matches "youtu.be" URLs, with optional params
        // matcher groups pull out: (anything prefixing the domain) (video ID) (any extra params)
        Pattern mobileYoutubePattern = Pattern.compile("(.*)youtu\\.be/([^?]+)\\??(.*)");
        Matcher matcher = mobileYoutubePattern.matcher(videoUrl);
        if (matcher.find()) {
            // oh good we need to rebuild the whole URL
            String prefix = matcher.group(1);
            String videoId = matcher.group(2);
            String extraParams = matcher.group(3);
            // if there are any params, need to append the & since the first follows the video ID param now
            extraParams = extraParams.isEmpty() ? "" : "&" + extraParams;

            //noinspection SpellCheckingInspection
            return String.format("%syoutube.com/watch?v=%s%s", prefix, videoId, extraParams);
        }
        return videoUrl;
    }


}
