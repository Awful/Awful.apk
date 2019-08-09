package com.ferg.awfulapp.task

import android.net.Uri
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.thread.AwfulPostIcon
import java.util.ArrayList

import android.content.Context
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

import com.ferg.awfulapp.constants.Constants.FUNCTION_NEW_THREAD
import com.ferg.awfulapp.constants.Constants.FUNCTION_PRIVATE_MESSAGE
import com.ferg.awfulapp.constants.Constants.POST_ICON_REQUEST_TYPES.PM

/**
 * An AwfulRequest that parses forum icons from the main forums page, and stores them in the database.
 */
class PostIconRequest(
    context: Context,
    private val type: Constants.POST_ICON_REQUEST_TYPES,
    forumid: Int
) : AwfulRequest<ArrayList<AwfulPostIcon>>(
    context,
    if (type == PM) FUNCTION_PRIVATE_MESSAGE else FUNCTION_NEW_THREAD, false
) {
    private var forumid = 0

    init {
        with(parameters) {
            if (type == Constants.POST_ICON_REQUEST_TYPES.FORUM_POST && forumid != 0) {
                add(Constants.PARAM_ACTION, Constants.ACTION_NEW_THREAD)
                add(Constants.PARAM_FORUM_ID, forumid.toString())
            } else {
                add(Constants.PARAM_ACTION, Constants.ACTION_NEW_MESSAGE)
            }
        }
        this.forumid = forumid
    }


    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): ArrayList<AwfulPostIcon> {
        return AwfulPostIcon.parsePostIcons(doc.getElementsByClass("posticon"), context)
    }

    protected fun handleError(error: AwfulError, doc: Document): Boolean {
        return error.isCritical
    }
}