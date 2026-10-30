package com.ferg.awfulapp.forums

import android.util.Log

/**
 * Created by baka kaba on 09/04/2016.
 * 
 * 
 * Represents a hierarchy of forums, with methods for building the structure
 * and converting it to various customisable list formats.
 */
class ForumStructure private constructor(private val forumTree: MutableList<Forum>) {


    companion object {
        private const val TAG = "ForumStructure"

        /**
         * Create a forum structure from an ordered list of Forums, according to their [Forum.parentId]s.
         *
         *
         * This builds a hierarchy by comparing forum and parent IDs, and placing forums in their
         * parents' subforum group. List ordering is respected, so forums in the same group will be
         * ordered by their position in the list.
         *
         *
         * The top level consists of the forums without a parent in the list, each of which may have
         * its own tree of subforums.
         *
         *
         * You can provide an optional top-level parent ID, to only return the forums with that
         * [Forum.parentId], whether the parent is present or not. This allows you to get a branch
         * of the hierarchy, e.g. the forums within a specific [Forum.SECTION], or the subforums
         * of a certain forum. Passing **null** will return the whole hierarchy.
         *
         * @param orderedForums    A list of Forums in the order they should appear,
         * with [Forum.id] and [Forum.parentId] set
         * @param topLevelParentId optional - the parent ID of the required branch of the hierarchy.
         * @return A ForumStructure representing the finished hierarchy
         */
        fun buildFromOrderedList(
            orderedForums: MutableList<Forum?>,
            topLevelParentId: Int?
        ): ForumStructure {
            val forumTree: MutableList<Forum> = ArrayList()

            // linked hashmap so we maintain the list's ordering
            val forumsById: MutableMap<Int?, Forum> = LinkedHashMap()
            for (forum in orderedForums) {
                forum?.let {
                    val forumCopy = Forum(it)
                    forumsById.put(forumCopy.id, forumCopy)
                }
            }

            /*
            keep list of all forums with parentID = toplevelID, OR no parent in forum map
         */
            var parentForum: Forum?
            for (forum in forumsById.values) {
                parentForum = forumsById[forum.parentId]

                // check if this forum is a top-level category 'forum' like Main or Community
                if (topLevelParentId == null && parentForum == null || topLevelParentId != null && forum.parentId == topLevelParentId) {
                    forumTree.add(forum)
                } else {
                    if (parentForum != null) {
                        parentForum.subforums.add(forum)
                    } else {
                        Log.w(TAG, "Unable to find parent forum with ID: " + forum.parentId)
                    }
                }
            }

            return ForumStructure(forumTree)
        }


        /**
         * Build a ForumStructure from a hierarchical tree of Forum objects.
         *
         *
         * This will treat the Forum/subforum structure as authoritative, and the Forums'
         * [Forum.parentId]s will be set to reflect the [Forum.id] of its containing Forum.
         * The order of each node list will be preserved.
         *
         * @param forumTree  A list of Forums, which in turn may contain Forums in their subforum lists
         * @param topLevelId The ID that represents the root of the hierarchy
         * @return A ForumStructure with the same hierarchy
         */
        fun buildFromTree(forumTree: MutableList<Forum>, topLevelId: Int): ForumStructure {
            val newForumTree: MutableList<Forum> = ArrayList()
            copyTreeWithParentId(forumTree, newForumTree, topLevelId)
            return ForumStructure(newForumTree)
        }


        /**
         * Recursively add the contents of a tree node into another tree node, specifying a new parent ID.
         *
         * @param sourceTree      The tree to copy
         * @param destinationTree The tree to copy into
         * @param parentId        The parent ID for the new tree
         */
        private fun copyTreeWithParentId(
            sourceTree: MutableList<Forum>,
            destinationTree: MutableList<Forum>,
            parentId: Int
        ) {
            for (sourceForum in sourceTree) {
                // TODO: this is hacky, should be able to set things all at once
                val forumCopy =
                    Forum(sourceForum.id, parentId, sourceForum.title, sourceForum.subtitle)
                forumCopy.type = sourceForum.type
                forumCopy.tagUrl = sourceForum.tagUrl
                forumCopy.isFavourite = sourceForum.isFavourite
                destinationTree.add(forumCopy)
                // copy this Forum's subforums, but ensure the parent IDs refer to this Forum's ID
                copyTreeWithParentId(sourceForum.subforums, forumCopy.subforums, forumCopy.id)
            }
        }

        /**
         * Recursively copy all subforums in a tree into a supplied list.
         * This maintains the hierarchy of the subforums and their descendants
         *
         * @param source     The source tree, whose hierarchy will be traversed
         * @param collection A list to collect all the subforum objects in
         */
        private fun copyForumTree(source: MutableList<Forum>, collection: MutableList<Forum>) {
            var forumCopy: Forum?
            for (forum in source) {
                forumCopy = Forum(forum)
                collection.add(forumCopy)
                copyForumTree(forum.subforums, forumCopy.subforums)
            }
        }


        /**
         * Recursively copy all subforums in a tree into a flat list.
         *
         * @param source     The source tree, whose hierarchy will be traversed
         * @param collection A list to collect all the subforum objects in
         */
        private fun collectSubforums(source: MutableList<Forum>, collection: MutableList<Forum>) {
            for (forum in source) {
                collection.add(Forum(forum))
                collectSubforums(forum.subforums, collection)
            }
        }
    }
    val numberOfForums: Int
        /**
         * Get the number of forums held in this structure.
         * 
         * @return The total number of forums, including section forums e.g. Main
         */
        get() = countForums(forumTree, 0)

    private fun countForums(forums: MutableList<Forum>, total: Int): Int {
        var total = total
        for (forum in forums) {
            total++
            countForums(forum.subforums, total)
        }
        return total
    }


    val asList: ListBuilder
        get() = ListBuilder()

    /**
     * Output format types:
     * 
     *  * FULL_TREE - the full hierarchy
     *  * TWO_LEVEL - categories/top-level forums/bookmarks etc at the top level,
     * each with any subforums compacted into a second level
     *  * FLAT - everything on a single level
     * 
     */
    enum class ListFormat {
        FULL_TREE, TWO_LEVEL, FLAT
    }


    inner class ListBuilder {
        private var includeSections = true

        private var listFormat : ListFormat = ListFormat.FULL_TREE


        /**
         * Include or exclude section forums (e.g. Main).
         * If sections are excluded, their immediate subforums will appear in the top level list.
         */
        fun includeSections(show: Boolean): ListBuilder {
            includeSections = show
            return this
        }


        /**
         * The type of list structure to produce.
         */
        fun formatAs( formatType: ListFormat): ListBuilder {
            listFormat = formatType
            return this
        }

        fun build(): MutableList<Forum> {
            val generatedList: MutableList<Forum> = ArrayList()

            for (rootForum in forumTree) {
                val rootForumCopy = Forum(rootForum)
                // only include sections if required
                if (!rootForum.isType(ForumType.SECTION) || includeSections) {
                    generatedList.add(rootForumCopy)
                }
                // add its subforums to the same level, or to the subforum list as appropriate
                for (mainForum in rootForum.subforums) {
                    val forumCopy = Forum(mainForum)
                    // the only time we don't add a top-level forum to the root list is when we're doing
                    // the full tree structure, and we're including sections (so the TLF is added as a subforum)
                    if (listFormat == ListFormat.FULL_TREE && includeSections) {
                        rootForumCopy.subforums.add(forumCopy)
                    } else {
                        generatedList.add(forumCopy)
                    }

                    when (listFormat) {
                        ListFormat.FLAT -> {
                            // flat list - add main forum and everything below it to the top level
                            collectSubforums(mainForum.subforums, generatedList)
                        }
                        ListFormat.TWO_LEVEL -> {
                            // two-level list - add main forum to the top level, and everything below it into its subforum list
                            collectSubforums(mainForum.subforums, forumCopy.subforums)
                        }
                        ListFormat.FULL_TREE -> {
                            // full tree structure
                            copyForumTree(mainForum.subforums, forumCopy.subforums)
                        }
                    }
                }
            }

            return generatedList
        }
    }
}
