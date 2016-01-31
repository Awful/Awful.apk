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

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.ferg.awfulapp.AwfulFragment;
import com.ferg.awfulapp.ForumDisplayFragment;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.util.AwfulUtils;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;
import com.samskivert.mustache.Template;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AwfulThread extends AwfulPagedItem  {
    private static final String TAG = "AwfulThread";

    public static final String PATH         = "/thread";
    public static final String UCP_PATH     = "/ucpthread";
    public static final Uri CONTENT_URI     = Uri.parse("content://" + Constants.AUTHORITY + PATH);
	public static final Uri CONTENT_URI_UCP = Uri.parse("content://" + Constants.AUTHORITY + UCP_PATH);

    public static final String ID 		            = "_id";
    public static final String INDEX 		        = "thread_index";
    public static final String FORUM_ID 	        = "forum_id";
    public static final String TITLE 		        = "title";
    public static final String POSTCOUNT 	        = "post_count";
    public static final String UNREADCOUNT          = "unread_count";
    public static final String AUTHOR 		        = "author";
    public static final String AUTHOR_ID 	        = "author_id";
	public static final String LOCKED               = "locked";
	public static final String BOOKMARKED           = "bookmarked";
	public static final String STICKY               = "sticky";
	public static final String CATEGORY             = "category";
	public static final String LASTPOSTER           = "killedby";
	public static final String FORUM_TITLE          = "forum_title";
	public static final String HAS_NEW_POSTS        = "has_new_posts";
    public static final String HAS_VIEWED_THREAD    = "has_viewed_thread";
    public static final String ARCHIVED             = "archived";
	public static final String RATING               = "rating";
    public static final String TAG_URL 		        = "tag_url";
    public static final String TAG_CACHEFILE 	    = "tag_cachefile";
    public static final String TAG_EXTRA            = "tag_extra";

    /** All the scripts used in generating HTML */
    private static final String[] JS_FILES = {
        "zepto.min.js",
        "selector.js",
        "scrollend.js",
        "inviewport.js",
        "fx.js",
        "fx_methods.js",
        "reorient.js",
        "json2.js",
        "twitterwidget.js",
        "salr.js",
        "thread.js"
    };

    private static final Pattern forumId_regex = Pattern.compile("forumid=(\\d+)");
	private static final Pattern urlId_regex = Pattern.compile("([^#]+)#(\\d+)$");


	public static ArrayList<ContentValues> parseForumThreads(Document aResponse, int start_index, int forumId) {
        ArrayList<ContentValues> result = new ArrayList<>();
        Element threads = aResponse.getElementById("forum");
        String update_time = new Timestamp(System.currentTimeMillis()).toString();
        Log.v(TAG, "Update time: " + update_time);
		for(Element node : threads.getElementsByClass("thread")){
            try {
    			ContentValues thread = new ContentValues();
                String threadId = node.id();
                if(threadId == null || threadId.length() < 1){
                	//skip the table header
                	continue;
                }
                thread.put(ID, Integer.parseInt(threadId.replaceAll("\\D", "")));
                if(forumId != Constants.USERCP_ID){//we don't update these values if we are loading bookmarks, or it will overwrite the cached forum results.
                	thread.put(INDEX, start_index);
                	thread.put(FORUM_ID, forumId);
                }
                start_index++;
                Elements tarThread = node.getElementsByClass("thread_title");
                Elements tarPostCount = node.getElementsByClass("replies");
            	if (tarPostCount.size() > 0) {
                    thread.put(POSTCOUNT, Integer.parseInt(tarPostCount.first().text().trim())+1);//this represents the number of replies, but the actual postcount includes OP
                }
                if (tarThread.size() > 0) {
                    thread.put(TITLE, tarThread.first().text().trim());
                }

                if(node.hasClass("closed")){
                	thread.put(LOCKED, 1);
                }else{
                	thread.put(LOCKED, 0);
                }


                Elements killedBy = node.getElementsByClass("lastpost");
                thread.put(LASTPOSTER, killedBy.first().getElementsByClass("author").first().text());
                Elements tarSticky = node.getElementsByClass("title_sticky");
                if (tarSticky.size() > 0) {
                    thread.put(STICKY,1);
                } else {
                    thread.put(STICKY,0);
                }

                Element rating = node.getElementsByClass("rating").first();
                if (rating != null && rating.children().size() > 0){
                	Element img = rating.children().first();
                	thread.put(RATING, AwfulRatings.getId(img.attr("src")));
                } else {
                	thread.put(RATING, AwfulRatings.NO_RATING);
                }

                Elements tarIcon = node.getElementsByClass("icon");
                if (tarIcon.size() > 0 && tarIcon.first().getAllElements().size() >0) {
                    Matcher threadTagMatcher = urlId_regex.matcher(tarIcon.first().getElementsByTag("img").first().attr("src"));
                    if(threadTagMatcher.find()){
                    	//thread tag stuff
        				Matcher fileNameMatcher = AwfulEmote.fileName_regex.matcher(threadTagMatcher.group(1));
        				if(fileNameMatcher.find()){
        					thread.put(TAG_CACHEFILE,fileNameMatcher.group(1));
        				}
                    	thread.put(TAG_URL, threadTagMatcher.group(1));
                    	thread.put(CATEGORY, threadTagMatcher.group(2));
                    }else{
                    	thread.put(CATEGORY, 0);
                    }
                }

                /*
                    secondary tags
                 */
                Element extraTag = node.getElementsByClass("icon2").first();
                if (extraTag != null && extraTag.children().size() > 0){
                    Element img = extraTag.children().first();
                    thread.put(TAG_EXTRA, ExtraTags.getId(img.attr("src")));
                } else {
                    thread.put(TAG_EXTRA, ExtraTags.NO_TAG);
                }

                Elements tarUser = node.getElementsByClass("author");
                if (tarUser.size() > 0) {
                    // There's got to be a better way to do this
                    thread.put(AUTHOR, tarUser.first().text().trim());
                    // And probably a much better way to do this
                    thread.put(AUTHOR_ID,tarUser.first().getElementsByAttribute("href").first().attr("href").substring(tarUser.first().getElementsByAttribute("href").first().attr("href").indexOf("userid=")+7));
                }

                Elements tarCount = node.getElementsByClass("count");
                if (tarCount.size() > 0 && tarCount.first().getAllElements().size() > 0) {
                    thread.put(UNREADCOUNT, Integer.parseInt(tarCount.first().getAllElements().first().text().trim()));
					thread.put(HAS_VIEWED_THREAD, 1);
                } else {
					thread.put(UNREADCOUNT, 0);
                	Elements tarXCount = node.getElementsByClass("x");
                    // If there are X's then the user has viewed the thread
					thread.put(HAS_VIEWED_THREAD, (tarXCount.isEmpty()?0:1));
                }
                Elements tarStar = node.getElementsByClass("star");
                if(tarStar.size()>0) {
                    // Bookmarks can only be detected now by the presence of a "bmX" class - no star image
                    if(tarStar.first().hasClass("bm0")) {
                        thread.put(BOOKMARKED, 1);
                    }
                    else if(tarStar.first().hasClass("bm1")) {
                        thread.put(BOOKMARKED, 2);
                    }
                    else if(tarStar.first().hasClass("bm2")) {
                        thread.put(BOOKMARKED, 3);
                    }
                    else {
                        thread.put(BOOKMARKED, 0);
                    }
                } else {
                    thread.put(BOOKMARKED, 0);
                }
        		thread.put(AwfulProvider.UPDATED_TIMESTAMP, update_time);
                result.add(thread);
            } catch (NullPointerException e) {
                // If we can't parse a row, just skip it
                e.printStackTrace();
                continue;
            }
        }
        return result;
	}

	public static ArrayList<ContentValues> parseSubforums(Document aResponse, int parentForumId){
        ArrayList<ContentValues> result = new ArrayList<>();
		Elements subforums = aResponse.getElementsByClass("subforum");
        for(Element sf : subforums){
        	Elements href = sf.getElementsByAttribute("href");
        	if(href.size() <1){
        		continue;
        	}
        	int id = Integer.parseInt(href.first().attr("href").replaceAll("\\D", ""));
        	if(id > 0){
        		ContentValues tmp = new ContentValues();
        		tmp.put(AwfulForum.ID, id);
        		tmp.put(AwfulForum.PARENT_ID, parentForumId);
        		tmp.put(AwfulForum.TITLE, href.first().text());
        		Elements subtext = sf.getElementsByTag("dd");
        		if(subtext.size() >1){
        			tmp.put(AwfulForum.SUBTEXT, subtext.first().text().replaceAll("\"", "").trim().substring(2));//ugh
        		}
        		result.add(tmp);
        	}
        }
        return result;
    }

    public static void getThreadPosts(ContentResolver contentResolv, Document response, int aThreadId, int aPage, int aPageSize, AwfulPreferences aPrefs, int aUserId) {


		Cursor threadData = contentResolv.query(ContentUris.withAppendedId(CONTENT_URI, aThreadId), AwfulProvider.ThreadProjection, null, null, null);
    	int totalReplies = 0, unread = 0, opId = 0, bookmarkStatus = 0, hasViewedThread = 0, postcount = 0;
		if(threadData != null && threadData.moveToFirst()){
			totalReplies    = threadData.getInt(threadData.getColumnIndex(POSTCOUNT));
			unread          = threadData.getInt(threadData.getColumnIndex(UNREADCOUNT));
            postcount       = threadData.getInt(threadData.getColumnIndex(POSTCOUNT));
			opId            = threadData.getInt(threadData.getColumnIndex(AUTHOR_ID));
			hasViewedThread = threadData.getInt(threadData.getColumnIndex(HAS_VIEWED_THREAD));
			bookmarkStatus  = threadData.getInt(threadData.getColumnIndex(BOOKMARKED));
		}

        ContentValues thread = new ContentValues();
        thread.put(ID, aThreadId);
    	Elements tarTitle = response.getElementsByClass("bclast");
        if (tarTitle.size() > 0) {
        	thread.put(TITLE, tarTitle.first().text().trim());
        }else{
        	Log.e(TAG,"TITLE NOT FOUND!");
        }

        Elements replyAlts = response.getElementsByAttributeValue("alt", "Reply");
        if (replyAlts.size() >0 && replyAlts.get(0).attr("src").contains("forum-closed")) {
        	thread.put(LOCKED, 1);
        }else{
        	thread.put(LOCKED, 0);
        }

        Elements bkButtons = response.getElementsByClass("thread_bookmark");
        if (bkButtons.size() >0) {
        	String bkSrc = bkButtons.get(0).attr("src");
        	if(bkSrc != null && bkSrc.contains("unbookmark")){
        		if(bookmarkStatus < 1){
        			thread.put(BOOKMARKED, 1);
        		}
        	}else{
        		thread.put(BOOKMARKED, 0);
        	}
            thread.put(ARCHIVED, 0);
        }else{
            thread.put(BOOKMARKED, 0);
            thread.put(ARCHIVED, 1);
        }
    	int forumId = -1;
    	for(Element breadcrumb : response.getElementsByClass("breadcrumbs")){
	    	for(Element forumLink : breadcrumb.getElementsByAttribute("href")){
	    		Matcher matchForumId = forumId_regex.matcher(forumLink.attr("href"));
	    		if(matchForumId.find()){//switched this to a regex
	    			forumId = Integer.parseInt(matchForumId.group(1));//so this won't fail
	    		}
	    	}
    	}
    	thread.put(FORUM_ID, forumId);
    	int lastPage = AwfulPagedItem.parseLastPage(response);

        if (threadData != null) {
            threadData.close();
        }
		int replycount;
		if(aUserId > 0){
			replycount = AwfulPagedItem.pageToIndex(lastPage, aPageSize, 0);
		}else{
			replycount = Math.max(totalReplies, AwfulPagedItem.pageToIndex(lastPage, aPageSize, 0));
		}

    	int newUnread = Math.max(0, replycount-AwfulPagedItem.pageToIndex(aPage, aPageSize, aPageSize-1));
    	if(unread > 0){
        	newUnread = Math.min(unread, newUnread);
    	}
    	if(aPage == lastPage){
    		newUnread = 0;
    	}
        if(postcount < replycount || aUserId > 0){
            thread.put(AwfulThread.POSTCOUNT, replycount);
            Log.v(TAG, "Parsed lastPage:"+lastPage+" old total: "+totalReplies+" new total:"+replycount);
        }
        if(postcount < replycount || newUnread < unread){
            thread.put(AwfulThread.UNREADCOUNT, newUnread);
            Log.i(TAG, aThreadId+" - Old unread: "+unread+" new unread: "+newUnread);
        }

        if(contentResolv.update(ContentUris.withAppendedId(CONTENT_URI, aThreadId), thread, null, null) <1){
            contentResolv.insert(CONTENT_URI, thread);
        }
        AwfulPost.syncPosts(contentResolv,
                response,
                aThreadId,
                ((unread == 0 && hasViewedThread == 0) ? 0 : totalReplies - unread),
                opId,
                aPrefs,
                AwfulPagedItem.pageToIndex(aPage, aPageSize, 0));
    }

    public static String getContainerHtml(AwfulPreferences aPrefs, int forumId){
        StringBuilder buffer = new StringBuilder("<!DOCTYPE html>\n<html>\n<head>\n");
        buffer.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0 maximum-scale=1.0 minimum-scale=1.0, user-scalable=no\" />\n");
        buffer.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n");
        buffer.append("<meta name='format-detection' content='telephone=no' />\n");
        buffer.append("<meta name='format-detection' content='address=no' />\n");


        // build the link tag, using the custom css path if necessary
        buffer.append("<link rel='stylesheet' href='");
        buffer.append(AwfulUtils.determineCSS(forumId));
        buffer.append("'>\n");
        buffer.append("<link rel='stylesheet' href='file:///android_asset/css/general.css' />");


        if(!aPrefs.preferredFont.contains("default")){
            buffer.append("<style id='font-face' type='text/css'>@font-face { font-family: userselected; src: url('content://com.ferg.awfulapp.webprovider/").append(aPrefs.preferredFont).append("'); }</style>\n");
        }
        for (String scriptName : JS_FILES) {
            buffer.append("<script src='file:///android_asset/")
                    .append(scriptName)
                    .append("' type='text/javascript'></script>\n");
        }

        buffer.append("</head><body><div id='container' class='container' ")
                .append((!aPrefs.noFAB ? "style='padding-bottom:75px'" : ""))
                .append("></div></body></html>");
        return buffer.toString();
    }

    public static String getHtml(ArrayList<AwfulPost> aPosts, AwfulPreferences aPrefs, int page, int lastPage, int forumId, boolean threadLocked) {
        StringBuilder buffer = new StringBuilder(1024);
        buffer.append("<div class='content'>\n");

        if(aPrefs.hideOldPosts && aPosts.size() > 0 && !aPosts.get(aPosts.size()-1).isPreviouslyRead()){
            int unreadCount = 0;
            for(AwfulPost ap : aPosts){
                if(!ap.isPreviouslyRead()){
                    unreadCount++;
                }
            }
            if(unreadCount < aPosts.size() && unreadCount > 0){
                buffer.append("    <article class='toggleread'>");
                buffer.append("      <a>\n");
                final int prevPosts = aPosts.size() - unreadCount;
                buffer.append("        <h3>Show ")
                        .append(prevPosts).append(" Previous Post").append(prevPosts > 1 ? "s" : "").append("</h3>\n");
                buffer.append("      </a>\n");
                buffer.append("    </article>");
            }
        }

        buffer.append(AwfulThread.getPostsHtml(aPosts, aPrefs, threadLocked));

        if(page == lastPage){
            buffer.append("<div class='unread' ></div>\n");
        }
        buffer.append("</div>\n");

        return buffer.toString();
    }

    public static String getPostsHtml(ArrayList<AwfulPost> aPosts, AwfulPreferences aPrefs, boolean threadLocked) {
        StringBuilder buffer = new StringBuilder();
        Template postTemplate;

        try {
            Reader templateReader = null;
            if (!"default".equals(aPrefs.layout)) {
                if (AwfulUtils.isMarshmallow()) {
                    int permissionCheck = ContextCompat.checkSelfPermission(aPrefs.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);

                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        File template = new File(Environment.getExternalStorageDirectory() + "/awful/" + aPrefs.layout);
                        if (template.isFile() && template.canRead()) {
                            templateReader = new FileReader(template);
                        }
                    }else{
                        Toast.makeText(aPrefs.getContext(), "Can't access custom layout because Awful lacks storage permissions. Reverting to default layout.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    File template = new File(Environment.getExternalStorageDirectory() + "/awful/" + aPrefs.layout);
                    if (template.isFile() && template.canRead()) {
                        templateReader = new FileReader(template);
                    }

                }
            }
            if (templateReader == null) {
                templateReader = new InputStreamReader(aPrefs.getResources().getAssets().open("mustache/post.mustache"));
            }
            postTemplate = Mustache.compiler().compile(templateReader);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

        for (AwfulPost post : aPosts) {
        	Map<String, String> postData = new HashMap<>();
            String username = post.getUsername();
            String avatar   = post.getAvatar();

        	postData.put("seen", (post.isPreviouslyRead() ? "read" : "unread"));
        	postData.put("isOP", (aPrefs.highlightOP && post.isOp()) ? "op" : null);
            postData.put("isMarked", (aPrefs.markedUsers.contains(username)) ? "marked" : null);
        	postData.put("postID", post.getId());
        	postData.put("isSelf", (aPrefs.highlightSelf && username.equals(aPrefs.username)) ? "self" : null);
            postData.put("avatarURL", (aPrefs.canLoadAvatars() && avatar != null && avatar.length() > 0 ) ? avatar : null);
        	postData.put("username", username);
        	postData.put("userID", post.getUserId());
        	postData.put("postDate", post.getDate());
        	postData.put("regDate", post.getRegDate());
        	postData.put("mod", (post.isMod()) ? "mod" : null);
        	postData.put("admin", (post.isAdmin()) ? "admin" : null);
        	postData.put("avatarText", ""+post.getAvatarText());
        	postData.put("lastReadUrl", post.getLastReadUrl());
        	postData.put("notOnProbation",(aPrefs.isOnProbation()) ? null : "notOnProbation");
        	postData.put("editable",(post.isEditable()) ? "editable" : null);
        	postData.put("postcontent", post.getContent());

        	try{
        		buffer.append(postTemplate.execute(postData));
        	}catch(MustacheException e){
        		e.printStackTrace();
        	}
        }
        return buffer.toString();
    }

	@SuppressWarnings("deprecation")
	public static void getView(View current, AwfulPreferences prefs, Cursor data, AQuery aq, AwfulFragment parent) {
        aq.recycle(current);
        Resources resources = current.getResources();
        Context context = current.getContext();

        String ForumName = null;
        if (prefs.forceForumThemes && ForumDisplayFragment.class.isInstance(parent)) {
            switch (((ForumDisplayFragment) parent).getForumId()) {
                case Constants.FORUM_ID_YOSPOS:
                    ForumName = prefs.amberDefaultPos ? ColorProvider.AMBERPOS : ColorProvider.YOSPOS;
                    break;
                case Constants.FORUM_ID_FYAD:
                case Constants.FORUM_ID_FYAD_SUB:
                    ForumName = ColorProvider.FYAD;
                    break;
                case Constants.FORUM_ID_BYOB:
                case Constants.FORUM_ID_COOL_CREW:
                    ForumName = ColorProvider.BYOB;
                    break;
            }
        }

        TextView info   = (TextView) current.findViewById(R.id.thread_info);
        TextView title  = (TextView) current.findViewById(R.id.title);
        TextView unread = (TextView) current.findViewById(R.id.unread_count);
        boolean stuck   = data.getInt(data.getColumnIndex(STICKY)) >0;
        int unreadCount = data.getInt(data.getColumnIndex(UNREADCOUNT));
        int bookmarked  = data.getInt(data.getColumnIndex(BOOKMARKED));
        boolean hasViewedThread = data.getInt(data.getColumnIndex(HAS_VIEWED_THREAD)) == 1;

        ImageView threadTag = (ImageView) current.findViewById(R.id.thread_tag);
        threadTag.setVisibility(View.GONE);
        if (prefs.threadInfo_Tag) {
			String tagFile = data.getString(data.getColumnIndex(TAG_CACHEFILE));
			if (!TextUtils.isEmpty(tagFile)) {
                threadTag.setVisibility(View.VISIBLE);
                String url = data.getString(data.getColumnIndex(TAG_URL));
                String localFileName = "@drawable/"+url.substring(url.lastIndexOf('/') + 1,url.lastIndexOf('.')).replace('-','_').toLowerCase();

                int imageID = resources.getIdentifier(localFileName, null, context.getPackageName());
                if (imageID == 0) {
                    aq.id(R.id.thread_tag).image(url);
                } else {
                    threadTag.setImageResource(imageID);
                }
            }
		}


        /*
            Tag overlay (secondary tags etc)
         */
        aq.id(R.id.thread_tag_overlay).gone();
        int tagId = data.getInt(data.getColumnIndex(TAG_EXTRA));
        if (ExtraTags.getType(tagId) != ExtraTags.TYPE_NO_TAG) {
            Drawable tagIcon = ExtraTags.getDrawable(tagId, resources);
            if (tagIcon != null) {
                aq.id(R.id.thread_tag_overlay).visible().image(tagIcon);
            }
        }


        info.setVisibility(View.VISIBLE);
        StringBuilder tmp = new StringBuilder();
        tmp.append(AwfulPagedItem.indexToPage(data.getInt(data.getColumnIndex(POSTCOUNT)), prefs.postPerPage)).append(" pgs");
        if(hasViewedThread){
            tmp.append(" | Last: ").append(NetworkUtils.unencodeHtml(data.getString(data.getColumnIndex(LASTPOSTER))));
        }else{
            tmp.append(" | OP: ").append(NetworkUtils.unencodeHtml(data.getString(data.getColumnIndex(AUTHOR))));
        }
        info.setText(tmp.toString().trim());


        /*
            Ratings
         */
        aq.id(R.id.thread_rating).gone();
        if (prefs.threadInfo_Rating) {
            int rating = data.getInt(data.getColumnIndex(RATING));
            Drawable ratingIcon = AwfulRatings.getDrawable(rating, resources);
            if (ratingIcon != null) {
                // replace thread tag with special rating tag in the Film Dump
                if (AwfulRatings.getType(rating) == AwfulRatings.TYPE_FILM_DUMP) {
                    aq.id(R.id.thread_tag).visible().image(ratingIcon);
                } else {
                    aq.id(R.id.thread_rating).visible().image(ratingIcon);
                }
            }
        }


        aq.id(R.id.thread_locked).gone();
        aq.id(R.id.thread_sticky).gone();
        if (stuck) {
            aq.id(R.id.thread_sticky).visible().image(resources.getDrawable(R.drawable.ic_sticky));
        } else if (data.getInt(data.getColumnIndex(LOCKED)) > 0){
            //don't show lock if sticky, aka: every rules thread
            int[] attrs = { R.attr.iconMenuLockedDark };
            TypedArray ta = context.getTheme().obtainStyledAttributes(attrs);
            aq.id(R.id.thread_locked).visible().image(ta.getDrawable(0));
            current.setBackgroundColor(ColorProvider.getBackgroundColor(ForumName));
        }

        unread.setVisibility(View.GONE);
        if(hasViewedThread) {
            unread.setVisibility(View.VISIBLE);
            unread.setTextColor(ColorProvider.getUnreadColorFont(ForumName));
            unread.setText(Integer.toString(unreadCount));
            GradientDrawable counter = (GradientDrawable) resources.getDrawable(R.drawable.unread_counter);
            if (counter != null) {
                counter.mutate();
                counter.setColor(ColorProvider.getUnreadColor(ForumName, unreadCount < 1, bookmarked));
                unread.setBackgroundDrawable(counter);
            }
        }

		title.setTypeface(null, Typeface.NORMAL);
        String titleText = data.getString(data.getColumnIndex(TITLE));
        if(titleText != null){
			title.setText(titleText);
		}
        title.setTextColor(ColorProvider.getTextColor(ForumName));
        info.setTextColor(ColorProvider.getAltTextColor(ForumName));
        title.setEllipsize(TruncateAt.END);
	}

}
