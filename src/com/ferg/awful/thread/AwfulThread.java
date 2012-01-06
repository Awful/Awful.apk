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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.htmlcleaner.TagNode;

import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.*;

import com.ferg.awful.R;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;
import com.ferg.awful.preferences.AwfulPreferences;
import com.ferg.awful.preferences.ColorPickerPreference;

public class AwfulThread extends AwfulPagedItem implements AwfulDisplayItem {
    private static final String TAG = "AwfulThread";

    private String mThreadId;
    private int threadId;
    private String mAuthor;
    private String mAuthorID;
    private boolean mSticky;
    private String mIcon;
    private int mUnreadCount;
	private int mTotalPosts;
    private boolean mClosed;
	private boolean mBookmarked;
	private String mKilledBy;
	private int forumId;
    private HashMap<Integer, ArrayList<AwfulPost>> mPosts;

    public AwfulThread() {
    	mPosts = new HashMap<Integer, ArrayList<AwfulPost>>();
    }

    public AwfulThread(String aThreadId) {
    	setThreadId(aThreadId);
    	mPosts = new HashMap<Integer, ArrayList<AwfulPost>>();
    }
    public AwfulThread(int aThreadId) {
    	setThreadId(aThreadId+"");
    	mPosts = new HashMap<Integer, ArrayList<AwfulPost>>();
    }
    
    
    public static TagNode getForumThreads(String aForumId) throws Exception {
		return getForumThreads(aForumId, 1);
	}
	
    public static TagNode getForumThreads(String aForumId, int aPage) throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(Constants.PARAM_FORUM_ID, aForumId);

		if (aPage != 0) {
			params.put(Constants.PARAM_PAGE, Integer.toString(aPage));
		}

        return NetworkUtils.get(Constants.FUNCTION_FORUM, params);
	}
	
    public static TagNode getUserCPThreads(int aPage) throws Exception {
    	HashMap<String, String> params = new HashMap<String, String>();
		params.put(Constants.PARAM_PAGE, Integer.toString(aPage));
        return NetworkUtils.get(Constants.FUNCTION_BOOKMARK, params);
	}

	public static ArrayList<AwfulThread> parseForumThreads(TagNode aResponse, int postPerPage) throws Exception {
        ArrayList<AwfulThread> result = new ArrayList<AwfulThread>();
        TagNode[] threads = aResponse.getElementsByAttValue("id", "forum", true, true);
        if(threads.length >1 || threads.length < 1){
        	return result;
        }
        TagNode[] tbody = threads[0].getElementsByName("tbody", false);
		for(TagNode node : tbody[0].getChildTags()){
            AwfulThread thread = new AwfulThread();
            
            try {
                String threadId = node.getAttributeByName("id");
                thread.setThreadId(threadId.replaceAll("thread", ""));
            } catch (NullPointerException e) {
                // If we can't parse a row, just skip it
                e.printStackTrace();
                continue;
            }
            	TagNode[] tarThread = node.getElementsByAttValue("class", "thread_title", true, true);
            	TagNode[] tarPostCount = node.getElementsByAttValue("class", "replies", true, true);
            	if (tarPostCount.length > 0) {
                    thread.setTotalCount(Integer.parseInt(tarPostCount[0].getText().toString().trim()), postPerPage);
                }
            	TagNode[] tarUser = node.getElementsByAttValue("class", "author", true, true);
                if (tarThread.length > 0) {
                    thread.setTitle(tarThread[0].getText().toString().trim());
                }

                TagNode[] killedBy = node.getElementsByAttValue("class", "lastpost", true, true);
                thread.setKilledBy(killedBy[0].getElementsByAttValue("class", "author", true, true)[0].getText().toString());
                TagNode[] tarSticky = node.getElementsByAttValue("class", "title title_sticky", true, true);
                if (tarSticky.length > 0) {
                    thread.setSticky(true);
                } else {
                    thread.setSticky(false);
                }

                TagNode[] tarIcon = node.getElementsByAttValue("class", "icon", true, true);
                if (tarIcon.length > 0 && tarIcon[0].getChildTags().length >0) {
                    thread.setIcon(tarIcon[0].getChildTags()[0].getAttributeByName("src"));
                }

                if (tarUser.length > 0) {
                    // There's got to be a better way to do this
                    thread.setAuthor(tarUser[0].getText().toString().trim());
			    // And probably a much better way to do this
			    thread.setAuthorID(((TagNode)tarUser[0].getElementListHavingAttribute("href", true).get(0)).getAttributes().get("href").substring(((TagNode)tarUser[0].getElementListHavingAttribute("href", true).get(0)).getAttributes().get("href").indexOf("userid=")+7));
                }

                TagNode[] tarCount = node.getElementsByAttValue("class", "count", true, true);
                if (tarCount.length > 0 && tarCount[0].getChildTags().length >0) {
                    thread.setUnreadCount(Integer.parseInt(tarCount[0].getChildTags()[0].getText().toString().trim()));
                } else {
                	TagNode[] tarXCount = node.getElementsByAttValue("class", "x", true, true);
					if (tarXCount.length > 0) {
						thread.setUnreadCount(0);
					} else {
						thread.setUnreadCount(-1);
					} 
                }
                TagNode[] tarStar = node.getElementsByAttValue("class", "star", true, true);
                if(tarStar.length>0){
                	TagNode[] tarStarImg = tarStar[0].getElementsByName("img", true);
                	if(tarStarImg.length >0 && !tarStarImg[0].getAttributeByName("src").contains("star-off")){
                		thread.setBookmarked(true);
                	}else{
                		thread.setBookmarked(false);
                	}
                }

                result.add(thread);
        }
        return result;
	}

	public void setBookmarked(boolean b) {
		mBookmarked = b;
	}
	public boolean isBookmarked() {
		return mBookmarked;
	}

	public static ArrayList<AwfulForum> parseSubforums(TagNode aResponse){
        ArrayList<AwfulForum> result = new ArrayList<AwfulForum>();
		TagNode[] subforums = aResponse.getElementsByAttValue("class", "subforum", true, false);
        for(TagNode sf : subforums){
        	TagNode[] href = sf.getElementsHavingAttribute("href", true);
        	if(href.length <1){
        		continue;
        	}
        	int id = Integer.parseInt(href[0].getAttributeByName("href").replaceAll("\\D", ""));
        	if(id > 0){
        		AwfulForum tmp = new AwfulForum(id);
        		tmp.setTitle(href[0].getText().toString());
        		TagNode[] subtext = sf.getElementsByName("dd", true);
        		if(subtext.length >0){
        			//Log.e(TAG,"parsed subtext: "+subtext[0].getText().toString().replaceAll("\"", "").trim().substring(2));
        			tmp.setSubtext(subtext[0].getText().toString().replaceAll("\"", "").trim().substring(2));//ugh
        		}
        		result.add(tmp);
        	}
        }
        return result;
    }

    public void getThreadPosts(int aPage, int postPerPage, AwfulPreferences prefs) throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(Constants.PARAM_THREAD_ID, mThreadId);
        params.put(Constants.PARAM_PER_PAGE, Integer.toString(postPerPage));
        params.put(Constants.PARAM_PAGE, Integer.toString(aPage));

        TagNode response = NetworkUtils.get(Constants.FUNCTION_THREAD, params);

        if (mTitle == null) {
        	TagNode[] tarTitle = response.getElementsByAttValue("class", "bclast", true, true);

            if (tarTitle.length > 0) {
                mTitle = tarTitle[0].getText().toString().trim();
            }
        }
        TagNode[] replyAlts = response.getElementsByAttValue("alt", "Reply", true, true);
        if(replyAlts.length >0 && replyAlts[0].getAttributeByName("src").contains("forum-closed")){
        	this.mClosed=true;
        }
        TagNode[] bkButtons = response.getElementsByAttValue("id", "button_bookmark", true, true);
        if(bkButtons.length >0){
        	String bkSrc = bkButtons[0].getAttributeByName("src");
        	setBookmarked(bkSrc != null && bkSrc.contains("unbookmark"));
        }
        int oldLastPage = getLastPage();
        int oldTotalCount = getTotalCount();
        parsePageNumbers(response);
        if(oldLastPage < getLastPage()){
			setTotalCount((getLastPage()-1)*postPerPage, postPerPage);
			setUnreadCount(getUnreadCount()+(getTotalCount()-oldTotalCount));
		}
        setPosts(AwfulPost.parsePosts(response, aPage, postPerPage, this, prefs), aPage);
    }

    public static String getHtml(ArrayList<AwfulPost> aPosts, AwfulPreferences aPrefs, boolean isTablet) {
        StringBuffer buffer = new StringBuffer("<html><head>");
        buffer.append("<meta name='viewport' content='width=device-width, height=device-height, target-densitydpi=device-dpi, initial-scale=1.0 maximum-scale=1.0 minimum-scale=1.0' />");
        buffer.append("<link rel='stylesheet' href='file:///android_asset/thread.css'>");
        
        if (!isTablet) {
            buffer.append("<link rel='stylesheet' href='file:///android_asset/thread-phone.css'>");
            buffer.append("<link rel='stylesheet' media='screen and (-webkit-device-pixel-ratio:1.5)' href='file:///android_asset/thread-hdpi.css' />");
            buffer.append("<link rel='stylesheet' media='screen and (-webkit-device-pixel-ratio:1)' href='file:///android_asset/thread-mdpi.css' />");
            buffer.append("<link rel='stylesheet' media='screen and (-webkit-device-pixel-ratio:.75)' href='file:///android_asset/thread-mdpi.css' />");
        }

        buffer.append("<script src='file:///android_asset/jquery.min.js' type='text/javascript'></script>");
        buffer.append("<script type='text/javascript'>");
        buffer.append("  window.JSON = null;");
        buffer.append("</script>");
        buffer.append("<script src='file:///android_asset/json2.js' type='text/javascript'></script>");
        buffer.append("<script src='file:///android_asset/ICanHaz.min.js' type='text/javascript'></script>");
        buffer.append("<script src='file:///android_asset/salr.js' type='text/javascript'></script>");
        buffer.append("<script src='file:///android_asset/thread.js' type='text/javascript'></script>");
        buffer.append("<style type='text/css'>");   
        buffer.append("a:link {color: "+ColorPickerPreference.convertToARGB(aPrefs.postLinkQuoteColor)+" }");
        buffer.append("a:visited {color: "+ColorPickerPreference.convertToARGB(aPrefs.postLinkQuoteColor)+"}");
        buffer.append("a:active {color: "+ColorPickerPreference.convertToARGB(aPrefs.postLinkQuoteColor)+"}");
        buffer.append("a:hover {color: "+ColorPickerPreference.convertToARGB(aPrefs.postLinkQuoteColor)+"}");
        buffer.append(".bbc-block { border-bottom: 1px "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor)+" solid; }");
        buffer.append(".bbc-block h4 { border-top: 1px "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor)+" solid; color: "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor2)+"; }");
        buffer.append(".bbc-spoiler, .bbc-spoiler li { color: "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor)+"; background: "+ColorPickerPreference.convertToARGB(aPrefs.postFontColor)+";}");
        
        buffer.append("</style>");
        buffer.append("</head><body>");
        buffer.append("<div class='content'>");
        buffer.append("    <table id='thread-body'>");

        if (isTablet) {
            buffer.append(AwfulThread.getPostsHtmlForTablet(aPosts, aPrefs));
        } else {
            buffer.append(AwfulThread.getPostsHtmlForPhone(aPosts, aPrefs));
        }

        buffer.append("    </table>");
        buffer.append("</div>");
        buffer.append("</body></html>");

        return buffer.toString();
    }

    public static String getPostsHtmlForPhone(ArrayList<AwfulPost> aPosts, AwfulPreferences aPrefs) {
        StringBuffer buffer = new StringBuffer();

        boolean light = true;
        String background = null;

        for (AwfulPost post : aPosts) {
        
            if (post.isPreviouslyRead()) {
                background = 
                    ColorPickerPreference.convertToARGB(light ? aPrefs.postReadBackgroundColor : aPrefs.postReadBackgroundColor2);
            } else {
                background = 
                    ColorPickerPreference.convertToARGB(light ? aPrefs.postBackgroundColor : aPrefs.postBackgroundColor2);
            }

            if(aPrefs.alternateBackground == true){
            	light = !light;
            }

            buffer.append("<tr class='" + (post.isPreviouslyRead() ? "read" : "unread") + "' id='" + post.getId() + "'>");
            buffer.append("    <td class='userinfo-row' style='width: 100%;"+(post.isOp()?"background-color:"+ColorPickerPreference.convertToARGB(aPrefs.postOPColor):"")+"'>");
            buffer.append("        <div class='avatar' "+((aPrefs.imagesEnabled != false && post.getAvatar() != null)?"style='height: 100px; width: 100px; background-image:url("+post.getAvatar()+");'":"")+">");
            buffer.append("        </div>");
            buffer.append("        <div class='userinfo'>");
            buffer.append("            <div class='username'>");
            buffer.append("                <h4>" + post.getUsername() + ((post.isMod())?"<img src='file:///android_res/drawable/blue_star.png' />":"")+ ((post.isAdmin())?"<img src='file:///android_res/drawable/red_star.png' />":"")  +  "</h4>");
            buffer.append("            </div>");
            buffer.append("            <div class='postdate'>");
            buffer.append("                " + post.getDate());
            buffer.append("            </div>");
            buffer.append("        </div>");
            buffer.append("        <div class='action-button " + (post.isEditable() ? "editable" : "noneditable") + "' id='" + post.getId() + "' lastreadurl='" + post.getLastReadUrl() + "' username='" + post.getUsername() + "'>");
            buffer.append("            <img src='file:///android_asset/post_action_icon.png' />");
            buffer.append("        </div>");
            buffer.append("    </td>");
            buffer.append("</tr>");
            buffer.append("<tr>");
            buffer.append("    <td class='post-cell' colspan='2' style='background: " + background + ";'>");
            buffer.append("        <div class='post-content' style='color: " + ColorPickerPreference.convertToARGB(aPrefs.postFontColor) + "; font-size: " + aPrefs.postFontSize + ";'>");
            buffer.append("            " + post.getContent());
            buffer.append("        </div>");
            buffer.append("    </td>");
            buffer.append("</tr>");
        }

        return buffer.toString();
    }

    public static String getPostsHtmlForTablet(ArrayList<AwfulPost> aPosts, AwfulPreferences aPrefs) {
        StringBuffer buffer = new StringBuffer();

        boolean light = true;
        String background = null;

        for (AwfulPost post : aPosts) {
        
            if (post.isPreviouslyRead()) {
                background = 
                    ColorPickerPreference.convertToARGB(light ? aPrefs.postReadBackgroundColor : aPrefs.postReadBackgroundColor2);
            } else {
                background = 
                    ColorPickerPreference.convertToARGB(light ? aPrefs.postBackgroundColor : aPrefs.postBackgroundColor2);
            }

            if(aPrefs.alternateBackground == true){
            	light = !light;
            }

            buffer.append("<tr class='" + (post.isPreviouslyRead() ? "read" : "unread") + "'>");
            buffer.append("    <td class='usercolumn' style='background: " + background + ";'>");
            buffer.append("        <div class='userinfo'>");
            buffer.append("            <div class='username' " + (post.isOp() ? "style='color: " + ColorPickerPreference.convertToARGB(aPrefs.postOPColor) + ";" : "style='color: " + ColorPickerPreference.convertToARGB(aPrefs.postFontColor) + ";") + "'>");
            buffer.append("                <h4>" + post.getUsername() + ((post.isMod())?"<img src='file:///android_res/drawable/blue_star.png' />":"")+ ((post.isAdmin())?"<img src='file:///android_res/drawable/red_star.png' />":"")  + "</h4>");
            buffer.append("            </div>");
            buffer.append("            <div class='postdate' " + (post.isOp() ? "style='color: " + ColorPickerPreference.convertToARGB(aPrefs.postOPColor) + ";" :  "style='color: " + ColorPickerPreference.convertToARGB(aPrefs.postFontColor) + ";") + "'>");
            buffer.append("                " + post.getDate());
            buffer.append("            </div>");
            buffer.append("        </div>");
            buffer.append("        <div class='avatar'>");

            if (aPrefs.imagesEnabled != false && post.getAvatar() != null) {
                buffer.append("            <img src='" + post.getAvatar() + "' />");
            }

            buffer.append("        </div>");
            buffer.append("    </td>");
            buffer.append("    <td class='post-cell' style='background: " + background + ";'>");
            buffer.append("        <div class='action-button " + (post.isEditable() ? "editable" : "noneditable") + "' id='" + post.getId() + "' lastreadurl='" + post.getLastReadUrl() + "' username='" + post.getUsername() + "'>");
            buffer.append("            <img src='file:///android_asset/post_action_icon.png' />");
            buffer.append("        </div>");
            buffer.append("        <div class='post-content' style='color: " + ColorPickerPreference.convertToARGB(aPrefs.postFontColor) + "; font-size: " + aPrefs.postFontSize + ";'>");
            buffer.append("            " + post.getContent());
            buffer.append("        </div>");
            buffer.append("    </td>");
            buffer.append("</tr>");
        }

        return buffer.toString();
    }

    public String getThreadId() {
        return mThreadId;
    }

    public void setThreadId(String aThreadId) {
        mThreadId = aThreadId;
        threadId = Integer.parseInt(aThreadId);
    }

    public String getAuthor() {
        return mAuthor;
    }

    public void setAuthor(String aAuthor) {
        mAuthor = aAuthor;
    }

    public String getAuthorID() {
        return mAuthorID;
    }

    public void setAuthorID(String aAuthorID) {
        mAuthorID = aAuthorID;
    }
    
    public String getKilledBy() {
		return mKilledBy;
	}

	public void setKilledBy(String mKilledBy) {
		this.mKilledBy = mKilledBy;
	}

	public String getIcon() {
        return mIcon;
    }

    public void setIcon(String aIcon) {
        mIcon = aIcon;
    }

    public boolean isClosed(){
    	return mClosed;
    }
    
    public void setClosed(boolean aClosed) {
        mClosed = aClosed;
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

    public int getForumId() {
		return forumId;
	}

	public void setForumId(int forumId) {
		this.forumId = forumId;
	}

	public ArrayList<AwfulPost> getPosts(int page) {
        return mPosts.get(page);
    }

    public void setPosts(ArrayList<AwfulPost> aPosts, int page) {
   		mPosts.put(page, aPosts);
    }

	@Override
	public View getView(LayoutInflater inf, View current, ViewGroup parent, AwfulPreferences prefs) {
		View tmp = current;
		if(tmp == null || tmp.getId() != R.layout.thread_item){
			tmp = inf.inflate(R.layout.thread_item, parent, false);
			tmp.setTag(this);
		}
		TextView info = (TextView) tmp.findViewById(R.id.threadinfo);
		ImageView sticky = (ImageView) tmp.findViewById(R.id.sticky_icon);
		ImageView bookmark = (ImageView) tmp.findViewById(R.id.bookmark_icon);
		if(mSticky){
			sticky.setImageResource(R.drawable.sticky);
			sticky.setVisibility(View.VISIBLE);
		}else{
			sticky.setVisibility(View.GONE);
		}
		if(mBookmarked && !(((ListView)parent).getId() == R.id.bookmark_list)){
			bookmark.setImageResource(R.drawable.blue_star);
			bookmark.setVisibility(View.VISIBLE);
			if(!mSticky){
				bookmark.setPadding(0, 5, 4, 0);
			}
		}else{
			if(!mSticky){
				bookmark.setVisibility(View.GONE);
			}else{
				bookmark.setVisibility(View.INVISIBLE);
			}
			
		}
		if(prefs.threadInfo.equals("threadpages")){
			info.setText((int)(Math.ceil(mTotalPosts/prefs.postPerPage)+1)+" pages");	
		}else if(prefs.threadInfo.equals("killedby")){
			info.setText("Killed By: "+mKilledBy);
		}else{
			info.setText("Author: "+mAuthor);
		}
		TextView unread = (TextView) tmp.findViewById(R.id.unread_count);
		if(mUnreadCount >=0){
			unread.setVisibility(View.VISIBLE);
			unread.setText(mUnreadCount+"");
            if (mUnreadCount == 0){
                unread.setBackgroundResource(R.drawable.unread_background_dim);
            }
		}else{
			unread.setVisibility(View.GONE);
		}
		TextView title = (TextView) tmp.findViewById(R.id.title);
		if(mTitle != null){
			title.setText(Html.fromHtml(mTitle));
		}
		if(prefs != null){
			title.setTextColor(prefs.postFontColor);
			info.setTextColor(prefs.postFontColor2);
			title.setSingleLine(!prefs.wrapThreadTitles);
			if(!prefs.wrapThreadTitles){
				title.setEllipsize(TruncateAt.END);
			}else{
				title.setEllipsize(null);
			}
		}
		return tmp;
	}

	@Override
	public int getID() {
		return threadId;
	}

	@Override
	public DISPLAY_TYPE getType() {
		return DISPLAY_TYPE.THREAD;
	}

	@Override
	public ArrayList<? extends AwfulDisplayItem> getChildren(int page) {
		return mPosts.get(page);
	}

    @Override
    public JSONArray getSerializedChildren(int aPage) {
        JSONArray result = new JSONArray();
        ArrayList<AwfulPost> posts = mPosts.get(aPage);

        try {
            for (AwfulPost post : posts) {
                result.put(post.toJSON().toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return result;
    }

	public void prunePages(int save){
		ArrayList<AwfulPost> tmp = mPosts.get(save);
		mPosts.clear();
		if(tmp != null){
			mPosts.put(save, tmp);
		}
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public int getChildrenCount(int page) {
		if(mPosts.get(page) == null){
			return 0;
		}
		return mPosts.get(page).size();
	}

	@Override
	public AwfulDisplayItem getChild(int page, int ix) {
		if(mPosts.get(page) == null){
			return null;
		}
		return mPosts.get(page).get(ix);
	}
	public int getLastReadPage(int postPerPage) {
		if(getUnreadCount()==-1){
			return 1;
		}
		if(mUnreadCount <= 0){
			return (mTotalPosts-mUnreadCount)/postPerPage+1;
		}
		return (mTotalPosts-mUnreadCount+1)/postPerPage+1;
	}
	public int getLastReadPost(int postPerPage) {
		if(getUnreadCount()==-1){
			return 0;
		}
		if(getUnreadCount()<=0){
			return postPerPage;
		}
		return (mTotalPosts-mUnreadCount+1)%postPerPage;
	}
	public void setTotalCount(int postTotal, int perPage) {
		mTotalPosts = postTotal;
		setLastPage(postTotal/perPage+1);
	}

	public int getTotalCount() {
		return mTotalPosts;
	}

	public boolean isPageCached(int page) {
		return mPosts.get(page) != null;
	}
}
