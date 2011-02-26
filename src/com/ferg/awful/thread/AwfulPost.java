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

import android.util.Log;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

public class AwfulPost {
    private static final String TAG = "AwfulPost";

    private static final String USERNAME_SEARCH = "//dt[@class='author']|//dt[@class='author op']|//dt[@class='author role-mod']|//dt[@class='author role-admin']|//dt[@class='author role-mod op']|//dt[@class='author role-admin op']";
    private static final String MOD_SEARCH      = "//dt[@class='author role-mod']|//dt[@class='author role-mod op']";
    private static final String ADMIN_SEARCH    = "//dt[@class='author role-admin']|//dt[@class='author role-admin op']";

    private static final String USERNAME  = "//dt[@class='author']";
    private static final String OP        = "//dt[@class='author op']";
	private static final String MOD       = "//dt[@class='author role-mod']";
	private static final String ADMIN     = "//dt[@class='author role-admin']";
    private static final String POST      = "//table[@class='post']";
    private static final String POST_ID   = "//table[@class='post']";
    private static final String POST_DATE = "//td[@class='postdate']";
    private static final String SEEN_LINK = "//td[@class='postdate']//a[@title='Mark thread seen up to this post']";
    private static final String SEEN      = "//tr[@class='seen1']|//tr[@class='seen2']";
    private static final String AVATAR    = "//dd[@class='title']//img";
    private static final String EDITED    = "//p[@class='editedby']/span";
    private static final String POSTBODY  = "//td[@class='postbody']";
	private static final String SEEN1     = "//tr[@class='seen1']";
	private static final String SEEN2     = "//tr[@class='seen2']";

    private static final String ELEMENT_POSTBODY     = "<td class=\"postbody\">";
    private static final String ELEMENT_END_TD       = "</td>";
    private static final String REPLACEMENT_POSTBODY = "<div class=\"postbody\">";
    private static final String REPLACEMENT_END_TD   = "</div>";

    private String mId;
    private String mDate;
    private String mUsername;
    private String mAvatar;
    private String mContent;
    private String mEdited;
	private boolean mLastRead = false;;

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

	public boolean isLastRead() {
		return mLastRead;
	}

	public void setLastRead(boolean aLastRead) {
		mLastRead = aLastRead;
	}

    public static ArrayList<AwfulPost> parsePosts(TagNode aThread) {
        ArrayList<AwfulPost> result = new ArrayList<AwfulPost>();
        HtmlCleaner cleaner = new HtmlCleaner();
        CleanerProperties properties = cleaner.getProperties();
        properties.setOmitComments(true);

		boolean lastReadFound = false;

        try {
            Object[] postNodes = aThread.evaluateXPath(POST);

            for (Object current : postNodes) {
				boolean previouslyRead = false;

                AwfulPost post = new AwfulPost();
                TagNode node = (TagNode) current;

                // We'll just reuse the array of objects rather than create 
                // a ton of them
                String id = node.getAttributeByName("id");
                post.setId(id.replaceAll("post", ""));

                Object[] nodeList = node.evaluateXPath(POST_DATE);
                if (nodeList.length > 0) {
                    TagNode dateNode = (TagNode) nodeList[0];

                    // There's got to be a better way to do this
                    dateNode.removeChild(dateNode.findElementHavingAttribute("href", false));
                    dateNode.removeChild(dateNode.findElementHavingAttribute("href", false));
                    dateNode.removeChild(dateNode.findElementHavingAttribute("href", false));

                    post.setDate(dateNode.getText().toString().trim());
                }

				// Assume it's a post by a normal user first
                nodeList = node.evaluateXPath(USERNAME);
                if (nodeList.length > 0) {
                    post.setUsername(((TagNode) nodeList[0]).getText().toString());
                }

				// If we didn't get a username, try for an OP
				if (post.getUsername() == null) {
					nodeList = node.evaluateXPath(OP);
					if (nodeList.length > 0) {
						post.setUsername(((TagNode) nodeList[0]).getText().toString());
					}
				}

				// Not an OP? Maybe it's a mod
				if (post.getUsername() == null) {
					nodeList = node.evaluateXPath(MOD);
					if (nodeList.length > 0) {
						post.setUsername(((TagNode) nodeList[0]).getText().toString());
					}
				}

				// If it's not a mod, it's probably an admin
				if (post.getUsername() == null) {
					nodeList = node.evaluateXPath(ADMIN);
					if (nodeList.length > 0) {
						post.setUsername(((TagNode) nodeList[0]).getText().toString());
					}
				}

				// If we haven't yet found the last read post then check if
				// this post has "last read" color highlighting. If it doesn't,
				// it's the first unread post for the page.
				if (!lastReadFound) {
					nodeList = node.evaluateXPath(SEEN1);
					if (nodeList.length > 0) {
						previouslyRead = true;
					} else {
						nodeList = node.evaluateXPath(SEEN2);
						if (nodeList.length > 0) {
							previouslyRead = true;
						}
					}

					if (!previouslyRead) {
						post.setLastRead(true);
						lastReadFound = true;
					}
				}

                nodeList = node.evaluateXPath(AVATAR);
                if (nodeList.length > 0) {
                    post.setAvatar(((TagNode) nodeList[0]).getAttributeByName("src"));
                }

                nodeList = node.evaluateXPath(EDITED);
                if (nodeList.length > 0) {
                    post.setEdited(((TagNode) nodeList[0]).getText().toString());
                }

                nodeList = node.evaluateXPath(POSTBODY);
                if (nodeList.length > 0) {
                    SimpleHtmlSerializer serializer = 
                        new SimpleHtmlSerializer(cleaner.getProperties());

                    post.setContent(serializer.getAsString((TagNode) nodeList[0]));
                }

                result.add(post);
            }

            Log.i(TAG, Integer.toString(postNodes.length));
        } catch (XPatherException e) {
            e.printStackTrace();
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
