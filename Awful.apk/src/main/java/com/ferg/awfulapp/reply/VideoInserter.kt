package com.ferg.awfulapp.reply

import android.app.Activity
import android.content.DialogInterface
import android.view.View
import android.widget.EditText
import com.ferg.awfulapp.R
import java.util.regex.Pattern

/**
 * Created by baka kaba on 26/09/2016.
 * 
 * 
 * Handles inserting BBcode video tags into an EditText.
 */
internal object VideoInserter : Inserter() {
    /**
     * Display a dialog to insert a video.
     * 
     * 
     * If the EditText contains selected text, it will be added to the URL field if it
     * appears to be a URL. Otherwise the clipboard will be checked in the same way.
     * 
     * 
     * The inserted block will replace any selection, otherwise it will be added at the cursor (or
     * if there's no cursor, at the end of the EditText).
     * 
     * @param replyMessage The wrapped text will be added here
     * @param activity     The current Activity, used to display the dialog UI
     */
    fun smartInsert(replyMessage: EditText, activity: Activity) {
        val layout = getDialogLayout(R.layout.insert_video_dialog, activity)
        val urlField = layout?.findViewById<View?>(R.id.url_field) as EditText

        // set the URL field to the selected text or the clipboard contents, if either looks like a URL
        val selectedText = getSelectedText(replyMessage)
        val clipboardText = getClipboardText(activity)
        if (isUrl(selectedText)) {
            setText(urlField, selectedText)
        } else if (isUrl(clipboardText)) {
            setText(urlField, clipboardText)
        }

        val clickListener =
            DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                insertWithoutDialog(
                    replyMessage,
                    urlField.text.toString()
                )
            }
        getDialogBuilder(activity, layout, clickListener)
            ?.setTitle("Insert video")
            ?.show()
    }


    /**
     * Perform the insertion.
     * 
     * 
     * Video URLs will be sanitized, in case they need to be altered to work with BBcode.
     * 
     * @param replyMessage The reply message being edited
     * @param videoUrl     the URL to add to the tag
     */
    fun insertWithoutDialog(replyMessage: EditText, videoUrl: String) {
        var videoUrl = videoUrl
        videoUrl = sanitiseUrl(videoUrl)
        val bbCodeTemplate = "%n[video]%s[/video]%n"
        val bbCode = String.format(bbCodeTemplate, videoUrl)
        insertIntoReply(replyMessage, bbCode)
    }


    /**
     * Handle any annoying URLs the site can't manage, i.e. the mobile youtu.be/lol stuff
     * 
     * @param videoUrl the url to check
     * @return a sanitized version if appropriate, otherwise the original is returned
     */
    private fun sanitiseUrl(videoUrl: String): String {
        /*
            I *think* all these mobile URLs are in the format
                youtu.be/{video ID, not a URL param}?{any actual params}
            and normal ones are all
                youtube.com/watch?{normal params including the video ID}
         */
        // matches "youtu.be" URLs, with optional params
        // matcher groups pull out: (anything prefixing the domain) (video ID) (any extra params)
        val mobileYoutubePattern = Pattern.compile("(.*)youtu\\.be/([^?]+)\\??(.*)")
        val matcher = mobileYoutubePattern.matcher(videoUrl)
        if (matcher.find()) {
            // oh good we need to rebuild the whole URL
            val prefix = matcher.group(1)
            val videoId = matcher.group(2)
            var extraParams = matcher.group(3)
            // if there are any params, need to append the & since the first follows the video ID param now
            extraParams = if (extraParams!!.isEmpty()) "" else "&$extraParams"

            return String.format("%syoutube.com/watch?v=%s%s", prefix, videoId, extraParams)
        }
        return videoUrl
    }
}
