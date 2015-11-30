package com.ferg.awfulapp.thread;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

/**
 * Created by Christoph on 30.11.2015.
 */
public class AwfulSearchForum {

    private int mForumId;
    private String mForumName;
    private boolean mChecked;

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean aChecked) {
        this.mChecked = aChecked;
    }

    public int getForumId() {
        return mForumId;
    }

    public void setForumId(int aForumId) {
        this.mForumId = aForumId;
    }

    public String getForumName() {
        return mForumName;
    }

    public void setForumName(String aForumName) {
        this.mForumName = aForumName;
    }

    public static ArrayList<AwfulSearchForum> parseSearchForums(Document doc) {
        ArrayList<AwfulSearchForum> searchForums = new ArrayList<>();

        Element forumListContainer = doc.getElementsByClass("forumlist_container").first();
        if(forumListContainer != null){
            Elements forumlists = forumListContainer.children();
            for (Element forumlist: forumlists){
                Elements forums = forumlist.getElementsByClass("search_forum");
                for (Element forum: forums){
                    AwfulSearchForum searchForum =  new AwfulSearchForum();
                    searchForum.setChecked(forum.hasClass("checked"));
                    searchForum.setForumId(Integer.parseInt(forum.attr("data-forumid")));
                    searchForum.setForumName(forum.text());
                    searchForums.add(searchForum);
                }
            }
        }

        return searchForums;
    }
}
