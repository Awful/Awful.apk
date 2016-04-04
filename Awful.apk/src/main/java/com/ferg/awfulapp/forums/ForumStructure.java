package com.ferg.awfulapp.forums;

import android.support.annotation.IntDef;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by baka kaba on 09/04/2016.
 *
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
     * Build a ForumStructure from an ordered list of Forum elements.
     * This will build a hierarchical tree based on the IDs and parent IDs of the Forums,
     * and the elements at each node will be in the order they appeared in the list.
     *
     * Use this to turn a collection of Forum items (in indexed order) into an ordered tree.
     * Only the Forums in the supplied list will be added - their subforum fields will be ignored.
     *
     * The topLevelId represents the parentID of the top level forums - any Forums with this as
     * their {@link Forum#parentId} will appear at the top of the hierarchy. If a Forum has a
     * parentId which doesn't match another Forum's id, and it isn't the topLevelId, that Forum is
     * effectively orphaned and will be excluded (along with its subforums) from the resulting ForumStructure.
     * This can be used to select a small branch of the tree (only the subforums of a given forum)
     *
     * @param orderedForums A list of Forums in the order they should appear,
     *                      with {@link Forum#id} and {@link Forum#parentId} set
     * @param topLevelId    The ID that represents the root of the hierarchy
     * @return              A ForumStructure representing the supplied forums
     */
    public static ForumStructure buildFromOrderedList(List<Forum> orderedForums, int topLevelId) {
        List<Forum> forumTree = new ArrayList<>();

        // linked hashmap so we maintain the list's ordering
        Map<Integer, Forum> forumsById = new LinkedHashMap<>();
        for (Forum forum : orderedForums) {
            Forum forumCopy = new Forum(forum);
            forumsById.put(forumCopy.id, forumCopy);
        }

        Forum parentForum;
        for (Forum forum : forumsById.values()) {
            // check if this forum is a top-level category 'forum' like Main or Community
            if (forum.parentId == topLevelId) {
                forumTree.add(forum);
            }
            // otherwise add the forum to its parent's subforum list
            else {
                parentForum = forumsById.get(forum.parentId);
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
     *
     * This will treat the Forum/subforum structure as authoritative, and the Forums'
     * {@link Forum#parentId}s will be set to reflect the {@link Forum#id} of its containing Forum.
     * The order of each node list will be preserved.
     *
     * @param forumTree     A list of Forums, which in turn may contain Forums in their subforum lists
     * @param topLevelId    The ID that represents the root of the hierarchy
     * @return              A ForumStructure with the same hierarchy
     */
    public static ForumStructure buildFromTree(List<Forum> forumTree, int topLevelId) {
        List<Forum> newForumTree = new ArrayList<>();
        copyTreeWithParentId(forumTree, newForumTree, topLevelId);
        return new ForumStructure(newForumTree);
    }


    /**
     * Recursively add the contents of a tree node into another tree node, specifying a new parent ID.
     * @param sourceTree        The tree to copy
     * @param destinationTree   The tree to copy into
     * @param parentId          The parent ID for the new tree
     */
    private static void copyTreeWithParentId(List<Forum> sourceTree, List<Forum> destinationTree, int parentId) {
        for (Forum sourceForum : sourceTree) {
            // TODO: this is hacky, should be able to set things all at once
            Forum forumCopy = new Forum(sourceForum.id, parentId, sourceForum.title, sourceForum.subtitle);
            forumCopy.setType(sourceForum.getType());
            destinationTree.add(forumCopy);
            // copy this Forum's subforums, but ensure the parent IDs refer to this Forum's ID
            copyTreeWithParentId(sourceForum.subforums, forumCopy.subforums, forumCopy.id);
        }
    }


    /**
     * Get the number of forums held in this structure.
     * @return  The total number of forums, including section forums e.g. Main
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

    public ListBuilder getAsList() {
        return new ListBuilder();
    }

    /**
     * Output format types:
     * <ul>
     *     <li>FULL_TREE - the full hierarchy</li>
     *     <li>TWO_LEVEL - categories/top-level forums/bookmarks etc at the top level,
     *     each with any subforums compacted into a second level</li>
     *     <li>FLAT - everything on a single level</li>
     * </ul>
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FULL_TREE, TWO_LEVEL, FLAT})
    @interface ListFormat {}
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
        public ListBuilder includeSections(boolean show) {
            includeSections = show;
            return this;
        }


        /**
         * The type of list structure to produce.
         */
        public ListBuilder formatAs(@ListFormat int formatType) {
            listFormat = formatType;
            return this;
        }

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
     * @param source        The source tree, whose hierarchy will be traversed
     * @param collection    A list to collect all the subforum objects in
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
     * @param source        The source tree, whose hierarchy will be traversed
     * @param collection    A list to collect all the subforum objects in
     */
    private static void collectSubforums(List<Forum> source, List<Forum> collection) {
        for (Forum forum : source) {
            collection.add(new Forum(forum));
            collectSubforums(forum.subforums, collection);
        }
    }


}
