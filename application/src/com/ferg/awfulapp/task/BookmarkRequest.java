package com.ferg.awfulapp.task;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.thread.AwfulThread;
import com.ferg.awfulapp.util.AwfulError;
import org.jsoup.nodes.Document;

/**
 * Created by matt on 8/8/13.
 */
public class BookmarkRequest extends AwfulRequest<Void> {
    private int threadId;
    private boolean add;
    public BookmarkRequest(Context context, int threadId, boolean add) {
        super(context, null);
        this.add = add;
        this.threadId = threadId;
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        addPostParam(Constants.PARAM_THREAD_ID, Integer.toString(threadId));
        if(add){
            addPostParam(Constants.PARAM_ACTION, "add");
        }else{
            addPostParam(Constants.PARAM_ACTION, "remove");
        }
        return Constants.FUNCTION_BOOKMARK;
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        ContentValues cv = new ContentValues();
        cv.put(AwfulThread.BOOKMARKED, add?1:0);
        getContentResolver().update(AwfulThread.CONTENT_URI, cv, AwfulThread.ID+"=?", AwfulProvider.int2StrArray(threadId));
        if(!add){
            getContentResolver().delete(AwfulThread.CONTENT_URI_UCP, AwfulThread.ID+"=?", AwfulProvider.int2StrArray(threadId));
        }
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
