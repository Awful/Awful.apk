package com.ferg.awfulapp.forums;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by baka kaba on 04/04/2016.
 *
 * Immutable class representing a Forum, including references to any subforums
 */
public class Forum {

    @NonNull
    public final String title;
    @NonNull
    public final String subtitle;
    public final int id;
    public final int parentId;
    @NonNull
    public final List<Forum> subforums;

    @Nullable private String tagUrl = null;

    @ForumType
    private int type = FORUM;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FORUM, SECTION, BOOKMARKS})
    public @interface ForumType {}
    public static final int FORUM = 0;
    public static final int SECTION = 1;
    public static final int BOOKMARKS = 2;




    Forum(int id, int parentId, String title, String subtitle) {
        this(id, parentId, title, subtitle, new ArrayList<Forum>());
    }

    Forum(int id, int parentId, String title, String subtitle,
          @NonNull List<Forum> subforums) {
        this.id = id;
        this.parentId = parentId;
        this.title = (title == null) ? "" : title;
        this.subtitle = (subtitle == null) ? "" : subtitle;
        this.subforums = subforums;
    }

    /**
     * Copy the supplied Forum, omitting its subfolders
     * @param sourceForum   The Forum object to copy
     */
    Forum(Forum sourceForum) {
        this(sourceForum.id, sourceForum.parentId, sourceForum.title, sourceForum.subtitle);
        setTagUrl(sourceForum.getTagUrl());
        setType(sourceForum.type);
    }


    public void setType(@ForumType int forumType) {
        type = forumType;
    }

    @ForumType
    public int getType() {
        return type;
    }


    public boolean isType(@ForumType int forumType) {
        return forumType == type;
    }


    public void setTagUrl(@Nullable String url) {
        tagUrl = url;
    }

    @Nullable
    public String getTagUrl() {
        return tagUrl;
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Forum)) {
            return false;
        }
        Forum other = (Forum) o;
        return other.id == id
                && other.parentId == parentId
                && other.title.equals(title)
                && other.subtitle.equals(subtitle);
    }
}
