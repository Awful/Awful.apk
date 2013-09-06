package com.ferg.awfulapp.task;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

/**
 * Created by Matthew on 8/9/13.
 */
public class MarkUnreadRequest extends AwfulRequest<Void> {
    private int threadId;
    public MarkUnreadRequest(Context context, int threadId) {
        super(context, null);
        addPostParam(Constants.PARAM_THREAD_ID, Integer.toString(threadId));
        addPostParam(Constants.PARAM_ACTION, "resetseen");
        this.threadId = threadId;
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        //since we aren't adding query arguments to a POST request,
        //we can just pass null in the constructor URL field and it'll skip this Uri.Builder
        return Constants.FUNCTION_THREAD;
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        ContentValues last_read = new ContentValues();
        last_read.put(AwfulPost.PREVIOUSLY_READ, 0);
        getContentResolver().update(AwfulPost.CONTENT_URI, last_read, AwfulPost.THREAD_ID+"=?", new String[]{Integer.toString(threadId)});
        ContentValues unread = new ContentValues();
        unread.put(AwfulThread.UNREADCOUNT, 0);
        unread.put(AwfulThread.HAS_VIEWED_THREAD, 0);
        getContentResolver().update(ContentUris.withAppendedId(AwfulThread.CONTENT_URI, threadId), unread, null, null);
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }

    @Override
    protected VolleyError customizeAlert(VolleyError error) {
        return new AwfulError("Failed to mark unread!");
    }
}
