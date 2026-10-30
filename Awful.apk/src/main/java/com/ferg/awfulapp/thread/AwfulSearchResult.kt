package com.ferg.awfulapp.thread

import android.text.TextUtils
import org.jsoup.nodes.Document

/**
 * Created by Christoph on 29.12.2015.
 */
class AwfulSearchResult {
    var resultsFound: Boolean = false
    var queryId: Int = 0
    var pages: Int = 0
    var resultList: MutableList<AwfulSearch> = mutableListOf()

    companion object {
        fun parseSearch(doc: Document): AwfulSearchResult {
            val result = AwfulSearchResult()
            /* If no results, then no class="this_page" or class="last_page" elements.
             *  If one page of results, class="this_page" but no class="last_page" element.
             *  If more than one page of results, both class="this_page" and class="last_page" elements.
             */
            result.resultsFound = doc.getElementsByClass("this_page").first() != null
            val lastPage = doc.getElementsByClass("last_page").first()
            if (lastPage != null) {
                val link = lastPage.child(0)
                val params = TextUtils.split(link.attr("href"), "&")
                result.queryId = TextUtils.split(params[1], "=")[1].toInt()
                result.pages = TextUtils.split(params[2], "=")[1].toInt()
            } else {
                result.pages = 1
                /* If there is only one page of results, there are no elements that allow us to scrape
                 *  a query ID from the response body itself. */
                result.queryId = 0
            }
            return result
        }
    }
}
