package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulSearch;
import com.ferg.awfulapp.thread.AwfulSearchForum;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

import java.util.ArrayList;

/**
 * Created by matt on 8/8/13.
 */
public class SearchForumsRequest extends AwfulRequest<ArrayList<AwfulSearchForum>> {
    public SearchForumsRequest(Context context) {
        super(context, null);
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        return Constants.FUNCTION_SEARCH;
    }

    @Override
    protected ArrayList<AwfulSearchForum> handleResponse(Document doc) throws AwfulError {
        return AwfulSearchForum.parseSearchForums(doc);
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.getErrorCode() == AwfulError.ERROR_PROBATION || error.isCritical();//Don't allow probation to pass
    }
}
