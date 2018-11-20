package com.ferg.awfulapp.task

import android.content.Context
import android.net.Uri
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.preferences.Keys
import com.ferg.awfulapp.thread.AwfulForum
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document
import timber.log.Timber
import java.util.regex.Pattern

/**
 * An AwfulRequest that parses forum icons from the main forums page, and stores them in the database.
 */
class IndexIconRequest(context: Context) : AwfulRequest<Void?>(context, null) {

    //TODO: do we even need this anymore? We don't display these forum icons, but we do parse the colours for our custom icons, so check what's needed
    companion object {
        val REQUEST_TAG = Any()
    }

    override val requestTag: Any
        get() = REQUEST_TAG


    override fun generateUrl(urlBuilder: Uri.Builder?): String = Constants.BASE_URL + "/"

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? {
        AwfulForum.processForumIcons(doc, contentResolver)
        updateProgress(80)

        //optional section, parses username from PM notification field.
        // TODO: this has nothing to do with parsing forum icons - if we need to update the username, do it separately. Also it's broken when there's an apostrophe in the username?
        val pmBlock = doc.getElementsByAttributeValue("id", "pm")
        try {
            if (pmBlock.size > 0) {
                val bolded = pmBlock.first().getElementsByTag("b")
                if (bolded.size > 1) {
                    val name = bolded.first()?.text()?.split("'".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()?.get(0)
                    val unread = bolded[1].text()
                    val findUnread = Pattern.compile("(\\d+)\\s+unread")
                    val matchUnread = findUnread.matcher(unread)
                    var unreadCount = -1
                    if (matchUnread.find()) {
                        unreadCount = Integer.parseInt(matchUnread.group(1))
                    }
                    Timber.v("text: $name - $unreadCount")
                    if (name != null && name.isNotEmpty()) {
                        preferences.setPreference(Keys.USERNAME, name)
                    }
                }
            }
        } catch (e: Exception) {
            //this chunk is optional, no need to fail everything if it doesn't work out.
        }

        return null
    }

}
