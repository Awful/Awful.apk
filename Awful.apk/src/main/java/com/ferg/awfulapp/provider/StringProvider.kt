package com.ferg.awfulapp.provider

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.thread.AwfulForum
import com.ferg.awfulapp.thread.AwfulThread

object StringProvider {
    private val prefs = AwfulPreferences.getInstance()

    fun getString(stringId: Int): String {
        return prefs.resources.getString(stringId)
    }

    fun getForumName(context: Context, forumId: Int): String? {
        return getName(
            context, forumId, AwfulForum.CONTENT_URI, AwfulProvider.ForumProjection,
            AwfulForum.TITLE, "Forum #"
        )
    }

    fun getThreadName(context: Context, threadId: Int): String? {
        return getName(
            context, threadId, AwfulThread.CONTENT_URI, AwfulProvider.ThreadProjection,
            AwfulThread.TITLE, "Thread #"
        )
    }


    private fun getName(
        context: Context, id: Int, contentUri: Uri, projection: Array<String>,
        columnName: String?, defaultPrefix: String?
    ): String? {
        val result: String?
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContentUris.withAppendedId(contentUri, id.toLong()),
            projection,
            null,
            null,
            null
        )
        result = if (cursor != null && cursor.moveToFirst()) {
            cursor.getString(cursor.getColumnIndexOrThrow(columnName))
        } else {
            defaultPrefix + id.toString()
        }
        cursor?.close()
        return result
    }
}
