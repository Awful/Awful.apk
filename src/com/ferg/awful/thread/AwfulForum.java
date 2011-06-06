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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import org.htmlcleaner.TagNode;

import com.ferg.awful.R;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;

public class AwfulForum extends AwfulPagedItem implements AwfulDisplayItem {
    private static final String TAG = "AwfulForum";

	public static final String ID      = "forum_id";
	public static final String TITLE   = "title";
	public static final String SUBTEXT = "subtext";

	private static final String FORUM_ROW   = "//table[@id='forums']//tr//td[@class='title']";
	//private static final String FORUM_TITLE = "//a[@class='forum']";
    //private static final String SUBFORUM    = "//div[@class='subforums']//a";

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

	public static ArrayList<AwfulForum> getForumsFromRemote() throws Exception {
		ArrayList<AwfulForum> result = new ArrayList<AwfulForum>();
		AwfulForum index = new AwfulForum();
		index.setForumId(0);
		index.setTitle("Something Awful Forums");
        TagNode response = NetworkUtils.get(Constants.BASE_URL);

		Object[] forumObjects = response.evaluateXPath(FORUM_ROW);

		for (Object current : forumObjects) {
			AwfulForum forum = new AwfulForum();
			TagNode node = (TagNode) current;

            // First, grab the parent forum
			TagNode[] title = node.getElementsByName("a", true);
            if (title.length > 0) {
                TagNode parentForum = title[0];
                forum.setTitle(parentForum.getText().toString());

                // Just nix the part we don't need to get the forum ID
                String id = parentForum.getAttributeByName("href");

                forum.setForumId(getForumId(id));
                forum.setSubtext(parentForum.getAttributeByName("title"));
            }

            // Now grab the subforums
            // we will see if the prior search found more than one link under the forum row, indicating subforums
            if (title.length > 1) {
                for (int x=1;x<title.length;x++) {
                    AwfulForum subforum = new AwfulForum();

                    TagNode subNode = title[x];

                    String id = subNode.getAttributeByName("href");

                    subforum.setTitle(subNode.getText().toString());
                    subforum.setForumId(getForumId(id));

                    forum.addSubforum(subforum);
                    result.add(subforum);
                }
            }
            result.add(forum);
            index.addSubforum(forum);
        }
		result.add(index);
		return result;
	}

    private static String getForumId(String aHref) {
        String[] idSplit = aHref.split("=");

        return idSplit[1];
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
	public View getView(LayoutInflater inf, View current, ViewGroup parent) {
		View tmp = current;
		if(tmp == null || tmp.getId() != R.layout.forum_item){
			tmp = inf.inflate(R.layout.forum_item, parent, false);
		}
		TextView title = (TextView) tmp.findViewById(R.id.title);
		TextView sub = (TextView) tmp.findViewById(R.id.subtext);
		title.setText(mTitle);
		sub.setText(mSubtext);
		return tmp;
	}

	@Override
	public int getID() {
		return forumId;
	}

	@Override
	public DISPLAY_TYPE getType() {
		return DISPLAY_TYPE.FORUM;
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

	@Override
	public boolean isEnabled() {
		return true;
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
}
