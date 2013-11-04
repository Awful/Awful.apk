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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ferg.awfulapp.util.AwfulUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.HorizontalScrollView;

import com.androidquery.AQuery;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.htmlwidget.HtmlView;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;

public class AwfulPost {
    private static final String TAG = "AwfulPost";

    public static final String PATH     = "/post";
    public static final Uri CONTENT_URI = Uri.parse("content://" + Constants.AUTHORITY + PATH);

    private static final Pattern fixCharacters_regex = Pattern.compile("([\\r\\f])");
	private static final Pattern youtubeId_regex = Pattern.compile("/v/([\\w_-]+)&?");
	private static final Pattern youtubeHDId_regex = Pattern.compile("/embed/([\\w_-]+)&?");
	private static final Pattern vimeoId_regex = Pattern.compile("clip_id=(\\d+)&?");
	private static final Pattern userid_regex = Pattern.compile("userid=(\\d+)");

    public static final String ID                    = "_id";
    public static final String POST_INDEX            = "post_index";
    public static final String THREAD_ID             = "thread_id";
    public static final String DATE                  = "date";
	public static final String REGDATE				 = "regdate";
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
	public static final String FORM_BOOKMARK = "bookmark";
	/** For comparing against replies to see if the user actually typed anything. **/
	public static final String REPLY_ORIGINAL_CONTENT = "original_reply";
	public static final String EDIT_POST_ID = "edit_id";




	private int mThreadId;
    private String mId;
    private String mDate;
    private String mRegDate;
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

	public String getRegDate() {
		return mRegDate;
	}

    public void setRegDate(String aRegDate) {
        mRegDate = aRegDate;
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
            int regdateIndex = aCursor.getColumnIndex(REGDATE);
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
                current.setRegDate(aCursor.getString(regdateIndex));
                current.setUserId(aCursor.getString(userIdIndex));
                current.setUsername(aCursor.getString(usernameIndex));
                current.setPreviouslyRead(aCursor.getInt(previouslyReadIndex) > 0 ? true : false);
                current.setLastReadUrl(aCursor.getInt(postIndexIndex)+"");
                current.setEditable(aCursor.getInt(editableIndex) == 1 ? true : false);
                current.setIsOp(aCursor.getInt(isOpIndex) == 1 ? true : false);
                current.setIsAdmin(aCursor.getInt(isAdminIndex) > 0 ? true : false);
                current.setIsMod(aCursor.getInt(isModIndex) > 0 ? true : false);
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
    
    private static Document convertVideos(Document contentNode, boolean inline, boolean hasFlash){

			Elements youtubeNodes = contentNode.getElementsByClass("youtube-player");
		
			for(Element youTube : youtubeNodes){
				String src = youTube.attr("src");
				int height = Integer.parseInt(youTube.attr("height"));
				int width = Integer.parseInt(youTube.attr("width"));
				Matcher youtubeMatcher = youtubeHDId_regex.matcher(src);
				if(youtubeMatcher.find()){
					String videoId = youtubeMatcher.group(1);
					String link = "http://www.youtube.com/watch?v=" + videoId;
					String image = "http://img.youtube.com/vi/" + videoId + "/0.jpg";
					if(!AwfulUtils.isICS()){
						Element youtubeLink = new Element(Tag.valueOf("a"),"");
						youtubeLink.attr("href", link);
						youtubeLink.attr("style", "background-color:#000;background-image:url("+image+");background-size:cover;background-repeat:no-repeat;background-position:center; position:relative;display:block;text-align:center; width:" + width + "; height:" + height);
						Element img = new Element(Tag.valueOf("img"),"");
						img.attr("class", "nolink videoPlayButton");
						img.attr("src", "file:///android_res/drawable/ic_menu_video.png");
						img.attr("style", "position:absolute;top:50%;left:50%;margin-top:-16px;margin-left:-16px;");
						youtubeLink.appendChild(img);
						youTube.replaceWith(youtubeLink);
					}else if(!inline){
						Element youtubeLink = new Element(Tag.valueOf("a"),"");
						youtubeLink.text(link);
						youtubeLink.attr("href", link);
						youTube.replaceWith(youtubeLink);
					}else{
						Element youtubeLink = new Element(Tag.valueOf("a"),"");
						youtubeLink.text(link);
						youtubeLink.attr("href", link);
						youTube.after(youtubeLink);
						youtubeLink.before(new Element(Tag.valueOf("br"),""));
					}
				}
			}
//		if(!inline || !hasFlash){
			
			Elements videoNodes = contentNode.getElementsByClass("bbcode_video");
			for(Element node : videoNodes){
				try{
					String src = null;
					int height = 0;
					int width = 0;
					Elements object = node.getElementsByTag("object");
					if(object.size() > 0){
						height = Integer.parseInt(object.get(0).attr("height"));
						width = Integer.parseInt(object.get(0).attr("width"));
						Elements emb = object.get(0).getElementsByTag("embed");
						if(emb.size() >0){
							src = emb.get(0).attr("src");
						}
					}
					if(src != null && height != 0 && width != 0){
						String link = null, image = null;
						Matcher youtube = youtubeId_regex.matcher(src);
						Matcher vimeo = vimeoId_regex.matcher(src);
						//we'll leave in the old youtube code in case something gets reverted
//						if(youtube.find()){
//							String videoId = youtube.group(1);
//							link = "http://www.youtube.com/watch?v=" + videoId;
//							image = "http://img.youtube.com/vi/" + videoId + "/0.jpg";
//						}else
						if(vimeo.find()){
							String videoId = vimeo.group(1);
							Element vimeoXML;
							try {
								vimeoXML = NetworkUtils.get("http://vimeo.com/api/v2/video/"+videoId+".xml");
							} catch (Exception e) {
								e.printStackTrace();
								continue;
							}
							if(vimeoXML.getElementsByTag("mobile_url").first() != null){
								link = vimeoXML.getElementsByTag("mobile_url").first().text().toString();
							}else{
								link = vimeoXML.getElementsByTag("url").first().text().toString();
							}
							image = vimeoXML.getElementsByTag("thumbnail_large").first().text().toString();
							src = link;
						}else{
							node.empty();
							Element ln = new Element(Tag.valueOf("a"),"");
							ln.attr("href", src);
							ln.text(src);
							node.replaceWith(ln);
							continue;
						}
						if(inline && !AwfulUtils.isICS()){
							node.empty();
							node.attr("style", "background-image:url("+image+");background-size:cover;background-repeat:no-repeat;background-position:center; position:relative;text-align:center; width:" + width + "; height:" + height);
							node.attr("onclick", "location.href=\""+link+"\"");
							Element img = new Element(Tag.valueOf("img"),"");
							img.attr("class", "nolink videoPlayButton");
							img.attr("src", "file:///android_res/drawable/ic_menu_video.png");
							img.attr("style", "position:absolute;top:50%;left:50%;margin-top:-23px;margin-left:-32px;");
							node.appendChild(img);
						}else if(inline && AwfulUtils.isICS()){
							//do nothing, let JS do that
						
						}else{
							node.empty();
							Element ln = new Element(Tag.valueOf("a"),"");
							ln.attr("href", link);
							ln.text(link);
							node.replaceWith(ln);
						}
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

    public static void syncPosts(ContentResolver content, Document aThread, int aThreadId, int unreadIndex, int opId, AwfulPreferences prefs, int startIndex){
        ArrayList<ContentValues> result = AwfulPost.parsePosts(aThread, aThreadId, unreadIndex, opId, prefs, startIndex);

        int resultCount = content.bulkInsert(CONTENT_URI, result.toArray(new ContentValues[result.size()]));
        Log.i(TAG, "Inserted "+resultCount+" posts into DB, threadId:"+aThreadId+" unreadIndex: "+unreadIndex);
    }
    
    public static ArrayList<ContentValues> parsePosts(Document aThread, int aThreadId, int unreadIndex, int opId, AwfulPreferences prefs, int startIndex){
    	ArrayList<ContentValues> result = new ArrayList<ContentValues>();
    	boolean lastReadFound = false;
		int index = startIndex;
        String update_time = new Timestamp(System.currentTimeMillis()).toString();
        Log.v(TAG,"Update time: "+update_time);
                
        convertVideos(aThread, prefs.inlineYoutube, prefs.hasFlash());
        
        Elements posts = aThread.getElementsByClass("post");
        for(Element postData : posts){
        	ContentValues post = new ContentValues();
        	post.put(THREAD_ID, aThreadId);
        	
        	//post id is formatted "post1234567", so we strip out the "post" prefix.
        	post.put(ID, Integer.parseInt(postData.id().replaceAll("\\D", "")));
        	
        	//timestamp for DB trimming after a week
            post.put(AwfulProvider.UPDATED_TIMESTAMP, update_time);
            
            //we calculate this beforehand, but now can pull this from the post (thanks cooch!)
            //wait actually no, FYAD doesn't support this. ~FYAD Privilege~
            try{
                post.put(POST_INDEX, Integer.parseInt(postData.attr("data-idx").replaceAll("\\D", "")));
            }catch(NumberFormatException nfe){
            	post.put(POST_INDEX, index);
            }
            
            //Check for "class=seenX", or just rely on unread index
            Elements seen = postData.getElementsByClass("seen");
            if(seen.size() < 1 && index > unreadIndex){
            	post.put(PREVIOUSLY_READ, 0);
            	lastReadFound = true;
            }else{
            	post.put(PREVIOUSLY_READ, 1);
            }
            index++;
            
            //set these to 0 now, update them if needed, probably should have used a default value in the SQL table
			post.put(IS_MOD, 0);
            post.put(IS_ADMIN, 0);
            
            //rather than repeatedly query for specific classes, we are just going to grab them all and run through them all
            Elements postClasses = postData.getElementsByAttribute("class");
            for(Element entry: postClasses){
            	String type = entry.attr("class");
            	
            	if (type.contains("author")) {
					post.put(USERNAME, entry.text().trim());
				}
            	
            	if (type.contains("registered")) {
					post.put(REGDATE, entry.text().trim());
				}

				if (type.contains("role-mod")) {
					post.put(IS_MOD, 1);
				}

				if (type.contains("role-admin")) {
                    post.put(IS_ADMIN, 1);
				}

				if (type.equalsIgnoreCase("title") && entry.children().size() > 0) {
					Elements avatar = entry.getElementsByTag("img");

					if (avatar.size() > 0) {
						post.put(AVATAR, avatar.get(0).attr("src"));
					}
					post.put(AVATAR_TEXT, entry.text().trim());
				}

				if (type.equalsIgnoreCase("postbody") || type.contains("complete_shit")) {
					Elements images = entry.getElementsByTag("img");

					for(Element img : images){
						//don't alter video mock buttons
						if((img.hasAttr("class") && img.attr("class").contains("videoPlayButton"))){
							continue;
						}
						boolean dontLink = false;
						boolean isTimg = img.hasClass("timg");
						Element parent = img.parent();
						String src = img.attr("src");

						if ((parent != null && parent.tagName().equalsIgnoreCase("a")) || (img.hasAttr("class") && img.attr("class").contains("nolink"))) { //image is linked, don't override
							dontLink = true;
						}
						if (img.hasAttr("title")) {
							if (!prefs.showSmilies) { //kill all emotes
								String name = img.attr("title");
								img.replaceWith(new Element(Tag.valueOf("p"),"").text(name));
							}
						} else {
							if (!lastReadFound && prefs.hideOldImages || !prefs.canLoadImages()) {
								if (!dontLink) {
									img.replaceWith(new Element(Tag.valueOf("a"),"").attr("href", src).text(src));
								} else {
									img.replaceWith(new Element(Tag.valueOf("p"),"").text(src));
								}
							} else {
								if (!dontLink) {
									String thumb = src;
									if(( !prefs.imgurThumbnails.equals("d") || (prefs.disableGifs && thumb.toLowerCase().contains(".gif"))) && thumb.contains("i.imgur.com")){
										int lastDot = thumb.lastIndexOf('.');
										thumb = thumb.substring(0, lastDot) + (prefs.imgurThumbnails.equals("d")?"h":prefs.imgurThumbnails) + thumb.substring(lastDot);
										img.attr("src", thumb);
									}
                                    if(prefs.disableTimgs || !isTimg){
                                        img.replaceWith(new Element(Tag.valueOf("a"),"").attr("href", src).appendChild(new Element(Tag.valueOf("img"),"").attr("src", thumb)));
                                    }
								}
							}
						}
					}

					StringBuffer fixedContent = new StringBuffer();
					Matcher fixCharMatch = fixCharacters_regex.matcher(entry.html());

                    while (fixCharMatch.find()) {
                        fixCharMatch.appendReplacement(fixedContent, "");
                    }

					fixCharMatch.appendTail(fixedContent);
                    post.put(CONTENT, fixedContent.toString());
				}

				if (type.equalsIgnoreCase("postdate")) {
					post.put(DATE, NetworkUtils.unencodeHtml(entry.text()).replaceAll("[^\\w\\s:,]", "").trim());
				}
				
				if (type.equalsIgnoreCase("profilelinks")) {
					Elements userlink = entry.getElementsByAttributeValueContaining("href","userid=");

					if (userlink.size() > 0) {
						Matcher userid = userid_regex.matcher(userlink.get(0).attr("href"));
						if(userid.find()){
							int uid = Integer.parseInt(userid.group(1));
							post.put(USER_ID, uid);
                        	post.put(IS_OP, (opId == uid ? 1 : 0));
						}else{
							Log.e(TAG, "Failed to parse UID!");
						}
					}else{
						Log.e(TAG, "Failed to parse UID!");
					}
				}
				if (type.equalsIgnoreCase("editedby") && entry.children().size() > 0) {
					post.put(EDITED, "<i>" + entry.children().get(0).text().trim() + "</i>");
				}
            	
            }
            post.put(EDITABLE, postData.getElementsByAttributeValue("alt", "Edit").size());
            result.add(post);
        }
        Log.i(TAG, Integer.toString(posts.size())+" posts found, "+result.size()+" posts parsed.");
    	return result;
    }

	public static void getView(View current, AQuery aq, AwfulPreferences mPrefs, final Cursor data, final Messenger buttonCallback) {
		aq.recycle(current);
//		aq.find(R.id.post_author).visible().text(Html.fromHtml(data.getString(data.getColumnIndex(USERNAME)))).textColor(current.getResources().getColor(R.color.default_post_font));
		aq.find(R.id.post_author).visible().text(data.getString(data.getColumnIndex(USERNAME))).textColor(current.getResources().getColor(R.color.default_post_font));
//		aq.find(R.id.post_date).visible().text(Html.fromHtml(data.getString(data.getColumnIndex(DATE)))).textColor(current.getResources().getColor(R.color.default_post_font));
		aq.find(R.id.post_date).visible().text(data.getString(data.getColumnIndex(DATE))).textColor(current.getResources().getColor(R.color.default_post_font));
		int background = 0;
		if(data.getInt(data.getColumnIndex(PREVIOUSLY_READ)) > 0){
			background = current.getResources().getColor(R.color.background_read);
		}else{
			background = current.getResources().getColor(R.color.background);
		}
//		final Drawable frog = current.getContext().getResources().getDrawable(R.drawable.icon);
//		Spanned postContent = Html.fromHtml(data.getString(data.getColumnIndex(CONTENT)),null,new TagHandler() {
//			
//			@Override
//			public void handleTag(boolean opening, String tag, Editable output,
//					XMLReader xmlReader) {
//				// TODO Auto-generated method stub
//				
//			}
//		});
		HtmlView contentView = (HtmlView) aq.find(R.id.post_content).visible().textColor(current.getResources().getColor(R.color.default_post_font)).backgroundColor(background).getView();
		contentView.cancelTasks();
		contentView.setMovementMethod(LinkMovementMethod.getInstance());
		contentView.setHtml(data.getString(data.getColumnIndex(CONTENT)), true);
		aq.find(R.id.post_avatar).visible().image(data.getString(data.getColumnIndex(AVATAR)), true, true, 96, 0);
		aq.find(R.id.post_avatar_text).text(data.getString(data.getColumnIndex(AVATAR_TEXT))).textColor(current.getResources().getColor(R.color.default_post_font)).gone();
		aq.find(R.id.post_header).backgroundColor(current.getResources().getColor(R.color.forums_blue));
		OnClickListener buttonClick = new OnClickListener(){
			private int id = data.getInt(data.getColumnIndex(ID));
			private Messenger notify = buttonCallback;
			@Override
			public void onClick(View v) {
				try {
					//Send message for the button press, attach post ID
					notify.send(Message.obtain(null, v.getId(), id, 0));
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		};
		aq.find(R.id.post_edit_button).clicked(buttonClick).visible();
		if(data.getInt(data.getColumnIndex(EDITABLE)) < 1){
			aq.find(R.id.post_edit_button).gone();
		}
		aq.find(R.id.post_copyurl_button).clicked(buttonClick);
		aq.find(R.id.post_pm_button).clicked(buttonClick);
		aq.find(R.id.post_quote_button).clicked(buttonClick);//TODO hide button if thread locked
		aq.find(R.id.post_userposts_button).clicked(buttonClick);
		aq.find(R.id.post_last_read).clicked(buttonClick);
		aq.find(R.id.post_header).clicked(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				View avatarText = v.findViewById(R.id.post_avatar_text);
				avatarText.setVisibility(avatarText.getVisibility() == View.GONE? View.VISIBLE : View.GONE);
				HorizontalScrollView buttonContainer = (HorizontalScrollView) v.findViewById(R.id.post_button_scoller);
				buttonContainer.setVisibility(buttonContainer.getVisibility() == View.GONE? View.VISIBLE : View.GONE);
				buttonContainer.scrollTo(buttonContainer.getWidth(), 0);
			}
		});
		aq.find(R.id.post_button_scoller).backgroundColor(current.getResources().getColor(R.color.background)).gone();
	}

}
