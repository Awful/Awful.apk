package com.ferg.awfulapp.task;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.PostPreviewParseTask;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

import timber.log.Timber;

/**
 * Created by matt on 8/8/13.
 */
public class PreviewEditRequest extends AwfulRequest<String> {
    public PreviewEditRequest(Context context, ContentValues reply) {
        super(context, null);
        addPostParam(Constants.PARAM_ACTION, "updatepost");
        addPostParam(Constants.PARAM_POST_ID, Integer.toString(reply.getAsInteger(AwfulPost.EDIT_POST_ID)));
        Timber.e(Constants.PARAM_POST_ID + ": " + Integer.toString(reply.getAsInteger(AwfulPost.EDIT_POST_ID)));
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
        addPostParam(Constants.PARAM_SUBMIT, Constants.SUBMIT_REPLY);
        addPostParam(Constants.PARAM_PREVIEW, Constants.PREVIEW_REPLY);

        buildFinalRequest();
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        return Constants.FUNCTION_EDIT_POST;
    }

    @Override
    protected String handleResponse(Document doc) throws AwfulError {
        return new PostPreviewParseTask(doc).call();
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        Timber.e(error);
        return error.getErrorCode() == AwfulError.ERROR_PROBATION || error.isCritical();//Don't allow probation to pass
    }
}
