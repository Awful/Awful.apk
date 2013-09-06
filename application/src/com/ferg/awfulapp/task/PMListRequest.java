package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.util.AwfulError;
import org.jsoup.nodes.Document;

/**
 * Created by matt on 8/8/13.
 */
public class PMListRequest extends AwfulRequest<Void> {
	
	int folder = 0;
	
    public PMListRequest(Context context, int folder) {
        super(context, null);
        this.folder = folder;
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        return Constants.FUNCTION_PRIVATE_MESSAGE+"?"+Constants.PARAM_FOLDERID+"="+folder;
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        AwfulMessage.processMessageList(getContentResolver(), doc, folder);
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
