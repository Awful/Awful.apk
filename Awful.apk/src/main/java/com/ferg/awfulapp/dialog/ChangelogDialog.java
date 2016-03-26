package com.ferg.awfulapp.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import com.ferg.awfulapp.R;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>Created by baka kaba on 23/03/2016.</p>
 *
 * <p>A class to display changelogs</p>
 *
 * <p>To add a new entry, just add a new item to the string array in changelog.xml</p>
 */
public abstract class ChangelogDialog {

    /**
     * Display the current changelog as a dialog
     * @param context   the activity which will style the dialog
     */
    public static void show(@NonNull Context context) {
        // get the changelog text, each version separated by a blank line
        Resources res = context.getResources();
        String changelogText = StringUtils.join(res.getStringArray(R.array.changelog), "\n\n");

        // Build a basic dialog with the changelog data
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.changelog_dialog_title))
                .setMessage(changelogText)
                .setPositiveButton(context.getString(R.string.alert_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

}
