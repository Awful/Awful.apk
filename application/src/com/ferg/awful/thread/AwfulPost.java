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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlcleaner.ContentNode;
import org.htmlcleaner.TagNode;
import org.json.*;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.provider.AwfulProvider;

public class AwfulPost {
    private static final String TAG = "AwfulPost";

    public static final String PATH     = "/post";
    public static final Uri CONTENT_URI = Uri.parse("content://" + Constants.AUTHORITY + PATH);

    private static final Pattern fixCharacters_regex = Pattern.compile("([\\r\\f])");
	private static final Pattern youtubeId_regex = Pattern.compile("/v/([\\w_-]+)&?");
	private static final Pattern youtubeHDId_regex = Pattern.compile("/embed/([\\w_-]+)&?");
	private static final Pattern vimeoId_regex = Pattern.compile("clip_id=(\\d+)&?");
	private static final Pattern postIndex_regex = Pattern.compile("index=(\\d+)");

    public static final String ID                    = "_id";
    public static final String POST_INDEX                    = "post_index";
    public static final String THREAD_ID             = "thread_id";
    public static final String DATE                  = "date";
    public static final String USER_ID               = "user_id";
    public static final String USERNAME              = "username";
    public static final String PREVIOUSLY_READ       = "previously_read";
    public static final String EDITABLE              = "editable";
    public static final String IS_OP                 = "is_op";
    public static final String IS_ADMIN              = "is_admin";
    public static final String IS_MOD                = "is_mod";
    public static final String AVATAR                = "avatar";
	public static final String AVATAR_TEXT 			 = "avatar_text";
    public static final String CONTENT               = "content";
    public static final String EDITED                = "edited";

	public static final String FORM_KEY = "form_key";
	public static final String FORM_COOKIE = "form_cookie";
	/** For comparing against replies to see if the user actually typed anything. **/
	public static final String REPLY_ORIGINAL_CONTENT = "original_reply";
	public static final String EDIT_POST_ID = "edit_id";



	private int mThreadId;
    private String mId;
    private String mDate;
    private String mUserId;
    private String mUsername;
    private String mAvatar;
    private String mAvatarText;
    private String mContent;
    private String mEdited;
    
	private boolean mPreviouslyRead = false;
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

    public void setThreadId(int aThreadId) {
        mThreadId = aThreadId;
    }

    public int getThreadId() {
        return mThreadId;
    }

    public void setIsOp(boolean aIsOp) {
        isOp = aIsOp;
    }

    public void setIsAdmin(boolean aIsAdmin) {
        isAdmin = aIsAdmin;
    }

    public void setIsMod(boolean aIsMod) {
        isMod = aIsMod;
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

    public static ArrayList<AwfulPost> fromCursor(Context aContext, Cursor aCursor) {
        ArrayList<AwfulPost> result = new ArrayList<AwfulPost>();

        if (aCursor.moveToFirst()) {
            int idIndex = aCursor.getColumnIndex(ID);
            int threadIdIndex = aCursor.getColumnIndex(THREAD_ID);
            int postIndexIndex = aCursor.getColumnIndex(POST_INDEX);//ooh, meta
            int dateIndex = aCursor.getColumnIndex(DATE);
            int userIdIndex = aCursor.getColumnIndex(USER_ID);
            int usernameIndex = aCursor.getColumnIndex(USERNAME);
            int previouslyReadIndex = aCursor.getColumnIndex(PREVIOUSLY_READ);
            int editableIndex = aCursor.getColumnIndex(EDITABLE);
            int isOpIndex = aCursor.getColumnIndex(IS_OP);
            int isAdminIndex = aCursor.getColumnIndex(IS_ADMIN);
            int isModIndex = aCursor.getColumnIndex(IS_MOD);
            int avatarIndex = aCursor.getColumnIndex(AVATAR);
            int avatarTextIndex = aCursor.getColumnIndex(AVATAR_TEXT);
            int contentIndex = aCursor.getColumnIndex(CONTENT);
            int editedIndex = aCursor.getColumnIndex(EDITED);

            AwfulPost current;

            do {
                current = new AwfulPost();
                current.setId(aCursor.getString(idIndex));
                current.setThreadId(aCursor.getInt(threadIdIndex));
                current.setDate(aCursor.getString(dateIndex));
                current.setUserId(aCursor.getString(userIdIndex));
                current.setUsername(aCursor.getString(usernameIndex));
                current.setPreviouslyRead(aCursor.getInt(previouslyReadIndex) > 0 ? true : false);
                current.setLastReadUrl(aCursor.getInt(postIndexIndex)+"");
                current.setEditable(aCursor.getInt(editableIndex) == 1 ? true : false);
                current.setIsOp(aCursor.getInt(isOpIndex) == 1 ? true : false);
                current.setIsAdmin(aCursor.getInt(isAdminIndex) == 1 ? true : false);
                current.setIsMod(aCursor.getInt(isModIndex) == 1 ? true : false);
                current.setAvatar(aCursor.getString(avatarIndex));
                current.setAvatarText(aCursor.getString(avatarTextIndex));
                current.setContent(aCursor.getString(contentIndex));
                current.setEdited(aCursor.getString(editedIndex));

                result.add(current);
            } while (aCursor.moveToNext());
        }else{
        	Log.i(TAG,"No posts to convert.");
        }
        return result;
    }

	private static TagNode convertVideos(TagNode contentNode) {
		TagNode[] videoNodes = contentNode.getElementsByAttValue("class", "bbcode_video", true, true);
		TagNode[] youtubeNodes = contentNode.getElementsByAttValue("class", "youtube-player", true, true);
		for(TagNode youTube : youtubeNodes){
			String src = youTube.getAttributeByName("src");
			int height = Integer.parseInt(youTube.getAttributeByName("height"));
			int width = Integer.parseInt(youTube.getAttributeByName("width"));
			Matcher youtube = youtubeHDId_regex.matcher(src);
			if(youtube.find()){
				String videoId = youtube.group(1);
				String link = "http://www.youtube.com/watch?v=" + videoId;
				String image = "http://img.youtube.com/vi/" + videoId + "/0.jpg";
				youTube.setName("a");
				youTube.setAttribute("href", link);
				youTube.removeAttribute("type");
				youTube.removeAttribute("frameborder");
				youTube.removeAttribute("src");
				youTube.removeAttribute("height");
				youTube.removeAttribute("width");
				youTube.setAttribute("style", "background-image:url("+image+"); position:relative;display:block;text-align:center; width:" + width + "; height:" + height);
				TagNode img = new TagNode("img");
				img.setAttribute("class", "nolink");
				img.setAttribute("src", "file:///android_res/drawable/ic_menu_video.png");
				img.setAttribute("style", "position:absolute;top:50%;left:50%;margin-top:-16px;margin-left:-16px;");
				youTube.addChild(img);
			}
		}
		for(TagNode node : videoNodes){
			try{
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
					Matcher youtube = youtubeId_regex.matcher(src);
					Matcher vimeo = vimeoId_regex.matcher(src);
					if(youtube.find()){//we'll leave in the old youtube code in case something gets reverted
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
			}catch(Exception e){
				continue;//if we fail to convert the video tag, we can still display the rest.
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

    public String getAvatarText() {
    	return mAvatarText;
	}

    public void setAvatarText(String text) {
    	mAvatarText = text;
	}


    public String getLastReadUrl() {
        return mLastReadUrl;
    }

    public void setLastReadUrl(String aLastReadUrl) {
        mLastReadUrl = aLastReadUrl;
    }
	
	public boolean isPreviouslyRead() {
		return mPreviouslyRead;
	}

	public void setPreviouslyRead(boolean aPreviouslyRead) {
		mPreviouslyRead = aPreviouslyRead;
	}
	
	public boolean isEditable() {
		return mEditable;
	}
	
	public void setEditable(boolean aEditable) {
		mEditable = aEditable;
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

    public static void syncPosts(ContentResolver content, TagNode aThread, int aThreadId, int unreadIndex, int opId, AwfulPreferences prefs, int startIndex){
        ArrayList<ContentValues> result = AwfulPost.parsePosts(aThread, aThreadId, unreadIndex, opId, prefs, startIndex);

        int resultCount = content.bulkInsert(CONTENT_URI, result.toArray(new ContentValues[result.size()]));
        Log.i(TAG, "Inserted "+resultCount+" posts into DB, threadId:"+aThreadId+" unreadIndex: "+unreadIndex);
    }

    public static ArrayList<ContentValues> parsePosts(TagNode aThread, int aThreadId, int unreadIndex, int opId, AwfulPreferences prefs, int startIndex){
        ArrayList<ContentValues> result = new ArrayList<ContentValues>();
		boolean lastReadFound = false;
		int index = startIndex;
        String update_time = new Timestamp(System.currentTimeMillis()).toString();
        Log.v(TAG,"Update time: "+update_time);
        try {
        	aThread = convertVideos(aThread);
        	TagNode[] postNodes = aThread.getElementsByAttValue("class", "post", true, true);
			boolean fyad = false;

            for (TagNode node : postNodes) {
            	//fyad status, to prevent processing postbody twice if we are in fyad
                ContentValues post = new ContentValues();                
                post.put(THREAD_ID, aThreadId);

                // We'll just reuse the array of objects rather than create 
                // a ton of them
                int id = Integer.parseInt(node.getAttributeByName("id").replaceAll("post", ""));
                post.put(ID, id);
                post.put(AwfulProvider.UPDATED_TIMESTAMP, update_time);
                post.put(POST_INDEX, index);
                if(index > unreadIndex){
                	post.put(PREVIOUSLY_READ, 0);
                	lastReadFound = true;
                }else{
                	post.put(PREVIOUSLY_READ, 1);
                }
                index++;
                
                TagNode[] postContent = node.getElementsHavingAttribute("class", true);
                for(TagNode pc : postContent){
					if (pc.getAttributeByName("class").contains("author")) {
						post.put(USERNAME, pc.getText().toString().trim());
					}

					if (pc.getAttributeByName("class").contains("role-mod")) {
						post.put(IS_MOD, 1);
					} else {
						post.put(IS_MOD, 0);
                    }

					if (pc.getAttributeByName("class").contains("role-admin")) {
                        post.put(IS_ADMIN, 1);
					} else {
                        post.put(IS_ADMIN, 0);
                    }

					if (pc.getAttributeByName("class").equalsIgnoreCase("title") && pc.getChildTags().length > 0) {
						TagNode[] avatar = pc.getElementsByName("img", true);

						if (avatar.length > 0) {
							post.put(AVATAR, avatar[0].getAttributeByName("src"));
						}
						post.put(AVATAR_TEXT, pc.getText().toString().trim());
					}

					if (pc.getAttributeByName("class").contains("complete_shit")) {
						StringBuffer fixedContent = new StringBuffer();
						Matcher fixCharMatch = fixCharacters_regex.matcher(NetworkUtils.getAsString(pc));

                        while (fixCharMatch.find()) {
                            fixCharMatch.appendReplacement(fixedContent, "");
                        }

						fixCharMatch.appendTail(fixedContent);
	                    post.put(CONTENT, fixedContent.toString());
	                    fyad = true;
					}

					if (pc.getAttributeByName("class").equalsIgnoreCase("postbody") && !fyad) {
						TagNode[] images = pc.getElementsByName("img", true);

						for(TagNode img : images){
							boolean dontLink = false;
							TagNode parent = img.getParent();
							String src = img.getAttributeByName("src");

							if ((parent != null && parent.getName().equals("a")) || (img.hasAttribute("class") && img.getAttributeByName("class").contains("nolink"))) { //image is linked, don't override
								dontLink = true;
							}

							if (img.hasAttribute("title")) {
								if (!prefs.showSmilies) { //kill all emotes
									String name = img.getAttributeByName("title");
									img.setName("p");
									img.addChild(new ContentNode(name));
								}
							} else {
								if (!lastReadFound && prefs.hideOldImages || !prefs.imagesEnabled) {
									if (!dontLink) {
										img.setName("a");
										img.setAttribute("href", src);
										img.addChild(new ContentNode(src));
									} else {
										img.setName("p");
										img.addChild(new ContentNode(src));
									}
								} else {
									if (!dontLink) {
										img.setName("a");
										img.setAttribute("href", src);
										TagNode newimg = new TagNode("img");
										if(!prefs.imgurThumbnails.equals("d") && src.contains("i.imgur.com")){
											int lastSlash = src.lastIndexOf('/');
											if(src.length()-lastSlash<=9){
											int pos = src.length() - 4;
												src = src.substring(0, pos) + prefs.imgurThumbnails + src.substring(pos);
											}
										}
										newimg.setAttribute("src", src);
										img.addChild(newimg);
									}
								}
							}
						}

						StringBuffer fixedContent = new StringBuffer();
						Matcher fixCharMatch = fixCharacters_regex.matcher(NetworkUtils.getAsString(pc));

                        while (fixCharMatch.find()) {
                            fixCharMatch.appendReplacement(fixedContent, "");
                        }

						fixCharMatch.appendTail(fixedContent);
	                    post.put(CONTENT, fixedContent.toString());
					}

					if (pc.getAttributeByName("class").equalsIgnoreCase("postdate")) {
						post.put(DATE, pc.getText().toString().replaceAll("[^\\w\\s:,]", "").trim());
					}
					
					if (pc.getAttributeByName("class").equalsIgnoreCase("profilelinks")) {
						TagNode[] links = pc.getElementsHavingAttribute("href", true);

						if (links.length > 0) {
							String href = links[0].getAttributeByName("href").trim();
                            String userId = href.substring(href.lastIndexOf("rid=") + 4);

							post.put(USER_ID, userId);

							if (Integer.toString(opId).equals(userId)) {//ugh
                                post.put(IS_OP, 1);
							} else {
                                post.put(IS_OP, 0);
                            }
						}
					}
					if (pc.getAttributeByName("class").equalsIgnoreCase("editedby") && pc.getChildTags().length > 0) {
						post.put(EDITED, "<i>" + pc.getChildTags()[0].getText().toString() + "</i>");
					}
				}
				TagNode[] editImgs = node.getElementsByAttValue("alt", "Edit", true, true);

                if (editImgs.length > 0) {
                    post.put(EDITABLE, 1);
                } else {
                    post.put(EDITABLE, 0);
                }
                result.add(post);
            }

            Log.i(TAG, Integer.toString(postNodes.length)+" posts found, "+result.size()+" posts parsed.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

}
