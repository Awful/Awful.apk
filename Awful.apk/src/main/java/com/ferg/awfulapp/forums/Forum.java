package com.ferg.awfulapp.forums;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by baka kaba on 04/04/2016.
 * <p/>
 * Immutable class representing a Forum, including references to any subforums
 */
@SuppressWarnings("SpellCheckingInspection")
public class Forum {

    @NonNull
    public final String title;
    @NonNull
    public final String subtitle;
    public final int id;
    public final int parentId;
    @NonNull
    public final List<Forum> subforums;

    @Nullable
    private String tagUrl = null;

    @ForumType
    private int type = FORUM;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FORUM, SECTION, BOOKMARKS})
    public @interface ForumType {
    }

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
     *
     * @param sourceForum The Forum object to copy
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


    /**
     * Get this forum's abbreviated name, as overlaid on its tag on the website.
     *
     * @return its tag text, or an empty string
     */
    @NonNull
    public String getAbbreviation() {
        return forumAbbreviations.get(id, "");
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


    private static final SparseArray<String> forumAbbreviations = new SparseArray<>();

    static {
        forumAbbreviations.append(273, "GBS");
        forumAbbreviations.append(26, "FYAD");
        forumAbbreviations.append(268, "BYOB");
        forumAbbreviations.append(272, "RSF");
        forumAbbreviations.append(242, "P/C");

        forumAbbreviations.append(44, "GAMES");
        forumAbbreviations.append(46, "D&D");
        forumAbbreviations.append(167, "PYF");
        forumAbbreviations.append(158, "A/T");
        forumAbbreviations.append(22, "SH/SC");
        forumAbbreviations.append(192, "IYG");
        forumAbbreviations.append(122, "SAS");
        forumAbbreviations.append(179, "YLLS");
        forumAbbreviations.append(161, "GWS");
        forumAbbreviations.append(91, "AI");
        forumAbbreviations.append(210, "DIY");
        forumAbbreviations.append(124, "PI");
        forumAbbreviations.append(132, "TFR");
        forumAbbreviations.append(90, "TCC");
        forumAbbreviations.append(218, "GIP");

        forumAbbreviations.append(31, "CC");
        forumAbbreviations.append(151, "CD");
        forumAbbreviations.append(182, "TBB");
        forumAbbreviations.append(150, "NMD");
        forumAbbreviations.append(130, "TVIV");
        forumAbbreviations.append(144, "BSS");
        forumAbbreviations.append(27, "ADTRW");
        forumAbbreviations.append(215, "PHIZ");
        forumAbbreviations.append(255, "RGD");

        forumAbbreviations.append(61, "SAMART");
        forumAbbreviations.append(43, "GM");
        forumAbbreviations.append(241, "LAN");
        forumAbbreviations.append(188, "QCS");

        forumAbbreviations.append(21, "55555");
        forumAbbreviations.append(25, "11111");
        forumAbbreviations.append(1, "RIP");
    }
}
