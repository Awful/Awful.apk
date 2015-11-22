package com.ferg.awfulapp.provider;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.util.Log;

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
        Cursor cursor = null;
        try {
            CursorLoader cl = new CursorLoader(context,
                    ContentUris.withAppendedId(contentUri, id),
                    projection,
                    null,
                    null,
                    null);
            cursor = cl.loadInBackground();
            if(cursor != null && !cursor.isClosed() && cursor.getCount() > 0 && cursor.moveToFirst()){
                return cursor.getString(cursor.getColumnIndex(columnName));
            }
        } catch (IllegalStateException ise){
            Log.e("StringProvider", "Error: "+ ise.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return defaultPrefix + String.valueOf(id);
    }
}
