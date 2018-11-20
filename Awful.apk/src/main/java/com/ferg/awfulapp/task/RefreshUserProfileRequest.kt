package com.ferg.awfulapp.task

import android.content.Context
import android.net.Uri
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.Keys.IGNORE_FORMKEY
import com.ferg.awfulapp.preferences.Keys.USER_AVATAR_URL
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

/**
 * Created by baka kaba on 21/12/18.
 *
 * Request to pull current data from the user's profile, and update the local app state.
 *
 * This currently updates the user's avatar URL, and the [AwfulPreferences.ignoreFormkey] used
 * to validate (I guess!?) attempts to ignore a user.
 */
class RefreshUserProfileRequest(context: Context) : AwfulRequest<Void?>(context, FUNCTION_MEMBER) {

    companion object {
        private const val PROFILE_ID_FOR_THIS_USER = "0"
    }

    override fun generateUrl(urlBuilder: Uri.Builder?): String {
        with(urlBuilder!!) {
            appendQueryParameter(PARAM_ACTION, ACTION_PROFILE)
            appendQueryParameter(PARAM_USER_ID, PROFILE_ID_FOR_THIS_USER)
            return build().toString()
        }
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? {
        val formKey = doc.selectFirst("[name=formkey]")
                ?: throw AwfulError("Couldn't read profile page")
        preferences.setPreference(IGNORE_FORMKEY, formKey.`val`())

        // TODO: set the username here, and have any "update username" actions use this request to do it
        // the user's avatar (if any) is the image before the first <br> tag -
        // any images after that are gang tags, extra images to make the avatar longer, etc
        val avatarUrl = doc.selectFirst(".title")
                ?.allElements
                ?.takeWhile { it.tagName() != "br" }
                ?.firstOrNull { it.tagName() == "img" }
                ?.attr("src")
        preferences.setPreference(USER_AVATAR_URL, avatarUrl ?: "")
        return null
    }

}
