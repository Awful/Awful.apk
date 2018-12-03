package com.ferg.awfulapp.task;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

import static com.ferg.awfulapp.constants.Constants.BASE_URL;
import static com.ferg.awfulapp.constants.Constants.SITE_HTML_ENCODING;

/**
 * Created by baka kaba on 11/11/2018.
 *
 * Wrapper class for AwfulRequests, allowing a request to receive and handle a response with the
 * page selector elements stripped out (which can speed up HTML parsing considerably)
 *
 * Ideally this is just temporary until all the outstanding requests can be moved over to using it
 */
public abstract class AwfulStrippedRequest<T> extends AwfulRequest<T> {

    public AwfulStrippedRequest(Context context, String apiUrl) {
        super(context, apiUrl);
    }

    /**
     * Handle the HTML document parsed from the response, which has had the page selector elements
     * stripped out.
     *
     * Values for the currently selected and last page are passed in - usually this is the only
     * information you'd need from the page selectors anyway. These may be null if the values
     * couldn't be parsed, e.g. if the site's page structure changes
     * @param doc the parsed HTML document, with the page selector elements removed
     * @param currentPage the value for the current page, according to the page selector
     * @param totalPages the value for the last page number, according to the page selector
     */
    abstract T handleStrippedResponse(Document doc, @Nullable Integer currentPage, @Nullable Integer totalPages) throws AwfulError;


    // TODO: can/should this be done with the outer <div class="pages"> tag instead?
    // matches a single page select block (usually 2 on a page)
    private final static Pattern pageSelectorRegex = Pattern.compile("<select data-url=\"\\S*\\.php.*</select>");
    // matches the "value" attribute of the <option> tag with a "selected" attribute
    private final static Pattern selectedPageRegex = Pattern.compile("value=\"(\\d*)\"\\s*selected");
    // matches the inner text of the last <option> tag (I can't safely get the contents of its "value" attr without the regex exploding over backtracking)
    private final static Pattern lastPageRegex = Pattern.compile(">\\s*(\\d*)\\s*</option>\\s*</select>");

    // data pulled after stripping unwanted elements from the source HTML in #parseAsHtml
    @Nullable
    private Integer selectedPage = null;
    @Nullable
    private Integer lastPage = null;

    @Override
    protected Document parseAsHtml(@NonNull NetworkResponse response) throws IOException {
        // TODO: fall back to superclass implementation on error, set retry flag
        long startTime = System.currentTimeMillis();
        Timber.d("Stripping page selectors from HTML to speed up parsing");
        // grab the data as a string, and match the select blocks
        String data = new String(response.data, SITE_HTML_ENCODING);
        Matcher pageSelectMatcher = pageSelectorRegex.matcher(data);

        // try and pull out the useful data before we throw the blocks away
        if (pageSelectMatcher.find()) {
            // separate matchers so one can fail without breaking the other
            Matcher selectedPageMatcher = selectedPageRegex.matcher(pageSelectMatcher.group());
            Matcher lastPageMatcher = lastPageRegex.matcher(pageSelectMatcher.group());
            if (selectedPageMatcher.find()) {
                try { selectedPage = Integer.parseInt(selectedPageMatcher.group(1)); }
                catch (NumberFormatException e) { }
            }
            if (lastPageMatcher.find()) {
                try { lastPage = Integer.parseInt(lastPageMatcher.group(1)); }
                catch (NumberFormatException e) { }
            }
        }

        // now dump the select blocks and parse what's left
        String smaller = pageSelectMatcher.replaceAll("");
        Timber.d("Garbage stripped (took " + (System.currentTimeMillis() - startTime) + "ms) - starting Jsoup parse");
        long jsoupParseStart = System.currentTimeMillis();
        Document doc = Jsoup.parse(smaller, BASE_URL);
        Timber.d("Jsoup parsing finished (took " + (System.currentTimeMillis() - jsoupParseStart) + "ms)");
        return doc;
    }

    @Override
    protected T handleResponseDocument(@NonNull Document document) throws AwfulError {
        return handleStrippedResponse(document, selectedPage, lastPage);
    }
}
