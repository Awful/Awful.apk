package com.ferg.awfulapp.thread

import com.ferg.awfulapp.thread.AwfulForum.getForumId
import org.jsoup.nodes.Document

/**
 * Created by Christoph on 29.11.2015.
 */
class AwfulSearch {
    var resultNumber: String? = null
    var username: String? = null
    var threadLink: String? = null
    var threadTitle: String? = null
    var forumId: Int = 0
    var forumTitle: String? = null
    var postDate: String? = null
    var blurb: String? = null


    companion object {
        private const val TAG = "AwfulSearch"

        fun parseSearchResult(aSearchRequest: Document): MutableList<AwfulSearch> {
            val result = mutableListOf<AwfulSearch>()

            val searchResultContainer = aSearchRequest.getElementById("search_results")
            val searchResults = searchResultContainer?.getElementsByClass("search_result") ?: return result
            for (searchResult in searchResults) {
                val search = AwfulSearch()

                search.resultNumber =
                    searchResult.getElementsByClass("result_number").first()?.text()
                search.blurb = searchResult.getElementsByClass("blurb").first()?.html()

                val threadLink = searchResult.getElementsByClass("threadlink").first()
                val threadTitle = threadLink?.getElementsByClass("threadtitle")?.first()
                search.threadTitle = threadTitle?.text()
                search.threadLink = threadTitle?.attr("href")

                val hitInfo = searchResult.getElementsByClass("hit_info").first()
                search.username = hitInfo?.getElementsByClass("username")?.first()?.text()
                search.forumTitle = hitInfo?.getElementsByClass("forumtitle")?.first()?.text()
                search.forumId = getForumId(
                    hitInfo?.getElementsByClass("forumtitle")?.first()?.attr("href") ?: "-1"
                )
                search.postDate = hitInfo?.childNode(4).toString().substring(3).trim { it <= ' ' }


                result.add(search)
            }

            return result
        }
    }
}
