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
public class MarkLastReadRequest extends AwfulRequest<Void> {
    private int threadId, postIndex;
    public MarkLastReadRequest(Context context, int threadId, int postIndex) {
        super(context, null);
        this.threadId = threadId;
        this.postIndex = postIndex;
        addPostParam(Constants.PARAM_ACTION, "setseen");
        addPostParam(Constants.PARAM_THREAD_ID, Integer.toString(threadId));
        addPostParam(Constants.PARAM_INDEX, Integer.toString(postIndex));
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        //since we aren't adding query arguments to a POST request,
        //we can just pass null in the constructor URL field and it'll skip this Uri.Builder
        return Constants.FUNCTION_THREAD;
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        //set unread posts (> unreadIndex)
        ContentValues last_read = new ContentValues();
        last_read.put(AwfulPost.PREVIOUSLY_READ, 0);
        ContentResolver resolv = getContentResolver();
        resolv.update(AwfulPost.CONTENT_URI,
                last_read,
                AwfulPost.THREAD_ID+"=? AND "+AwfulPost.POST_INDEX+">?",
                AwfulProvider.int2StrArray(threadId, postIndex));

        //set previously read posts (< unreadIndex)
        last_read.put(AwfulPost.PREVIOUSLY_READ, 1);
        resolv.update(AwfulPost.CONTENT_URI,
                last_read,
                AwfulPost.THREAD_ID+"=? AND "+AwfulPost.POST_INDEX+"<=?",
                AwfulProvider.int2StrArray(threadId, postIndex));

        //update unread count
        Cursor threadData = resolv.query(ContentUris.withAppendedId(AwfulThread.CONTENT_URI, threadId), AwfulProvider.ThreadProjection, null, null, null);
        if(threadData != null && threadData.moveToFirst()){
            ContentValues thread_update = new ContentValues();
            thread_update.put(AwfulThread.UNREADCOUNT, threadData.getInt(threadData.getColumnIndex(AwfulThread.POSTCOUNT)) - postIndex);
            resolv.update(AwfulThread.CONTENT_URI,
                    thread_update,
                    AwfulThread.ID + "=?",
                    AwfulProvider.int2StrArray(threadId));
        }
        if (threadData != null) {
            threadData.close();
        }
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }

    @Override
    protected VolleyError customizeAlert(VolleyError error) {
        return new AwfulError("Failed to mark post!");
    }
}
