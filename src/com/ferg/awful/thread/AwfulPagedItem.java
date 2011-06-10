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

import java.util.ArrayList;

import android.util.Log;

import org.htmlcleaner.TagNode;

import com.ferg.awful.constants.Constants;

public abstract class AwfulPagedItem {
    private static final String TAG = "AwfulPagedItem";

	private int mLastPage;
    protected String mTitle;
	
	public abstract ArrayList<? extends AwfulDisplayItem> getChildren(int page);
	public abstract int getChildrenCount(int page);
	public abstract AwfulDisplayItem getChild(int page, int ix);

	public int parsePageNumbers(TagNode aForum) throws Exception {
		int currentPage = 1;
		TagNode[] tarCurrentPage = aForum.getElementsByAttValue("class", "curpage", true, true);
		if (tarCurrentPage.length > 0) {
			currentPage = Integer.parseInt(tarCurrentPage[0].getText().toString());
		}else{
			mLastPage = 1;
			currentPage = 1;
			return currentPage;
		}

		//nodeList = aForum.evaluateXPath(LAST_PAGE);
		TagNode[] tarLastPage = aForum.getElementsByAttValue("class", "pagenumber", true, true);
		if (tarLastPage.length > 0) {
			// We'll look at the last link in the page bar first. If it has the "next page"
			// title attribute, we'll go back one to grab the highest direct page number. Otherwise
			// we'll be looking at the Last link, and we can parse out the page number from there.
			int index = tarLastPage.length - 1;

			TagNode node = tarLastPage[index];
			if (node.hasAttribute("title")) {
				if (!node.getAttributeByName("title").equals("last page")) {
					Log.i(TAG, "Next button!");
					node = tarLastPage[index - 1];
				}
			}

			String href = node.getAttributeByName("href");

			// Chop up all the parameters and find the pagenumber param
			String[] params = href.split("&");

			for (String param : params) {
				String[] keyValue = param.split("=");

				if (keyValue[0].equals("amp;" + Constants.PARAM_PAGE)) {
					mLastPage = Integer.parseInt(keyValue[1]);
                    if (currentPage > mLastPage) {
                        mLastPage = currentPage;
                    }
				}
			}
		}
		return currentPage;
	}

	public int getLastPage() {
		return mLastPage;
	}

	public void setLastPage(int aLastPage) {
		mLastPage = aLastPage;
	}
	public String getTitle() {
        return mTitle;
    }
	public void setTitle(String aTitle) {
        mTitle = aTitle;
    }


	public abstract int getID();
	public int getLastReadPage() {
		return 1;
	}

	public boolean isPaged(){
		return (getLastPage() > 1);
	}
}
