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

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;

public class AwfulThread extends AwfulPagedItem implements Parcelable {
    private static final String TAG = "AwfulThread";

    private static final String THREAD_ROW      = "//table[@id='forum']//tr";
    private static final String THREAD_SEEN_ROW = "//tr[@class='thread']";
    private static final String THREAD_TITLE    = "//a[@class='thread_title']";
    private static final String THREAD_STICKY   = "//td[@class='title title_sticky']";
    private static final String THREAD_ICON     = "//td[@class='icon']/img";
    private static final String THREAD_AUTHOR   = "//td[@class='author']/a";
    private static final String UNREAD_POSTS    = "//a[@class='count']//b";
    private static final String UNREAD_UNDO     = "//a[@class='x']";
	private static final String CURRENT_PAGE    = "//span[@class='curpage']";
	private static final String LAST_PAGE       = "//a[@class='pagenumber']";

    private String mThreadId;
    private String mTitle;
    private String mAuthor;
    private boolean mSticky;
    private String mIcon;
    private int mUnreadCount;
    private ArrayList<AwfulPost> mPosts;

    public AwfulThread() {}

    public AwfulThread(String aThreadId) {
        mThreadId = aThreadId;
    }

    public AwfulThread(Parcel aAwfulThread) {
        mThreadId    = aAwfulThread.readString();
        mTitle       = aAwfulThread.readString();
        mAuthor      = aAwfulThread.readString();
        mSticky      = aAwfulThread.readInt() == 1 ? true : false;
        mIcon        = aAwfulThread.readString();
        mUnreadCount = aAwfulThread.readInt();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public AwfulThread createFromParcel(Parcel aAwfulThread) {
            return new AwfulThread(aAwfulThread);
        }

        public AwfulThread[] newArray(int aSize) {
            return new AwfulThread[aSize];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel aDestination, int aFlags) {
        aDestination.writeString(mThreadId);
        aDestination.writeString(mTitle);
        aDestination.writeString(mAuthor);
        aDestination.writeInt(mSticky ? 1 : 0);
        aDestination.writeString(mIcon);
        aDestination.writeInt(mUnreadCount);
    }
    
    public static TagNode getForumThreads(String aForumId) throws Exception {
		return getForumThreads(aForumId, 0);
	}
	
    public static TagNode getForumThreads(String aForumId, int aPage) throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(Constants.PARAM_FORUM_ID, aForumId);

		if (aPage != 0) {
			params.put(Constants.PARAM_PAGE, Integer.toString(aPage));
		}

        return NetworkUtils.get(Constants.FUNCTION_FORUM, params);
	}
	
    public static TagNode getUserCPThreads() throws Exception {
        return NetworkUtils.get(Constants.FUNCTION_USERCP, null);
	}

	public static ArrayList<AwfulThread> parseForumThreads(TagNode aResponse) throws Exception {
        ArrayList<AwfulThread> result = new ArrayList<AwfulThread>();

        Object[] threadObjects = aResponse.evaluateXPath(THREAD_ROW);

        for (Object current : threadObjects) {
            AwfulThread thread = new AwfulThread();
            TagNode node = (TagNode) current;

            try {
                String threadId = node.getAttributeByName("id");
                thread.setThreadId(threadId.replaceAll("thread", ""));
            } catch (NullPointerException e) {
                // If we can't parse a row, just skip it
                e.printStackTrace();
                continue;
            }

                Object[] nodeList = node.evaluateXPath(THREAD_TITLE);
                if (nodeList.length > 0) {
                    thread.setTitle(((TagNode) nodeList[0]).getText().toString().trim());
                }

                nodeList = node.evaluateXPath(THREAD_STICKY);
                if (nodeList.length > 0) {
                    thread.setSticky(true);
                } else {
                    thread.setSticky(false);
                }

                nodeList = node.evaluateXPath(THREAD_ICON);
                if (nodeList.length > 0) {
                    thread.setIcon(((TagNode) nodeList[0]).getAttributeByName("src"));
                }

                nodeList = node.evaluateXPath(THREAD_AUTHOR);
                if (nodeList.length > 0) {
                    TagNode authorNode = (TagNode) nodeList[0];

                    // There's got to be a better way to do this
                    authorNode.removeChild(authorNode.findElementHavingAttribute("href", false));

                    thread.setAuthor(authorNode.getText().toString().trim());
                }

                nodeList = node.evaluateXPath(UNREAD_POSTS);
                if (nodeList.length > 0) {
                    thread.setUnreadCount(Integer.parseInt(
                                ((TagNode) nodeList[0]).getText().toString().trim()));
                } else {
                    thread.setUnreadCount(0);
                }

                result.add(thread);
        }

        return result;
    }

    public void getThreadPosts() throws Exception {
        getThreadPosts(-1);
    }

    public void getThreadPosts(int aPage) throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(Constants.PARAM_THREAD_ID, mThreadId);
        // TODO: This seems to always return 0 results
        // params.put(Constants.PARAM_PER_PAGE, "20");

        if (aPage == -1) {
            params.put(Constants.PARAM_GOTO, "newpost");
        } else {
            params.put(Constants.PARAM_PAGE, Integer.toString(aPage));
        } 

		TagNode response = NetworkUtils.get(Constants.FUNCTION_THREAD, params);

        setPosts(AwfulPost.parsePosts(response));
		parsePageNumbers(response);
    }

    public String getThreadId() {
        return mThreadId;
    }

    public void setThreadId(String aThreadId) {
        mThreadId = aThreadId;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String aTitle) {
        mTitle = aTitle;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public void setAuthor(String aAuthor) {
        mAuthor = aAuthor;
    }

    public String getIcon() {
        return mIcon;
    }

    public void setIcon(String aIcon) {
        mIcon = aIcon;
    }

    public boolean isSticky() {
        return mSticky;
    }

    public void setSticky(boolean aSticky) {
        mSticky = aSticky;
    }

    public int getUnreadCount() {
        return mUnreadCount;
    }

    public void setUnreadCount(int aUnreadCount) {
        mUnreadCount = aUnreadCount;
    }

    public ArrayList<AwfulPost> getPosts() {
        return mPosts;
    }

    public void setPosts(ArrayList<AwfulPost> aPosts) {
        mPosts = aPosts;
    }
}
