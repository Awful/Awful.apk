package com.ferg.awfulapp.task;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

/**
 * Created by matt on 9/11/13.
 */
public class SendPrivateMessageRequest extends AwfulRequest<Void> {
    private int pmId;
    public SendPrivateMessageRequest(Context context, int replyId) {
        super(context, Constants.FUNCTION_PRIVATE_MESSAGE);
        pmId = replyId;
        //TODO note: this is a quick-and-dirty conversion to the new request system.
        //we need to revamp this draft system to match the changes made in the post reply system.
        Cursor pmInfo = context.getContentResolver().query(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, replyId), AwfulProvider.DraftProjection, null, null, null);
        if (pmInfo != null && pmInfo.moveToFirst()) {
            addPostParam(Constants.PARAM_PRIVATE_MESSAGE_ID, Integer.toString(replyId));
            addPostParam(Constants.PARAM_ACTION, Constants.ACTION_DOSEND);
            addPostParam(Constants.DESTINATION_TOUSER, pmInfo.getString(pmInfo.getColumnIndex(AwfulMessage.RECIPIENT)));
            addPostParam(Constants.PARAM_TITLE, NetworkUtils.encodeHtml(pmInfo.getString(pmInfo.getColumnIndex(AwfulMessage.TITLE))));
            if (replyId > 0) {
                addPostParam("prevmessageid", Integer.toString(replyId));
            }
            addPostParam(Constants.PARAM_PARSEURL, Constants.YES);
            addPostParam("savecopy", "yes");
            addPostParam("iconid", "0");
            addPostParam(Constants.PARAM_MESSAGE, NetworkUtils.encodeHtml(pmInfo.getString(pmInfo.getColumnIndex(AwfulMessage.REPLY_CONTENT))));
        } else {
            Toast.makeText(context, "Unable to send private message!", Toast.LENGTH_LONG).show();
        }
        if (pmInfo != null) {
            pmInfo.close();
        }
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        return Constants.FUNCTION_PRIVATE_MESSAGE;
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        getContentResolver().delete(AwfulMessage.CONTENT_URI, AwfulMessage.ID+"=?", AwfulProvider.int2StrArray(pmId));
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
