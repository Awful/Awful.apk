package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulSearch;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

import java.util.ArrayList;

/**
 * Created by matt on 8/8/13.
 */
public class SearchResultRequest extends AwfulRequest<ArrayList<AwfulSearch>> {

    int mQuery, mPage;

    public SearchResultRequest(Context context, int query, int page) {
        super(context, Constants.FUNCTION_SEARCH);
        mQuery = query;
        mPage = page;

        buildFinalRequest();
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        urlBuilder.appendQueryParameter(Constants.PARAM_ACTION, Constants.ACTION_RESULTS);
        urlBuilder.appendQueryParameter(Constants.PARAM_QID, String.valueOf(mQuery));
        urlBuilder.appendQueryParameter(Constants.PAGE, String.valueOf(mPage));
        return urlBuilder.build().toString();
    }

    @Override
    protected ArrayList<AwfulSearch> handleResponse(Document doc) throws AwfulError {
        return AwfulSearch.parseSearchResult(doc);
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.getErrorCode() == AwfulError.ERROR_PROBATION || error.isCritical();//Don't allow probation to pass
    }
}
