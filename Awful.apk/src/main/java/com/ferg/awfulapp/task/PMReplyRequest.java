package com.ferg.awfulapp.task;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

/**
 * Created by matt on 8/8/13.
 */
public class PMReplyRequest extends AwfulRequest<Void> {
    private int id;
    public PMReplyRequest(Context context, int id) {
        super(context, Constants.FUNCTION_PRIVATE_MESSAGE);
        this.id = id;
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        urlBuilder.appendQueryParameter(Constants.PARAM_ACTION, "newmessage");
        urlBuilder.appendQueryParameter(Constants.PARAM_PRIVATE_MESSAGE_ID, Integer.toString(id));
        return urlBuilder.build().toString();
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        ContentValues reply = AwfulMessage.processReplyMessage(doc, id);
        //we remove the reply content so as not to override the existing reply.
        String replyContent = reply.getAsString(AwfulMessage.REPLY_CONTENT);
        reply.remove(AwfulMessage.REPLY_CONTENT);
        String replyTitle = reply.getAsString(AwfulMessage.TITLE);
        reply.remove(AwfulMessage.TITLE);
        if(getContentResolver().update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, id), reply, null, null)<1){
            //but if the reply doesn't already exist, re-add that reply and insert it.
            reply.put(AwfulMessage.REPLY_CONTENT, replyContent);
            reply.put(AwfulMessage.TITLE, replyTitle);
            getContentResolver().insert(AwfulMessage.CONTENT_URI_REPLY, reply);
        }
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
