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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.ferg.awfulapp.ForumsIndexFragment.ForumEntry;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;

public class AwfulForum extends AwfulPagedItem {
    private static final String TAG = "AwfulForum";

    public static final String PATH     = "/forum";
    public static final Uri CONTENT_URI = Uri.parse("content://" + Constants.AUTHORITY + PATH);

	public static final String ID      = "_id";
	public static final String PARENT_ID      = "parent_forum_id";
	public static final String INDEX = "forum_index";//for ordering by
	public static final String TITLE   = "title";
	public static final String SUBTEXT = "subtext";
	public static final String PAGE_COUNT = "page_count";
    public static final String TAG_URL 		="tag_url";
    public static final String TAG_CACHEFILE 	="tag_cachefile";

	//private static final String FORUM_ROW   = "//table[@id='forums']//tr";
	//private static final String FORUM_TITLE = "//a[@class='forum']";
    //private static final String SUBFORUM    = "//div[@class='subforums']//a";

	private static final Pattern forumId_regex = Pattern.compile("forumid=(\\d+)");
	private static final Pattern forumTitle_regex = Pattern.compile("(.+)-{1}.+$");

	public static void getForumsFromRemote(Document response, ContentResolver contentInterface){
		ArrayList<ContentValues> result = new ArrayList<ContentValues>();

        String update_time = new Timestamp(System.currentTimeMillis()).toString();
        
		ContentValues bookmarks = new ContentValues();
		bookmarks.put(ID, Constants.USERCP_ID);
		bookmarks.put(TITLE, "Bookmarks");
		bookmarks.put(PARENT_ID, 0);
		bookmarks.put(INDEX, 0);
		bookmarks.put(AwfulProvider.UPDATED_TIMESTAMP, update_time);
		result.add(bookmarks);
		
		int ix = 1;
		Elements forumObjects = response.getElementById("forums").getElementsByTag("tr");
		for (Element node : forumObjects) {
			try{
				ContentValues forum = new ContentValues();
				int forumId = 0;
	            // First, grab the parent forum
				Element title = node.getElementsByClass("forum").first();
	            if (title != null) {
	            	//the title node also has a forum class, so we want the 2nd node with a forum class
	            	Element parentForum = title.getElementsByClass("forum").get(1);
	                forum.put(TITLE,parentForum.text());
	                forum.put(PARENT_ID, 0);
	                forum.put(INDEX, ix);
	                ix++;
	                // Just nix the part we don't need to get the forum ID
	                String id = parentForum.attr("href");
	                forumId=getForumId(id);
	                forum.put(ID,forumId);
	                forum.put(SUBTEXT,parentForum.attr("title"));
	            }
	            Element tarIcon = node.getElementsByClass("icon").first();
                if (tarIcon != null) {
                	Element imgTag = tarIcon.getElementsByTag("img").first();
                	if(imgTag != null && imgTag.hasAttr("src")){
	                    String url = imgTag.attr("src");
	                    if(url != null){
	                    	//thread tag stuff
	        				Matcher fileNameMatcher = AwfulEmote.fileName_regex.matcher(url);
	        				if(fileNameMatcher.find()){
	        					forum.put(TAG_CACHEFILE,fileNameMatcher.group(1));
	        				}
	        				forum.put(TAG_URL, url);
	                    }
                	}
                }
                forum.put(AwfulProvider.UPDATED_TIMESTAMP, update_time);
	            result.add(forum);
	
	            // Now grab the subforums
	            // we will see if the prior search found more than one link under the forum row, indicating subforums
	            Element subforumBlock = node.getElementsByClass("subforums").first();
	            if(subforumBlock != null){
	            	Elements subforums = subforumBlock.getElementsByTag("a");
	                for (Element subNode : subforums) {
	                	ContentValues subforum = new ContentValues();
	
	                    String id = subNode.attr("href");
	
	                    subforum.put(TITLE,subNode.text());
	                    subforum.put(ID,getForumId(id));
	                    subforum.put(PARENT_ID, forumId);
	                    result.add(subforum);
	                }
	            }
			}catch(Exception e){
				e.printStackTrace();
				continue;
			}
        }
		Log.i(TAG,"Deleted old forums: "+contentInterface.delete(AwfulForum.CONTENT_URI, AwfulForum.PARENT_ID+"=?", AwfulProvider.int2StrArray(0)));
        contentInterface.bulkInsert(AwfulForum.CONTENT_URI, result.toArray(new ContentValues[result.size()]));
	}
	
	public static void parseThreads(Document page, int forumId, int pageNumber, ContentResolver contentInterface) throws Exception{
		ArrayList<ContentValues> result = AwfulThread.parseForumThreads(page, AwfulPagedItem.forumPageToIndex(pageNumber), forumId);
		ContentValues forumData = new ContentValues();
    	forumData.put(ID, forumId);
    	forumData.put(TITLE, AwfulForum.parseTitle(page));
		ArrayList<ContentValues> newSubforums = AwfulThread.parseSubforums(page, forumId);
		contentInterface.delete(AwfulForum.CONTENT_URI, PARENT_ID+"=?", AwfulProvider.int2StrArray(forumId));
		contentInterface.bulkInsert(AwfulForum.CONTENT_URI, newSubforums.toArray(new ContentValues[newSubforums.size()]));
        int lastPage = AwfulPagedItem.parseLastPage(page);
        Log.i(TAG, "Last Page: " +lastPage);
    	forumData.put(PAGE_COUNT, lastPage);
    	contentInterface.delete(AwfulThread.CONTENT_URI, 
    							AwfulThread.FORUM_ID+"= ? AND "+AwfulThread.INDEX+">=? AND "+AwfulThread.INDEX+"<?", 
    							AwfulProvider.int2StrArray(forumId, AwfulPagedItem.forumPageToIndex(pageNumber), AwfulPagedItem.forumPageToIndex(pageNumber+1)));
		if(contentInterface.update(ContentUris.withAppendedId(CONTENT_URI, forumId), forumData, null, null) <1){
        	contentInterface.insert(CONTENT_URI, forumData);
		}
        contentInterface.bulkInsert(AwfulThread.CONTENT_URI, result.toArray(new ContentValues[result.size()]));
	}
	
	public static void parseUCPThreads(Document page, int pageNumber, ContentResolver contentInterface) throws Exception{
		ArrayList<ContentValues> threads = AwfulThread.parseForumThreads(page, AwfulPagedItem.forumPageToIndex(pageNumber), Constants.USERCP_ID);
		ArrayList<ContentValues> ucp_ids = new ArrayList<ContentValues>();
		int start_index = AwfulPagedItem.forumPageToIndex(pageNumber);
        String update_time = new Timestamp(System.currentTimeMillis()).toString();
		for(ContentValues thread : threads){
			ContentValues ucp_entry = new ContentValues();
			ucp_entry.put(AwfulThread.ID, thread.getAsInteger(AwfulThread.ID));
			ucp_entry.put(AwfulThread.INDEX, start_index);
			ucp_entry.put(AwfulProvider.UPDATED_TIMESTAMP, update_time);
			start_index++;
			ucp_ids.add(ucp_entry);
		}
		Log.i(TAG,"Parsed UCP entries:"+ucp_ids.size());
		ContentValues forumData = new ContentValues();
    	forumData.put(ID, Constants.USERCP_ID);
    	forumData.put(TITLE, "Bookmarks");
    	forumData.put(PARENT_ID, 0);
    	forumData.put(INDEX, 0);
    	forumData.put(AwfulProvider.UPDATED_TIMESTAMP, update_time);
        int lastPage = AwfulPagedItem.parseLastPage(page);
        Log.i(TAG, "Last Page: " +lastPage);
    	forumData.put(PAGE_COUNT, lastPage);
    	contentInterface.delete(AwfulThread.CONTENT_URI_UCP, 
				AwfulThread.INDEX+">=? AND "+AwfulThread.INDEX+"<?", 
				AwfulProvider.int2StrArray(AwfulPagedItem.forumPageToIndex(pageNumber), AwfulPagedItem.forumPageToIndex(pageNumber+1)));
		if(contentInterface.update(ContentUris.withAppendedId(CONTENT_URI, Constants.USERCP_ID), forumData, null, null) <1){
        	contentInterface.insert(CONTENT_URI, forumData);
		}
		contentInterface.bulkInsert(AwfulThread.CONTENT_URI, threads.toArray(new ContentValues[threads.size()]));
		contentInterface.bulkInsert(AwfulThread.CONTENT_URI_UCP, ucp_ids.toArray(new ContentValues[ucp_ids.size()]));
	}

    private static int getForumId(String aHref) {
    	Matcher forumIdMatch = forumId_regex.matcher(aHref);
    	if(forumIdMatch.find()){
    		return Integer.parseInt(forumIdMatch.group(1));
    	}
        return -1;
    }
    
	public static void getView(View current, AwfulPreferences mPrefs, Cursor data, boolean hasSidebar, boolean selected) {
		TextView title = (TextView) current.findViewById(R.id.title);
		TextView sub = (TextView) current.findViewById(R.id.subtext);
		if(mPrefs != null){
			title.setTextColor(mPrefs.postFontColor);
			sub.setTextColor(mPrefs.postFontColor2);
		}
		title.setText(Html.fromHtml(data.getString(data.getColumnIndex(TITLE))));
		String subtext = data.getString(data.getColumnIndex(SUBTEXT));
		if(subtext == null || subtext.length() < 1){
			sub.setVisibility(View.GONE);
		}else{
			sub.setVisibility(View.VISIBLE);
			sub.setText(subtext);
		}
		if(hasSidebar){
			current.setBackgroundResource(R.drawable.gradient_left);
		}else{
			current.setBackgroundResource(0);
		}
		if(selected){
			current.findViewById(R.id.selector).setVisibility(View.VISIBLE);
		}else{
			current.findViewById(R.id.selector).setVisibility(View.GONE);
		}
	}

	public static String parseTitle(Document data) {
		Elements result = data.getElementsByTag("title");
		String title = result.first().text();
		Matcher m = forumTitle_regex.matcher(title);
		if(m.find()){
			return m.group(1).trim();
		}
		return title;
	}

	/**
	 * This function takes a thread list item and reuses it as a subforum item.
	 * This is a hack to make a single cursor listadapter successfully combine thread and subforum items.
	 * @param current
	 * @param aPrefs
	 * @param data
	 * @param selected 
	 * @param mIsSidebar 
	 */	
	public static void getExpandableForumView(View current, AQuery aq, AwfulPreferences aPrefs, ForumEntry data, boolean selected, boolean hasChildren) {
		aq.recycle(current);
		if(selected){
			aq.backgroundColor(aPrefs.postBackgroundColor2);
		}else{
			aq.backgroundColor(aPrefs.postBackgroundColor);
		}
		aq.find(R.id.icon_box).gone();
		aq.find(R.id.selector).gone();
		aq.find(R.id.unread_count).gone();
		TextView title = (TextView) current.findViewById(R.id.title);
		title.setTypeface(null, Typeface.BOLD);
		String titleText = (data.title != null ? data.title : "");
		aq.find(R.id.title).textColor(aPrefs.postFontColor).text(Html.fromHtml(titleText)).getTextView().setSingleLine(!aPrefs.wrapThreadTitles);
		aq.find(R.id.threadinfo).gone();
		
		if(aPrefs.threadInfo_Tag && data.tagUrl != null){
			aq.id(R.id.forum_tag).visible().image(data.tagUrl, true, true);
		}else{
			aq.id(R.id.forum_tag).gone();
		}
	}
}
