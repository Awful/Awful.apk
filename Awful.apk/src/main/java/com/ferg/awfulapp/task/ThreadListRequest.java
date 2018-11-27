package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.ferg.awfulapp.announcements.AnnouncementsManager;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.messages.PmManager;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.thread.AwfulPagedItem;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

/**
 * Created by matt on 8/7/13.
 */
public class ThreadListRequest extends AwfulStrippedRequest<Void> {

    public static final Object REQUEST_TAG = new Object();

    private int forumId, page;

    public ThreadListRequest(Context context, int forumId, int page) {
        // TODO: 19/09/2016 decide whether to handle all USERCP requests as bookmark urls (and do the PmManager calls a different way)
        // I'm so sorry, thanks for forcing me to do this JAVA
        super(context, forumId != Constants.USERCP_ID ? Constants.FUNCTION_FORUM
                : page == 1 ? Constants.FUNCTION_USERCP
                : Constants.FUNCTION_BOOKMARK);
        this.forumId = forumId;
        this.page = page;
    }


    @Override
    public Object getRequestTag() {
        return REQUEST_TAG;
    }


    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        if (forumId != Constants.USERCP_ID) {
            urlBuilder.appendQueryParameter(Constants.PARAM_FORUM_ID, Integer.toString(forumId));
        }
        urlBuilder.appendQueryParameter(Constants.PARAM_PAGE, Integer.toString(page));
        return urlBuilder.build().toString();
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        int lastPage = AwfulPagedItem.parseLastPage(doc);
        handleStrippedResponse(doc, page, lastPage);
        return null;
    }

    @Override
    Void handleStrippedResponse(Document doc, @Nullable Integer currentPage, @Nullable Integer totalPages) throws AwfulError {
        int thisPage = (currentPage == null) ? page : currentPage;
        int lastPage = (totalPages == null) ? thisPage : totalPages;
        // TODO: legacy try/catch - work out what this is meant to be catching exactly, and if we can ditch it
        try {
            if (forumId == Constants.USERCP_ID) {
                AwfulForum.parseUCPThreads(doc, page, lastPage, getContentResolver());
                PmManager.parseUcpPage(doc);
            } else {
                AwfulForum.parseThreads(forumId, page, lastPage, doc, getContentResolver());
                AnnouncementsManager.getInstance().parseForumPage(doc);
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
