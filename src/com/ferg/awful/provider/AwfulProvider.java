package com.ferg.awful.provider;

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

import com.ferg.awful.constants.Constants;
import com.ferg.awful.thread.AwfulSubforum;
import com.ferg.awful.thread.AwfulForum;

import java.util.ArrayList;
import java.util.HashMap;

public class AwfulProvider extends ContentProvider {
    private static final String TAG = "AwfulProvider";

    private static final String DATABASE_NAME = "awful.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_FORUM    = "forum";
    private static final String TABLE_SUBFORUM = "subforum";

    private static final int FORUM       = 0;
    private static final int FORUM_ID    = 1;
    private static final int SUBFORUM    = 2;
    private static final int SUBFORUM_ID = 3;

    private static final UriMatcher sUriMatcher;
	private static HashMap<String, String> sForumProjectionMap;
	private static HashMap<String, String> sSubforumProjectionMap;

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context aContext) {
            super(aContext, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase aDb) {
            aDb.execSQL("CREATE TABLE " + TABLE_FORUM + " (" +
                AwfulForum.ID      + " INTEGER UNIQUE," + 
                AwfulForum.TITLE   + " VARCHAR,"        + 
                AwfulForum.SUBTEXT + " VARCHAR);");

            aDb.execSQL("CREATE TABLE " + TABLE_SUBFORUM + " (" +
                AwfulSubforum.ID        + " INTEGER UNIQUE," + 
                AwfulSubforum.TITLE     + " VARCHAR,"        + 
                AwfulSubforum.PARENT_ID + " INTEGER);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase aDb, int aOldVersion, int aNewVersion) {
            aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_FORUM);
            aDb.execSQL("DROP TABLE IF EXISTS " + TABLE_SUBFORUM);

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
            default:
                break;
        }

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
            case SUBFORUM:
                table = TABLE_SUBFORUM;
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

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(aUri);
        switch(match) {
            case FORUM:
                table = TABLE_FORUM;
                break;
            case SUBFORUM:
                table = TABLE_SUBFORUM;
                break;
        }

		db.beginTransaction();

		try {
			for (ContentValues value : aValues) {
				db.insert(table, "", value);
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
			case SUBFORUM:
				table = TABLE_SUBFORUM;
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
			case SUBFORUM_ID:
                aSelectionArgs = insertSelectionArg(aSelectionArgs, aUri.getLastPathSegment());        
                builder.appendWhere(AwfulSubforum.ID + "=?");
			case SUBFORUM:
				builder.setTables(TABLE_SUBFORUM);
				builder.setProjectionMap(sSubforumProjectionMap);
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

    private String appendWhere(String aWhere, String aAppend) {
        if (aWhere == null) {
            return aAppend;
        }

        return aWhere + " AND " + aAppend;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sForumProjectionMap = new HashMap<String, String>();
		sSubforumProjectionMap = new HashMap<String, String>();

		sUriMatcher.addURI(Constants.AUTHORITY, "forum", FORUM);
		sUriMatcher.addURI(Constants.AUTHORITY, "forum/#", FORUM_ID);
		sUriMatcher.addURI(Constants.AUTHORITY, "subforum", SUBFORUM);
		sUriMatcher.addURI(Constants.AUTHORITY, "subforum/#", SUBFORUM_ID);

		sForumProjectionMap.put(AwfulForum.ID, AwfulForum.ID);
		sForumProjectionMap.put(AwfulForum.TITLE, AwfulForum.TITLE);
		sForumProjectionMap.put(AwfulForum.SUBTEXT, AwfulForum.SUBTEXT);

		sSubforumProjectionMap.put(AwfulSubforum.ID, AwfulSubforum.ID);
		sSubforumProjectionMap.put(AwfulSubforum.TITLE, AwfulSubforum.TITLE);
		sSubforumProjectionMap.put(AwfulSubforum.PARENT_ID, AwfulSubforum.PARENT_ID);
    }
}
