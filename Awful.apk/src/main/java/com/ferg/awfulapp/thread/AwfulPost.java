/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the software nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.ferg.awfulapp.thread;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class AwfulPost {
    private static final String TAG = "AwfulPost";

    public static final String PATH     = "/post";
    public static final Uri CONTENT_URI = Uri.parse("content://" + Constants.AUTHORITY + PATH);

    private static final Pattern fixCharacters_regex = Pattern.compile("([\\r\\f])");
	private static final Pattern youtubeId_regex = Pattern.compile("/v/([\\w_-]+)&?");
	private static final Pattern youtubeHDId_regex = Pattern.compile("/embed/([\\w_-]+)&?");
    private static final Pattern tiktokId_regex = Pattern.compile("([\\d]+)$");
    private static final Pattern imgurId_regex = Pattern.compile("^(.*\\.imgur\\.com/)([\\w]+)(\\..*)$");
	private static final Pattern vimeoId_regex = Pattern.compile("clip_id=(\\d+)&?");
    private static final Pattern userid_regex = Pattern.compile("userid=(\\d+)");

    private static final List<String> HTTPS_SUPPORTED_DOMAINS =
            Collections.unmodifiableList(Arrays.asList("imgur.com", "somethingawful.com", "giphy.com"));

    public static final String ID                    = "_id";
    public static final String POST_INDEX            = "post_index";
    public static final String THREAD_ID             = "thread_id";
    public static final String DATE                  = "date";
	public static final String REGDATE				 = "regdate";
    public static final String USER_ID               = "user_id";
    public static final String USERNAME              = "username";
    // 2022/09/21 - TODO: the disadvantage of storing this with a post is that it's not automatically refreshed if a user is unblocked
    public static final String IS_IGNORED            = "is_ignored";
    public static final String PREVIOUSLY_READ       = "previously_read";
    public static final String EDITABLE              = "editable";
    public static final String IS_OP                 = "is_op";
    public static final String IS_PLAT               = "is_plat";
    public static final String ROLE                  = "role";
    public static final String AVATAR                = "avatar";
    // people may be using gangtags, etc. as avatars with a 1x1 primary av
    public static final String AVATAR_SECOND         = "avatar_second";
	public static final String AVATAR_TEXT 			 = "avatar_text";
    public static final String CONTENT               = "content";
    public static final String EDITED                = "edited";

	public static final String FORM_KEY = "form_key";
	public static final String FORM_COOKIE = "form_cookie";
    public static final String FORM_BOOKMARK = "bookmark";
    public static final String FORM_SIGNATURE = "signature";
    public static final String FORM_DISABLE_SMILIES = "disablesmilies";
	/** For comparing against replies to see if the user actually typed anything. **/
	public static final String REPLY_ORIGINAL_CONTENT = "original_reply";
	public static final String EDIT_POST_ID = "edit_id";




	private int mThreadId = -1;
    private String mId = "";
    private String mDate = "";
    private String mRegDate = "";
    private String mUserId = "";
    private String mUsername = "";
    private String mAvatar = "";
    private String mAvatarSecond = "";
    private String mAvatarText = "";
    private String mContent = "";
    private String mEdited = "";

    private boolean isIgnored = false;
	private boolean mPreviouslyRead = false;
    private String mLastReadUrl = "";
    private boolean mEditable;
    private boolean isOp = false;
    private boolean isPlat = false;
    private String mRole = "";


    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("id", mId);
        result.put("date", mDate);
        result.put("user_id", mUserId);
        result.put("username", mUsername);
        result.put("avatar", mAvatar);
        result.put("avatar_second", mAvatarSecond);
        result.put("content", mContent);
        result.put("edited", mEdited);
        result.put("isIgnored", Boolean.toString(isIgnored()));
        result.put("previouslyRead", Boolean.toString(mPreviouslyRead));
        result.put("lastReadUrl", mLastReadUrl);
        result.put("editable", Boolean.toString(mEditable));
        result.put("role", mRole);
        result.put("isOp", Boolean.toString(isOp()));
        result.put("isPlat", Boolean.toString(isPlat()));

        return result;
    }

    public boolean isOp() {
    	return isOp;
    }

    public boolean isPlat() {
        return isPlat;
    }

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

	public String getRegDate() {
		return mRegDate;
	}

    public void setRegDate(String aRegDate) {
        mRegDate = aRegDate;
    }

    public String getUserId() {
    	return mUserId;
    }

    public void setUserId(String aUserId) {
    	mUserId = aUserId;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String aUsername) {
        mUsername = aUsername;
    }

    public void setThreadId(int aThreadId) {
        mThreadId = aThreadId;
    }

    public int getThreadId() {
        return mThreadId;
    }

    public void setIsOp(boolean aIsOp) {
        isOp = aIsOp;
    }

    public void setIsPlat(boolean plat) { isPlat = plat;}

    public String getRole() { return mRole; }

    public void setRole(String role) { mRole = role; }

    public String getAvatar() {
        return mAvatar;
    }

    public void setAvatar(String aAvatar) {
        mAvatar = aAvatar;
    }

    public String getAvatarSecond() {
        return mAvatarSecond;
    }

    public void setAvatarSecond(String aAvatarSecond) { mAvatarSecond = aAvatarSecond; }

    public String getContent() {
        return mContent;
    }

    public void setContent(String aContent) {
        mContent = aContent;
    }

    public static ArrayList<AwfulPost> fromCursor(Context aContext, Cursor aCursor) {
        ArrayList<AwfulPost> result = new ArrayList<AwfulPost>();

        if (aCursor.moveToFirst()) {
            int idIndex = aCursor.getColumnIndex(ID);
            int threadIdIndex = aCursor.getColumnIndex(THREAD_ID);
            int postIndexIndex = aCursor.getColumnIndex(POST_INDEX);//ooh, meta
            int dateIndex = aCursor.getColumnIndex(DATE);
            int regdateIndex = aCursor.getColumnIndex(REGDATE);
            int userIdIndex = aCursor.getColumnIndex(USER_ID);
            int usernameIndex = aCursor.getColumnIndex(USERNAME);
            int isIgnoredIndex = aCursor.getColumnIndex(IS_IGNORED);
            int previouslyReadIndex = aCursor.getColumnIndex(PREVIOUSLY_READ);
            int editableIndex = aCursor.getColumnIndex(EDITABLE);
            int isOpIndex = aCursor.getColumnIndex(IS_OP);
            int isPlatIndex = aCursor.getColumnIndex(IS_PLAT);
            int roleIndex = aCursor.getColumnIndex(ROLE);
            int avatarIndex = aCursor.getColumnIndex(AVATAR);
            int avatarSecondIndex = aCursor.getColumnIndex(AVATAR_SECOND);
            int avatarTextIndex = aCursor.getColumnIndex(AVATAR_TEXT);
            int contentIndex = aCursor.getColumnIndex(CONTENT);
            int editedIndex = aCursor.getColumnIndex(EDITED);

            AwfulPost current;

            do {
                current = new AwfulPost();
                current.setId(aCursor.getString(idIndex));
                current.setThreadId(aCursor.getInt(threadIdIndex));
                current.setDate(aCursor.getString(dateIndex));
                current.setRegDate(aCursor.getString(regdateIndex));
                current.setUserId(aCursor.getString(userIdIndex));
                current.setUsername(aCursor.getString(usernameIndex));
                current.setIsIgnored(aCursor.getInt(isIgnoredIndex) == 1);
                current.setPreviouslyRead(aCursor.getInt(previouslyReadIndex) > 0);
                current.setLastReadUrl(aCursor.getInt(postIndexIndex)+"");
                current.setEditable(aCursor.getInt(editableIndex) == 1);
                current.setIsOp(aCursor.getInt(isOpIndex) == 1);
                current.setIsPlat(aCursor.getInt(isPlatIndex) > 0);
                current.setRole(aCursor.getString(roleIndex));
                current.setAvatar(aCursor.getString(avatarIndex));
                current.setAvatarSecond(aCursor.getString(avatarSecondIndex));
                current.setAvatarText(aCursor.getString(avatarTextIndex));
                current.setContent(aCursor.getString(contentIndex));
                current.setEdited(aCursor.getString(editedIndex));

                result.add(current);
            } while (aCursor.moveToNext());
        }else{
            Timber.i("No posts to convert.");
        }
        return result;
    }


    /**
     * Process any videos found within an Element's hierarchy.
     *
     * This will look for appropriate video elements, and rewrite or replace them as necessary so
     * the app can display them according to the user's preferences.
     * This mutates the supplied Element's structure.
     * @param contentNode       the Element to search and edit
     * @param inlineYouTubes    whether YouTube videos should be displayed inline, or replaced with a link
     * @param inlineTiktoks     whether TikTok videos should be displayed inline, or replaced with a link
     */
    public static void convertVideos(Element contentNode, boolean inlineYouTubes, boolean inlineTiktoks){

        Elements youtubeNodes = contentNode.getElementsByClass("youtube-player");

        for (Element youTube : youtubeNodes) {
            try {
                String src = youTube.attr("src");
                //int height = Integer.parseInt(youTube.attr("height"));
                //int width = Integer.parseInt(youTube.attr("width"));
                Matcher youtubeMatcher = youtubeHDId_regex.matcher(src);
                if (youtubeMatcher.find()) {
                    String videoId = youtubeMatcher.group(1);
                    String link = "http://www.youtube.com/watch?v=" + videoId;
                    String image = "http://img.youtube.com/vi/" + videoId + "/0.jpg";

                    Element youtubeLink = new Element(Tag.valueOf("a"), "");
                    youtubeLink.text(link);
                    youtubeLink.attr("href", link);
                    if (!inlineYouTubes || postElementIsNMWSOrSpoilered(youTube)) {
                        youTube.replaceWith(youtubeLink);
                    } else {
                        youTube.after(youtubeLink);
                        youtubeLink.before(new Element(Tag.valueOf("br"), ""));

                        Element youtubeContainer = new Element(Tag.valueOf("div"), "");
                        youtubeContainer.addClass("videoWrapper");
                        youTube.before(youtubeContainer);
                        youtubeContainer.appendChild(youTube);
                        youTube.attr("sandbox", youTube.attr("sandbox") + " allow-top-navigation");
                    }
                }

            } catch (Exception e) {
                Timber.e(e, "Failed youtube conversion:");
                continue; //if we fail to convert the video tag, we can still display the rest.
            }
        }

        /*
         * TikTok URL forms seem to be:
         * https://www.tiktok.com/embed/[video id = \d+]
         * https://www.tiktok.com/@[username]/video/[video id]
         * there are more but they don't relate to embedding and don't appear to have video IDs associated
        */
        Elements tiktokNodes = contentNode.getElementsByClass("tiktok-player");

        for (Element tiktok : tiktokNodes) {
            try {
                String src = tiktok.attr("src");
                Matcher tiktokMatcher = tiktokId_regex.matcher(src);
                if (tiktokMatcher.find()) {
                    String videoId = tiktokMatcher.group(1);
                    // usernames aren't included in the embed link format, thankfully they don't matter
                    String linkURLPrefix = "https://www.tiktok.com/@/video/";
                    String link = linkURLPrefix + videoId;

                    Element tiktokLink = new Element(Tag.valueOf("a"), "");
                    tiktokLink.text(link);
                    tiktokLink.attr("href", link);
                    if (!inlineTiktoks || postElementIsNMWSOrSpoilered(tiktok)) {
                        tiktok.replaceWith(tiktokLink);
                    } else {
                        tiktok.after(tiktokLink);
                        tiktokLink.before(new Element(Tag.valueOf("br"), ""));
                    }
                }
            } catch (Exception e) {
                Timber.e(e, "Failed TikTok conversion:");
                continue;
            }
        }

        Elements videoNodes = contentNode.getElementsByClass("bbcode_video");
        for (Element node : videoNodes) {
            try {
                String src = null;
                int height = 0;
                int width = 0;
                Elements object = node.getElementsByTag("object");
                if (object.size() > 0) {
                    height = Integer.parseInt(object.get(0).attr("height"));
                    width = Integer.parseInt(object.get(0).attr("width"));
                    Elements emb = object.get(0).getElementsByTag("embed");
                    if (emb.size() > 0) {
                        src = emb.get(0).attr("src");
                    }
                }
                if (src != null && height != 0 && width != 0) {
                    String link = null, image = null;
                    Matcher vimeo = vimeoId_regex.matcher(src);
                    if (vimeo.find()) {
                        String videoId = vimeo.group(1);
                        Element vimeoXML;
                        try {
                            vimeoXML = NetworkUtils.get("http://vimeo.com/api/v2/video/" + videoId + ".xml");
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        }
                        if (vimeoXML.getElementsByTag("mobile_url").first() != null) {
                            link = vimeoXML.getElementsByTag("mobile_url").first().text();
                        } else {
                            link = vimeoXML.getElementsByTag("url").first().text();
                        }
                        image = vimeoXML.getElementsByTag("thumbnail_large").first().text();
                        src = link;
                    } else {
                        node.empty();
                        Element ln = new Element(Tag.valueOf("a"), "");
                        ln.attr("href", src);
                        ln.text(src);
                        node.replaceWith(ln);
                        continue;
                    }
                    node.empty();
                    Element ln = new Element(Tag.valueOf("a"), "");
                    ln.attr("href", link);
                    ln.text(link);
                    node.replaceWith(ln);
                }

            } catch (Exception e) {
                Timber.e(e, "Failed video conversion:");
                continue;//if we fail to convert the video tag, we can still display the rest.
            }
        }
    }

    /**
     * Duplicates logic from embedding.js. Make changes in both locations!
     * @param postElement Must have a containing .postbody element
     * @return boolean
     */
    private static boolean postElementIsNMWSOrSpoilered(Element postElement) {
        return (Objects.requireNonNull(postElement.closest(".postbody")).selectFirst("img[title=':nws:'], img[title=':nms:']") != null)
                || Objects.requireNonNull(postElement.parent()).hasClass("bbc-spoiler");
    }

	public String getEdited() {
        return mEdited;
    }

    public void setEdited(String aEdited) {
        mEdited = aEdited;
    }

    public String getAvatarText() {
    	return mAvatarText;
	}

    public void setAvatarText(String text) {
    	mAvatarText = text;
	}


    public String getLastReadUrl() {
        return mLastReadUrl;
    }

    public void setLastReadUrl(String aLastReadUrl) {
        mLastReadUrl = aLastReadUrl;
    }

    public boolean isIgnored() { return isIgnored; }

    public void setIsIgnored(boolean ignored) { isIgnored = ignored; }

    public boolean isPreviouslyRead() {
		return mPreviouslyRead;
	}

	public void setPreviouslyRead(boolean aPreviouslyRead) {
		mPreviouslyRead = aPreviouslyRead;
	}

	public boolean isEditable() {
		return mEditable;
	}

	public void setEditable(boolean aEditable) {
		mEditable = aEditable;
	}



    /**
     * Parse a thread page to grab its post data.
     *
     * @param content
     * @param aThread
     * @param aThreadId
     * @param unreadIndex
     * @param opId
     * @param prefs
     * @param startIndex
     * @return the number of posts found on the page
     */
    public static int syncPosts(ContentResolver content, Document aThread, int aThreadId, int unreadIndex, int opId, AwfulPreferences prefs, int startIndex){
        List<ContentValues> result = AwfulPost.parsePosts(aThread, aThreadId, unreadIndex, opId, prefs, startIndex);
        int resultCount = content.bulkInsert(CONTENT_URI, result.toArray(new ContentValues[result.size()]));
        Timber.i("Inserted " + resultCount + " posts into DB, threadId:" + aThreadId + " unreadIndex: " + unreadIndex);
        return resultCount;
    }


    public static List<ContentValues> parsePosts(Document aThread, int aThreadId, int unreadIndex, int opId, AwfulPreferences prefs, int startIndex){
		int index = startIndex;
        String updateTime = new Timestamp(System.currentTimeMillis()).toString();

        Elements posts = aThread.getElementsByClass("post");
        List<Callable<ContentValues>> parseTasks = new ArrayList<>(posts.size());
        for(Element postData : posts){
            parseTasks.add(new PostParseTask(postData, updateTime, index, unreadIndex, aThreadId, opId, prefs));
            index++;
        }

        long startTime = System.currentTimeMillis();
        // parse posts using multithreading if possible - some of the Jsoup calls (#html in particular) are very slow
        // (#html should be a lot faster when jsoup updates to handle Windows-1252 encoding user their fast path for Entities#canEncode)
        List<ContentValues> result = ForumParsingKt.parse(parseTasks);
        float averageParseTime = (System.currentTimeMillis() - startTime) / (float) parseTasks.size();
        Timber.i("%d posts found, %d posts parsed\nAverage parse time: %.3fms", posts.size(), result.size(), averageParseTime);
        return result;
    }


    /**
     * Process an img element from a post, to make it display correctly in the app.
     * <p>
     * This performs any necessary conversion, referring to user preferences to determine if e.g.
     * images should be loaded or converted to a link. This mutates the supplied Element.
     *
     * @param img        an Element represented by an img tag
     * @param isOldImage whether this is an old image (from a previously seen post), may be hidden
     * @param prefs      preferences used to make decisions
     */
    public static void processPostImage(Element img, boolean isOldImage, AwfulPreferences prefs) {
        //don't alter video mock buttons
        if (img.hasClass("videoPlayButton")) {
            return;
        }
        tryConvertToHttps(img);
        boolean isTimg = img.hasClass("timg");
        String originalUrl = img.attr("src");

	// Fix postimg.org images
	if (originalUrl.contains("postimg.org")) {
		originalUrl = originalUrl.replace(".org/",".cc/");
	}
        // check whether images can be converted to / wrapped in a link
        boolean alreadyLinked = img.parent() != null && img.parent().tagName().equalsIgnoreCase("a");
        boolean linkOk = !img.hasClass("nolink");

        // image is a smiley - if required, replace it with its :code: (held in the 'title' attr)
        if (img.hasAttr("title")) {
            if (!prefs.showSmilies) {
                String name = img.attr("title");
                img.replaceWith(new Element(Tag.valueOf("span"), "").text(name));
            }
            return;
        }

        // image shouldn't be displayed - convert to link / plaintext url
        // if image is wrapped in an <a>, make a link to image and the <a>
        if (isOldImage && prefs.hideOldImages || !prefs.canLoadImages()) {
            if (!linkOk) {
                img.replaceWith(new Element(Tag.valueOf("span"), "").text(originalUrl).attr("class","link-no-ok"));
            } else if (alreadyLinked) {
                Element newParent = new Element(Tag.valueOf("span"), "").attr("class", "converted-to-link");
                Element parent = img.parent();

                parent.appendText(parent.attr("href")).attr("class", "a-link");

                parent.parent().insertChildren(parent.elementSiblingIndex(), newParent);  // set the image as the first child of the div
                newParent.appendChild(parent);
                newParent.appendChild(img);
                img.replaceWith(new Element(Tag.valueOf("a"), "")
                        .attr("href", originalUrl)
                        .text(originalUrl)
                        .attr("class", "img-link"));
            } else {
                // switch out for a link with the url
                img.replaceWith(new Element(Tag.valueOf("a"), "").attr("href", originalUrl).text(originalUrl));
            }
            return;
        }

        // normal image - if we can't link it (e.g. to turn into an expandable thumbnail) there's nothing else to do
        if (!linkOk || alreadyLinked) {
            return;
        }

        // handle linking, thumbnailing, gif conversion etc

        // default to the 'thumbnail' url just being the full image
        String thumbUrl = originalUrl;

        // thumbnail any imgur images according to user prefs, if set
        if (!prefs.imgurThumbnails.equals("d") && thumbUrl.contains("i.imgur.com")) {
            thumbUrl = imgurAsThumbnail(thumbUrl, prefs.imgurThumbnails);
        }

        // handle gifs - different cases for different sites
        if (prefs.disableGifs && StringUtils.containsIgnoreCase(thumbUrl, ".gif")) {
            if (StringUtils.containsIgnoreCase(thumbUrl, "imgur.com")) {
                thumbUrl = imgurAsThumbnail(thumbUrl, "h");
            } else if (StringUtils.containsIgnoreCase(thumbUrl, "i.kinja-img.com")) {
                thumbUrl = thumbUrl.replace(".gif", ".jpg");
            } else if (StringUtils.containsIgnoreCase(thumbUrl, "giphy.com")) {
                thumbUrl = thumbUrl.replace("://i.giphy.com", "://media.giphy.com/media");
                if (thumbUrl.endsWith("giphy.gif")) {
                    thumbUrl = thumbUrl.replace("giphy.gif", "200_s.gif");
                } else {
                    thumbUrl = thumbUrl.replace(".gif", "/200_s.gif");
                }
            } else if (StringUtils.containsIgnoreCase(thumbUrl, "giant.gfycat.com")) {
                thumbUrl = thumbUrl.replace("giant.gfycat.com", "thumbs.gfycat.com");
                thumbUrl = thumbUrl.replace(".gif", "-poster.jpg");
            } else {
                thumbUrl = "file:///android_asset/images/gif.png";
                img.attr("width", "200px");
            }

            // link and rewrite image, setting the link as click-to-play
            thumbnailAndLink(img, thumbUrl).addClass("playGif");
            return;
        }

        // non-gif images - wrap them in a link, unless handling as a TIMG (to avoid breaking its click behaviour)
        if (!isTimg || prefs.disableTimgs) {
            // if the image hasn't been processed then thumbUrl will be the original image URL, i.e. a full-size image
            thumbnailAndLink(img, thumbUrl);
        }
    }


    /**
     * Rewrite a Imgur image url as a thumbnailed version, if possible (e.g. not already a thumbnail).
     *
     * @param imgurUrl      the image url to rewrite
     * @param thumbnailCode the type of thumbnail, usually a single character code
     * @return the rewritten url, or the original if it couldn't be rewritten
     */
    @NonNull
    private static String imgurAsThumbnail(@NonNull String imgurUrl, @NonNull String thumbnailCode) {

        Matcher match =  imgurId_regex.matcher(imgurUrl);
        if(match.find()) {
            String imgurBase = match.group(1);
            String imgurImageId = match.group(2);
            String imgurImageEnd = match.group(3);

            //check if already thumbnails
            if (imgurImageId.length() != 6 && imgurImageId.length() != 8) {
                imgurUrl = imgurBase + imgurImageId + thumbnailCode + imgurImageEnd;
            }
        }
        return imgurUrl;
    }


    /**
     * Rewrite an image element as a thumbnail, wrapping it in a link to the original image URL.
     * <p>
     * The image will be replaced in the DOM by the anchor element, with the image as its child, and
     * the anchor returned. Classes on the image element are cleared.
     *
     * @param img          the image element
     * @param thumbnailUrl the URL for the wrapped image
     * @return the link element, containing the modified image element
     */
    @NonNull
    private static Element thumbnailAndLink(@NonNull Element img, @NonNull String thumbnailUrl) {
        Element link = new Element(Tag.valueOf("a"), "").attr("href", img.attr("src"));
        // rewrite the image (new src, no classes) and wrap it with the link in the DOM
        img.attr("src", thumbnailUrl);
        img.classNames(Collections.emptySet());
        img.replaceWith(link);
        link.appendChild(img);
        return link;
    }


    /**
     * Converts URLs to https versions, where appropriate.
     * <p>
     * This mutates the element directly.
     */
    public static void tryConvertToHttps(@NonNull Element element) {
        String attr;
        String url;

        // get the element's url attribute, give up if it doesn't have one
        if (element.hasAttr("href")) {
            attr = "href";
        } else if (element.hasAttr("src")) {
            attr = "src";
        } else {
            return;
        }

        // if the element's url is for a https-able domain, rewrite it
        for (String domain : HTTPS_SUPPORTED_DOMAINS) {
            url = element.attr(attr);
            if (StringUtils.containsIgnoreCase(url, domain)) {
                element.attr(attr, url.replace("http://", "https://"));
                return;
            }
        }
    }

}
