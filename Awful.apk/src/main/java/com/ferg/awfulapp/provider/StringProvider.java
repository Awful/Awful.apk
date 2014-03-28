package com.ferg.awfulapp.provider;

import android.content.ContentUris;
import android.database.Cursor;
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
        try {
            CursorLoader cl = new CursorLoader(context,
                    ContentUris.withAppendedId(AwfulForum.CONTENT_URI, forumId),
                    AwfulProvider.ForumProjection,
                    null,
                    null,
                    null);
            Cursor forum = cl.loadInBackground();
            if (forum != null && !forum.isClosed() && forum.getCount() > 0 && forum.moveToFirst()) {
                String name = forum.getString(forum.getColumnIndex(AwfulForum.TITLE));
                forum.close();
                return name;
            }
        }catch(IllegalStateException ise){
            Log.e("StringProvider", "Error: "+ ise.getMessage());
        }
        return "Forum #"+String.valueOf(forumId);
    }

    public static String getThreadName(AwfulActivity context, int threadId){
        try {
            CursorLoader cl = new CursorLoader(context,
                    ContentUris.withAppendedId(AwfulThread.CONTENT_URI, threadId),
                    AwfulProvider.ThreadProjection,
                    null,
                    null,
                    null);
            Cursor thread = cl.loadInBackground();
            if(thread != null && !thread.isClosed() && thread.getCount() >0 && thread.moveToFirst()){
                String name = thread.getString(thread.getColumnIndex(AwfulThread.TITLE));
                thread.close();
                return name;
            }
        }catch(IllegalStateException ise){
            Log.e("StringProvider", "Error: "+ ise.getMessage());
        }
        return "Thread #"+String.valueOf(threadId);
    }
}
