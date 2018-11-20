package com.ferg.awfulapp.task

import android.content.Context
import android.net.Uri
import com.ferg.awfulapp.announcements.AnnouncementsManager
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.messages.PmManager
import com.ferg.awfulapp.thread.AwfulForum
import com.ferg.awfulapp.thread.AwfulPagedItem
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document

/**
 * Fetch the threads on a certain [page] of the forum with the given [forumId].
 *
 * This loads and parses the page, updating the cache database as appropriate,
 * depending on whether a normal or bookmarks page was loaded.
 *
 * This request also hands the page off to other parsers, e.g. for announcements
 * and private messages, to scrape any updated information the page contains.
 */
class ThreadListRequest(context: Context, private val forumId: Int, private val page: Int)
    : AwfulStrippedRequest<Void?>(context, when {
        forumId != USERCP_ID -> FUNCTION_FORUM
        page == 1 -> FUNCTION_USERCP
        else -> FUNCTION_BOOKMARK
    }) {
// TODO: 19/09/2016 decide whether to handle all USERCP requests as bookmark urls (and do the PmManager calls a different way)

    override val requestTag: Any
        get() = REQUEST_TAG


    override fun generateUrl(urlBuilder: Uri.Builder?): String {
        with(urlBuilder!!) {
            if (forumId != USERCP_ID) appendQueryParameter(PARAM_FORUM_ID, forumId.toString())
            appendQueryParameter(PARAM_PAGE, page.toString())
            return build().toString()
        }
    }

    @Throws(AwfulError::class)
    override fun handleResponse(doc: Document): Void? {
        val lastPage = AwfulPagedItem.parseLastPage(doc)
        handleStrippedResponse(doc, page, lastPage)
        return null
    }

    @Throws(AwfulError::class)
    override fun handleStrippedResponse(document: Document, currentPage: Int?, totalPages: Int?): Void? {
        val thisPage = currentPage ?: page
        val lastPage = totalPages ?: thisPage
        // TODO: legacy try/catch - work out what this is meant to be catching exactly, and if we can ditch it
        try {
            // parse the threads on the page, and also check for announcements/PMs depending on where they appear
            if (forumId == USERCP_ID) {
                AwfulForum.parseUCPThreads(document, page, lastPage, contentResolver)
                PmManager.parseUcpPage(document)
            } else {
                AwfulForum.parseThreads(forumId, page, lastPage, document, contentResolver)
                AnnouncementsManager.getInstance().parseForumPage(document)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw AwfulError()
        }
        return null
    }


    companion object {
        val REQUEST_TAG = Any()
    }
}
