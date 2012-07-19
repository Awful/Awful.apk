package com.ferg.awfulapp.user;

import java.util.HashMap;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;

public class Profile {
    public static final String TAG = "Profile";

    private static final String USERNAME   = "//dt[@class='author']";
    private static final String REGISTERED = "//dd[@class='registered']";
    private static final String AVATAR     = "//dd[@class='title']//img";
    private static final String INFO       = "//td[@class='info']";

    private String mUsername;
    private String mRegistered;
    private String mAvatar;
    private String mInfo;

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String aUsername) {
        mUsername = aUsername;
    }

    public String getRegistered() {
        return mRegistered;
    }

    public void setRegistered(String aRegistered) {
        mRegistered = aRegistered;
    }

    public String getAvatar() {
        return mAvatar;
    }

    public void setAvatar(String aAvatar) {
        mAvatar = aAvatar;
    }

    public String getInfo() {
        return mInfo;
    }

    public void setInfo(String aInfo) {
        mInfo = aInfo;
    }

    public static Profile withId(String aUserId) {
		long time = System.currentTimeMillis();
        Profile result = new Profile();

        HashMap<String, String> params = new HashMap<String, String>();
        params.put(Constants.PARAM_ACTION, "getinfo");
        params.put(Constants.PARAM_USER_ID, aUserId);

        HtmlCleaner cleaner = new HtmlCleaner();
        CleanerProperties properties = cleaner.getProperties();
        properties.setOmitComments(true);

        try {
            TagNode response = NetworkUtils.get(Constants.FUNCTION_MEMBER, params);

            Object[] nodeList = response.evaluateXPath(USERNAME);
            if (nodeList.length > 0) {
                result.setUsername(((TagNode) nodeList[0]).getText().toString().trim());
            }

            nodeList = response.evaluateXPath(REGISTERED);
            if (nodeList.length > 0) {
                result.setRegistered(((TagNode) nodeList[0]).getText().toString().trim());
            }

            nodeList = response.evaluateXPath(AVATAR);
            if (nodeList.length > 0) {
                result.setRegistered(((TagNode) nodeList[0]).getAttributeByName("src"));
            }

            nodeList = response.evaluateXPath(INFO);
            if (nodeList.length > 0) {
                SimpleHtmlSerializer serializer = 
                    new SimpleHtmlSerializer(cleaner.getProperties());

                result.setInfo(serializer.getAsString((TagNode) nodeList[0]));
            }
        } catch (XPatherException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e(TAG, "Process Time: "+ (System.currentTimeMillis() - time));
        return result;
    }
}
