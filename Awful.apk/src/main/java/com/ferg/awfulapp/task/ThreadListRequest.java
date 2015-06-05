package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

/**
 * Created by matt on 8/7/13.
 */
public class ThreadListRequest extends AwfulRequest<Void> {

    public static final Object REQUEST_TAG = new Object();

    private int forumId, page;
    public ThreadListRequest(Context context, int forumId, int page) {
        super(context, forumId == Constants.USERCP_ID ? Constants.FUNCTION_BOOKMARK : Constants.FUNCTION_FORUM);
        this.forumId = forumId;
        this.page = page;
    }


    @Override
    public Object getRequestTag() {
        return REQUEST_TAG;
    }


    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        if(forumId != Constants.USERCP_ID){
            urlBuilder.appendQueryParameter(Constants.PARAM_FORUM_ID, Integer.toString(forumId));
        }
        urlBuilder.appendQueryParameter(Constants.PARAM_PAGE, Integer.toString(page));
        return urlBuilder.build().toString();
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        try {
            if(forumId == Constants.USERCP_ID){
                AwfulForum.parseUCPThreads(doc, page, getContentResolver());
            }else{
                AwfulForum.parseThreads(doc, forumId, page, getContentResolver());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new AwfulError();
        }
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
