package com.ferg.awfulapp.thread;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Christoph on 30.11.2015.
 */
public class AwfulSearchForum {

    private int mForumId;
    private String mForumName;
    private boolean mChecked;
    private int mDepth;
    private Set<String> mParents = new HashSet<>();

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

    public Set<String> getParents() {
        return mParents;
    }

    public void setParents(Set<String> aParents) {
        this.mParents = aParents;
    }

    public int getDepth() {
        return mDepth;
    }

    public void setDepth(int aDepth) {
        this.mDepth = aDepth;
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
                    String forumName = forum.text();
                    if(forum.hasClass("depth1")){
                        forumName = " "+ forumName;
                        searchForum.setDepth(1);
                    }else if(forum.hasClass("depth2")){
                        forumName = "  "+ forumName;
                        searchForum.setDepth(2);
                    }else if(forum.hasClass("depth3")){
                        forumName = "   "+ forumName;
                        searchForum.setDepth(3);
                    }else{
                        searchForum.setDepth(0);
                    }
                    searchForum.setForumName(forumName);

                    for (String className : forum.classNames()){
                        if(className.startsWith("parent")){
                            searchForum.getParents().add(className);
                        }
                    }
                    searchForums.add(searchForum);
                }
            }
        }

        return searchForums;
    }
}
