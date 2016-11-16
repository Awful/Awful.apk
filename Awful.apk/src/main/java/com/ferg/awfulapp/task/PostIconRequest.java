package com.ferg.awfulapp.task;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPostIcon;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

import java.util.ArrayList;

/**
 * Created by matt on 8/8/13.
 */
public class PostIconRequest extends AwfulRequest<ArrayList<AwfulPostIcon>> {
    private int forumid = 0;
    private Constants.POSTICON_REQUEST_TYPES type;
    public PostIconRequest(Context context, Constants.POSTICON_REQUEST_TYPES type, int forumid) {
        super(context, (type == Constants.POSTICON_REQUEST_TYPES.PM ? Constants.FUNCTION_PRIVATE_MESSAGE: Constants.FUNCTION_NEW_THREAD));
        this.forumid = forumid;
        this.type = type;
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        if(type == Constants.POSTICON_REQUEST_TYPES.FORUM_POST && forumid != 0){
            urlBuilder.appendQueryParameter(Constants.PARAM_ACTION, Constants.ACTION_NEW_THREAD);
            urlBuilder.appendQueryParameter(Constants.PARAM_FORUM_ID, String.valueOf(forumid));
        }else{
            urlBuilder.appendQueryParameter(Constants.PARAM_ACTION, Constants.ACTION_NEW_MESSAGE);
        }
        return urlBuilder.build().toString();
    }

    @Override
    protected ArrayList<AwfulPostIcon> handleResponse(Document doc) throws AwfulError {
        ArrayList<AwfulPostIcon> icons = AwfulPostIcon.parsePostIcons(doc.getElementsByClass("posticon"));
        return icons;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
