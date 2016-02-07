package com.ferg.awfulapp.thread;

import android.util.Log;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

/**
 * Created by Christoph on 29.11.2015.
 */
public class AwfulSearch {
    private static final String TAG = "AwfulSearch";

    private String mResultNumber;
    private String mUsername;
    private String mThreadLink;
    private String mThreadTitle;
    private int mForumId;
    private String mForumTitle;
    private String mPostDate;
    private String mBlurb;


    public static ArrayList<AwfulSearch> parseSearchResult(Document aSearchRequest){
        ArrayList<AwfulSearch> result = new ArrayList<>();

        Element searchResultContainer = aSearchRequest.getElementById("search_results");
        Elements searchResults = searchResultContainer.getElementsByClass("search_result");
        for(Element searchResult : searchResults){
            AwfulSearch search = new AwfulSearch();

            search.setResultNumber(searchResult.getElementsByClass("result_number").first().text());
            search.setBlurb(searchResult.getElementsByClass("blurb").first().html());


            Element threadTitle = searchResult.getElementsByClass("threadlink").first().getElementsByClass("threadtitle").first();
            search.setThreadTitle(threadTitle.text());
            search.setThreadLink(threadTitle.attr("href"));

            Element hitInfo = searchResult.getElementsByClass("hit_info").first();
            search.setUsername(hitInfo.getElementsByClass("username").first().text());
            search.setForumTitle(hitInfo.getElementsByClass("forumtitle").first().text());
            search.setForumId(AwfulForum.getForumId(hitInfo.getElementsByClass("forumtitle").first().attr("href")));
            search.setPostDate(hitInfo.childNode(4).toString().substring(3));


            result.add(search);
        }

        return result;
    }

    public String getResultNumber() {
        return mResultNumber;
    }

    public void setResultNumber(String aResultNumber) {
        this.mResultNumber = aResultNumber;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String aUsername) {
        this.mUsername = aUsername;
    }

    public String getThreadLink() {
        return mThreadLink;
    }

    public void setThreadLink(String aThreadLink) {
        this.mThreadLink = aThreadLink;
    }

    public String getThreadTitle() {
        return mThreadTitle;
    }

    public void setThreadTitle(String aThreadTitle) {
        this.mThreadTitle = aThreadTitle;
    }

    public int getForumId() {
        return mForumId;
    }

    public void setForumId(int aForumId) {
        this.mForumId = aForumId;
    }

    public String getForumTitle() {
        return mForumTitle;
    }

    public void setForumTitle(String aForumTitle) {
        this.mForumTitle = aForumTitle;
    }

    public String getPostDate() {
        return mPostDate;
    }

    public void setPostDate(String aPostDate) {
        this.mPostDate = aPostDate;
    }

    public String getBlurb() {
        return mBlurb;
    }

    public void setBlurb(String aBlurb) {
        this.mBlurb = aBlurb;
    }
}
