package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.ACTION_PROFILE
import com.ferg.awfulapp.constants.Constants.FUNCTION_MEMBER
import com.ferg.awfulapp.constants.Constants.PARAM_ACTION
import com.ferg.awfulapp.constants.Constants.PARAM_USER_ID
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.preferences.IntPreference
import com.ferg.awfulapp.preferences.StringPreference
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

    init {
        with(parameters) {
            add(PARAM_ACTION, ACTION_PROFILE)
            add(PARAM_USER_ID, PROFILE_ID_FOR_THIS_USER)
        }
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? {
        val formKey = doc.selectFirst("[name=formkey]")
                ?: throw AwfulError("Couldn't read profile page")
        preferences.setPreference(StringPreference.IGNORE_FORMKEY, formKey.`val`())

        val userId = doc.selectFirst("[name=userId]")
                ?: throw AwfulError("Couldn't read profile page")
        preferences.setPreference(IntPreference.USER_ID, userId.`val`().toInt())
        val username = doc.getElementById("loggedinusername")
                ?: throw AwfulError("Couldn't read profile page")
        preferences.setPreference(StringPreference.USERNAME, username.text().trim())

        // the user's avatar (if any) is the image before the first <br> tag -
        // any images after that are gang tags, extra images to make the avatar longer, etc
        val avatarUrl = doc.selectFirst(".title")
                ?.allElements
                ?.takeWhile { it.tagName() != "br" }
                ?.firstOrNull { it.tagName() == "img" }
                ?.attr("src")
        preferences.setPreference(StringPreference.USER_AVATAR_URL, avatarUrl ?: "")
        return null
    }

}
