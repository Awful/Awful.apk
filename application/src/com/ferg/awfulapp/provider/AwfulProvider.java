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

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.thread.AwfulEmote;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;

public class AwfulProvider extends ContentProvider {
    private static final String TAG = "AwfulProvider";
    /**
     * So this whole thing works, but is really ugly and maintains poorly. I'd rewrite it, but it's a lot of work for no real benefit this late in the project.
     *
     * e: I really need to refactor this shit, I have a split-up version of this pattern that functions the same but is a hell of a lot easier to maintain.
     */

    private static final String DATABASE_NAME = "awful.db";
    private static final int DATABASE_VERSION = 28;

    public static final String TABLE_FORUM    = "forum";
    public static final String TABLE_THREADS    = "threads";
    public static final String TABLE_UCP_THREADS    = "ucp_thread";
    public static final String TABLE_POSTS    = "posts";
    public static final String TABLE_EMOTES    = "emotes";
    public static final String TABLE_PM    = "private_messages";
    public static final String TABLE_DRAFTS    = "draft_messages";
    
    public static final String UPDATED_TIMESTAMP    = "timestamp_row_update";

    private static final int FORUM     = 0;
    private static final int FORUM_ID  = 1;
    private static final int POST      = 2;
    private static final int POST_ID   = 3;
    private static final int THREAD    = 4;
    private static final int THREAD_ID = 5;
    private static final int UCP_THREAD    = 6;
    private static final int UCP_THREAD_ID = 7;
    private static final int PM    = 8;
    private static final int PM_ID = 9;
    private static final int DRAFT    = 10;
    private static final int DRAFT_ID = 11;
    private static final int EMOTE    = 12;
    private static final int EMOTE_ID = 13;

    private static final UriMatcher sUriMatcher;
	private static HashMap<String, String> sForumProjectionMap;
	private static HashMap<String, String> sThreadProjectionMap;
	private static HashMap<String, String> sPostProjectionMap;
	private static HashMap<String, String> sUCPThreadProjectionMap;
	private static HashMap<String, String> sPMProjectionMap;
	private static HashMap<String, String> sDraftProjectionMap;
	private static HashMap<String, String> sPMReplyProjectionMap;
	private static HashMap<String, String> sEmoteProjectionMap;
	
	public static final String[] ThreadProjection = new String[]{
		AwfulThread.ID,
		AwfulThread.FORUM_ID,
		AwfulThread.INDEX,
		AwfulThread.TITLE,
		AwfulThread.POSTCOUNT,
		AwfulThread.UNREADCOUNT,
		AwfulThread.AUTHOR,
		AwfulThread.AUTHOR_ID,
		AwfulThread.LOCKED,
		AwfulThread.BOOKMARKED,
		AwfulThread.STICKY,
		AwfulThread.CATEGORY,
		AwfulThread.LASTPOSTER,
		AwfulThread.TAG_URL,
		AwfulThread.TAG_CACHEFILE,
		AwfulThread.FORUM_TITLE,
		AwfulThread.HAS_NEW_POSTS,
		AwfulThread.HAS_VIEWED_THREAD,
        AwfulThread.ARCHIVED,
        AwfulThread.RATING,
		UPDATED_TIMESTAMP };

	public static final String[] ForumProjection = new String[]{
		AwfulForum.ID,
		AwfulForum.PARENT_ID,
		AwfulForum.INDEX,
		AwfulForum.TITLE,
		AwfulForum.SUBTEXT,
		AwfulForum.PAGE_COUNT,
		AwfulForum.TAG_URL,
		AwfulForum.TAG_CACHEFILE,
		UPDATED_TIMESTAMP
	};
	
	public static final String[] PostProjection = new String[]{
        AwfulPost.ID,
        AwfulPost.THREAD_ID,
        AwfulPost.POST_INDEX,
        AwfulPost.DATE,
        AwfulPost.REGDATE,
        AwfulPost.USER_ID,
        AwfulPost.USERNAME,
        AwfulPost.PREVIOUSLY_READ,
        AwfulPost.EDITABLE,
        AwfulPost.IS_OP,
        AwfulPost.IS_ADMIN,
        AwfulPost.IS_MOD,
        AwfulPost.AVATAR,
        AwfulPost.AVATAR_TEXT,
        AwfulPost.CONTENT,
        AwfulPost.EDITED
	};
	
	public static final String[] PMProjection = new String[]{
		AwfulMessage.ID,
		AwfulMessage.AUTHOR,
		AwfulMessage.TITLE,
		AwfulMessage.CONTENT,
		AwfulMessage.UNREAD,
		AwfulMessage.DATE
	};
	public static final String[] DraftProjection = new String[]{
		AwfulMessage.ID,
		AwfulMessage.TYPE,
		AwfulMessage.RECIPIENT,
		AwfulMessage.TITLE,
		AwfulMessage.REPLY_CONTENT
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
		UPDATED_TIMESTAMP
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
		AwfulMessage.REPLY_CONTENT
	};
	
	public static final String[] EmoteProjection = new String[]{
		AwfulEmote.ID,
		AwfulEmote.TEXT,
		AwfulEmote.SUBTEXT,
		AwfulEmote.URL,
		AwfulEmote.INDEX,
		UPDATED_TIMESTAMP
	};
	
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context aContext) {
            super(aContext, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase aDb) {
        	createForumTable(aDb);
        	createThreadTable(aDb);
        	createUCPTable(aDb);
        	createPostTable(aDb);
        	createEmoteTable(aDb);
        	createPMTable(aDb);
        	createDraftTable(aDb);
        }
        	
        	
    	public void createForumTable(SQLiteDatabase aDb) {
            aDb.execSQL("CREATE TABLE " + TABLE_FORUM + " (" +
                AwfulForum.ID      + " INTEGER UNIQUE," + 
                AwfulForum.PARENT_ID      + " INTEGER," + //subforums list parent forum id, primary forums list 0 (index)
                AwfulForum.INDEX      + " INTEGER,"   	 + 
                AwfulForum.TITLE   + " VARCHAR,"        + 
                AwfulForum.SUBTEXT + " VARCHAR,"        + 
                AwfulForum.PAGE_COUNT + " INTEGER,"		+
                AwfulForum.TAG_URL      + " VARCHAR,"    + 
                AwfulForum.TAG_CACHEFILE + " VARCHAR,"   +
            	UPDATED_TIMESTAMP   + " DATETIME);");
    	}
        public void createThreadTable(SQLiteDatabase aDb) {
            aDb.execSQL("CREATE TABLE " + TABLE_THREADS + " ("    +
                AwfulThread.ID      + " INTEGER UNIQUE,"  + 
                AwfulThread.FORUM_ID      + " INTEGER,"   + 
                AwfulThread.INDEX    + " INTEGER," + 
                AwfulThread.TITLE   + " VARCHAR,"         + 
                AwfulThread.POSTCOUNT   + " INTEGER,"     + 
                AwfulThread.UNREADCOUNT   + " INTEGER,"   + 
                AwfulThread.AUTHOR 		 + " VARCHAR,"    + 
                AwfulThread.AUTHOR_ID 		+ " INTEGER," +
                AwfulThread.LOCKED   	+ " INTEGER,"     + 
                AwfulThread.BOOKMARKED 	    + " INTEGER," +
                AwfulThread.STICKY   		+ " INTEGER," +
                AwfulThread.CATEGORY   		+ " INTEGER," +
                AwfulThread.LASTPOSTER   	+ " VARCHAR," +
                AwfulThread.TAG_URL      + " VARCHAR,"    + 
                AwfulThread.TAG_CACHEFILE + " VARCHAR,"   +
                AwfulThread.HAS_VIEWED_THREAD + " INTEGER, " +
                AwfulThread.ARCHIVED + " INTEGER, " +
                AwfulThread.RATING + " INTEGER, " +
            	UPDATED_TIMESTAMP   + " DATETIME);");
    	}
        public void createUCPTable(SQLiteDatabase aDb) {
            
            aDb.execSQL("CREATE TABLE " + TABLE_UCP_THREADS + " ("    +
                AwfulThread.ID      + " INTEGER UNIQUE,"  + //to be joined with thread table
                AwfulThread.INDEX      + " INTEGER," +
            	UPDATED_TIMESTAMP   + " DATETIME);");
    	}
        public void createPostTable(SQLiteDatabase aDb) {

            aDb.execSQL("CREATE TABLE " + TABLE_POSTS + " (" +
                AwfulPost.ID                    + " INTEGER UNIQUE," + 
                AwfulPost.THREAD_ID             + " INTEGER,"        + 
                AwfulPost.POST_INDEX            + " INTEGER,"        + 
                AwfulPost.DATE                  + " VARCHAR,"        + 
                AwfulPost.REGDATE               + " VARCHAR,"        + 
                AwfulPost.USER_ID               + " INTEGER,"        + 
                AwfulPost.USERNAME              + " VARCHAR,"        +
                AwfulPost.PREVIOUSLY_READ       + " INTEGER,"        +
                AwfulPost.EDITABLE              + " INTEGER,"        +
                AwfulPost.IS_OP                 + " INTEGER,"        +
                AwfulPost.IS_ADMIN              + " INTEGER,"        +
                AwfulPost.IS_MOD                + " INTEGER,"        +
                AwfulPost.AVATAR                + " VARCHAR,"        + 
                AwfulPost.AVATAR_TEXT           + " VARCHAR,"        + 
                AwfulPost.CONTENT               + " VARCHAR,"        + 
                AwfulPost.EDITED                + " VARCHAR," +
            	UPDATED_TIMESTAMP   + " DATETIME);");
    	}
        public void createEmoteTable(SQLiteDatabase aDb) {
            
            aDb.execSQL("CREATE TABLE " + TABLE_EMOTES + " ("    +
        		AwfulEmote.ID      	 + " INTEGER UNIQUE,"  + 
        		AwfulEmote.TEXT      + " VARCHAR,"   + 
                AwfulEmote.SUBTEXT   + " VARCHAR,"         + 
                AwfulEmote.URL   	 + " VARCHAR,"     + 
                AwfulEmote.INDEX   	 + " INTEGER,"     + 
            	UPDATED_TIMESTAMP   + " DATETIME);");
    	}
        public void createPMTable(SQLiteDatabase aDb) {
            
            aDb.execSQL("CREATE TABLE " + TABLE_PM + " ("    +
                AwfulMessage.ID      	 + " INTEGER UNIQUE,"  + 
                AwfulMessage.TITLE      + " VARCHAR,"   + 
                AwfulMessage.AUTHOR      + " VARCHAR,"   + 
                AwfulMessage.CONTENT      + " VARCHAR,"   + 
                AwfulMessage.UNREAD      + " INTEGER,"   + 
                AwfulMessage.FOLDER      + " INTEGER,"   + 
                AwfulMessage.DATE + " VARCHAR," +
            	UPDATED_TIMESTAMP   + " DATETIME);");
    	}
        public void createDraftTable(SQLiteDatabase aDb) {
            
            
            aDb.execSQL("CREATE TABLE " + TABLE_DRAFTS + " ("    +
                AwfulMessage.ID      	 + " INTEGER UNIQUE,"  + 
                AwfulMessage.TYPE      	 + " INTEGER,"  + 
                AwfulMessage.TITLE      + " VARCHAR,"   + 
                AwfulPost.FORM_KEY      + " VARCHAR,"   + 
                AwfulPost.FORM_COOKIE      + " VARCHAR,"   + 
                AwfulPost.EDIT_POST_ID      + " INTEGER,"   + 
                AwfulMessage.RECIPIENT      + " VARCHAR,"   + 
                AwfulMessage.REPLY_CONTENT      + " VARCHAR," +
                AwfulPost.REPLY_ORIGINAL_CONTENT      + " VARCHAR," +
                AwfulPost.FORM_BOOKMARK        + " VARCHAR,"  +
                AwfulMessage.REPLY_ATTACHMENT      + " VARCHAR," +
                AwfulMessage.EPOC_TIMESTAMP + " INTEGER, " +
            	UPDATED_TIMESTAMP   + " DATETIME);");
            

        }
        
        
        @Override
        public void onUpgrade(SQLiteDatabase aDb, int aOldVersion, int aNewVersion) {
        	switch(aOldVersion){//this switch intentionally falls through!
        	case 16:
                aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
            	createPostTable(aDb);
        	case 17:
                aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_FORUM);
            	createForumTable(aDb);
        	case 18:
        		aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_EMOTES);
        		createEmoteTable(aDb);
        		//fixing my stupid mistake hard-coding the bookmark ID at 2
        		ContentValues forumFix = new ContentValues();
        		forumFix.put(AwfulForum.ID, Constants.USERCP_ID);
        		aDb.update(TABLE_FORUM, forumFix,AwfulForum.ID+"=?", int2StrArray(2));
        		
        		//clear out secondary forum pages to implement forum variable perPage sizes
        		aDb.delete(TABLE_THREADS, AwfulThread.INDEX+">?", int2StrArray(30));
        	case 19:
        		//added regdate to post table
        		//post table is just cache, we can just wipe it
                aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
                createPostTable(aDb);
                
                //added attachment to draft table
                aDb.execSQL("ALTER TABLE "+TABLE_DRAFTS+" ADD COLUMN "+AwfulMessage.REPLY_ATTACHMENT + " VARCHAR");
        	case 20:
        		// Update the threads table
        		aDb.execSQL("ALTER TABLE " + TABLE_THREADS + " ADD COLUMN " + AwfulThread.HAS_VIEWED_THREAD + " INTEGER");
        	case 21:
            	wipeRecreateTables(aDb);//clear cache to resolve remaining blank-forum issue.
        	case 23:
        		//added bookmark setting to draft table
                aDb.execSQL("ALTER TABLE "+TABLE_DRAFTS+" ADD COLUMN "+AwfulPost.FORM_BOOKMARK + " VARCHAR");
        	case 24:
        		aDb.execSQL("ALTER TABLE "+TABLE_THREADS+" ADD COLUMN "+AwfulThread.RATING + " INTEGER");
        	case 25:
                aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_THREADS);
                createThreadTable(aDb);
            case 26:
                aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_DRAFTS);
                createDraftTable(aDb);
            case 27:
                aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_PM);
                createPMTable(aDb);
                    break;//make sure to keep this break statement on the last case of this switch
    		default:
            	wipeRecreateTables(aDb);
        	}
        }
        
        @Override
		public void onDowngrade(SQLiteDatabase aDb, int oldVersion,	int newVersion) {
        	wipeRecreateTables(aDb);
		}

		private void dropAllTables(SQLiteDatabase aDb){
            aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_FORUM);
            aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_THREADS);
            aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
            aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_EMOTES);
            aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_UCP_THREADS);
            aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_PM);
            aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_DRAFTS);
        }
		
		public void wipeRecreateTables(SQLiteDatabase aDb){
			dropAllTables(aDb);
            onCreate(aDb);
		}
    }

    private DatabaseHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new DatabaseHelper(getContext());

        return true;
    }

    @Override
    public String getType(Uri aUri) {
        return null;
    }

    @Override
    public int delete(Uri aUri, String aWhere, String[] aWhereArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String table = null;

		// Be careful when using this! A generic URI will delete all rows in the
		// table.  We want to do this before syncing so that we can easily throw
		// out old data.
        final int match = sUriMatcher.match(aUri);
        switch (match) {
            case FORUM:
                table = TABLE_FORUM;
                break;
            case POST:
                table = TABLE_POSTS;
                break;
            case THREAD:
                table = TABLE_THREADS;
                break;
            case UCP_THREAD:
                table = TABLE_UCP_THREADS;
                break;
			case PM:
				table = TABLE_PM;
				break;
			case DRAFT:
				table = TABLE_DRAFTS;
				break;
			case EMOTE:
				table = TABLE_EMOTES;
				break;
            default:
                break;
        }
        assert(table != null);

        return db.delete(table, aWhere, aWhereArgs);
    }

    @Override
    public int update(Uri aUri, ContentValues aValues, String aWhere, String[] aWhereArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String table = null;

        final int match = sUriMatcher.match(aUri);
        switch (match) {
            case FORUM_ID:
                aWhereArgs = insertSelectionArg(aWhereArgs, aUri.getLastPathSegment());        
                aWhere = AwfulForum.ID + "=?";
            case FORUM:
                table = TABLE_FORUM;
                break;
            case POST_ID:
                aWhereArgs = insertSelectionArg(aWhereArgs, aUri.getLastPathSegment());        
                aWhere = AwfulPost.ID + "=?";
            case POST:
                table = TABLE_POSTS;
                break;
            case THREAD_ID:
                aWhereArgs = insertSelectionArg(aWhereArgs, aUri.getLastPathSegment());        
                aWhere = AwfulThread.ID + "=?";
            case THREAD:
                table = TABLE_THREADS;
                break;
            case UCP_THREAD_ID:
                aWhereArgs = insertSelectionArg(aWhereArgs, aUri.getLastPathSegment());        
                aWhere = AwfulThread.ID + "=?";
            case UCP_THREAD:
                table = TABLE_UCP_THREADS;
                break;
			case PM_ID:
                aWhereArgs = insertSelectionArg(aWhereArgs, aUri.getLastPathSegment());        
                aWhere = AwfulMessage.ID + "=?";
			case PM:
				table = TABLE_PM;
				break;
			case DRAFT_ID:
                aWhereArgs = insertSelectionArg(aWhereArgs, aUri.getLastPathSegment());        
                aWhere = AwfulMessage.ID + "=?";
			case DRAFT:
				table = TABLE_DRAFTS;
				break;
			case EMOTE_ID:
                aWhereArgs = insertSelectionArg(aWhereArgs, aUri.getLastPathSegment());        
                aWhere = AwfulEmote.ID + "=?";
			case EMOTE:
				table = TABLE_EMOTES;
				break;
        }

        int result = db.update(table, aValues, aWhere, aWhereArgs);

		getContext().getContentResolver().notifyChange(aUri, null);

		return result;
    }

	@Override
	public int bulkInsert(Uri aUri, ContentValues[] aValues) {
        String table = null;
		int result = 0;
		String id_row = null;
		String unique_match = null;
		String unique_match2 = null;

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(aUri);
        switch(match) {
            case FORUM:
                table = TABLE_FORUM;
                id_row = AwfulForum.ID;//these ID rows are useless, they're required to be _id, remove this
                break;
            case POST:
                table = TABLE_POSTS;
                id_row = AwfulPost.ID;
                unique_match = AwfulPost.POST_INDEX;
                unique_match2 = AwfulPost.THREAD_ID;
                break;
            case THREAD:
                table = TABLE_THREADS;
                id_row = AwfulThread.ID;
                break;
            case UCP_THREAD:
                table = TABLE_UCP_THREADS;
                id_row = AwfulThread.ID;
                break;
			case PM:
				table = TABLE_PM;
                id_row = AwfulMessage.ID;
				break;
			case DRAFT:
				table = TABLE_DRAFTS;
                id_row = AwfulMessage.ID;
				break;
			case EMOTE:
				table = TABLE_EMOTES;
                id_row = AwfulEmote.ID;
                unique_match = AwfulEmote.TEXT;
				break;
        }

		db.beginTransaction();

		try {
			for (ContentValues value : aValues) {
				if(unique_match != null){
					if(unique_match2 != null){
						db.delete(table, unique_match+"=? AND "+unique_match2+"=?", int2StrArray(value.getAsInteger(unique_match), value.getAsInteger(unique_match2)));
					}else{
						db.delete(table, unique_match+"=?", new String[]{value.getAsString(unique_match)});
					}
				}
				try{
					db.insertOrThrow(table, "", value);
				}catch(SQLException sqle){
					db.update(table, value, id_row+"=?", int2StrArray(value.getAsInteger(id_row)));
				}
				result++;
			}

			db.setTransactionSuccessful();

            if (result > 0) {
                getContext().getContentResolver().notifyChange(aUri, null);
            }
		} catch (SQLiteConstraintException e) {
			Log.i(TAG, e.toString());
		} finally {
			db.endTransaction();
		}

		return result;
	}

    @Override
    public Uri insert(Uri aUri, ContentValues aValues) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String table = null;

        final int match = sUriMatcher.match(aUri);
        switch(match) {
			case FORUM:
				table = TABLE_FORUM;
				break;
			case POST:
				table = TABLE_POSTS;
				break;
			case THREAD:
				table = TABLE_THREADS;
				break;
			case UCP_THREAD:
				table = TABLE_UCP_THREADS;
				break;
			case PM:
				table = TABLE_PM;
				break;
			case DRAFT:
				table = TABLE_DRAFTS;
				break;
			case EMOTE:
				table = TABLE_EMOTES;
				break;
        }

        long rowId = db.insert(table, "", aValues); 
        
        if (rowId > -1) {
            Uri rowUri = ContentUris.withAppendedId(aUri, rowId);

            return rowUri;
        }

        throw new SQLException("Failed to insert row into " + aUri);
    }

    @Override
    public Cursor query(Uri aUri, String[] aProjection, String aSelection,
        String[] aSelectionArgs, String aSortOrder) 
    {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		// Typically this should fetch a readable database but we're querying before
		// we actually add anything, so make it writable.
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        final int match = sUriMatcher.match(aUri);
        switch(match) {
			case FORUM_ID:
                aSelectionArgs = insertSelectionArg(aSelectionArgs, aUri.getLastPathSegment());        
                builder.appendWhere(AwfulForum.ID + "=?");
			case FORUM:
				builder.setTables(TABLE_FORUM);
				builder.setProjectionMap(sForumProjectionMap);
				break;
			case POST_ID:
                aSelectionArgs = insertSelectionArg(aSelectionArgs, aUri.getLastPathSegment());        
                builder.appendWhere(AwfulPost.ID + "=?");
			case POST:
				builder.setTables(TABLE_POSTS);
				builder.setProjectionMap(sPostProjectionMap);
				break;
			case THREAD_ID:
                aSelectionArgs = insertSelectionArg(aSelectionArgs, aUri.getLastPathSegment());        
                builder.appendWhere(TABLE_THREADS+"."+AwfulThread.ID + "=?");
			case THREAD:
				builder.setTables(TABLE_THREADS+" LEFT OUTER JOIN "+TABLE_FORUM+" ON "+TABLE_THREADS+"."+AwfulThread.FORUM_ID+"="+TABLE_FORUM+"."+AwfulForum.ID);
				builder.setProjectionMap(sThreadProjectionMap);
				break;
			case UCP_THREAD_ID:
                aSelectionArgs = insertSelectionArg(aSelectionArgs, aUri.getLastPathSegment());        
                builder.appendWhere(AwfulThread.ID + "=?");
			case UCP_THREAD:
				//hopefully this join works
				builder.setTables(TABLE_UCP_THREADS+", "+TABLE_THREADS+" ON "+TABLE_UCP_THREADS+"."+AwfulThread.ID+"="+TABLE_THREADS+"."+AwfulThread.ID);
				builder.setProjectionMap(sUCPThreadProjectionMap);
				break;
			case PM_ID:
                aSelectionArgs = insertSelectionArg(aSelectionArgs, aUri.getLastPathSegment());        
                builder.appendWhere(TABLE_PM+"."+AwfulMessage.ID + "=?");
			case PM:
				builder.setTables(TABLE_PM+" LEFT OUTER JOIN "+TABLE_DRAFTS+" ON "+TABLE_PM+"."+AwfulMessage.ID+"="+TABLE_DRAFTS+"."+AwfulMessage.ID);
				builder.setProjectionMap(sPMReplyProjectionMap);
				break;
			case DRAFT_ID:
                aSelectionArgs = insertSelectionArg(aSelectionArgs, aUri.getLastPathSegment());        
                builder.appendWhere(AwfulMessage.ID + "=?");
			case DRAFT:
				builder.setTables(TABLE_DRAFTS);
				builder.setProjectionMap(sDraftProjectionMap);
				break;
			case EMOTE_ID:
                aSelectionArgs = insertSelectionArg(aSelectionArgs, aUri.getLastPathSegment());        
                builder.appendWhere(AwfulEmote.ID + "=?");
			case EMOTE:
				builder.setTables(TABLE_EMOTES);
				builder.setProjectionMap(sEmoteProjectionMap);
				break;
        }

        Cursor result = builder.query(db, aProjection, aSelection, 
            aSelectionArgs, null, null, aSortOrder);
        result.setNotificationUri(getContext().getContentResolver(), aUri);

        return result;
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
    
    //the leaning pyramid of boilerplate
    public static String[] int2StrArray(int arg1){
    	return new String[]{Integer.toString(arg1)};
    }
    
    public static String[] int2StrArray(int arg1, int arg2){
    	return new String[]{Integer.toString(arg1), Integer.toString(arg2)};
    }
    
    public static String[] int2StrArray(int arg1, int arg2, int arg3){
    	return new String[]{Integer.toString(arg1),Integer.toString(arg2),Integer.toString(arg3)};
    }
    
    public static String[] int2StrArray(int arg1, int arg2, int arg3, int arg4){
    	return new String[]{Integer.toString(arg1),Integer.toString(arg2),Integer.toString(arg3),Integer.toString(arg4)};
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sForumProjectionMap = new HashMap<String, String>();
        sPostProjectionMap = new HashMap<String, String>();
        sThreadProjectionMap = new HashMap<String, String>();
        sUCPThreadProjectionMap = new HashMap<String, String>();
        sPMProjectionMap = new HashMap<String, String>();
        sDraftProjectionMap = new HashMap<String, String>();
        sPMReplyProjectionMap = new HashMap<String, String>();
        sEmoteProjectionMap = new HashMap<String, String>();

		sUriMatcher.addURI(Constants.AUTHORITY, "forum", FORUM);
		sUriMatcher.addURI(Constants.AUTHORITY, "forum/#", FORUM_ID);
		sUriMatcher.addURI(Constants.AUTHORITY, "thread", THREAD);
		sUriMatcher.addURI(Constants.AUTHORITY, "thread/#", THREAD_ID);
		sUriMatcher.addURI(Constants.AUTHORITY, "post", POST);
		sUriMatcher.addURI(Constants.AUTHORITY, "post/#", POST_ID);
		sUriMatcher.addURI(Constants.AUTHORITY, "ucpthread", UCP_THREAD);
		sUriMatcher.addURI(Constants.AUTHORITY, "ucpthread/#", UCP_THREAD_ID);
		sUriMatcher.addURI(Constants.AUTHORITY, "privatemessages", PM);
		sUriMatcher.addURI(Constants.AUTHORITY, "privatemessages/#", PM_ID);
		sUriMatcher.addURI(Constants.AUTHORITY, "draftreplies", DRAFT);
		sUriMatcher.addURI(Constants.AUTHORITY, "draftreplies/#", DRAFT_ID);
		sUriMatcher.addURI(Constants.AUTHORITY, "emote", EMOTE);
		sUriMatcher.addURI(Constants.AUTHORITY, "emote/#", EMOTE_ID);

		sForumProjectionMap.put(AwfulForum.ID, AwfulForum.ID);
		sForumProjectionMap.put(AwfulForum.PARENT_ID, AwfulForum.PARENT_ID);
		sForumProjectionMap.put(AwfulForum.INDEX, AwfulForum.INDEX);
		sForumProjectionMap.put(AwfulForum.TITLE, AwfulForum.TITLE);
		sForumProjectionMap.put(AwfulForum.SUBTEXT, AwfulForum.SUBTEXT);
		sForumProjectionMap.put(AwfulForum.PAGE_COUNT, AwfulForum.PAGE_COUNT);
		sForumProjectionMap.put(AwfulForum.TAG_URL, AwfulForum.TAG_URL);
		sForumProjectionMap.put(AwfulForum.TAG_CACHEFILE, AwfulForum.TAG_CACHEFILE);
		sForumProjectionMap.put(UPDATED_TIMESTAMP, UPDATED_TIMESTAMP);

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
		sPostProjectionMap.put(AwfulPost.AVATAR, AwfulPost.AVATAR);
		sPostProjectionMap.put(AwfulPost.AVATAR_TEXT, AwfulPost.AVATAR_TEXT);
		sPostProjectionMap.put(AwfulPost.CONTENT, AwfulPost.CONTENT);
		sPostProjectionMap.put(AwfulPost.EDITED, AwfulPost.EDITED);
		
		sThreadProjectionMap.put(AwfulThread.ID, TABLE_THREADS+"."+AwfulThread.ID+" AS "+AwfulThread.ID);
		sThreadProjectionMap.put(AwfulThread.FORUM_ID, AwfulThread.FORUM_ID);
		sThreadProjectionMap.put(AwfulThread.INDEX, AwfulThread.INDEX);
		sThreadProjectionMap.put(AwfulThread.TITLE, TABLE_THREADS+"."+AwfulThread.TITLE+" AS "+AwfulThread.TITLE);
		sThreadProjectionMap.put(AwfulThread.POSTCOUNT, AwfulThread.POSTCOUNT);
		sThreadProjectionMap.put(AwfulThread.UNREADCOUNT, AwfulThread.UNREADCOUNT);
		sThreadProjectionMap.put(AwfulThread.AUTHOR, AwfulThread.AUTHOR);
		sThreadProjectionMap.put(AwfulThread.AUTHOR_ID, AwfulThread.AUTHOR_ID);
		sThreadProjectionMap.put(AwfulThread.LOCKED, AwfulThread.LOCKED);
		sThreadProjectionMap.put(AwfulThread.BOOKMARKED, AwfulThread.BOOKMARKED);
		sThreadProjectionMap.put(AwfulThread.STICKY, AwfulThread.STICKY);
		sThreadProjectionMap.put(AwfulThread.CATEGORY, AwfulThread.CATEGORY);
		sThreadProjectionMap.put(AwfulThread.LASTPOSTER, AwfulThread.LASTPOSTER);
		sThreadProjectionMap.put(AwfulThread.HAS_NEW_POSTS, AwfulThread.UNREADCOUNT+" > 0 AS "+ AwfulThread.HAS_NEW_POSTS);
		sThreadProjectionMap.put(AwfulThread.HAS_VIEWED_THREAD, AwfulThread.HAS_VIEWED_THREAD);
        sThreadProjectionMap.put(AwfulThread.ARCHIVED, AwfulThread.ARCHIVED);
        sThreadProjectionMap.put(AwfulThread.RATING, AwfulThread.RATING);
		sThreadProjectionMap.put(AwfulThread.TAG_URL, TABLE_THREADS+"."+AwfulThread.TAG_URL+" AS "+AwfulThread.TAG_URL);
		sThreadProjectionMap.put(AwfulThread.TAG_CACHEFILE, TABLE_THREADS+"."+AwfulThread.TAG_CACHEFILE+" AS "+AwfulThread.TAG_CACHEFILE);
		sThreadProjectionMap.put(AwfulThread.FORUM_TITLE, TABLE_FORUM+"."+AwfulForum.TITLE+" AS "+AwfulThread.FORUM_TITLE);
		sThreadProjectionMap.put(UPDATED_TIMESTAMP, TABLE_THREADS+"."+UPDATED_TIMESTAMP+" AS "+UPDATED_TIMESTAMP);
		
		
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
		sUCPThreadProjectionMap.put(AwfulThread.BOOKMARKED, AwfulThread.BOOKMARKED);
		sUCPThreadProjectionMap.put(AwfulThread.STICKY, AwfulThread.STICKY);
		sUCPThreadProjectionMap.put(AwfulThread.CATEGORY, AwfulThread.CATEGORY);
		sUCPThreadProjectionMap.put(AwfulThread.LASTPOSTER, AwfulThread.LASTPOSTER);
		sUCPThreadProjectionMap.put(AwfulThread.TAG_URL, AwfulThread.TAG_URL);
		sUCPThreadProjectionMap.put(AwfulThread.TAG_CACHEFILE, AwfulThread.TAG_CACHEFILE);
		sUCPThreadProjectionMap.put(AwfulThread.HAS_NEW_POSTS, AwfulThread.UNREADCOUNT+" > 0 AS "+AwfulThread.HAS_NEW_POSTS);
		sUCPThreadProjectionMap.put(AwfulThread.HAS_VIEWED_THREAD, AwfulThread.HAS_VIEWED_THREAD);
        sUCPThreadProjectionMap.put(AwfulThread.ARCHIVED, AwfulThread.ARCHIVED);
        sUCPThreadProjectionMap.put(AwfulThread.RATING, AwfulThread.RATING);
		sUCPThreadProjectionMap.put(AwfulThread.FORUM_TITLE, "null");
		sUCPThreadProjectionMap.put(UPDATED_TIMESTAMP, TABLE_UCP_THREADS+"."+UPDATED_TIMESTAMP+" AS "+UPDATED_TIMESTAMP);
		
		sPMProjectionMap.put(AwfulMessage.ID, AwfulMessage.ID);
		sPMProjectionMap.put(AwfulMessage.TITLE, AwfulMessage.TITLE);
		sPMProjectionMap.put(AwfulMessage.CONTENT, AwfulMessage.CONTENT);
		sPMProjectionMap.put(AwfulMessage.AUTHOR, AwfulMessage.AUTHOR);
		sPMProjectionMap.put(AwfulMessage.DATE, AwfulMessage.DATE);
		sPMProjectionMap.put(AwfulMessage.UNREAD, AwfulMessage.UNREAD);
		
		sDraftProjectionMap.put(AwfulMessage.ID, AwfulMessage.ID);
		sDraftProjectionMap.put(AwfulMessage.TITLE, AwfulMessage.TITLE);
		sDraftProjectionMap.put(AwfulPost.FORM_COOKIE, AwfulPost.FORM_COOKIE);
		sDraftProjectionMap.put(AwfulPost.FORM_KEY, AwfulPost.FORM_KEY);
		sDraftProjectionMap.put(AwfulMessage.REPLY_CONTENT, AwfulMessage.REPLY_CONTENT);
		sDraftProjectionMap.put(AwfulMessage.RECIPIENT, AwfulMessage.RECIPIENT);
		sDraftProjectionMap.put(AwfulMessage.TYPE, AwfulMessage.TYPE);
		sDraftProjectionMap.put(AwfulPost.EDIT_POST_ID, AwfulPost.EDIT_POST_ID);
		sDraftProjectionMap.put(AwfulPost.REPLY_ORIGINAL_CONTENT, AwfulPost.REPLY_ORIGINAL_CONTENT);
		sDraftProjectionMap.put(AwfulMessage.REPLY_ATTACHMENT, AwfulMessage.REPLY_ATTACHMENT);
		sDraftProjectionMap.put(AwfulPost.FORM_BOOKMARK, AwfulPost.FORM_BOOKMARK);
        sDraftProjectionMap.put(AwfulMessage.EPOC_TIMESTAMP, AwfulMessage.EPOC_TIMESTAMP);
		sDraftProjectionMap.put(UPDATED_TIMESTAMP, UPDATED_TIMESTAMP);
		
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
		sPMReplyProjectionMap.put(AwfulMessage.FOLDER, AwfulMessage.FOLDER);
		
		sEmoteProjectionMap.put(AwfulEmote.ID, AwfulEmote.ID);
		sEmoteProjectionMap.put(AwfulEmote.TEXT, AwfulEmote.TEXT);
		sEmoteProjectionMap.put(AwfulEmote.SUBTEXT, AwfulEmote.SUBTEXT);
		sEmoteProjectionMap.put(AwfulEmote.URL, AwfulEmote.URL);
		sEmoteProjectionMap.put(AwfulEmote.INDEX, AwfulEmote.INDEX);
		sEmoteProjectionMap.put(UPDATED_TIMESTAMP, UPDATED_TIMESTAMP);
    }
}
