package com.ferg.awfulapp.task

import android.content.Context
import com.android.volley.NetworkResponse
import com.ferg.awfulapp.constants.Constants.BASE_URL
import com.ferg.awfulapp.constants.Constants.SITE_HTML_ENCODING
import com.ferg.awfulapp.util.AwfulError
import cz.msebera.android.httpclient.entity.ContentType
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
        val contentType = response.headers?.get("content-type")
        var charset = SITE_CHARSET;
        if (contentType != null) {
            charset = ContentType.parse(contentType).charset
        }
        Timber.d("Stripping page selectors from HTML to speed up parsing")
        // grab the data as a string, and match the select blocks
        val html = String(response.data, charset)

        // now dump the select blocks and parse what's left
        val smaller = pageSelectorRegex.replace(html, "")
        Timber.d("Garbage stripped (took ${startTime.elapsed}ms) - starting Jsoup parse")
        val jsoupParseStart = System.currentTimeMillis()
        val jsoupResponse = Jsoup.parse(smaller, BASE_URL).also {
            Timber.d("jsoup parsing finished (took ${jsoupParseStart.elapsed}ms)")
        }
        val pages = jsoupResponse.getElementsByClass("pages").first()
        if (pages != null) {
            selectedPage = pages.dataset().getValue("current-page").toInt()
            lastPage = pages.dataset().getValue("total-pages").toInt()
        }
        return jsoupResponse;
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
    }
}
