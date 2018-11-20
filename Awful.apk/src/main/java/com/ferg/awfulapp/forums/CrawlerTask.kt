package com.ferg.awfulapp.forums

import android.content.Context
import android.net.Uri
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document
import timber.log.Timber
import java.util.*

/**
 *
 * Created by baka kaba on 19/12/2018.
 *
 * A task that parses and updates the forum structure by spidering subforum links.
 * This task is heavy (since it loads every forum page, ~75 pages at the time of writing)
 * but parses every forum visible to the user, and captures subtitle data.
 *
 * This basically works by building a tree asynchronously. It loads the main forum page,
 * parses it for the section links (Main, Discussion etc) and adds them to the root list as
 * [Forum] objects. Then each link is followed with a separate request, and each page
 * is parsed for its list of subforums. These are all created as Forum objects, added to their
 * parents' subforum lists, and then their links (if any) are followed, until there's nothing left.
 */
internal class CrawlerTask(context: Context, priority: Priority) : UpdateTask(context, priority.delayMillis) {

    private val forumSections = Collections.synchronizedList(ArrayList<Forum>())
    override val initialTask: ForumParseTask = MainForumRequest()


    override fun buildForumStructure(): ForumStructure {
        return ForumStructure.buildFromTree(forumSections, ForumRepository.TOP_LEVEL_PARENT_ID)
    }


    /**
     * Parse the category links (Main, Discussion etc) on the main forum page [document].
     *
     * This is to ensure all the 'hidden' subforums are picked up, which don't show up
     * on the main single-page listing - you have to visit the pages for each category.
     * Any categories found will be added to [forumSections].
     */
    private fun parseMainSections(document: Document) {
        // look for section links on the main page - fail immediately if we can't find them!
        val sections = document.getElementsByClass("category")
        if (sections.isEmpty()) {
            fail("unable to parse main forum page - 0 links found!")
            return
        }

        // parse each section to get its data, and add a 'forum' to the top level list
        sections.map { it.selectFirst("a") }.forEach { link ->
            forumSections.addForum(parentId = ForumRepository.TOP_LEVEL_PARENT_ID, url = link.attr("abs:href"), title = link.text())
        }
    }


    /**
     * Parse a forum page [document], and attempt to scrape any subforum links it contains.
     *
     * This can be used on category pages (e.g. the 'Main' link) as well as actual
     * forum pages (e.g. GBS). The [forum] object represents the page being parsed, and will
     * have its subforums updated as appropriate.
     */
    private fun parseSubforums(forum: Forum, document: Document) {
        val subforumElements = document.select("tr.subforum")
        if (DEBUG) Timber.d("Parsed forum ${forum.title} - found ${subforumElements.size} subforums")

        // parse details and create subforum objects, and add them to this forum's subforum list
        for (element in subforumElements) {
            val link = element.selectFirst("a")
            val subtitle = element.select("dd").text().removePrefix("- ") // strip leading junk on subtitles
            forum.subforums.addForum(parentId = forum.id, url = link.attr("abs:href"), title = link.text(), subtitle = subtitle)
        }
    }


    /**
     * Parse a forum's url to retrieve its ID, or null if it couldn't be found
     */
    private fun getForumId(url: String): Int? =
            Uri.parse(url).getQueryParameter(PARAM_FORUM_ID)?.toIntOrNull()


    /**
     * Create and add a Forum object to a list.
     *
     * @param parentId  The ID of the parent forum
     * @param url       The url of this forum
     * @param title     The title of this forum
     * @param subtitle  The subtitle of this forum
     */
    private fun MutableList<Forum>.addForum(parentId: Int, url: String, title: String, subtitle: String = "") {
        // the subforum list needs to be synchronized since multiple async requests can add to it
        val subforums = Collections.synchronizedList(ArrayList<Forum>())
        val forumId = getForumId(url)
        if (forumId == null) {
            Timber.w("Unable to find forum ID key $PARAM_FORUM_ID in url $url")
            return
        }
        val forum = Forum(forumId, parentId, title, subtitle, subforums)
        add(forum)
        if (url.isNotEmpty()) startTask(ParseSubforumsRequest(forum, url))
    }


    ///////////////////////////////////////////////////////////////////////////
    // Requests
    ///////////////////////////////////////////////////////////////////////////


    /**
     * A request that fetches the main forums page and parses it for sections (Main etc)
     */
    private inner class MainForumRequest : UpdateTask.ForumParseTask() {

        override val url: String
            get() = BASE_URL

        override fun onRequestSucceeded(doc: Document) {
            Timber.i("Parsing main page")
            parseMainSections(doc)
        }

        override fun onRequestFailed(error: AwfulError) {
            Timber.w("Failed to get index page!\n${error.message}")
        }
    }


    /**
     * A request that fetches a forum page and parses it for subforum data
     *
     * This loads a [url] representing a [forum], and parses the resulting page, adding the data to
     * the [forum] object.
     */
    private inner class ParseSubforumsRequest(private val forum: Forum, override val url: String) : UpdateTask.ForumParseTask() {

        override fun onRequestSucceeded(doc: Document) {
            parseSubforums(forum, doc)
        }

        override fun onRequestFailed(error: AwfulError) {
            Timber.w("Failed to load forum: ${forum.title}\n${error.message}")
        }
    }

    /**
     * Priority used to throttle update tasks with a given delay
     */
    enum class Priority(val delayMillis: Int) {
        LOW(2000), HIGH(0)
    }
}
