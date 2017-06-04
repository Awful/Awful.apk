package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

/**
 * Created by Matthew on 8/8/13.
 */
public class IgnoreRequest extends AwfulRequest<Void> {
    public IgnoreRequest(Context context, int userId) {
        super(context, null);//member2? heh, ~radium~
        addPostParam(Constants.PARAM_ACTION, Constants.ACTION_ADDLIST);
        addPostParam(Constants.PARAM_USERLIST, Constants.USERLIST_IGNORE);
        addPostParam(Constants.FORMKEY, getPreferences().ignoreFormkey);
        addPostParam(Constants.PARAM_USER_ID, Integer.toString(userId));
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        //since we aren't adding query arguments to a POST request,
        //we can just pass null in the constructor URL field and it'll skip this Uri.Builder
        return Constants.FUNCTION_MEMBER2;
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
}
