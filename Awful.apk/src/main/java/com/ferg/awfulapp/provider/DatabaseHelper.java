package com.ferg.awfulapp.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;

import com.ferg.awfulapp.thread.AwfulEmote;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.thread.AwfulMessage;
import com.ferg.awfulapp.thread.AwfulPost;
import com.ferg.awfulapp.thread.AwfulThread;

/**
 * Created by baka kaba on 06/05/2017.
 *
 * Manages the app database, handling initialisation and version changes.
 * Extracted from {@link AwfulProvider}.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "awful.db";
    private static final int DATABASE_VERSION = 36;

    static final String TABLE_FORUM    = "forum";
    static final String TABLE_THREADS    = "threads";
    // TODO: 06/05/2017 this is only public because a fragment is building selection arguments - move that out of there!
    public static final String TABLE_UCP_THREADS    = "ucp_thread";
    static final String TABLE_POSTS    = "posts";
    static final String TABLE_EMOTES    = "emotes";
    static final String TABLE_PM    = "private_messages";
    static final String TABLE_DRAFTS    = "draft_messages";

    public static final String UPDATED_TIMESTAMP    = "timestamp_row_update";

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


    private void createForumTable(SQLiteDatabase aDb) {
        aDb.execSQL("CREATE TABLE " + TABLE_FORUM + " (" +
                AwfulForum.ID + " INTEGER UNIQUE," +
                AwfulForum.PARENT_ID + " INTEGER," + //subforums list parent forum id, primary forums list 0 (index)
                AwfulForum.INDEX + " INTEGER," +
                AwfulForum.TITLE + " VARCHAR," +
                AwfulForum.SUBTEXT + " VARCHAR," +
                AwfulForum.PAGE_COUNT + " INTEGER," +
                AwfulForum.TAG_URL + " VARCHAR," +
                AwfulForum.TAG_CACHEFILE + " VARCHAR," +
                UPDATED_TIMESTAMP + " DATETIME);");
    }

    private void createThreadTable(SQLiteDatabase aDb) {
        aDb.execSQL("CREATE TABLE " + TABLE_THREADS + " (" +
                AwfulThread.ID + " INTEGER UNIQUE," +
                AwfulThread.FORUM_ID + " INTEGER," +
                AwfulThread.INDEX + " INTEGER," +
                AwfulThread.TITLE + " VARCHAR," +
                AwfulThread.POSTCOUNT + " INTEGER," +
                AwfulThread.UNREADCOUNT + " INTEGER," +
                AwfulThread.AUTHOR + " VARCHAR," +
                AwfulThread.AUTHOR_ID + " INTEGER," +
                AwfulThread.LOCKED + " INTEGER," +
                AwfulThread.CAN_OPEN_CLOSE + " INTEGER," +
                AwfulThread.BOOKMARKED + " INTEGER," +
                AwfulThread.STICKY + " INTEGER," +
                AwfulThread.CATEGORY + " INTEGER," +
                AwfulThread.LASTPOSTER + " VARCHAR," +
                AwfulThread.TAG_URL + " VARCHAR," +
                AwfulThread.TAG_CACHEFILE + " VARCHAR," +
                AwfulThread.TAG_EXTRA + " INTEGER, " +
                AwfulThread.HAS_VIEWED_THREAD + " INTEGER, " +
                AwfulThread.ARCHIVED + " INTEGER, " +
                AwfulThread.RATING + " INTEGER, " +
                UPDATED_TIMESTAMP + " DATETIME);");
    }

    private void createUCPTable(SQLiteDatabase aDb) {
        aDb.execSQL("CREATE TABLE " + TABLE_UCP_THREADS + " (" +
                AwfulThread.ID + " INTEGER UNIQUE," + //to be joined with thread table
                AwfulThread.INDEX + " INTEGER," +
                UPDATED_TIMESTAMP + " DATETIME);");
    }

    private void createPostTable(SQLiteDatabase aDb) {
        aDb.execSQL("CREATE TABLE " + TABLE_POSTS + " (" +
                AwfulPost.ID + " INTEGER UNIQUE," +
                AwfulPost.THREAD_ID + " INTEGER," +
                AwfulPost.POST_INDEX + " INTEGER," +
                AwfulPost.DATE + " VARCHAR," +
                AwfulPost.REGDATE + " VARCHAR," +
                AwfulPost.USER_ID + " INTEGER," +
                AwfulPost.USERNAME + " VARCHAR," +
                AwfulPost.IS_IGNORED + " INTEGER," +
                AwfulPost.PREVIOUSLY_READ + " INTEGER," +
                AwfulPost.EDITABLE + " INTEGER," +
                AwfulPost.IS_OP + " INTEGER," +
                AwfulPost.IS_PLAT + " INTEGER," +
                AwfulPost.ROLE + " VARCHAR," +
                AwfulPost.AVATAR + " VARCHAR," +
                AwfulPost.AVATAR_SECOND + " VARCHAR," +
                AwfulPost.AVATAR_TEXT + " VARCHAR," +
                AwfulPost.CONTENT + " VARCHAR," +
                AwfulPost.EDITED + " VARCHAR," +
                UPDATED_TIMESTAMP + " DATETIME);");
    }

    private void createEmoteTable(SQLiteDatabase aDb) {
        aDb.execSQL("CREATE TABLE " + TABLE_EMOTES + " (" +
                AwfulEmote.ID + " INTEGER UNIQUE," +
                AwfulEmote.TEXT + " VARCHAR," +
                AwfulEmote.SUBTEXT + " VARCHAR," +
                AwfulEmote.URL + " VARCHAR," +
                AwfulEmote.INDEX + " INTEGER," +
                UPDATED_TIMESTAMP + " DATETIME);");
    }

    private void createPMTable(SQLiteDatabase aDb) {
        aDb.execSQL("CREATE TABLE " + TABLE_PM + " (" +
                AwfulMessage.ID + " INTEGER UNIQUE," +
                AwfulMessage.TITLE + " VARCHAR," +
                AwfulMessage.AUTHOR + " VARCHAR," +
                AwfulMessage.CONTENT + " VARCHAR," +
                AwfulMessage.UNREAD + " INTEGER," +
                AwfulMessage.FOLDER + " INTEGER," +
                AwfulMessage.ICON + " VARCHAR," +
                AwfulMessage.DATE + " VARCHAR," +
                UPDATED_TIMESTAMP + " DATETIME);");
    }

    private void createDraftTable(SQLiteDatabase aDb) {
        aDb.execSQL("CREATE TABLE " + TABLE_DRAFTS + " (" +
                AwfulMessage.ID + " INTEGER UNIQUE," +
                AwfulMessage.TYPE + " INTEGER," +
                AwfulMessage.TITLE + " VARCHAR," +
                AwfulPost.FORM_KEY + " VARCHAR," +
                AwfulPost.FORM_COOKIE + " VARCHAR," +
                AwfulPost.EDIT_POST_ID + " INTEGER," +
                AwfulMessage.RECIPIENT      + " VARCHAR,"   +
                AwfulMessage.REPLY_CONTENT      + " VARCHAR," +
                AwfulMessage.REPLY_ICON      + " VARCHAR," +
                AwfulPost.REPLY_ORIGINAL_CONTENT + " VARCHAR," +
                AwfulPost.FORM_BOOKMARK + " VARCHAR," +
                AwfulMessage.REPLY_ATTACHMENT + " VARCHAR," +
                AwfulMessage.EPOC_TIMESTAMP + " INTEGER, " +
                UPDATED_TIMESTAMP + " DATETIME);");
    }


    @Override
    public void onUpgrade(SQLiteDatabase aDb, int aOldVersion, int aNewVersion) {
        switch (aOldVersion) {//this switch intentionally falls through!
            case 23:
            case 24:
            case 25:
            case 26:
                dropTables(aDb, TABLE_DRAFTS);
                createDraftTable(aDb);
            case 27:
            case 28:
            case 29:
                dropTables(aDb, TABLE_PM, TABLE_POSTS);
                createPMTable(aDb);
                createPostTable(aDb);
            case 30:
                dropTables(aDb, TABLE_FORUM);
                createForumTable(aDb);
            case 31:
                dropTables(aDb, TABLE_THREADS);
                createThreadTable(aDb);
            case 32:
                dropTables(aDb, TABLE_DRAFTS);
                createDraftTable(aDb);
            case 33:
            case 34:
            case 35:
                dropTables(aDb, TABLE_POSTS);
                createPostTable(aDb);
                break;//make sure to keep this break statement on the last case of this switch
            default:
                wipeRecreateTables(aDb);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase aDb, int oldVersion, int newVersion) {
        wipeRecreateTables(aDb);
    }

    /**
     * Attempt to drop the named tables in the given database
     */
    private void dropTables(@NonNull SQLiteDatabase db, @NonNull String... tableNames) {
        for (String table : tableNames) {
            db.execSQL("DROP TABLE IF EXISTS " + table);
        }
    }

    private void wipeRecreateTables(SQLiteDatabase aDb) {
        String[] allTables = {TABLE_FORUM, TABLE_THREADS, TABLE_POSTS, TABLE_EMOTES, TABLE_UCP_THREADS, TABLE_PM, TABLE_DRAFTS};
        dropTables(aDb, allTables);
        onCreate(aDb);
    }
}
