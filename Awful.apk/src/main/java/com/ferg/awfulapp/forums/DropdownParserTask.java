package com.ferg.awfulapp.forums;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * <p>Created by baka kaba on 18/04/2016.</p>
 * <p/>
 * <p>Forum update task that parses the forum jump dropdown.
 * This task only requires a single page load, but can't scrape subtitles
 * and misses certain forums.</p>
 * <p/>
 * <p>It works by finding the set of <code>option</code> tags in the dropdown
 * box, then looks for the items that are formatted like forum entries.
 * It keeps a running model of the current branch hierarchy, so it can identify
 * what level a forum should be on and which forum is its parent</p>
 */
public class DropdownParserTask extends UpdateTask {

    private final List<Forum> parsedForums = new ArrayList<>();

    {
        TAG = "DropdownParserTask";
    }


    public DropdownParserTask(@NonNull Context context) {
        super(context);
        initialTask = new DropdownParseRequest();
    }


    private class DropdownParseRequest extends ForumParseTask {

        {
            url = Constants.FUNCTION_FORUM + "?" + Constants.PARAM_FORUM_ID + "=" + Constants.FORUM_ID_GOLDMINE;
        }

        @Override
        protected void onRequestSucceeded(Document doc) {
            Log.i(TAG, "Got page - parsing dropdown to get forum hierarchy");
            parsePage(doc);
        }


        @Override
        protected void onRequestFailed(AwfulError error) {
            Log.w(TAG, "Request error - couldn't get page to parse");
        }
    }


    private void parsePage(Document doc) {
        // can't do anything without a page - fail immediately
        if (doc == null) {
            fail("no document to parse");
            return;
        }

        class ParsedForum {
            final int id;
            final int depth;


            ParsedForum(int id, int depth) {
                this.id = id;
                this.depth = depth;
            }
        }

        try {
            // this stack works like a breadcrumb trail, so we can compare to the last forum
            // added at the current depth, and work out the new forum's relationship to it
            Stack<ParsedForum> parsedForumStack = new Stack<>();
            // this is a dummy forum representing the root - every other forum will be below this (depth >= 0)
            parsedForumStack.push(new ParsedForum(ForumRepository.TOP_LEVEL_PARENT_ID, -1));
            ParsedForum previousForum;

            // get the items from the dropdown
            Elements forums = doc.select("form.forum_jump option");
            Log.d(TAG, "Found " + forums.size() + " elements");
            for (Element option : forums) {
                String idString = option.val();
                String rawTitle;
                try {
                    // we need the raw text so we can preserve the leading space that identifies sections like Main
                    rawTitle = option.textNodes().get(0).getWholeText();
                } catch (IndexOutOfBoundsException e) {
                    throw new ParseException("Couldn't get TextNode for title", 0);
                }

                Integer forumDepth = getForumDepth(rawTitle);
                if (forumDepth == null) {
                    // not a forum
                    continue;
                }
                // strip off the depth-marking prefix (dashes plus a leading space)
                String title = rawTitle.substring(forumDepth + 1);

                // if this new forum isn't as deep as the previous one on the stack, it's a new branch
                // and we need to back up. Pop them off until we find a forum on the same or higher level
                previousForum = parsedForumStack.peek();
                while (previousForum.depth > forumDepth) {
                    parsedForumStack.pop();
                    previousForum = parsedForumStack.peek();
                }

                if (forumDepth == previousForum.depth) {
                    // the new forum is a sibling of the one on the stack - remove that so
                    // this will take its place as the last added on this level (and the one
                    // on the stack is its parent)
                    parsedForumStack.pop();
                    previousForum = parsedForumStack.peek();
                }


                int id;
                try {
                    id = Integer.parseInt(idString);
                } catch (NumberFormatException e) {
                    throw new ParseException("Can't parse forum ID as int! Got: " + idString, 0);
                }
                // we're going to push the new forum onto the stack - the one currently on top will be its parent
                int parentId = previousForum.id;
                parsedForums.add(new Forum(id, parentId, title, ""));
                parsedForumStack.push(new ParsedForum(id, forumDepth));

                if (Constants.DEBUG) {
                    Log.d(TAG, String.format("(ID:%s Parent:%d) %s", id, parentId, title));
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "Unexpected parse failure - has the page formatting changed?", e);
            if (Constants.DEBUG) {
                Toast.makeText(context, "DROPDOWN PARSE ERROR\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }


    /**
     * Get the depth of a forum by its title prefix in the dropdown.
     * The value is the number of leading dash characters, so the actual
     * depth is relative (e.g. one level below -- is ----, so one level deeper than 2 is 4)
     *
     * @param rawTitleText The raw, unnormalised text from the dropdown (the whitespace is important)
     * @return The depth value, which is the number of leading dashes (so you can strip them for display)
     */
    @Nullable
    private Integer getForumDepth(@NonNull String rawTitleText) {
        if (rawTitleText.isEmpty()) {
            return null;
        }

        // Valid forum titles have some number of - chars followed by a space, so get the number of -s
        for (int i = 0; i < rawTitleText.length(); i++) {
            char c = rawTitleText.charAt(i);
            if (c == ' ') {
                return i;
            } else if (c != '-') {
                // if we run into a non-dash before we find a space, give up
                break;
            }
        }

        // we didn't find a space after some dashes, so this isn't a valid forum title!
        return null;
    }


    @NonNull
    @Override
    protected ForumStructure buildForumStructure() {
        return ForumStructure.buildFromOrderedList(parsedForums, ForumRepository.TOP_LEVEL_PARENT_ID);
    }
}
