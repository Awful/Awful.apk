package com.ferg.awfulapp.task

import android.content.Context
import android.net.Uri

import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.util.AwfulError

import org.jsoup.nodes.Document

/**
 * An AwfulRequest that adds a userId to the user's ignore list.
 */
class IgnoreRequest(context: Context, userId: Int) : AwfulRequest<Void?>(context, null) {
    init {
        addPostParam(Constants.PARAM_ACTION, Constants.ACTION_ADDLIST)
        addPostParam(Constants.PARAM_USERLIST, Constants.USERLIST_IGNORE)
        addPostParam(Constants.FORMKEY, preferences.ignoreFormkey)
        addPostParam(Constants.PARAM_USER_ID, Integer.toString(userId))
    }

    override fun generateUrl(urlBuilder: Uri.Builder?): String {
        //since we aren't adding query arguments to a POST request,
        //we can just pass null in the constructor URL field and it'll skip this Uri.Builder
        return Constants.FUNCTION_MEMBER2
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? = null // nothing to handle, just fire and forget

}
