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

    int queryId;
    int pages;
    ArrayList<AwfulSearch> resultList;

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
        Element lastPage = doc.getElementsByClass("last_page").first();
        if(lastPage != null){
            Element link = lastPage.child(0);
            String[] params = TextUtils.split(link.attr("href"),"&");
            result.setQueryId(Integer.parseInt(TextUtils.split(params[1],"=")[1]));
            result.setPages(Integer.parseInt(TextUtils.split(params[2],"=")[1]));
        }else{
            result.setPages(1);
            result.setQueryId(0);
        }
        return result;
    }
}
