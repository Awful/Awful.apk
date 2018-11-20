package com.ferg.awfulapp.forums

import android.content.Context
import com.ferg.awfulapp.constants.Constants.*
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber
import java.text.ParseException
import java.util.*

/**
 *
 * Created by baka kaba on 18/04/2016.
 *
 * Forum update task that parses the forum jump dropdown.
 * This task only requires a single page load, but can't scrape subtitles
 * and misses certain forums.
 *
 * It works by finding the set of `option` tags in the dropdown
 * box, then looks for the items that are formatted like forum entries.
 * It keeps a running model of the current branch hierarchy, so it can identify
 * what level a forum should be on and which forum is its parent
 */
internal class DropdownParserTask(context: Context) : UpdateTask(context) {

    override val initialTask: ForumParseTask = DropdownParseRequest()
    private val parsedForums = ArrayList<Forum>()


    private inner class DropdownParseRequest : UpdateTask.ForumParseTask() {

        override val url: String
            get() = "$FUNCTION_FORUM?$PARAM_FORUM_ID=$FORUM_ID_GOLDMINE"

        override fun onRequestSucceeded(doc: Document) {
            Timber.i("Got page - parsing dropdown to get forum hierarchy")
            parsePage(doc)
        }

        override fun onRequestFailed(error: AwfulError) {
            Timber.w("Request error - couldn't get page to parse")
        }
    }


    /**
     * Parse a forum hierarchy from a page containing a dropdown picker, populating [parsedForums]
     */
    private fun parsePage(doc: Document?) {
        // can't do anything without a page - fail immediately
        if (doc == null) {
            fail("no document to parse")
            return
        }

        try {
            // this stack works like a breadcrumb trail, so we can compare to the last forum
            // added at the current depth, and work out the new forum's relationship to it
            val forumHierarchy = Stack<ParsedForum>().apply {
                // this is a dummy forum representing the root - every other forum will be below this (depth >= 0)
                push(ParsedForum(ForumRepository.TOP_LEVEL_PARENT_ID, "root", -1))
            }

            fun getPreviousForum(): ParsedForum = forumHierarchy.peek()

            // get the items from the dropdown
            val items = doc.select("form.forum_jump option")
            Timber.d("Found ${items.size} elements")
            if (items.isEmpty()) throw ParseException("No dropdown items found!", 0)

            items.mapNotNull(::parseForum).forEach { forum ->
                // backtrack until we get to a forum at a higher level than this forum, so we can insert it below
                while (getPreviousForum().depth >= forum.depth) {
                    forumHierarchy.pop()
                }

                // we're going to push the new forum onto the stack - the one currently on top will be its parent
                val parentId = getPreviousForum().id
                parsedForums.add(Forum(forum.id, parentId, forum.title, ""))
                forumHierarchy.push(forum)
                if (DEBUG) Timber.d(" ${forum.title} (ID:${forum.id} Parent:$parentId)")
            }
        } catch (e: ParseException) {
            Timber.e(e, "Unexpected parse failure - has the page formatting changed?")
        }

    }


    // Regex for identifying a forum title and extracting its indent(if any) and title content
    // format is [optional indent dashes] [required space] [required title text]
    private val forumTitleRegex = Regex("""^(-*) +(.+)""")

    /**
     * Parse forum details from an item in the dropdown picker.
     *
     * This will return null for non-standard forum items (e.g. Private Messages) and things like
     * dividers. Depth is signified by the number of dashes preceding the title - right now this is
     * 2 per level (so "---- Forum 2" is below "-- Forum 1") but this could change, so we just use the
     * raw value and treat the numbers as relative (if it's equal it's the same level, if it's bigger
     * it's a lower level, etc)
     */
    @Throws(ParseException::class)
    private fun parseForum(dropdownItem: Element): ParsedForum? {
        // we need the raw text so we can preserve the leading space that identifies sections like Main
        val rawTitle = dropdownItem.textNodes().firstOrNull()?.wholeText
                ?: throw ParseException("Couldn't get TextNode for title", 0)
        // we're expecting leading dashes followed by a space, and then some characters
        val result = forumTitleRegex.find(rawTitle)
                ?: return null
        // get the forum ID - this will fail for non-forums ("Private Messages" etc) as they have non-int IDs,
        // which is why we do this after verifying it is a forum
        val idString = dropdownItem.`val`()
        val forumId = idString.toIntOrNull()
                ?: throw ParseException("Can't parse forum ID as int! Got: $idString", 0)
        return result.destructured.let { (dashes, title) -> ParsedForum(forumId, title, depth = dashes.length) }
    }


    override fun buildForumStructure(): ForumStructure =
            ForumStructure.buildFromOrderedList(parsedForums, ForumRepository.TOP_LEVEL_PARENT_ID)

    private data class ParsedForum(val id: Int, val title: String, val depth: Int)
}


