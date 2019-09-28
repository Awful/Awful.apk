package com.ferg.awfulapp.preferences;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import com.ferg.awfulapp.R;

class AboutDialogBuilder {
    @NonNull
    private final Context context;
    private String message;

    AboutDialogBuilder(@NonNull Context context) {
        this.context = context;
    }

    public Dialog build() {
        return new AlertDialog.Builder(context)
                .setTitle(getTitle())
                .setNeutralButton(android.R.string.ok, (dialog, which) -> {
                })
                .setMessage(message)
                .create();
    }

    public AboutDialogBuilder add(String text) {
        return append(text + "\n");
    }

    public AboutDialogBuilder add(String subtitle, String[] items) {
        StringBuilder stringBuilder = new StringBuilder(subtitle + "\n");

        for (String s : items) {
            stringBuilder.append("- ").append(s).append("\n");
        }

        return append(stringBuilder.toString());
    }

    private AboutDialogBuilder append(String string) {
        if (message == null)
            message = "";
        else
            message += "\n";

        message += string;

        return this;
    }

    private String getTitle() {
        String title = context.getText(R.string.app_name).toString();

        try {
            title += " " +
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // rather unlikely, just return app_name without version
        }

        return title;
    }
}