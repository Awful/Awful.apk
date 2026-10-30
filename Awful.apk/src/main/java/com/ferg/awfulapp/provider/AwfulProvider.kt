/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the software nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
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
 */
package com.ferg.awfulapp.provider

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.util.Log
import androidx.core.database.sqlite.transaction
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.thread.AwfulEmote
import com.ferg.awfulapp.thread.AwfulForum
import com.ferg.awfulapp.thread.AwfulMessage
import com.ferg.awfulapp.thread.AwfulPost
import com.ferg.awfulapp.thread.AwfulThread

class AwfulProvider : ContentProvider() {
    companion object {
    private const val TAG = "AwfulProvider"

    /////////////////////////////////////////////////////////////////////////
    // Matching Uris to types
    /////////////////////////////////////////////////////////////////////////
    private const val URI_FORUM = 0
    private const val URI_FORUM_ID = 1
    private const val URI_POST = 2
    private const val URI_POST_ID = 3
    private const val URI_THREAD = 4
    private const val URI_THREAD_ID = 5
    private const val URI_UCP_THREAD = 6
    private const val URI_UCP_THREAD_ID = 7
    private const val URI_PM = 8
    private const val URI_PM_ID = 9
    private const val URI_DRAFT = 10
    private const val URI_DRAFT_ID = 11
    private const val URI_EMOTE = 12
    private const val URI_EMOTE_ID = 13
    private const val URI_THREAD_DRAFT = 14
    private const val URI_THREAD_DRAFT_ID = 15

    /** This just holds the Uri types that directly refer to tables, not IDs  */
    private val TABLE_URIS: MutableSet<Int> = HashSet(
        listOf(
            URI_FORUM,
            URI_POST,
            URI_THREAD,
            URI_UCP_THREAD,
            URI_PM,
            URI_DRAFT,
            URI_EMOTE,
            URI_THREAD_DRAFT
        )
    )

    private val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(Constants.AUTHORITY, "forum", URI_FORUM)
        addURI(Constants.AUTHORITY, "forum/#", URI_FORUM_ID)
        addURI(Constants.AUTHORITY, "thread", URI_THREAD)
        addURI(Constants.AUTHORITY, "thread/#", URI_THREAD_ID)
        addURI(Constants.AUTHORITY, "post", URI_POST)
        addURI(Constants.AUTHORITY, "post/#", URI_POST_ID)
        addURI(Constants.AUTHORITY, "ucpthread", URI_UCP_THREAD)
        addURI(Constants.AUTHORITY, "ucpthread/#", URI_UCP_THREAD_ID)
        addURI(Constants.AUTHORITY, "privatemessages", URI_PM)
        addURI(Constants.AUTHORITY, "privatemessages/#", URI_PM_ID)
        addURI(Constants.AUTHORITY, "draftreplies", URI_DRAFT)
        addURI(Constants.AUTHORITY, "draftreplies/#", URI_DRAFT_ID)
        addURI(Constants.AUTHORITY, "emote", URI_EMOTE)
        addURI(Constants.AUTHORITY, "emote/#", URI_EMOTE_ID)
        addURI(Constants.AUTHORITY, "draftthreads", URI_THREAD_DRAFT)
        addURI(Constants.AUTHORITY, "draftthreads/#", URI_THREAD_DRAFT_ID)
    }


    ////////////////////////////////////////////////////////////////////////
    // Projections
    /////////////////////////////////////////////////////////////////////////
    // TODO: 06/05/2017 some of these maps are broken into multiple basic projections, and some are almost the full map. Might be worth checking if this is still right
    private fun arrayOfKeys(map: MutableMap<String, *>): Array<String> {
        return map.keys.toTypedArray<String>()
    }

    // Forum
    private val sForumProjectionMap = hashMapOf(
        AwfulForum.ID to AwfulForum.ID,
        AwfulForum.PARENT_ID to AwfulForum.PARENT_ID,
        AwfulForum.INDEX to AwfulForum.INDEX,
        AwfulForum.TITLE to AwfulForum.TITLE,
        AwfulForum.SUBTEXT to AwfulForum.SUBTEXT,
        AwfulForum.PAGE_COUNT to AwfulForum.PAGE_COUNT,
        AwfulForum.TAG_URL to AwfulForum.TAG_URL,
        AwfulForum.TAG_CACHEFILE to AwfulForum.TAG_CACHEFILE,
        DatabaseHelper.UPDATED_TIMESTAMP to DatabaseHelper.UPDATED_TIMESTAMP
    )

    @JvmField
    val ForumProjection: Array<String> = arrayOfKeys(sForumProjectionMap)

    // Thread
    private val sThreadProjectionMap = hashMapOf(
        AwfulThread.ID to "${DatabaseHelper.TABLE_THREADS}.${AwfulThread.ID} AS ${AwfulThread.ID}",
        AwfulThread.FORUM_ID to AwfulThread.FORUM_ID,
        AwfulThread.INDEX to AwfulThread.INDEX,
        AwfulThread.TITLE to "${DatabaseHelper.TABLE_THREADS}.${AwfulThread.TITLE} AS ${AwfulThread.TITLE}",
        AwfulThread.POSTCOUNT to AwfulThread.POSTCOUNT,
        AwfulThread.UNREADCOUNT to AwfulThread.UNREADCOUNT,
        AwfulThread.AUTHOR to AwfulThread.AUTHOR,
        AwfulThread.AUTHOR_ID to AwfulThread.AUTHOR_ID,
        AwfulThread.LOCKED to AwfulThread.LOCKED,
        AwfulThread.CAN_OPEN_CLOSE to AwfulThread.CAN_OPEN_CLOSE,
        AwfulThread.BOOKMARKED to AwfulThread.BOOKMARKED,
        AwfulThread.STICKY to AwfulThread.STICKY,
        AwfulThread.CATEGORY to AwfulThread.CATEGORY,
        AwfulThread.LASTPOSTER to AwfulThread.LASTPOSTER,
        AwfulThread.LAST_POST_DATE to AwfulThread.LAST_POST_DATE,
        AwfulThread.HAS_NEW_POSTS to "${AwfulThread.UNREADCOUNT} > 0 AS ${AwfulThread.HAS_NEW_POSTS}",
        AwfulThread.HAS_VIEWED_THREAD to AwfulThread.HAS_VIEWED_THREAD,
        AwfulThread.ARCHIVED to AwfulThread.ARCHIVED,
        AwfulThread.RATING to AwfulThread.RATING,
        AwfulThread.TAG_URL to "${DatabaseHelper.TABLE_THREADS}.${AwfulThread.TAG_URL} AS ${AwfulThread.TAG_URL}",
        AwfulThread.TAG_EXTRA to "${DatabaseHelper.TABLE_THREADS}.${AwfulThread.TAG_EXTRA} AS ${AwfulThread.TAG_EXTRA}",
        AwfulThread.TAG_CACHEFILE to "${DatabaseHelper.TABLE_THREADS}.${AwfulThread.TAG_CACHEFILE} AS ${AwfulThread.TAG_CACHEFILE}",
        AwfulThread.FORUM_TITLE to "${DatabaseHelper.TABLE_FORUM}.${AwfulForum.TITLE} AS ${AwfulThread.FORUM_TITLE}",
        DatabaseHelper.UPDATED_TIMESTAMP to "${DatabaseHelper.TABLE_THREADS}.${DatabaseHelper.UPDATED_TIMESTAMP} AS ${DatabaseHelper.UPDATED_TIMESTAMP}"
    )

    @JvmField
    val ThreadProjection: Array<String> = arrayOfKeys(sThreadProjectionMap)

    // Post
    private val sPostProjectionMap = hashMapOf(
        AwfulPost.ID to AwfulPost.ID,
        AwfulPost.THREAD_ID to AwfulPost.THREAD_ID,
        AwfulPost.POST_INDEX to AwfulPost.POST_INDEX,
        AwfulPost.DATE to AwfulPost.DATE,
        AwfulPost.REGDATE to AwfulPost.REGDATE,
        AwfulPost.USER_ID to AwfulPost.USER_ID,
        AwfulPost.USERNAME to AwfulPost.USERNAME,
        AwfulPost.IS_IGNORED to AwfulPost.IS_IGNORED,
        AwfulPost.PREVIOUSLY_READ to AwfulPost.PREVIOUSLY_READ,
        AwfulPost.EDITABLE to AwfulPost.EDITABLE,
        AwfulPost.HAS_MOD_CONTROLS to AwfulPost.HAS_MOD_CONTROLS,
        AwfulPost.IS_OP to AwfulPost.IS_OP,
        AwfulPost.IS_PLAT to AwfulPost.IS_PLAT,
        AwfulPost.ROLE to AwfulPost.ROLE,
        AwfulPost.ICON to AwfulPost.ICON,
        AwfulPost.AVATAR to AwfulPost.AVATAR,
        AwfulPost.AVATAR_SECOND to AwfulPost.AVATAR_SECOND,
        AwfulPost.AVATAR_TEXT to AwfulPost.AVATAR_TEXT,
        AwfulPost.CONTENT to AwfulPost.CONTENT,
        AwfulPost.EDITED to AwfulPost.EDITED
    )

    val PostProjection: Array<String> = arrayOfKeys(sPostProjectionMap)

    // UCP Thread
    private val sUCPThreadProjectionMap = hashMapOf(
        AwfulThread.ID to "${DatabaseHelper.TABLE_THREADS}.${AwfulThread.ID} AS ${AwfulThread.ID}",
        AwfulThread.FORUM_ID to AwfulThread.FORUM_ID,
        AwfulThread.INDEX to "${DatabaseHelper.TABLE_UCP_THREADS}.${AwfulThread.INDEX} AS ${AwfulThread.INDEX}",
        AwfulThread.TITLE to "${DatabaseHelper.TABLE_THREADS}.${AwfulThread.TITLE} AS ${AwfulThread.TITLE}",
        AwfulThread.POSTCOUNT to AwfulThread.POSTCOUNT,
        AwfulThread.UNREADCOUNT to AwfulThread.UNREADCOUNT,
        AwfulThread.AUTHOR to AwfulThread.AUTHOR,
        AwfulThread.AUTHOR_ID to AwfulThread.AUTHOR_ID,
        AwfulThread.LOCKED to AwfulThread.LOCKED,
        AwfulThread.CAN_OPEN_CLOSE to AwfulThread.CAN_OPEN_CLOSE,
        AwfulThread.BOOKMARKED to AwfulThread.BOOKMARKED,
        AwfulThread.STICKY to AwfulThread.STICKY,
        AwfulThread.CATEGORY to AwfulThread.CATEGORY,
        AwfulThread.LASTPOSTER to AwfulThread.LASTPOSTER,
        AwfulThread.LAST_POST_DATE to AwfulThread.LAST_POST_DATE,
        AwfulThread.TAG_URL to AwfulThread.TAG_URL,
        AwfulThread.TAG_EXTRA to AwfulThread.TAG_EXTRA,
        AwfulThread.TAG_CACHEFILE to AwfulThread.TAG_CACHEFILE,
        AwfulThread.HAS_NEW_POSTS to "${AwfulThread.UNREADCOUNT} > 0 AS ${AwfulThread.HAS_NEW_POSTS}",
        AwfulThread.HAS_VIEWED_THREAD to AwfulThread.HAS_VIEWED_THREAD,
        AwfulThread.ARCHIVED to AwfulThread.ARCHIVED,
        AwfulThread.RATING to AwfulThread.RATING,
        AwfulThread.FORUM_TITLE to "null",
        DatabaseHelper.UPDATED_TIMESTAMP to "${DatabaseHelper.TABLE_UCP_THREADS}.${DatabaseHelper.UPDATED_TIMESTAMP} AS ${DatabaseHelper.UPDATED_TIMESTAMP}"
    )

    // Drafts
    private val sDraftProjectionMap = hashMapOf(
        AwfulMessage.ID to AwfulMessage.ID,
        AwfulMessage.TITLE to AwfulMessage.TITLE,
        AwfulPost.FORM_COOKIE to AwfulPost.FORM_COOKIE,
        AwfulPost.FORM_KEY to AwfulPost.FORM_KEY,
        AwfulMessage.REPLY_CONTENT to AwfulMessage.REPLY_CONTENT,
        AwfulMessage.REPLY_ICON to AwfulMessage.REPLY_ICON,
        AwfulMessage.RECIPIENT to AwfulMessage.RECIPIENT,
        AwfulMessage.TYPE to AwfulMessage.TYPE,
        AwfulPost.EDIT_POST_ID to AwfulPost.EDIT_POST_ID,
        AwfulPost.REPLY_ORIGINAL_CONTENT to AwfulPost.REPLY_ORIGINAL_CONTENT,
        AwfulMessage.REPLY_ATTACHMENT to AwfulMessage.REPLY_ATTACHMENT,
        AwfulPost.FORM_BOOKMARK to AwfulPost.FORM_BOOKMARK,
        AwfulMessage.EPOC_TIMESTAMP to AwfulMessage.EPOC_TIMESTAMP,
        DatabaseHelper.UPDATED_TIMESTAMP to DatabaseHelper.UPDATED_TIMESTAMP
    )

    val DraftProjection: Array<String> = arrayOf(
        AwfulMessage.ID,
        AwfulMessage.TYPE,
        AwfulMessage.RECIPIENT,
        AwfulMessage.TITLE,
        AwfulMessage.REPLY_CONTENT,
        AwfulMessage.REPLY_ICON
    )
    val DraftPostProjection: Array<String> = arrayOf(
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
    )

    private val sDraftThreadProjectionMap = hashMapOf(
        AwfulMessage.ID to AwfulMessage.ID,
        AwfulPost.FORM_COOKIE to AwfulPost.FORM_COOKIE,
        AwfulPost.FORM_KEY to AwfulPost.FORM_KEY,
        AwfulMessage.POST_CONTENT to AwfulMessage.POST_CONTENT,
        AwfulMessage.POST_SUBJECT to AwfulMessage.POST_SUBJECT,
        AwfulMessage.POST_ICON_ID to AwfulMessage.POST_ICON_ID,
        AwfulMessage.POST_ICON_URL to AwfulMessage.POST_ICON_URL,
        AwfulMessage.REPLY_ATTACHMENT to AwfulMessage.REPLY_ATTACHMENT,
        AwfulPost.FORM_BOOKMARK to AwfulPost.FORM_BOOKMARK,
        AwfulMessage.EPOC_TIMESTAMP to AwfulMessage.EPOC_TIMESTAMP,
        DatabaseHelper.UPDATED_TIMESTAMP to DatabaseHelper.UPDATED_TIMESTAMP
    )

    val DraftThreadProjection: Array<String> = arrayOfKeys(sDraftThreadProjectionMap)

    // Private messages
    private val sPMReplyProjectionMap = hashMapOf(
        AwfulMessage.ID to "${DatabaseHelper.TABLE_PM}.${AwfulMessage.ID} AS ${AwfulMessage.ID}",
        AwfulMessage.TITLE to "${DatabaseHelper.TABLE_PM}.${AwfulMessage.TITLE} AS ${AwfulMessage.TITLE}",
        AwfulMessage.CONTENT to AwfulMessage.CONTENT,
        AwfulMessage.AUTHOR to AwfulMessage.AUTHOR,
        AwfulMessage.DATE to AwfulMessage.DATE,
        AwfulMessage.UNREAD to AwfulMessage.UNREAD,
        AwfulMessage.REPLY_CONTENT to AwfulMessage.REPLY_CONTENT,
        AwfulMessage.REPLY_TITLE to "${DatabaseHelper.TABLE_DRAFTS}.${AwfulMessage.TITLE} AS ${AwfulMessage.REPLY_TITLE}",
        AwfulMessage.RECIPIENT to AwfulMessage.RECIPIENT,
        AwfulMessage.TYPE to AwfulMessage.TYPE,
        AwfulMessage.ICON to AwfulMessage.ICON,
        AwfulMessage.REPLY_ICON to AwfulMessage.REPLY_ICON,
        AwfulMessage.FOLDER to AwfulMessage.FOLDER
    )

    val PMProjection: Array<String> = arrayOf(
        AwfulMessage.ID,
        AwfulMessage.AUTHOR,
        AwfulMessage.TITLE,
        AwfulMessage.CONTENT,
        AwfulMessage.UNREAD,
        AwfulMessage.ICON,
        AwfulMessage.DATE
    )
    val PMReplyProjection: Array<String> = arrayOf(
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
    )

    // Emotes
    private val sEmoteProjectionMap = hashMapOf(
        AwfulEmote.ID to AwfulEmote.ID,
        AwfulEmote.TEXT to AwfulEmote.TEXT,
        AwfulEmote.SUBTEXT to AwfulEmote.SUBTEXT,
        AwfulEmote.URL to AwfulEmote.URL,
        AwfulEmote.INDEX to AwfulEmote.INDEX,
        DatabaseHelper.UPDATED_TIMESTAMP to DatabaseHelper.UPDATED_TIMESTAMP
    )

    val EmoteProjection: Array<String> = arrayOfKeys(sEmoteProjectionMap)


    /**
     * Matches a Uri to the defined patterns.
     *
     * @param aUri the Uri to match against
     * @param throwIfUnmatched if true, a failed match will throw a RuntimeException
     * @return the ID of the matched pattern, or [UriMatcher.NO_MATCH] if it failed
     */
    private fun matchUri(aUri: Uri, throwIfUnmatched: Boolean): Int {
        val match: Int = sUriMatcher.match(aUri)
        if (match == UriMatcher.NO_MATCH && throwIfUnmatched) {
            throw RuntimeException("Unmatched Uri: $aUri")
        }
        return match
    }


    /**
     * Throw an exception if the supplied Uri type constant does not correspond to a table.
     */
    @SuppressLint("DefaultLocale")
    private fun assertIsTableUri(uriType: Int) {
        if (uriType !in TABLE_URIS) {
            throw RuntimeException(
                "Uri type [$uriType] does not correspond to a table"
            )
        }
    }

    /**
     * Convert an array of ints to an array of Strings
     */
    @JvmStatic
    fun int2StrArray(vararg args: Int): Array<String> {
        return args.map(Int::toString).toTypedArray()
    }
}
    private lateinit var mDbHelper: DatabaseHelper


    /////////////////////////////////////////////////////////////////////////
    // ContentProvider functions
    /////////////////////////////////////////////////////////////////////////
    override fun onCreate(): Boolean {
        mDbHelper = DatabaseHelper(context)
        return true
    }

    override fun getType(aUri: Uri): String? {
        return null
    }

    override fun delete(aUri: Uri, aWhere: String?, aWhereArgs: Array<String>?): Int {
        val db = mDbHelper.writableDatabase
        val uriType: Int = matchUri(aUri, true)
        assertIsTableUri(uriType)
        val table = getTableForUriType(uriType)

        // if there's no Where clause, this will delete everything in the table!
        return db.delete(table, aWhere, aWhereArgs)
    }


    override fun update(
        aUri: Uri,
        aValues: ContentValues?,
        aWhere: String?,
        aWhereArgs: Array<String>?
    ): Int {
        var where = aWhere
        var whereArgs = aWhereArgs
        val db = mDbHelper.writableDatabase

        val uriType: Int = matchUri(aUri, true)
        val table = getTableForUriType(uriType)

        // ID-type Uris need a Where clause as well as the table
        val whereClause = when (uriType) {
            URI_FORUM_ID -> AwfulForum.ID
            URI_POST_ID -> AwfulPost.ID
            URI_THREAD_ID,
            URI_UCP_THREAD_ID -> AwfulThread.ID
            URI_PM_ID,
            URI_DRAFT_ID -> AwfulMessage.ID
            URI_EMOTE_ID -> AwfulEmote.ID
            else -> null
        }
        if (whereClause != null) {
            where = "$whereClause=?"
            whereArgs = insertSelectionArg(whereArgs,  aUri.lastPathSegment)
        }

        val result = db.update(table, aValues, where, whereArgs)
        context!!.contentResolver.notifyChange(aUri, null)
        return result
    }


    override fun insert(aUri: Uri, aValues: ContentValues?): Uri {
        val db = mDbHelper.writableDatabase

        val uriType: Int = matchUri(aUri, true)
        assertIsTableUri(uriType)
        val table = getTableForUriType(uriType)

        val rowId = db.insert(table, "", aValues)
        if (rowId > -1) {
            return ContentUris.withAppendedId(aUri, rowId)
        }
        throw SQLException("Failed to insert row into $aUri")
    }


    override fun bulkInsert(aUri: Uri, aValues: Array<ContentValues>): Int {
        // avoid DB operations and update notifications when there's nothing to do
        if (aValues.isEmpty()) {
            return 0
        }
        val db = mDbHelper.writableDatabase

        val uriType: Int = matchUri(aUri, true)
        assertIsTableUri(uriType)
        val table = getTableForUriType(uriType)

        db.transaction {
            try {
                for (value in aValues) {
                    if (uriType == URI_POST) {
                        delete(
                            table,
                            "${AwfulPost.POST_INDEX}=? AND ${AwfulPost.THREAD_ID}=?",
                            int2StrArray(
                                value.getAsInteger(AwfulPost.POST_INDEX),
                                value.getAsInteger(AwfulPost.THREAD_ID)
                            )
                        )
                    } else if (uriType == URI_EMOTE) {
                        delete(
                            table,
                            "${AwfulEmote.TEXT}=?",
                            arrayOf<String>(value.getAsString(AwfulEmote.TEXT))
                        )
                    }
                    replace(table, "", value)
                }

                context!!.contentResolver.notifyChange(aUri, null)
            } catch (e: SQLiteConstraintException) {
                Log.w(TAG, e.toString())
                // transaction failed (exception throws before #setTransactionSuccessful), no rows inserted
                return 0
            } finally {
            }
        }
        // transaction succeeded, all rows inserted
        return aValues.size
    }


    override fun query(
        aUri: Uri, aProjection: Array<String>?, aSelection: String?,
        aSelectionArgs: Array<String>?, aSortOrder: String?
    ): Cursor? {
        var aSelectionArgs = aSelectionArgs
        val builder = SQLiteQueryBuilder()
        // Typically this should fetch a readable database but we're querying before
        // we actually add anything, so make it writable.
        val db = mDbHelper.readableDatabase

        val uriType: Int = matchUri(aUri, false)
        // check for non-match, return null since an unrecognized/malformed Uri gives us nothing useful to do
        if (uriType == UriMatcher.NO_MATCH) {
            val msg = """
                Unrecognised query Uri!
                Uri: $aUri
                Projection: ${aProjection?.contentToString()}
                Selection: $aSelection
                Selection args: ${aSelectionArgs?.contentToString()}
                Sort order: $aSortOrder
            """.trimIndent()
            Log.w(TAG, msg)
            return null
        }

        // get the basic table name for this Uri - some will need to replace this with something more complex below
        var table = getTableForUriType(uriType)
        var whereClause: String? = null
        // set params on the query builder according to the type of Uri - these pairs fall through intentionally
        when (uriType) {
            URI_FORUM_ID -> {
                whereClause = AwfulForum.ID
                builder.projectionMap = sForumProjectionMap
            }

            URI_FORUM -> builder.projectionMap = sForumProjectionMap
            URI_POST_ID -> {
                whereClause = AwfulPost.ID
                builder.projectionMap = sPostProjectionMap
            }

            URI_POST -> builder.projectionMap = sPostProjectionMap
            URI_THREAD_ID -> {
                whereClause = DatabaseHelper.TABLE_THREADS + "." + AwfulThread.ID
                table =
                    DatabaseHelper.TABLE_THREADS + " LEFT OUTER JOIN " + DatabaseHelper.TABLE_FORUM + " ON " + DatabaseHelper.TABLE_THREADS + "." + AwfulThread.FORUM_ID + "=" + DatabaseHelper.TABLE_FORUM + "." + AwfulForum.ID
                builder.projectionMap = sThreadProjectionMap
            }

            URI_THREAD -> {
                table =
                    DatabaseHelper.TABLE_THREADS + " LEFT OUTER JOIN " + DatabaseHelper.TABLE_FORUM + " ON " + DatabaseHelper.TABLE_THREADS + "." + AwfulThread.FORUM_ID + "=" + DatabaseHelper.TABLE_FORUM + "." + AwfulForum.ID
                builder.projectionMap = sThreadProjectionMap
            }

            URI_UCP_THREAD_ID -> {
                whereClause = AwfulThread.ID
                //hopefully this join works
                table =
                    DatabaseHelper.TABLE_UCP_THREADS + ", " + DatabaseHelper.TABLE_THREADS + " ON " + DatabaseHelper.TABLE_UCP_THREADS + "." + AwfulThread.ID + "=" + DatabaseHelper.TABLE_THREADS + "." + AwfulThread.ID
                builder.projectionMap = sUCPThreadProjectionMap
            }

            URI_UCP_THREAD -> {
                table =
                    DatabaseHelper.TABLE_UCP_THREADS + ", " + DatabaseHelper.TABLE_THREADS + " ON " + DatabaseHelper.TABLE_UCP_THREADS + "." + AwfulThread.ID + "=" + DatabaseHelper.TABLE_THREADS + "." + AwfulThread.ID
                builder.projectionMap = sUCPThreadProjectionMap
            }

            URI_PM_ID -> {
                whereClause = DatabaseHelper.TABLE_PM + "." + AwfulMessage.ID
                table =
                    DatabaseHelper.TABLE_PM + " LEFT OUTER JOIN " + DatabaseHelper.TABLE_DRAFTS + " ON " + DatabaseHelper.TABLE_PM + "." + AwfulMessage.ID + "=" + DatabaseHelper.TABLE_DRAFTS + "." + AwfulMessage.ID
                builder.projectionMap = sPMReplyProjectionMap
            }

            URI_PM -> {
                table =
                    DatabaseHelper.TABLE_PM + " LEFT OUTER JOIN " + DatabaseHelper.TABLE_DRAFTS + " ON " + DatabaseHelper.TABLE_PM + "." + AwfulMessage.ID + "=" + DatabaseHelper.TABLE_DRAFTS + "." + AwfulMessage.ID
                builder.projectionMap = sPMReplyProjectionMap
            }

            URI_DRAFT_ID -> {
                whereClause = AwfulMessage.ID
                builder.projectionMap = sDraftProjectionMap
            }

            URI_DRAFT -> builder.projectionMap = sDraftProjectionMap
            URI_THREAD_DRAFT_ID -> {
                whereClause = AwfulMessage.ID
                builder.projectionMap = sDraftThreadProjectionMap
            }

            URI_THREAD_DRAFT -> builder.projectionMap = sDraftThreadProjectionMap
            URI_EMOTE_ID -> {
                whereClause = AwfulEmote.ID
                builder.projectionMap = sEmoteProjectionMap
            }

            URI_EMOTE -> builder.projectionMap = sEmoteProjectionMap
            else ->                 // this should explicitly handle all valid Uris, so if we get here, someone blew it
                throw RuntimeException("$TAG - Unhandled URI type: $uriType")
        }

        builder.tables = table
        if (whereClause != null) {
            builder.appendWhere("$whereClause=?")
            aSelectionArgs = insertSelectionArg(aSelectionArgs, aUri.lastPathSegment!!)
        }

        // perform the query
        try {
            val result = builder.query(
                db, aProjection, aSelection,
                aSelectionArgs, null, null, aSortOrder
            )
            result.setNotificationUri(context?.contentResolver, aUri)
            return result
        } catch (e: Exception) {
            val msg =
                String.format("aUri:\n%s\nQuery tables string:\n%s", aUri, builder.tables)
            Log.w(TAG, msg, e)
            throw e
        }
    }


    /////////////////////////////////////////////////////////////////////////
    // Utility methods
    /////////////////////////////////////////////////////////////////////////
    /**
     * Look up the DB table corresponding to a ContentProvider Uri type.
     *
     * This is intended to map all types to a table, so it will throw a RuntimeException if a
     * currently unhandled Uri type is passed in .
     * @param uriType one of the Uri types returned by [.matchUri]
     * @ return the name of its corresponding database table
     */
    private fun getTableForUriType(uriType: Int): String {
        return when (uriType) {
            URI_FORUM_ID,
            URI_FORUM -> DatabaseHelper.TABLE_FORUM
            URI_POST_ID,
            URI_POST -> DatabaseHelper.TABLE_POSTS
            URI_THREAD_ID,
            URI_THREAD -> DatabaseHelper.TABLE_THREADS
            URI_UCP_THREAD_ID,
            URI_UCP_THREAD -> DatabaseHelper.TABLE_UCP_THREADS
            URI_PM_ID,
            URI_PM -> DatabaseHelper.TABLE_PM
            URI_DRAFT_ID,
            URI_DRAFT -> DatabaseHelper.TABLE_DRAFTS
            URI_THREAD_DRAFT_ID,
            URI_THREAD_DRAFT -> DatabaseHelper.TABLE_THREAD_DRAFTS
            URI_EMOTE_ID,
            URI_EMOTE -> DatabaseHelper.TABLE_EMOTES
            else -> throw RuntimeException("Invalid table constant: $uriType")
        }
    }


    /**
     * Inserts an argument at the beginning of the selection arg list.
     *
     * The [SQLiteQueryBuilder]'s where clause is
     * prepended to the user's where clause (combined with 'AND') to generate
     * the final where close, so arguments associated with the QueryBuilder are
     * prepended before any user selection args to keep them in the right order.
     */
    private fun insertSelectionArg(selectionArgs: Array<String>?, arg: String?): Array<String> {
        return if (selectionArgs == null) {
            arrayOf(arg ?: "")
        } else {
            arrayOf(arg ?: "", *selectionArgs)
        }
    }
}
