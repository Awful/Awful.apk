package com.ferg.awfulapp.forums

import android.util.SparseArray
import kotlin.collections.mutableListOf

/**
 * Created by baka kaba on 04/04/2016.
 *
 * Immutable class representing a Forum, including references to any subforums
 */


enum class ForumType {
    FORUM, SECTION, BOOKMARKS
}

class Forum (
    val id: Int,
    val parentId: Int,
    var title: String?,
    var subtitle: String?,
    val subforums: MutableList<Forum>,
    var tagUrl: String? = null,
    var isFavourite: Boolean = false,
    var type: ForumType = ForumType.FORUM
) {

    constructor(id: Int, parentId: Int, title: String?, subtitle: String?) :
            this(id, parentId, title, subtitle, mutableListOf())

    // Copy constructor (omitting subforums)
    constructor(sourceForum: Forum) : this(
        id = sourceForum.id,
        parentId = sourceForum.parentId,
        title = sourceForum.title,
        subtitle = sourceForum.subtitle,
        subforums = mutableListOf(),
        tagUrl = sourceForum.tagUrl,
        type = sourceForum.type,
        isFavourite = sourceForum.isFavourite
    )

    fun isType( forumType: ForumType): Boolean {
        return forumType == type
    }

    /**
     * Get this forum's abbreviated name, as overlaid on its tag on the website.
     *
     * @return its tag text, or a generated abbreviation
     */
    val abbreviation: String
        get() = forumAbbreviations.get(id, abbreviateTitle())

    /**
     * Generate an abbreviated version of this forum's title.
     *
     * @return the result, or an empty string if it couldn't be abbreviated
     */
    private fun abbreviateTitle(): String {
        var cleanTitle = title?.replace(Regex("[^A-Za-z0-9/ :&]"), "") ?: ""
        if (cleanTitle.contains(":")) {
            val firstPart = cleanTitle.split(":")[0]
            if (firstPart.length > 6) {
                cleanTitle = firstPart
            } else {
                return cleanTitle.split(":")[0]
            }
        }

        val words = cleanTitle.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val sb = StringBuilder()
        for (word in words) {
            if (word.isNotEmpty()) {
                if (word.matches(Regex("^\\d{0,3}$"))) {
                    sb.append(word)
                } else {
                    sb.append(word[0].uppercaseChar())
                }
            }
        }
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Forum) {
            return false
        }
        return other.id == id &&
                other.parentId == parentId &&
                other.title == title &&
                other.subtitle == subtitle
    }

    companion object  {

        private val forumAbbreviations = SparseArray<String>().apply {
            append(692, "1999")
            append(273, "GBS")
            append(26, "FYAD")
            append(268, "BYOB")
            append(272, "RSF")
            append(242, "P/C")

            append(44, "GAMES")
            append(46, "D&D")
            append(269, "C-SPAM")
            append(167, "PYF")
            append(158, "A/T")
            append(22, "SH/SC")
            append(192, "IYG")
            append(122, "SAS")
            append(179, "YLLS")
            append(161, "GWS")
            append(91, "AI")
            append(210, "DIY")
            append(124, "PI")
            append(132, "TFR")
            append(90, "TCC")
            append(218, "GIP")

            append(31, "CC")
            append(151, "CD")
            append(182, "TBB")
            append(150, "NMD")
            append(130, "TVIV")
            append(144, "BSS")
            append(27, "ADTRW")
            append(215, "PHIZ")
            append(255, "RGD")

            append(61, "SAMART")
            append(43, "GM")
            append(241, "LAN")
            append(188, "QCS")

            append(21, "55555")
            append(25, "11111")
            append(1, "RIP")
        }
    }
}