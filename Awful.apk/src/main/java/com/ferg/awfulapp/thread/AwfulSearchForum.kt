package com.ferg.awfulapp.thread

import org.jsoup.nodes.Document

/**
 * Created by Christoph on 30.11.2015.
 */
class AwfulSearchForum {
    var forumId: Int = 0
    var forumName: String? = null
    var isChecked: Boolean = false
    var depth: Int = 0
    var parents: MutableSet<String> = mutableSetOf()

    companion object {
        fun parseSearchForums(doc: Document): ArrayList<AwfulSearchForum> {
            val searchForums = ArrayList<AwfulSearchForum>()

            val forumListContainer = doc.getElementsByClass("forumlist_container").first() ?: return searchForums
            val forumLists = forumListContainer.children()
            for (forumList in forumLists) {
                val forums = forumList.getElementsByClass("search_forum")
                for (forum in forums) {
                    val searchForum = AwfulSearchForum()
                    searchForum.isChecked = forum.hasClass("checked")
                    searchForum.forumId = forum.attr("data-forumid").toInt()
                    var forumName = forum.text()
                    if (forum.hasClass("depth1")) {
                        forumName = " $forumName"
                        searchForum.depth = 1
                    } else if (forum.hasClass("depth2")) {
                        forumName = "  $forumName"
                        searchForum.depth = 2
                    } else if (forum.hasClass("depth3")) {
                        forumName = "   $forumName"
                        searchForum.depth = 3
                    } else {
                        searchForum.depth = 0
                    }
                    searchForum.forumName = forumName

                    for (className in forum.classNames()) {
                        if (className.startsWith("parent")) {
                            searchForum.parents.add(className)
                        }
                    }
                    searchForums.add(searchForum)
                }
            }

            return searchForums
        }
    }
}
