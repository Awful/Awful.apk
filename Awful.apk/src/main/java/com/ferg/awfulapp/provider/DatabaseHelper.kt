package com.ferg.awfulapp.provider

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.ferg.awfulapp.thread.AwfulEmote
import com.ferg.awfulapp.thread.AwfulForum
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.thread.AwfulPost
import com.ferg.awfulapp.thread.AwfulThread

/**
 * Created by baka kaba on 06/05/2017.
 * 
 * Manages the app database, handling initialisation and version changes.
 * Extracted from [AwfulProvider].
 */
class DatabaseHelper internal constructor(aContext: Context?) :
    SQLiteOpenHelper(aContext, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(aDb: SQLiteDatabase) {
        createForumTable(aDb)
        createThreadTable(aDb)
        createUCPTable(aDb)
        createPostTable(aDb)
        createEmoteTable(aDb)
        createPMTable(aDb)
        createDraftTable(aDb)
        createThreadDraftTable(aDb)
    }


    private fun createForumTable(aDb: SQLiteDatabase) {
        aDb.execSQL(
            "CREATE TABLE " + TABLE_FORUM + " (" +
                    AwfulForum.ID + " INTEGER UNIQUE," +
                    AwfulForum.PARENT_ID + " INTEGER," +  //subforums list parent forum id, primary forums list 0 (index)
                    AwfulForum.INDEX + " INTEGER," +
                    AwfulForum.TITLE + " VARCHAR," +
                    AwfulForum.SUBTEXT + " VARCHAR," +
                    AwfulForum.PAGE_COUNT + " INTEGER," +
                    AwfulForum.TAG_URL + " VARCHAR," +
                    AwfulForum.TAG_CACHEFILE + " VARCHAR," +
                    UPDATED_TIMESTAMP + " DATETIME);"
        )
    }

    private fun createThreadTable(aDb: SQLiteDatabase) {
        aDb.execSQL(
            "CREATE TABLE " + TABLE_THREADS + " (" +
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
                    AwfulThread.LAST_POST_DATE + " INTEGER, " +
                    UPDATED_TIMESTAMP + " DATETIME);"
        )
    }

    private fun createUCPTable(aDb: SQLiteDatabase) {
        aDb.execSQL(
            "CREATE TABLE " + TABLE_UCP_THREADS + " (" +
                    AwfulThread.ID + " INTEGER UNIQUE," +  //to be joined with thread table
                    AwfulThread.INDEX + " INTEGER," +
                    UPDATED_TIMESTAMP + " DATETIME);"
        )
    }

    private fun createPostTable(aDb: SQLiteDatabase) {
        aDb.execSQL(
            "CREATE TABLE " + TABLE_POSTS + " (" +
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
                    AwfulPost.HAS_MOD_CONTROLS + " INTEGER," +
                    AwfulPost.IS_OP + " INTEGER," +
                    AwfulPost.IS_PLAT + " INTEGER," +
                    AwfulPost.ROLE + " VARCHAR," +
                    AwfulPost.ICON + " VARCHAR," +
                    AwfulPost.AVATAR + " VARCHAR," +
                    AwfulPost.AVATAR_SECOND + " VARCHAR," +
                    AwfulPost.AVATAR_TEXT + " VARCHAR," +
                    AwfulPost.CONTENT + " VARCHAR," +
                    AwfulPost.EDITED + " VARCHAR," +
                    UPDATED_TIMESTAMP + " DATETIME);"
        )
    }

    private fun createEmoteTable(aDb: SQLiteDatabase) {
        aDb.execSQL(
            "CREATE TABLE " + TABLE_EMOTES + " (" +
                    AwfulEmote.ID + " INTEGER UNIQUE," +
                    AwfulEmote.TEXT + " VARCHAR," +
                    AwfulEmote.SUBTEXT + " VARCHAR," +
                    AwfulEmote.URL + " VARCHAR," +
                    AwfulEmote.INDEX + " INTEGER," +
                    UPDATED_TIMESTAMP + " DATETIME);"
        )
    }

    private fun createPMTable(aDb: SQLiteDatabase) {
        aDb.execSQL(
            "CREATE TABLE " + TABLE_PM + " (" +
                    AwfulMessage.ID + " INTEGER UNIQUE," +
                    AwfulMessage.TITLE + " VARCHAR," +
                    AwfulMessage.AUTHOR + " VARCHAR," +
                    AwfulMessage.CONTENT + " VARCHAR," +
                    AwfulMessage.UNREAD + " INTEGER," +
                    AwfulMessage.FOLDER + " INTEGER," +
                    AwfulMessage.ICON + " VARCHAR," +
                    AwfulMessage.DATE + " VARCHAR," +
                    UPDATED_TIMESTAMP + " DATETIME);"
        )
    }

    private fun createDraftTable(aDb: SQLiteDatabase) {
        aDb.execSQL(
            "CREATE TABLE " + TABLE_DRAFTS + " (" +
                    AwfulMessage.ID + " INTEGER UNIQUE," +
                    AwfulMessage.TYPE + " INTEGER," +
                    AwfulMessage.TITLE + " VARCHAR," +
                    AwfulPost.FORM_KEY + " VARCHAR," +
                    AwfulPost.FORM_COOKIE + " VARCHAR," +
                    AwfulPost.EDIT_POST_ID + " INTEGER," +
                    AwfulMessage.RECIPIENT + " VARCHAR," +
                    AwfulMessage.REPLY_CONTENT + " VARCHAR," +
                    AwfulMessage.REPLY_ICON + " VARCHAR," +
                    AwfulPost.REPLY_ORIGINAL_CONTENT + " VARCHAR," +
                    AwfulPost.FORM_BOOKMARK + " VARCHAR," +
                    AwfulMessage.REPLY_ATTACHMENT + " VARCHAR," +
                    AwfulMessage.EPOC_TIMESTAMP + " INTEGER, " +
                    UPDATED_TIMESTAMP + " DATETIME);"
        )
    }

    private fun createThreadDraftTable(aDb: SQLiteDatabase) {
        aDb.execSQL(
            "CREATE TABLE " + TABLE_THREAD_DRAFTS + " (" +
                    AwfulMessage.ID + " INTEGER UNIQUE," +
                    AwfulPost.FORM_KEY + " VARCHAR," +
                    AwfulPost.FORM_COOKIE + " VARCHAR," +
                    AwfulMessage.POST_CONTENT + " VARCHAR," +
                    AwfulMessage.POST_SUBJECT + " VARCHAR," +
                    AwfulMessage.POST_ICON_ID + " VARCHAR," +
                    AwfulMessage.POST_ICON_URL + " VARCHAR," +
                    AwfulPost.FORM_BOOKMARK + " VARCHAR," +
                    AwfulMessage.REPLY_ATTACHMENT + " VARCHAR," +
                    AwfulMessage.EPOC_TIMESTAMP + " INTEGER, " +
                    UPDATED_TIMESTAMP + " DATETIME);"
        )
    }


    override fun onUpgrade(aDb: SQLiteDatabase, aOldVersion: Int, aNewVersion: Int) {
        when (aOldVersion) {
            23, 24, 25, 26 -> {
                dropTables(aDb, TABLE_DRAFTS)
                createDraftTable(aDb)
                dropTables(aDb, TABLE_PM, TABLE_POSTS)
                createPMTable(aDb)
                createPostTable(aDb)
                dropTables(aDb, TABLE_FORUM)
                createForumTable(aDb)
                dropTables(aDb, TABLE_THREADS)
                createThreadTable(aDb)
                dropTables(aDb, TABLE_DRAFTS)
                createDraftTable(aDb)
                dropTables(aDb, TABLE_POSTS)
                createPostTable(aDb)
                dropTables(aDb, TABLE_THREAD_DRAFTS)
                createThreadDraftTable(aDb)
                dropTables(aDb, TABLE_POSTS)
                createPostTable(aDb)
                aDb.execSQL("ALTER TABLE " + TABLE_THREADS + " ADD COLUMN " + AwfulThread.LAST_POST_DATE + " INTEGER DEFAULT 0")
            }

            27, 28, 29 -> {
                dropTables(aDb, TABLE_PM, TABLE_POSTS)
                createPMTable(aDb)
                createPostTable(aDb)
                dropTables(aDb, TABLE_FORUM)
                createForumTable(aDb)
                dropTables(aDb, TABLE_THREADS)
                createThreadTable(aDb)
                dropTables(aDb, TABLE_DRAFTS)
                createDraftTable(aDb)
                dropTables(aDb, TABLE_POSTS)
                createPostTable(aDb)
                dropTables(aDb, TABLE_THREAD_DRAFTS)
                createThreadDraftTable(aDb)
                dropTables(aDb, TABLE_POSTS)
                createPostTable(aDb)
                aDb.execSQL("ALTER TABLE " + TABLE_THREADS + " ADD COLUMN " + AwfulThread.LAST_POST_DATE + " INTEGER DEFAULT 0")
            }

            30 -> {
                dropTables(aDb, TABLE_FORUM)
                createForumTable(aDb)
                dropTables(aDb, TABLE_THREADS)
                createThreadTable(aDb)
                dropTables(aDb, TABLE_DRAFTS)
                createDraftTable(aDb)
                dropTables(aDb, TABLE_POSTS)
                createPostTable(aDb)
                dropTables(aDb, TABLE_THREAD_DRAFTS)
                createThreadDraftTable(aDb)
                dropTables(aDb, TABLE_POSTS)
                createPostTable(aDb)
                aDb.execSQL("ALTER TABLE " + TABLE_THREADS + " ADD COLUMN " + AwfulThread.LAST_POST_DATE + " INTEGER DEFAULT 0")
            }

            31 -> {
                dropTables(aDb, TABLE_THREADS)
                createThreadTable(aDb)
                dropTables(aDb, TABLE_DRAFTS)
                createDraftTable(aDb)
                dropTables(aDb, TABLE_POSTS)
                createPostTable(aDb)
                dropTables(aDb, TABLE_THREAD_DRAFTS)
                createThreadDraftTable(aDb)
                dropTables(aDb, TABLE_POSTS)
                createPostTable(aDb)
                aDb.execSQL("ALTER TABLE " + TABLE_THREADS + " ADD COLUMN " + AwfulThread.LAST_POST_DATE + " INTEGER DEFAULT 0")
            }

            32 -> {
                dropTables(aDb, TABLE_DRAFTS)
                createDraftTable(aDb)
                dropTables(aDb, TABLE_POSTS)
                createPostTable(aDb)
                dropTables(aDb, TABLE_THREAD_DRAFTS)
                createThreadDraftTable(aDb)
                dropTables(aDb, TABLE_POSTS)
                createPostTable(aDb)
                aDb.execSQL("ALTER TABLE " + TABLE_THREADS + " ADD COLUMN " + AwfulThread.LAST_POST_DATE + " INTEGER DEFAULT 0")
            }

            33, 34, 35 -> {
                dropTables(aDb, TABLE_POSTS)
                createPostTable(aDb)
                dropTables(aDb, TABLE_THREAD_DRAFTS)
                createThreadDraftTable(aDb)
                dropTables(aDb, TABLE_POSTS)
                createPostTable(aDb)
                aDb.execSQL("ALTER TABLE " + TABLE_THREADS + " ADD COLUMN " + AwfulThread.LAST_POST_DATE + " INTEGER DEFAULT 0")
            }

            36 -> {
                dropTables(aDb, TABLE_THREAD_DRAFTS)
                createThreadDraftTable(aDb)
                dropTables(aDb, TABLE_POSTS)
                createPostTable(aDb)
                aDb.execSQL("ALTER TABLE " + TABLE_THREADS + " ADD COLUMN " + AwfulThread.LAST_POST_DATE + " INTEGER DEFAULT 0")
            }

            37 -> {
                dropTables(aDb, TABLE_POSTS)
                createPostTable(aDb)
                aDb.execSQL("ALTER TABLE " + TABLE_THREADS + " ADD COLUMN " + AwfulThread.LAST_POST_DATE + " INTEGER DEFAULT 0")
            }

            38 -> aDb.execSQL("ALTER TABLE " + TABLE_THREADS + " ADD COLUMN " + AwfulThread.LAST_POST_DATE + " INTEGER DEFAULT 0")
            39 -> aDb.execSQL("ALTER TABLE " + TABLE_POSTS + " ADD COLUMN " + AwfulPost.HAS_MOD_CONTROLS + " INTEGER DEFAULT 0");
            else -> wipeRecreateTables(aDb)
        }
    }

    override fun onDowngrade(aDb: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        wipeRecreateTables(aDb)
    }

    /**
     * Attempt to drop the named tables in the given database
     */
    private fun dropTables(db: SQLiteDatabase, vararg tableNames: String) {
        for (table in tableNames) {
            db.execSQL("DROP TABLE IF EXISTS $table")
        }
    }

    private fun wipeRecreateTables(aDb: SQLiteDatabase) {
        val allTables = arrayOf(
            TABLE_FORUM,
            TABLE_THREADS,
            TABLE_POSTS,
            TABLE_EMOTES,
            TABLE_UCP_THREADS,
            TABLE_PM,
            TABLE_DRAFTS,
            TABLE_THREAD_DRAFTS
        )
        dropTables(aDb, *allTables)
        onCreate(aDb)
    }

    companion object {
        private const val DATABASE_NAME = "awful.db"
        private const val DATABASE_VERSION = 40

        const val TABLE_FORUM: String = "forum"
        const val TABLE_THREADS: String = "threads"

        // TODO: 06/05/2017 this is only public because a fragment is building selection arguments - move that out of there!
        const val TABLE_UCP_THREADS: String = "ucp_thread"
        const val TABLE_POSTS: String = "posts"
        const val TABLE_EMOTES: String = "emotes"
        const val TABLE_PM: String = "private_messages"
        const val TABLE_DRAFTS: String = "draft_messages"
        const val TABLE_THREAD_DRAFTS: String = "draft_threads"

        const val UPDATED_TIMESTAMP: String = "timestamp_row_update"
    }
}
