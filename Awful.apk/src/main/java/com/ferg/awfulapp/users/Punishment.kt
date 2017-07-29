package com.ferg.awfulapp.users

import android.net.Uri
import com.ferg.awfulapp.constants.Constants
import com.ferg.awfulapp.util.AwfulParseException
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag

/**
 * Created by baka kaba on 28/07/2017.
 *
 * Represents the punishment details from a rap sheet / leper's colony entry.
 */
class Punishment private constructor(val type: Type, val badPostUrl: String?, val badPoster: User,
                                     val requestedBy: User, val approvedBy: User,
                                     val reasonHtml: String = "-no details-", val timestamp: String) {

    companion object {

        /**
         * Generate a Punishment by parsing a row from a rap sheet or leper's colony table.
         * @param entry the <tr> element holding the row
         */
        fun parse(entry: Element): Punishment {
            // we're only handling the expected layout - a row of 6 cells, each holding a specific bit of data
            if (entry.tag() != Tag.valueOf("tr"))
                throw AwfulParseException("Expected a <tr> element, got: ${entry.tag()}")

            val items = entry.select("td")
            if (items.size != 6) throw AwfulParseException("Expected 6 columns, found ${items.size}")

            // parse all the things - these are all expected to work, an exception is thrown if there's a problem (e.g. change in the page structure)
            // sometimes the BAN etc column is plain text instead of a link (deleted post?) - that's why we're allowing null for the url part
            val (badPostUrl, badPostType) = items[0].findHypertext(false)
            val timestamp = items[1].text()
            val (badPosterUrl, badPoster) = items[2].findHypertext()
            val reason = items[3].html()
            val (requestedByUrl, requestedBy) = items[4].findHypertext()
            val (approvedByUrl, approvedBy) = items[5].findHypertext()

            // the non-null assertions here are safe, because #findHypertext would have thrown a parse exception
            return Punishment(
                    type = parseType(badPostType),
                    badPostUrl = badPostUrl,
                    badPoster = User(badPosterUrl!!.userId(), badPoster),
                    requestedBy = User(requestedByUrl!!.userId(), requestedBy),
                    approvedBy = User(approvedByUrl!!.userId(), approvedBy),
                    reasonHtml = reason,
                    timestamp = timestamp
            )
        }

        private fun parseType(name: String): Type =
                try { Type.valueOf(name.toUpperCase()) } catch (e: IllegalArgumentException) { Type.UNKNOWN }

        // TODO: better handling of parse errors? defaulting to an ID of -1 isn't great
        private fun String.userId() = Uri.parse(this).getQueryParameter(Constants.PARAM_USER_ID)?.toIntOrNull() ?: -1

        private fun Element.findHypertext(linkRequired: Boolean = true): Hypertext {
            val link = selectFirst("a")?.attr("href") ?: if (!linkRequired) null else throw AwfulParseException("Failed to find link in: ${html()}")
            return Hypertext(link, text())
        }

        private data class Hypertext(val url: String?, val text: String)
    }

    /**
     * Represents a type of punishment - bans etc.
     * [icon] holds the name of a drawable resource to display
     */
    enum class Type(val icon: String) {
        PROBATION("lc_probation.png"),
        BAN("lc_ban.png"),
        PERMABAN("lc_permaban.png"),
        AUTOBAN("lc_autoban.png"),
        UNKNOWN("mods_expert.png")
    }
}
