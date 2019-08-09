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

package com.ferg.awfulapp.provider;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.ferg.awfulapp.AwfulApplication;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulEmote;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.ferg.awfulapp.provider.DatabaseHelper.TABLE_DRAFTS;
import static com.ferg.awfulapp.provider.DatabaseHelper.TABLE_EMOTES;
import static com.ferg.awfulapp.provider.DatabaseHelper.TABLE_FORUM;
import static com.ferg.awfulapp.provider.DatabaseHelper.TABLE_PM;
import static com.ferg.awfulapp.provider.DatabaseHelper.TABLE_POSTS;
import static com.ferg.awfulapp.provider.DatabaseHelper.TABLE_THREADS;
import static com.ferg.awfulapp.provider.DatabaseHelper.TABLE_UCP_THREADS;

public class AwfulProvider extends ContentProvider {
    private static final String TAG = "AwfulProvider";

    private DatabaseHelper mDbHelper;
    /** Set in #onCreate, so it should never be null when methods come to use it*/
    private Context context;


    ///////////////////////////////////////////////////////////////////////////
    // Matching Uris to types
    ///////////////////////////////////////////////////////////////////////////

    private static final int URI_FORUM = 0;
    private static final int URI_FORUM_ID = 1;
    private static final int URI_POST = 2;
    private static final int URI_POST_ID = 3;
    private static final int URI_THREAD = 4;
    private static final int URI_THREAD_ID = 5;
    private static final int URI_UCP_THREAD = 6;
    private static final int URI_UCP_THREAD_ID = 7;
    private static final int URI_PM = 8;
    private static final int URI_PM_ID = 9;
    private static final int URI_DRAFT = 10;
    private static final int URI_DRAFT_ID = 11;
    private static final int URI_EMOTE = 12;
    private static final int URI_EMOTE_ID = 13;
    /** This just holds the Uri types that directly refer to tables, not IDs */
	private static final Set<Integer> TABLE_URIS = new HashSet<>(Arrays.asList(URI_FORUM, URI_POST, URI_THREAD, URI_UCP_THREAD, URI_PM, URI_DRAFT, URI_EMOTE));

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(Constants.AUTHORITY, "forum", URI_FORUM);
        sUriMatcher.addURI(Constants.AUTHORITY, "forum/#", URI_FORUM_ID);
        sUriMatcher.addURI(Constants.AUTHORITY, "thread", URI_THREAD);
        sUriMatcher.addURI(Constants.AUTHORITY, "thread/#", URI_THREAD_ID);
        sUriMatcher.addURI(Constants.AUTHORITY, "post", URI_POST);
        sUriMatcher.addURI(Constants.AUTHORITY, "post/#", URI_POST_ID);
        sUriMatcher.addURI(Constants.AUTHORITY, "ucpthread", URI_UCP_THREAD);
        sUriMatcher.addURI(Constants.AUTHORITY, "ucpthread/#", URI_UCP_THREAD_ID);
        sUriMatcher.addURI(Constants.AUTHORITY, "privatemessages", URI_PM);
        sUriMatcher.addURI(Constants.AUTHORITY, "privatemessages/#", URI_PM_ID);
        sUriMatcher.addURI(Constants.AUTHORITY, "draftreplies", URI_DRAFT);
        sUriMatcher.addURI(Constants.AUTHORITY, "draftreplies/#", URI_DRAFT_ID);
        sUriMatcher.addURI(Constants.AUTHORITY, "emote", URI_EMOTE);
        sUriMatcher.addURI(Constants.AUTHORITY, "emote/#", URI_EMOTE_ID);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Projections
    ///////////////////////////////////////////////////////////////////////////

    // TODO: 06/05/2017 some of these maps are broken into multiple basic projections, and some are almost the full map. Might be worth checking if this is still right

    @NonNull
    private static String[] arrayOfKeys(@NonNull Map<String, ?> map) {
        return map.keySet().toArray(new String[map.size()]);
    }

    // Forum
	private static final HashMap<String, String> sForumProjectionMap = new HashMap<>();
    static {
        sForumProjectionMap.put(AwfulForum.ID, AwfulForum.ID);
        sForumProjectionMap.put(AwfulForum.PARENT_ID, AwfulForum.PARENT_ID);
        sForumProjectionMap.put(AwfulForum.INDEX, AwfulForum.INDEX);
        sForumProjectionMap.put(AwfulForum.TITLE, AwfulForum.TITLE);
        sForumProjectionMap.put(AwfulForum.SUBTEXT, AwfulForum.SUBTEXT);
        sForumProjectionMap.put(AwfulForum.PAGE_COUNT, AwfulForum.PAGE_COUNT);
        sForumProjectionMap.put(AwfulForum.TAG_URL, AwfulForum.TAG_URL);
        sForumProjectionMap.put(AwfulForum.TAG_CACHEFILE, AwfulForum.TAG_CACHEFILE);
        sForumProjectionMap.put(DatabaseHelper.UPDATED_TIMESTAMP, DatabaseHelper.UPDATED_TIMESTAMP);
    }
    public static final String[] ForumProjection = arrayOfKeys(sForumProjectionMap);

    // Thread
	private static final HashMap<String, String> sThreadProjectionMap = new HashMap<>();
    static {
        sThreadProjectionMap.put(AwfulThread.ID, TABLE_THREADS+"."+AwfulThread.ID+" AS "+AwfulThread.ID);
        sThreadProjectionMap.put(AwfulThread.FORUM_ID, AwfulThread.FORUM_ID);
        sThreadProjectionMap.put(AwfulThread.INDEX, AwfulThread.INDEX);
        sThreadProjectionMap.put(AwfulThread.TITLE, TABLE_THREADS+"."+AwfulThread.TITLE+" AS "+AwfulThread.TITLE);
        sThreadProjectionMap.put(AwfulThread.POSTCOUNT, AwfulThread.POSTCOUNT);
        sThreadProjectionMap.put(AwfulThread.UNREADCOUNT, AwfulThread.UNREADCOUNT);
        sThreadProjectionMap.put(AwfulThread.AUTHOR, AwfulThread.AUTHOR);
        sThreadProjectionMap.put(AwfulThread.AUTHOR_ID, AwfulThread.AUTHOR_ID);
        sThreadProjectionMap.put(AwfulThread.LOCKED, AwfulThread.LOCKED);
        sThreadProjectionMap.put(AwfulThread.CAN_OPEN_CLOSE, AwfulThread.CAN_OPEN_CLOSE);
        sThreadProjectionMap.put(AwfulThread.BOOKMARKED, AwfulThread.BOOKMARKED);
        sThreadProjectionMap.put(AwfulThread.STICKY, AwfulThread.STICKY);
        sThreadProjectionMap.put(AwfulThread.CATEGORY, AwfulThread.CATEGORY);
        sThreadProjectionMap.put(AwfulThread.LASTPOSTER, AwfulThread.LASTPOSTER);
        sThreadProjectionMap.put(AwfulThread.HAS_NEW_POSTS, AwfulThread.UNREADCOUNT+" > 0 AS "+ AwfulThread.HAS_NEW_POSTS);
        sThreadProjectionMap.put(AwfulThread.HAS_VIEWED_THREAD, AwfulThread.HAS_VIEWED_THREAD);
        sThreadProjectionMap.put(AwfulThread.ARCHIVED, AwfulThread.ARCHIVED);
        sThreadProjectionMap.put(AwfulThread.RATING, AwfulThread.RATING);
        sThreadProjectionMap.put(AwfulThread.TAG_URL, TABLE_THREADS+"."+AwfulThread.TAG_URL+" AS "+AwfulThread.TAG_URL);
        sThreadProjectionMap.put(AwfulThread.TAG_EXTRA, TABLE_THREADS+"."+AwfulThread.TAG_EXTRA+" AS "+AwfulThread.TAG_EXTRA);
        sThreadProjectionMap.put(AwfulThread.TAG_CACHEFILE, TABLE_THREADS+"."+AwfulThread.TAG_CACHEFILE+" AS "+AwfulThread.TAG_CACHEFILE);
        sThreadProjectionMap.put(AwfulThread.FORUM_TITLE, TABLE_FORUM+"."+AwfulForum.TITLE+" AS "+AwfulThread.FORUM_TITLE);
        sThreadProjectionMap.put(DatabaseHelper.UPDATED_TIMESTAMP, TABLE_THREADS+"."+ DatabaseHelper.UPDATED_TIMESTAMP+" AS "+ DatabaseHelper.UPDATED_TIMESTAMP);
    }
    public static final String[] ThreadProjection = arrayOfKeys(sThreadProjectionMap);

    // Post
	private static final HashMap<String, String> sPostProjectionMap = new HashMap<>();
    static {
        sPostProjectionMap.put(AwfulPost.ID, AwfulPost.ID);
        sPostProjectionMap.put(AwfulPost.THREAD_ID, AwfulPost.THREAD_ID);
        sPostProjectionMap.put(AwfulPost.POST_INDEX, AwfulPost.POST_INDEX);
        sPostProjectionMap.put(AwfulPost.DATE, AwfulPost.DATE);
        sPostProjectionMap.put(AwfulPost.REGDATE, AwfulPost.REGDATE);
        sPostProjectionMap.put(AwfulPost.USER_ID, AwfulPost.USER_ID);
        sPostProjectionMap.put(AwfulPost.USERNAME, AwfulPost.USERNAME);
        sPostProjectionMap.put(AwfulPost.PREVIOUSLY_READ, AwfulPost.PREVIOUSLY_READ);
        sPostProjectionMap.put(AwfulPost.EDITABLE, AwfulPost.EDITABLE);
        sPostProjectionMap.put(AwfulPost.IS_OP, AwfulPost.IS_OP);
        sPostProjectionMap.put(AwfulPost.IS_ADMIN, AwfulPost.IS_ADMIN);
        sPostProjectionMap.put(AwfulPost.IS_MOD, AwfulPost.IS_MOD);
        sPostProjectionMap.put(AwfulPost.IS_PLAT, AwfulPost.IS_PLAT);
        sPostProjectionMap.put(AwfulPost.AVATAR, AwfulPost.AVATAR);
        sPostProjectionMap.put(AwfulPost.AVATAR_TEXT, AwfulPost.AVATAR_TEXT);
        sPostProjectionMap.put(AwfulPost.CONTENT, AwfulPost.CONTENT);
        sPostProjectionMap.put(AwfulPost.EDITED, AwfulPost.EDITED);
    }
    public static final String[] PostProjection = arrayOfKeys(sPostProjectionMap);

    // UCP Thread
	private static final HashMap<String, String> sUCPThreadProjectionMap = new HashMap<>();
    static {
        //hopefully this should let the join happen
        //but documentation on projection maps is fucking scarce.
        sUCPThreadProjectionMap.put(AwfulThread.ID, TABLE_THREADS+"."+AwfulThread.ID+" AS "+AwfulThread.ID);//threads._id AS _id
        sUCPThreadProjectionMap.put(AwfulThread.FORUM_ID, AwfulThread.FORUM_ID);
        sUCPThreadProjectionMap.put(AwfulThread.INDEX, TABLE_UCP_THREADS+"."+AwfulThread.INDEX+" AS "+AwfulThread.INDEX);
        sUCPThreadProjectionMap.put(AwfulThread.TITLE, TABLE_THREADS+"."+AwfulThread.TITLE+" AS "+AwfulThread.TITLE);
        sUCPThreadProjectionMap.put(AwfulThread.POSTCOUNT, AwfulThread.POSTCOUNT);
        sUCPThreadProjectionMap.put(AwfulThread.UNREADCOUNT, AwfulThread.UNREADCOUNT);
        sUCPThreadProjectionMap.put(AwfulThread.AUTHOR, AwfulThread.AUTHOR);
        sUCPThreadProjectionMap.put(AwfulThread.AUTHOR_ID, AwfulThread.AUTHOR_ID);
        sUCPThreadProjectionMap.put(AwfulThread.LOCKED, AwfulThread.LOCKED);
        sUCPThreadProjectionMap.put(AwfulThread.CAN_OPEN_CLOSE, AwfulThread.CAN_OPEN_CLOSE);
        sUCPThreadProjectionMap.put(AwfulThread.BOOKMARKED, AwfulThread.BOOKMARKED);
        sUCPThreadProjectionMap.put(AwfulThread.STICKY, AwfulThread.STICKY);
        sUCPThreadProjectionMap.put(AwfulThread.CATEGORY, AwfulThread.CATEGORY);
        sUCPThreadProjectionMap.put(AwfulThread.LASTPOSTER, AwfulThread.LASTPOSTER);
        sUCPThreadProjectionMap.put(AwfulThread.TAG_URL, AwfulThread.TAG_URL);
        sUCPThreadProjectionMap.put(AwfulThread.TAG_EXTRA, AwfulThread.TAG_EXTRA);
        sUCPThreadProjectionMap.put(AwfulThread.TAG_CACHEFILE, AwfulThread.TAG_CACHEFILE);
        sUCPThreadProjectionMap.put(AwfulThread.HAS_NEW_POSTS, AwfulThread.UNREADCOUNT+" > 0 AS "+AwfulThread.HAS_NEW_POSTS);
        sUCPThreadProjectionMap.put(AwfulThread.HAS_VIEWED_THREAD, AwfulThread.HAS_VIEWED_THREAD);
        sUCPThreadProjectionMap.put(AwfulThread.ARCHIVED, AwfulThread.ARCHIVED);
        sUCPThreadProjectionMap.put(AwfulThread.RATING, AwfulThread.RATING);
        sUCPThreadProjectionMap.put(AwfulThread.FORUM_TITLE, "null");
        sUCPThreadProjectionMap.put(DatabaseHelper.UPDATED_TIMESTAMP, TABLE_UCP_THREADS+"."+ DatabaseHelper.UPDATED_TIMESTAMP+" AS "+ DatabaseHelper.UPDATED_TIMESTAMP);
    }

    // Drafts
	private static final HashMap<String, String> sDraftProjectionMap = new HashMap<>();
    static {
        sDraftProjectionMap.put(AwfulMessage.ID, AwfulMessage.ID);
        sDraftProjectionMap.put(AwfulMessage.TITLE, AwfulMessage.TITLE);
        sDraftProjectionMap.put(AwfulPost.FORM_COOKIE, AwfulPost.FORM_COOKIE);
        sDraftProjectionMap.put(AwfulPost.FORM_KEY, AwfulPost.FORM_KEY);
        sDraftProjectionMap.put(AwfulMessage.REPLY_CONTENT, AwfulMessage.REPLY_CONTENT);
        sDraftProjectionMap.put(AwfulMessage.REPLY_ICON, AwfulMessage.REPLY_ICON);
        sDraftProjectionMap.put(AwfulMessage.RECIPIENT, AwfulMessage.RECIPIENT);
        sDraftProjectionMap.put(AwfulMessage.TYPE, AwfulMessage.TYPE);
        sDraftProjectionMap.put(AwfulPost.EDIT_POST_ID, AwfulPost.EDIT_POST_ID);
        sDraftProjectionMap.put(AwfulPost.REPLY_ORIGINAL_CONTENT, AwfulPost.REPLY_ORIGINAL_CONTENT);
        sDraftProjectionMap.put(AwfulMessage.REPLY_ATTACHMENT, AwfulMessage.REPLY_ATTACHMENT);
        sDraftProjectionMap.put(AwfulPost.FORM_BOOKMARK, AwfulPost.FORM_BOOKMARK);
        sDraftProjectionMap.put(AwfulMessage.EPOC_TIMESTAMP, AwfulMessage.EPOC_TIMESTAMP);
        sDraftProjectionMap.put(DatabaseHelper.UPDATED_TIMESTAMP, DatabaseHelper.UPDATED_TIMESTAMP);
    }
    public static final String[] DraftProjection = new String[]{
            AwfulMessage.ID,
            AwfulMessage.TYPE,
            AwfulMessage.RECIPIENT,
            AwfulMessage.TITLE,
            AwfulMessage.REPLY_CONTENT,
            AwfulMessage.REPLY_ICON
    };
    public static final String[] DraftPostProjection = new String[]{
            AwfulMessage.ID,
            AwfulMessage.TYPE,
            AwfulPost.FORM_COOKIE,
            AwfulPost.FORM_KEY,
            AwfulPost.EDIT_POST_ID,
            AwfulPost.REPLY_ORIGINAL_CONTENT,
            AwfulMessage.REPLY_CONTENT,
            AwfulMessage.REPLY_ATTACHMENT,
            AwfulPost.FORM_BOOKMARK,
            AwfulMessage.EPOC_TIMESTAMP,
            DatabaseHelper.UPDATED_TIMESTAMP
    };

    // Private messages
	private static final HashMap<String, String> sPMReplyProjectionMap = new HashMap<>();
    static {
        sPMReplyProjectionMap.put(AwfulMessage.ID, TABLE_PM+"."+AwfulMessage.ID+" AS "+AwfulMessage.ID);
        sPMReplyProjectionMap.put(AwfulMessage.TITLE, TABLE_PM+"."+AwfulMessage.TITLE+" AS "+AwfulMessage.TITLE);
        sPMReplyProjectionMap.put(AwfulMessage.CONTENT, AwfulMessage.CONTENT);
        sPMReplyProjectionMap.put(AwfulMessage.AUTHOR, AwfulMessage.AUTHOR);
        sPMReplyProjectionMap.put(AwfulMessage.DATE, AwfulMessage.DATE);
        sPMReplyProjectionMap.put(AwfulMessage.UNREAD, AwfulMessage.UNREAD);
        sPMReplyProjectionMap.put(AwfulMessage.REPLY_CONTENT, AwfulMessage.REPLY_CONTENT);
        sPMReplyProjectionMap.put(AwfulMessage.REPLY_TITLE, TABLE_DRAFTS+"."+AwfulMessage.TITLE+" AS "+AwfulMessage.REPLY_TITLE);
        sPMReplyProjectionMap.put(AwfulMessage.RECIPIENT, AwfulMessage.RECIPIENT);
        sPMReplyProjectionMap.put(AwfulMessage.TYPE, AwfulMessage.TYPE);
        sPMReplyProjectionMap.put(AwfulMessage.ICON, AwfulMessage.ICON);
        sPMReplyProjectionMap.put(AwfulMessage.REPLY_ICON, AwfulMessage.REPLY_ICON);
        sPMReplyProjectionMap.put(AwfulMessage.FOLDER, AwfulMessage.FOLDER);
    }
    public static final String[] PMProjection = new String[]{
            AwfulMessage.ID,
            AwfulMessage.AUTHOR,
            AwfulMessage.TITLE,
            AwfulMessage.CONTENT,
            AwfulMessage.UNREAD,
            AwfulMessage.ICON,
            AwfulMessage.DATE
    };
    public static final String[] PMReplyProjection = new String[]{
            AwfulMessage.ID,
            AwfulMessage.AUTHOR,
            AwfulMessage.TITLE,
            AwfulMessage.CONTENT,
            AwfulMessage.UNREAD,
            AwfulMessage.DATE,
            AwfulMessage.TYPE,
            AwfulMessage.RECIPIENT,
            AwfulMessage.REPLY_TITLE,
            AwfulMessage.REPLY_CONTENT,
            AwfulMessage.REPLY_ICON
    };

    // Emotes
	private static final HashMap<String, String> sEmoteProjectionMap = new HashMap<>();
    static {
        sEmoteProjectionMap.put(AwfulEmote.ID, AwfulEmote.ID);
        sEmoteProjectionMap.put(AwfulEmote.TEXT, AwfulEmote.TEXT);
        sEmoteProjectionMap.put(AwfulEmote.SUBTEXT, AwfulEmote.SUBTEXT);
        sEmoteProjectionMap.put(AwfulEmote.URL, AwfulEmote.URL);
        sEmoteProjectionMap.put(AwfulEmote.INDEX, AwfulEmote.INDEX);
        sEmoteProjectionMap.put(DatabaseHelper.UPDATED_TIMESTAMP, DatabaseHelper.UPDATED_TIMESTAMP);
    }
    public static final String[] EmoteProjection = arrayOfKeys(sEmoteProjectionMap);


    ///////////////////////////////////////////////////////////////////////////
    // ContentProvider functions
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onCreate() {
        context = getContext();
        mDbHelper = new DatabaseHelper(context);
        return true;
    }

    @Override
    public String getType(@NonNull Uri aUri) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri aUri, String aWhere, String[] aWhereArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int uriType = matchUri(aUri, true);
        assertIsTableUri(uriType);
        String table = getTableForUriType(uriType);

        // if there's no Where clause, this will delete everything in the table!
        return db.delete(table, aWhere, aWhereArgs);
    }


    @Override
    public int update(@NonNull Uri aUri, ContentValues aValues, String aWhere, String[] aWhereArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        final int uriType = matchUri(aUri, true);
        String table = getTableForUriType(uriType);
        String whereClause;

        // ID-type Uris need a Where clause as well as the table
        switch (uriType) {
            case URI_FORUM_ID:
                whereClause = AwfulForum.ID;
                break;
            case URI_POST_ID:
                whereClause = AwfulPost.ID;
                break;
            case URI_THREAD_ID:
            case URI_UCP_THREAD_ID:
                whereClause = AwfulThread.ID;
                break;
            case URI_PM_ID:
            case URI_DRAFT_ID:
                whereClause = AwfulMessage.ID;
                break;
            case URI_EMOTE_ID:
                whereClause = AwfulEmote.ID;
                break;
            default:
                whereClause = null;
        }
        if (whereClause != null) {
            aWhere = whereClause + "=?";
            aWhereArgs = insertSelectionArg(aWhereArgs, aUri.getLastPathSegment());
        }

        int result = db.update(table, aValues, aWhere, aWhereArgs);
        context.getContentResolver().notifyChange(aUri, null);
        return result;
    }


    @Override
    public Uri insert(@NonNull Uri aUri, ContentValues aValues) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        final int uriType = matchUri(aUri, true);
        assertIsTableUri(uriType);
        String table = getTableForUriType(uriType);

        long rowId = db.insert(table, "", aValues);
        if (rowId > -1) {
            return ContentUris.withAppendedId(aUri, rowId);
        }
        throw new SQLException("Failed to insert row into " + aUri);
    }


    @Override
    public int bulkInsert(@NonNull Uri aUri, @NonNull ContentValues[] aValues) {
        // avoid DB operations and update notifications when there's nothing to do
        if (aValues.length == 0) {
            return 0;
        }
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        final int uriType = matchUri(aUri, true);
        assertIsTableUri(uriType);
        String table = getTableForUriType(uriType);

        db.beginTransaction();
        try {
            for (ContentValues value : aValues) {
                if (uriType == URI_POST) {
                    db.delete(table, AwfulPost.POST_INDEX + "=? AND " + AwfulPost.THREAD_ID + "=?",
                            int2StrArray(value.getAsInteger(AwfulPost.POST_INDEX), value.getAsInteger(AwfulPost.THREAD_ID)));
                } else if (uriType == URI_EMOTE) {
                    db.delete(table, AwfulEmote.TEXT + "=?", new String[]{value.getAsString(AwfulEmote.TEXT)});
                }
                db.replace(table, "", value);
            }

            db.setTransactionSuccessful();
            context.getContentResolver().notifyChange(aUri, null);
        } catch (SQLiteConstraintException e) {
            Log.w(TAG, e.toString());
            // transaction failed (exception throws before #setTransactionSuccessful), no rows inserted
            return 0;
        } finally {
            db.endTransaction();
        }
        // transaction succeeded, all rows inserted
        return aValues.length;
    }


    @Nullable
    @Override
    public Cursor query(@NonNull Uri aUri, String[] aProjection, String aSelection,
                        String[] aSelectionArgs, String aSortOrder)
    {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        // Typically this should fetch a readable database but we're querying before
        // we actually add anything, so make it writable.
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        final int uriType = matchUri(aUri, false);
        // check for non-match, return null since an unrecognised/malformed Uri gives us nothing useful to do
        if (uriType == UriMatcher.NO_MATCH) {
            String msg = String.format("Unrecognised query Uri!\nUri: %s\nProjection: %s\nSelection: %s\nSelection args: %s\nSort order: %s",
                    aUri, Arrays.toString(aProjection), aSelection, Arrays.toString(aSelectionArgs), aSortOrder);
            if (AwfulApplication.crashlyticsEnabled()) {
                Crashlytics.log(Log.WARN, TAG, msg);
            } else {
                Log.w(TAG, msg);
            }
            return null;
        }

        // get the basic table name for this Uri - some will need to replace this with something more complex below
        String table = getTableForUriType(uriType);
        String whereClause = null;
        // set params on the query builder according to the type of Uri - these pairs fall through intentionally
        switch(uriType) {
            case URI_FORUM_ID:
                whereClause = AwfulForum.ID;
            case URI_FORUM:
                builder.setProjectionMap(sForumProjectionMap);
                break;

            case URI_POST_ID:
                whereClause = AwfulPost.ID;
            case URI_POST:
                builder.setProjectionMap(sPostProjectionMap);
                break;

            case URI_THREAD_ID:
                whereClause = TABLE_THREADS+"."+AwfulThread.ID;
            case URI_THREAD:
                table = TABLE_THREADS+" LEFT OUTER JOIN "+ TABLE_FORUM+" ON "+ TABLE_THREADS+"."+AwfulThread.FORUM_ID+"="+ TABLE_FORUM+"."+AwfulForum.ID;
                builder.setProjectionMap(sThreadProjectionMap);
                break;

            case URI_UCP_THREAD_ID:
                whereClause = AwfulThread.ID;
            case URI_UCP_THREAD:
                //hopefully this join works
                table = TABLE_UCP_THREADS+", "+ TABLE_THREADS+" ON "+ TABLE_UCP_THREADS+"."+AwfulThread.ID+"="+ TABLE_THREADS+"."+AwfulThread.ID;
                builder.setProjectionMap(sUCPThreadProjectionMap);
                break;

            case URI_PM_ID:
                whereClause = TABLE_PM+"."+AwfulMessage.ID;
            case URI_PM:
                table = TABLE_PM+" LEFT OUTER JOIN "+ TABLE_DRAFTS+" ON "+ TABLE_PM+"."+AwfulMessage.ID+"="+ TABLE_DRAFTS+"."+AwfulMessage.ID;
                builder.setProjectionMap(sPMReplyProjectionMap);
                break;

            case URI_DRAFT_ID:
                whereClause = AwfulMessage.ID;
            case URI_DRAFT:
                builder.setProjectionMap(sDraftProjectionMap);
                break;

            case URI_EMOTE_ID:
                whereClause = AwfulEmote.ID;
            case URI_EMOTE:
                builder.setProjectionMap(sEmoteProjectionMap);
                break;
            default:
                // this should explicitly handle all valid Uris, so if we get here, someone blew it
                throw new RuntimeException(TAG + " - Unhandled URI type: " + uriType);
        }

        builder.setTables(table);
        if (whereClause != null) {
            builder.appendWhere(whereClause + "=?");
            aSelectionArgs = insertSelectionArg(aSelectionArgs, aUri.getLastPathSegment());
        }

        // perform the query
        try {
            Cursor result = builder.query(db, aProjection, aSelection,
                    aSelectionArgs, null, null, aSortOrder);
            result.setNotificationUri(context.getContentResolver(), aUri);
            return result;
        } catch (Exception e) {
            String msg = String.format("aUri:\n%s\nQuery tables string:\n%s", aUri, builder.getTables());
            if (AwfulApplication.crashlyticsEnabled()){
                Crashlytics.log(Log.WARN, TAG, msg);
            } else{
                Log.w(TAG, msg, e);
            }
            throw e;
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Utility methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Look up the DB table corresponding to a ContentProvider Uri type.
     *
     * This is intended to map all types to a table, so it will throw a RuntimeException if a
     * currently unhandled Uri type is passed in.
     * @param uriType one of the Uri types returned by {@link #matchUri(Uri, boolean)}
     * @return the name of its corresponding database table
     */
    @NonNull
    private String getTableForUriType(int uriType) {
		switch (uriType) {
			case URI_FORUM_ID:
			case URI_FORUM:
				return TABLE_FORUM;
			case URI_POST_ID:
			case URI_POST:
				return TABLE_POSTS;
			case URI_THREAD_ID:
			case URI_THREAD:
				return TABLE_THREADS;
			case URI_UCP_THREAD_ID:
			case URI_UCP_THREAD:
				return TABLE_UCP_THREADS;
			case URI_PM_ID:
			case URI_PM:
				return TABLE_PM;
			case URI_DRAFT_ID:
			case URI_DRAFT:
				return TABLE_DRAFTS;
			case URI_EMOTE_ID:
			case URI_EMOTE:
				return TABLE_EMOTES;
			default:
				throw new RuntimeException("Invalid table constant: " + uriType);
		}
	}


	/**
	 * Matches a Uri to the defined patterns.
	 *
	 * @param aUri the Uri to match against
	 * @param throwIfUnmatched if true, a failed match will throw a RuntimeException
	 * @return the ID of the matched pattern, or {@link UriMatcher#NO_MATCH} if it failed
	 */
	private static int matchUri(@NonNull Uri aUri, boolean throwIfUnmatched) {
		final int match = sUriMatcher.match(aUri);
		if (match == UriMatcher.NO_MATCH && throwIfUnmatched) {
			throw new RuntimeException("Unmatched Uri: " + aUri);
		}
		return match;
	}


	/**
	 * Throw an exception if the supplied Uri type constant does not correspond to a table.
	 */
	@SuppressLint("DefaultLocale")
	private static void assertIsTableUri(int uriType) {
		if (!TABLE_URIS.contains(uriType)) {
			throw new RuntimeException(String.format("Uri type [%d] does not correspond to a table", uriType));
		}
	}

    /**
      * Inserts an argument at the beginning of the selection arg list.
      *
      * The {@link android.database.sqlite.SQLiteQueryBuilder}'s where clause is
      * prepended to the user's where clause (combined with 'AND') to generate
      * the final where close, so arguments associated with the QueryBuilder are
      * prepended before any user selection args to keep them in the right order.
      */
    private String[] insertSelectionArg(String[] selectionArgs, String arg) {
        if (selectionArgs == null) {
            return new String[] {arg};
        } else {
            int newLength = selectionArgs.length + 1;
            String[] newSelectionArgs = new String[newLength];
            newSelectionArgs[0] = arg;
            System.arraycopy(selectionArgs, 0, newSelectionArgs, 1, selectionArgs.length);
            return newSelectionArgs;
        }
    }

	/**
	 * Convert an array of ints to an array of Strings
	 */
	@NonNull
	public static String[] int2StrArray(@NonNull int... args){
		String[] strings = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			strings[i] = Integer.toString(args[i]);
		}
		return strings;
    }

}
