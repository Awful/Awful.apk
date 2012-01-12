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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import com.ferg.awful.R;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;

public class AwfulForum extends AwfulPagedItem implements AwfulDisplayItem {
    private static final String TAG = "AwfulForum";

    public static final String PATH     = "/forum";
    public static final Uri CONTENT_URI = Uri.parse("content://" + Constants.AUTHORITY + PATH);

	public static final String ID      = "forum_id";
	public static final String PARENT_ID      = "parent_forum_id";
	public static final String TITLE   = "title";
	public static final String SUBTEXT = "subtext";
	public static final String PAGE_COUNT = "page_count";

	private static final String FORUM_ROW   = "//table[@id='forums']//tr//td[@class='title']";
	//private static final String FORUM_TITLE = "//a[@class='forum']";
    //private static final String SUBFORUM    = "//div[@class='subforums']//a";

	private static final Pattern forumId_regex = Pattern.compile("forumid=(\\d+)");


	private String mTitle;
	private String mForumId;
	private int forumId;
	private String mSubtext;
    private ArrayList<AwfulForum> mSubforums;
	private HashMap<Integer, ArrayList<AwfulThread>> threads;
	
	public AwfulForum() {
        mSubforums = new ArrayList<AwfulForum>();
        threads = new HashMap<Integer, ArrayList<AwfulThread>>();
    }

	public AwfulForum(int mForumID2) {
		this();
		setForumId(mForumID2);
	}

	public static void getForumsFromRemote(TagNode response, ContentResolver contentInterface) throws XPatherException {
		ArrayList<ContentValues> result = new ArrayList<ContentValues>();

		Object[] forumObjects = response.evaluateXPath(FORUM_ROW);
		for (Object current : forumObjects) {
			try{
				ContentValues forum = new ContentValues();
				TagNode node = (TagNode) current;
				int forumId = 0;
	            // First, grab the parent forum
				TagNode[] title = node.getElementsByName("a", true);
	            if (title.length > 0) {
	                TagNode parentForum = title[0];
	                forum.put(TITLE,parentForum.getText().toString());
	                forum.put(PARENT_ID, 0);
	
	                // Just nix the part we don't need to get the forum ID
	                String id = parentForum.getAttributeByName("href");
	                forumId=getForumId(id);
	                forum.put(ID,forumId);
	                forum.put(SUBTEXT,parentForum.getAttributeByName("title"));
	            }
	            result.add(forum);
	
	            // Now grab the subforums
	            // we will see if the prior search found more than one link under the forum row, indicating subforums
	            if (title.length > 1) {
	                for (int x=1;x<title.length;x++) {
	                	ContentValues subforum = new ContentValues();
	
	                    TagNode subNode = title[x];
	
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
        contentInterface.bulkInsert(CONTENT_URI, result.toArray(new ContentValues[result.size()]));
	}
	
	public static void parseThreads(TagNode page, int forumId, int pageNumber, ContentResolver contentInterface){
		ArrayList<ContentValues> result = AwfulThread.parseForumThreads(page, forumId);
		ContentValues forumData = new ContentValues();
    	forumData.put(TITLE, AwfulForum.parseTitle(page));
    	ArrayList<ContentValues> newSubforums = AwfulThread.parseSubforums(page);
        int lastPage = AwfulPagedItem.parseLastPage(page);
        Log.i(TAG, "Last Page: " +lastPage);
    	forumData.put(PAGE_COUNT, lastPage);
        if(contentInterface.update(ContentUris.withAppendedId(CONTENT_URI, forumId), forumData, null, null) <1){
        	contentInterface.insert(CONTENT_URI, forumData);
    	}
        contentInterface.bulkInsert(AwfulThread.CONTENT_URI, result.toArray(new ContentValues[result.size()]));
	}

    private static int getForumId(String aHref) {
    	Matcher forumIdMatch = forumId_regex.matcher(aHref);
    	if(forumIdMatch.find()){
    		return Integer.parseInt(forumIdMatch.group(1));
    	}
        return -1;
    }
    
	public String getTitle() {
		return mTitle;
	}

	public void setTitle(String aTitle) {
		mTitle = aTitle;
	}

	public String getForumId() {
		return mForumId;
	}

	public void setForumId(String aForumId) {
		mForumId = aForumId;
		forumId = Integer.parseInt(mForumId);
	}

	public void setForumId(int aForumId) {
		mForumId = Integer.toString(aForumId);
		forumId = aForumId;
	}

	public String getSubtext() {
		return mSubtext;
	}

	public void setSubtext(String aSubtext) {
		mSubtext = aSubtext;
	}

	public ArrayList<AwfulForum> getSubforums() {
		return mSubforums;
	}

    public void addSubforum(AwfulForum aSubforum) {
        mSubforums.add(aSubforum);
    }

	public void setSubforum(ArrayList<AwfulForum> aSubforums) {
		mSubforums = aSubforums;
	}

	@Override
	public View getView(LayoutInflater inf, View current, ViewGroup parent, AwfulPreferences mPrefs, Cursor data) {
		View tmp = current;
		if(tmp == null || tmp.getId() != R.layout.forum_item){
			tmp = inf.inflate(R.layout.forum_item, parent, false);
		}
		TextView title = (TextView) tmp.findViewById(R.id.title);
		TextView sub = (TextView) tmp.findViewById(R.id.subtext);
		if(mPrefs != null){
			title.setTextColor(mPrefs.postFontColor);
			sub.setTextColor(mPrefs.postFontColor2);
		}
		title.setText(Html.fromHtml(mTitle));
		sub.setText(mSubtext);
		return tmp;
	}

	@Override
	public int getID() {
		return forumId;
	}

	@Override
	public ArrayList<? extends AwfulDisplayItem> getChildren(int page) {
		ArrayList<AwfulDisplayItem> tmp = new ArrayList<AwfulDisplayItem>();
		if(page <2){
			tmp.addAll(mSubforums);
		}
		if(threads.get(page) != null){
			tmp.addAll(threads.get(page));
		}
		return tmp;
	}

	public void setThreadPage(int mPage, ArrayList<AwfulThread> threadList) {
		threads.put(mPage, threadList);
	}

	@Override
	public int getChildrenCount(int page) {
		return (page <2 ? mSubforums.size() : 0)+
		(threads.get(page) == null? 0 : threads.get(page).size());
	}

	@Override
	public AwfulDisplayItem getChild(int page, int ix) {
		if(ix<mSubforums.size() && page < 2){
			return mSubforums.get(ix);
		}
		if(page < 2){
			return threads.get(page).get(ix-mSubforums.size());
		}
		return threads.get(page).get(ix);
	}

	public static String parseTitle(TagNode data) {
		TagNode[] result = data.getElementsByName("title", true);
		return result[0].getText().toString();
	}

	@Override
	public boolean isPageCached(int page) {
		return threads.get(page) != null || forumId == 0;
	}
}
