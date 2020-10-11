package com.ferg.awfulapp.forums;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by baka kaba on 09/04/2016.
 * <p>
 * Represents a hierarchy of forums, with methods for building the structure
 * and converting it to various customisable list formats.
 */
public class ForumStructure {

    private static final String TAG = "ForumStructure";
    private final List<Forum> forumTree;


    private ForumStructure(List<Forum> forumTree) {
        this.forumTree = forumTree;
    }


    /**
     * Create a forum structure from an ordered list of Forums, according to their {@link Forum#parentId}s.
     * <p>
     * This builds a hierarchy by comparing forum and parent IDs, and placing forums in their
     * parents' subforum group. List ordering is respected, so forums in the same group will be
     * ordered by their position in the list.
     * <p>
     * The top level consists of the forums without a parent in the list, each of which may have
     * its own tree of subforums.
     * <p>
     * You can provide an optional top-level parent ID, to only return the forums with that
     * {@link Forum#parentId}, whether the parent is present or not. This allows you to get a branch
     * of the hierarchy, e.g. the forums within a specific {@link Forum#SECTION}, or the subforums
     * of a certain forum. Passing <b>null</b> will return the whole hierarchy.
     *
     * @param orderedForums    A list of Forums in the order they should appear,
     *                         with {@link Forum#id} and {@link Forum#parentId} set
     * @param topLevelParentId optional - the parent ID of the required branch of the hierarchy.
     * @return A ForumStructure representing the finished hierarchy
     */
    @NonNull
    static ForumStructure buildFromOrderedList(List<Forum> orderedForums, @Nullable Integer topLevelParentId) {
        List<Forum> forumTree = new ArrayList<>();

        // linked hashmap so we maintain the list's ordering
        Map<Integer, Forum> forumsById = new LinkedHashMap<>();
        for (Forum forum : orderedForums) {
            Forum forumCopy = new Forum(forum);
            forumsById.put(forumCopy.id, forumCopy);
        }

        /*
            keep list of all forums with parentID = toplevelID, OR no parent in forum map
         */

        Forum parentForum;
        for (Forum forum : forumsById.values()) {
            parentForum = forumsById.get(forum.parentId);

            // check if this forum is a top-level category 'forum' like Main or Community
            if (topLevelParentId == null && parentForum == null || topLevelParentId != null && forum.parentId == topLevelParentId) {
                forumTree.add(forum);
            }
            // otherwise add the forum to its parent's subforum list
            else {
                if (parentForum != null) {
                    parentForum.subforums.add(forum);
                } else {
                    Log.w(TAG, "Unable to find parent forum with ID: " + forum.parentId);
                }
            }
        }

        return new ForumStructure(forumTree);
    }


    /**
     * Build a ForumStructure from a hierarchical tree of Forum objects.
     * <p>
     * This will treat the Forum/subforum structure as authoritative, and the Forums'
     * {@link Forum#parentId}s will be set to reflect the {@link Forum#id} of its containing Forum.
     * The order of each node list will be preserved.
     *
     * @param forumTree  A list of Forums, which in turn may contain Forums in their subforum lists
     * @param topLevelId The ID that represents the root of the hierarchy
     * @return A ForumStructure with the same hierarchy
     */
    @NonNull
    static ForumStructure buildFromTree(List<Forum> forumTree, int topLevelId) {
        List<Forum> newForumTree = new ArrayList<>();
        copyTreeWithParentId(forumTree, newForumTree, topLevelId);
        return new ForumStructure(newForumTree);
    }


    /**
     * Recursively add the contents of a tree node into another tree node, specifying a new parent ID.
     *
     * @param sourceTree      The tree to copy
     * @param destinationTree The tree to copy into
     * @param parentId        The parent ID for the new tree
     */
    private static void copyTreeWithParentId(List<Forum> sourceTree, List<Forum> destinationTree, int parentId) {
        for (Forum sourceForum : sourceTree) {
            // TODO: this is hacky, should be able to set things all at once
            Forum forumCopy = new Forum(sourceForum.id, parentId, sourceForum.title, sourceForum.subtitle);
            forumCopy.setType(sourceForum.getType());
            forumCopy.setTagUrl(sourceForum.getTagUrl());
            forumCopy.setFavourite(sourceForum.isFavourite());
            destinationTree.add(forumCopy);
            // copy this Forum's subforums, but ensure the parent IDs refer to this Forum's ID
            copyTreeWithParentId(sourceForum.subforums, forumCopy.subforums, forumCopy.id);
        }
    }


    /**
     * Get the number of forums held in this structure.
     *
     * @return The total number of forums, including section forums e.g. Main
     */
    public int getNumberOfForums() {
        return countForums(forumTree, 0);
    }

    private int countForums(List<Forum> forums, int total) {
        for (Forum forum : forums) {
            total++;
            countForums(forum.subforums, total);
        }
        return total;
    }


    ///////////////////////////////////////////////////////////////////////////
    // List builder
    ///////////////////////////////////////////////////////////////////////////

    @NonNull
    public ListBuilder getAsList() {
        return new ListBuilder();
    }

    /**
     * Output format types:
     * <ul>
     * <li>FULL_TREE - the full hierarchy</li>
     * <li>TWO_LEVEL - categories/top-level forums/bookmarks etc at the top level,
     * each with any subforums compacted into a second level</li>
     * <li>FLAT - everything on a single level</li>
     * </ul>
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FULL_TREE, TWO_LEVEL, FLAT})
    @interface ListFormat {
    }

    public static final int FULL_TREE = 0;
    public static final int TWO_LEVEL = 1;
    public static final int FLAT = 2;

    public class ListBuilder {


        private boolean includeSections = true;
        @ListFormat
        private int listFormat;


        /**
         * Include or exclude section forums (e.g. Main).
         * If sections are excluded, their immediate subforums will appear in the top level list.
         */
        @NonNull
        public ListBuilder includeSections(boolean show) {
            includeSections = show;
            return this;
        }


        /**
         * The type of list structure to produce.
         */
        @NonNull
        public ListBuilder formatAs(@ListFormat int formatType) {
            listFormat = formatType;
            return this;
        }

        @NonNull
        public List<Forum> build() {
            List<Forum> generatedList = new ArrayList<>();

            for (Forum rootForum : forumTree) {
                Forum rootForumCopy = new Forum(rootForum);
                // only include sections if required
                if (!rootForum.isType(Forum.SECTION) || includeSections) {
                    generatedList.add(rootForumCopy);
                }
                // add its subforums to the same level, or to the subforum list as appropriate
                for (Forum mainForum : rootForum.subforums) {
                    Forum forumCopy = new Forum(mainForum);
                    // the only time we don't add a top-level forum to the root list is when we're doing
                    // the full tree structure, and we're including sections (so the TLF is added as a subforum)
                    if (listFormat == FULL_TREE && includeSections) {
                        rootForumCopy.subforums.add(forumCopy);
                    } else {
                        generatedList.add(forumCopy);
                    }

                    if (listFormat == FLAT) {
                        // flat list - add main forum and everything below it to the top level
                        collectSubforums(mainForum.subforums, generatedList);
                    } else if (listFormat == TWO_LEVEL) {
                        // two-level list - add main forum to the top level, and everything below it into its subforum list
                        collectSubforums(mainForum.subforums, forumCopy.subforums);
                    } else if (listFormat == FULL_TREE) {
                        // full tree structure
                        copyForumTree(mainForum.subforums, forumCopy.subforums);
                    }
                }
            }

            return generatedList;
        }
    }


    /**
     * Recursively copy all subforums in a tree into a supplied list.
     * This maintains the hierarchy of the subforums and their descendants
     *
     * @param source     The source tree, whose hierarchy will be traversed
     * @param collection A list to collect all the subforum objects in
     */
    private static void copyForumTree(List<Forum> source, List<Forum> collection) {
        Forum forumCopy;
        for (Forum forum : source) {
            forumCopy = new Forum(forum);
            collection.add(forumCopy);
            copyForumTree(forum.subforums, forumCopy.subforums);
        }
    }


    /**
     * Recursively copy all subforums in a tree into a flat list.
     *
     * @param source     The source tree, whose hierarchy will be traversed
     * @param collection A list to collect all the subforum objects in
     */
    private static void collectSubforums(List<Forum> source, List<Forum> collection) {
        for (Forum forum : source) {
            collection.add(new Forum(forum));
            collectSubforums(forum.subforums, collection);
        }
    }


}
