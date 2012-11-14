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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.service.AwfulSyncService;

public abstract class AwfulPagedItem {
    private static final String TAG = "AwfulPagedItem";
    
	private static final Pattern pageNumber_regex = Pattern.compile("Pages \\((\\d+)\\)");
	
    public static int parseLastPage(Document pagedItem){
    	Element pages = pagedItem.getElementsByAttributeValue("class", "pages").first();
    	Element pages2 = pagedItem.getElementsByAttributeValue("class", "pages top").first();
    	Matcher lastPageMatch = null;
    	if(pages != null){
    		lastPageMatch = pageNumber_regex.matcher(pages.text());
    	}else{
    		if(pages2 != null){
	    		lastPageMatch = pageNumber_regex.matcher(pages2.text());
	    	}
    	}
    	if(lastPageMatch != null && lastPageMatch.find()){
    		return Integer.parseInt(lastPageMatch.group(1));
    	}
		return 1;
    }
    
    public static int parseLastPage(Element pagedItem){
    	Matcher lastPageMatch = null;
    	Elements pages = pagedItem.getElementsByClass("pages");
    	if(pages.size() > 0){
    		lastPageMatch = pageNumber_regex.matcher(pages.get(0).text());
    	}
    	if(lastPageMatch != null && lastPageMatch.find()){
    		return Integer.parseInt(lastPageMatch.group(1));
    	}
		return 1;
    }
    
	public static int indexToPage(int index, int perPage){
		return (index-1)/perPage+1;//easier than using math.ceil.
	}
	public static int pageToIndex(int page, int perPage, int offset){
		return (page-1)*perPage+1+offset;
	}
	/**
	 * Converts page number to index assuming default item-per-page.
	 * ONLY USE FOR FORUM/THREADS, posts REQUIRE the dynamic per-page setting.
	 * @param page
	 * @return starting index
	 */
	public static int forumPageToIndex(int page) {
		return Math.max(1, (page-1)*Constants.THREADS_PER_PAGE+1);
	}
	

	public static int getLastReadPage(int unread, int total, int postPerPage) {
		if(unread<0 || total < 1){
			return 1;
		}
		if(unread == 0){
			return indexToPage(total,postPerPage);
		}
		return indexToPage(total-unread+1,postPerPage);
	}
	
	/**
	 * Checks a page for forum errors, triggering error message callbacks if found.
	 * Detects forum closures, logged-out state, and banned/probate status.
	 * @param page Full HTML page to check.
	 * @param handler Messenger to send reply messages to.
	 * @return true if error is found, false otherwise
	 * @throws RemoteException
	 */
	public static String checkPageErrors(Document page, Messenger handler) throws RemoteException{
        if(page.getElementsByAttributeValue("id", "notregistered").size() > 0){
        	handler.send(Message.obtain(null, AwfulSyncService.MSG_ERR_NOT_LOGGED_IN, 0, 0));
        	return "Error - Not Logged In";
        }
        if(page.getElementById("closemsg") != null){
        	handler.send(Message.obtain(null, AwfulSyncService.MSG_ERROR_FORUMS_CLOSED, 0, 0, page.getElementsByClass("reason").text()));
        	return "Error - Forums Closed (Site Down)";
        }
        return null;
	}
}
