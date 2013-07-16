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

import com.android.volley.toolbox.NetworkImageView;
import com.ferg.awfulapp.AwfulFragment;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.ferg.awfulapp.AwfulActivity;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.ColorPickerPreference;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.service.AwfulSyncService;
import com.ferg.awfulapp.task.ThreadTask;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;
import com.samskivert.mustache.Template;

public class AwfulThread extends AwfulPagedItem  {
    private static final String TAG = "AwfulThread";

    public static final String PATH     = "/thread";
    public static final String UCP_PATH     = "/ucpthread";
    public static final Uri CONTENT_URI = Uri.parse("content://" + Constants.AUTHORITY + PATH);
	public static final Uri CONTENT_URI_UCP = Uri.parse("content://" + Constants.AUTHORITY + UCP_PATH);
    
    public static final String ID 		="_id";
    public static final String INDEX 		="thread_index";
    public static final String FORUM_ID 	="forum_id";
    public static final String TITLE 		="title";
    public static final String POSTCOUNT 	="post_count";
    public static final String UNREADCOUNT  ="unread_count";
    public static final String AUTHOR 		="author";
    public static final String AUTHOR_ID 	="author_id";
	public static final String LOCKED = "locked";
	public static final String BOOKMARKED = "bookmarked";
	public static final String STICKY = "sticky";
	public static final String CATEGORY = "category";
	public static final String LASTPOSTER = "killedby";
	public static final String FORUM_TITLE = "forum_title";
	public static final String HAS_NEW_POSTS = "has_new_posts";
    public static final String HAS_VIEWED_THREAD = "has_viewed_thread";
    public static final String ARCHIVED = "archived";
	public static final String RATING = "rating";

    public static final String TAG_URL 		="tag_url";
    public static final String TAG_CACHEFILE 	="tag_cachefile";
	
	private static final Pattern forumId_regex = Pattern.compile("forumid=(\\d+)");
	private static final Pattern urlId_regex = Pattern.compile("([^#]+)#(\\d+)$");

	
    public static Document getForumThreads(int aForumId, int aPage, Messenger statusCallback) throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(Constants.PARAM_FORUM_ID, Integer.toString(aForumId));

		if (aPage != 0) {
			params.put(Constants.PARAM_PAGE, Integer.toString(aPage));
		}

        return NetworkUtils.get(Constants.FUNCTION_FORUM, params, statusCallback, 50);
	}
	
    public static Document getUserCPThreads(int aPage, Messenger statusCallback) throws Exception {
    	HashMap<String, String> params = new HashMap<String, String>();
		params.put(Constants.PARAM_PAGE, Integer.toString(aPage));
        return NetworkUtils.get(Constants.FUNCTION_BOOKMARK, params, statusCallback, 50);
	}

	public static ArrayList<ContentValues> parseForumThreads(Document aResponse, int start_index, int forumId) throws Exception{
        ArrayList<ContentValues> result = new ArrayList<ContentValues>();
        Element threads = aResponse.getElementById("forum");
        String update_time = new Timestamp(System.currentTimeMillis()).toString();
        Log.v(TAG,"Update time: "+update_time);
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
                if(rating != null && rating.children().size() > 0){
                	Element img = rating.children().first();
                	int rate = Integer.parseInt(""+img.attr("src").charAt(img.attr("src").length()-10));
                	thread.put(RATING, rate);
                }else{
                	thread.put(RATING,0);
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

                Elements tarUser = node.getElementsByClass("author");
                if (tarUser.size() > 0) {
                    // There's got to be a better way to do this
                    thread.put(AUTHOR, tarUser.first().text().trim());
                    // And probably a much better way to do this
                    thread.put(AUTHOR_ID,tarUser.first().getElementsByAttribute("href").first().attr("href").substring(tarUser.first().getElementsByAttribute("href").first().attr("href").indexOf("userid=")+7));
                }

                Elements tarCount = node.getElementsByClass("count");
                if (tarCount.size() > 0 && tarCount.first().getAllElements().size() >0) {
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
        ArrayList<ContentValues> result = new ArrayList<ContentValues>();
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
        		if(subtext.size() >0){
        			tmp.put(AwfulForum.SUBTEXT, subtext.first().text().replaceAll("\"", "").trim().substring(2));//ugh
        		}
        		result.add(tmp);
        	}
        }
        return result;
    }

    public static String getThreadPosts(Context aContext, int aThreadId, int aPage, int aPageSize, AwfulPreferences aPrefs, int aUserId, Messenger statusUpdates, ThreadTask parentTask) throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(Constants.PARAM_THREAD_ID, Integer.toString(aThreadId));
        params.put(Constants.PARAM_PER_PAGE, Integer.toString(aPageSize));
        params.put(Constants.PARAM_PAGE, Integer.toString(aPage));
        params.put(Constants.PARAM_USER_ID, Integer.toString(aUserId));
        
        ContentResolver contentResolv = aContext.getContentResolver();
		Cursor threadData = contentResolv.query(ContentUris.withAppendedId(CONTENT_URI, aThreadId), AwfulProvider.ThreadProjection, null, null, null);
    	int totalReplies = 0, unread = 0, opId = 0, bookmarkStatus = 0, hasViewedThread = 0, postcount = 0;
		if(threadData.moveToFirst()){
			totalReplies = threadData.getInt(threadData.getColumnIndex(POSTCOUNT));
			unread = threadData.getInt(threadData.getColumnIndex(UNREADCOUNT));
            postcount = threadData.getInt(threadData.getColumnIndex(POSTCOUNT));
			opId = threadData.getInt(threadData.getColumnIndex(AUTHOR_ID));
			hasViewedThread = threadData.getInt(threadData.getColumnIndex(HAS_VIEWED_THREAD));
			bookmarkStatus = threadData.getInt(threadData.getColumnIndex(BOOKMARKED));
		}
        
        //notify user we are starting update
        statusUpdates.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, aThreadId, 10));
        
        Document response = NetworkUtils.get(Constants.FUNCTION_THREAD, params, statusUpdates, 25);

        if(parentTask.isCancelled()){
            return null;
        }

        //notify user we have gotten message body, this represents a large portion of this function
        statusUpdates.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, aThreadId, 50));
        
        String error = AwfulPagedItem.checkPageErrors(response, statusUpdates, aPrefs);
        if(error != null){
        	return error;
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

        //notify user we have began processing thread info
        statusUpdates.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, aThreadId, 55));

		threadData.close();
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
        if(postcount < replycount){
            thread.put(AwfulThread.POSTCOUNT, replycount);
            Log.v(TAG, "Parsed lastPage:"+lastPage+" old total: "+totalReplies+" new total:"+replycount);
        }
        if(postcount < replycount || newUnread < unread){
            thread.put(AwfulThread.UNREADCOUNT, newUnread);
            Log.i(TAG, aThreadId+" - Old unread: "+unread+" new unread: "+newUnread);
        }

        //notify user we have began processing posts
        statusUpdates.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, aThreadId, 65));
        
        AwfulPost.syncPosts(contentResolv, 
        					response, 
        					aThreadId, 
        					((unread == 0 && hasViewedThread == 0) ? 0 : totalReplies-unread),
        					opId, 
        					aPrefs,
        					AwfulPagedItem.pageToIndex(aPage, aPageSize, 0));
        
    	if(contentResolv.update(ContentUris.withAppendedId(CONTENT_URI, aThreadId), thread, null, null) <1){
    		contentResolv.insert(CONTENT_URI, thread);
    	}
    	
        //notify user we are done with this stage
        statusUpdates.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, aThreadId, 100));
        return null;
    }

    public static String getHtml(ArrayList<AwfulPost> aPosts, AwfulPreferences aPrefs, boolean isTablet, int page, int lastPage, int forumId, boolean threadLocked) {
        int unreadCount = 0;
        if(aPosts.size() > 0 && !aPosts.get(aPosts.size()-1).isPreviouslyRead()){
        	for(AwfulPost ap : aPosts){
        		if(!ap.isPreviouslyRead()){
        			unreadCount++;
        		}
        	}
        }
    	
    	
    	StringBuffer buffer = new StringBuffer("<!DOCTYPE html>\n<html>\n<head>\n");
        buffer.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0 maximum-scale=1.0 minimum-scale=1.0, user-scalable=no\" />\n");
        buffer.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n");
        buffer.append("<meta name='format-detection' content='telephone=no' />\n");
        buffer.append("<meta name='format-detection' content='address=no' />\n");
        File css = new File(Environment.getExternalStorageDirectory()+"/awful/"+aPrefs.theme);
        if(!aPrefs.forceForumThemes && StringUtils.countMatches(aPrefs.theme,".")>1 && css.exists() && css.isFile() && css.canRead()){
        	buffer.append("<link rel='stylesheet' href='"+Environment.getExternalStorageDirectory()+"/awful/"+aPrefs.theme+"'>\n");
        }else if(aPrefs.forceForumThemes){
        	switch(forumId){
				//TODO: No FYAD theme yet        	
//    			case(26):
//	    			buffer.append("<link rel='stylesheet' href='file:///android_asset/css/fyad.css'>\n");
//	    			break;
        		//RIP BYOB
//        		case(208):
//        			buffer.append("<link rel='stylesheet' href='file:///android_asset/css/byob.css'>\n");
//        			break;
        		case(219):
        			buffer.append("<link rel='stylesheet' href='file:///android_asset/css/yospos.css'>\n");
        			break;
        		default:
        			buffer.append("<link rel='stylesheet' href='file:///android_asset/css/"+aPrefs.theme+"'>\n");
        			break;
        	}
        }else{
            buffer.append("<link rel='stylesheet' href='file:///android_asset/css/"+aPrefs.theme+"'>\n");
        }
        if(!aPrefs.preferredFont.contains("default")){
        	buffer.append("<style type='text/css'>@font-face { font-family: userselected; src: url('content://com.ferg.awfulapp.webprovider/"+aPrefs.preferredFont+"'); }</style>\n");
        }
        buffer.append("<script src='file:///android_asset/jquery.min.js' type='text/javascript'></script>\n");
        buffer.append("<script src='file:///android_asset/jquery.ba-resize.min.js' type='text/javascript'></script>\n");
        
        buffer.append("<script type='text/javascript'>\n");
        buffer.append("  window.JSON = null;");
        if(isTablet){
        	buffer.append("window.isTablet = true;");
        }else{
        	buffer.append("window.isTablet = false;");
        }
        if(aPrefs.hideOldPosts && unreadCount > 0 && aPosts.size()-unreadCount > 0){
            buffer.append("window.hideRead = true;");
        }else{
            buffer.append("window.hideRead = false;");
        }
        buffer.append("</script>\n");
        
        
        buffer.append("<script src='file:///android_asset/json2.js' type='text/javascript'></script>\n");
        buffer.append("<script src='file:///android_asset/salr.js' type='text/javascript'></script>\n");
        buffer.append("<script src='file:///android_asset/thread.js' type='text/javascript'></script>\n");
        

        //this is a stupid workaround for animation performance issues. it's only needed for honeycomb/ICS
        if(AwfulActivity.isHoneycomb()){
	        buffer.append("<script type='text/javascript'>\n");
	        buffer.append("$(window).scroll(gifHide);");
	        buffer.append("$(window).ready(gifHide);");
	        buffer.append("</script>\n");
        }
        
        buffer.append("<style type='text/css'>\n");   
        if(!aPrefs.disableTimgs){
            buffer.append(".timg {border-color: #FF0 }\n");
        }

        //buffer.append(".bbc-spoiler, .bbc-spoiler li, .bbc-spoiler a { color: "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor)+"; background: "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor)+";}\n");
        
        if(aPrefs.hideOldPosts && unreadCount > 0 && aPosts.size()-unreadCount > 0){
            buffer.append(".read {display:none;}\n");
        }else{
            buffer.append(".toggleread {display:none;}\n");
        }
        
        buffer.append("</style>\n");
        buffer.append("</head>\n<body>\n");
        buffer.append("	  <div class='content' >\n");
        buffer.append("		<a class='toggleread' style='color: " + ColorPickerPreference.convertToARGB(ColorProvider.getTextColor(aPrefs)) + ";'>\n");
        buffer.append("			<h3>Show "+(aPosts.size()-unreadCount)+" Previous Post"+(aPosts.size()-unreadCount > 1?"s":"")+"</h3>\n");
        buffer.append("		</a>\n");

        buffer.append(AwfulThread.getPostsHtml(aPosts, aPrefs, threadLocked, isTablet));

        buffer.append("<div class='unread' ></div>\n");
        
        buffer.append("</div>\n");
        buffer.append("</body>\n</html>\n");

        return buffer.toString();
    }

    public static String getPostsHtml(ArrayList<AwfulPost> aPosts, AwfulPreferences aPrefs, boolean threadLocked, boolean isTablet) {
        StringBuffer buffer = new StringBuffer();
        Template postTemplate = null;

        try {
        	Reader templateReader;
        	if(!"default".equals(aPrefs.layout)){
        		File template = new File(Environment.getExternalStorageDirectory()+"/awful/"+aPrefs.layout);
        		if(template.exists() && template.isFile() && template.canRead()){
            		templateReader = new FileReader(template);
        		}else{
            		templateReader = new InputStreamReader(aPrefs.getResources().getAssets().open("mustache/post.mustache"));
            	}
        	}else{
        		templateReader = new InputStreamReader(aPrefs.getResources().getAssets().open("mustache/post.mustache"));
        	}
        	postTemplate = Mustache.compiler().compile(templateReader);
			} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}


        for (AwfulPost post : aPosts) {

        	Map<String, String> postData = new HashMap<String, String>();
        	
        	postData.put("seen", (post.isPreviouslyRead() ? "read" : "unread"));
        	postData.put("isOP", (post.isOp())?"op":null);
        	postData.put("postID", post.getId());
        	postData.put("isSelf", (aPrefs.highlightUsername && post.getUsername().equals(aPrefs.username)) ? "self" : null);
        	postData.put("avatarURL", (aPrefs.canLoadAvatars() && post.getAvatar() != null &&  post.getAvatar().length()>0) ? post.getAvatar() : null);
        	postData.put("username", post.getUsername());
        	postData.put("userID", post.getUserId());
        	postData.put("postDate", post.getDate());
        	postData.put("regDate", post.getRegDate());
        	postData.put("mod", (post.isMod())?"mod":null);
        	postData.put("admin", (post.isAdmin())?"admin":null);
        	postData.put("avatarText", post.getAvatarText());
        	postData.put("lastReadUrl",  post.getLastReadUrl());
        	postData.put("notOnProbation", (aPrefs.isOnProbation())?null:"notOnProbation");
        	postData.put("editable", (post.isEditable())?"editable":null);
        	postData.put("postcontent",  post.getContent());
        	
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
		TextView info = (TextView) current.findViewById(R.id.threadinfo);
		TextView title = (TextView) current.findViewById(R.id.title);
        TextView unread = (TextView) current.findViewById(R.id.unread_count);
		boolean stuck = data.getInt(data.getColumnIndex(STICKY)) >0;
        int unreadCount = data.getInt(data.getColumnIndex(UNREADCOUNT));
        boolean hasViewedThread = data.getInt(data.getColumnIndex(HAS_VIEWED_THREAD)) == 1;
		info.setSingleLine(!prefs.wrapThreadTitles);

        NetworkImageView threadTag = (NetworkImageView) current.findViewById(R.id.thread_tag);
		if(!prefs.threadInfo_Tag || !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            threadTag.setVisibility(View.GONE);
		}else{
			String tagFile = data.getString(data.getColumnIndex(TAG_CACHEFILE));
			if(tagFile != null){
                threadTag.setImageUrl(data.getString(data.getColumnIndex(TAG_URL)), parent.getImageLoader());
			}else{
                threadTag.setVisibility(View.GONE);
			}
		}

		if(!prefs.threadInfo_Author && !prefs.threadInfo_Killed && !prefs.threadInfo_Page && !prefs.threadInfo_Rating){
            info.setVisibility(View.VISIBLE);
			info.setText("");
		}else{
			info.setVisibility(View.VISIBLE);
			StringBuilder tmp = new StringBuilder();
			if(prefs.threadInfo_Page){
				tmp.append(AwfulPagedItem.indexToPage(data.getInt(data.getColumnIndex(POSTCOUNT)), prefs.postPerPage)+" pgs");
			}
            if(hasViewedThread){
                tmp.append(" | Last: "+NetworkUtils.unencodeHtml(data.getString(data.getColumnIndex(LASTPOSTER))));
            }else{
                tmp.append(" | OP: "+NetworkUtils.unencodeHtml(data.getString(data.getColumnIndex(AUTHOR))));
            }

            //TODO update to work with new layout
            //TODO also, update to use local assets
			if(prefs.threadInfo_Rating && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){//TODO how does this need external media? AQ doesn't require it, it'll revert to memcache
				String tagFile = data.getString(data.getColumnIndex(TAG_CACHEFILE));
				if(tagFile != null){
					switch(data.getInt(data.getColumnIndex(RATING))){
					case(1):
						aq.id(R.id.thread_rating).visible().image("http://fi.somethingawful.com/rate/default/1stars.gif", true, true);
						break;
					case(2):
						aq.id(R.id.thread_rating).visible().image("http://fi.somethingawful.com/rate/default/2stars.gif", true, true);
						break;
					case(3):
						aq.id(R.id.thread_rating).visible().image("http://fi.somethingawful.com/rate/default/3stars.gif", true, true);
						break;
					case(4):
						aq.id(R.id.thread_rating).visible().image("http://fi.somethingawful.com/rate/default/4stars.gif", true, true);
						break;
					case(5):
						aq.id(R.id.thread_rating).visible().image("http://fi.somethingawful.com/rate/default/5stars.gif", true, true);
						break;
					default:
						aq.id(R.id.thread_rating).gone();
						break;
					}
				}else{
					aq.id(R.id.thread_rating).gone();
				}
			}else{
				aq.id(R.id.thread_rating).gone();
			}
			info.setText(tmp.toString().trim());
		}
        if(stuck){
            info.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_sticky, 0, 0, 0);
        }else if(data.getInt(data.getColumnIndex(LOCKED)) > 0){
            //don't show lock if sticky, aka: every rules thread
            info.setCompoundDrawablesWithIntrinsicBounds(R.drawable.light_inline_lock,0,0,0);
            current.setBackgroundColor(ColorProvider.getBackgroundColor(prefs));
        }else{
            info.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

		unread.setTextColor(ColorProvider.getUnreadColorFont(prefs));
		if(unreadCount > 0) {
			unread.setVisibility(View.VISIBLE);
			unread.setText(Integer.toString(unreadCount));
			GradientDrawable counter = (GradientDrawable) current.getResources().getDrawable(R.drawable.unread_counter).mutate();
            counter.setColor(ColorProvider.getUnreadColor(prefs));
            unread.setBackgroundDrawable(counter);
		}
		else if(hasViewedThread) {
			unread.setVisibility(View.VISIBLE);
			unread.setText(Integer.toString(unreadCount));
			GradientDrawable counter = (GradientDrawable) current.getResources().getDrawable(R.drawable.unread_counter).mutate();
            counter.setColor(ColorProvider.getUnreadColorDim(prefs));
            unread.setBackgroundDrawable(counter);
        }
		else {
			unread.setVisibility(View.GONE);
		}
		title.setTypeface(null, Typeface.NORMAL);
		if(data.getString(data.getColumnIndex(TITLE)) != null){
			title.setText(data.getString(data.getColumnIndex(TITLE)));
		}
		if(prefs != null){
			title.setTextColor(ColorProvider.getTextColor(prefs));
			info.setTextColor(ColorProvider.getAltTextColor(prefs));
			title.setSingleLine(!prefs.wrapThreadTitles);
			if(!prefs.wrapThreadTitles){
				title.setEllipsize(TruncateAt.END);
			}else{
				title.setEllipsize(null);
			}
		}
	}
	
}
