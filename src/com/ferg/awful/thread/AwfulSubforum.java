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

import com.ferg.awful.constants.Constants;

public class AwfulSubforum extends AwfulPagedItem implements Parcelable {
	public static final String ID        = "forum_id";
	public static final String TITLE     = "title";
	public static final String PARENT_ID = "parent_id";

	public static final String PATH = "/subforum";
	public static final Uri CONTENT_URI = Uri.parse("content://" + Constants.AUTHORITY + PATH);

	private String mTitle;
	private String mForumId;
	
	public AwfulSubforum() {}

	public AwfulSubforum(Parcel aAwfulSubforum) {
        mTitle       = aAwfulSubforum.readString();
        mForumId     = aAwfulSubforum.readString();
	}

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public AwfulSubforum createFromParcel(Parcel aAwfulSubforum) {
            return new AwfulSubforum(aAwfulSubforum);
        }

        public AwfulSubforum[] newArray(int aSize) {
            return new AwfulSubforum[aSize];
        }
    };

	public void save(Context aContext, int aParentId) {
		ContentValues params = new ContentValues();
		params.put(ID, Integer.parseInt(mForumId));
		params.put(TITLE, mTitle);
		params.put(PARENT_ID, aParentId);

		aContext.getContentResolver().insert(CONTENT_URI, params);
	}
    
    public static ArrayList<AwfulSubforum> fromParentId(Context aContext, int aParentId) {
        ArrayList<AwfulSubforum> result = new ArrayList<AwfulSubforum>();

        Cursor query = aContext.getContentResolver().query(CONTENT_URI, null, PARENT_ID + "=" +
                Integer.toString(aParentId), null, null);

        if (query.moveToFirst()) {
            int idIndex       = query.getColumnIndex(ID);
            int titleIndex    = query.getColumnIndex(TITLE);

            AwfulSubforum current;

            do {
                current = new AwfulSubforum();
                current.setForumId(Integer.toString(query.getInt(idIndex)));
                current.setTitle(query.getString(titleIndex));

                result.add(current);
            } while (query.moveToNext());
        }

        query.close();

        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel aDestination, int aFlags) {
        aDestination.writeString(mTitle);
        aDestination.writeString(mForumId);
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
}
