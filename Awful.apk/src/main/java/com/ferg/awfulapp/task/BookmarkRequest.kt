package com.ferg.awfulapp.task

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.thread.AwfulThread
import com.ferg.awfulapp.util.AwfulError
import com.ferg.awfulapp.util.toSqlBoolean
import org.jsoup.nodes.Document

/**
 * Created by matt on 8/8/13.
 */
class BookmarkRequest(context: Context, private val threadId: Int, private val add: Boolean) : AwfulRequest<Void?>(context, null) {

    override fun generateUrl(urlBuilder: Uri.Builder?): String {
        addPostParam(Constants.PARAM_THREAD_ID, Integer.toString(threadId))
        if (add) {
            addPostParam(Constants.PARAM_ACTION, "add")
        } else {
            addPostParam(Constants.PARAM_ACTION, "remove")
        }
        return Constants.FUNCTION_BOOKMARK
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
