package com.ferg.awfulapp.forums;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.ferg.awfulapp.forums.Forum.BOOKMARKS;
import static com.ferg.awfulapp.forums.Forum.SECTION;
import static com.ferg.awfulapp.forums.ForumStructure.FULL_TREE;
import static com.ferg.awfulapp.forums.ForumStructure.TWO_LEVEL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

/**
 * Created by baka kaba on 09/04/2016.
 */
public class ForumStructureTest {

    private static final int TOP_LEVEL_ID = 0;

    private Forum bookmarks;
    private Forum discussion;
    private Forum shsc;
    private Forum games;
    private Forum cobol;
    private Forum projectlog;

    private Forum bookmarksDup;
    private Forum discussionDup;
    private Forum shscDup;
    private Forum gamesDup;
    private Forum cobolDup;
    private Forum projectlogDup;

    private List<Forum> sourceTree;
    private List<Forum> partialTree;

    @Before
    public void setUp() {
        // reset the handy test objects
        bookmarks = new Forum(1, TOP_LEVEL_ID, "Bookmarks", "");
        discussion = new Forum(2, TOP_LEVEL_ID, "Discussion", "");
        shsc = new Forum(3, 2, "SH/SC", "");
        games = new Forum(4, 2, "Games", "");
        cobol = new Forum(5, 3, "Cavern of COBOL", "");
        projectlog = new Forum(6, 5, "project.log", "");

        // mark the special forum types
        bookmarks.setType(BOOKMARKS);
        discussion.setType(SECTION);

        // copy these so we can build another structure without affecting the subforum lists in the originals
        bookmarksDup = new Forum(bookmarks);
        discussionDup = new Forum(discussion);
        shscDup = new Forum(shsc);
        gamesDup = new Forum(games);
        cobolDup = new Forum(cobol);
        projectlogDup = new Forum(projectlog);

        /* build the tree from the forum hierarchy, making this:
                     root
          bookmarks      discussion
                       shsc    games
                      cobol
                    projectlog
        */
        sourceTree = new ArrayList<>();
        Collections.addAll(sourceTree, bookmarks, discussion);
        Collections.addAll(discussion.subforums, shsc, games);
        shsc.subforums.add(cobol);
        cobol.subforums.add(projectlog);

        // create an expected 'copy' using copies of the original forum objects, so this is an independent structure
        partialTree = new ArrayList<>();
        Forum bookmarksCopy = new Forum(bookmarks);
        Forum discussionCopy = new Forum(discussion);
        Forum shscCopy = new Forum(shsc);
        Forum gamesCopy = new Forum(games);
        Forum cobolCopy = new Forum(cobol);
        Forum projectlogCopy = new Forum(projectlog);
        Collections.addAll(partialTree, bookmarksCopy, discussionCopy);
        Collections.addAll(discussionCopy.subforums, shscCopy, gamesCopy);
        shscCopy.subforums.add(cobolCopy);
        cobolCopy.subforums.add(projectlogCopy);
    }


    /*
        Build from a flat list
     */


    @Test
    public void buildFromOrderedList() throws Exception {
        // create an ordered, flat list of the forums in sourceTree
        List<Forum> sourceList = new ArrayList<>();
        Collections.addAll(sourceList, bookmarks, discussion, shsc, games, cobol, projectlog);

        // build a ForumStructure from the flat list, and get the tree it creates
        ForumStructure forumStructure = ForumStructure.buildFromOrderedList(sourceList, TOP_LEVEL_ID);
        List<Forum> builtTree = forumStructure.getAsList().formatAs(FULL_TREE).build();

        // the built tree should match sourceTree
        assertThat(builtTree, is(aForumTreeMatching(sourceTree)));
    }


    @Test
    public void buildFromOrderedList_withOrphanedNodes() {
        // To be part of the final tree, a forum's parent ID needs to match another forum in
        // the tree, or the the top-level ID that represents the root of the tree

        // create a list of source objects, with some that won't be below the root (and will be discarded)
        List<Forum> partialSourceList = new ArrayList<>();
        Collections.addAll(partialSourceList, bookmarks, discussion, shsc, cobol, projectlog, games);
        // set a new ID for the root - only forums with discussion as their parent (or their descendants) should be added
        int newTopLevelId = discussion.id;

        // build the expected result - basically discussion's subforum hierarchy
        partialTree = new ArrayList<>();
        Forum newShsc = new Forum(shsc);
        Forum newGames = new Forum(games);
        Forum newCobol = new Forum(cobol);
        Forum newProjectlog = new Forum(projectlog);
        Collections.addAll(partialTree, newShsc, newGames);
        newShsc.subforums.add(newCobol);
        newCobol.subforums.add(newProjectlog);


        // build a ForumStructure from the flat list, using the new root ID
        ForumStructure forumStructure = ForumStructure.buildFromOrderedList(partialSourceList, newTopLevelId);
        List<Forum> builtTree = forumStructure.getAsList().formatAs(FULL_TREE).build();


        // the tree should only contain the section under the new root ID
        assertThat(builtTree, is(aForumTreeMatching(partialTree)));
    }


    @Test
    public void buildFromOrderedList_withEmptyList() {
        // create a ForumStructure from an empty list
        ForumStructure forumStructure = ForumStructure.buildFromOrderedList(Collections.<Forum>emptyList(), TOP_LEVEL_ID);
        List<Forum> builtTree = forumStructure.getAsList().formatAs(FULL_TREE).build();

        // the resulting tree should be empty
        assertThat(builtTree.isEmpty(), is(true));
    }


    /*
        Build from a tree
     */


    @Test
    public void buildFromForumTree() throws Exception {
        // build a hierarchy that matches the example tree, but with inconsistent ID/ParentID relationships
        // We've got different parents, missing parents, the whole thing
        List<Forum> inconsistentTree = new ArrayList<>();
        Forum newBookmarks = new Forum(1, TOP_LEVEL_ID, "Bookmarks", "");
        Forum newDiscussion = new Forum(2, TOP_LEVEL_ID, "Discussion", "");
        Forum newShsc = new Forum(3, 6, "SH/SC", "");
        Forum newGames = new Forum(4, 44, "Games", "");
        Forum newCobol = new Forum(5, 3, "Cavern of COBOL", "");
        Forum newProjectlog = new Forum(6, TOP_LEVEL_ID, "project.log", "");
        Collections.addAll(inconsistentTree, newBookmarks, newDiscussion);
        Collections.addAll(newDiscussion.subforums, newShsc, newGames);
        newShsc.subforums.add(newCobol);
        newCobol.subforums.add(newProjectlog);

        // build a structure from this and get its tree
        ForumStructure forumStructure = ForumStructure.buildFromTree(inconsistentTree, TOP_LEVEL_ID);
        List<Forum> builtTree = forumStructure.getAsList().formatAs(FULL_TREE).build();

        // the original should not match the expected tree (with all the ID mismatches)
        assertThat(inconsistentTree, is(not(aForumTreeMatching(sourceTree))));
        // the built tree should have resolved all this and match
        assertThat(builtTree, is(aForumTreeMatching(sourceTree)));
    }


    @Test
    public void buildFromForumTree_withEmptySource() {
        // create a ForumStructure from an empty tree
        ForumStructure forumStructure = ForumStructure.buildFromTree(Collections.<Forum>emptyList(), TOP_LEVEL_ID);
        List<Forum> builtTree = forumStructure.getAsList().formatAs(FULL_TREE).build();

        // the resulting tree should be empty
        assertThat(builtTree.isEmpty(), is(true));
    }



    ///////////////////////////////////////////////////////////////////////////
    // List builder
    ///////////////////////////////////////////////////////////////////////////


    @Test
    public void getFullTree() {
        // create a forum structure from a tree
        ForumStructure forumStructure = ForumStructure.buildFromTree(sourceTree, TOP_LEVEL_ID);

        // get the structure as a list representing the full tree
        List<Forum> builtTree = forumStructure.getAsList().formatAs(FULL_TREE).build();

        // this should match what we used to make the ForumStructure
        assertThat(builtTree, is(aForumTreeMatching(sourceTree)));
    }


    @Test
    public void getFullTree_withoutSections() {
        // create a forum structure from a tree
        ForumStructure forumStructure = ForumStructure.buildFromTree(sourceTree, TOP_LEVEL_ID);
        // this is the expected result - the same hierarchy only with the sections removed,
        // and their immediate subforums 'promoted' to the top level
        List<Forum> expectedTreeWithoutSections = new ArrayList<>();
        Collections.addAll(expectedTreeWithoutSections, bookmarks, shsc, games);

        // get the structure as a list representing the full tree
        List<Forum> builtList = forumStructure.getAsList().includeSections(false).formatAs(FULL_TREE).build();

        // the result should match the expected tree with the 'discussion' section removed
        assertThat(builtList, is(aForumTreeMatching(expectedTreeWithoutSections)));
    }


    @Test
    public void getTwoLevelTree() {
        // create a forum structure from a tree
        ForumStructure forumStructure = ForumStructure.buildFromTree(sourceTree, TOP_LEVEL_ID);
        // this is the expected result - top-level forums on the same level as bookmarks and sections,
        // all subforums flattened onto one level under the parent forum
        List<Forum> expectedTwoLevelTree = new ArrayList<>();
        Collections.addAll(expectedTwoLevelTree, bookmarksDup, discussionDup, shscDup, gamesDup);
        Collections.addAll(shscDup.subforums, cobolDup, projectlogDup);

        // get the structure as a list representing a two-level tree
        List<Forum> builtList = forumStructure.getAsList().formatAs(TWO_LEVEL).build();

        // this should match the expected tree we created
        assertThat(builtList, is(aForumTreeMatching(expectedTwoLevelTree)));
    }


    @Test
    public void getTwoLevelTree_withoutSections() {
        // create a forum structure from a tree
        ForumStructure forumStructure = ForumStructure.buildFromTree(sourceTree, TOP_LEVEL_ID);
        // this is the expected result - top-level forums on the same level as bookmarks,
        // with the discussion section removed, and subforums flattened onto one level
        List<Forum> expectedTwoLevelTree = new ArrayList<>();
        Collections.addAll(expectedTwoLevelTree, bookmarksDup, shscDup, gamesDup);
        Collections.addAll(shscDup.subforums, cobolDup, projectlogDup);

        // get the structure as a list representing a two-level tree
        List<Forum> builtList = forumStructure.getAsList().includeSections(false).formatAs(TWO_LEVEL).build();

        // this should match the expected tree we created
        assertThat(builtList, is(aForumTreeMatching(expectedTwoLevelTree)));
    }


    @Test
    public void getFlatList() {
        // create a forum structure from a tree
        ForumStructure forumStructure = ForumStructure.buildFromTree(sourceTree, TOP_LEVEL_ID);
        // this is the expected result - everything on one level, in index order
        List<Forum> expectedFlatList = new ArrayList<>();
        Collections.addAll(expectedFlatList, bookmarksDup, discussionDup, shscDup, cobolDup, projectlogDup, gamesDup);

        // get the structure as a flat list
        List<Forum> builtList = forumStructure.getAsList().formatAs(ForumStructure.FLAT).build();

        // this should match our expected list
        assertThat(builtList, is(aForumTreeMatching(expectedFlatList)));
    }


    @Test
    public void getFlatList_withoutSections() {
        // create a forum structure from a tree
        ForumStructure forumStructure = ForumStructure.buildFromTree(sourceTree, TOP_LEVEL_ID);
        // this is the expected result - everything on one level, in index order, with the discussion section missing
        List<Forum> expectedFlatList = new ArrayList<>();
        Collections.addAll(expectedFlatList, bookmarksDup, shscDup, cobolDup, projectlogDup, gamesDup);

        // get the structure as a flat list
        List<Forum> builtList = forumStructure.getAsList().includeSections(false).formatAs(ForumStructure.FLAT).build();

        // this should match our expected list
        assertThat(builtList, is(aForumTreeMatching(expectedFlatList)));
    }



    private Matcher<List<Forum>> aForumTreeMatching(final List<Forum> matchToThis) {
        return new TypeSafeMatcher<List<Forum>>() {

            private String errorMsg;

            @Override
            protected boolean matchesSafely(List<Forum> otherTree) {
                return nodesMatch(otherTree, matchToThis);
            }


            private boolean nodesMatch(List<Forum> otherNode, List<Forum> nodeToMatch) {
                if (otherNode.size() != nodeToMatch.size()) {
                    errorMsg = String.format("got the wrong number of forums (%d vs %d)\n", otherNode.size(), nodeToMatch.size());
                    errorMsg = String.format("%s\nYours contains:%s\nShould match:%s", errorMsg, printNode(otherNode), printNode(nodeToMatch));
                    return false;
                }
                Forum firstForum, secondForum;
                // check the forums in each position match, and do the same for their subforums
                for (int i = 0; i < otherNode.size(); i++) {
                    firstForum = otherNode.get(i);
                    secondForum = nodeToMatch.get(i);
                    if (!firstForum.equals(secondForum)) {
                        errorMsg = String.format("Forums in position %d don't match", i);
                        errorMsg = String.format("%s\nYours: (%d)%s\nShould match: (%d)%s",
                                errorMsg, firstForum.id, firstForum.title, secondForum.id, secondForum.title);
                        return false;
                    } else if (!nodesMatch(firstForum.subforums, secondForum.subforums)) {
                        errorMsg = String.format("Subforum of %s and %s\n%s", firstForum.title, secondForum.title, errorMsg);
                        return false;
                    }
                }

                return true;
            }

            private String printNode(List<Forum> node) {
                String message = "";
                for (Forum forum : node) {
                    message = message + " (" + forum.id + ")" + forum.title;
                }
                return message;
            }


            @Override
            public void describeTo(Description description) {
                description.appendText("a tree with the same hierarchy of Forum objects, in the same order");
            }


            @Override
            protected void describeMismatchSafely(List<Forum> item, Description mismatchDescription) {
                mismatchDescription.appendText(errorMsg);
            }
        };
    }

}