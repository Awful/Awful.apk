package com.ferg.awfulapp.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;

import com.ferg.awfulapp.AwfulActivity;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.thread.AwfulForum;
import com.ferg.awfulapp.thread.AwfulThread;

public class StringProvider {

    private static AwfulPreferences prefs = AwfulPreferences.getInstance();

    public static String getString(int stringId){
        return prefs.getResources().getString(stringId);
    }

    public static String getForumName(AwfulActivity context, int forumId){
        return getName(context, forumId, AwfulForum.CONTENT_URI, AwfulProvider.ForumProjection,
                AwfulForum.TITLE, "Forum #");
    }

    public static String getThreadName(AwfulActivity context, int threadId){
        return getName(context, threadId, AwfulThread.CONTENT_URI, AwfulProvider.ThreadProjection,
                AwfulThread.TITLE, "Thread #");
    }


    private static String getName(AwfulActivity context, int id, Uri contentUri, String[] projection,
                                  String columnName, String defaultPrefix) {
        String result;
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(ContentUris.withAppendedId(contentUri, id),
                projection,
                null,
                null,
                null);
        if (cursor != null && cursor.moveToFirst()) {
            result = cursor.getString(cursor.getColumnIndex(columnName));
        } else {
            result = defaultPrefix + String.valueOf(id);
        }
        if (cursor != null) {
            cursor.close();
        }
        return result;
    }
}
