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
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import com.ferg.awfulapp.AwfulActivity;
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

	private static final String FORUM_ROW   = "//table[@id='forums']//tr";
	//private static final String FORUM_TITLE = "//a[@class='forum']";
    //private static final String SUBFORUM    = "//div[@class='subforums']//a";

	private static final Pattern forumId_regex = Pattern.compile("forumid=(\\d+)");
	private static final Pattern forumTitle_regex = Pattern.compile("(.+)-{1}.+$");

	public static void getForumsFromRemote(TagNode response, ContentResolver contentInterface) throws XPatherException {
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
		Object[] forumObjects = response.evaluateXPath(FORUM_ROW);
		for (Object current : forumObjects) {
			try{
				ContentValues forum = new ContentValues();
				TagNode node = (TagNode) current;
				int forumId = 0;
	            // First, grab the parent forum
				TagNode title = node.findElementByAttValue("class", "forum", true, true);
	            if (title != null) {
	                TagNode parentForum = title;
	                forum.put(TITLE,parentForum.getText().toString());
	                forum.put(PARENT_ID, 0);
	                forum.put(INDEX, ix);
	                ix++;
	                // Just nix the part we don't need to get the forum ID
	                String id = parentForum.getAttributeByName("href");
	                forumId=getForumId(id);
	                forum.put(ID,forumId);
	                forum.put(SUBTEXT,parentForum.getAttributeByName("title"));
	            }
	            TagNode tarIcon = node.findElementByAttValue("class", "icon", true, true);
                if (tarIcon != null) {
                	TagNode imgTag = tarIcon.findElementByName("img", true);
                	if(imgTag != null && imgTag.hasAttribute("src")){
	                    String url = imgTag.getAttributeByName("src");
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
	            TagNode subforumBlock = node.findElementByAttValue("class", "subforums", true, true);
	            if(subforumBlock != null){
		            TagNode[] subforums = subforumBlock.getElementsByName("a", true);
	                for (int x=0;x<subforums.length;x++) {
	                	ContentValues subforum = new ContentValues();
	
	                    TagNode subNode = subforums[x];
	
	                    String id = subNode.getAttributeByName("href");
	
	                    subforum.put(TITLE,subNode.getText().toString());
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
		contentInterface.delete(AwfulForum.CONTENT_URI, AwfulForum.PARENT_ID+"=?", AwfulProvider.int2StrArray(0));
        contentInterface.bulkInsert(AwfulForum.CONTENT_URI, result.toArray(new ContentValues[result.size()]));
	}
	
	public static void parseThreads(TagNode page, int forumId, int pageNumber, ContentResolver contentInterface) throws Exception{
		ArrayList<ContentValues> result = AwfulThread.parseForumThreads(page, AwfulPagedItem.pageToIndex(pageNumber), forumId);
		ContentValues forumData = new ContentValues();
    	forumData.put(ID, forumId);
    	forumData.put(TITLE, AwfulForum.parseTitle(page));
		ArrayList<ContentValues> newSubforums = AwfulThread.parseSubforums(page, forumId);
		contentInterface.bulkInsert(AwfulForum.CONTENT_URI, newSubforums.toArray(new ContentValues[newSubforums.size()]));
        int lastPage = AwfulPagedItem.parseLastPage(page);
        Log.i(TAG, "Last Page: " +lastPage);
    	forumData.put(PAGE_COUNT, lastPage);
    	contentInterface.delete(AwfulThread.CONTENT_URI, 
    							AwfulThread.FORUM_ID+"= ? AND "+AwfulThread.INDEX+">=? AND "+AwfulThread.INDEX+"<?", 
    							AwfulProvider.int2StrArray(forumId, AwfulPagedItem.pageToIndex(pageNumber), AwfulPagedItem.pageToIndex(pageNumber+1)));
		if(contentInterface.update(ContentUris.withAppendedId(CONTENT_URI, forumId), forumData, null, null) <1){
        	contentInterface.insert(CONTENT_URI, forumData);
		}
        contentInterface.bulkInsert(AwfulThread.CONTENT_URI, result.toArray(new ContentValues[result.size()]));
	}
	
	public static void parseUCPThreads(TagNode page, int pageNumber, ContentResolver contentInterface) throws Exception{
		ArrayList<ContentValues> threads = AwfulThread.parseForumThreads(page, AwfulPagedItem.pageToIndex(pageNumber), Constants.USERCP_ID);
		ArrayList<ContentValues> ucp_ids = new ArrayList<ContentValues>();
		int start_index = AwfulPagedItem.pageToIndex(pageNumber);
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
        int lastPage = AwfulPagedItem.parseLastPage(page);
        Log.i(TAG, "Last Page: " +lastPage);
    	forumData.put(PAGE_COUNT, lastPage);
    	contentInterface.delete(AwfulThread.CONTENT_URI_UCP, 
				AwfulThread.INDEX+">=? AND "+AwfulThread.INDEX+"<?", 
				AwfulProvider.int2StrArray(AwfulPagedItem.pageToIndex(pageNumber), AwfulPagedItem.pageToIndex(pageNumber+1)));
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

	public static String parseTitle(TagNode data) {
		TagNode[] result = data.getElementsByName("title", true);
		String title = result[0].getText().toString();
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
	public static ImageView getSubforumView(View current, AwfulActivity parent, AwfulPreferences aPrefs, Cursor data, boolean hasSidebar, boolean selected) {
		TextView title = (TextView) current.findViewById(R.id.title);
		TextView sub = (TextView) current.findViewById(R.id.threadinfo);
		current.findViewById(R.id.icon_box).setVisibility(View.GONE);
		current.findViewById(R.id.thread_tag).setVisibility(View.GONE);
		current.findViewById(R.id.unread_count).setVisibility(View.GONE);
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
		if(aPrefs != null){
			title.setTextColor(aPrefs.postFontColor);
			sub.setTextColor(aPrefs.postFontColor2);
		}
		title.setText(Html.fromHtml(data.getString(data.getColumnIndex(TITLE))));
		title.setTypeface(null, Typeface.BOLD);
		String subtext = data.getString(data.getColumnIndex(SUBTEXT));
		if(subtext != null && subtext.length() > 0){
			sub.setVisibility(View.VISIBLE);
			sub.setText(subtext);
		}else{
			sub.setVisibility(View.GONE);
		}
		
		ImageView threadTag = (ImageView) current.findViewById(R.id.forum_tag);
		String tagFile = data.getString(data.getColumnIndex(TAG_CACHEFILE));
		if(aPrefs.threadInfo_Tag && tagFile != null && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			if(!AwfulThread.threadTagExists(parent, tagFile)){
				threadTag.setVisibility(View.INVISIBLE);
				threadTag.setTag(new String[]{tagFile,data.getString(data.getColumnIndex(TAG_URL))});
			}else{
				threadTag.setTag(tagFile);
			}
		}else{
			threadTag.setVisibility(View.GONE);
			threadTag = null;
		}
		return threadTag;
	}
}
