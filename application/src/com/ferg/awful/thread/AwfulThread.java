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

package com.ferg.awful.thread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlcleaner.TagNode;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.*;

import com.ferg.awful.R;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.preferences.ColorPickerPreference;
import com.ferg.awful.provider.AwfulProvider;

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

    public static final String TAG_URL 		="tag_url";
    public static final String TAG_CACHEFILE 	="tag_cachefile";
	
	private static final Pattern forumId_regex = Pattern.compile("forumid=(\\d+)");
	private static final Pattern urlId_regex = Pattern.compile("([^#]+)#(\\d+)$");

    
    public static TagNode getForumThreads(int aForumId) throws Exception {
		return getForumThreads(aForumId, 1);
	}
	
    public static TagNode getForumThreads(int aForumId, int aPage) throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(Constants.PARAM_FORUM_ID, Integer.toString(aForumId));

		if (aPage != 0) {
			params.put(Constants.PARAM_PAGE, Integer.toString(aPage));
		}

        return NetworkUtils.get(Constants.FUNCTION_FORUM, params);
	}
	
    public static TagNode getUserCPThreads(int aPage) throws Exception {
    	HashMap<String, String> params = new HashMap<String, String>();
		params.put(Constants.PARAM_PAGE, Integer.toString(aPage));
        return NetworkUtils.get(Constants.FUNCTION_BOOKMARK, params);
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
                		thread.put(BOOKMARKED, 1);
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

    public static void getThreadPosts(Context aContext, int aThreadId, int aPage, int aPageSize, AwfulPreferences aPrefs) throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(Constants.PARAM_THREAD_ID, Integer.toString(aThreadId));
        params.put(Constants.PARAM_PER_PAGE, Integer.toString(aPageSize));
        params.put(Constants.PARAM_PAGE, Integer.toString(aPage));

        TagNode response = NetworkUtils.get(Constants.FUNCTION_THREAD, params);
        ContentValues thread = new ContentValues();
        thread.put(ID, aThreadId);
        if (true /* mTitle == null */) {
        	TagNode[] tarTitle = response.getElementsByAttValue("class", "bclast", true, true);

            if (tarTitle.length > 0) {
            	thread.put(TITLE, tarTitle[0].getText().toString().trim());
            }
        }

        TagNode[] replyAlts = response.getElementsByAttValue("alt", "Reply", true, true);
        if (replyAlts.length >0 && replyAlts[0].getAttributeByName("src").contains("forum-closed")) {
        	thread.put(LOCKED, 1);
        }else{
        	thread.put(LOCKED, 0);
        }

        TagNode[] bkButtons = response.getElementsByAttValue("id", "button_bookmark", true, true);
        if (bkButtons.length >0) {
        	String bkSrc = bkButtons[0].getAttributeByName("src");
        	thread.put(BOOKMARKED, bkSrc != null && bkSrc.contains("unbookmark"));
        }
        TagNode breadcrumbs = response.findElementByAttValue("class", "breadcrumbs", true, true);
    	TagNode[] forumlinks = breadcrumbs.getElementsHavingAttribute("href", true);
    	int forumId = -1;
    	for(TagNode fl : forumlinks){
    		Matcher matchForumId = forumId_regex.matcher(fl.getAttributeByName("href"));
    		if(matchForumId.find()){//switched this to a regex
    			forumId = Integer.parseInt(matchForumId.group(1));//so this won't fail
    		}
    	}
    	thread.put(FORUM_ID, forumId);
    	int lastPage = AwfulPagedItem.parseLastPage(response);
    	

    	ContentResolver contentResolv = aContext.getContentResolver();
		Cursor threadData = contentResolv.query(ContentUris.withAppendedId(CONTENT_URI, aThreadId), AwfulProvider.ThreadProjection, null, null, null);
		int totalReplies = 0, unread = -1, opId = 0;
		if(threadData.moveToFirst()){
			totalReplies = threadData.getInt(threadData.getColumnIndex(POSTCOUNT));
			unread = threadData.getInt(threadData.getColumnIndex(UNREADCOUNT));
			opId = threadData.getInt(threadData.getColumnIndex(AUTHOR_ID));
		}
    	int replycount = Math.max(totalReplies, AwfulPagedItem.pageToIndex(lastPage, aPageSize, 0));
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
    }

    public static String getHtml(ArrayList<AwfulPost> aPosts, AwfulPreferences aPrefs, boolean isTablet, boolean lastPage) {
        StringBuffer buffer = new StringBuffer("<html><head>");
        buffer.append("<meta name='viewport' content='width=device-width, height=device-height, target-densitydpi=device-dpi, initial-scale=1.0 maximum-scale=1.0 minimum-scale=1.0' />");
        buffer.append("<link rel='stylesheet' href='file:///android_asset/thread.css'>");
        
        if (!isTablet) {
            buffer.append("<link rel='stylesheet' href='file:///android_asset/thread-phone.css'>");
            buffer.append("<link rel='stylesheet' media='screen and (-webkit-device-pixel-ratio:1.5)' href='file:///android_asset/thread-hdpi.css' />");
            buffer.append("<link rel='stylesheet' media='screen and (-webkit-device-pixel-ratio:1)' href='file:///android_asset/thread-mdpi.css' />");
            buffer.append("<link rel='stylesheet' media='screen and (-webkit-device-pixel-ratio:.75)' href='file:///android_asset/thread-mdpi.css' />");
        }
        
        buffer.append("<style type='text/css'>@font-face { font-family: userselected; src: url('content://com.ferg.awful.webprovider/"+aPrefs.preferredFont+"'); }</style>");
        
        buffer.append("<script src='file:///android_asset/jquery.min.js' type='text/javascript'></script>");
        buffer.append("<script type='text/javascript'>");
        buffer.append("  window.JSON = null;");
        buffer.append("</script>");
        buffer.append("<script src='file:///android_asset/json2.js' type='text/javascript'></script>");
        buffer.append("<script src='file:///android_asset/ICanHaz.min.js' type='text/javascript'></script>");
        buffer.append("<script src='file:///android_asset/salr.js' type='text/javascript'></script>");
        buffer.append("<script src='file:///android_asset/thread.js' type='text/javascript'></script>");
        buffer.append("<style type='text/css'>");   
        buffer.append("a:link {color: "+ColorPickerPreference.convertToARGB(aPrefs.postLinkQuoteColor)+" }");
        buffer.append("a:visited {color: "+ColorPickerPreference.convertToARGB(aPrefs.postLinkQuoteColor)+"}");
        buffer.append("a:active {color: "+ColorPickerPreference.convertToARGB(aPrefs.postLinkQuoteColor)+"}");
        buffer.append("a:hover {color: "+ColorPickerPreference.convertToARGB(aPrefs.postLinkQuoteColor)+"}");
        buffer.append(".bbc-block { border-bottom: 1px "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor)+" solid; }");
        buffer.append(".bbc-block h4 { border-top: 1px "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor)+" solid; color: "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor2)+"; }");
        buffer.append(".bbc-spoiler, .bbc-spoiler li, .bbc-spoiler a { color: "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor)+"; background: "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor)+";}");
        
        buffer.append("</style>");
        buffer.append("</head><body>");
        buffer.append("<div class='content'>");
        buffer.append("    <table id='thread-body'>");

        if (isTablet) {
            buffer.append(AwfulThread.getPostsHtmlForTablet(aPosts, aPrefs));
        } else {
            buffer.append(AwfulThread.getPostsHtmlForPhone(aPosts, aPrefs));
        }

        buffer.append("    </table>");
        if(lastPage){
        	buffer.append("<div class='unread' ></div>");
        }
        buffer.append("</div>");
        buffer.append("</body></html>");

        return buffer.toString();
    }

    public static String getPostsHtmlForPhone(ArrayList<AwfulPost> aPosts, AwfulPreferences aPrefs) {
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

            buffer.append("<tr class='" + (post.isPreviouslyRead() ? "read" : "unread") + "' id='" + post.getId() + "'>");
            buffer.append("    <td class='userinfo-row' style='width: 100%;"+(post.isOp()?"background-color:"+ColorPickerPreference.convertToARGB(aPrefs.postOPColor):"")+"'>");
            buffer.append("        <div class='avatar' "+((aPrefs.imagesEnabled != false && post.getAvatar() != null)?"style='height: 100px; width: 100px; background-image:url("+post.getAvatar()+");'":"")+">");
            buffer.append("        </div>");
            buffer.append("        <div class='userinfo'>");
            buffer.append("            <div class='username'>");
            buffer.append("                <h4>" + post.getUsername() + ((post.isMod())?"<img src='file:///android_res/drawable/blue_star.png' />":"")+ ((post.isAdmin())?"<img src='file:///android_res/drawable/red_star.png' />":"")  +  "</h4>");
            buffer.append("            </div>");
            buffer.append("            <div class='postdate'>");
            buffer.append("                " + post.getDate());
            buffer.append("            </div>");
            buffer.append("        </div>");
            buffer.append("        <div class='action-button " + (post.isEditable() ? "editable" : "noneditable") + "' id='" + post.getId() + "' lastreadurl='" + post.getLastReadUrl() + "' username='" + post.getUsername() + "'>");
            buffer.append("            <img src='file:///android_asset/post_action_icon.png' />");
            buffer.append("        </div>");
            buffer.append("        <div class='avatar-text' style='display:none;float: left; width: 100%;overflow: hidden;'>"+(post.getAvatarText()!= null?post.getAvatarText():""));
            buffer.append("        </div>");
            buffer.append("    </td>");
            buffer.append("</tr>");
            buffer.append("<tr>");
            buffer.append("    <td class='post-cell' colspan='2' style='background: " + background + ";'>");
            buffer.append("        <div class='post-content' style='color: " + ColorPickerPreference.convertToARGB(aPrefs.postFontColor) + "; font-size: " + aPrefs.postFontSize + ";'>");
            buffer.append("            " + post.getContent());
            buffer.append("        </div>");
            buffer.append("    </td>");
            buffer.append("</tr>");
        }

        return buffer.toString();
    }

    public static String getPostsHtmlForTablet(ArrayList<AwfulPost> aPosts, AwfulPreferences aPrefs) {
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

            buffer.append("<tr class='" + (post.isPreviouslyRead() ? "read" : "unread") + "'>");
            buffer.append("    <td class='usercolumn' style='background: " + background + ";'>");
            buffer.append("        <div class='userinfo'>");
            buffer.append("            <div class='username' " + (post.isOp() ? "style='color: " + ColorPickerPreference.convertToARGB(aPrefs.postOPColor) + ";" : "style='color: " + ColorPickerPreference.convertToARGB(aPrefs.postFontColor) + ";") + "'>");
            buffer.append("                <h4>" + post.getUsername() + ((post.isMod())?"<img src='file:///android_res/drawable/blue_star.png' />":"")+ ((post.isAdmin())?"<img src='file:///android_res/drawable/red_star.png' />":"")  + "</h4>");
            buffer.append("            </div>");
            buffer.append("            <div class='postdate' " + (post.isOp() ? "style='color: " + ColorPickerPreference.convertToARGB(aPrefs.postOPColor) + ";" :  "style='color: " + ColorPickerPreference.convertToARGB(aPrefs.postFontColor) + ";") + "'>");
            buffer.append("                " + post.getDate());
            buffer.append("            </div>");
            buffer.append("        </div>");
            buffer.append("        <div class='avatar'>");

            if (aPrefs.imagesEnabled != false && post.getAvatar() != null) {
                buffer.append("            <img src='" + post.getAvatar() + "' />");
            }

            buffer.append("        </div>");
            buffer.append("        <div class='avatar-text' style='display:none;float: left; width: 100%;overflow: hidden;color: " + ColorPickerPreference.convertToARGB(aPrefs.postFontColor) + ";'>"+(post.getAvatarText()!= null?post.getAvatarText():""));
            buffer.append("        </div>");
            buffer.append("    </td>");
            buffer.append("    <td class='post-cell' style='background: " + background + ";'>");
            buffer.append("        <div class='action-button " + (post.isEditable() ? "editable" : "noneditable") + "' id='" + post.getId() + "' lastreadurl='" + post.getLastReadUrl() + "' username='" + post.getUsername() + "'>");
            buffer.append("            <img src='file:///android_asset/post_action_icon.png' />");
            buffer.append("        </div>");
            buffer.append("        <div class='post-content' style='color: " + ColorPickerPreference.convertToARGB(aPrefs.postFontColor) + "; font-size: " + aPrefs.postFontSize + ";'>");
            buffer.append("            " + post.getContent());
            buffer.append("        </div>");
            buffer.append("    </td>");
            buffer.append("</tr>");
        }

        return buffer.toString();
    }

	public static String getView(View current, AwfulPreferences prefs, Cursor data, Context context, boolean hideBookmark) {
		String tag_url = null;
		TextView info = (TextView) current.findViewById(R.id.threadinfo);
		ImageView sticky = (ImageView) current.findViewById(R.id.sticky_icon);
		ImageView bookmark = (ImageView) current.findViewById(R.id.bookmark_icon);
		boolean stuck = data.getInt(data.getColumnIndex(STICKY)) >0;
		if(stuck){
			sticky.setImageResource(R.drawable.sticky);
			sticky.setVisibility(View.VISIBLE);
		}else{
			sticky.setVisibility(View.GONE);
		}

		ImageView threadTag = (ImageView) current.findViewById(R.id.thread_tag);
		if(!prefs.threadInfo_Tag){
			threadTag.setVisibility(View.GONE);
		}else{
			String tagFile = data.getString(data.getColumnIndex(TAG_CACHEFILE));
			Bitmap tagImg = null;
			if(tagFile != null){
				tagImg = getCategory(context, tagFile);
				if(tagImg != null){
					threadTag.setVisibility(View.VISIBLE);
					threadTag.setImageBitmap(tagImg);
				}else{
					threadTag.setVisibility(View.GONE);
					tag_url = data.getString(data.getColumnIndex(TAG_URL));
				}			
			}else{
				threadTag.setVisibility(View.GONE);
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
		if(!hideBookmark && data.getInt(data.getColumnIndex(BOOKMARKED)) >0){
			bookmark.setImageResource(R.drawable.blue_star);
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
		TextView title = (TextView) current.findViewById(R.id.title);
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
		return tag_url;
	}
	
	public static Bitmap getCategory(Context aContext, String fileName){
		try{
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
				File cacheDir;
				if(Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO){
					cacheDir = new File(Environment.getExternalStorageDirectory(),"Android/data/com.ferg.awful/cache/category/");
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
	
}
