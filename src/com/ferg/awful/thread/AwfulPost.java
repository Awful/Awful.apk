package com.ferg.awful.thread;

import android.util.Log;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

public class AwfulPost {
    private static final String TAG = "AwfulPost";

    private static final String USERNAME_SEARCH = "//dt[@class='author']|//dt[@class='author op']|//dt[@class='author role-mod']|//dt[@class='author role-admin']|//dt[@class='author role-mod op']|//dt[@class='author role-admin op']";
    private static final String MOD_SEARCH      = "//dt[@class='author role-mod']|//dt[@class='author role-mod op']";
    private static final String ADMIN_SEARCH    = "//dt[@class='author role-admin']|//dt[@class='author role-admin op']";

    private static final String USERNAME  = "//dt[@class='author']";
    private static final String POST      = "//table[@class='post']";
    private static final String POST_ID   = "//table[@class='post']";
    private static final String POST_DATE = "//td[@class='postdate']";
    private static final String SEEN_LINK = "//td[@class='postdate']//a[@title='Mark thread seen up to this post']";
    private static final String SEEN      = "//tr[@class='seen1']|//tr[@class='seen2']";
    private static final String AVATAR    = "//dd[@class='title']//img";
    private static final String EDITED    = "//p[@class='editedby']/span";
    private static final String POSTBODY  = "//td[@class='postbody']";

    private static final String ELEMENT_POSTBODY     = "<td class=\"postbody\">";
    private static final String ELEMENT_END_TD       = "</td>";
    private static final String REPLACEMENT_POSTBODY = "<div class=\"postbody\">";
    private static final String REPLACEMENT_END_TD   = "</div>";

    private String mId;
    private String mDate;
    private String mUsername;
    private String mAvatar;
    private String mContent;
    private String mEdited;

    public String getId() {
        return mId;
    }

    public void setId(String aId) {
        mId = aId;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String aDate) {
        mDate = aDate;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String aUsername) {
        mUsername = aUsername;
    }

    public String getAvatar() {
        return mAvatar;
    }

    public void setAvatar(String aAvatar) {
        mAvatar = aAvatar;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String aContent) {
        mContent = aContent;
    }

    public String getEdited() {
        return mEdited;
    }

    public void setEdited(String aEdited) {
        mEdited = aEdited;
    }

    public static ArrayList<AwfulPost> parsePosts(TagNode aThread) {
        ArrayList<AwfulPost> result = new ArrayList<AwfulPost>();
        HtmlCleaner cleaner = new HtmlCleaner();
        CleanerProperties properties = cleaner.getProperties();
        properties.setOmitComments(true);

        try {
            Object[] postNodes = aThread.evaluateXPath(POST);

            for (Object current : postNodes) {
                AwfulPost post = new AwfulPost();
                TagNode node = (TagNode) current;

                Object[] postId = node.evaluateXPath(POST_ID);
                if (postId.length > 0) {
                    post.setId(((TagNode) postId[0]).getText().toString());
                }

                Object[] postDate = node.evaluateXPath(POST_DATE);
                if (postDate.length > 0) {
                    TagNode dateNode = (TagNode) postDate[0];

                    // There's got to be a better way to do this
                    dateNode.removeChild(dateNode.findElementHavingAttribute("href", false));
                    dateNode.removeChild(dateNode.findElementHavingAttribute("href", false));
                    dateNode.removeChild(dateNode.findElementHavingAttribute("href", false));

                    post.setDate(dateNode.getText().toString().trim());
                }

                Object[] postUsername = node.evaluateXPath(USERNAME);
                if (postUsername.length > 0) {
                    post.setUsername(((TagNode) postUsername[0]).getText().toString());
                }

                Object[] postAvatar = node.evaluateXPath(AVATAR);
                if (postAvatar.length > 0) {
                    post.setAvatar(((TagNode) postAvatar[0]).getAttributeByName("src"));
                }

                Object[] postEdited = node.evaluateXPath(EDITED);
                if (postEdited.length > 0) {
                    post.setEdited(((TagNode) postEdited[0]).getText().toString());
                }

                Object[] postBody = node.evaluateXPath(POSTBODY);
                if (postBody.length > 0) {
                    SimpleHtmlSerializer serializer = 
                        new SimpleHtmlSerializer(cleaner.getProperties());

                    post.setContent(serializer.getAsString((TagNode) postBody[0]));
                }

                result.add(post);
            }

            Log.i(TAG, Integer.toString(postNodes.length));
        } catch (XPatherException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private static String createPostHtml(String aHtml) {
        aHtml = aHtml.replaceAll(ELEMENT_POSTBODY, REPLACEMENT_POSTBODY);
        aHtml = aHtml.replaceAll(ELEMENT_END_TD, REPLACEMENT_END_TD);

        return aHtml;
    }
}
