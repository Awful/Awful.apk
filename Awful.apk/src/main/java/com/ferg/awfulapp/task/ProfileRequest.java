package com.ferg.awfulapp.task;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by matt on 8/8/13.
 */
public class ProfileRequest extends AwfulRequest<Void> {
    private String username;
    public ProfileRequest(Context context, String username) {
        super(context, Constants.FUNCTION_MEMBER);
        this.username = username;
    }

    public ProfileRequest(Context context) {
        this(context,null);
    }

    @Override
    protected String generateUrl(Uri.Builder urlBuilder) {
        urlBuilder.appendQueryParameter(Constants.PARAM_ACTION, Constants.ACTION_PROFILE);
        urlBuilder.appendQueryParameter(Constants.PARAM_USERNAME, (TextUtils.isEmpty(username) || username.equals("0")) ? getPreferences().username : username);
        return urlBuilder.build().toString();
    }

    @Override
    protected Void handleResponse(Document doc) throws AwfulError {
        Element formkey = doc.getElementsByAttributeValue("name", "formkey").first();
        if (formkey != null) {
            try {
                getPreferences().setStringPreference("ignore_formkey", formkey.val());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            throw new AwfulError("Profile page did not load");
        }
        Element title = doc.getElementsByClass("title").first();
        if(null != title){
            String userTitle = "";
            Elements titleTags = title.getAllElements();
            for(Element tag : titleTags){
                String tagName = tag.tagName();
                if("br".equals(tagName)){
                    break;
                }else if("img".equals(tagName)){
                    userTitle = tag.attr("src");
                    break;
                }
            }
            try {
                getPreferences().setStringPreference("user_title", userTitle);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected boolean handleError(AwfulError error, Document doc) {
        return error.isCritical();
    }
}
