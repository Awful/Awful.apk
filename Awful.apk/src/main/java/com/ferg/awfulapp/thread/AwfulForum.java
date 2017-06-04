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
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.forums.ForumRepository;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.DatabaseHelper;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AwfulForum extends AwfulPagedItem {
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
	private static final String TAG = "AwfulForum";
	private static final Pattern forumId_regex = Pattern.compile("forumid=(\\d+)");

	public static void processForumIcons(Document response, ContentResolver contentInterface){
		Elements forumIcons = response.getElementById("forums").getElementsByClass("icon");
		for (Element node : forumIcons) {
                if (node != null) {
					ContentValues forum = new ContentValues();
                	Element imgTag = node.getElementsByTag("img").first();
                	if(imgTag != null && imgTag.hasAttr("src")){
	                    String url = imgTag.attr("src");
	                    if(url != null){
							if(url.startsWith("//")){
								// damn you and you protocol-less image urls, ZDR
								url = Constants.BASE_URL.substring(0,Constants.BASE_URL.indexOf("//")) + url;
							}
	                    	//thread tag stuff
	        				Matcher fileNameMatcher = AwfulEmote.fileName_regex.matcher(url);
	        				if(fileNameMatcher.find()){
	        					forum.put(TAG_CACHEFILE,fileNameMatcher.group(1));
	        				}
	        				forum.put(TAG_URL, url);
	                    }
                	}
					int forumId = getForumId(node.getElementsByTag("a").first().attr("href"));
					contentInterface.update(ContentUris.withAppendedId(CONTENT_URI, forumId), forum, null, null);
                }
        }
	}


	/**
	 * Parse a forum page, storing the thread list and updating the threads in the database.
	 *
	 * @param page             a forum page containing a list of threads
	 * @param forumId          the ID of the forum being parsed
	 * @param pageNumber       the number of the page being parsed, e.g. page 2 of GBS
	 * @param contentInterface used for database access
	 */
	public static void parseThreads(Document page, int forumId, int pageNumber, ContentResolver contentInterface) {
		// get the threads on a (normal) forum page, index them and store
		List<ContentValues> threads = AwfulThread.parseForumThreads(page, forumId, forumPageToIndex(pageNumber));
		deletePageOfThreads(forumId, pageNumber, contentInterface);
		insertThreads(threads, contentInterface);

		// update page count for forum
		int lastPage = parseLastPage(page);
		ForumRepository.getInstance(null).setPageCount(forumId, lastPage);
	}


	/**
	 * Parse a Bookmarks page, storing the thread list and updating the threads in the database.
	 *
	 * @param page             a page containing the user's bookmarks
	 * @param pageNumber       the number of the page being parsed, e.g. page 2 of the bookmarks
	 * @param contentInterface used for database access
	 */
	public static void parseUCPThreads(@NonNull Document page, int pageNumber, @NonNull ContentResolver contentInterface) {
		// get all the threads on the bookmarks page, with their INDEXes set appropriately, and store them
		List<ContentValues> threads = AwfulThread.parseForumThreads(page, Constants.USERCP_ID, forumPageToIndex(pageNumber));
		insertThreads(threads, contentInterface);

		// for each thread on the page, create a bookmark (with the thread's ID) in the same position (same index)
		String update_time = new Timestamp(System.currentTimeMillis()).toString();
		List<ContentValues> bookmarks = new ArrayList<>();

		int start_index = forumPageToIndex(pageNumber);
		for (ContentValues thread : threads) {
			ContentValues bookmark = new ContentValues();
			bookmark.put(AwfulThread.ID, thread.getAsInteger(AwfulThread.ID));
			bookmark.put(AwfulThread.INDEX, start_index);
			bookmark.put(DatabaseHelper.UPDATED_TIMESTAMP, update_time);
			start_index++;
			bookmarks.add(bookmark);
		}
		Log.i(TAG, "Parsed UCP entries: " + bookmarks.size());

		// delete all the bookmarked threads for this page, re-add the new ones in the same place (thanks to the matching indices)
		deletePageOfBookmarks(pageNumber, contentInterface);
		insertBookmarks(bookmarks, contentInterface);
		// update bookmarks forum
		ForumRepository.getInstance(null).setPageCount(Constants.USERCP_ID, parseLastPage(page));
	}


	private static void insertThreads(@NonNull List<ContentValues> threads, @NonNull ContentResolver resolver) {
		resolver.bulkInsert(AwfulThread.CONTENT_URI, threads.toArray(new ContentValues[threads.size()]));
	}


	private static void insertBookmarks(@NonNull List<ContentValues> bookmarks, @NonNull ContentResolver resolver) {
		resolver.bulkInsert(AwfulThread.CONTENT_URI_UCP, bookmarks.toArray(new ContentValues[bookmarks.size()]));

	}


	private static void deletePageOfThreads(int forumId, int pageNum, @NonNull ContentResolver resolver) {
		if (forumId == Constants.USERCP_ID) {
			throw new RuntimeException("This method deletes threads from forums, not the bookmarks table!");
		}
		resolver.delete(AwfulThread.CONTENT_URI,
				String.format("%s=? AND %s>=? AND %s<?", AwfulThread.FORUM_ID, AwfulThread.INDEX, AwfulThread.INDEX),
				AwfulProvider.int2StrArray(forumId, forumPageToIndex(pageNum), forumPageToIndex(pageNum + 1)));
	}


	private static void deletePageOfBookmarks(int pageNum, @NonNull ContentResolver resolver) {
		resolver.delete(AwfulThread.CONTENT_URI_UCP, String.format("%s>=? AND %s<?", AwfulThread.INDEX, AwfulThread.INDEX),
				AwfulProvider.int2StrArray(forumPageToIndex(pageNum), forumPageToIndex(pageNum + 1)));
	}


	public static int getForumId(String aHref) {
		Matcher forumIdMatch = forumId_regex.matcher(aHref);
    	if(forumIdMatch.find()){
    		return Integer.parseInt(forumIdMatch.group(1));
    	}
        return -1;
    }

}
