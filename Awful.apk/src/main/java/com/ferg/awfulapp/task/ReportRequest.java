package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Created by matt on 8/8/13.
 */
public class ReportRequest extends AwfulRequest<String> {
    private int postId;
    private String mComments;
    public ReportRequest(Context context, int postId, String mComments) {
        super(context, null);
        this.mComments = mComments;
        this.postId = postId;
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        addPostParam(Constants.PARAM_COMMENTS, mComments);
        addPostParam(Constants.PARAM_POST_ID, Integer.toString(postId));
        addPostParam(Constants.PARAM_ACTION, "submit");

        return Constants.FUNCTION_REPORT;
    }

    @Override
    protected String handleResponse(Document doc) throws AwfulError {

		if(doc.getElementById("content") != null){
			Element standard = doc.getElementsByClass("standard").first();
			if(standard != null && standard.hasText()){
				if(standard.text().contains("Thank you, but this thread has already been reported recently!")){
					throw new AwfulError("Someone has already reported this thread recently");
					
				}else if(standard.text().contains("Your alert has been submitted to the Moderators.")){
					return "Your alert has been submitted to the Moderators."; //"Thank you for your report";
				}
			}
			throw new AwfulError("An error occurred while trying to process your report");
		}
		throw new AwfulError("An error occurred while trying to send your report");
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
