package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;

import com.android.volley.VolleyError;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

/**
 * Created by Matthew on 8/9/13.
 */
public class VoteRequest extends AwfulRequest<Void> {
    public VoteRequest(Context context, int threadId, int vote) {
        super(context, null);
        addPostParam(Constants.PARAM_THREAD_ID, String.valueOf(threadId));
        addPostParam(Constants.PARAM_VOTE, String.valueOf(vote + 1));
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        //since we aren't adding query arguments to a POST request,
        //we can just pass null in the constructor URL field and it'll skip this Uri.Builder
        return Constants.FUNCTION_RATE_THREAD;
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        //nothin a doin' here, if we fail we'll see in the failed callback
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }

    @Override
    protected VolleyError customizeAlert(VolleyError error) {
        return new AwfulError(getContext().getString(R.string.vote_failed));
    }
}
