package com.ferg.awfulapp.task;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

/**
 * Created by Matthew on 8/9/13.
 */
public class BookmarkColorRequest extends AwfulRequest<Void> {
    private int threadId, postIndex;
    public BookmarkColorRequest(Context context, int threadId) {
        super(context, null);
        this.threadId = threadId;
        addPostParam(Constants.PARAM_ACTION, "cat_toggle");
        addPostParam(Constants.PARAM_THREAD_ID, Integer.toString(threadId));
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        //since we aren't adding query arguments to a POST request,
        //we can just pass null in the constructor URL field and it'll skip this Uri.Builder
        return Constants.FUNCTION_BOOKMARK;
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        //nothing to do
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }

}
