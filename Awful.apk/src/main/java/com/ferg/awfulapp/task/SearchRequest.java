package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulSearch;
import com.ferg.awfulapp.thread.AwfulSearchResult;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

/**
 * Created by matt on 8/8/13.
 */
public class SearchRequest extends AwfulRequest<AwfulSearchResult> {
    public SearchRequest(Context context, String query, int[] forums) {
        super(context, null);
        addPostParam(Constants.PARAM_ACTION, Constants.ACTION_QUERY);
        addPostParam(Constants.PARAM_QUERY, query);
        if(forums !=null) {
            for (int i = 0; i < forums.length; i++) {
                addPostParam(String.format(Constants.PARAM_FORUMS, i), String.valueOf(forums[i]));
            }
        }
        buildFinalRequest();
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        return Constants.FUNCTION_SEARCH;
    }

    @Override
    protected AwfulSearchResult handleResponse(Document doc) throws AwfulError {
        AwfulSearchResult result = AwfulSearchResult.parseSearch(doc);
                result.setResultList(AwfulSearch.parseSearchResult(doc));
        return result;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
