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

public class AwfulForum implements Parcelable {
    private static final String TAG = "AwfulForum";

	private static final String FORUM_ROW    = "//table[@id='forums']//tr//td[@class='title']//a[@class='forum']";
	private static final String FORUM_TITLE  = "//a[@class='forum']";
	private static final String CURRENT_PAGE = "//span[@class='curpage']";
	private static final String LAST_PAGE    = "//a[@class='pagenumber']";
	// TODO: Parse subforums

	private String mTitle;
	private String mForumId;
	private String mSubtext;
	private int mCurrentPage;
	private int mLastPage;
	
	public AwfulForum() {}

	public AwfulForum(Parcel aAwfulForum) {
        mTitle       = aAwfulForum.readString();
        mForumId     = aAwfulForum.readString();
        mSubtext     = aAwfulForum.readString();
        mCurrentPage = aAwfulForum.readInt();
        mLastPage    = aAwfulForum.readInt();
	}

	public void parsePageNumbers(TagNode aForum) throws Exception {
		Object[] nodeList = aForum.evaluateXPath(CURRENT_PAGE);
		if (nodeList.length > 0) {
			mCurrentPage = Integer.parseInt(((TagNode) nodeList[0]).getText().toString());
		}

		nodeList = aForum.evaluateXPath(LAST_PAGE);
		if (nodeList.length > 0) {
			// We'll look at the last link in the page bar first. If it has the "next page"
			// title attribute, we'll go back one to grab the highest direct page number. Otherwise
			// we'll be looking at the Last link, and we can parse out the page number from there.
			int index = nodeList.length - 1;

			TagNode node = (TagNode) nodeList[index];
			if (node.hasAttribute("title")) {
				if (!node.getAttributeByName("title").equals("last page")) {
					Log.i(TAG, "Next button!");
					node = (TagNode) nodeList[index - 1];
				}
			}

			String href = node.getAttributeByName("href");

			// Chop up all the parameters and find the pagenumber param
			String[] params = href.split("&");

			for (String param : params) {
				String[] keyValue = param.split("=");

				Log.i(TAG, "Key: " + keyValue[0] + " Value: " + keyValue[1]);
				
				if (keyValue[0].equals("amp;" + Constants.PARAM_PAGE)) {
					mLastPage = Integer.parseInt(keyValue[1]);
				}
			}
		}
	}

	public static ArrayList<AwfulForum> getForums() throws Exception {
		ArrayList<AwfulForum> result = new ArrayList<AwfulForum>();

        TagNode response = NetworkUtils.get(Constants.BASE_URL);

		Object[] forumObjects = response.evaluateXPath(FORUM_ROW);

		for (Object current : forumObjects) {
			AwfulForum forum = new AwfulForum();
			TagNode node = (TagNode) current;

			forum.setTitle(node.getText().toString());

			// Just nix the part we don't need to get the forum ID
			String id = node.getAttributeByName("href");
			String[] idSplit = id.split("=");
				
			forum.setForumId(idSplit[1]);	

			forum.setSubtext(node.getAttributeByName("title"));

			result.add(forum);
		}

		return result;
	}

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public AwfulForum createFromParcel(Parcel aAwfulForum) {
            return new AwfulForum(aAwfulForum);
        }

        public AwfulForum[] newArray(int aSize) {
            return new AwfulForum[aSize];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel aDestination, int aFlags) {
        aDestination.writeString(mTitle);
        aDestination.writeString(mForumId);
        aDestination.writeString(mSubtext);
        aDestination.writeInt(mCurrentPage);
        aDestination.writeInt(mLastPage);
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
	}

	public String getSubtext() {
		return mSubtext;
	}

	public void setSubtext(String aSubtext) {
		mSubtext = aSubtext;
	}

	public int getCurrentPage() {
		return mCurrentPage;
	}

	public void setCurrentPage(int aCurrentPage) {
		mCurrentPage = aCurrentPage;
	}

	public int getLastPage() {
		return mLastPage;
	}

	public void setLastPage(int aLastPage) {
		mLastPage = aLastPage;
	}
}
