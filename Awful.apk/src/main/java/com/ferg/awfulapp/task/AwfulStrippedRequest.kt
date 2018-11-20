package com.ferg.awfulapp.task

import android.content.Context
import com.android.volley.NetworkResponse
import com.ferg.awfulapp.constants.Constants.BASE_URL
import com.ferg.awfulapp.constants.Constants.SITE_HTML_ENCODING
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber
import java.io.IOException
import java.nio.charset.Charset

/**
 * Created by baka kaba on 11/11/2018.
 *
 * Wrapper class for AwfulRequests, allowing a request to receive and handle a response with the
 * page selector elements stripped out (which can speed up HTML parsing considerably)
 *
 * Ideally this is just temporary until all the outstanding requests can be moved over to using it
 */
abstract class AwfulStrippedRequest<T>(context: Context, apiUrl: String) : AwfulRequest<T>(context, apiUrl) {

    // data pulled after stripping unwanted elements from the source HTML in #parseAsHtml
    private var selectedPage: Int? = null
    private var lastPage: Int? = null

    /**
     * Handle the HTML [document] parsed from the response, which has had the page selector elements
     * stripped out.
     *
     * Values for the currently selected and last page are passed in - usually this is the only
     * information you'd need from the page selectors anyway. These may be null if the values
     * couldn't be parsed, e.g. if the site's page structure changes
     * @param currentPage the value for the current page, according to the page selector
     * @param totalPages the value for the last page number, according to the page selector
     */
    @Throws(AwfulError::class)
    internal abstract fun handleStrippedResponse(document: Document, currentPage: Int?, totalPages: Int?): T

    @Throws(IOException::class)
    override fun parseAsHtml(response: NetworkResponse): Document {
        // TODO: fall back to superclass implementation on error, set retry flag
        val startTime = System.currentTimeMillis()
        Timber.d("Stripping page selectors from HTML to speed up parsing")
        // grab the data as a string, and match the select blocks
        val html = String(response.data, SITE_CHARSET)

        // try and pull out the useful data before we throw the blocks away
        pageSelectorRegex.find(html)?.value?.let { selectBlock ->
            // separate matchers so one can fail without breaking the other
            selectedPage = selectedPageRegex.find(selectBlock)?.tryParseInt()
            lastPage = lastPageRegex.find(selectBlock)?.tryParseInt()
        }

        // now dump the select blocks and parse what's left
        val smaller = pageSelectorRegex.replace(html, "")
        Timber.d("Garbage stripped (took ${startTime.elapsed}ms) - starting Jsoup parse")
        val jsoupParseStart = System.currentTimeMillis()
        return Jsoup.parse(smaller, BASE_URL).also {
            Timber.d("jsoup parsing finished (took ${jsoupParseStart.elapsed}ms)")
        }
    }

    private val Long.elapsed get() = System.currentTimeMillis() - this
    private fun MatchResult.tryParseInt() = this.groupValues[1].toIntOrNull()

    @Throws(AwfulError::class)
    override fun handleResponseDocument(document: Document): T {
        return handleStrippedResponse(document, selectedPage, lastPage)
    }

    companion object {
        private val SITE_CHARSET = Charset.forName(SITE_HTML_ENCODING)

        // TODO: can/should this be done with the outer <div class="pages"> tag instead?
        // matches a single page select block (usually 2 on a page)
        private val pageSelectorRegex = Regex("""<select data-url="\S*\.php.*</select>""")
        // matches the "value" attribute of the <option> tag with a "selected" attribute
        private val selectedPageRegex = Regex("""value="(\d*)"\s*selected""")
        // matches the inner text of the last <option> tag
        // (I can't safely get the contents of its "value" attr without the regex exploding over backtracking)
        private val lastPageRegex = Regex(""">\s*(\d*)\s*</option>\s*</select>""")
    }
}
