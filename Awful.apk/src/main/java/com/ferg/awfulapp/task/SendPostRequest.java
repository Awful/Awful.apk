package com.ferg.awfulapp.task;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

/**
 * Created by matt on 8/8/13.
 */
public class SendPostRequest extends AwfulRequest<Void> {
    public SendPostRequest(Context context, ContentValues reply) {
        super(context, null);
        addPostParam(Constants.PARAM_ACTION, "postreply");
        addPostParam(Constants.PARAM_THREAD_ID, Integer.toString(reply.getAsInteger(AwfulMessage.ID)));
        addPostParam(Constants.PARAM_FORMKEY, reply.getAsString(AwfulPost.FORM_KEY));
        addPostParam(Constants.PARAM_FORM_COOKIE, reply.getAsString(AwfulPost.FORM_COOKIE));
        addPostParam(Constants.PARAM_MESSAGE, NetworkUtils.encodeHtml(reply.getAsString(AwfulMessage.REPLY_CONTENT)));
        addPostParam(Constants.PARAM_PARSEURL, Constants.YES);
        if(reply.containsKey(AwfulPost.FORM_BOOKMARK) && reply.getAsString(AwfulPost.FORM_BOOKMARK).equalsIgnoreCase("checked")){
            addPostParam(Constants.PARAM_BOOKMARK, Constants.YES);
        }
        if(reply.containsKey(AwfulMessage.REPLY_SIGNATURE)){
            addPostParam(AwfulMessage.REPLY_SIGNATURE, Constants.YES);
        }
        if(reply.containsKey(AwfulMessage.REPLY_DISABLE_SMILIES)){
            addPostParam(AwfulMessage.REPLY_DISABLE_SMILIES, Constants.YES);
        }
        if(reply.containsKey(AwfulMessage.REPLY_ATTACHMENT)){
            Toast.makeText(context, "Attaching: " + reply.getAsString(AwfulMessage.REPLY_ATTACHMENT), Toast.LENGTH_LONG).show();
            attachFile(Constants.PARAM_ATTACHMENT, reply.getAsString(AwfulMessage.REPLY_ATTACHMENT));
        }

        buildFinalRequest();
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        return Constants.FUNCTION_POST_REPLY;
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.getErrorCode() == AwfulError.ERROR_PROBATION || error.isCritical();//Don't allow probation to pass
    }
}
