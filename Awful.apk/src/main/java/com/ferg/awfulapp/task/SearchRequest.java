package com.ferg.awfulapp.task;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulSearch;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by matt on 8/8/13.
 */
public class SearchRequest extends AwfulRequest<ArrayList<AwfulSearch>> {
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
    protected ArrayList<AwfulSearch> handleResponse(Document doc) throws AwfulError {
        return AwfulSearch.parseSearch(doc);
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.getErrorCode() == AwfulError.ERROR_PROBATION || error.isCritical();//Don't allow probation to pass
    }
}
