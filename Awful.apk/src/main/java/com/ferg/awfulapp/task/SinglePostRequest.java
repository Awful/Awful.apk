package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulThread;
import com.ferg.awfulapp.util.AwfulError;
import com.ferg.awfulapp.util.AwfulUtils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Created by matt on 8/7/13.
 */
public class SinglePostRequest extends AwfulRequest<String> {

    public static final Object REQUEST_TAG = new Object();

    private String postId;
    public SinglePostRequest(Context context, String postId) {
        super(context, Constants.FUNCTION_THREAD);
        this.postId = postId;
    }


    @Override
    public Object getRequestTag() {
        return REQUEST_TAG;
    }


    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        urlBuilder.appendQueryParameter(Constants.PARAM_ACTION, Constants.ACTION_SHOWPOST);
        urlBuilder.appendQueryParameter(Constants.PARAM_POST_ID, this.postId);
        return urlBuilder.build().toString();
    }

    @Override
    protected String handleResponse(Document doc) throws AwfulError {
        Element postbody = doc.getElementsByClass("postbody").first();
        return postbody.html();
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
