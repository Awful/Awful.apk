package com.ferg.awfulapp.thread;

import android.text.TextUtils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * Created by Christoph on 29.12.2015.
 */
public class AwfulSearchResult {

    boolean resultsFound;
    int queryId;
    int pages;
    ArrayList<AwfulSearch> resultList;

    public boolean getResultsFound() { return resultsFound; }

    public void setResultsFound(boolean resultsFound) { this.resultsFound = resultsFound; }

    public int getQueryId() {
        return queryId;
    }

    public void setQueryId(int queryId) {
        this.queryId = queryId;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public ArrayList<AwfulSearch> getResultList() {
        return resultList;
    }

    public void setResultList(ArrayList<AwfulSearch> resultList) {
        this.resultList = resultList;
    }

    public static AwfulSearchResult parseSearch(Document doc) {
        AwfulSearchResult result = new AwfulSearchResult();
        /* If no results, then no class="this_page" or class="last_page" elements.
        *  If one page of results, class="this_page" but no class="last_page" element.
        *  If more than one page of results, both class="this_page" and class="last_page" elements. */
        result.setResultsFound(doc.getElementsByClass("this_page").first() != null);
        Element lastPage = doc.getElementsByClass("last_page").first();
        if(lastPage != null){
            Element link = lastPage.child(0);
            String[] params = TextUtils.split(link.attr("href"),"&");
            result.setQueryId(Integer.parseInt(TextUtils.split(params[1],"=")[1]));
            result.setPages(Integer.parseInt(TextUtils.split(params[2],"=")[1]));
        }else{
            result.setPages(1);
            /* If there is only one page of results, there are no elements that allow us to scrape
            *  a query ID from the response body itself. */
            result.setQueryId(0);
        }
        return result;
    }
}
