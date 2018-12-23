package com.ferg.awfulapp.task

import android.content.Context
import com.ferg.awfulapp.constants.Constants.FUNCTION_ANNOUNCEMENTS
import com.ferg.awfulapp.preferences.AwfulPreferences
import com.ferg.awfulapp.thread.AwfulPost
import com.ferg.awfulapp.thread.AwfulPost.tryConvertToHttps
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document
import timber.log.Timber
import java.util.*

/**
 * Created by baka kaba on 24/01/2017.
 *
 *
 * Request to load announcements and parse them as AwfulPosts.
 *
 * This performs no database caching!
 *
 * Announcements are like a weird variation on posts in a thread, as such they only have a few
 * of the usual page elements, and only a few properties set in AwfulPost.
 */
@JvmSuppressWildcards
class AnnouncementsRequest(context: Context)
    : AwfulRequest<List<@JvmSuppressWildcards AwfulPost>>(context, FUNCTION_ANNOUNCEMENTS) {

    init {
        parameters.add("forumid", "1")
    }

    private fun parseAnnouncement(aThread: Document): List<AwfulPost> {
        val results = ArrayList<AwfulPost>()
        val prefs = AwfulPreferences.getInstance()

        // TODO: tidy up when there's an announcement to test against
        // grab all the main announcement sections - these contain *most* of the data we need :/
        val mainAnnouncements = aThread.select("#main_full  tr[valign='top']")
        Timber.d("parseAnnouncement: found ${mainAnnouncements.size} announcements")
        for (announcementSection in mainAnnouncements) {
            val announcement = AwfulPost()

            val author = announcementSection.selectFirst(".author")
            if (author != null) {
                announcement.username = author.text()
            }

            val regDate = announcementSection.selectFirst(".registered")
            if (regDate != null) {
                announcement.regDate = regDate.text()
            }

            val avatar = announcementSection.selectFirst(".title img")
            if (avatar != null) {
                tryConvertToHttps(avatar)
                announcement.avatar = avatar.attr("src")
            }

            // not sure if this ever appears for announcements but whatever, may as well
            val editedBy = announcementSection.selectFirst(".editedby")
            if (editedBy != null) {
                announcement.edited = "<i>" + editedBy.text() + "</i>"
            }

            // announcements have their post date in a whole other section directly after the announcement section
            val postDateSection = announcementSection.nextElementSibling()
            if (postDateSection != null) {
                val postDate = postDateSection.selectFirst(".postdate")
                if (postDate != null) {
                    announcement.date = postDate.text()
                }
            }


            val postBody = announcementSection.selectFirst(".postbody")
            if (postBody != null) {
                // process videos, images and links and store the resulting post HTML
                AwfulPost.convertVideos(postBody, prefs.inlineYoutube)
                for (image in postBody.getElementsByTag("img")) {
                    AwfulPost.processPostImage(image, false, prefs)
                }
                for (link in postBody.getElementsByTag("a")) {
                    tryConvertToHttps(link)
                }
                announcement.content = postBody.html()
            }
            // I guess this is important...?
            announcement.isEditable = false
            results.add(announcement)
            Timber.i("${mainAnnouncements.size} posts found, ${results.size} posts parsed.")
        }
        return results
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): List<AwfulPost> {
        return parseAnnouncement(doc)
    }

}
