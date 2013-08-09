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
public class QuoteRequest extends AwfulRequest<ContentValues> {
    private int threadId, postId;
    public QuoteRequest(Context context, int threadId, int postId) {
        super(context, Constants.FUNCTION_POST_REPLY);
        this.threadId = threadId;
        this.postId = postId;
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        urlBuilder.appendQueryParameter(Constants.PARAM_ACTION, "newreply");
        urlBuilder.appendQueryParameter(Constants.PARAM_POST_ID, Integer.toString(postId));
        return urlBuilder.build().toString();
    }

    @Override
    protected ContentValues handleResponse(Document doc) throws AwfulError {
        ContentValues newReply = Reply.processQuote(doc, threadId, postId);
        newReply.put(AwfulProvider.UPDATED_TIMESTAMP, new Timestamp(System.currentTimeMillis()).toString());
        return newReply;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
