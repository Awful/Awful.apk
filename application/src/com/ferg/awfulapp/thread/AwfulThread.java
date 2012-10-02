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
import java.io.FileInputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlcleaner.TagNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.ferg.awfulapp.AwfulActivity;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.preferences.ColorPickerPreference;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.service.AwfulSyncService;

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

    public static final String TAG_URL 		="tag_url";
    public static final String TAG_CACHEFILE 	="tag_cachefile";
	
	private static final Pattern forumId_regex = Pattern.compile("forumid=(\\d+)");
	private static final Pattern urlId_regex = Pattern.compile("([^#]+)#(\\d+)$");
	
    public static TagNode getForumThreads(int aForumId, int aPage, Messenger statusCallback) throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(Constants.PARAM_FORUM_ID, Integer.toString(aForumId));

		if (aPage != 0) {
			params.put(Constants.PARAM_PAGE, Integer.toString(aPage));
		}

        return NetworkUtils.get(Constants.FUNCTION_FORUM, params, statusCallback, 50);
	}
	
    public static TagNode getUserCPThreads(int aPage, Messenger statusCallback) throws Exception {
    	HashMap<String, String> params = new HashMap<String, String>();
		params.put(Constants.PARAM_PAGE, Integer.toString(aPage));
        return NetworkUtils.get(Constants.FUNCTION_BOOKMARK, params, statusCallback, 50);
	}

	public static ArrayList<ContentValues> parseForumThreads(TagNode aResponse, int start_index, int forumId) throws Exception{
        ArrayList<ContentValues> result = new ArrayList<ContentValues>();
        TagNode[] threads = aResponse.getElementsByAttValue("id", "forum", true, true);
        if(threads.length >1 || threads.length < 1){
        	return null;
        }
        String update_time = new Timestamp(System.currentTimeMillis()).toString();
        Log.v(TAG,"Update time: "+update_time);
        TagNode[] tbody = threads[0].getElementsByName("tbody", false);
		for(TagNode node : tbody[0].getChildTags()){
            try {
    			ContentValues thread = new ContentValues();
                String threadId = node.getAttributeByName("id");
                thread.put(ID, Integer.parseInt(threadId.replaceAll("\\D", "")));
                if(forumId != Constants.USERCP_ID){//we don't update these values if we are loading bookmarks, or it will overwrite the cached forum results.
                	thread.put(INDEX, start_index);
                	thread.put(FORUM_ID, forumId);
                }
                start_index++;
            	TagNode[] tarThread = node.getElementsByAttValue("class", "thread_title", true, true);
            	TagNode[] tarPostCount = node.getElementsByAttValue("class", "replies", true, true);
            	if (tarPostCount.length > 0) {
                    thread.put(POSTCOUNT, Integer.parseInt(tarPostCount[0].getText().toString().trim())+1);//this represents the number of replies, but the actual postcount includes OP
                }
                if (tarThread.length > 0) {
                    thread.put(TITLE, tarThread[0].getText().toString().trim());
                }
                
                if(node.hasAttribute("class") && node.getAttributeByName("class").contains("closed")){
                	thread.put(LOCKED, 1);
                }else{
                	thread.put(LOCKED, 0);
                }
                	

                TagNode[] killedBy = node.getElementsByAttValue("class", "lastpost", true, true);
                thread.put(LASTPOSTER, killedBy[0].getElementsByAttValue("class", "author", true, true)[0].getText().toString());
                TagNode[] tarSticky = node.getElementsByAttValue("class", "title title_sticky", true, true);
                if (tarSticky.length > 0) {
                    thread.put(STICKY,1);
                } else {
                    thread.put(STICKY,0);
                }

                TagNode[] tarIcon = node.getElementsByAttValue("class", "icon", true, true);
                if (tarIcon.length > 0 && tarIcon[0].getChildTags().length >0) {
                    Matcher threadTagMatcher = urlId_regex.matcher(tarIcon[0].getChildTags()[0].getAttributeByName("src"));
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

            	TagNode[] tarUser = node.getElementsByAttValue("class", "author", true, true);
                if (tarUser.length > 0) {
                    // There's got to be a better way to do this
                    thread.put(AUTHOR, tarUser[0].getText().toString().trim());
                    // And probably a much better way to do this
                    thread.put(AUTHOR_ID,((TagNode)tarUser[0].getElementListHavingAttribute("href", true).get(0)).getAttributes().get("href").substring(((TagNode)tarUser[0].getElementListHavingAttribute("href", true).get(0)).getAttributes().get("href").indexOf("userid=")+7));
                }

                TagNode[] tarCount = node.getElementsByAttValue("class", "count", true, true);
                if (tarCount.length > 0 && tarCount[0].getChildTags().length >0) {
                    thread.put(UNREADCOUNT, Integer.parseInt(tarCount[0].getChildTags()[0].getText().toString().trim()));
                } else {
                	TagNode[] tarXCount = node.getElementsByAttValue("class", "x", true, true);
					if (tarXCount.length > 0) {
						thread.put(UNREADCOUNT, 0);
					} else {
						thread.put(UNREADCOUNT,-1);
					} 
                }
                TagNode[] tarStar = node.getElementsByAttValue("class", "star", true, true);
                if(tarStar.length>0){
                	TagNode[] tarStarImg = tarStar[0].getElementsByName("img", true);
                	if(tarStarImg.length >0 && !tarStarImg[0].getAttributeByName("src").contains("star-off")){
                		if(tarStarImg[0].getAttributeByName("src").contains("star0")){
                    		thread.put(BOOKMARKED, 1);
                		}else if(tarStarImg[0].getAttributeByName("src").contains("star1")){
                    		thread.put(BOOKMARKED, 2);
                		}else if(tarStarImg[0].getAttributeByName("src").contains("star2")){
                    		thread.put(BOOKMARKED, 3);
                		}
                	}else{
                		thread.put(BOOKMARKED, 0);
                	}
                }else{
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
	
	public static ArrayList<ContentValues> parseSubforums(TagNode aResponse, int parentForumId){
        ArrayList<ContentValues> result = new ArrayList<ContentValues>();
		TagNode[] subforums = aResponse.getElementsByAttValue("class", "subforum", true, false);
        for(TagNode sf : subforums){
        	TagNode[] href = sf.getElementsHavingAttribute("href", true);
        	if(href.length <1){
        		continue;
        	}
        	int id = Integer.parseInt(href[0].getAttributeByName("href").replaceAll("\\D", ""));
        	if(id > 0){
        		ContentValues tmp = new ContentValues();
        		tmp.put(AwfulForum.ID, id);
        		tmp.put(AwfulForum.PARENT_ID, parentForumId);
        		tmp.put(AwfulForum.TITLE, href[0].getText().toString());
        		TagNode[] subtext = sf.getElementsByName("dd", true);
        		if(subtext.length >0){
        			//Log.i(TAG,"parsed subtext: "+subtext[0].getText().toString().replaceAll("\"", "").trim().substring(2));
        			tmp.put(AwfulForum.SUBTEXT, subtext[0].getText().toString().replaceAll("\"", "").trim().substring(2));//ugh
        		}
        		result.add(tmp);
        	}
        }
        return result;
    }

    public static void getThreadPosts(Context aContext, int aThreadId, int aPage, int aPageSize, AwfulPreferences aPrefs, int aUserId, Messenger statusUpdates) throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(Constants.PARAM_THREAD_ID, Integer.toString(aThreadId));
        params.put(Constants.PARAM_PER_PAGE, Integer.toString(aPageSize));
        params.put(Constants.PARAM_PAGE, Integer.toString(aPage));
        params.put(Constants.PARAM_USER_ID, Integer.toString(aUserId));
        
        ContentResolver contentResolv = aContext.getContentResolver();
		Cursor threadData = contentResolv.query(ContentUris.withAppendedId(CONTENT_URI, aThreadId), AwfulProvider.ThreadProjection, null, null, null);
    	int totalReplies = 0, unread = -1, opId = 0, bookmarkStatus = 0;
		if(threadData.moveToFirst()){
			totalReplies = threadData.getInt(threadData.getColumnIndex(POSTCOUNT));
			unread = threadData.getInt(threadData.getColumnIndex(UNREADCOUNT));
			opId = threadData.getInt(threadData.getColumnIndex(AUTHOR_ID));
			bookmarkStatus = threadData.getInt(threadData.getColumnIndex(BOOKMARKED));
		}
        
        //notify user we are starting update
        statusUpdates.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, aThreadId, 10));
        
        Document response = NetworkUtils.getJSoup(Constants.FUNCTION_THREAD, params, statusUpdates, 25);

        //notify user we have gotten message body, this represents a large portion of this function
        statusUpdates.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, aThreadId, 50));
        
        if(AwfulPagedItem.checkPageErrors(response, statusUpdates)){
        	return;//errors found, skip processing.
        }
        
        ContentValues thread = new ContentValues();
        thread.put(ID, aThreadId);
    	Elements tarTitle = response.getElementsByAttributeValue("class", "bclast");
        if (tarTitle.size() > 0) {
        	thread.put(TITLE, tarTitle.get(0).text().trim());
        }
            
        Elements replyAlts = response.getElementsByAttributeValue("alt", "Reply");
        if (replyAlts.size() >0 && replyAlts.get(0).attr("src").contains("forum-closed")) {
        	thread.put(LOCKED, 1);
        }else{
        	thread.put(LOCKED, 0);
        }

        Elements bkButtons = response.getElementsByAttributeValue("id", "button_bookmark");
        if (bkButtons.size() >0) {
        	String bkSrc = bkButtons.get(0).attr("src");
        	if(bkSrc != null && bkSrc.contains("unbookmark")){
        		if(bookmarkStatus < 1){
        			thread.put(BOOKMARKED, 1);
        		}
        	}else{
        		thread.put(BOOKMARKED, 0);
        	}
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
    	Log.v(TAG, "Parsed lastPage:"+lastPage+" old total: "+totalReplies+" new total:"+replycount);
    	
    	thread.put(AwfulThread.POSTCOUNT, replycount);
    	int newUnread = Math.max(0, replycount-AwfulPagedItem.pageToIndex(aPage, aPageSize, aPageSize-1));
    	if(unread >= 0){
        	newUnread = Math.min(unread, newUnread);
    	}
    	if(aPage == lastPage){
    		newUnread = 0;
    	}
    	thread.put(AwfulThread.UNREADCOUNT, newUnread);
    	Log.i(TAG, aThreadId+" - Old unread: "+unread+" new unread: "+newUnread);

        //notify user we have began processing posts
        statusUpdates.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, aThreadId, 65));
        
        AwfulPost.syncPosts(contentResolv, 
        					response, 
        					aThreadId, 
        					(unread < 0 ? 0 : totalReplies-unread),
        					opId, 
        					aPrefs,
        					AwfulPagedItem.pageToIndex(aPage, aPageSize, 0));
        
    	if(contentResolv.update(ContentUris.withAppendedId(CONTENT_URI, aThreadId), thread, null, null) <1){
    		contentResolv.insert(CONTENT_URI, thread);
    	}
    	
        //notify user we are done with this stage
        statusUpdates.send(Message.obtain(null, AwfulSyncService.MSG_PROGRESS_PERCENT, aThreadId, 100));
    }

    public static String getHtml(ArrayList<AwfulPost> aPosts, AwfulPreferences aPrefs, boolean isTablet, int page, int lastPage, boolean threadLocked) {
        StringBuffer buffer = new StringBuffer("<html>\n<head>\n");
        buffer.append("<meta name='viewport' content='width=device-width, height=device-height, target-densitydpi=device-dpi, initial-scale=1.0 maximum-scale=1.0 minimum-scale=1.0' />\n");
        buffer.append("<meta name='format-detection' content='telephone=no' />\n");
        buffer.append("<meta name='format-detection' content='address=no' />\n");
        
        buffer.append("<link rel='stylesheet' href='file:///android_asset/thread.css'>\n");
        
        if (!isTablet) {
            buffer.append("<link rel='stylesheet' href='file:///android_asset/thread-phone.css'>");
            buffer.append("<link rel='stylesheet' media='screen and (-webkit-device-pixel-ratio:1.5)' href='file:///android_asset/thread-hdpi.css' />\n");
            buffer.append("<link rel='stylesheet' media='screen and (-webkit-device-pixel-ratio:1)' href='file:///android_asset/thread-mdpi.css' />\n");
            buffer.append("<link rel='stylesheet' media='screen and (-webkit-device-pixel-ratio:.75)' href='file:///android_asset/thread-mdpi.css' />\n");
        }
        if(!aPrefs.preferredFont.contains("default")){
        	buffer.append("<style type='text/css'>@font-face { font-family: userselected; src: url('content://com.ferg.awfulapp.webprovider/"+aPrefs.preferredFont+"'); }</style>\n");
        }
        buffer.append("<script src='file:///android_asset/jquery.min.js' type='text/javascript'></script>\n");
        
        buffer.append("<script type='text/javascript'>\n");
        buffer.append("  window.JSON = null;");
        buffer.append("</script>\n");
        
        
        buffer.append("<script src='file:///android_asset/json2.js' type='text/javascript'></script>\n");
        buffer.append("<script src='file:///android_asset/ICanHaz.min.js' type='text/javascript'></script>\n");
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
        buffer.append("a:link {color: "+ColorPickerPreference.convertToARGB(aPrefs.postLinkQuoteColor)+" }\n");
        buffer.append("a:visited {color: "+ColorPickerPreference.convertToARGB(aPrefs.postLinkQuoteColor)+"}\n");
        buffer.append("a:active {color: "+ColorPickerPreference.convertToARGB(aPrefs.postLinkQuoteColor)+"}\n");
        buffer.append("a:hover {color: "+ColorPickerPreference.convertToARGB(aPrefs.postLinkQuoteColor)+"}\n");
        if(!aPrefs.postDividerEnabled){
            buffer.append(".userinfo-row {border-top-width:0px;}\n");
            buffer.append(".post-buttons {border-bottom-width:0px;}\n");
        }
        buffer.append(".bbc-block { border-bottom: 1px "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor)+" solid; }\n");
        buffer.append(".bbc-block h4 { border-top: 1px "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor)+" solid; color: "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor2)+"; }\n");
        buffer.append(".bbc-spoiler, .bbc-spoiler li, .bbc-spoiler a { color: "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor)+"; background: "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor)+";}\n");
        
        buffer.append("</style>\n");
        buffer.append("</head>\n<body>\n");
        buffer.append("<div class='content' >\n");
        buffer.append("    <table id='thread-body' style='font-size: " + aPrefs.postFontSizePx + "px; color: " + ColorPickerPreference.convertToARGB(aPrefs.postFontColor) + ";'>\n");

        if (isTablet) {
            buffer.append(AwfulThread.getPostsHtmlForTablet(aPosts, aPrefs, threadLocked));
        } else {
            buffer.append(AwfulThread.getPostsHtmlForPhone(aPosts, aPrefs, threadLocked));
        }
        buffer.append("    </table>");

        if(page >= lastPage){
        	buffer.append("<div class='unread' ></div>\n");
        }else{
        	//buffer.append("<a class='nextpage' href='http://next.next' style='border-color:"+ColorPickerPreference.convertToARGB(aPrefs.postDividerColor)+";border-top:1px;text-decoration:none;color:"+ColorPickerPreference.convertToARGB(aPrefs.postFontColor)+";background-color:"+ColorPickerPreference.convertToARGB(aPrefs.postBackgroundColor)+"; position:relative;display:block;text-align:center; width:100%; height:60px'><div style='font-size:32px;margin-top:-16px;text-decoration:none;position:absolute;text-align:center;top:50%;width:100%;'>"+(lastPage - page)+" Page"+(lastPage - page > 1? "s":"")+" Remaining</div></a>");
        }
        buffer.append("</div>\n");
        buffer.append("</body>\n</html>\n");

        return buffer.toString();
    }

    public static String getPostsHtmlForPhone(ArrayList<AwfulPost> aPosts, AwfulPreferences aPrefs, boolean threadLocked) {
        StringBuffer buffer = new StringBuffer();

        boolean light = true;
        String background = null;

        for (AwfulPost post : aPosts) {
        
            if (post.isPreviouslyRead()) {
                background = 
                    ColorPickerPreference.convertToARGB(light ? aPrefs.postReadBackgroundColor : aPrefs.postReadBackgroundColor2);
            } else {
                background = 
                    ColorPickerPreference.convertToARGB(light ? aPrefs.postBackgroundColor : aPrefs.postBackgroundColor2);
            }

            if(aPrefs.alternateBackground == true){
            	light = !light;
            }

            buffer.append("<tr class='" + (post.isPreviouslyRead() ? "read" : "unread") + "' id='" + post.getId() + "'>\n");
            buffer.append("    <td class='userinfo-row' style='width: 100%; color: "+ColorPickerPreference.convertToARGB(aPrefs.postHeaderFontColor)+"; border-color:"+ColorPickerPreference.convertToARGB(aPrefs.postDividerColor)+";background-color:"+(post.isOp()?ColorPickerPreference.convertToARGB(aPrefs.postOPColor):ColorPickerPreference.convertToARGB(aPrefs.postHeaderBackgroundColor))+"'>\n");
            buffer.append("        <div class='avatar' "+((aPrefs.avatarsEnabled != false && post.getAvatar() != null)?"style='height: 100px; width: 100px; background-image:url("+post.getAvatar()+");'":"")+">\n");
            buffer.append("        </div>\n");
            buffer.append("        <div class='userinfo'>\n");
            buffer.append("            <div class='username' >\n");
            buffer.append("                <h4>" + post.getUsername() + (post.isMod()?"<img src='file:///android_res/drawable/ic_star_blue.png' />":"")+ (post.isAdmin()?"<img src='file:///android_res/drawable/ic_star_red.png' />":"")  +  "</h4>\n");
            buffer.append("            </div>");
            buffer.append("            <div class='postdate' >\n");
            buffer.append("                " + post.getDate());
            buffer.append("            </div>\n");
            buffer.append("        </div>\n");
            buffer.append("        <div class='action-button' >\n");
            buffer.append("            <img src='file:///android_res/drawable/"+aPrefs.icon_theme+"_inline_more.png' />\n");
            buffer.append("        </div>\n");
            buffer.append("    </td>\n");
            buffer.append("</tr>\n");
            buffer.append("<tr>\n");
            buffer.append("    <td class='post-buttons' style='border-color:"+ColorPickerPreference.convertToARGB(aPrefs.postDividerColor)+";background: "+(post.isOp()?ColorPickerPreference.convertToARGB(aPrefs.postOPColor):ColorPickerPreference.convertToARGB(aPrefs.postHeaderBackgroundColor))+";'>\n");
            buffer.append("        <div class='avatar-text-phone' style='display:none;float: left; width: 100%;overflow: hidden; color: "+ColorPickerPreference.convertToARGB(aPrefs.postHeaderFontColor)+";'>\n"+(post.getAvatarText()!= null?post.getAvatarText()+"<br/>":""));
            if(post.isEditable()){
            	buffer.append("        		<div class='"+(threadLocked?"":"edit_button ")+"inline-button' id='" + post.getId() + "' />\n");
                buffer.append("        			<img src='file:///android_res/drawable/"+aPrefs.icon_theme+"_inline_edit.png' style='position:relative;vertical-align:middle;' /> "+(threadLocked?"Locked":"Edit"));
                buffer.append("        		</div>\n");
            }
        	buffer.append("        		<div class='"+(threadLocked?"":"quote_button ")+"inline-button' id='" + post.getId() + "' />\n");
            buffer.append("        			<img src='file:///android_res/drawable/"+aPrefs.icon_theme+"_inline_quote.png' style='position:relative;vertical-align:middle;' /> "+(threadLocked?"Locked":"Quote"));
            buffer.append("\n        		</div>\n");
            buffer.append("        		<div class='lastread_button inline-button' lastreadurl='" + post.getLastReadUrl() + "' />\n");
            buffer.append("        			<img src='file:///android_res/drawable/"+aPrefs.icon_theme+"_inline_lastread.png' style='position:relative;vertical-align:middle;' />Last Read\n");
            buffer.append("        		</div>\n");
            buffer.append("        		<div class='more_button inline-button' id='" + post.getId() + "' username='" + post.getUsername() + "' userid='" + post.getUserId() + "' >\n");
            buffer.append("        			<img src='file:///android_res/drawable/"+aPrefs.icon_theme+"_inline_more.png' style='position:relative;vertical-align:middle;' /> More\n");
            buffer.append("        		</div>\n");
            buffer.append("        </div>\n");
            buffer.append("    </td>\n");
            buffer.append("</tr>\n");
            buffer.append("<tr>\n");
            buffer.append("    <td class='post-cell' colspan='2' style='background: " + background + ";'>\n");
            buffer.append("        <div class='post-content' style='color: " + ColorPickerPreference.convertToARGB((post.isPreviouslyRead() ? aPrefs.postReadFontColor : aPrefs.postFontColor)) + ";'>\n");
            buffer.append("            " + post.getContent());
            buffer.append("\n        </div>\n");
            buffer.append("    </td>\n");
            buffer.append("</tr>\n");
        }

        return buffer.toString();
    }

    public static String getPostsHtmlForTablet(ArrayList<AwfulPost> aPosts, AwfulPreferences aPrefs, boolean threadLocked) {
        StringBuffer buffer = new StringBuffer();

        boolean light = true;
        String background = null;

        for (AwfulPost post : aPosts) {
        
            if (post.isPreviouslyRead()) {
                background = 
                    ColorPickerPreference.convertToARGB(light ? aPrefs.postReadBackgroundColor : aPrefs.postReadBackgroundColor2);
            } else {
                background = 
                    ColorPickerPreference.convertToARGB(light ? aPrefs.postBackgroundColor : aPrefs.postBackgroundColor2);
            }

            if(aPrefs.alternateBackground == true){
            	light = !light;
            }

            buffer.append("<tr class='" + (post.isPreviouslyRead() ? "read" : "unread") + "'>\n");
            buffer.append("    <td class='usercolumn' style='background: " + background + ";color: " + ColorPickerPreference.convertToARGB(aPrefs.postFontColor) + ";'>\n");
            buffer.append("        	<div class='user-button' >\n");
            buffer.append("        		<div class='userinfo'>\n");
            buffer.append("            		<div class='username' " + (post.isOp() ? "style='color: " + ColorPickerPreference.convertToARGB(aPrefs.postOPColor) + ";'" : "") + ">\n");
            buffer.append("                		<h4>" + post.getUsername() + ((post.isMod())?"<img src='file:///android_res/drawable/ic_star_blue.png' />":"")+ ((post.isAdmin())?"<img src='file:///android_res/drawable/ic_star_red.png' />":"")  + "</h4>\n");
            buffer.append("            		</div>");
            buffer.append("            		<div class='postdate' " + (post.isOp() ? "style='color: " + ColorPickerPreference.convertToARGB(aPrefs.postOPColor) + ";'" : "") + ">\n");
            buffer.append("           		     " + post.getDate());
            buffer.append("            		</div>\n");
            buffer.append("        		</div>\n");
            buffer.append("        		<div class='avatar'>\n");

            if (aPrefs.avatarsEnabled != false && post.getAvatar() != null) {
                buffer.append("            <img class='gif' src='" + post.getAvatar() + "' />\n");
            }else{
                buffer.append("            <img src='file:///android_res/drawable/"+aPrefs.icon_theme+"_inline_more.png' />\n");
            }

            buffer.append("        		</div>\n");
            buffer.append("        </div>\n");
            buffer.append("        <div class='avatar-text' style='display:none;float: left; width: 100%;overflow: hidden;'>"+(post.getAvatarText()!= null?post.getAvatarText()+"<br/>":""));
            buffer.append("        </div>\n");
            buffer.append("    </td>\n");
            buffer.append("    <td class='post-cell' style='background: " + background + ";'>\n");
            buffer.append("    		<div class='button-row' style='display: none; width: 100%;color: " + ColorPickerPreference.convertToARGB(aPrefs.postFontColor) + ";'>\n");
            if(post.isEditable()){
            	buffer.append("        		<div class='edit_button inline-button' id='" + post.getId() + "' />\n");
                buffer.append("        			<img src='file:///android_res/drawable/"+aPrefs.icon_theme+"_inline_edit.png' style='position:relative;vertical-align:middle;' /> Edit\n");
                buffer.append("        		</div>\n");
            }else{
            	buffer.append("        		<div class='quote_button inline-button' id='" + post.getId() + "' />\n");
                buffer.append("        			<img src='file:///android_res/drawable/"+aPrefs.icon_theme+"_inline_quote.png' style='position:relative;vertical-align:middle;' /> Quote");
                buffer.append("        		</div>\n");
            }
            buffer.append("        		<div class='lastread_button inline-button' lastreadurl='" + post.getLastReadUrl() + "' />\n");
            buffer.append("        			<img src='file:///android_res/drawable/"+aPrefs.icon_theme+"_inline_lastread.png' style='position:relative;vertical-align:middle;' />Last Read");
            buffer.append("        		</div>\n");
            buffer.append("        		<div class='more_button inline-button' id='" + post.getId() + "' username='" + post.getUsername() + "' userid='" + post.getUserId() + "' >\n");
            buffer.append("        			<img src='file:///android_res/drawable/"+aPrefs.icon_theme+"_inline_more.png' style='position:relative;vertical-align:middle;' /> More");
            buffer.append("        		</div>\n");
            buffer.append("    			<hr style='clear:both;' />\n");
            buffer.append("    		</div>\n");
            buffer.append("        <div class='post-content'>\n");
            buffer.append("            " + post.getContent());
            buffer.append("        </div>\n");
            buffer.append("    </td>\n");
            buffer.append("</tr>\n");
        }

        return buffer.toString();
    }

	public static void getView(View current, AwfulPreferences prefs, Cursor data, AQuery aq, boolean hideBookmark, boolean selected) {
		aq.recycle(current);
		TextView info = (TextView) current.findViewById(R.id.threadinfo);
		ImageView sticky = (ImageView) current.findViewById(R.id.sticky_icon);
		ImageView bookmark = (ImageView) current.findViewById(R.id.bookmark_icon);
		TextView title = (TextView) current.findViewById(R.id.title);
		boolean stuck = data.getInt(data.getColumnIndex(STICKY)) >0;
		if(stuck){
			sticky.setImageResource(R.drawable.ic_sticky);
			sticky.setVisibility(View.VISIBLE);
		}else{
			sticky.setVisibility(View.GONE);
		}
		
		if(!prefs.threadInfo_Tag || !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			aq.id(R.id.thread_tag).gone();
		}else{
			String tagFile = data.getString(data.getColumnIndex(TAG_CACHEFILE));
			if(tagFile != null){
				aq.id(R.id.thread_tag).visible().image(data.getString(data.getColumnIndex(TAG_URL)), true, true);
			}else{
				aq.id(R.id.thread_tag).gone();
			}
		}

		if(!prefs.threadInfo_Author && !prefs.threadInfo_Killed && !prefs.threadInfo_Page){
			info.setVisibility(View.GONE);
		}else{
			info.setVisibility(View.VISIBLE);
			StringBuilder tmp = new StringBuilder();
			if(prefs.threadInfo_Page){
				tmp.append(AwfulPagedItem.indexToPage(data.getInt(data.getColumnIndex(POSTCOUNT)), prefs.postPerPage)+" pgs");	
			}
			if(prefs.threadInfo_Killed){
				if(tmp.length()>0){
					tmp.append(" | ");
				}
				tmp.append("Last: "+data.getString(data.getColumnIndex(LASTPOSTER)));
			}
			if(prefs.threadInfo_Author){
				if(tmp.length()>0){
					tmp.append(" | ");
				}
				tmp.append("OP: "+data.getString(data.getColumnIndex(AUTHOR)));
			}
			info.setText(tmp.toString().trim());
		}
		int mark = data.getInt(data.getColumnIndex(BOOKMARKED));
		if(mark > 1 || (!hideBookmark && mark == 1)){
			switch(mark){
			case 1:
				bookmark.setImageResource(R.drawable.ic_star_blue);
				break;
			case 2:
				bookmark.setImageResource(R.drawable.ic_star_red);
				break;
			case 3:
				bookmark.setImageResource(R.drawable.ic_star_gold);
				break;
			}
			bookmark.setVisibility(View.VISIBLE);
			if(!stuck){
				bookmark.setPadding(0, 5, 4, 0);
			}
		}else{
			if(!stuck){
				bookmark.setVisibility(View.GONE);
			}else{
				bookmark.setVisibility(View.INVISIBLE);
			}
			
		}
		
		if(selected){
			current.findViewById(R.id.selector).setVisibility(View.VISIBLE);
		}else{
			current.findViewById(R.id.selector).setVisibility(View.GONE);
		}
		
		TextView unread = (TextView) current.findViewById(R.id.unread_count);
		int unreadCount = data.getInt(data.getColumnIndex(UNREADCOUNT));
		if(unreadCount >=0){
			unread.setVisibility(View.VISIBLE);
			unread.setText(unreadCount+"");
            if (unreadCount == 0){
                unread.setBackgroundResource(R.drawable.unread_background_dim);
            }else{
                unread.setBackgroundResource(R.drawable.unread_background);
            }
		}else{
			unread.setVisibility(View.GONE);
		}
		title.setTypeface(null, Typeface.NORMAL);
		if(data.getString(data.getColumnIndex(TITLE)) != null){
			title.setText(Html.fromHtml(data.getString(data.getColumnIndex(TITLE))));
		}
		if(prefs != null){
			title.setTextColor(prefs.postFontColor);
			info.setTextColor(prefs.postFontColor2);
			title.setSingleLine(!prefs.wrapThreadTitles);
			if(!prefs.wrapThreadTitles){
				title.setEllipsize(TruncateAt.END);
			}else{
				title.setEllipsize(null);
			}
		}
		
		if(data.getInt(data.getColumnIndex(LOCKED)) > 0){
			aq.find(R.id.forum_tag).image(R.drawable.light_inline_link).visible().width(15);
		}else{
			aq.find(R.id.forum_tag).gone();
		}
	}
	
	public static boolean threadTagExists(Context aContext, String filename){
		try{
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
				File cacheDir;
				if(Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO){
					cacheDir = new File(Environment.getExternalStorageDirectory(),"Android/data/com.ferg.awfulapp/cache/category/");
				}else{
					cacheDir = new File(aContext.getExternalCacheDir(),"category/");
				}
				File cachedImg = new File(cacheDir, filename);
				if(cachedImg.exists() && cachedImg.canRead()){
					return true;
				}
			}
		}catch(Exception e){
		}
		return false;
	}
	
	public static Bitmap getCategory(Context aContext, String fileName){
		try{
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
				File cacheDir;
				if(Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO){
					cacheDir = new File(Environment.getExternalStorageDirectory(),"Android/data/com.ferg.awfulapp/cache/category/");
				}else{
					cacheDir = new File(aContext.getExternalCacheDir(),"category/");
				}
				File cachedImg = new File(cacheDir, fileName);
				if(cachedImg.exists() && cachedImg.canRead()){
					FileInputStream is = new FileInputStream(cachedImg);
					Bitmap data = BitmapFactory.decodeStream(is);
					is.close();
					return data;
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static void setBitmap(Context context, ImageView imgView, HashMap<String, Bitmap> imageCache){
		if(imgView.getTag() instanceof String){
			String imageName = (String) imgView.getTag();
			Bitmap image = imageCache.get(imageName);
			if(image == null){
				image = getCategory(context, imageName);
				imageCache.put(imageName, image);
			}
			if(image != null){
				imgView.setVisibility(View.VISIBLE);
				imgView.setScaleType(ScaleType.FIT_XY);
				imgView.setImageBitmap(image);
			}
		}
	}
	
}
