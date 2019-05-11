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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.ferg.awfulapp.AwfulFragment;
import com.ferg.awfulapp.ForumDisplayFragment;
import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.forums.ClassicThreadTag;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.ColorProvider;
import com.ferg.awfulapp.provider.DatabaseHelper;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static butterknife.ButterKnife.findById;

public class AwfulThread extends AwfulPagedItem  {

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


    // TODO: 04/06/2017 explicit default values, nulls where parsed data doesn't set values (i.e. never added to the ContentValues)?
    public int id;
    public int index;
    public String title;

    public int forumId;
//  public   String forumTitle;

    public String author;
    public int authorId;
    public String lastPoster;
    public int postCount;
    public int unreadCount;

    public int rating;
    public int bookmarkType;

    public boolean isLocked;
    public boolean isSticky;
    public boolean canOpenClose;
    public boolean hasBeenViewed;
    public boolean archived;

    public String tagUrl;
    public String tagCacheFile;
    public int tagExtra;
    public int category;


    @Nullable
    public static AwfulThread fromCursorRow(@NonNull Cursor row) {
        if (row.isBeforeFirst() || row.isAfterLast()) {
            Timber.w("fromCursor: passed empty row");
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

    public int getReadCount() {
        return postCount - unreadCount;
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
    static List<ContentValues> parseForumThreads(Document forumPage, int forumId, int startIndex) {
        long startTime = System.currentTimeMillis();
        String update_time = new Timestamp(startTime).toString();
        Timber.v("Update time: %s", update_time);
        String username = AwfulPreferences.getInstance().username;

        List<ForumParseTask> parseTasks = new ArrayList<>();
        for (Element threadElement : forumPage.select("#forum .thread")) {
            if (TextUtils.isEmpty(threadElement.id())) {
                //skip the table header
                continue;
            }
            parseTasks.add(new ForumParseTask(threadElement, forumId, startIndex, username, update_time));
            startIndex++;
        }
        List<ContentValues> result = ForumParsingKt.parse(parseTasks);

        float averageParseTime = (System.currentTimeMillis() - startTime) / (float) result.size();
        Timber.i("%d threads parsed\nAverage parse time: %.3fms", result.size(), averageParseTime);
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
     *  @param resolver     a ContentResolver used to access the database
     * @param page         the thread page's HTML document
     * @param threadId     the ID of this thread
     * @param pageNumber   which page of the thread this document represents
     * @param lastPageNumber the number of the last page in this thread
     * @param postsPerPage used to calculate post counts
     * @param prefs        a preferences instance
     * @param filterUserId if this page is for a thread filtered by user, this should be set to the user's ID, otherwise 0
     */
    public static void parseThreadPage(ContentResolver resolver, Document page, int threadId, int pageNumber, int lastPageNumber, int postsPerPage, AwfulPreferences prefs, int filterUserId) {
        long startTime = System.currentTimeMillis();
        // TODO: 03/06/2017 see issue #503 on GitHub - filtering by user means the thread data gets overwritten by the pages from this new, shorter thread containing their posts
        final int BLANK_USER_ID = 0;
        // TODO: 05/01/2018 this filtering on userID thing isn't actually doing anything...
        final boolean filteringOnUserId = filterUserId > BLANK_USER_ID;

        // finally write new thread data to the database
        ContentValues cv = new ThreadPageParseTask(resolver, page, threadId, pageNumber, lastPageNumber, postsPerPage, prefs).call();
        // TODO: 04/06/2017 this should be handled in the database-management classes
        String update_time = new Timestamp(startTime).toString();
        cv.put(DatabaseHelper.UPDATED_TIMESTAMP, update_time);
        if (resolver.update(ContentUris.withAppendedId(CONTENT_URI, threadId), cv, null, null) < 1) {
            resolver.insert(CONTENT_URI, cv);
        }

        Timber.i("Thread parse time: %dms", System.currentTimeMillis() - startTime);
    }


    @SuppressWarnings("deprecation")
	public static void setDataOnThreadListItem(View item, AwfulPreferences prefs, Cursor data, AwfulFragment parent) {
        AwfulThread thread = fromCursorRow(data);
        if (thread == null) {
            Timber.w("setDataOnThreadView: unable to get data for thread!");
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
        final ClassicThreadTag threadTag = (ClassicThreadTag) findById(item, R.id.thread_tag);
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
