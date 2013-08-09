package com.ferg.awfulapp.task;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.reply.Reply;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.util.AwfulError;
import org.jsoup.nodes.Document;

import java.sql.Timestamp;

/**
 * Created by matt on 8/8/13.
 */
public class ReplyRequest extends AwfulRequest<ContentValues> {
    private int threadId;
    public ReplyRequest(Context context, int threadId) {
        super(context, Constants.FUNCTION_POST_REPLY);
        this.threadId = threadId;
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        urlBuilder.appendQueryParameter(Constants.PARAM_ACTION, "newreply");
        urlBuilder.appendQueryParameter(Constants.PARAM_THREAD_ID, Integer.toString(threadId));
        return urlBuilder.build().toString();
    }

    @Override
    protected ContentValues handleResponse(Document doc) throws AwfulError {
        ContentValues newReply = Reply.processReply(doc, threadId);
        newReply.put(AwfulProvider.UPDATED_TIMESTAMP, new Timestamp(System.currentTimeMillis()).toString());
//        if(getContentResolver().update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI_REPLY, threadId), newReply, null, null)<1){
//            getContentResolver().insert(AwfulMessage.CONTENT_URI_REPLY, newReply);
//        }
        return newReply;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
