package com.ferg.awfulapp.task

import android.content.ContentValues
import android.content.Context
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.thread.AwfulThread
import com.ferg.awfulapp.util.AwfulError
import com.ferg.awfulapp.util.toSqlBoolean
import org.jsoup.nodes.Document

/**
 * Add or remove a bookmark on the site for the given [threadId], updating the local database.
 */
class BookmarkRequest(context: Context, private val threadId: Int, private val add: Boolean)
    : AwfulRequest<Void?>(context, FUNCTION_BOOKMARK, isPostRequest = true) {

    init {
        with(parameters) {
            add(PARAM_THREAD_ID, threadId.toString())
            add(PARAM_ACTION, if (add) "add" else "remove")
        }
    }


    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? {
        val cv = ContentValues()
        cv.put(AwfulThread.BOOKMARKED, add.toSqlBoolean)
        val id = arrayOf(threadId.toString())

        with(contentResolver) {
            update(AwfulThread.CONTENT_URI, cv, "${AwfulThread.ID}=?", id)
            if (!add) delete(AwfulThread.CONTENT_URI_UCP, "${AwfulThread.ID}=?", id)
        }
        return null
    }

}
