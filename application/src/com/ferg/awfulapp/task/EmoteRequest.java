package com.ferg.awfulapp.task;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulEmote;
import com.ferg.awfulapp.util.AwfulError;
import org.jsoup.nodes.Document;

import java.util.ArrayList;

/**
 * Created by matt on 8/8/13.
 */
public class EmoteRequest extends AwfulRequest<Void> {
    public EmoteRequest(Context context) {
        super(context, Constants.FUNCTION_MISC);
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        urlBuilder.appendQueryParameter(Constants.PARAM_ACTION, "showsmilies");
        return urlBuilder.build().toString();
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        ArrayList<ContentValues> emotes = AwfulEmote.parseEmotes(doc);
        int inserted = getContentResolver().bulkInsert(AwfulEmote.CONTENT_URI, emotes.toArray(new ContentValues[emotes.size()]));
        if(inserted < 0){
            throw new AwfulError();
        }
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
