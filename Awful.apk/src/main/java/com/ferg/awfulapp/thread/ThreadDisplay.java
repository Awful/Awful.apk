package com.ferg.awfulapp.thread;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.widget.Toast;

import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulTheme;
import com.ferg.awfulapp.util.AwfulUtils;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;
import com.samskivert.mustache.Template;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

/**
 * Created by baka kaba on 04/06/2017.
 * <p>
 * Contains methods used to construct an actual thread page.
 * Extracted from {@link AwfulThread}
 */
public abstract class ThreadDisplay {

    // TODO: 16/08/2017 generate this automatically from the folder contents
    /**
     * All the scripts from the javascript folder used in generating HTML
     */
    static final String[] JS_FILES = {
            "polyfills.js",
            "twitterwidget.js",
            "longtap.js",
            "jsonp.js",
            "embedding.js",
            "thread.js"
    };

    /**
     * Get the main HTML for the containing page.
     * <p>
     * This contains no post data, but sets up the basic template with the required JS scripts,
     * CSS etc., and a container element to insert post data into.
     *
     * @param aPrefs  used to customise the template to the user's preferences
     * @param forumId the ID of the forum this thread belongs to, used for themeing
     * @return the template containing a container class
     */
    public static String getContainerHtml(AwfulPreferences aPrefs, int forumId) {
        StringBuilder buffer = new StringBuilder("<!DOCTYPE html>\n<html>\n<head>\n");
        buffer.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0 maximum-scale=1.0 minimum-scale=1.0, user-scalable=no\" />\n");
        buffer.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n");
        buffer.append("<meta name='format-detection' content='telephone=no' />\n");
        buffer.append("<meta name='format-detection' content='address=no' />\n");


        // build the theme css tag, using the appropriate css path
        // the dark-theme attribute can be used to e.g. embed a dark or light widget
        AwfulTheme theme = AwfulTheme.forForum(forumId);
        buffer.append(String.format("<link id='theme-css' rel='stylesheet' data-dark-theme='%b' href='%s'>\n", theme.isDark(), theme.getCssPath()));
        buffer.append("<link rel='stylesheet' href='file:///android_asset/css/general.css' />");


        if (!aPrefs.preferredFont.contains("default")) {
            buffer.append("<style id='font-face' type='text/css'>@font-face { font-family: userselected; src: url('content://com.ferg.awfulapp.webprovider/").append(aPrefs.preferredFont).append("'); }</style>\n");
        }
        for (String scriptName : JS_FILES) {
            buffer.append("<script src='file:///android_asset/javascript/")
                    .append(scriptName)
                    .append("' type='text/javascript'></script>\n");
        }

        buffer.append("</head><body><div id='container' class='container' ")
                .append((!aPrefs.noFAB ? "style='padding-bottom:75px'" : ""))
                .append("></div><script type='text/javascript'>containerInit();</script></body></html>");
        return buffer.toString();
    }


    /**
     * Generates post content HTML for a list of posts.
     * <p>
     * This method produces the content that should be inserted into the container template that
     * {@link #getContainerHtml(AwfulPreferences, int)} produces.
     *
     * @param aPosts   the list of posts to generate HTML for
     * @param aPrefs   used to customise the post content to the user's preferences
     * @param page     the number of the page this represents
     * @param lastPage the number of the last page in this thread
     * @return the generated content, ready for insertion into the template
     */
    public static String getHtml(List<AwfulPost> aPosts, AwfulPreferences aPrefs, int page, int lastPage) {
        StringBuilder buffer = new StringBuilder(1024);
        buffer.append("<div class='content'>\n");

        // if we're hiding read posts, work out how many are read and add the 'show old posts' link
        if (aPrefs.hideOldPosts && aPosts.size() > 0 && !aPosts.get(aPosts.size() - 1).isPreviouslyRead()) {
            int unreadCount = 0;
            for (AwfulPost ap : aPosts) {
                if (!ap.isPreviouslyRead()) {
                    unreadCount++;
                }
            }
            if (unreadCount < aPosts.size() && unreadCount > 0) {
                buffer.append("    <article class='toggleread'>");
                buffer.append("      <a>\n");
                final int prevPosts = aPosts.size() - unreadCount;
                buffer.append("        <h3>Show ")
                        .append(prevPosts).append(" Previous Post").append(prevPosts > 1 ? "s" : "").append("</h3>\n");
                buffer.append("      </a>\n");
                buffer.append("    </article>");
            }
        }

        // add the actual posts
        buffer.append(getPostsHtml(aPosts, aPrefs));

        if (page == lastPage) {
            buffer.append("<div class='unread' ></div>\n");
        }
        buffer.append("</div>\n");

        return buffer.toString();
    }


    /**
     * Generates HTML for a list of posts using the appropriate Mustache layout.
     * <p>
     * This method generates HTML for the actual posts, taking user preferences into account.
     *
     * @return a HTML string representing all the posts
     */
    private static String getPostsHtml(List<AwfulPost> aPosts, AwfulPreferences aPrefs) {
        StringBuilder buffer = new StringBuilder();
        Template postTemplate;

        try {
            postTemplate = getPostTemplate(aPrefs);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

        // should be fine to re-use this since we rewrite every mapping each time
        Map<String, String> postData = new ArrayMap<>();
        postData.put("notOnProbation", (aPrefs.isOnProbation()) ? null : "notOnProbation");

        // run each post's data through the template, and combine into a final HTML string
        for (AwfulPost post : aPosts) {
            String username = post.getUsername();
            String avatar = post.getAvatar();

            postData.put("seen", post.isPreviouslyRead() ? "read" : "unread");
            postData.put("isOP", (aPrefs.highlightOP && post.isOp()) ? "op" : null);
            postData.put("isMarked", aPrefs.markedUsers.contains(username) ? "marked" : null);
            postData.put("postID", post.getId());
            postData.put("isSelf", (aPrefs.highlightSelf && username.equals(aPrefs.username)) ? "self" : null);
            postData.put("avatarURL", (aPrefs.canLoadAvatars() && avatar != null && avatar.length() > 0) ? avatar : null);
            postData.put("username", username);
            postData.put("userID", post.getUserId());
            postData.put("postDate", post.getDate());
            postData.put("regDate", post.getRegDate());
            postData.put("mod", post.isMod() ? "mod" : null);
            postData.put("admin", post.isAdmin() ? "admin" : null);
            postData.put("plat", post.isPlat() ? "plat" : null);
            postData.put("avatarText", "" + post.getAvatarText());
            postData.put("lastReadUrl", post.getLastReadUrl());
            postData.put("editable", post.isEditable() ? "editable" : null);
            postData.put("postcontent", post.getContent());

            try {
                buffer.append(postTemplate.execute(postData));
            } catch (MustacheException e) {
                e.printStackTrace();
            }
        }
        return buffer.toString();
    }

    /**
     * Get a Mustache template for posts, according to the user's preferences.
     * <p>
     * Falls back to the default template if a custom layout can't be accessed.
     *
     * @param aPrefs used to check if a custom layout is selected
     * @throws IOException if the default template can't be read
     */
    private static Template getPostTemplate(AwfulPreferences aPrefs) throws IOException {
        Template postTemplate;
        Reader templateReader = null;

        // user has a custom template selected (nobody uses this I bet)
        if (!"default".equals(aPrefs.layout)) {
            if (AwfulUtils.isMarshmallow()) {
                int permissionCheck = ContextCompat.checkSelfPermission(aPrefs.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);

                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    File template = new File(Environment.getExternalStorageDirectory() + "/awful/" + aPrefs.layout);
                    if (template.isFile() && template.canRead()) {
                        templateReader = new FileReader(template);
                    }
                } else {
                    Toast.makeText(aPrefs.getContext(), "Can't access custom layout because Awful lacks storage permissions. Reverting to default layout.", Toast.LENGTH_LONG).show();
                }
            } else {
                File template = new File(Environment.getExternalStorageDirectory() + "/awful/" + aPrefs.layout);
                if (template.isFile() && template.canRead()) {
                    templateReader = new FileReader(template);
                }

            }
        }

        // use the default if necessary
        if (templateReader == null) {
            templateReader = new InputStreamReader(aPrefs.getResources().getAssets().open("mustache/post.mustache"));
        }
        postTemplate = Mustache.compiler().compile(templateReader);
        return postTemplate;
    }

}
