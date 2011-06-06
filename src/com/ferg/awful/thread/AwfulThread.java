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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ferg.awful.R;
import com.ferg.awful.constants.Constants;
import com.ferg.awful.network.NetworkUtils;

public class AwfulThread extends AwfulPagedItem implements AwfulDisplayItem {
    private static final String TAG = "AwfulThread";

    private String mThreadId;
    private int threadId;
    private String mAuthor;
    private boolean mSticky;
    private String mIcon;
    private int mUnreadCount;
	private int mTotalPosts;
    private int mPTI;
	private boolean mBookmarked;
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
	
    public static TagNode getUserCPThreads() throws Exception {
        return NetworkUtils.get(Constants.FUNCTION_USERCP, null, null);
	}

	public static ArrayList<AwfulThread> parseForumThreads(TagNode aResponse) throws Exception {
        ArrayList<AwfulThread> result = new ArrayList<AwfulThread>();
        TagNode[] threads = aResponse.getElementsByAttValue("id", "forum", true, true);
        if(threads.length >1){
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
                    thread.setTotalCount(Integer.parseInt(tarPostCount[0].getText().toString().trim()));
                }
            	TagNode[] tarUser = node.getElementsByAttValue("class", "author", true, true);
                if (tarThread.length > 0) {
                    thread.setTitle(tarThread[0].getText().toString().trim());
                }

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

    public void getThreadPosts() throws Exception {
        getThreadPosts(-1);
    }

    public void getThreadPosts(int aPage) throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(Constants.PARAM_THREAD_ID, mThreadId);

        if (aPage == -1) {
            params.put(Constants.PARAM_GOTO, "newpost");
        } else {
            params.put(Constants.PARAM_PAGE, Integer.toString(aPage));
        }

        List<URI> redirects = new LinkedList<URI>();
        TagNode response = NetworkUtils.get(
                Constants.FUNCTION_THREAD, params, redirects);

        mPTI = -1;
        if (redirects.size() > 1) {
            String fragment = redirects.get(redirects.size() - 1).getFragment();
            if (fragment.startsWith(Constants.FRAGMENT_PTI)) {
                mPTI = Integer.parseInt(
                        fragment.substring(Constants.FRAGMENT_PTI.length()));
            }
        }
        if (mTitle == null) {
        	TagNode[] tarTitle = response.getElementsByAttValue("class", "bclast", true, true);

            if (tarTitle.length > 0) {
                mTitle = tarTitle[0].getText().toString().trim();
            }
        }

        setPosts(AwfulPost.parsePosts(response, mPTI), aPage);
        parsePageNumbers(response);
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

    public String getIcon() {
        return mIcon;
    }

    public void setIcon(String aIcon) {
        mIcon = aIcon;
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

    public ArrayList<AwfulPost> getPosts(int page) {
        return mPosts.get(page);
    }

    public void setPosts(ArrayList<AwfulPost> aPosts, int page) {
   		mPosts.put(page, aPosts);
    }

	@Override
	public View getView(LayoutInflater inf, View current, ViewGroup parent) {
		View tmp = current;
		if(tmp == null || tmp.getId() != R.layout.thread_item){
			tmp = inf.inflate(R.layout.thread_item, parent, false);
			tmp.setTag(this);
		}
		TextView author = (TextView) tmp.findViewById(R.id.author);
		ImageView sticky = (ImageView) tmp.findViewById(R.id.sticky_icon);
		if(mSticky){
			sticky.setImageResource(R.drawable.sticky);
			sticky.setVisibility(View.VISIBLE);
		}
		if(mBookmarked){
			sticky.setImageResource(R.drawable.star_blue);
			sticky.setVisibility(View.VISIBLE);
		}
		if(!mSticky && !mBookmarked){
			sticky.setVisibility(View.GONE);
		}
		author.setText(mAuthor);
		TextView unread = (TextView) tmp.findViewById(R.id.unread_count);
		if(mUnreadCount >=0){
			unread.setVisibility(View.VISIBLE);
			unread.setText(mUnreadCount+"");
		}else{
			unread.setVisibility(View.GONE);
		}
		TextView title = (TextView) tmp.findViewById(R.id.title);
		title.setText(mTitle);
		
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
		return mPosts.get(mPosts.size()-1);
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
	public int getLastReadPage() {
		if(getUnreadCount()==-1){
			return 1;
		}
		return (mTotalPosts-mUnreadCount+1)/Constants.ITEMS_PER_PAGE+1;
	}
	public int getLastReadPost() {
		if(getUnreadCount()==-1){
			return 0;
		}
		return (mTotalPosts-mUnreadCount+1)%Constants.ITEMS_PER_PAGE;
	}
	public void setTotalCount(int postTotal) {
		mTotalPosts = postTotal;
	}

	public int getTotalCount() {
		return mTotalPosts;
	}
}
