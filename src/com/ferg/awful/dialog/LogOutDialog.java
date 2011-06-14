package com.ferg.awful.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import com.ferg.awful.AwfulLoginActivity;
import com.ferg.awful.R;
import com.ferg.awful.network.NetworkUtils;

public class LogOutDialog extends AlertDialog {
    Context c;
    public LogOutDialog(Context context) {
        super(context);
        c = context;
        setTitle(context.getString(R.string.logout));
        setMessage(context.getString(R.string.logout_message));
        setButton(context.getString(R.string.logout), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                NetworkUtils.clearLoginCookies(getContext());
                getContext().startActivity(new Intent().setClass(c, AwfulLoginActivity.class));
            }
        });
        setButton2(context.getString(R.string.cancel), (OnClickListener) null);
    }
}
