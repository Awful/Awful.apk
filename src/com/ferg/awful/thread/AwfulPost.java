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
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import android.util.Log;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;

public class AwfulPost {
    private static final String TAG = "AwfulPost";

    /*private static final String USERNAME_SEARCH = "//dt[@class='author']|//dt[@class='author op']|//dt[@class='author role-mod']|//dt[@class='author role-admin']|//dt[@class='author role-mod op']|//dt[@class='author role-admin op']";
    private static final String MOD_SEARCH      = "//dt[@class='author role-mod']|//dt[@class='author role-mod op']";
    private static final String ADMIN_SEARCH    = "//dt[@class='author role-admin']|//dt[@class='author role-admin op']";

    private static final String USERNAME  = "//dt[@class='author']";
    private static final String OP        = "//dt[@class='author op']";
	private static final String MOD       = "//dt[@class='author role-mod']";
	private static final String ADMIN     = "//dt[@class='author role-admin']";
    private static final String POST      = "//table[@class='post']";
    private static final String POST_ID   = "//table[@class='post']";
    private static final String POST_DATE = "//td[@class='postdate']";
    private static final String SEEN_LINK = "//td[@class='postdate']//a";
    private static final String AVATAR    = "//dd[@class='title']//img";
    private static final String EDITED    = "//p[@class='editedby']/span";
    private static final String POSTBODY  = "//td[@class='postbody']";
	private static final String SEEN1     = "//tr[@class='seen1']";
	private static final String SEEN2     = "//tr[@class='seen2']";
	private static final String SEEN      = SEEN1+"|"+SEEN2;
	private static final String USERINFO  = "//tr[position()=1]/td[position()=1]"; //this would be nicer if HtmlCleaner supported starts-with
	private static final String PROFILE_LINKS = "//ul[@class='profilelinks']//a";
    private static final String EDITABLE  = "//img[@alt='Edit']";
    */
    private static final Pattern fixCharacters = Pattern.compile("([\\n\\r\\f\u0091-\u0095\u0099\u0080\u0082\u0083])");
    private static HashMap<String,String> replaceMap = new HashMap<String,String>(10);
    static {
    	replaceMap.put("\u0080", "\u20AC");
    	replaceMap.put("\u0082", "\u201A");
    	replaceMap.put("\u0083", "\u0192");
    	replaceMap.put("\u0091", "'");
    	replaceMap.put("\u0092", "'");
    	replaceMap.put("\u0093", "\"");
    	replaceMap.put("\u0094", "\"");
    	replaceMap.put("\u0095", "\u2022");
    	replaceMap.put("\u0099", "\u2122");
    	replaceMap.put("\n", "");
    	replaceMap.put("\r", "");
    	replaceMap.put("\f", "");
    }
    
	private static final String USERINFO_PREFIX = "userinfo userid-";

    private static final String ELEMENT_POSTBODY     = "<td class=\"postbody\">";
    private static final String ELEMENT_END_TD       = "</td>";
    private static final String REPLACEMENT_POSTBODY = "<div class=\"postbody\">";
    private static final String REPLACEMENT_END_TD   = "</div>";
    
    private static final String LINK_PROFILE      = "Profile";
    private static final String LINK_MESSAGE      = "Message";
    private static final String LINK_POST_HISTORY = "Post History";
    private static final String LINK_RAP_SHEET    = "Rap Sheet";

    private String mId;
    private String mDate;
    private String mUserId;
    private String mUsername;
    private String mAvatar;
    private String mContent;
    private String mEdited;
	private boolean mLastRead = false;
	private boolean mPreviouslyRead = false;
	private boolean mEven = false;
	private boolean mHasProfileLink = false;
	private boolean mHasMessageLink = false;
	private boolean mHasPostHistoryLink = false;
	private boolean mHasRapSheetLink = false;
    private String mLastReadUrl;
    private boolean mEditable;

    public String getId() {
        return mId;
    }

    public void setId(String aId) {
        mId = aId;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String aDate) {
        mDate = aDate;
    }

    public String getUserId() {
    	return mUserId;
    }
    
    public void setUserId(String aUserId) {
    	mUserId = aUserId;
    }
    
    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String aUsername) {
        mUsername = aUsername;
    }

    public String getAvatar() {
        return mAvatar;
    }

    public void setAvatar(String aAvatar) {
        mAvatar = aAvatar;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String aContent) {
        mContent = aContent;
    }

    public String getEdited() {
        return mEdited;
    }

    public void setEdited(String aEdited) {
        mEdited = aEdited;
    }

    public String getLastReadUrl() {
        return mLastReadUrl;
    }

    public void setLastReadUrl(String aLastReadUrl) {
        mLastReadUrl = aLastReadUrl;
    }

	public boolean isLastRead() {
		return mLastRead;
	}

	public void setLastRead(boolean aLastRead) {
		mLastRead = aLastRead;
	}
	
	public boolean isPreviouslyRead() {
		return mPreviouslyRead;
	}

	public void setPreviouslyRead(boolean aPreviouslyRead) {
		mPreviouslyRead = aPreviouslyRead;
	}
	
	public void setEven(boolean mEven) {
		this.mEven = mEven;
	}

	public boolean isEven() {
		return mEven;
	}
	
	public void setHasProfileLink(boolean aHasProfileLink) {
		mHasProfileLink = aHasProfileLink;
	}
	
	public boolean hasProfileLink() {
		return mHasProfileLink;
	}
	
	public void setHasMessageLink(boolean aHasMessageLink) {
		mHasMessageLink = aHasMessageLink;
	}
	
	public boolean hasMessageLink() {
		return mHasMessageLink;
	}
	
	public void setHasPostHistoryLink(boolean aHasPostHistoryLink) {
		mHasPostHistoryLink = aHasPostHistoryLink;
	}
	
	public boolean hasPostHistoryLink() {
		return mHasPostHistoryLink;
	}
	
	public void setHasRapSheetLink(boolean aHasRapSheetLink) {
		mHasRapSheetLink = aHasRapSheetLink;
	}

	public boolean isEditable() {
		return mEditable;
	}
	
	public void setEditable(boolean aEditable) {
		mEditable = aEditable;
	}
	
	public boolean hasRapSheetLink() {
		return mHasRapSheetLink;
	}

    public ArrayList<AwfulPost> markLastRead() {
        ArrayList<AwfulPost> result = new ArrayList<AwfulPost>();

        try {
            TagNode response = NetworkUtils.get(Constants.BASE_URL + mLastReadUrl);
            
            result = parsePosts(response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static ArrayList<AwfulPost> parsePosts(TagNode aThread) {
        ArrayList<AwfulPost> result = new ArrayList<AwfulPost>();

		boolean lastReadFound = false;
		boolean even = false;
        try {
        	TagNode[] postNodes = aThread.getElementsByAttValue("class", "post", true, true);
            for (TagNode node : postNodes) {
				
                AwfulPost post = new AwfulPost();

                // We'll just reuse the array of objects rather than create 
                // a ton of them
                String id = node.getAttributeByName("id");
                post.setId(id.replaceAll("post", ""));
                
                TagNode[] postContent = node.getElementsHavingAttribute("class", true);
                for(TagNode pc : postContent){
					if(pc.getAttributeByName("class").contains("author")){
						post.setUsername(pc.getText().toString().trim());
					}
					if(pc.getAttributeByName("class").equalsIgnoreCase("title") && pc.getChildTags().length >0){
						TagNode[] avatar = pc.getElementsByName("img", true);
						if(avatar.length >0){
							post.setAvatar(avatar[0].getAttributeByName("src"));
						}
					}
					if(pc.getAttributeByName("class").equalsIgnoreCase("postbody")){
						StringBuffer fixedContent = new StringBuffer();
						Matcher fixCharMatch = fixCharacters.matcher(NetworkUtils.getAsString(pc));
						while(fixCharMatch.find()){
							fixCharMatch.appendReplacement(fixedContent, replaceMap.get(fixCharMatch.group(1)));
							}
						fixCharMatch.appendTail(fixedContent);
	                    post.setContent(fixedContent.toString());
					}
					if(pc.getAttributeByName("class").equalsIgnoreCase("postdate")){//done
						if(pc.getChildTags().length>0){
							post.setLastReadUrl(pc.getChildTags()[0].getAttributeByName("href").replaceAll("&amp;", "&"));
						}
						post.setDate(pc.getText().toString().replaceAll("[^\\w\\s:,]", "").trim());
					}
					if(pc.getAttributeByName("class").equalsIgnoreCase("profilelinks")){
						TagNode[] links = pc.getElementsHavingAttribute("href", true);
						if(links.length >0){
							String href = links[0].getAttributeByName("href").trim();
							post.setUserId(href.substring(href.lastIndexOf("rid=")+4));
							for (TagNode linkNode : links) {
			                	String link = linkNode.getText().toString();
			                	if     (link.equals(LINK_PROFILE))      post.setHasProfileLink(true);
			                	else if(link.equals(LINK_MESSAGE))      post.setHasMessageLink(true);
			                	else if(link.equals(LINK_POST_HISTORY)) post.setHasPostHistoryLink(true);
			                	// Rap sheet is actually filled in by javascript for some stupid reason
			                }
						}
					}
					if(pc.getAttributeByName("class").contains("seen") && !lastReadFound){
						post.setPreviouslyRead(true);
					}

                    if (!post.isPreviouslyRead()) {
                        post.setLastRead(true);
                        lastReadFound = true;
                    }

					if(pc.getAttributeByName("class").equalsIgnoreCase("editedby") && pc.getChildTags().length >0){
						post.setEdited("<i>" + pc.getChildTags()[0].getText().toString() + "</i>");
					}
				}
                
				post.setEven(even); // even/uneven post for alternating colors
				even = !even;
				TagNode[] editImgs = node.getElementsByAttValue("alt", "Edit", true, true);
                if (editImgs.length > 0) {
                    Log.i(TAG, "Editable!");
                    post.setEditable(true);
                } else {
                    post.setEditable(false);
                }

                //it's always there though, so we can set it true without an explicit check
                post.setHasRapSheetLink(true);
                result.add(post);
            }

            Log.i(TAG, Integer.toString(postNodes.length));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private static String createPostHtml(String aHtml) {
        aHtml = aHtml.replaceAll(ELEMENT_POSTBODY, REPLACEMENT_POSTBODY);
        aHtml = aHtml.replaceAll(ELEMENT_END_TD, REPLACEMENT_END_TD);

        return aHtml;
    }

}
