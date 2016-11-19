package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulPostIcon;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

import java.util.ArrayList;

import static com.ferg.awfulapp.constants.Constants.FUNCTION_NEW_THREAD;
import static com.ferg.awfulapp.constants.Constants.FUNCTION_PRIVATE_MESSAGE;
import static com.ferg.awfulapp.constants.Constants.POST_ICON_REQUEST_TYPES.PM;

/**
 * Created by matt on 8/8/13.
 */
public class PostIconRequest extends AwfulRequest<ArrayList<AwfulPostIcon>> {
    private int forumid = 0;
    private Constants.POST_ICON_REQUEST_TYPES type;
    public PostIconRequest(Context context, Constants.POST_ICON_REQUEST_TYPES type, int forumid) {
        super(context, (type == PM ? FUNCTION_PRIVATE_MESSAGE: FUNCTION_NEW_THREAD));
        this.forumid = forumid;
        this.type = type;
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        if(type == Constants.POST_ICON_REQUEST_TYPES.FORUM_POST && forumid != 0){
            urlBuilder.appendQueryParameter(Constants.PARAM_ACTION, Constants.ACTION_NEW_THREAD);
            urlBuilder.appendQueryParameter(Constants.PARAM_FORUM_ID, String.valueOf(forumid));
        }else{
            urlBuilder.appendQueryParameter(Constants.PARAM_ACTION, Constants.ACTION_NEW_MESSAGE);
        }
        return urlBuilder.build().toString();
    }

    @Override
    protected ArrayList<AwfulPostIcon> handleResponse(Document doc) throws AwfulError {
        return AwfulPostIcon.parsePostIcons(doc.getElementsByClass("posticon"), getContext());
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
