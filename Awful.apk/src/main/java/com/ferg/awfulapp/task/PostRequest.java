package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulThread;
import com.ferg.awfulapp.util.AwfulError;
import com.ferg.awfulapp.util.AwfulUtils;
import org.jsoup.nodes.Document;

/**
 * Created by matt on 8/7/13.
 */
public class PostRequest extends AwfulRequest<Integer> {
    private int threadId, page, userId;
    public PostRequest(Context context, int threadId, int page, int userId) {
        super(context, Constants.FUNCTION_THREAD);
        this.threadId = threadId;
        this.page = page;
        this.userId = userId;
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        urlBuilder.appendQueryParameter(Constants.PARAM_THREAD_ID, Integer.toString(threadId));
        urlBuilder.appendQueryParameter(Constants.PARAM_PER_PAGE, Integer.toString(getPreferences().postPerPage));
        urlBuilder.appendQueryParameter(Constants.PARAM_PAGE, Integer.toString(page));
        if(userId > 0){
            urlBuilder.appendQueryParameter(Constants.PARAM_USER_ID, Integer.toString(userId));
        }
        return urlBuilder.build().toString();
    }

    @Override
    protected Integer handleResponse(Document doc) throws AwfulError {
        AwfulUtils.trimDbEntries(getContentResolver());
        AwfulThread.getThreadPosts(getContentResolver(), doc, threadId, page, getPreferences().postPerPage, getPreferences(), userId);
        return page;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
