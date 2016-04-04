package com.ferg.awfulapp.forums;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.ferg.awfulapp.constants.Constants.DEBUG;

/**
 * <p>Created by baka kaba on 01/04/2016.</p>
 * <p/>
 * <p>A task that parses and updates the forum structure by spidering subforum links.
 * This task is heavy (since it loads every forum page, ~75 pages at the time of writing)
 * but parses every forum visible to the user, and captures subtitle data.</p>
 * <p/>
 * <p>This basically works by building a tree asynchronously. It loads the main forum page,
 * parses it for the section links (Main, Discussion etc) and adds them to the root list as
 * {@link Forum} objects. Then each link is followed with a separate request, and each page
 * is parsed for its list of subforums. These are all created as Forum objects, added to their
 * parents' subforum lists, and then their links are followed.</p>
 */
public class CrawlerTask extends UpdateTask {

    /**
     * Delay constant for throttling low-priority update tasks (in ms)
     */
    public static final int PRIORITY_LOW = 2000;
    /**
     * Delay constant for throttling high-priority update tasks (in ms)
     */
    public static final int PRIORITY_HIGH = 0;

    private final List<Forum> forumSections = Collections.synchronizedList(new ArrayList<Forum>());

    {
        TAG = "CrawlerTask";
    }


    public CrawlerTask(@NonNull Context context, int priority) {
        super(context);
        initialTask = new MainForumRequest();
        taskDelayMillis = (priority == PRIORITY_HIGH) ? PRIORITY_HIGH : PRIORITY_LOW;
    }


    @NonNull
    @Override
    protected ForumStructure buildForumStructure() {
        return ForumStructure.buildFromTree(forumSections, ForumRepository.TOP_LEVEL_PARENT_ID);
    }


    /**
     * Parse the category links on the main forum page (Main, Discussion etc).
     * This is to ensure all the 'hidden' subforums are picked up, which don't show up
     * on the main single-page listing.
     *
     * @param doc A JSoup document built from the main forum page
     */
    private void parseMainSections(Document doc) {
        // look for section links on the main page - fail immediately if we can't find them!
        Elements sections = doc.getElementsByClass("category");
        if (sections.size() == 0) {
            fail("unable to parse main forum page - 0 links found!");
            return;
        }

        // parse each section to get its data, and add a 'forum' to the top level list
        for (Element section : sections) {
            Element link = section.select("a").first();
            String title = link.text();
            String url = link.attr("abs:href");

            addForumToList(forumSections, ForumRepository.TOP_LEVEL_PARENT_ID, url, title, "");
        }
    }


    /**
     * Parse a forum page, and attempt to scrape any subforum links it contains.
     * This can be used on category pages (e.g. the 'Main' link) as well as actual
     * forum pages (e.g. GBS)
     *
     * @param forum The Forum object representing the forum being parsed
     * @param doc   A JSoup document built from the forum's url
     */
    private void parseSubforums(Forum forum, Document doc) {

        // look for subforums
        Elements subforumElements = doc.getElementsByTag("tr").select(".subforum");
        if (DEBUG) {
            String message = "Parsed forum (%s) - found %d subforums";
            Log.d(TAG, String.format(message, forum.title, subforumElements.size()));
        }

        // parse details and create subforum objects, and add them to this forum's subforum list
        for (Element subforumElement : subforumElements) {
            Element link = subforumElement.select("a").first();
            String title = link.text();
            String subtitle = subforumElement.select("dd").text();
            String url = link.attr("abs:href");

            // strip leading junk on subtitles
            final String garbage = "- ";
            if (subtitle.startsWith(garbage)) {
                subtitle = subtitle.substring(garbage.length());
            }

            addForumToList(forum.subforums, forum.id, url, title, subtitle);
        }
    }


    /**
     * Parse a forum's url to retrieve its ID
     *
     * @param url The forum's full url
     * @return Its ID
     * @throws InvalidParameterException if the ID could not be found
     */
    private int getForumId(@NonNull String url) throws InvalidParameterException {
        String FORUM_ID_KEY = "forumid";
        try {
            Uri uri = Uri.parse(url);
            String forumId = uri.getQueryParameter(FORUM_ID_KEY);
            return Integer.valueOf(forumId);
        } catch (NumberFormatException e) {
            String message = "Unable to find forum ID key (%s) in url (%s)\nException: %s";
            throw new InvalidParameterException(String.format(message, FORUM_ID_KEY, url, e.getMessage()));
        }
    }


    /**
     * Create and add a Forum object to a list.
     *
     * @param forumList The list to add to
     * @param parentId  The ID of the parent forum
     * @param url       The url of this forum
     * @param title     The title of this forum
     * @param subtitle  The subtitle of this forum
     */
    private void addForumToList(@NonNull List<Forum> forumList,
                                int parentId,
                                @NonNull String url,
                                @NonNull String title,
                                @NonNull String subtitle) {
        try {
            // the subforum list needs to be synchronized since multiple async requests can add to it
            List<Forum> subforums = Collections.synchronizedList(new ArrayList<Forum>());
            Forum forum = new Forum(getForumId(url), parentId, title, subtitle, subforums);
            forumList.add(forum);

            if (!"".equals(url)) {
                startTask(new ParseSubforumsRequest(forum, url));
            }
        } catch (InvalidParameterException e) {
            Log.w(TAG, e.getMessage());
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Requests
    ///////////////////////////////////////////////////////////////////////////


    /**
     * A request that fetches the main forums page and parses it for sections (Main etc)
     */
    private class MainForumRequest extends ForumParseTask {

        {
            url = Constants.BASE_URL;
        }

        @Override
        protected void onRequestSucceeded(Document doc) {
            Log.i(TAG, "Parsing main page");
            parseMainSections(doc);
        }


        @Override
        protected void onRequestFailed(AwfulError error) {
            Log.w(TAG, "Failed to get index page!\n" + error.getMessage());
        }
    }


    /**
     * A request that fetches a forum page and parses it for subforum data
     */
    private class ParseSubforumsRequest extends ForumParseTask {

        @NonNull
        private final Forum forum;


        /**
         * Parse a Forum to add any subforums
         *
         * @param forum A Forum to load and parse, and add any subforums to
         */
        private ParseSubforumsRequest(@NonNull Forum forum, @NonNull String url) {
            this.forum = forum;
            this.url = url;
        }


        @Override
        protected void onRequestSucceeded(Document doc) {
            parseSubforums(forum, doc);
        }


        @Override
        protected void onRequestFailed(AwfulError error) {
            Log.w(TAG, String.format("Failed to load forum: %s\n%s", forum.title, error.getMessage()));
        }
    }

}
