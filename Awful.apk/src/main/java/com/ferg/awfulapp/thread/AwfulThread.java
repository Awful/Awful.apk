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

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.ferg.awfulapp.AwfulFragment;
import com.ferg.awfulapp.ForumDisplayFragment;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.AwfulProvider;
import com.ferg.awfulapp.provider.AwfulTheme;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.provider.DatabaseHelper;
import com.ferg.awfulapp.util.AwfulUtils;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;
import com.samskivert.mustache.Template;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static butterknife.ButterKnife.findById;
import static java.lang.Integer.parseInt;

public class AwfulThread extends AwfulPagedItem  {
    private static final String TAG = "AwfulThread";

    public static final String PATH         = "/thread";
    public static final String UCP_PATH     = "/ucpthread";
    public static final Uri CONTENT_URI     = Uri.parse("content://" + Constants.AUTHORITY + PATH);
	public static final Uri CONTENT_URI_UCP = Uri.parse("content://" + Constants.AUTHORITY + UCP_PATH);

    public static final String ID 		            = "_id";
    public static final String INDEX 		        = "thread_index";
    public static final String FORUM_ID 	        = "forum_id";
    public static final String TITLE 		        = "title";
    public static final String POSTCOUNT 	        = "post_count";
    public static final String UNREADCOUNT          = "unread_count";
    public static final String AUTHOR 		        = "author";
    public static final String AUTHOR_ID 	        = "author_id";
    public static final String LOCKED               = "locked";
    public static final String CAN_OPEN_CLOSE       = "can_open_close";
	public static final String BOOKMARKED           = "bookmarked";
	public static final String STICKY               = "sticky";
	public static final String CATEGORY             = "category";
	public static final String LASTPOSTER           = "killedby";
	public static final String FORUM_TITLE          = "forum_title";
	public static final String HAS_NEW_POSTS        = "has_new_posts";
    public static final String HAS_VIEWED_THREAD    = "has_viewed_thread";
    public static final String ARCHIVED             = "archived";
	public static final String RATING               = "rating";
    public static final String TAG_URL 		        = "tag_url";
    public static final String TAG_CACHEFILE 	    = "tag_cachefile";
    public static final String TAG_EXTRA            = "tag_extra";

    /**
     * All the scripts from the javascript folder used in generating HTML
     */
    static final String[] JS_FILES = {
            "zepto/zepto.min.js",
            "zepto/selector.js",
            "zepto/fx.js",
            "zepto/fx_methods.js",
            "zepto/touch.js",
            "scrollend.js",
            "inviewport.js",
            "json2.js",
            "twitterwidget.js",
            "salr.js",
            "thread.js"
    };

    private static final Pattern forumId_regex = Pattern.compile("forumid=(\\d+)");
	private static final Pattern urlId_regex = Pattern.compile("([^#]+)#(\\d+)$");


    // TODO: 04/06/2017 explicit default values, nulls where parsed data doesn't set values (i.e. never added to the ContentValues)?
    int id;
    int index;
    String title;

    int forumId;
//    String forumTitle;

    String author;
    int authorId;
    String lastPoster;
    int postCount;
    int unreadCount;

    int rating;
    int bookmarkType;

    boolean isLocked;
    boolean isSticky;
    boolean canOpenClose;
    boolean hasBeenViewed;
    boolean archived;

    String tagUrl;
    String tagCacheFile;
    int tagExtra;
    int category;


    @Nullable
    public static AwfulThread fromCursorRow(@NonNull Cursor row) {
        if (row.isBeforeFirst() || row.isAfterLast()) {
            Log.w(TAG, "fromCursor: passed empty row");
            return null;
        }
        AwfulThread thread = new AwfulThread();

        thread.id = row.getInt(row.getColumnIndex(ID));
        thread.index = row.getInt(row.getColumnIndex(INDEX));
        thread.title = row.getString(row.getColumnIndex(TITLE));

        thread.forumId = row.getInt(row.getColumnIndex(FORUM_ID));
        // TODO: 03/06/2017 this column name is taken from the thread projection, but is it ever used?
//        thread.forumTitle = row.getString(row.getColumnIndex(FORUM_TITLE));

        thread.author = row.getString(row.getColumnIndex(AUTHOR));
        thread.authorId = row.getInt(row.getColumnIndex(AUTHOR_ID));
        thread.lastPoster = row.getString(row.getColumnIndex(LASTPOSTER));
        thread.postCount = row.getInt(row.getColumnIndex(POSTCOUNT));
        thread.unreadCount = row.getInt(row.getColumnIndex(UNREADCOUNT));

        thread.rating = row.getInt(row.getColumnIndex(RATING));
        thread.bookmarkType = row.getInt(row.getColumnIndex(BOOKMARKED));

        thread.isLocked = row.getInt(row.getColumnIndex(LOCKED)) > 0;
        thread.archived = row.getInt(row.getColumnIndex(ARCHIVED)) > 0;
        thread.isSticky = row.getInt(row.getColumnIndex(STICKY)) > 0;
        thread.canOpenClose = row.getInt(row.getColumnIndex(CAN_OPEN_CLOSE)) > 0;
        thread.hasBeenViewed = row.getInt(row.getColumnIndex(HAS_VIEWED_THREAD)) == 1;

        thread.tagUrl = row.getString(row.getColumnIndex(TAG_URL));
        thread.tagCacheFile = row.getString(row.getColumnIndex(TAG_CACHEFILE));
        thread.tagExtra = row.getInt(row.getColumnIndex(TAG_EXTRA));
        thread.category = row.getInt(row.getColumnIndex(CATEGORY));

        return thread;
    }


    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(ID, id);
        cv.put(INDEX, index);
        cv.put(FORUM_ID, forumId);
        cv.put(TITLE, title);
        cv.put(AUTHOR, author);
        cv.put(AUTHOR_ID, authorId);


        cv.put(CAN_OPEN_CLOSE, asSqlBoolean(canOpenClose));

        cv.put(LASTPOSTER, lastPoster);
        cv.put(LOCKED, asSqlBoolean(isLocked));
        cv.put(STICKY, asSqlBoolean(isSticky));
        cv.put(RATING, rating);
        cv.put(TAG_URL, tagUrl);
        cv.put(CATEGORY, category);
        cv.put(TAG_CACHEFILE, tagCacheFile);

        cv.put(TAG_EXTRA, tagExtra);
        cv.put(POSTCOUNT, postCount);

        cv.put(UNREADCOUNT, unreadCount);
        cv.put(HAS_VIEWED_THREAD, asSqlBoolean(hasBeenViewed));
        cv.put(BOOKMARKED, bookmarkType);
        return cv;
    }

    private static int asSqlBoolean(boolean value) {
        return value ? 1 : 0;
    }

    public boolean hasNewPosts() {
        return unreadCount > 0;
    }

    public int getPageCount(int postsPerPage) {
        return AwfulPagedItem.indexToPage(postCount, postsPerPage);
    }


    /**
     * Parse a list of threads in a forum and generate their metadata.
     * <p>
     * This doesn't write to the database, you need to do this with the returned data.
     *
     * @param forumPage  the page to parse
     * @param forumId    the ID of the forum this page is from
     * @param startIndex the threads' positions in the forum will start from this index
     * @return the list of all the threads' metadata objects, ready for storage
     */
    public static List<ContentValues> parseForumThreads(Document forumPage, int forumId, int startIndex) {
        String update_time = new Timestamp(System.currentTimeMillis()).toString();
        List<ContentValues> result = new ArrayList<>();
        Log.v(TAG, "Update time: " + update_time);
        String username = AwfulPreferences.getInstance().username;

        for (Element threadElement : forumPage.select("#forum .thread")) {
            try {
                String threadId = threadElement.id();
                if (TextUtils.isEmpty(threadId)) {
                    //skip the table header
                    continue;
                }

                // start building thread data
                AwfulThread thread = new AwfulThread();
                thread.id = parseInt(threadId.replaceAll("\\D", ""));
                thread.index = startIndex;
                thread.forumId = forumId;
                startIndex++;


                // parse out the various elements in the thread html:

                // title
                Element title = threadElement.select(".thread_title").first();
                if (title != null) {
                    thread.title = title.text();
                }

                // thread author, and whether it's the user
                boolean userIsAuthor = false;
                Element author = threadElement.select(".author").first();
                if (author != null) {
                    thread.author = author.text();
                    String href = author.select("a[href*='userid']").first().attr("href");
                    thread.authorId = parseInt(Uri.parse(href).getQueryParameter("userid"));

                    userIsAuthor = author.text().equals(username);
                }
                thread.canOpenClose = userIsAuthor;

                thread.lastPoster = threadElement.select(".lastpost .author").first().text();
                thread.isLocked = threadElement.hasClass("closed");
                thread.isSticky = threadElement.select(".title_sticky").isEmpty();


                // optional thread rating
                Element rating = threadElement.select(".rating img").first();
                thread.rating = (rating != null) ? AwfulRatings.getId(rating.attr("src")) : AwfulRatings.NO_RATING;


                // main thread tag
                Element threadTag = threadElement.select(".icon img").first();
                if (threadTag != null) {
                    Matcher threadTagMatcher = urlId_regex.matcher(threadTag.attr("src"));
                    if (threadTagMatcher.find()) {
                        thread.tagUrl = threadTagMatcher.group(1);
                        thread.category = parseInt(threadTagMatcher.group(2));
                        //thread tag stuff
                        Matcher fileNameMatcher = AwfulEmote.fileName_regex.matcher(threadTagMatcher.group(1));
                        if (fileNameMatcher.find()) {
                            thread.tagCacheFile = fileNameMatcher.group(1);
                        }
                    } else {
                        thread.category = 0;
                    }
                }

                // secondary thread tag (e.g. Ask/Tell type)
                Element extraTag = threadElement.select(".icon2 img").first();
                thread.tagExtra = (extraTag != null) ? ExtraTags.getId(extraTag.attr("src")) : ExtraTags.NO_TAG;


                // replies / postcount
                Element postCount = threadElement.select(".replies").first();
                if (postCount != null) {
                    // this represents the number of replies, but the actual postcount includes OP
                    thread.postCount = Integer.parseInt(postCount.text()) + 1;
                }


                // unread count / viewed status
                Element unreadCount = threadElement.select(".count").first();
                if (unreadCount != null) {
                    thread.unreadCount = parseInt(unreadCount.text());
                    thread.hasBeenViewed = true;
                } else {
                    thread.unreadCount = 0;
                    // If there are X's then the user has viewed the thread
                    thread.hasBeenViewed = !threadElement.select(".x").isEmpty();
                }

                // bookmarked status
                Element star = threadElement.select(".star").first();
                int bookmarkType = 0;
                if (star != null) {
                    // Bookmarks can only be detected now by the presence of a "bmX" class - no star image
                    if (star.hasClass("bm0")) {
                        bookmarkType = 1;
                    } else if (star.hasClass("bm1")) {
                        bookmarkType = 2;
                    } else if (star.hasClass("bm2")) {
                        bookmarkType = 3;
                    }
                }
                thread.bookmarkType = bookmarkType;


                // finally create and add the parsed thread
                // TODO: 04/06/2017 handle this in the database classes
                ContentValues cv = thread.toContentValues();
                cv.put(DatabaseHelper.UPDATED_TIMESTAMP, update_time);
                // don't update these values if we are loading bookmarks, or it will overwrite the cached forum results.
                if (forumId == Constants.USERCP_ID) {
                    cv.remove(INDEX);
                    cv.remove(FORUM_ID);
                }
                result.add(cv);
            } catch (NullPointerException e) {
                // If we can't parse a row, just skip it
                e.printStackTrace();
            }
        }
        return result;
    }


    /**
     * Parse a page from a thread, updating metadata and parsing the contained posts.
     * <p>
     * This will update the current read/unread counts, estimating the total number of posts
     * if the last recorded total is too low (by the current number of pages) and this isn't the last page
     * (meaning we only know how many full pages there are, not how many posts are on the last page).
     * Defaults to a minimum estimate, i.e. a single post on the last page.
     * <p>
     * Also stores/updates the rest of the thread metadata - title, locked status etc., and passes
     * the page to {@link AwfulPost} for parsing and syncing.
     *
     * @param resolver     a ContentResolver used to access the database
     * @param page         the thread page's HTML document
     * @param threadId     the ID of this thread
     * @param pageNumber   which page of the thread this document represents
     * @param postsPerPage used to calculate post counts
     * @param prefs        a preferences instance
     * @param filterUserId if this page is for a thread filtered by user, this should be set to the user's ID, otherwise 0
     */
    public static void parseThreadPage(ContentResolver resolver, Document page, int threadId, int pageNumber, int postsPerPage, AwfulPreferences prefs, int filterUserId) {
        // TODO: 03/06/2017 see issue #503 on GitHub - filtering by user means the thread data gets overwritten by the pages from this new, shorter thread containing their posts
        final int BLANK_USER_ID = 0;
        final boolean filteringOnUserId = filterUserId > BLANK_USER_ID;

        // first parse general thread metadata

        Cursor threadData = resolver.query(ContentUris.withAppendedId(CONTENT_URI, threadId), AwfulProvider.ThreadProjection, null, null, null);
        AwfulThread thread = null;
        if (threadData != null && threadData.moveToFirst()) {
            thread = fromCursorRow(threadData);
            threadData.close();
        }
        if (thread == null) {
            thread = new AwfulThread();
        }

        thread.id = threadId;

        Element title = page.getElementsByClass("bclast").first();
        thread.title = (title == null) ? "UNKNOWN TITLE" : title.text();

        // look for a real reply button - if there isn't one, this thread is locked
        Element replyButton = page.select("[alt=Reply]:not([src*='forum-closed'])").first();
        thread.isLocked = (replyButton == null);

        Element openCloseButton = page.select("[alt='Close thread']").first();
        thread.canOpenClose = (openCloseButton != null);

        Element bookmarkButton = page.select(".thread_bookmark").first();
        // we're assuming no bookmark button means archived - there's an explicit archived icon we could find too
        thread.archived = (bookmarkButton == null);
        boolean bookmarked = (bookmarkButton != null) && bookmarkButton.attr("src").contains("unbookmark");

        if (!bookmarked) {
            thread.bookmarkType = 0;
        } else if (thread.bookmarkType == 0) {
            // update bookmarked status when it was previously 'unbookmarked'
            thread.bookmarkType = 1;
        }

        // ID of this thread's forum
        int forumId = -1;
        Matcher matchForumId;
        for (Element breadcrumb : page.select(".breadcrumbs [href]")) {
            matchForumId = forumId_regex.matcher(breadcrumb.attr("href"));
            if (matchForumId.find()) {//switched this to a regex
                forumId = parseInt(matchForumId.group(1));//so this won't fail
            }
        }
        thread.forumId = forumId;


        // now calculate some read/unread numbers based on what we can see on the page
        int lastPageNumber = AwfulPagedItem.parseLastPage(page);
        int firstPostOnPageIndex = AwfulPagedItem.pageToIndex(pageNumber, postsPerPage, 0);
        int firstUnreadIndex = !thread.hasBeenViewed ? 0 : thread.postCount - thread.unreadCount;

        // hand off the page for post parsing, and get back the number of posts it found
        // TODO: 02/06/2017 sort out the ignored posts issue, the post parser doesn't put them in the DB (if you have 'always hide' on in the settings) and it messes up the numbers
        int postsOnThisPage = AwfulPost.syncPosts(resolver, page, threadId, firstUnreadIndex, thread.authorId, prefs, firstPostOnPageIndex);
        int postsOnPreviousPages = (pageNumber - 1) * postsPerPage;
        int postsRead = postsOnPreviousPages + postsOnThisPage;

        // we might have more recent info here, see if we need to update lastPostCount
        // first up, if this is the last page then we've read all the posts
        if (pageNumber == lastPageNumber) {
            thread.postCount = postsRead;
        } else {
            // we can calculate a minimum and maximum posts range by looking at the last page number
            int minTotal = ((lastPageNumber - 1) * postsPerPage) + 1;   // one post on the last page, any preceding pages are full
            int maxTotal = lastPageNumber * postsPerPage;               // all pages full
            // if lastPostCount is within this range, let's just assume it's more accurate than taking the minimum
            // if it's outside of that range it's obviously a stale value, use the min as our best guess
            int lastPostCount = thread.postCount;
            thread.postCount = (minTotal <= lastPostCount && lastPostCount <= maxTotal) ? lastPostCount : minTotal;
        }
        thread.unreadCount = thread.postCount - postsRead;

        Log.d(TAG, String.format("getThreadPosts: Thread ID %d, page %d of %d, %d posts on page%n%d posts total: %d read/%d unread",
                thread.id, pageNumber, lastPageNumber, postsOnThisPage, thread.postCount, postsRead, thread.unreadCount));

        // finally write new thread data to the database
        ContentValues cv = thread.toContentValues();
        // TODO: 04/06/2017 this should be handled in the database-management classes
        String update_time = new Timestamp(System.currentTimeMillis()).toString();
        cv.put(DatabaseHelper.UPDATED_TIMESTAMP, update_time);
        if (resolver.update(ContentUris.withAppendedId(CONTENT_URI, threadId), cv, null, null) < 1) {
            resolver.insert(CONTENT_URI, cv);
        }
    }


    public static String getContainerHtml(AwfulPreferences aPrefs, int forumId){
        StringBuilder buffer = new StringBuilder("<!DOCTYPE html>\n<html>\n<head>\n");
        buffer.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0 maximum-scale=1.0 minimum-scale=1.0, user-scalable=no\" />\n");
        buffer.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n");
        buffer.append("<meta name='format-detection' content='telephone=no' />\n");
        buffer.append("<meta name='format-detection' content='address=no' />\n");


        // build the theme css tag, using the appropriate css path
        // the dark-theme attribute can be used to e.g. embed a dark or light widget
        AwfulTheme theme = AwfulTheme.forForum(forumId);
        buffer.append(String.format("<link id='theme-css' rel='stylesheet' data-dark-theme='%b' href='%s'>\n", theme.isDark(), theme.getCssPath()));
        buffer.append("<link rel='stylesheet' href='file:///android_asset/css/general.css' />");


        if(!aPrefs.preferredFont.contains("default")){
            buffer.append("<style id='font-face' type='text/css'>@font-face { font-family: userselected; src: url('content://com.ferg.awfulapp.webprovider/").append(aPrefs.preferredFont).append("'); }</style>\n");
        }
        for (String scriptName : JS_FILES) {
            buffer.append("<script src='file:///android_asset/javascript/")
                    .append(scriptName)
                    .append("' type='text/javascript'></script>\n");
        }

        buffer.append("</head><body><div id='container' class='container' ")
                .append((!aPrefs.noFAB ? "style='padding-bottom:75px'" : ""))
                .append("></div></body></html>");
        return buffer.toString();
    }


    public static String getHtml(List<AwfulPost> aPosts, AwfulPreferences aPrefs, int page, int lastPage, int forumId, boolean threadLocked) {
        StringBuilder buffer = new StringBuilder(1024);
        buffer.append("<div class='content'>\n");

        if(aPrefs.hideOldPosts && aPosts.size() > 0 && !aPosts.get(aPosts.size()-1).isPreviouslyRead()){
            int unreadCount = 0;
            for(AwfulPost ap : aPosts){
                if(!ap.isPreviouslyRead()){
                    unreadCount++;
                }
            }
            if(unreadCount < aPosts.size() && unreadCount > 0){
                buffer.append("    <article class='toggleread'>");
                buffer.append("      <a>\n");
                final int prevPosts = aPosts.size() - unreadCount;
                buffer.append("        <h3>Show ")
                        .append(prevPosts).append(" Previous Post").append(prevPosts > 1 ? "s" : "").append("</h3>\n");
                buffer.append("      </a>\n");
                buffer.append("    </article>");
            }
        }

        buffer.append(AwfulThread.getPostsHtml(aPosts, aPrefs, threadLocked));

        if(page == lastPage){
            buffer.append("<div class='unread' ></div>\n");
        }
        buffer.append("</div>\n");

        return buffer.toString();
    }


    public static String getPostsHtml(List<AwfulPost> aPosts, AwfulPreferences aPrefs, boolean threadLocked) {
        StringBuilder buffer = new StringBuilder();
        Template postTemplate;

        try {
            Reader templateReader = null;
            if (!"default".equals(aPrefs.layout)) {
                if (AwfulUtils.isMarshmallow()) {
                    int permissionCheck = ContextCompat.checkSelfPermission(aPrefs.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);

                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        File template = new File(Environment.getExternalStorageDirectory() + "/awful/" + aPrefs.layout);
                        if (template.isFile() && template.canRead()) {
                            templateReader = new FileReader(template);
                        }
                    }else{
                        Toast.makeText(aPrefs.getContext(), "Can't access custom layout because Awful lacks storage permissions. Reverting to default layout.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    File template = new File(Environment.getExternalStorageDirectory() + "/awful/" + aPrefs.layout);
                    if (template.isFile() && template.canRead()) {
                        templateReader = new FileReader(template);
                    }

                }
            }
            if (templateReader == null) {
                templateReader = new InputStreamReader(aPrefs.getResources().getAssets().open("mustache/post.mustache"));
            }
            postTemplate = Mustache.compiler().compile(templateReader);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

        for (AwfulPost post : aPosts) {
        	Map<String, String> postData = new HashMap<>();
            String username = post.getUsername();
            String avatar   = post.getAvatar();

        	postData.put("seen", (post.isPreviouslyRead() ? "read" : "unread"));
        	postData.put("isOP", (aPrefs.highlightOP && post.isOp()) ? "op" : null);
            postData.put("isMarked", (aPrefs.markedUsers.contains(username)) ? "marked" : null);
        	postData.put("postID", post.getId());
        	postData.put("isSelf", (aPrefs.highlightSelf && username.equals(aPrefs.username)) ? "self" : null);
            postData.put("avatarURL", (aPrefs.canLoadAvatars() && avatar != null && avatar.length() > 0 ) ? avatar : null);
        	postData.put("username", username);
        	postData.put("userID", post.getUserId());
        	postData.put("postDate", post.getDate());
        	postData.put("regDate", post.getRegDate());
        	postData.put("mod", (post.isMod()) ? "mod" : null);
            postData.put("admin", (post.isAdmin()) ? "admin" : null);
            postData.put("plat", (post.isPlat()) ? "plat" : null);
        	postData.put("avatarText", ""+post.getAvatarText());
        	postData.put("lastReadUrl", post.getLastReadUrl());
        	postData.put("notOnProbation",(aPrefs.isOnProbation()) ? null : "notOnProbation");
        	postData.put("editable",(post.isEditable()) ? "editable" : null);
        	postData.put("postcontent", post.getContent());

        	try{
        		buffer.append(postTemplate.execute(postData));
        	}catch(MustacheException e){
        		e.printStackTrace();
        	}
        }
        return buffer.toString();
    }


	@SuppressWarnings("deprecation")
	public static void setDataOnThreadListItem(View item, AwfulPreferences prefs, Cursor data, AwfulFragment parent) {
        AwfulThread thread = fromCursorRow(data);
        if (thread == null) {
            Log.w(TAG, "setDataOnThreadView: unable to get data for thread!");
            return;
        }

        Resources resources = item.getResources();
        Context context = item.getContext();
        // get the forum ID for getting themed resources
        Integer forumId = null;
        if (ForumDisplayFragment.class.isInstance(parent)) {
            forumId = ((ForumDisplayFragment) parent).getForumId();
        }


        // thread title
        TextView title  = findById(item, R.id.title);
        title.setText(thread.title != null ? thread.title : "UNKNOWN");
        title.setTextColor(ColorProvider.PRIMARY_TEXT.getColor(forumId));


        // main thread tag
        final ImageView threadTag = findById(item, R.id.thread_tag);
        threadTag.setVisibility(GONE);
        if (prefs.threadInfo_Tag) {
			if (!TextUtils.isEmpty(thread.tagCacheFile)) {
                threadTag.setVisibility(VISIBLE);
                String url = thread.tagUrl;
                String localFileName = "@drawable/"+url.substring(url.lastIndexOf('/') + 1,url.lastIndexOf('.')).replace('-','_').toLowerCase();

                int imageID = resources.getIdentifier(localFileName, null, context.getPackageName());
                if (imageID == 0) {
                    NetworkUtils.getImageLoader().get(url, new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                            threadTag.setImageBitmap(response.getBitmap());
                        }

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            threadTag.setImageResource(R.drawable.empty_thread_tag);
                        }
                    });
                } else {
                    threadTag.setImageResource(imageID);
                }
            }
		}


        // tag overlay (secondary tags etc)
        ImageView forumTagOverlay = findById(item, R.id.thread_tag_overlay);
        ImageView inlineForumTagOverlay = findById(item, R.id.thread_tag_overlay_optional);
        forumTagOverlay.setVisibility(GONE);
        inlineForumTagOverlay.setVisibility(GONE);
        if (ExtraTags.getType(thread.tagExtra) != ExtraTags.TYPE_NO_TAG) {
            Drawable tagIcon = ExtraTags.getDrawable(thread.tagExtra, resources);
            if (tagIcon != null) {
                if(prefs.threadInfo_Tag) {
                    showImage(forumTagOverlay, tagIcon);
                }else {
                    showImage(inlineForumTagOverlay, tagIcon);
                }
            }
        }


        // page count / author / last poster info line
        TextView info   = findById(item, R.id.thread_info);
        info.setVisibility(VISIBLE);
        String tmp = String.format(Locale.US, "%d pgs | %s: %s",
                thread.getPageCount(prefs.postPerPage),
                thread.hasBeenViewed ? "Last" : "OP",
                NetworkUtils.unencodeHtml(thread.hasBeenViewed ? thread.lastPoster : thread.author));
        info.setText(tmp.trim());
        info.setTextColor(ColorProvider.ALT_TEXT.getColor(forumId));


        // ratings
        ImageView threadRating = findById(item, R.id.thread_rating);
        ImageView inlineThreadRating = findById(item, R.id.thread_rating_optional);
        threadRating.setVisibility(GONE);
        inlineThreadRating.setVisibility(GONE);
        // if we're showing ratings...
        if (prefs.threadInfo_Rating) {
            Drawable ratingIcon = AwfulRatings.getDrawable(thread.rating, resources);
                // Film Dump replaces the actual thread tag, instead of using the separate rating view
                if (AwfulRatings.getType(thread.rating) == AwfulRatings.TYPE_FILM_DUMP) {
                    showImage(threadTag, ratingIcon);
                } else {
                    showImage(prefs.threadInfo_Tag ? threadRating : inlineThreadRating, ratingIcon);
                }
        }


        // locked and sticky status
        ImageView threadLocked = findById(item, R.id.thread_locked);
        ImageView threadSticky = findById(item, R.id.thread_sticky);
        threadSticky.setVisibility(thread.isSticky ? VISIBLE : GONE);
        threadLocked.setVisibility(thread.isLocked && !thread.isSticky ? VISIBLE : GONE);
        if (thread.isLocked && !thread.isSticky) {
            // TODO: 03/06/2017 what's this about?
            item.setBackgroundColor(ColorProvider.BACKGROUND.getColor(forumId));
        }


        // unread counter
        TextView unread = findById(item, R.id.unread_count);
        unread.setVisibility(GONE);
        if(thread.hasBeenViewed) {
            unread.setVisibility(VISIBLE);
            unread.setTextColor(ColorProvider.UNREAD_TEXT.getColor(forumId));
            unread.setText(Integer.toString(thread.unreadCount));
            GradientDrawable counter = (GradientDrawable) resources.getDrawable(R.drawable.unread_counter);
            if (counter != null) {
                counter.mutate();
                boolean dim = !thread.hasNewPosts();
                if (thread.bookmarkType > 0 && prefs.coloredBookmarks) {
                    counter.setColor(ColorProvider.getBookmarkColor(thread.bookmarkType, dim));
                } else {
                    ColorProvider colorAttr = dim ? ColorProvider.UNREAD_BACKGROUND_DIM : ColorProvider.UNREAD_BACKGROUND;
                    counter.setColor(colorAttr.getColor(forumId));
                }
                unread.setBackgroundDrawable(counter);
            }
        }

	}

	/** Utility method to set and show an imageview */
    private static void showImage(ImageView imageView, Drawable drawable) {
        imageView.setVisibility(VISIBLE);
        imageView.setImageDrawable(drawable);
    }

}
