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
        return Constants.BASE_URL + "/";
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        AwfulForum.getForumsFromRemote(doc, getContentResolver());
        updateProgress(80);

        //optional section, parses username from PM notification field.
        Elements pmBlock = doc.getElementsByAttributeValue("id", "pm");
        try {
            if (pmBlock.size() > 0) {
                Elements bolded = pmBlock.first().getElementsByTag("b");
                if (bolded.size() > 1) {
                    String name = bolded.first().text().split("'")[0];
                    String unread = bolded.get(1).text();
                    Pattern findUnread = Pattern.compile("(\\d+)\\s+unread");
                    Matcher matchUnread = findUnread.matcher(unread);
                    int unreadCount = -1;
                    if (matchUnread.find()) {
                        unreadCount = Integer.parseInt(matchUnread.group(1));
                    }
                    Log.v("IndexRequest", "text: " + name + " - " + unreadCount);
                    if (name != null && name.length() > 0) {
                        getPreferences().setStringPreference("username", name);
                    }
                }
            }
        } catch (Exception e) {
            //this chunk is optional, no need to fail everything if it doesn't work out.
        }
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
