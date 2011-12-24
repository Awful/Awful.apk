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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.*;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;

public class AwfulPost implements AwfulDisplayItem {
    private static final String TAG = "AwfulPost";

    private static final Pattern fixCharacters = Pattern.compile("([\\r\\f])");
	private static final Pattern youtubeId = Pattern.compile("/v/([\\w_-]+)&?");
	private static final Pattern vimeoId = Pattern.compile("clip_id=(\\d+)&?");
    
    private static final String LINK_PROFILE      = "Profile";
    private static final String LINK_MESSAGE      = "Message";
    private static final String LINK_POST_HISTORY = "Post History";

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
    private boolean isOp = false;
    private boolean isAdmin = false;
    private boolean isMod = false;

    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("id", mId);
        result.put("date", mDate);
        result.put("user_id", mUserId);
        result.put("username", mUsername);
        result.put("avatar", mAvatar);
        result.put("content", mContent);
        result.put("edited", mEdited);
        result.put("previouslyRead", Boolean.toString(mPreviouslyRead));
        result.put("lastReadUrl", mLastReadUrl);
        result.put("editable", Boolean.toString(mEditable));
        result.put("isOp", Boolean.toString(isOp()));

        return result;
    }

    public boolean isOp() {
    	return isOp;
    }
    
    public boolean isAdmin() {
    	return isAdmin;
    }
    
    public boolean isMod() {
    	return isMod;
    }
    
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

    private static TagNode convertVideos(TagNode contentNode) {
		
		
		TagNode[] videoNodes = contentNode.getElementsByAttValue("class", "bbcode_video", true, true);
		for(TagNode node : videoNodes){
			String src = null;
			int height = 0;
			int width = 0;
			TagNode[] object = node.getElementsByName("object", false);
			if(object.length > 0){
				height = Integer.parseInt(object[0].getAttributeByName("height"));
				width = Integer.parseInt(object[0].getAttributeByName("width"));
				TagNode[] emb = object[0].getElementsByName("embed", true);
				if(emb.length >0){
					src = emb[0].getAttributeByName("src");
				}
			}
			if(src != null && height != 0 && width != 0){
				String link = null, image = null;
				Matcher youtube = youtubeId.matcher(src);
				Matcher vimeo = vimeoId.matcher(src);
				if(youtube.find()){
					String videoId = youtube.group(1);
					link = "http://www.youtube.com/watch?v=" + videoId;
					image = "http://img.youtube.com/vi/" + videoId + "/0.jpg";
				}else if(vimeo.find()){
					String videoId = vimeo.group(1);
					TagNode vimeoXML;
					try {
						vimeoXML = NetworkUtils.get("http://vimeo.com/api/v2/video/"+videoId+".xml");
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
					if(vimeoXML.findElementByName("mobile_url", true) != null){
						link = vimeoXML.findElementByName("mobile_url", true).getText().toString();
					}else{
						link = vimeoXML.findElementByName("url", true).getText().toString();
					}
					image = vimeoXML.findElementByName("thumbnail_large", true).getText().toString();
				}else{
					node.removeAllChildren();
					TagNode ln = new TagNode("a");
					ln.setAttribute("href", src);
					ln.addChild(new ContentNode(src));
					node.addChild(ln);
					continue;
				}
				node.removeAllChildren();
				node.setAttribute("style", "background-image:url("+image+"); position:relative;text-align:center; width:" + width + "; height:" + height);
				node.setAttribute("onclick", "location.href=\""+link+"\"");
				TagNode img = new TagNode("img");
				img.setAttribute("class", "nolink");
				img.setAttribute("src", "file:///android_res/drawable/play.png");
				img.setAttribute("style", "position:absolute;top:50%;left:50%;margin-top:-23px;margin-left:-32px;");
				node.addChild(img);
			}
		}
		
		return contentNode;
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

    public void markLastRead() {
        try {
            if(mLastReadUrl != null){
            	NetworkUtils.get(Constants.BASE_URL+ mLastReadUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<AwfulPost> parsePosts(TagNode aThread, int aPage, int postPerPage, AwfulThread aThreadObject, AwfulPreferences prefs){
        ArrayList<AwfulPost> result = new ArrayList<AwfulPost>();
        
        int lastReadPage = aThreadObject.getLastReadPage(postPerPage);
        int lastReadPost = aThreadObject.getLastReadPost(postPerPage);

		boolean lastReadFound = false;
		boolean even = false;
        try {
        	TagNode breadcrumbs = aThread.findElementByAttValue("class", "breadcrumbs", true, true);
        	TagNode[] forumlinks = breadcrumbs.getElementsByName("a", true);
        	TagNode forumlink = forumlinks[forumlinks.length-2];
        	String forumurl = forumlink.getAttributeByName("href").toString();
        	aThreadObject.setForumId(Integer.parseInt(forumurl.substring("showthread.php?threadid=".length()+1)));
        	aThread = convertVideos(aThread);
        	TagNode[] postNodes = aThread.getElementsByAttValue("class", "post", true, true);
            int index = 1;
			boolean fyad = false;
            for (TagNode node : postNodes) {
            	//fyad status, to prevent processing postbody twice if we are in fyad
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
					if(pc.getAttributeByName("class").contains("role-mod")){
						post.isMod = true;
					}
					if(pc.getAttributeByName("class").contains("role-admin")){
						post.isAdmin = true;
					}
					if(pc.getAttributeByName("class").equalsIgnoreCase("title") && pc.getChildTags().length >0){
						TagNode[] avatar = pc.getElementsByName("img", true);
						if(avatar.length >0){
							post.setAvatar(avatar[0].getAttributeByName("src"));
						}
					}
					if(pc.getAttributeByName("class").contains("complete_shit")){
						StringBuffer fixedContent = new StringBuffer();
						Matcher fixCharMatch = fixCharacters.matcher(NetworkUtils.getAsString(pc));
						while(fixCharMatch.find()){
							fixCharMatch.appendReplacement(fixedContent, "");
							}
						fixCharMatch.appendTail(fixedContent);
	                    post.setContent(fixedContent.toString());
	                    fyad = true;
					}
					if(pc.getAttributeByName("class").equalsIgnoreCase("postbody") && !fyad){
						TagNode[] images = pc.getElementsByName("img", true);
						for(TagNode img : images){
							boolean dontLink = false;
							TagNode parent = img.getParent();
							String src = img.getAttributeByName("src");
							if((parent != null && parent.getName().equals("a")) || (img.hasAttribute("class") && img.getAttributeByName("class").contains("nolink"))){//image is linked, don't override
								dontLink = true;
							}
							if(img.hasAttribute("title")){
								if(!prefs.showSmilies){//kill all emotes
									String name = img.getAttributeByName("title");
									img.setName("p");
									img.addChild(new ContentNode(name));
								}
							}else{
								if((post.mPreviouslyRead || !lastReadFound) && prefs.hideOldImages || !prefs.imagesEnabled){
									if(!dontLink){
										img.setName("a");
										img.setAttribute("href", src);
										img.addChild(new ContentNode(src));
									}else{
										img.setName("p");
										img.addChild(new ContentNode(src));
									}
								}else{
									if(!dontLink){
										img.setName("a");
										img.setAttribute("href", src);
										TagNode newimg = new TagNode("img");
										if(!prefs.imgurThumbnails.equals('d') && src.contains("i.imgur.com")){
											int pos = src.length() - 4;
											src = src.substring(0, pos) + prefs.imgurThumbnails + src.substring(pos);
										}
										newimg.setAttribute("src", src);
										img.addChild(newimg);
									}
								}
							}
						}
						StringBuffer fixedContent = new StringBuffer();
						Matcher fixCharMatch = fixCharacters.matcher(NetworkUtils.getAsString(pc));
						while(fixCharMatch.find()){
							fixCharMatch.appendReplacement(fixedContent, "");
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
							if(aThreadObject != null && aThreadObject.getAuthorID() != null && aThreadObject.getAuthorID().equals(post.getUserId())){
								post.isOp = true;
							}
							for (TagNode linkNode : links) {
			                	String link = linkNode.getText().toString();
			                	if     (link.equals(LINK_PROFILE))      post.setHasProfileLink(true);
			                	else if(link.equals(LINK_MESSAGE))      post.setHasMessageLink(true);
			                	else if(link.equals(LINK_POST_HISTORY)) post.setHasPostHistoryLink(true);
			                	// Rap sheet is actually filled in by javascript for some stupid reason
			                }
						}
					}
					if(aPage < lastReadPage || (aPage == lastReadPage && index <= lastReadPost) ||
					   (pc.getAttributeByName("class").contains("seen") && !lastReadFound)){
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
                index++;
            }

            Log.i(TAG, Integer.toString(postNodes.length));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

	@Override
	public View getView(LayoutInflater inf, View current, ViewGroup parent, AwfulPreferences mPrefs) {
        return null;
    }

	@Override
	public int getID() {
		return Integer.parseInt(mId);
	}

	@Override
	public DISPLAY_TYPE getType() {
		return DISPLAY_TYPE.POST;
	}

	@Override
	public boolean isEnabled() {
		return false;
	}

}
