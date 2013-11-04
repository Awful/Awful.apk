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
public class PMRequest extends AwfulRequest<Void> {
    private int id;
    public PMRequest(Context context, int id) {
        super(context, Constants.FUNCTION_PRIVATE_MESSAGE);
        this.id = id;
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        urlBuilder.appendQueryParameter(Constants.PARAM_ACTION, "show");
        urlBuilder.appendQueryParameter(Constants.PARAM_PRIVATE_MESSAGE_ID, Integer.toString(id));
        return urlBuilder.build().toString();
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        ContentValues message = AwfulMessage.processMessage(doc, id);
        if(getContentResolver().update(ContentUris.withAppendedId(AwfulMessage.CONTENT_URI, id), message, null, null)<1){
            getContentResolver().insert(AwfulMessage.CONTENT_URI, message);
        }
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
