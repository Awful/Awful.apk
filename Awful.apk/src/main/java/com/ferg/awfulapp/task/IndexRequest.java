package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by matt on 8/7/13.
 */
public class IndexRequest extends AwfulRequest<Void> {

    public static final Object REQUEST_TAG = new Object();

    public IndexRequest(Context context) {
        super(context, null);
    }


    @Override
    public Object getRequestTag() {
        return REQUEST_TAG;
    }

    @Override
    protected String generateUrl(Uri.Builder build) {
        return Constants.FUNCTION_FORUM + "?" + Constants.PARAM_FORUM_ID + "=" + Constants.FORUM_ID_GOLDMINE;
    }
    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        AwfulForum.processForums(doc, getContentResolver());
        updateProgress(80);
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
