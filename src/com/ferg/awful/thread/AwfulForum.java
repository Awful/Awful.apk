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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;

public class AwfulForum extends AwfulSubforum implements Parcelable {
    private static final String TAG = "AwfulForum";

	public static final String ID      = "forum_id";
	public static final String TITLE   = "title";
	public static final String SUBTEXT = "subtext";

	public static final String PATH = "/forum";
	public static final Uri CONTENT_URI = Uri.parse("content://" + Constants.AUTHORITY + PATH);

	private static final String FORUM_ROW   = "//table[@id='forums']//tr//td[@class='title']";
	private static final String FORUM_TITLE = "//a[@class='forum']";
    private static final String SUBFORUM    = "//div[@class='subforums']//a";

	private String mTitle;
	private String mForumId;
	private String mSubtext;
    private ArrayList<AwfulSubforum> mSubforums;
	
	public AwfulForum() {
        mSubforums = new ArrayList<AwfulSubforum>();
    }

	public AwfulForum(Parcel aAwfulForum) {
        mSubforums = new ArrayList<AwfulSubforum>();

        mTitle       = aAwfulForum.readString();
        mForumId     = aAwfulForum.readString();
        mSubtext     = aAwfulForum.readString();
        aAwfulForum.readTypedList(mSubforums, AwfulSubforum.CREATOR);

        setCurrentPage(aAwfulForum.readInt());
        setLastPage(aAwfulForum.readInt());
	}

	public void save(Context aContext) {
		ContentValues params = new ContentValues();
		params.put(ID, Integer.parseInt(mForumId));
		params.put(TITLE, mTitle);
		params.put(SUBTEXT, mSubtext);

		aContext.getContentResolver().insert(CONTENT_URI, params);
	}

	public static ArrayList<AwfulForum> getForums(Context aContext) throws Exception {
        ArrayList<AwfulForum> result = new ArrayList<AwfulForum>();

        Cursor query = aContext.getContentResolver().query(CONTENT_URI, null, null, null, null);

        // If we have nothing in the cache, resort to the remote
        if (query.getCount() == 0) {
            return getForumsFromRemote(aContext);
        }
        
        if (query.moveToFirst()) {
            int idIndex      = query.getColumnIndex(ID);
            int titleIndex   = query.getColumnIndex(TITLE);
            int subtextIndex = query.getColumnIndex(SUBTEXT);

            AwfulForum current;

            do {
                int id = query.getInt(idIndex);

                current = new AwfulForum();
                current.setForumId(Integer.toString(id));
                current.setTitle(query.getString(titleIndex));
                current.setSubtext(query.getString(subtextIndex));
                current.setSubforum(AwfulSubforum.fromParentId(aContext, id));

                result.add(current);
            } while (query.moveToNext());
        }

        query.close();

        return result;
    }

	private static ArrayList<AwfulForum> getForumsFromRemote(Context aContext) throws Exception {
		long time = System.currentTimeMillis();
		ArrayList<AwfulForum> result = new ArrayList<AwfulForum>();

        TagNode response = NetworkUtils.get(Constants.BASE_URL);

		Object[] forumObjects = response.evaluateXPath(FORUM_ROW);

		for (Object current : forumObjects) {
			AwfulForum forum = new AwfulForum();
			TagNode node = (TagNode) current;

            // First, grab the parent forum
            Object[] nodeList = node.evaluateXPath(FORUM_TITLE);
            if (nodeList.length > 0) {
                TagNode parentForum = (TagNode) nodeList[0];
                forum.setTitle(parentForum.getText().toString());

                // Just nix the part we don't need to get the forum ID
                String id = parentForum.getAttributeByName("href");

                forum.setForumId(getForumId(id));
                forum.setSubtext(parentForum.getAttributeByName("title"));
            }

            // Now grab the subforums
            nodeList = node.evaluateXPath(SUBFORUM);
            if (nodeList.length > 0) {
                for (Object obj : nodeList) {
                    AwfulSubforum subforum = new AwfulSubforum();

                    TagNode subNode = (TagNode) obj;

                    String id = subNode.getAttributeByName("href");

                    subforum.setTitle(subNode.getText().toString());
                    subforum.setForumId(getForumId(id));

					// Cache the subforum in the database
					subforum.save(aContext, Integer.parseInt(forum.getForumId()));

                    forum.addSubforum(subforum);
                }
            }

			forum.save(aContext);
            
            result.add(forum);
        }
        Log.e(TAG, "Process Time: "+ (System.currentTimeMillis() - time));
		return result;
	}

    private static String getForumId(String aHref) {
        String[] idSplit = aHref.split("=");

        return idSplit[1];
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
        aDestination.writeTypedList(mSubforums);
        aDestination.writeInt(getCurrentPage());
        aDestination.writeInt(getLastPage());
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

	public ArrayList<AwfulSubforum> getSubforums() {
		return mSubforums;
	}

    public void addSubforum(AwfulSubforum aSubforum) {
        mSubforums.add(aSubforum);
    }

	public void setSubforum(ArrayList<AwfulSubforum> aSubforums) {
		mSubforums = aSubforums;
	}
}
